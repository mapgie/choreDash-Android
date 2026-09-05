package com.mapgie.dash.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * What the edit-alarm sheet guarantees about unsaved drafts: it opens on the
 * record's own values at whole-minute precision, a New sheet opens on this time
 * tomorrow with any seeded link, and a draft is only "different" when the sheet
 * would show something else.
 */
class ReminderDraftTest {

    private val now: Instant = Instant.parse("2026-07-10T09:30:00Z")

    private val plants = ReminderDto(
        id = "m1",
        subject = "Water the plants",
        remindAt = "2026-07-13T07:00:45Z",
        choreId = "chore-9",
        repeatDays = listOf("FRIDAY", "MONDAY", "WEDNESDAY"),
    )

    @Test
    fun `opening an existing memo takes its fields, sorted days and whole-minute ring`() {
        val opened = ReminderDraft.of(plants, now = now)
        assertEquals("Water the plants", opened.subject)
        assertEquals(Instant.parse("2026-07-13T07:00:00Z").toEpochMilli(), opened.ringAtEpochMillis)
        assertEquals(listOf("MONDAY", "WEDNESDAY", "FRIDAY"), opened.repeatDays)
        assertEquals("chore-9", opened.choreId)
        assertEquals("", opened.taskId)
        assertEquals("", opened.sound)
        assertEquals("content://x/1", ReminderDraft.of(plants.copy(sound = "content://x/1"), now = now).sound)
    }

    @Test
    fun `a new memo opens this time tomorrow with the seeded subject and link`() {
        val opened = ReminderDraft.of(null, initialSubject = "Meds", initialTaskId = "task-2", now = now)
        assertEquals("Meds", opened.subject)
        assertEquals(Instant.parse("2026-07-11T09:30:00Z").toEpochMilli(), opened.ringAtEpochMillis)
        assertTrue(opened.repeatDays.isEmpty())
        assertEquals("task-2", opened.taskId)
        assertEquals("", opened.choreId)
    }

    @Test
    fun `a draft differs only when a field changed`() {
        val opened = ReminderDraft.of(plants, now = now)
        assertFalse(opened.copy().differsFrom(opened))
        assertTrue(opened.copy(subject = "Water the ferns").differsFrom(opened))
        assertTrue(opened.copy(repeatDays = listOf("MONDAY")).differsFrom(opened))
        assertTrue(opened.copy(choreId = "").differsFrom(opened))
    }

    @Test
    fun `display name is the title typed so far, or nothing`() {
        assertEquals("Meds", ReminderDraft(subject = " Meds ").displayName())
        assertNull(ReminderDraft(subject = "   ").displayName())
    }

    @Test
    fun `a draft survives a trip through JSON unchanged`() {
        val draft = ReminderDraft.of(plants, now = now)
        val json = Json.encodeToString(ReminderDraft.serializer(), draft)
        assertEquals(draft, Json.decodeFromString(ReminderDraft.serializer(), json))
    }
}
