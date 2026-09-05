package com.mapgie.dash.ui.screens.reminders

import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderSortKey
import com.mapgie.dash.data.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the Memos list shows for a given [ReminderUiState]: the Active / Done /
 * All split (a once-only memo is done once it has rung; a repeating one is
 * never done), the sort pill orders, and the "linked to" suffix. Pure state
 * logic, no ViewModel or Android involved. Fire times are far in the past or
 * far in the future so no bucket depends on the wall clock.
 */
class ReminderUiStateTest {

    private fun memo(
        id: String,
        remindAt: String = "2099-07-15T07:00:00Z",
        repeatDays: List<String> = emptyList(),
        reminded: Boolean = false,
        completedAt: String? = null,
        archivedAt: String? = null,
        lastRangAt: String? = null,
        acknowledgedAt: String? = null,
        createdAt: String = "2026-07-01T00:00:00Z",
        choreId: String? = null,
        taskId: String? = null,
    ) = ReminderDto(
        id = id, subject = id, remindAt = remindAt, repeatDays = repeatDays,
        reminded = reminded, completedAt = completedAt, archivedAt = archivedAt,
        lastRangAt = lastRangAt, acknowledgedAt = acknowledgedAt, createdAt = createdAt,
        choreId = choreId, taskId = taskId,
    )

    private fun ids(list: List<ReminderDto>) = list.map { it.id }

    private val upcoming = memo("upcoming")
    private val rang = memo("rang", remindAt = "2020-01-01T08:00:00Z", reminded = true, lastRangAt = "2020-01-01T08:00:00Z")
    private val completed = memo("completed", remindAt = "2020-01-02T08:00:00Z", reminded = true, completedAt = "2020-01-02T09:00:00Z")
    private val archived = memo("archived", archivedAt = "2026-07-01T00:00:00Z")
    private val weekly = memo(
        "weekly", remindAt = "2099-07-13T07:00:00Z", repeatDays = listOf("MONDAY"),
        lastRangAt = "2020-01-01T07:00:00Z", acknowledgedAt = "2020-01-01T07:05:00Z",
    )
    private val everything = listOf(upcoming, rang, completed, archived, weekly)

    // ── Buckets ───────────────────────────────────────────────────────────────

    @Test
    fun `a once-only memo that has rung is done, not active`() {
        val state = ReminderUiState(reminders = everything)
        assertTrue("rang" !in ids(state.active))
        assertTrue("rang" in ids(state.done))
    }

    @Test
    fun `active holds unrung once-only memos and every repeating memo, never archived ones`() {
        val state = ReminderUiState(reminders = everything)
        assertEquals(listOf("weekly", "upcoming"), ids(state.active))
    }

    @Test
    fun `done lists once-only memos that rang or were dismissed, soonest ring first`() {
        val state = ReminderUiState(reminders = everything, filter = ReminderFilter.DONE)
        assertEquals(listOf("rang", "completed"), ids(state.displayed))
    }

    @Test
    fun `a repeating memo is never done, even after it has rung`() {
        val state = ReminderUiState(reminders = everything)
        assertTrue("weekly" !in ids(state.done))
        assertTrue("weekly" in ids(state.active))
    }

    @Test
    fun `all lists everything, archived included`() {
        val state = ReminderUiState(reminders = everything, filter = ReminderFilter.ALL)
        assertEquals(everything.size, state.displayed.size)
        assertTrue("archived" in ids(state.displayed))
    }

    @Test
    fun `active is the default filter and the chip count matches it`() {
        val state = ReminderUiState(reminders = everything)
        assertEquals(ReminderFilter.ACTIVE, state.filter)
        assertEquals(2, state.activeCount)
        assertEquals(ids(state.active), ids(state.displayed))
    }

    // ── Sort ──────────────────────────────────────────────────────────────────

    @Test
    fun `soonest first orders active memos by their next ring`() {
        val soon = memo("soon", remindAt = "2099-07-11T09:00:00Z")
        val state = ReminderUiState(reminders = listOf(upcoming, soon, weekly))
        assertEquals(listOf("soon", "weekly", "upcoming"), ids(state.active))
    }

    @Test
    fun `latest first reverses soonest first`() {
        val soon = memo("soon", remindAt = "2099-07-11T09:00:00Z")
        val state = ReminderUiState(
            reminders = listOf(upcoming, soon, weekly),
            sort = SortOrder(ReminderSortKey.NEXT_RING, reversed = true),
        )
        assertEquals(listOf("upcoming", "weekly", "soon"), ids(state.active))
    }

    @Test
    fun `name sorts A to Z ignoring case, and Z to A when reversed`() {
        val list = listOf(memo("banana"), memo("Apple"), memo("cherry"))
        assertEquals(
            listOf("Apple", "banana", "cherry"),
            ids(ReminderUiState(reminders = list, sort = SortOrder(ReminderSortKey.NAME)).active),
        )
        assertEquals(
            listOf("cherry", "banana", "Apple"),
            ids(ReminderUiState(reminders = list, sort = SortOrder(ReminderSortKey.NAME, reversed = true)).active),
        )
    }

    @Test
    fun `added sorts newest first`() {
        val list = listOf(
            memo("old", createdAt = "2026-01-01T00:00:00Z"),
            memo("new", createdAt = "2026-06-01T00:00:00Z"),
        )
        assertEquals(
            listOf("new", "old"),
            ids(ReminderUiState(reminders = list, sort = SortOrder(ReminderSortKey.CREATED)).active),
        )
    }

    // ── Linked suffix ─────────────────────────────────────────────────────────

    @Test
    fun `linked to names the kind of item, or nothing for a standalone memo`() {
        val state = ReminderUiState()
        assertEquals("chore", state.linkedTo(memo("c", choreId = "chore-1")))
        assertEquals("task", state.linkedTo(memo("t", taskId = "task-1")))
        assertNull(state.linkedTo(memo("s")))
    }
}
