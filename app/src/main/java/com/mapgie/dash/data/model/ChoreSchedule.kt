package com.mapgie.dash.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * The unit a chore repeats in. [wire] is what `tags.repeat_unit` holds: null for
 * days, so every chore saved before units existed reads as days unchanged.
 * [days] is the approximate length written to `interval_days` alongside it, which
 * keeps the web app, cadence buckets and snooze lengths working on one number.
 */
enum class RepeatUnit(val wire: String?, val days: Int, val singular: String, val plural: String) {
    DAY(null, 1, "day", "days"),
    WEEK("week", 7, "week", "weeks"),
    MONTH("month", 30, "month", "months"),
    YEAR("year", 365, "year", "years");

    /** [date] moved on by [count] of this unit, on the calendar (a year is a year, leap or not). */
    fun addTo(date: LocalDate, count: Long): LocalDate = when (this) {
        DAY -> date.plusDays(count)
        WEEK -> date.plusWeeks(count)
        MONTH -> date.plusMonths(count)
        YEAR -> date.plusYears(count)
    }

    companion object {
        /** The unit stored as [wire]; anything unknown or null reads as days. */
        fun fromWire(wire: String?): RepeatUnit = entries.firstOrNull { it.wire != null && it.wire == wire } ?: DAY

        /** Every value the app can write to `tags.repeat_unit` (days are written as null). */
        val storedValues: List<String> get() = entries.mapNotNull { it.wire }
    }
}

/**
 * The when-fields a chore edit saves together: how often it repeats, the due
 * date it is anchored to, and [leadDays], this chore's own "hide until this
 * many days before due" (null: the list's automatic rule decides).
 */
data class ChoreSchedule(
    val repeat: ChoreRepeat? = null,
    val dueDate: LocalDate? = null,
    val leadDays: Int? = null,
)

/** "Every [every] [unit]": how often a chore comes round. */
data class ChoreRepeat(val every: Int, val unit: RepeatUnit) {

    /** The approximate length in days, as stored in `interval_days`. */
    val intervalDays: Double get() = (every.toLong() * unit.days).toDouble()

    /** The [k]th occurrence counted from [anchor] (k may be negative), always measured from the anchor. */
    fun occurrence(anchor: LocalDate, k: Long): LocalDate = unit.addTo(anchor, every.toLong() * k)

    /** Card caption words: "every 3d", "weekly", "every 2mo", "yearly". */
    fun shortLabel(): String = when (unit) {
        RepeatUnit.DAY -> "every ${every}d"
        RepeatUnit.WEEK -> if (every == 1) "weekly" else "every ${every}w"
        RepeatUnit.MONTH -> if (every == 1) "monthly" else "every ${every}mo"
        RepeatUnit.YEAR -> if (every == 1) "yearly" else "every ${every}y"
    }

    /** Spoken and dialog words: "every 1 year", "every 3 days". */
    fun longLabel(): String = "every $every ${if (every == 1) unit.singular else unit.plural}"

    companion object {
        /**
         * Reads the repeat back from the two stored columns. A unit other than days
         * applies only when [intervalDays] is a whole multiple of it, so an interval
         * edited elsewhere (say to 45 on a "month" chore) falls back to plain days
         * rather than being rounded into something the user never set.
         */
        fun from(intervalDays: Double?, unit: RepeatUnit): ChoreRepeat? {
            val days = intervalDays?.takeIf { it > 0 } ?: return null
            if (unit != RepeatUnit.DAY && days == Math.floor(days) && days.toLong() % unit.days == 0L) {
                return ChoreRepeat((days.toLong() / unit.days).toInt(), unit)
            }
            return ChoreRepeat(days.roundToInt().coerceAtLeast(1), RepeatUnit.DAY)
        }
    }
}

/**
 * How early a log can come and still tick off a due date that does not repeat.
 * Logs older than this belong to the chore's history before the date was set.
 */
const val ONE_OFF_EARLY_DAYS = 30L

/**
 * The date a dated chore is next due, or null once a one-off date has been done.
 *
 * [anchor] is the due date the user set; the chore then falls due on the
 * anchor and every [repeat] after it. The latest log ticks off the occurrence
 * nearest to it, early or late, so paying the insurance a week before 1 Oct or a
 * few days after both move it on to next year's 1 Oct. Nothing is written when a
 * chore is logged: undoing the log undoes the advance for free. Occurrences
 * before the anchor never come due, so a log from before the date was set cannot
 * skip it.
 *
 * With no [repeat] the date is done by any log from [ONE_OFF_EARLY_DAYS] before it on.
 */
fun nextChoreDueDate(anchor: LocalDate, repeat: ChoreRepeat?, lastLog: LocalDate?): LocalDate? {
    if (repeat == null) {
        val done = lastLog != null && !lastLog.isBefore(anchor.minusDays(ONE_OFF_EARLY_DAYS))
        return if (done) null else anchor
    }
    if (lastLog == null) return anchor

    // Bracket the log between two occurrences: occurrence(k) <= lastLog < occurrence(k + 1).
    val approxDays = (repeat.every.toLong() * repeat.unit.days).coerceAtLeast(1)
    var k = Math.floorDiv(ChronoUnit.DAYS.between(anchor, lastLog), approxDays)
    while (repeat.occurrence(anchor, k).isAfter(lastLog)) k--
    while (!repeat.occurrence(anchor, k + 1).isAfter(lastLog)) k++

    val before = repeat.occurrence(anchor, k)
    val after = repeat.occurrence(anchor, k + 1)
    // A log exactly half way counts as the next one done early.
    val ticked = if (ChronoUnit.DAYS.between(before, lastLog) < ChronoUnit.DAYS.between(lastLog, after)) k else k + 1
    return repeat.occurrence(anchor, maxOf(ticked + 1, 0))
}

/** How many days ahead a dated chore turns amber: half its repeat, between 1 and 7 days. */
fun dueSoonDays(repeat: ChoreRepeat?): Long =
    repeat?.let { (it.every.toLong() * it.unit.days / 2).coerceIn(1, 7) } ?: 7

/** "1 Oct" this year, "1 Oct 2027" any other year. */
fun formatDueDate(date: LocalDate, today: LocalDate = LocalDate.now()): String =
    date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "d MMM" else "d MMM yyyy"))
