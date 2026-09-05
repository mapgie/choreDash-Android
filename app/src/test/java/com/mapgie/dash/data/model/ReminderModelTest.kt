package com.mapgie.dash.data.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderModelTest {

    private fun reminder(
        remindAt: String = "2026-07-10T09:00:00Z",
        reminded: Boolean = false,
        completedAt: String? = null,
        archivedAt: String? = null,
        repeatDays: List<String> = emptyList(),
        lastRangAt: String? = null,
        acknowledgedAt: String? = null,
    ) = ReminderDto(
        id = "r1",
        subject = "subject",
        remindAt = remindAt,
        reminded = reminded,
        completedAt = completedAt,
        archivedAt = archivedAt,
        repeatDays = repeatDays,
        lastRangAt = lastRangAt,
        acknowledgedAt = acknowledgedAt,
    )

    private val zone: ZoneId = ZoneId.of("UTC")
    // Friday 10 July 2026, 09:30.
    private val now: Instant = Instant.parse("2026-07-10T09:30:00Z")
    private val monWedFri = listOf("MONDAY", "WEDNESDAY", "FRIDAY")

    @Test
    fun `remindAtInstant parses ISO instants`() {
        assertEquals(Instant.parse("2026-07-10T09:00:00Z"), reminder().remindAtInstant())
    }

    @Test
    fun `remindAtInstant returns null for garbage instead of throwing`() {
        assertNull(reminder(remindAt = "not-a-date").remindAtInstant())
        assertNull(reminder(remindAt = "").remindAtInstant())
        // Local date-time without offset is not a valid Instant
        assertNull(reminder(remindAt = "2026-07-10T09:00:00").remindAtInstant())
    }

    @Test
    fun `fresh reminder needs scheduling`() {
        assertTrue(reminder().needsScheduling())
    }

    @Test
    fun `past-due reminder still needs scheduling`() {
        // The fire time being in the past must NOT exclude it: a reminder that came
        // due while the device was off is delivered late by BootWorker, not dropped.
        assertTrue(reminder(remindAt = "2020-01-01T00:00:00Z").needsScheduling())
    }

    @Test
    fun `reminded, completed, or archived reminders do not need scheduling`() {
        assertFalse(reminder(reminded = true).needsScheduling())
        assertFalse(reminder(completedAt = "2026-07-09T12:00:00Z").needsScheduling())
        assertFalse(reminder(archivedAt = "2026-07-09T12:00:00Z").needsScheduling())
    }

    @Test
    fun `unparseable fire time does not need scheduling`() {
        assertFalse(reminder(remindAt = "garbage").needsScheduling())
    }

    @Test
    fun `task reminderInstant parses and rejects like the reminder helper`() {
        val task = TaskDto(id = "t1", title = "title", reminderAt = "2026-07-10T09:00:00Z")
        assertEquals(Instant.parse("2026-07-10T09:00:00Z"), task.reminderInstant())
        assertNull(task.copy(reminderAt = "tomorrow-ish").reminderInstant())
        assertNull(task.copy(reminderAt = null).reminderInstant())
    }

    // ── Repeat schedule ───────────────────────────────────────────────────────

    @Test
    fun `repeat days parse by name and ignore garbage`() {
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            reminder(repeatDays = listOf("MONDAY", "funday", "FRIDAY")).repeatDaySet(),
        )
        assertFalse(reminder().repeats)
        assertTrue(reminder(repeatDays = listOf("SUNDAY")).repeats)
    }

    @Test
    fun `a repeating memo always needs scheduling until archived`() {
        val weekly = reminder(remindAt = "2026-07-13T07:00:00Z", repeatDays = monWedFri, lastRangAt = "2026-07-10T07:00:00Z")
        assertTrue(weekly.needsScheduling())
        assertFalse(weekly.copy(archivedAt = "2026-07-10T08:00:00Z").needsScheduling())
    }

    @Test
    fun `ringing a once-only memo marks it reminded and records when`() {
        val rang = reminder(remindAt = "2026-07-10T09:30:00Z").afterRing(now, zone)
        assertTrue(rang.reminded)
        assertEquals(now.toString(), rang.lastRangAt)
        assertEquals("2026-07-10T09:30:00Z", rang.remindAt)
        assertEquals(now, rang.unacknowledgedRing())
    }

    @Test
    fun `ringing a repeating memo arms the next chosen day at the same local time`() {
        val rang = reminder(remindAt = "2026-07-10T09:30:00Z", repeatDays = monWedFri).afterRing(now, zone)
        assertFalse(rang.reminded)
        assertEquals("2026-07-13T09:30:00Z", rang.remindAt) // Monday
        assertEquals(now, rang.unacknowledgedRing())
    }

    @Test
    fun `a snoozed re-ring keeps the next ring that is already armed`() {
        // The 07:00 ring was snoozed; when it fires again at 09:30 the record already
        // points at Monday. Advancing again would skip Monday altogether.
        val snoozed = reminder(remindAt = "2026-07-13T07:00:00Z", repeatDays = monWedFri, lastRangAt = "2026-07-10T07:00:00Z")
        val rang = snoozed.afterRing(now, zone)
        assertEquals("2026-07-13T07:00:00Z", rang.remindAt)
        assertEquals(now.toString(), rang.lastRangAt)
    }

    @Test
    fun `done completes a once-only memo`() {
        val done = reminder(remindAt = "2026-07-10T07:00:00Z", reminded = true).afterDone(now, zone)
        assertEquals(now.toString(), done.completedAt)
        assertNull(done.unacknowledgedRing())
    }

    @Test
    fun `done acknowledges a repeating memo's waiting ring and leaves its next ring alone`() {
        val waiting = reminder(remindAt = "2026-07-13T07:00:00Z", repeatDays = monWedFri, lastRangAt = "2026-07-10T07:00:00Z")
        val done = waiting.afterDone(now, zone)
        assertEquals(now.toString(), done.acknowledgedAt)
        assertEquals("2026-07-13T07:00:00Z", done.remindAt)
        assertNull(done.completedAt)
        assertNull(done.unacknowledgedRing())
    }

    @Test
    fun `done with nothing waiting skips the next ring of a repeating memo`() {
        val quiet = reminder(remindAt = "2026-07-10T19:00:00Z", repeatDays = monWedFri)
        assertEquals("2026-07-13T19:00:00Z", quiet.afterDone(now, zone).remindAt)
    }

    @Test
    fun `undo reopens a once-only memo and un-acknowledges a repeating one`() {
        assertNull(reminder(completedAt = "2026-07-10T08:00:00Z").afterUndone().completedAt)
        val ack = reminder(remindAt = "2026-07-13T07:00:00Z", repeatDays = monWedFri, lastRangAt = "2026-07-10T07:00:00Z", acknowledgedAt = "2026-07-10T07:05:00Z")
        assertEquals(Instant.parse("2026-07-10T07:00:00Z"), ack.afterUndone().unacknowledgedRing())
    }

    @Test
    fun `saving aligns a repeating ring onto a chosen day in the future`() {
        // Set for Saturday but repeats Mon/Wed/Fri: moves to Monday at the same time.
        val offDay = reminder(remindAt = "2026-07-11T07:00:00Z", repeatDays = monWedFri)
        assertEquals("2026-07-13T07:00:00Z", offDay.withScheduleAligned(now, zone).remindAt)
        // Already on Friday and later today: untouched.
        val onDay = reminder(remindAt = "2026-07-10T19:00:00Z", repeatDays = monWedFri)
        assertEquals("2026-07-10T19:00:00Z", onDay.withScheduleAligned(now, zone).remindAt)
        // Once-only memos are never moved.
        assertEquals("2026-07-11T07:00:00Z", reminder(remindAt = "2026-07-11T07:00:00Z").withScheduleAligned(now, zone).remindAt)
    }

    @Test
    fun `legacy once-only records that rang before ring times were stored use the fire time`() {
        val legacy = reminder(remindAt = "2026-07-10T07:00:00Z", reminded = true)
        assertEquals(Instant.parse("2026-07-10T07:00:00Z"), legacy.unacknowledgedRing())
    }
}
