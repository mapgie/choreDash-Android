package com.mapgie.dash.data.supabase

/**
 * The part of an exception message worth putting on screen.
 *
 * supabase-kt's REST exceptions describe the whole request: the Postgres
 * message on the first line, then "URL:", "Headers:" (apikey and bearer
 * included) and "Http Method:" on the lines after. Only the first line tells
 * the user anything ("permission denied for table tags"); the rest is noise
 * on a phone and puts the project key on the screen. Keep the message up to
 * the first such marker, and fall back to the exception's type when there is
 * no message at all.
 */
fun Throwable.userFacingMessage(): String {
    val raw = message?.trim().orEmpty()
    if (raw.isEmpty()) return this::class.simpleName ?: "Something went wrong"
    return userFacingMessage(raw)
}

/** [Throwable.userFacingMessage] on the message text alone, for the test. */
fun userFacingMessage(raw: String): String {
    val cut = REQUEST_DUMP.find(raw)?.range?.first
    val head = if (cut == null) raw else raw.substring(0, cut)
    return head.trim().trimEnd(',', ';').ifEmpty { "Request failed" }
}

// The request dump's field labels, at the start of the message or after any whitespace.
private val REQUEST_DUMP = Regex("""(^|\s)(URL|Headers|Http Method):""")
