package com.mapgie.dash.data.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * How urgent a memo is right now. Maps onto the shared status scale in the UI
 * (rose / amber / sage / neutral) but lives here, in plain Kotlin, so the
 * list-state tests can pin the buckets.
 */
enum class ReminderStatus {
    /** It rang and nobody has answered yet, or a once-only ring was missed. */
    RANG,

    /** Rings within the next 24 hours. */
    DUE_SOON,

    /** Rings later than that. */
    UPCOMING,

    /** A once-only memo that was marked Done. */
    DONE,
}

private val WEEKDAYS = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)
private val WEEKENDS = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** The three shortcut chips on the Repeat card, plus the day sets they stand for. */
enum class RepeatPreset(val label: String, val days: Set<DayOfWeek>) {
    WEEKDAYS_ONLY("Weekdays", WEEKDAYS),
    WEEKENDS_ONLY("Weekends", WEEKENDS),
    EVERY_DAY("Every day", DayOfWeek.values().toSet());

    companion object {
        fun matching(days: Set<DayOfWeek>): RepeatPreset? = entries.firstOrNull { it.days == days }
    }
}

/**
 * Every line of copy that describes a memo's schedule, computed from instants
 * so the wording is unit-tested without Android: the card's meta line, its
 * next-ring badge, and the sheet's "Next ring" banner.
 */
object ReminderScheduleText {

    private const val MIDDLE_DOT = " · "

    private val fullTime = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val hourOnly = DateTimeFormatter.ofPattern("h a", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    /** "7:00 AM" */
    fun time(timeOfDay: LocalTime): String = fullTime.format(timeOfDay).uppercase(Locale.ENGLISH)

    /** "9 AM" on the hour, otherwise "7:30 AM". Short form for badges. */
    fun shortTime(timeOfDay: LocalTime): String =
        (if (timeOfDay.minute == 0) hourOnly else fullTime).format(timeOfDay).uppercase(Locale.ENGLISH)

    /**
     * "Weekdays", "Weekends", "Every day", or the chosen days Monday-first:
     * "Mon · Wed · Fri". Empty for a once-only schedule.
     */
    fun daysLabel(days: Set<DayOfWeek>): String {
        if (days.isEmpty()) return ""
        RepeatPreset.matching(days)?.let { return it.label }
        return days.sorted().joinToString(MIDDLE_DOT) {
            it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        }
    }

    /**
     * The card's meta line: "Weekdays · 8:00 PM", "Mon · Wed · Fri · 7:00 AM",
     * or "Once · doesn't repeat"; with " · linked to chore" / " · linked to task"
     * when [linkedTo] names what it hangs off.
     */
    fun scheduleLine(
        reminder: ReminderDto,
        zone: ZoneId = ZoneId.systemDefault(),
        linkedTo: String? = null,
    ): String {
        val days = reminder.repeatDaySet()
        val base = if (days.isEmpty()) {
            "Once${MIDDLE_DOT}doesn't repeat"
        } else {
            val time = reminder.timeOfDay(zone)
            if (time == null) daysLabel(days) else daysLabel(days) + MIDDLE_DOT + time(time)
        }
        return if (linkedTo == null) base else base + MIDDLE_DOT + "linked to $linkedTo"
    }

    /** Which urgency bucket the memo is in at [now]. */
    fun status(reminder: ReminderDto, now: Instant): ReminderStatus {
        if (!reminder.repeats && reminder.completedAt != null) return ReminderStatus.DONE
        if (reminder.unacknowledgedRing() != null) return ReminderStatus.RANG
        val next = reminder.remindAtInstant() ?: return ReminderStatus.UPCOMING
        if (!next.isAfter(now)) return ReminderStatus.RANG
        return if (ChronoUnit.HOURS.between(now, next) < 24) ReminderStatus.DUE_SOON else ReminderStatus.UPCOMING
    }

    /**
     * The card's right-hand badge: "rang 2h ago", "due 3h ago" (missed while
     * the device was off), "rings today 8 PM", "rings tomorrow 9 AM",
     * "rings Wed 7 AM", "rings 12 Oct 9 AM", or "done".
     */
    fun nextRingBadge(reminder: ReminderDto, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        if (!reminder.repeats && reminder.completedAt != null) return "done"
        reminder.unacknowledgedRing()?.let { return "rang ${ago(it, now)}" }
        val next = reminder.remindAtInstant() ?: return "no time set"
        if (!next.isAfter(now)) return "due ${ago(next, now)}"
        return "rings " + whenLabel(next, now, zone)
    }

    /** "today 8 PM", "tomorrow 9 AM", "Wed 7 AM" within the week, otherwise "12 Oct 9 AM". */
    fun whenLabel(at: Instant, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        val today = now.atZone(zone).toLocalDate()
        val z = at.atZone(zone)
        val day = z.toLocalDate()
        val time = shortTime(z.toLocalTime())
        return when {
            day == today -> "today $time"
            day == today.plusDays(1) -> "tomorrow $time"
            day.isBefore(today.plusDays(7)) -> day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + time
            else -> dayMonth.format(day) + " " + time
        }
    }

    /** The sheet banner's absolute half: "Wed, 7:00 AM". */
    fun bannerWhen(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        val z = at.atZone(zone)
        return z.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + ", " + time(z.toLocalTime())
    }

    /** The sheet banner's relative half: "in 4 days", "in 3 hours", "in 20 minutes", "now". */
    fun bannerRelative(at: Instant, now: Instant): String {
        if (!at.isAfter(now)) return "now"
        val minutes = ChronoUnit.MINUTES.between(now, at)
        val hours = ChronoUnit.HOURS.between(now, at)
        val days = ChronoUnit.DAYS.between(now, at)
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "in ${plural(minutes, "minute")}"
            hours < 24 -> "in ${plural(hours, "hour")}"
            else -> "in ${plural(days, "day")}"
        }
    }

    /** "just now", "5m ago", "2h ago", "3d ago". */
    fun ago(then: Instant, now: Instant): String {
        val minutes = ChronoUnit.MINUTES.between(then, now)
        if (minutes < 1) return "just now"
        if (minutes < 60) return "${minutes}m ago"
        val hours = ChronoUnit.HOURS.between(then, now)
        if (hours < 24) return "${hours}h ago"
        return "${ChronoUnit.DAYS.between(then, now)}d ago"
    }

    private fun plural(n: Long, unit: String): String = if (n == 1L) "1 $unit" else "$n ${unit}s"
}
