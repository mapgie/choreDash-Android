package com.mapgie.dash.ui.theme

import com.mapgie.dash.data.model.TaskDto
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The status tone a task card's spine and badge wear. Urgency drives the tone
 * while a task is open, and an open task always has one (undated counts as
 * neutral, so no card goes bare); a completed task signals nothing, whatever
 * its due date, so its spine goes transparent instead of keeping a rose
 * "overdue" bar in the Done list. Pure mapper, no Compose involved.
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
    fun `an open task with no due date keeps a quiet spine, like one due later`() {
        // Every chore card carries a spine; an undated task is "not pressing",
        // not "nothing", so it wears the neutral tone rather than going bare.
        assertEquals(StatusTone.NEUTRAL, task().statusTone())
        assertEquals(StatusTone.NEUTRAL, task(dueDate = nextWeek).statusTone())
    }

    @Test
    fun `a done task with no due date shows no tone`() {
        assertEquals(StatusTone.NONE, task(completedAt = "2026-08-01T10:00:00Z").statusTone())
    }
}
