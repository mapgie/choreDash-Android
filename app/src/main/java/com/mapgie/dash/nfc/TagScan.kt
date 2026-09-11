package com.mapgie.dash.nfc

import java.net.URLDecoder

/** Which kind of thing an NFC tag stands for: a chore's tag id, or a memo's id. */
enum class TagKind { CHORE, MEMO }

/** A scanned tag resolved to the kind that minted it and the bare id it carries. */
data class ScannedTag(val kind: TagKind, val id: String)

/**
 * Turns the raw payload read off an NFC tag into a [ScannedTag]. Pure (no Android),
 * so it is unit-tested directly.
 *
 * The app writes a `chordash://tag?tag=<id>` (chore) or `chordash://memo?memo=<id>`
 * (memo) URI record. Tags written by hand with NFC Tools often store the same URI
 * as a plain **text** record instead, so this accepts either: a chordash URI in any
 * position is reduced to its `tag=`/`memo=` param (or last path segment), and the
 * host says which kind. Anything that is not a chordash URI is treated as a bare
 * chore/tag id (legacy Tasker tags that hold just the id). This is what stops a raw
 * `chordash://…` string ever leaking through as an id or a name.
 */
fun parseTagPayload(raw: String): ScannedTag? {
    val text = raw.trim()
    if (text.isEmpty()) return null

    val scheme = "chordash://"
    if (!text.startsWith(scheme, ignoreCase = true)) {
        // Not a deep link: the whole payload is a bare id (a chore tag id).
        return ScannedTag(TagKind.CHORE, text)
    }

    val rest = text.substring(scheme.length)
    val host = rest.substringBefore('?').substringBefore('/').substringBefore('#').lowercase()
    val query = rest.substringAfter('?', "").substringBefore('#')
    val params = query.split('&').mapNotNull { part ->
        val eq = part.indexOf('=')
        if (eq <= 0) null else part.take(eq) to part.substring(eq + 1)
    }.toMap()

    val kind = if (host == "memo") TagKind.MEMO else TagKind.CHORE
    val encoded = when (kind) {
        TagKind.MEMO -> params["memo"] ?: params["tag"]
        TagKind.CHORE -> params["tag"] ?: params["memo"]
    } ?: rest.substringBefore('?').substringBefore('#').substringAfterLast('/').ifBlank { null }

    val id = encoded
        ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    return ScannedTag(kind, id)
}
