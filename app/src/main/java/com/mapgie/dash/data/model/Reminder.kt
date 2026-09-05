package com.mapgie.dash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * A memo (the user may call it a reminder or an alarm): a scheduled ring with an
 * optional day-of-week repeat, stored on-device by ReminderRepository.
 *
 * [remindAt] is always the *next* ring. A once-only memo keeps it fixed, is
 * marked [reminded] when it fires, and is then done: you set it, it rings, you
 * snooze or dismiss. A repeating memo advances it to the next chosen weekday at
 * the same local time each time it rings, so it is never "reminded" and never
 * "completed"; it retires only through archiving.
 */
@Serializable
data class ReminderDto(
    @SerialName("id") val id: String,
    @SerialName("subject") val subject: String,
    /** The next ring, as an ISO instant. */
    @SerialName("remind_at") val remindAt: String,
    @SerialName("chore_id") val choreId: String? = null,
    @SerialName("task_id") val taskId: String? = null,
    /** Set when a once-only memo is marked Done. Never set on a repeating memo. */
    @SerialName("completed_at") val completedAt: String? = null,
    /** True once a once-only memo has rung. Always false on a repeating memo. */
    @SerialName("reminded") val reminded: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("archived_at") val archivedAt: String? = null,
    /** Weekdays this memo rings on, as [DayOfWeek] names. Empty means it rings once. */
    @SerialName("repeat_days") val repeatDays: List<String> = emptyList(),
    /** When the memo last rang; drives the "rang 2h ago" badge. */
    @SerialName("last_rang_at") val lastRangAt: String? = null,
    /** Kept for records written by 0.31.0; no longer read. */
    @SerialName("acknowledged_at") val acknowledgedAt: String? = null,
    /**
     * The ringtone this memo rings with on the Alarm delivery style, as a content
     * URI string; null means the device's default alarm tone. The Notification
     * style plays its channel's sound regardless (Android fixes channel sounds).
     */
    @SerialName("sound") val sound: String? = null,
)

@Serializable
data class ReminderInsert(
    @SerialName("subject") val subject: String,
    @SerialName("remind_at") val remindAt: String,
    @SerialName("chore_id") val choreId: String? = null,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("repeat_days") val repeatDays: List<String> = emptyList(),
    @SerialName("sound") val sound: String? = null,
)

private fun parseInstant(raw: String?): Instant? =
    raw?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun ReminderDto.remindAtInstant(): Instant? = parseInstant(remindAt)

fun ReminderDto.lastRangInstant(): Instant? = parseInstant(lastRangAt)

fun ReminderDto.isPast(): Boolean =
    remindAtInstant()?.isBefore(Instant.now()) ?: false

/** The weekdays this memo repeats on; unknown names are ignored. */
fun ReminderDto.repeatDaySet(): Set<DayOfWeek> = parseRepeatDays(repeatDays)

fun parseRepeatDays(names: List<String>): Set<DayOfWeek> =
    names.mapNotNull { name -> runCatching { DayOfWeek.valueOf(name) }.getOrNull() }.toSet()

/** True when this memo rings on a weekly schedule rather than once. */
val ReminderDto.repeats: Boolean
    get() = repeatDaySet().isNotEmpty()

/** The local wall-clock time this memo rings at, read off its next ring. */
fun ReminderDto.timeOfDay(zone: ZoneId = ZoneId.systemDefault()): LocalTime? =
    remindAtInstant()?.atZone(zone)?.toLocalTime()

// True when this reminder still needs an alarm or immediate delivery: never shown,
// not completed, not archived, and carries a parseable fire time. Past-due entries
// are included deliberately — a reminder that came due while the device was off is
// still pending, and BootWorker decides between scheduling and immediate delivery.
// A repeating memo is never "reminded" or "completed", so it stays pending until archived.
fun ReminderDto.needsScheduling(): Boolean =
    !reminded && completedAt == null && archivedAt == null && remindAtInstant() != null

/**
 * True once a once-only memo has had its moment: it rang, or it was marked done
 * from the notification or ring screen. A repeating memo is never done.
 */
val ReminderDto.isDone: Boolean
    get() = !repeats && (reminded || completedAt != null)

/** When a once-only memo rang, for the Done badge; legacy records fall back to their fire time. */
fun ReminderDto.rangAt(): Instant? =
    if (!repeats && reminded) lastRangInstant() ?: remindAtInstant() else null

/**
 * The first instant strictly after [after] that falls on one of [days] at
 * [timeOfDay] in [zone], or null when [days] is empty. Walks at most eight
 * days, so a schedule with any day set always resolves.
 */
fun nextOccurrence(after: Instant, timeOfDay: LocalTime, days: Set<DayOfWeek>, zone: ZoneId): Instant? {
    if (days.isEmpty()) return null
    var date = after.atZone(zone).toLocalDate()
    repeat(8) {
        if (date.dayOfWeek in days) {
            val candidate = ZonedDateTime.of(date, timeOfDay, zone).toInstant()
            if (candidate.isAfter(after)) return candidate
        }
        date = date.plusDays(1)
    }
    return null
}

/** The next ring of a repeating memo strictly after [after], keeping its local time of day. */
fun ReminderDto.nextOccurrenceAfter(after: Instant, zone: ZoneId = ZoneId.systemDefault()): Instant? {
    val time = timeOfDay(zone) ?: return null
    return nextOccurrence(after, time, repeatDaySet(), zone)
}

/**
 * A repeating memo whose stored ring is in the past, or on a day it no longer
 * repeats on, is moved to its next valid occurrence after [now]. Once-only
 * memos are returned unchanged. Applied on every save.
 */
fun ReminderDto.withScheduleAligned(now: Instant, zone: ZoneId = ZoneId.systemDefault()): ReminderDto {
    if (!repeats) return this
    val current = remindAtInstant() ?: return this
    val aligned = current.isAfter(now) && current.atZone(zone).dayOfWeek in repeatDaySet()
    if (aligned) return this
    val next = nextOccurrenceAfter(now, zone) ?: return this
    return copy(remindAt = next.toString())
}

/**
 * The record after it rings at [now]. A once-only memo is marked reminded, which
 * makes it done. A repeating memo records the ring and arms the next occurrence.
 * A snoozed re-ring arrives while [remindAt] already points at the next
 * occurrence; that one is kept rather than advanced a second time.
 */
fun ReminderDto.afterRing(now: Instant, zone: ZoneId = ZoneId.systemDefault()): ReminderDto {
    val rang = now.toString()
    if (!repeats) return copy(reminded = true, lastRangAt = rang)
    val current = remindAtInstant() ?: return copy(reminded = true, lastRangAt = rang)
    val next = if (current.isAfter(now)) current else nextOccurrenceAfter(now, zone) ?: current
    return copy(remindAt = next.toString(), reminded = false, lastRangAt = rang)
}

/**
 * The record after Done on the notification or ring screen at [now]. A once-only
 * memo completes (it is also done once it has rung). A repeating memo has
 * nothing to record: Done just dismisses this ring, and the next one stays armed.
 */
fun ReminderDto.afterDone(now: Instant): ReminderDto =
    if (!repeats) copy(completedAt = now.toString()) else this
