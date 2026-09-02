package com.mapgie.dash.ui.screens.tasks

import com.mapgie.dash.data.model.OwnerFilter
import com.mapgie.dash.data.model.TaskDto
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the Tasks list shows for a given [TaskUiState]: owner scope, the
 * open-versus-done split (archived never shown), the far-future
 * hide threshold, and the priority sort. Pure state logic, no ViewModel or
 * Android involved.
 */
class TaskUiStateTest {

    private val me = "ana"

    private fun task(
        id: String,
        owner: String? = null,
        completedAt: String? = null,
        archivedAt: String? = null,
        dueDate: String? = null,
        priority: String = "normal",
    ) = TaskDto(
        id = id,
        title = id,
        owner = owner,
        completedAt = completedAt,
        archivedAt = archivedAt,
        dueDate = dueDate,
        priority = priority,
    )

    private fun ids(tasks: List<TaskDto>) = tasks.map { it.id }

    // ── Owner scope ───────────────────────────────────────────────────────────

    private val mine = task("mine", owner = me)
    private val theirs = task("theirs", owner = "mo")
    private val unassigned = task("unassigned", owner = null)
    private val byOwner = listOf(mine, theirs, unassigned)

    @Test
    fun `everyone lists every owner`() {
        val state = TaskUiState(tasks = byOwner, ownerHandle = me, ownerFilter = OwnerFilter.EVERYONE)
        assertEquals(listOf("mine", "theirs", "unassigned"), ids(state.displayed))
    }

    @Test
    fun `mine lists only my tasks`() {
        val state = TaskUiState(tasks = byOwner, ownerHandle = me, ownerFilter = OwnerFilter.MINE)
        assertEquals(listOf("mine"), ids(state.displayed))
    }

    @Test
    fun `mine and unassigned lists my tasks and unclaimed ones`() {
        val state = TaskUiState(tasks = byOwner, ownerHandle = me, ownerFilter = OwnerFilter.MINE_AND_UNASSIGNED)
        assertEquals(listOf("mine", "unassigned"), ids(state.displayed))
    }

    // ── Open, done, archived ──────────────────────────────────────────────────

    private val open = task("open")
    private val done = task("done", completedAt = "2026-08-01T10:00:00Z")
    private val archived = task("archived", archivedAt = "2026-08-01T10:00:00Z")
    private val doneAndArchived = task("done-archived", completedAt = "2026-08-01T10:00:00Z", archivedAt = "2026-08-02T10:00:00Z")
    private val byStatus = listOf(open, done, archived, doneAndArchived)

    @Test
    fun `archived tasks never show, done or not`() {
        val state = TaskUiState(tasks = byStatus)
        assertEquals(listOf("open", "done"), ids(state.displayed))
    }

    @Test
    fun `open tasks go to the main list and done tasks to the done section`() {
        val state = TaskUiState(tasks = byStatus)
        assertEquals(listOf("open"), ids(state.activeTasks))
        assertEquals(listOf("done"), ids(state.doneTasks))
    }

    // ── Far-future hide threshold ─────────────────────────────────────────────

    @Test
    fun `hide threshold drops tasks due beyond it but keeps undated ones`() {
        val today = LocalDate.now()
        val soon = task("soon", dueDate = today.plusDays(3).toString())
        val far = task("far", dueDate = today.plusDays(30).toString())
        val undated = task("undated")
        val state = TaskUiState(tasks = listOf(soon, far, undated), hideThresholdDays = 7)
        assertEquals(setOf("soon", "undated"), ids(state.activeTasks).toSet())
    }

    @Test
    fun `negative hide threshold means nothing is hidden`() {
        val far = task("far", dueDate = LocalDate.now().plusDays(400).toString())
        val state = TaskUiState(tasks = listOf(far), hideThresholdDays = -1)
        assertEquals(listOf("far"), ids(state.activeTasks))
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    fun `priority sort orders higher, normal, lower`() {
        val state = TaskUiState(
            tasks = listOf(task("lower", priority = "lower"), task("normal"), task("higher", priority = "higher")),
            sort = TaskSort.PRIORITY,
        )
        assertEquals(listOf("higher", "normal", "lower"), ids(state.displayed))
    }

    @Test
    fun `due sort puts overdue before undated regardless of priority`() {
        val overdue = task("overdue", dueDate = LocalDate.now().minusDays(2).toString(), priority = "lower")
        val undatedHigh = task("undated-high", priority = "higher")
        val state = TaskUiState(tasks = listOf(undatedHigh, overdue), sort = TaskSort.DUE)
        assertEquals(listOf("overdue", "undated-high"), ids(state.displayed))
    }
}
