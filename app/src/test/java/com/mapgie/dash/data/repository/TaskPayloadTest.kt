package com.mapgie.dash.data.repository

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
