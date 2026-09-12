package com.mapgie.dash.ui.theme

import com.mapgie.dash.data.model.Severity
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.data.model.TaskDto
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The status tone a task card's spine, badge and icon chip wear. The due date
 * decides while it is close (overdue, today, this week); priority decides the
 * rest, so an open task always has a colour: fresh by default, attention for
 * high priority, the fourth "low" colour for low priority with nothing pressing.
 * A completed task signals nothing, whatever its due date, so its spine goes
 * transparent instead of keeping a rose "overdue" bar in the Done list. Pure
 * mapper, no Compose involved.
 */
class TaskStatusToneTest {

    private val yesterday = LocalDate.now().minusDays(1).toString()
    private val today = LocalDate.now().toString()
    private val inThreeDays = LocalDate.now().plusDays(3).toString()
    private val nextWeek = LocalDate.now().plusDays(10).toString()
    private val done = "2026-08-01T10:00:00Z"

    private fun task(dueDate: String? = null, priority: String = "normal", completedAt: String? = null) =
        TaskDto(id = "t", title = "t", dueDate = dueDate, priority = priority, completedAt = completedAt)

    @Test
    fun `an open overdue task is critical whatever its priority`() {
        assertEquals(StatusTone.CRITICAL, task(dueDate = yesterday).statusTone())
        assertEquals(StatusTone.CRITICAL, task(dueDate = yesterday, priority = "lower").statusTone())
        assertEquals(StatusTone.CRITICAL, task(dueDate = yesterday, priority = "higher").statusTone())
    }

    @Test
    fun `a task due today needs attention whatever its priority`() {
        assertEquals(StatusTone.ATTENTION, task(dueDate = today).statusTone())
        assertEquals(StatusTone.ATTENTION, task(dueDate = today, priority = "lower").statusTone())
    }

    @Test
    fun `a task due this week is fresh, lifted to attention by a high priority`() {
        assertEquals(StatusTone.OK, task(dueDate = inThreeDays).statusTone())
        assertEquals(StatusTone.OK, task(dueDate = inThreeDays, priority = "lower").statusTone())
        assertEquals(StatusTone.ATTENTION, task(dueDate = inThreeDays, priority = "higher").statusTone())
    }

    @Test
    fun `an open task with nothing pressing is fresh by default, never bare`() {
        assertEquals(StatusTone.OK, task().statusTone())
        assertEquals(StatusTone.OK, task(dueDate = nextWeek).statusTone())
    }

    @Test
    fun `a high priority task with nothing pressing needs attention`() {
        assertEquals(StatusTone.ATTENTION, task(priority = "higher").statusTone())
        assertEquals(StatusTone.ATTENTION, task(dueDate = nextWeek, priority = "higher").statusTone())
    }

    @Test
    fun `a low priority task with nothing pressing wears the low tone`() {
        assertEquals(StatusTone.LOW, task(priority = "lower").statusTone())
        assertEquals(StatusTone.LOW, task(dueDate = nextWeek, priority = "lower").statusTone())
    }

    @Test
    fun `a done task shows no tone even when it was overdue or high priority`() {
        assertEquals(StatusTone.NONE, task(dueDate = yesterday, completedAt = done).statusTone())
        assertEquals(StatusTone.NONE, task(dueDate = nextWeek, completedAt = done).statusTone())
        assertEquals(StatusTone.NONE, task(priority = "higher", completedAt = done).statusTone())
        assertEquals(StatusTone.NONE, task(completedAt = done).statusTone())
    }

    // ── Severity colours ──────────────────────────────────────────────────────

    @Test
    fun `each signalling tone has its own Settings row and the quiet tones have none`() {
        assertEquals(Severity.OVERDUE, StatusTone.CRITICAL.severity)
        assertEquals(Severity.DUE_SOON, StatusTone.ATTENTION.severity)
        assertEquals(Severity.FRESH, StatusTone.OK.severity)
        assertEquals(Severity.LOW, StatusTone.LOW.severity)
        assertNull(StatusTone.NEUTRAL.severity)
        assertNull(StatusTone.NONE.severity)
        Severity.entries.forEach { assertEquals(it, it.tone().severity) }
    }

    @Test
    fun `the low tone wears blue by default`() {
        assertEquals(Swatch.BLUE, SeverityColors().swatchFor(StatusTone.LOW))
    }

    @Test
    fun `a severity set to none gives its tone no swatch, like a quiet tone`() {
        val colours = SeverityColors(Severity.defaults + (Severity.FRESH to null))
        assertNull(colours.swatchFor(StatusTone.OK))
        assertNull(colours.swatchFor(StatusTone.NEUTRAL))
        assertEquals(Swatch.ROSE, colours.swatchFor(StatusTone.CRITICAL))
    }
}
