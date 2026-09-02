package com.mapgie.dash.ui.screens.tasks

import com.mapgie.dash.data.model.CategoryCatalog
import com.mapgie.dash.data.model.OwnerFilter
import com.mapgie.dash.data.model.SortOrder
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskSortKey
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the Tasks list shows for a given [TaskUiState]: owner scope, the
 * open-versus-done split (archived never shown), the far-future
 * hide threshold, the sort pill orders, grouping and the summary bar. Pure state logic, no ViewModel or
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
            sort = SortOrder(TaskSortKey.PRIORITY),
        )
        assertEquals(listOf("higher", "normal", "lower"), ids(state.displayed))
    }

    @Test
    fun `due sort puts overdue before undated regardless of priority`() {
        val overdue = task("overdue", dueDate = LocalDate.now().minusDays(2).toString(), priority = "lower")
        val undatedHigh = task("undated-high", priority = "higher")
        val state = TaskUiState(tasks = listOf(undatedHigh, overdue), sort = SortOrder(TaskSortKey.DUE))
        assertEquals(listOf("overdue", "undated-high"), ids(state.displayed))
    }

    @Test
    fun `due latest first reverses the dated tasks but keeps undated ones last`() {
        val today = LocalDate.now()
        val overdue = task("overdue", dueDate = today.minusDays(2).toString())
        val soon = task("soon", dueDate = today.plusDays(2).toString())
        val undated = task("undated", priority = "higher")
        val state = TaskUiState(tasks = listOf(undated, overdue, soon), sort = SortOrder(TaskSortKey.DUE, reversed = true))
        assertEquals(listOf("soon", "overdue", "undated"), ids(state.displayed))
    }

    @Test
    fun `priority lowest first flips the priority order`() {
        val state = TaskUiState(
            tasks = listOf(task("higher", priority = "higher"), task("lower", priority = "lower"), task("normal")),
            sort = SortOrder(TaskSortKey.PRIORITY, reversed = true),
        )
        assertEquals(listOf("lower", "normal", "higher"), ids(state.displayed))
    }

    @Test
    fun `name sort is alphabetical and case-insensitive`() {
        val state = TaskUiState(
            tasks = listOf(task("banana"), task("Apple"), task("cherry")),
            sort = SortOrder(TaskSortKey.NAME),
        )
        assertEquals(listOf("Apple", "banana", "cherry"), ids(state.displayed))
    }

    // ── Grouping and summary ──────────────────────────────────────────────────

    private fun categorised(id: String, category: String?) = TaskDto(id = id, title = id, category = category)

    @Test
    fun `groups follow the catalog order with general last and other after`() {
        val state = TaskUiState(
            tasks = listOf(categorised("g", "General"), categorised("a", "Admin"), categorised("n", null), categorised("c", "Car")),
            catalog = CategoryCatalog(order = listOf("Car")),
        )
        assertEquals(listOf("Car", "Admin", "General", OTHER_CATEGORY_LABEL), state.grouped.map { it.first })
    }

    @Test
    fun `summary counts open tasks and names those hidden by the threshold`() {
        val today = LocalDate.now()
        val soon = task("soon", dueDate = today.plusDays(3).toString())
        val far = task("far", dueDate = today.plusDays(30).toString())
        val done = task("done", completedAt = "2026-08-01T10:00:00Z")
        assertEquals("1 task · 1 hidden", TaskUiState(tasks = listOf(soon, far, done), hideThresholdDays = 7).summaryLabel)
        assertEquals("2 tasks", TaskUiState(tasks = listOf(soon, far, done)).summaryLabel)
    }
}
