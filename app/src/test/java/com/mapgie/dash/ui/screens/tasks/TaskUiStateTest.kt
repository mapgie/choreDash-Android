package com.mapgie.dash.ui.screens.tasks

import com.mapgie.dash.data.model.CategoryCatalog
import com.mapgie.dash.data.model.CategoryStyle
import com.mapgie.dash.data.model.ChoreColourAxes
import com.mapgie.dash.data.model.ColourChoresBy
import com.mapgie.dash.data.model.OwnerFilter
import com.mapgie.dash.data.model.PRIVATE_CATEGORY
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.SortOrder
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskSortKey
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        category: String? = null,
    ) = TaskDto(
        id = id,
        title = id,
        owner = owner,
        completedAt = completedAt,
        archivedAt = archivedAt,
        dueDate = dueDate,
        priority = priority,
        category = category,
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

    @Test
    fun `mine includes private tasks whoever they are assigned to`() {
        // A private task lives on this phone, so it is mine even with no owner or
        // someone else's handle on it.
        val privateTasks = listOf(
            task("secret", owner = null, category = PRIVATE_CATEGORY),
            task("secret-mo", owner = "mo", category = "private"),
        )
        val state = TaskUiState(tasks = byOwner + privateTasks, ownerHandle = me, ownerFilter = OwnerFilter.MINE)
        assertEquals(listOf("mine", "secret", "secret-mo"), ids(state.displayed))
    }

    @Test
    fun `private tasks group under their own header`() {
        val state = TaskUiState(
            tasks = listOf(task("gift", category = PRIVATE_CATEGORY), task("bike", category = "Car")),
            groupByCategory = true,
        )
        assertEquals(listOf("Car", PRIVATE_CATEGORY), state.grouped.map { it.first })
        assertEquals(listOf("gift"), ids(state.grouped.first { it.first == PRIVATE_CATEGORY }.second))
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

    @Test
    fun `done section lists most recently completed first`() {
        // Priority sort would order these higher-normal-lower; the done section
        // must ignore that and lead with the most recently completed task.
        val early = task("early", completedAt = "2026-08-01T10:00:00Z", priority = "higher")
        val late = task("late", completedAt = "2026-08-05T09:00:00Z", priority = "lower")
        val mid = task("mid", completedAt = "2026-08-03T12:00:00Z")
        val state = TaskUiState(tasks = listOf(early, late, mid), sort = SortOrder(TaskSortKey.PRIORITY))
        assertEquals(listOf("late", "mid", "early"), ids(state.doneTasks))
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
    fun `eventually tasks trail dated ones under the due sort, like undated`() {
        val overdue = task("overdue", dueDate = LocalDate.now().minusDays(1).toString())
        val eventually = TaskDto(id = "eventually", title = "eventually", duePeriod = "eventually")
        val state = TaskUiState(tasks = listOf(eventually, overdue), sort = SortOrder(TaskSortKey.DUE))
        assertEquals(listOf("overdue", "eventually"), ids(state.displayed))
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

    // ── Zen rows ──────────────────────────────────────────────────────────────

    @Test
    fun `zen shows only open tasks, so a ticked task leaves the list`() {
        // Noon today cannot cross midnight during the run.
        val zone = ZoneId.systemDefault()
        val noonToday = LocalDate.now(zone).atTime(12, 0).atZone(zone).toInstant().toString()
        val open = task("open")
        val doneToday = task("done_today", completedAt = noonToday)
        val state = TaskUiState(tasks = listOf(doneToday, open), zenMode = true)
        assertEquals(listOf("open"), ids(state.zenRows))
        assertEquals(listOf("done_today"), ids(state.doneTasks))
    }

    // ── Colour axes (Settings › Colours) ──────────────────────────────────────
    // The Tasks list must obey the same spine/icon axes as the Chores list, not
    // colour itself purely by urgency.

    private val kitchen = TaskDto(id = "k", title = "k", category = "Kitchen")
    private val uncategorised = TaskDto(id = "u", title = "u", category = null)
    private val kitchenCatalog = CategoryCatalog(styles = mapOf("Kitchen" to CategoryStyle(swatch = Swatch.PEACH.name)))

    @Test
    fun `by default a task colours its icon by category and its spine by severity`() {
        // Fresh install defaults: spine + badge by severity, icon by category.
        val state = TaskUiState(catalog = kitchenCatalog)
        assertEquals(Swatch.PEACH, state.iconSwatchFor(kitchen))
        assertNull(state.spineSwatchFor(kitchen))
    }

    @Test
    fun `colouring the spine by category gives the task its category swatch`() {
        val state = TaskUiState(
            catalog = kitchenCatalog,
            colourAxes = ChoreColourAxes(ColourChoresBy.CATEGORY, ColourChoresBy.CATEGORY),
        )
        assertEquals(Swatch.PEACH, state.spineSwatchFor(kitchen))
        assertEquals(Swatch.PEACH, state.iconSwatchFor(kitchen))
    }

    @Test
    fun `an uncategorised task still gets a stable fallback swatch when colouring by category`() {
        val state = TaskUiState(colourAxes = ChoreColourAxes(ColourChoresBy.CATEGORY, ColourChoresBy.CATEGORY))
        // effectiveSwatch(null) is a fixed fallback, never null, so the chip is always coloured.
        assertEquals(Swatch.SAGE, state.iconSwatchFor(uncategorised))
        assertEquals(Swatch.SAGE, state.spineSwatchFor(uncategorised))
    }

    // ── Reminders a task owns ─────────────────────────────────────────────────
    // A task can carry several reminders (memos linked by task_id); the card
    // counts the live ones so adding a reminder never hides the task.

    private fun memo(id: String, taskId: String?, archivedAt: String? = null, completedAt: String? = null) =
        ReminderDto(id = id, subject = "s", remindAt = "2026-09-20T09:00:00Z", taskId = taskId, archivedAt = archivedAt, completedAt = completedAt)

    @Test
    fun `a task counts its live reminders and ignores archived, done and other tasks'`() {
        val reminders = listOf(
            memo("a", taskId = "t1"),
            memo("b", taskId = "t1"),
            memo("c", taskId = "t1", archivedAt = "2026-09-01T00:00:00Z"),
            memo("d", taskId = "t1", completedAt = "2026-09-01T00:00:00Z"),
            memo("e", taskId = "other"),
        )
        val state = TaskUiState(reminders = reminders)
        assertEquals(2, state.activeReminderCountFor("t1"))
        assertEquals(1, state.activeReminderCountFor("other"))
        assertEquals(0, state.activeReminderCountFor("none"))
    }
}
