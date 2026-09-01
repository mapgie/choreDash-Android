package com.mapgie.dash.data.model

import java.time.Duration

/**
 * How long a swipe-to-snooze hides a chore. With smart visibility on, the chore
 * sleeps for its cadence bucket's lead time, the same window that decides when
 * it would come back into view anyway. Otherwise, and whenever that lead time is
 * zero or the chore has no repeat interval, it sleeps for one day.
 */
fun defaultSnoozeDuration(
    intervalDays: Double?,
    smartVisibility: Boolean,
    choreLeadDays: Map<CadenceBucket, Int>,
): Duration {
    if (smartVisibility && intervalDays != null) {
        val bucket = CadenceBucket.forInterval(intervalDays)
        val leadDays = choreLeadDays[bucket] ?: bucket.defaultLeadDays
        if (leadDays >= 1) return Duration.ofDays(leadDays.toLong())
    }
    return Duration.ofDays(1)
}
