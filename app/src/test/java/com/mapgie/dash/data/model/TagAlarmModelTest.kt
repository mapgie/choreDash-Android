package com.mapgie.dash.data.model

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules of a tag-alarm, the memo a tap on its NFC tag arms for one morning:
 * which morning a tap picks (the next time the first ring comes round, weekday
 * ignored), that a tap never removes a ring, how a morning's follow-ups play
 * out, and when the alarm goes dormant. Pure Kotlin, fixed clock, fixed zone.
 * Tap times sit well clear of the ring times so nothing depends on the run.
 */
class TagAlarmModelTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    // Mon 6 Jul 2026 .. Sun 12 Jul 2026, all UTC.
    private val tue0500 = Instant.parse("2026-07-07T05:00:00Z")
    private val tue2200 = Instant.parse("2026-07-07T22:00:00Z")
    private val sat2200 = Instant.parse("2026-07-11T22:00:00Z")

    private fun alarm(
        ringTimes: List<String> = listOf("06:30"),
        remindAt: String = "2026-07-01T06:30:00Z",
        armed: Boolean = false,
        tagId: String? = "bedside",
        id: String = "office-a",
        archivedAt: String? = null,
    ) = ReminderDto(
        id = id, subject = id, remindAt = remindAt, tagAlarm = true, tagId = tagId,
        ringTimes = ringTimes, armed = armed, archivedAt = archivedAt,
    )

    private val morning = listOf("05:15", "05:30", "05:45")

    // ── Which morning a tap arms ──────────────────────────────────────────────

    @Test
    fun `armed at Tue 22-00 with first time 06-30 rings Wed 06-30`() {
        val armed = alarm().armedForNextMorning(tue2200, zone)
        assertTrue(armed.armed)
        assertEquals("2026-07-08T06:30:00Z", armed.remindAt)
    }

    @Test
    fun `armed at Tue 05-00 with first time 06-30 rings Tue 06-30`() {
        assertEquals("2026-07-07T06:30:00Z", alarm().armedForNextMorning(tue0500, zone).remindAt)
    }

    @Test
    fun `armed at Sat 22-00 rings Sun, weekday ignored`() {
        assertEquals("2026-07-12T06:30:00Z", alarm().armedForNextMorning(sat2200, zone).remindAt)
    }

    @Test
    fun `a tap arms the first ring of the morning, not a later one`() {
        // 05:20: the first ring has passed, so the tap means tomorrow's morning.
        val tap = Instant.parse("2026-07-07T05:20:00Z")
        assertEquals("2026-07-08T05:15:00Z", alarm(ringTimes = morning).armedForNextMorning(tap, zone).remindAt)
    }

    @Test
    fun `arming clears a stale reminded or done mark`() {
        val stale = alarm().copy(reminded = true, completedAt = "2026-07-01T07:00:00Z")
        val armed = stale.armedForNextMorning(tue2200, zone)
        assertFalse(armed.reminded)
        assertNull(armed.completedAt)
        assertTrue(armed.needsScheduling())
    }

    // ── A tap never removes a ring ────────────────────────────────────────────

    @Test
    fun `arming twice yields identical rings`() {
        val once = alarm().armedForNextMorning(tue2200, zone)
        val twice = once.armedForNextMorning(tue2200.plusSeconds(600), zone)
        assertEquals(once, twice)
    }

    @Test
    fun `arming during a running morning keeps pending follow-ups`() {
        // 05:15 rang; 05:30 is armed. A tap at 05:20 must not push the morning to tomorrow.
        val running = alarm(ringTimes = morning, remindAt = "2026-07-07T05:30:00Z", armed = true)
        val tap = Instant.parse("2026-07-07T05:20:00Z")
        assertEquals(running, running.armedForNextMorning(tap, zone))
    }

    @Test
    fun `an armed alarm whose ring was missed is re-armed by a tap`() {
        // Phone was off through Tuesday's ring; a tap Tuesday night arms Wednesday.
        val missed = alarm(remindAt = "2026-07-07T06:30:00Z", armed = true)
        assertEquals("2026-07-08T06:30:00Z", missed.armedForNextMorning(tue2200, zone).remindAt)
    }

    // ── The morning plays out ─────────────────────────────────────────────────

    @Test
    fun `follow-ups ring on the same date as the first ring`() {
        val first = alarm(ringTimes = morning, remindAt = "2026-07-08T05:15:00Z", armed = true)
        val afterFirst = first.afterRing(Instant.parse("2026-07-08T05:15:01Z"), zone)
        assertTrue(afterFirst.armed)
        assertEquals("2026-07-08T05:30:00Z", afterFirst.remindAt)
        val afterSecond = afterFirst.afterRing(Instant.parse("2026-07-08T05:30:01Z"), zone)
        assertEquals("2026-07-08T05:45:00Z", afterSecond.remindAt)
    }

    @Test
    fun `last ring dismissed leaves the alarm dormant`() {
        val last = alarm(ringTimes = morning, remindAt = "2026-07-08T05:45:00Z", armed = true)
        val rang = last.afterRing(Instant.parse("2026-07-08T05:45:01Z"), zone)
        assertFalse(rang.armed)
        assertFalse(rang.needsScheduling())
        assertFalse(rang.isDone)
        assertEquals("2026-07-08T05:45:01Z", rang.lastRangAt)
        // A morning with no follow-ups is over after its one ring.
        assertFalse(alarm(remindAt = "2026-07-08T06:30:00Z", armed = true).afterRing(Instant.parse("2026-07-08T06:30:01Z"), zone).armed)
    }

    @Test
    fun `a snoozed re-ring keeps the follow-up that is already armed`() {
        // 05:15 rang and was snoozed; the record already points at 05:30. When the
        // snooze fires at 05:25 the 05:30 ring must not be skipped.
        val snoozed = alarm(ringTimes = morning, remindAt = "2026-07-08T05:30:00Z", armed = true)
        val rang = snoozed.afterRing(Instant.parse("2026-07-08T05:25:00Z"), zone)
        assertEquals("2026-07-08T05:30:00Z", rang.remindAt)
        assertTrue(rang.armed)
    }

    @Test
    fun `done from the alert dismisses this ring and keeps the follow-up armed`() {
        val running = alarm(ringTimes = morning, remindAt = "2026-07-08T05:30:00Z", armed = true)
        assertEquals(running, running.afterDone(Instant.parse("2026-07-08T05:16:00Z")))
        assertTrue(running.needsScheduling())
    }

    @Test
    fun `turn off cancels every pending ring`() {
        val running = alarm(ringTimes = morning, remindAt = "2026-07-08T05:30:00Z", armed = true)
        val off = running.disarmed()
        assertFalse(off.armed)
        assertFalse(off.needsScheduling())
    }

    // ── Dormant state ─────────────────────────────────────────────────────────

    @Test
    fun `dormant tag-alarm needs no scheduling and is never done`() {
        val dormant = alarm(remindAt = "2020-01-01T06:30:00Z")
        assertFalse(dormant.needsScheduling())
        assertFalse(dormant.isDone)
        assertFalse(dormant.repeats)
    }

    @Test
    fun `dormant tag-alarm is off on the card, an armed one shows its ring`() {
        val now = tue2200
        val dormant = alarm()
        assertEquals(ReminderStatus.OFF, ReminderScheduleText.status(dormant, now))
        assertEquals("off", ReminderScheduleText.nextRingBadge(dormant, now, zone))
        val armed = dormant.armedForNextMorning(now, zone)
        assertEquals(ReminderStatus.DUE_SOON, ReminderScheduleText.status(armed, now))
        assertEquals("tomorrow 6:30 AM", ReminderScheduleText.nextRingBadge(armed, now, zone))
    }

    @Test
    fun `the card's schedule line lists the morning`() {
        assertEquals(
            "Tag-alarm · 5:15 AM · 5:30 AM · 5:45 AM",
            ReminderScheduleText.scheduleLine(alarm(ringTimes = morning), zone),
        )
        assertEquals("Tag-alarm · no time set", ReminderScheduleText.scheduleLine(alarm(ringTimes = emptyList()), zone))
    }

    @Test
    fun `follow-ups remaining counts what is still to come`() {
        assertEquals(2, alarm(ringTimes = morning).followUpsRemaining(zone))
        assertEquals(1, alarm(ringTimes = morning, remindAt = "2026-07-08T05:30:00Z", armed = true).followUpsRemaining(zone))
        assertEquals(0, alarm(ringTimes = morning, remindAt = "2026-07-08T05:45:00Z", armed = true).followUpsRemaining(zone))
    }

    // ── Saving ────────────────────────────────────────────────────────────────

    @Test
    fun `saving a dormant tag-alarm leaves it dormant`() {
        val saved = alarm(ringTimes = morning).withScheduleAligned(tue2200, zone)
        assertFalse(saved.armed)
    }

    @Test
    fun `editing the times of an armed alarm keeps its morning and takes the new first time`() {
        val armed = alarm(ringTimes = listOf("06:30"), remindAt = "2026-07-08T06:30:00Z", armed = true)
        val edited = armed.copy(ringTimes = listOf("06:45", "07:00")).withScheduleAligned(tue2200, zone)
        assertTrue(edited.armed)
        assertEquals("2026-07-08T06:45:00Z", edited.remindAt)
    }

    @Test
    fun `editing an armed alarm past its morning arms the next one rather than none`() {
        // Armed for Tue 06:30, edited Tue night to 06:00: Tuesday has no 06:00 left, so Wednesday.
        val armed = alarm(ringTimes = listOf("06:30"), remindAt = "2026-07-07T06:30:00Z", armed = true)
        val edited = armed.copy(ringTimes = listOf("06:00")).withScheduleAligned(tue2200, zone)
        assertEquals("2026-07-08T06:00:00Z", edited.remindAt)
    }

    @Test
    fun `ring times parse sorted, deduplicated, and without garbage`() {
        assertEquals(
            listOf(LocalTime.of(5, 15), LocalTime.of(5, 30)),
            parseRingTimes(listOf("05:30", "05:15", "05:30", "half five")),
        )
        assertEquals("05:15", formatRingTime(LocalTime.of(5, 15)))
    }

    // ── Conflicts and words ───────────────────────────────────────────────────

    @Test
    fun `other tag-alarms armed for the same morning are conflicts, others are not`() {
        val now = tue2200
        val officeA = alarm(id = "Office A").armedForNextMorning(now, zone)
        val officeB = alarm(id = "Office B", ringTimes = listOf("07:15"), tagId = "b").armedForNextMorning(now, zone)
        val dormantHome = alarm(id = "Home", tagId = "h")
        // A morning still playing out today is not tomorrow's problem.
        val runningToday = alarm(id = "Today", tagId = "t", ringTimes = morning, remindAt = "2026-07-07T23:30:00Z", armed = true)
        val archived = alarm(id = "Old", tagId = "o", archivedAt = "2026-07-01T00:00:00Z").armedForNextMorning(now, zone)
        val plainMemo = ReminderDto(id = "memo", subject = "memo", remindAt = "2026-07-08T09:00:00Z")
        val all = listOf(officeA, officeB, dormantHome, runningToday, archived, plainMemo)
        assertEquals(listOf("Office B"), officeA.conflictingTagAlarms(all, zone).map { it.id })
    }

    @Test
    fun `the toast names the alarm and its first ring`() {
        val armed = alarm(id = "Office A").armedForNextMorning(tue2200, zone)
        assertEquals("Office A: tomorrow 6:30 AM", TagAlarmText.armedToast(armed, tue2200, zone))
    }

    @Test
    fun `the conflict question names the others and when they are set`() {
        val b = alarm(id = "Office B", ringTimes = listOf("07:15")).armedForNextMorning(tue2200, zone)
        val h = alarm(id = "Home", ringTimes = listOf("08:00")).armedForNextMorning(tue2200, zone)
        assertEquals("Office B is also set for tomorrow 7:15 AM. Turn it off?", TagAlarmText.conflictQuestion(listOf(b), tue2200, zone))
        assertEquals("Office B and Home are also set for tomorrow. Turn them off?", TagAlarmText.conflictQuestion(listOf(b, h), tue2200, zone))
        assertEquals("", TagAlarmText.conflictQuestion(emptyList(), tue2200, zone))
    }

    @Test
    fun `a plain memo is untouched by the tag-alarm rules`() {
        val memo = ReminderDto(id = "memo", subject = "memo", remindAt = "2026-07-08T09:00:00Z")
        assertFalse(memo.isTagAlarm)
        assertEquals(memo, memo.armedForNextMorning(tue2200, zone))
        assertTrue(memo.needsScheduling())
    }
}
