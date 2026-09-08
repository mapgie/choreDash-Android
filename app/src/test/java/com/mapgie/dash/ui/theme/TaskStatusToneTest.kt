package com.mapgie.dash.ui.theme

import com.mapgie.dash.data.model.TaskDto
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The status tone a task card's spine and badge wear. Urgency drives the tone
 * while a task is open; a completed task signals nothing, whatever its due date,
 * so its spine goes transparent instead of keeping a rose "overdue" bar in the
 * Done list. Pure mapper, no Compose involved.
 */
class TaskStatusToneTest {

    private val yesterday = LocalDate.now().minusDays(1).toString()
    private val nextWeek = LocalDate.now().plusDays(10).toString()

    private fun task(dueDate: String? = null, completedAt: String? = null) =
        TaskDto(id = "t", title = "t", dueDate = dueDate, completedAt = completedAt)

    @Test
    fun `an open overdue task is critical`() {
        assertEquals(StatusTone.CRITICAL, task(dueDate = yesterday).statusTone())
    }

    @Test
    fun `a done task shows no tone even when it was overdue`() {
        assertEquals(StatusTone.NONE, task(dueDate = yesterday, completedAt = "2026-08-01T10:00:00Z").statusTone())
    }

    @Test
    fun `a done task with a future due date also shows no tone`() {
        assertEquals(StatusTone.NONE, task(dueDate = nextWeek, completedAt = "2026-08-01T10:00:00Z").statusTone())
    }

    @Test
    fun `a task with no due date shows no tone open or done`() {
        assertEquals(StatusTone.NONE, task().statusTone())
        assertEquals(StatusTone.NONE, task(completedAt = "2026-08-01T10:00:00Z").statusTone())
    }
}
