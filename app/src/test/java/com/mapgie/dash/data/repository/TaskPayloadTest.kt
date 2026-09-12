package com.mapgie.dash.data.repository

import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskInsert
import com.mapgie.dash.data.model.TaskUpdate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPayloadTest {

    @Test
    fun `edit payload always writes every editable column`() {
        val payload = editTaskPayload(TaskUpdate(title = "Buy a bike", priority = "normal"))
        assertEquals(
            setOf(
                "title", "notes", "category", "owner",
                "priority", "due_date", "due_period", "reminder_at",
            ),
            payload.keys,
        )
        assertEquals("Buy a bike", payload["title"])
    }

    @Test
    fun `cleared fields stay present as explicit nulls`() {
        // Regression: clearing owner/notes/due date used to be silently dropped
        // from the PATCH body, leaving the old value in Supabase.
        val payload = editTaskPayload(TaskUpdate(title = "t", owner = null, dueDate = null))
        assertTrue(payload.containsKey("owner"))
        assertNull(payload["owner"])
        assertTrue(payload.containsKey("due_date"))
        assertNull(payload["due_date"])
    }

    @Test
    fun `edit payload never touches completion, archival, or reminded state`() {
        val payload = editTaskPayload(TaskUpdate(title = "t"))
        assertTrue("completed_at" !in payload)
        assertTrue("archived_at" !in payload)
        assertTrue("reminded" !in payload)
    }

    @Test
    fun `un-completing sends an explicit null, completing sends the timestamp`() {
        // Regression: markUndone used to PATCH an empty body, which returned no
        // rows and surfaced to the user as a "List is empty" error.
        val undone = completedAtPayload(null)
        assertEquals(setOf("completed_at"), undone.keys)
        assertNull(undone["completed_at"])

        assertEquals("2026-08-26T12:00:00Z", completedAtPayload("2026-08-26T12:00:00Z")["completed_at"])
    }

    @Test
    fun `archiving sends only archived_at, restoring sends it as an explicit null`() {
        val archived = archivedAtPayload("2026-08-26T12:00:00Z")
        assertEquals(setOf("archived_at"), archived.keys)
        assertEquals("2026-08-26T12:00:00Z", archived["archived_at"])

        val restored = archivedAtPayload(null)
        assertEquals(setOf("archived_at"), restored.keys)
        assertNull(restored["archived_at"])
    }

    // ── Private (phone-only) rows ─────────────────────────────────────────────

    private val storedPrivate = TaskDto(
        id = "t1", title = "Buy a present", category = "Private", owner = "ana",
        completedAt = "2026-08-01T10:00:00Z", archivedAt = null, reminderAt = "2026-09-01T09:00:00Z",
        reminded = true, createdAt = "2026-07-01T10:00:00Z",
    )

    @Test
    fun `editing a stored row changes exactly the edit-sheet columns`() {
        val edited = storedPrivate.edited(
            TaskUpdate(title = "Wrap the present", category = "Private", owner = null, priority = "higher",
                dueDate = "2026-09-10", reminderAt = "2026-09-01T09:00:00Z")
        )
        assertEquals("Wrap the present", edited.title)
        assertNull(edited.owner)
        assertEquals("higher", edited.priority)
        assertEquals("2026-09-10", edited.dueDate)
        // Completion, archival, creation and (with the reminder unchanged) reminded state survive.
        assertEquals("2026-08-01T10:00:00Z", edited.completedAt)
        assertEquals("2026-07-01T10:00:00Z", edited.createdAt)
        assertEquals(true, edited.reminded)
        assertEquals("t1", edited.id)
    }

    @Test
    fun `a changed reminder time is a new reminder, so reminded resets`() {
        val edited = storedPrivate.edited(TaskUpdate(title = "t", reminderAt = "2026-09-02T09:00:00Z"))
        assertEquals(false, edited.reminded)
        assertEquals(false, storedPrivate.edited(TaskUpdate(title = "t", reminderAt = null)).reminded)
    }

    @Test
    fun `a new private row carries the insert and nothing else`() {
        val row = privateRow(
            "id-1",
            TaskInsert(title = "Buy a present", category = "Private", dueDate = "2026-09-10", reminderAt = null),
            createdAt = "2026-08-26T12:00:00Z",
        )
        assertEquals("id-1", row.id)
        assertEquals("Private", row.category)
        assertEquals("2026-08-26T12:00:00Z", row.createdAt)
        assertNull(row.completedAt)
        assertNull(row.archivedAt)
        assertEquals(false, row.reminded)
    }

    @Test
    fun `a row moving to the shared list keeps its id and its done and archived state`() {
        val insert = storedPrivate.copy(category = "Kitchen").toInsert()
        assertEquals("t1", insert.id)
        assertEquals("Kitchen", insert.category)
        assertEquals("2026-08-01T10:00:00Z", insert.completedAt)
        assertNull(insert.archivedAt)
        // The id travels in the JSON body; unset optional columns stay out of it.
        val json = Json { encodeDefaults = false }
        val body = json.encodeToString(insert)
        assertTrue(body.contains("\"id\":\"t1\""))
        assertTrue("archived_at" !in body)
        assertTrue("id" !in json.encodeToString(TaskInsert(title = "fresh")))
    }

    @Test
    fun `serializing TaskUpdate drops null fields, which is why payloads are maps`() {
        // Documents the kotlinx.serialization behaviour behind the bug: with
        // encodeDefaults=false (the Supabase client's setting), a field set to
        // its default null is omitted entirely, so TaskUpdate cannot express
        // "set this column to null". If this assertion ever fails, the map
        // payloads above may no longer be needed.
        val json = Json { encodeDefaults = false }
        assertEquals("{}", json.encodeToString(TaskUpdate(completedAt = null)))
    }
}
