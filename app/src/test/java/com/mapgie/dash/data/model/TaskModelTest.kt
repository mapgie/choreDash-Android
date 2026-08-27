package com.mapgie.dash.data.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskModelTest {

    private fun task(
        dueDate: String? = null,
        duePeriod: String? = null,
        priority: String = "normal",
    ) = TaskDto(
        id = "t1",
        title = "title",
        priority = priority,
        dueDate = dueDate,
        duePeriod = duePeriod,
    )

    private val today: LocalDate = LocalDate.now(ZoneId.systemDefault())

    @Test
    fun `due date maps to urgency buckets`() {
        assertEquals(TaskUrgency.OVERDUE, task(dueDate = today.minusDays(1).toString()).urgency())
        assertEquals(TaskUrgency.TODAY, task(dueDate = today.toString()).urgency())
        assertEquals(TaskUrgency.THIS_WEEK, task(dueDate = today.plusDays(3).toString()).urgency())
        assertEquals(TaskUrgency.LATER, task(dueDate = today.plusDays(10).toString()).urgency())
    }

    @Test
    fun `unparseable due date is NONE, not a crash`() {
        assertEquals(TaskUrgency.NONE, task(dueDate = "next tuesday").urgency())
    }

    @Test
    fun `due period maps to urgency when no date is set`() {
        assertEquals(TaskUrgency.TODAY, task(duePeriod = "today").urgency())
        assertEquals(TaskUrgency.THIS_WEEK, task(duePeriod = "this_week").urgency())
        assertEquals(TaskUrgency.LATER, task(duePeriod = "this_month").urgency())
        assertEquals(TaskUrgency.NONE, task().urgency())
    }

    @Test
    fun `a due date wins over a due period`() {
        assertEquals(
            TaskUrgency.OVERDUE,
            task(dueDate = today.minusDays(1).toString(), duePeriod = "this_month").urgency(),
        )
    }

    @Test
    fun `priority strings parse with NORMAL as the fallback`() {
        assertEquals(TaskPriority.HIGHER, task(priority = "higher").priorityEnum())
        assertEquals(TaskPriority.LOWER, task(priority = "lower").priorityEnum())
        assertEquals(TaskPriority.NORMAL, task(priority = "normal").priorityEnum())
        assertEquals(TaskPriority.NORMAL, task(priority = "URGENT!!").priorityEnum())
    }
}
