package com.mapgie.dash.data.model

/**
 * User-facing cadence categories for smart chore visibility. Every repeat interval maps
 * to exactly one bucket; each bucket has a configurable "lead time" (days before due)
 * after which a chore becomes visible again. Defaults encode the adaptive rule that a
 * chore should reappear in the last stretch of its cycle: an every-2-days chore hides
 * on its off day, a monthly chore surfaces about a week out.
 */
enum class CadenceBucket(
    val label: String,
    val example: String,
    val defaultLeadDays: Int,
) {
    DAILY("Daily", "repeats every day", 1),
    FEW_DAYS("Every few days", "repeats every 2 to 4 days", 1),
    WEEKLY("Weekly", "repeats every 5 to 10 days", 3),
    FORTNIGHTLY("Fortnightly", "repeats every 11 to 20 days", 5),
    MONTHLY("Monthly and longer", "repeats every 3+ weeks", 7);

    companion object {
        /**
         * Maps a raw repeat interval in days to its bucket. Boundaries are half-open and
         * exhaustive over all positive values; zero or negative intervals count as daily.
         */
        fun forInterval(intervalDays: Double): CadenceBucket = when {
            intervalDays <= 1.0 -> DAILY
            intervalDays <= 4.0 -> FEW_DAYS
            intervalDays <= 10.0 -> WEEKLY
            intervalDays <= 20.0 -> FORTNIGHTLY
            else -> MONTHLY
        }
    }
}
