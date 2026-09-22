package com.mapgie.dash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Serializable
data class TagDto(
    @SerialName("id") val id: String,
    @SerialName("tag_id") val tagId: String,
    @SerialName("label") val label: String,
    @SerialName("category") val category: String? = null,
    @SerialName("owner") val owner: String? = null,
    @SerialName("interval_days") val intervalDays: Double? = null,
    /** The due date the user set (ISO date), or null for a chore timed from its last log. */
    @SerialName("due_date") val dueDate: String? = null,
    /** [RepeatUnit.wire]: what [intervalDays] counts in. Null means days. */
    @SerialName("repeat_unit") val repeatUnit: String? = null,
    /** Hide until this many days before due; null leaves it to the automatic rule. */
    @SerialName("lead_days") val leadDays: Int? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ScanDto(
    @SerialName("id") val id: String,
    @SerialName("tag_id") val tagId: String,
    @SerialName("scanned_at") val scannedAt: String
)

@Serializable
data class ScanInsert(
    @SerialName("tag_id") val tagId: String,
    @SerialName("scanned_at") val scannedAt: String
)

@Serializable
data class TagInsert(
    /**
     * Explicit row id, normally null so Postgres generates one. Set when a chore
     * moves out of the private category so it keeps the id its widget pin and
     * memos refer to.
     */
    @SerialName("id") val id: String? = null,
    @SerialName("tag_id") val tagId: String,
    @SerialName("label") val label: String,
    @SerialName("category") val category: String? = null,
    @SerialName("owner") val owner: String? = null,
    @SerialName("interval_days") val intervalDays: Double? = null,
    // Null (the default) is left out of the insert, so a chore without a date
    // saves even before schema.sql has added these columns.
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("repeat_unit") val repeatUnit: String? = null,
    @SerialName("lead_days") val leadDays: Int? = null,
)

enum class ChoreStatus { NEVER, FRESH, AGING, STALE }

/** A logged instant as a date on this phone's calendar. */
private fun Instant.localDate(): LocalDate = atZone(ZoneId.systemDefault()).toLocalDate()

data class Chore(
    val id: String,
    val tagId: String,
    val label: String,
    val category: String?,
    val owner: String?,
    val intervalDays: Double?,
    val archivedAt: String?,
    val lastScanned: Instant?,
    val lastScanId: String?,
    val status: ChoreStatus,
    /** The due date the user set; the chore then falls due on it and every [repeat] after. */
    val dueDate: LocalDate? = null,
    val repeatUnit: RepeatUnit = RepeatUnit.DAY,
    /** This chore's own "hide until N days before due", or null for the automatic rule. */
    val leadDays: Int? = null,
) {
    /** True for a chore in the reserved private category: on this phone only, never in Supabase. */
    val isPrivate: Boolean get() = isPrivateCategory(category)

    /** How often this chore comes round, or null if it has no repeat. */
    val repeat: ChoreRepeat? get() = ChoreRepeat.from(intervalDays, repeatUnit)

    /**
     * The date a dated chore is next due (see [nextChoreDueDate]); null for a
     * chore timed from its last log, and for a one-off date that has been done.
     */
    val nextDueDate: LocalDate?
        get() = dueDate?.let { nextChoreDueDate(it, repeat, lastScanned?.localDate()) }

    /**
     * True when this chore falls due within [days] from now, or is already due.
     * A dated chore counts calendar days; a chore with nothing to be due (never
     * logged and undated, or a done one-off date) counts as within, so it shows.
     */
    fun isDueWithin(days: Int): Boolean {
        if (dueDate != null) return (daysUntilDue() ?: return true) <= days
        val due = dueInstant() ?: return true
        return Duration.between(Instant.now(), due).toDays() <= days
    }

    /** Whole days from today to [nextDueDate]; negative when overdue. */
    private fun daysUntilDue(): Long? =
        nextDueDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    /** Hours until this chore is considered no longer "fresh", per its own thresholds. */
    private fun freshThresholdHours(): Long {
        return if (intervalDays != null) {
            (intervalDays * 0.5).coerceAtLeast(1.0).let { (it * 24).toLong() }
        } else {
            (CATEGORY_FRESH_DAYS[category] ?: 2L) * 24
        }
    }

    /**
     * True if this chore won't become due (stale) for another 60+ days.
     * Matches choreDash web's isDistant(): uses the full interval, not the 50% fresh threshold.
     * Only interval-based chores can be distant; category-based chores have short intervals.
     */
    fun isDistant(): Boolean {
        if (dueDate != null) return (daysUntilDue() ?: return false) > 60
        val last = lastScanned ?: return false
        val fullIntervalHours = intervalDays?.let { (it * 24).toLong() } ?: return false
        val dueInstant = last.plus(fullIntervalHours, ChronoUnit.HOURS)
        return Duration.between(Instant.now(), dueInstant).toDays() > 60
    }

    /**
     * Fraction of this chore's cadence window already elapsed, clamped to 0..1
     * (1 = due or overdue). The window is the full interval for interval-based
     * chores and the category aging threshold otherwise, matching the point at
     * which [computeStatus] turns the chore stale. Null if never scanned.
     */
    fun pressureFraction(): Float? {
        if (dueDate != null) {
            // A done one-off has no pressure left; otherwise the share of the
            // repeat (or the one-off lead-up) already gone.
            val left = daysUntilDue() ?: return 0f
            val window = (repeat?.intervalDays ?: ONE_OFF_EARLY_DAYS.toDouble()).toFloat()
            return (1f - left / window).coerceIn(0f, 1f)
        }
        val last = lastScanned ?: return null
        val hoursSince = ChronoUnit.HOURS.between(last, Instant.now()).toFloat()
        val windowHours = if (intervalDays != null) {
            (intervalDays * 24).toFloat()
        } else {
            ((CATEGORY_AGING_DAYS[category] ?: 5L) * 24).toFloat()
        }
        if (windowHours <= 0f) return 1f
        return (hoursSince / windowHours).coerceIn(0f, 1f)
    }

    /**
     * When this chore falls due: the last log plus the full repeat window (the
     * interval, or the category aging threshold), the same point at which
     * [computeStatus] turns it stale. Null before the first log. For a dated
     * chore, the start of [nextDueDate] (null once a one-off date is done).
     */
    fun dueInstant(): Instant? {
        if (dueDate != null) return nextDueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()
        val last = lastScanned ?: return null
        val windowHours = if (intervalDays != null) {
            (intervalDays * 24).toLong()
        } else {
            (CATEGORY_AGING_DAYS[category] ?: 5L) * 24
        }
        return last.plus(windowHours, ChronoUnit.HOURS)
    }

    /**
     * The card badge: "35d over", "1d left", "6h left", "due now", or "never"
     * before the first log. Counted against [dueInstant], so the words agree with
     * the spine colour and the Overdue filter. A dated chore counts whole days
     * ("due today", "9d left") and reads "done" once a one-off date is ticked off.
     */
    fun dueBadgeText(): String {
        if (dueDate != null) {
            // Counted in calendar days: a date is due all day, not from midnight on.
            val left = daysUntilDue() ?: return "done"
            return when {
                left < 0 -> "${-left}d over"
                left == 0L -> "due today"
                else -> "${left}d left"
            }
        }
        val due = dueInstant() ?: return "never"
        val diff = Duration.between(Instant.now(), due)
        val overdue = diff.isNegative
        val hours = diff.abs().toHours()
        val days = hours / 24
        return when {
            overdue && days >= 1 -> "${days}d over"
            overdue && hours >= 1 -> "${hours}h over"
            overdue -> "due now"
            days >= 1 -> "${days}d left"
            hours >= 1 -> "${hours}h left"
            else -> "due now"
        }
    }

    /**
     * Countdown text matching choreDash web's nextDueText(), e.g. "in 2d", "in 5h",
     * "1d overdue", "3h overdue". Returns null if never scanned.
     */
    fun nextDueText(): String? {
        val last = lastScanned ?: return null
        val due = last.plus(freshThresholdHours(), ChronoUnit.HOURS)
        val diff = Duration.between(Instant.now(), due)
        val overdue = diff.isNegative
        val absDiff = diff.abs()
        val hours = absDiff.toHours()
        val days = hours / 24
        return when {
            overdue && days >= 1 -> "${days}d overdue"
            overdue -> "${hours}h overdue"
            days >= 1 -> "in ${days}d"
            else -> "in ${hours}h"
        }
    }

    companion object {
        private val CATEGORY_FRESH_DAYS = mapOf(
            "Laundry" to 3L,
            "Cleaning" to 2L,
            "Kitchen" to 2L,
            "Outside" to 2L,
            "Plants" to 2L
        )
        private val CATEGORY_AGING_DAYS = mapOf(
            "Laundry" to 7L,
            "Cleaning" to 5L,
            "Kitchen" to 5L,
            "Outside" to 5L,
            "Plants" to 5L
        )

        fun from(tag: TagDto, lastScanned: Instant?, lastScanId: String?): Chore {
            val dueDate = tag.dueDate?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            val repeatUnit = RepeatUnit.fromWire(tag.repeatUnit)
            val status = if (dueDate != null) {
                val repeat = ChoreRepeat.from(tag.intervalDays, repeatUnit)
                datedStatus(nextChoreDueDate(dueDate, repeat, lastScanned?.localDate()), repeat)
            } else {
                computeStatus(tag.category, tag.intervalDays, lastScanned)
            }
            return Chore(
                id = tag.id,
                tagId = tag.tagId,
                label = tag.label,
                category = tag.category,
                owner = tag.owner,
                intervalDays = tag.intervalDays,
                archivedAt = tag.archivedAt,
                lastScanned = lastScanned,
                lastScanId = lastScanId,
                status = status,
                dueDate = dueDate,
                repeatUnit = repeatUnit,
                leadDays = tag.leadDays?.takeIf { it >= 0 },
            )
        }

        /**
         * Status for a dated chore: stale from the due date on, soon within
         * [dueSoonDays] of it, fresh before that and once a one-off date is done.
         */
        private fun datedStatus(nextDue: LocalDate?, repeat: ChoreRepeat?): ChoreStatus {
            val left = nextDue?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) } ?: return ChoreStatus.FRESH
            return when {
                left <= 0 -> ChoreStatus.STALE
                left <= dueSoonDays(repeat) -> ChoreStatus.AGING
                else -> ChoreStatus.FRESH
            }
        }

        private fun computeStatus(
            category: String?,
            intervalDays: Double?,
            lastScanned: Instant?
        ): ChoreStatus {
            if (lastScanned == null) return ChoreStatus.NEVER
            val daysSince = ChronoUnit.DAYS.between(lastScanned, Instant.now())

            return if (intervalDays != null) {
                val interval = intervalDays.toLong()
                val freshThreshold = (interval * 0.5).toLong().coerceAtLeast(1)
                when {
                    daysSince <= freshThreshold -> ChoreStatus.FRESH
                    daysSince < interval -> ChoreStatus.AGING
                    else -> ChoreStatus.STALE
                }
            } else {
                val freshDays = CATEGORY_FRESH_DAYS[category] ?: 2L
                val agingDays = CATEGORY_AGING_DAYS[category] ?: 5L
                when {
                    daysSince <= freshDays -> ChoreStatus.FRESH
                    daysSince <= agingDays -> ChoreStatus.AGING
                    else -> ChoreStatus.STALE
                }
            }
        }
    }
}
