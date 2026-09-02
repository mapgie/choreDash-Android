package com.mapgie.dash.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * What the Edit sheets guarantee about unsaved drafts: a draft is only offered
 * back when it would change something, the New sheets open on their defaults,
 * and a draft survives a trip through JSON unchanged.
 */
class SheetDraftTest {

    private val meds = Chore(
        id = "chore-1",
        tagId = "04a1b2",
        label = "Meds",
        category = "Health",
        owner = "sam",
        intervalDays = 28.0,
        archivedAt = null,
        lastScanned = null,
        lastScanId = null,
        status = ChoreStatus.NEVER,
    )

    private val taxes = TaskDto(
        id = "task-1",
        title = "File taxes",
        notes = "Need last year's return",
        category = "Admin",
        owner = "sam",
        priority = "higher",
        dueDate = "2026-09-30",
        reminderAt = "2026-09-20T09:30:45Z",
    )

    @Test
    fun `a draft that matches the opened chore is not offered`() {
        val opened = ChoreDraft.of(meds)
        assertFalse(ChoreDraft.of(meds).differsFrom(opened))
        assertEquals(ChoreDraft("Meds", "Health", "sam", 28, "04a1b2"), opened)
    }

    @Test
    fun `a draft with a different title is offered`() {
        val opened = ChoreDraft.of(meds)
        assertTrue(opened.copy(label = "Meds (evening)").differsFrom(opened))
        assertTrue(opened.copy(intervalDays = null).differsFrom(opened))
    }

    @Test
    fun `a new chore sheet opens on General with the scanned tag id`() {
        val opened = ChoreDraft.of(null, initialTagId = "abc123")
        assertEquals(ChoreDraft(category = GENERAL_CATEGORY, tagId = "abc123"), opened)
        assertFalse(ChoreDraft(category = GENERAL_CATEGORY, tagId = "abc123").differsFrom(opened))
        assertNull(opened.displayName())
        assertEquals("Water plants", opened.copy(label = " Water plants ").displayName())
    }

    @Test
    fun `a task draft reads due date priority and reminder from the opened task`() {
        val opened = TaskDraft.of(taxes)
        assertEquals("File taxes", opened.title)
        assertEquals("Need last year's return", opened.notes)
        assertEquals(TaskPriority.HIGHER, opened.priorityEnum())
        assertEquals(TaskDueType.DATE, opened.dueType)
        assertEquals(LocalDate.of(2026, 9, 30), opened.dueDate())
        assertTrue(opened.reminderEnabled)
    }

    @Test
    fun `a stored reminder with seconds opens as the same whole minute`() {
        val opened = TaskDraft.of(taxes)
        assertEquals(Instant.parse("2026-09-20T09:30:00Z").toEpochMilli(), opened.reminderAtEpochMillis)
        assertFalse(opened.copy(reminderAtEpochMillis = Instant.parse("2026-09-20T09:30:00Z").toEpochMilli()).differsFrom(opened))
    }

    @Test
    fun `a blank new task draft is not offered`() {
        val opened = TaskDraft.of(null)
        assertEquals(TaskDraft(category = GENERAL_CATEGORY), opened)
        assertFalse(TaskDraft(category = GENERAL_CATEGORY).differsFrom(opened))
        assertTrue(opened.copy(title = "Call the vet").differsFrom(opened))
    }

    @Test
    fun `turning a reminder off is a change but its time no longer counts`() {
        val opened = TaskDraft.of(taxes)
        assertTrue(opened.copy(reminderEnabled = false, reminderAtEpochMillis = null).differsFrom(opened))
        val off = TaskDraft.of(taxes.copy(reminderAt = null))
        assertFalse(off.copy(reminderEnabled = false, reminderAtEpochMillis = null).differsFrom(off))
    }

    @Test
    fun `drafts round-trip through JSON`() {
        val chore = ChoreDraft.of(meds).copy(label = "Meds (evening)", intervalDays = null)
        assertEquals(chore, Json.decodeFromString(ChoreDraft.serializer(), Json.encodeToString(ChoreDraft.serializer(), chore)))

        val task = TaskDraft.of(taxes).copy(dueType = TaskDueType.PERIOD, duePeriod = "this_week", dueDateEpochDay = null)
        assertEquals(task, Json.decodeFromString(TaskDraft.serializer(), Json.encodeToString(TaskDraft.serializer(), task)))
    }

    @Test
    fun `an unknown priority name falls back to normal`() {
        assertEquals(TaskPriority.NORMAL, TaskDraft(priority = "urgent").priorityEnum())
    }

    @Test
    fun `drafts are keyed by item id and new sheets share one key`() {
        assertEquals("chore-1", draftKeyFor("chore-1"))
        assertEquals(NEW_DRAFT_KEY, draftKeyFor(null))
    }
}
