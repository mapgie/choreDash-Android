package com.mapgie.dash.ui.screens.reminder

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Which record a reminder notification points at. Matches the `{kind}` route segment. */
enum class ReminderViewKind(val routeArg: String) {
    /** A standalone reminder stored by [com.mapgie.dash.data.repository.ReminderRepository]. */
    REMINDER("reminder"),

    /** A task whose own `reminder_at` fired. */
    TASK("task");

    companion object {
        fun fromRouteArg(arg: String?): ReminderViewKind? = values().firstOrNull { it.routeArg == arg }
    }
}

/** One pending nudge, of either kind, used to pick the footer's "next:" item. */
data class UpcomingNudge(
    val kind: ReminderViewKind,
    val id: String,
    val subject: String,
    val at: Instant,
)

/**
 * Every line of copy on the reminder view, computed from instants so the wording
 * can be unit-tested without Android: the header clock, the "due today · about
 * 5 minutes" meta line, and the "next: ... · 4:30pm" footer.
 */
object ReminderViewText {

    private const val MIDDLE_DOT = " · "

    private val headerFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val footerFormatter = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)
    private val dayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    /** Header clock, e.g. "6:00 PM". */
    fun headerTime(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        headerFormatter.format(at.atZone(zone)).uppercase(Locale.ENGLISH)

    /** Footer clock, e.g. "4:30pm". */
    fun footerTime(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        footerFormatter.format(at.atZone(zone)).lowercase(Locale.ENGLISH)

    /**
     * "due today · about 5 minutes", "due tomorrow · in 14 hours",
     * "due today · 20 minutes ago". Units truncate (5m30s reads as 5 minutes).
     */
    fun metaLine(remindAt: Instant, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        dueDay(remindAt, now, zone) + MIDDLE_DOT + relativeToNow(remindAt, now)

    /** "due today", "due tomorrow", "due yesterday", otherwise "due 15 Apr". */
    fun dueDay(remindAt: Instant, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        val today = now.atZone(zone).toLocalDate()
        val day = remindAt.atZone(zone).toLocalDate()
        return when (day) {
            today -> "due today"
            today.plusDays(1) -> "due tomorrow"
            today.minusDays(1) -> "due yesterday"
            else -> "due " + dayFormatter.format(day)
        }
    }

    /**
     * Future: "right now", "about 5 minutes", "in 14 hours", "in 3 days".
     * Past: "just now", "20 minutes ago", "3 hours ago", "2 days ago".
     */
    fun relativeToNow(remindAt: Instant, now: Instant): String {
        val past = remindAt.isBefore(now)
        val from = if (past) remindAt else now
        val to = if (past) now else remindAt
        val minutes = ChronoUnit.MINUTES.between(from, to)
        val hours = ChronoUnit.HOURS.between(from, to)
        val days = ChronoUnit.DAYS.between(from, to)
        return when {
            minutes < 1 -> if (past) "just now" else "right now"
            minutes < 60 -> if (past) "${plural(minutes, "minute")} ago" else "about ${plural(minutes, "minute")}"
            hours < 24 -> if (past) "${plural(hours, "hour")} ago" else "in ${plural(hours, "hour")}"
            else -> if (past) "${plural(days, "day")} ago" else "in ${plural(days, "day")}"
        }
    }

    /** "next: Pick up prescription · 4:30pm", or "nothing else scheduled". */
    fun nextLine(next: UpcomingNudge?, zone: ZoneId = ZoneId.systemDefault()): String =
        if (next == null) "nothing else scheduled"
        else "next: ${next.subject}$MIDDLE_DOT${footerTime(next.at, zone)}"

    /**
     * The earliest pending nudge other than the one being viewed. A pending item
     * whose time has already passed still counts: it is delivered late rather
     * than dropped, so it really is the next thing the user will hear about.
     */
    fun nextAfter(
        currentKind: ReminderViewKind?,
        currentId: String?,
        candidates: List<UpcomingNudge>,
    ): UpcomingNudge? =
        candidates
            .filterNot { it.kind == currentKind && it.id == currentId }
            .minByOrNull { it.at }

    private fun plural(n: Long, unit: String): String = if (n == 1L) "1 $unit" else "$n ${unit}s"
}
