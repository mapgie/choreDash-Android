package com.mapgie.dash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
