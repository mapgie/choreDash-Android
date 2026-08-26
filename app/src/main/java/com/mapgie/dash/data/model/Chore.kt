package com.mapgie.dash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Serializable
data class TagDto(
    @SerialName("id") val id: String,
    @SerialName("tag_id") val tagId: String,
    @SerialName("label") val label: String,
    @SerialName("category") val category: String? = null,
    @SerialName("owner") val owner: String? = null,
    @SerialName("interval_days") val intervalDays: Double? = null,
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
    @SerialName("tag_id") val tagId: String,
    @SerialName("label") val label: String,
    @SerialName("category") val category: String? = null,
    @SerialName("owner") val owner: String? = null,
    @SerialName("interval_days") val intervalDays: Double? = null
)

enum class ChoreStatus { NEVER, FRESH, AGING, STALE }

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
    val status: ChoreStatus
) {
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
            val status = computeStatus(tag.category, tag.intervalDays, lastScanned)
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
                status = status
            )
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
