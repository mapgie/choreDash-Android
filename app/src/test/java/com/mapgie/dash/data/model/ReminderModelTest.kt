package com.mapgie.dash.data.model

import java.time.Instant
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
    ) = ReminderDto(
        id = "r1",
        subject = "subject",
        remindAt = remindAt,
        reminded = reminded,
        completedAt = completedAt,
        archivedAt = archivedAt,
    )

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
}
