package com.mapgie.dash.data.model

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * A tag-alarm is the third kind of memo, for someone whose mornings follow no
 * fixed pattern: a dormant alarm with a linked NFC tag and a "morning" (a first
 * ring plus follow-ups later the same day). A tap on its tag, or Set for next in
 * the app, arms exactly one morning: the next time the first ring time comes
 * round, today if it is still ahead and tomorrow otherwise, weekday never
 * consulted. The rings of that morning play out one after another through
 * [ReminderDto.remindAt] (each ring arms the next follow-up), and after the last
 * one the alarm is dormant again until the next tap.
 *
 * A tap never removes a ring, so tapping is idempotent: an alarm already armed
 * with a ring still ahead is left exactly as it is.
 *
 * Everything here is plain Kotlin so `TagAlarmModelTest` can pin every rule.
 */

/** The most rings one morning can hold: the first ring plus five follow-ups. */
const val MAX_TAG_ALARM_RINGS = 6

private val RING_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

val ReminderDto.isTagAlarm: Boolean
    get() = tagAlarm

/** "05:15", the form [ReminderDto.ringTimes] stores. */
fun formatRingTime(time: LocalTime): String = RING_TIME_FORMAT.format(time)

/** Parses stored ring times, dropping garbage and duplicates, sorted earliest first. */
fun parseRingTimes(raw: List<String>): List<LocalTime> =
    raw.mapNotNull { runCatching { LocalTime.parse(it) }.getOrNull() }
        .map { it.withSecond(0).withNano(0) }
        .distinct()
        .sorted()

/** This tag-alarm's morning, earliest ring first. Empty on any other kind of memo. */
fun ReminderDto.ringTimeList(): List<LocalTime> = parseRingTimes(ringTimes)

/** The ring a tap arms: the earliest time of the morning. */
fun ReminderDto.firstRingTime(): LocalTime? = ringTimeList().firstOrNull()

/**
 * The next morning whose first ring is strictly after [now]: today when [firstRing]
 * is still ahead, otherwise tomorrow. The weekday plays no part.
 */
fun nextMorning(now: Instant, firstRing: LocalTime, zone: ZoneId): Instant {
    val today = now.atZone(zone).toLocalDate()
    val todayAt = ZonedDateTime.of(today, firstRing, zone).toInstant()
    return if (todayAt.isAfter(now)) todayAt else ZonedDateTime.of(today.plusDays(1), firstRing, zone).toInstant()
}

/**
 * The record after a tap on its tag (or Set for next) at [now]. Arms the next
 * morning, unless a ring is already armed and still ahead: tomorrow's first ring
 * from an earlier tap, or a follow-up of a morning in progress. A tap never
 * removes a ring, so those are returned unchanged.
 */
fun ReminderDto.armedForNextMorning(now: Instant, zone: ZoneId = ZoneId.systemDefault()): ReminderDto {
    val first = firstRingTime() ?: return this
    val current = remindAtInstant()
    if (armed && current != null && current.isAfter(now)) return this
    return copy(remindAt = nextMorning(now, first, zone).toString(), armed = true, reminded = false, completedAt = null)
}

/**
 * An armed tag-alarm after its ring times were edited: stays on the morning it
 * was armed for, moved to the first of the new times still ahead on that date.
 * When that morning has no ring left ahead, it arms the next morning instead:
 * a ring nobody needed beats a morning nobody was woken for.
 */
fun ReminderDto.realigned(now: Instant, zone: ZoneId = ZoneId.systemDefault()): ReminderDto {
    val first = firstRingTime() ?: return copy(armed = false)
    val current = remindAtInstant() ?: return copy(remindAt = nextMorning(now, first, zone).toString(), armed = true)
    val date = current.atZone(zone).toLocalDate()
    val onThatDate = ringTimeList()
        .map { ZonedDateTime.of(date, it, zone).toInstant() }
        .firstOrNull { it.isAfter(now) }
    val next = onThatDate ?: nextMorning(now, first, zone)
    return copy(remindAt = next.toString(), armed = true, reminded = false, completedAt = null)
}

/** The record after Turn off / Stop for today: dormant, its morning over. The caller clears the alarm. */
fun ReminderDto.disarmed(): ReminderDto = copy(armed = false)

/**
 * The first ring time later than [ring]'s local time on the same date, or null
 * when [ring] was the last of its morning.
 */
fun ReminderDto.nextFollowUp(ring: Instant, zone: ZoneId = ZoneId.systemDefault()): Instant? {
    val zoned = ring.atZone(zone)
    val time = zoned.toLocalTime()
    val next = ringTimeList().firstOrNull { it.isAfter(time) } ?: return null
    return ZonedDateTime.of(zoned.toLocalDate(), next, zone).toInstant()
}

/**
 * The record after a tag-alarm rings at [now]: the next follow-up of the morning
 * is armed, or the alarm goes dormant after the last one. A snoozed re-ring
 * arrives while [ReminderDto.remindAt] already points at the next follow-up;
 * that one is kept rather than skipped.
 */
fun ReminderDto.afterTagAlarmRing(now: Instant, zone: ZoneId = ZoneId.systemDefault()): ReminderDto {
    val rang = now.toString()
    val current = remindAtInstant() ?: return copy(armed = false, reminded = false, lastRangAt = rang)
    if (current.isAfter(now)) return copy(reminded = false, lastRangAt = rang)
    val next = nextFollowUp(current, zone)
    return if (next != null) copy(remindAt = next.toString(), armed = true, reminded = false, lastRangAt = rang)
    else copy(armed = false, reminded = false, lastRangAt = rang)
}

/**
 * How many rings of the morning follow the one in [ReminderDto.remindAt]: for
 * the card's "+2 follow-ups". A dormant alarm counts everything after its first ring.
 */
fun ReminderDto.followUpsRemaining(zone: ZoneId = ZoneId.systemDefault()): Int {
    val times = ringTimeList()
    if (times.isEmpty()) return 0
    val pending = if (armed) remindAtInstant()?.atZone(zone)?.toLocalTime() ?: times.first() else times.first()
    return times.count { it.isAfter(pending) }
}

/**
 * The other armed tag-alarms set for the same morning as this one: the ones the
 * "Turn off?" question after a tap is about. An alarm armed for a different date
 * (say a morning still playing out today while this one is set for tomorrow) is
 * not a conflict.
 */
fun ReminderDto.conflictingTagAlarms(all: List<ReminderDto>, zone: ZoneId = ZoneId.systemDefault()): List<ReminderDto> {
    val date = remindAtInstant()?.atZone(zone)?.toLocalDate() ?: return emptyList()
    return all.filter { other ->
        other.id != id && other.isTagAlarm && other.armed && other.archivedAt == null &&
            other.remindAtInstant()?.atZone(zone)?.toLocalDate() == date
    }
}

/**
 * A friendly tag id for a tag-alarm, from its name: "Office A" becomes
 * "office-a". Lower-case ASCII letters, digits and single hyphens only, so it
 * reads well on a card and travels safely inside the `chordash://memo?memo=`
 * URI; at most 40 characters; "memo" when nothing usable is left.
 */
fun suggestTagId(subject: String): String {
    val slug = subject.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(40)
        .trimEnd('-')
    return slug.ifEmpty { "memo" }
}

/** [suggestTagId], made unique against [taken] by a numeric suffix: "office-a-2". */
fun freeTagId(subject: String, taken: Set<String>): String {
    val base = suggestTagId(subject)
    if (base !in taken) return base
    var n = 2
    while ("$base-$n" in taken) n++
    return "$base-$n"
}

/** The words the tap feedback and the conflict question use, kept testable. */
object TagAlarmText {

    /** The toast after a tap: "Office A: tomorrow 5:15 AM". */
    fun armedToast(alarm: ReminderDto, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        val at = alarm.remindAtInstant() ?: return alarm.subject
        return "${alarm.subject}: ${ReminderScheduleText.whenLabel(at, now, zone)}"
    }

    /**
     * "Office B is also set for tomorrow 7:15 AM. Turn it off?", or for several
     * "Office B and Home are also set for tomorrow. Turn them off?".
     */
    fun conflictQuestion(conflicts: List<ReminderDto>, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        if (conflicts.isEmpty()) return ""
        if (conflicts.size == 1) {
            val only = conflicts.first()
            val at = only.remindAtInstant()
            val setFor = if (at == null) "" else " for ${ReminderScheduleText.whenLabel(at, now, zone)}"
            return "${only.subject} is also set$setFor. Turn it off?"
        }
        val names = conflicts.dropLast(1).joinToString(", ") { it.subject } + " and " + conflicts.last().subject
        val day = conflicts.first().remindAtInstant()?.let { dayWord(it, now, zone) } ?: ""
        return "$names are also set$day. Turn them off?"
    }

    private fun dayWord(at: Instant, now: Instant, zone: ZoneId): String {
        val today = now.atZone(zone).toLocalDate()
        val day = at.atZone(zone).toLocalDate()
        return when (day) {
            today -> " for today"
            today.plusDays(1) -> " for tomorrow"
            else -> ""
        }
    }
}
