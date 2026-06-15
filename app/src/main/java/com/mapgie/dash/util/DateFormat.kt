package com.mapgie.dash.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val sameYearFormatter = DateTimeFormatter.ofPattern("d MMM").withZone(ZoneId.systemDefault())
private val otherYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())

/** Absolute date, e.g. "15 Apr" or "1 Apr 2025" when not the current year. */
fun formatAbsoluteDate(instant: Instant): String {
    val zone = ZoneId.systemDefault()
    val year = instant.atZone(zone).year
    val currentYear = ZonedDateTime.now(zone).year
    return if (year != currentYear) otherYearFormatter.format(instant) else sameYearFormatter.format(instant)
}

/** Relative time, e.g. "just now", "5m ago", "3h ago", "2d ago", "8w ago". */
fun relativeTime(instant: Instant): String {
    val now = Instant.now()
    val minutes = ChronoUnit.MINUTES.between(instant, now)
    if (minutes < 2) return "just now"
    if (minutes < 60) return "${minutes}m ago"
    val hours = ChronoUnit.HOURS.between(instant, now)
    if (hours < 24) return "${hours}h ago"
    val days = ChronoUnit.DAYS.between(instant, now)
    if (days < 7) return "${days}d ago"
    return "${days / 7}w ago"
}
