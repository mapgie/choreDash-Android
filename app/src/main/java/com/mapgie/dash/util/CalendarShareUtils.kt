package com.mapgie.dash.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mapgie.dash.BuildConfig
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * Shared info describing a single calendar-able item (chore, task, or reminder),
 * used to build "Add to calendar" intents, .ics files, and plain-text shares
 * without duplicating date/time derivation logic across screens.
 */
data class CalendarEventInfo(
    val title: String,
    val description: String? = null,
    val location: String? = null,
    /** Start of the event in epoch millis, or null if the item has no date/time. */
    val beginMillis: Long? = null,
    /** End of the event in epoch millis. Only meaningful if [beginMillis] is set. */
    val endMillis: Long? = null,
    /** True if this should be represented as an all-day event. */
    val allDay: Boolean = false
)

/**
 * Builds [CalendarEventInfo] from a date-only value (e.g. a chore/task due date with
 * no time component). Produces an all-day event spanning midnight to midnight the next day.
 */
fun calendarEventForDate(
    title: String,
    description: String?,
    date: LocalDate,
    location: String? = null
): CalendarEventInfo {
    val start = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    return CalendarEventInfo(
        title = title,
        description = description,
        location = location,
        beginMillis = start,
        endMillis = end,
        allDay = true
    )
}

/**
 * Builds [CalendarEventInfo] from a specific instant (e.g. a task or reminder
 * "remind at" timestamp). Produces a one-hour timed event starting at that instant.
 */
fun calendarEventForInstant(
    title: String,
    description: String?,
    instant: Instant,
    location: String? = null,
    durationMinutes: Long = 60
): CalendarEventInfo {
    val start = instant.toEpochMilli()
    val end = instant.plusSeconds(durationMinutes * 60).toEpochMilli()
    return CalendarEventInfo(
        title = title,
        description = description,
        location = location,
        beginMillis = start,
        endMillis = end,
        allDay = false
    )
}

/**
 * Builds [CalendarEventInfo] with no date/time at all, for items without any
 * due date, due period, or reminder. The system calendar app will default to "now".
 */
fun calendarEventWithoutTime(title: String, description: String?, location: String? = null) =
    CalendarEventInfo(title = title, description = description, location = location)

object CalendarShareUtils {

    private val ICS_DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)
    private val ICS_DATETIME_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)

    /**
     * Builds an [Intent.ACTION_INSERT] intent that launches the system calendar app's
     * "create event" UI, pre-filled from [info]. Does not write to the calendar directly,
     * so no calendar permission is required.
     */
    fun buildAddToCalendarIntent(info: CalendarEventInfo): Intent {
        val intent = Intent(Intent.ACTION_INSERT, android.provider.CalendarContract.Events.CONTENT_URI)
            .putExtra(android.provider.CalendarContract.Events.TITLE, info.title)

        info.description?.let {
            intent.putExtra(android.provider.CalendarContract.Events.DESCRIPTION, it)
        }
        info.location?.let {
            intent.putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, it)
        }
        if (info.beginMillis != null) {
            intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, info.beginMillis)
            info.endMillis?.let {
                intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, it)
            }
            if (info.allDay) {
                intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            }
        }
        return intent
    }

    /**
     * Writes a minimal .ics file describing [info] to the app's cache directory
     * (under `cache/ics/`) and returns an [Intent.ACTION_SEND] chooser intent sharing
     * it as `text/calendar` via [FileProvider]. Requires a `<provider>` entry for
     * androidx.core.content.FileProvider in the manifest.
     */
    fun buildShareIcsIntent(context: Context, info: CalendarEventInfo): Intent {
        val icsDir = File(context.cacheDir, "ics").apply { mkdirs() }
        val file = File(icsDir, "event_${UUID.randomUUID()}.ics")
        file.writeText(buildIcsContent(info))

        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share as calendar event")
    }

    /**
     * Builds an [Intent.ACTION_SEND] chooser intent sharing a plain-text summary of [info].
     */
    fun buildSharePlainTextIntent(info: CalendarEventInfo): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, buildPlainTextSummary(info))
        }
        return Intent.createChooser(sendIntent, "Share")
    }

    private fun buildPlainTextSummary(info: CalendarEventInfo): String {
        val lines = mutableListOf(info.title)

        if (info.beginMillis != null) {
            val begin = Instant.ofEpochMilli(info.beginMillis)
            if (info.allDay) {
                val date = begin.atZone(ZoneOffset.UTC).toLocalDate()
                lines += "When: ${date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
            } else {
                val zoned = begin.atZone(java.time.ZoneId.systemDefault())
                lines += "When: ${zoned.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm"))}"
            }
        }

        info.location?.takeIf { it.isNotBlank() }?.let { lines += "Where: $it" }
        info.description?.takeIf { it.isNotBlank() }?.let {
            lines += ""
            lines += it
        }

        return lines.joinToString("\n")
    }

    private fun buildIcsContent(info: CalendarEventInfo): String {
        val now = ICS_DATETIME_FORMAT.format(Instant.now().atZone(ZoneOffset.UTC))
        val uid = "${UUID.randomUUID()}@chordash"

        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//choreDash//ICS Export//EN",
            "BEGIN:VEVENT",
            "UID:$uid",
            "DTSTAMP:$now",
            "SUMMARY:${escapeIcsText(info.title)}"
        )

        if (info.beginMillis != null) {
            val begin = Instant.ofEpochMilli(info.beginMillis)
            if (info.allDay) {
                val startDate = begin.atZone(ZoneOffset.UTC).toLocalDate()
                lines += "DTSTART;VALUE=DATE:${ICS_DATE_FORMAT.format(startDate)}"
                val endDate = info.endMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                } ?: startDate.plusDays(1)
                lines += "DTEND;VALUE=DATE:${ICS_DATE_FORMAT.format(endDate)}"
            } else {
                lines += "DTSTART:${ICS_DATETIME_FORMAT.format(begin.atZone(ZoneOffset.UTC))}"
                val end = info.endMillis?.let { Instant.ofEpochMilli(it) } ?: begin.plusSeconds(3600)
                lines += "DTEND:${ICS_DATETIME_FORMAT.format(end.atZone(ZoneOffset.UTC))}"
            }
        }

        info.description?.takeIf { it.isNotBlank() }?.let {
            lines += "DESCRIPTION:${escapeIcsText(it)}"
        }
        info.location?.takeIf { it.isNotBlank() }?.let {
            lines += "LOCATION:${escapeIcsText(it)}"
        }

        lines += "END:VEVENT"
        lines += "END:VCALENDAR"

        return lines.joinToString("\r\n") + "\r\n"
    }

    /** Escapes commas, semicolons, backslashes, and newlines per RFC 5545. */
    private fun escapeIcsText(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
}
