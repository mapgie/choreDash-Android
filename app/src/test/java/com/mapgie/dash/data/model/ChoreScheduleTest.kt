package com.mapgie.dash.data.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chores with a due date and a calendar repeat ("house insurance, due 1 Oct,
 * every year"). The next-due rule is tested on fixed dates; the status and
 * badge tests sit their dates days away from today so a run cannot straddle
 * a boundary.
 */
class ChoreScheduleTest {

    private val insuranceDue = LocalDate.of(2026, 10, 1)
    private val yearly = ChoreRepeat(1, RepeatUnit.YEAR)

    // ── The next due date ────────────────────────────────────────────────────

    @Test
    fun `a dated chore that was never logged is due on its date`() {
        assertEquals(insuranceDue, nextChoreDueDate(insuranceDue, yearly, lastLog = null))
    }

    @Test
    fun `logging a yearly chore a week early moves it to next year`() {
        assertEquals(
            LocalDate.of(2027, 10, 1),
            nextChoreDueDate(insuranceDue, yearly, lastLog = LocalDate.of(2026, 9, 24)),
        )
    }

    @Test
    fun `logging a yearly chore a few days late also moves it to next year`() {
        assertEquals(
            LocalDate.of(2027, 10, 1),
            nextChoreDueDate(insuranceDue, yearly, lastLog = LocalDate.of(2026, 10, 5)),
        )
    }

    @Test
    fun `a log from long before the date does not tick it off`() {
        assertEquals(insuranceDue, nextChoreDueDate(insuranceDue, yearly, lastLog = LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun `a yearly date keeps its day across the years`() {
        assertEquals(
            LocalDate.of(2029, 10, 1),
            nextChoreDueDate(insuranceDue, yearly, lastLog = LocalDate.of(2028, 9, 30)),
        )
    }

    @Test
    fun `a monthly date on the 31st stays on the month end`() {
        val rent = LocalDate.of(2026, 1, 31)
        val monthly = ChoreRepeat(1, RepeatUnit.MONTH)
        assertEquals(LocalDate.of(2026, 2, 28), nextChoreDueDate(rent, monthly, lastLog = LocalDate.of(2026, 1, 30)))
        assertEquals(LocalDate.of(2026, 3, 31), nextChoreDueDate(rent, monthly, lastLog = LocalDate.of(2026, 2, 28)))
    }

    // ── Reading the repeat back ──────────────────────────────────────────────

    @Test
    fun `a yearly repeat is stored as 365 days and read back as one year`() {
        assertEquals(365.0, yearly.intervalDays, 0.0)
        assertEquals(yearly, ChoreRepeat.from(365.0, RepeatUnit.fromWire("year")))
        assertEquals("yearly", yearly.shortLabel())
    }

    @Test
    fun `a chore saved before repeat units existed reads as days`() {
        assertEquals(ChoreRepeat(7, RepeatUnit.DAY), ChoreRepeat.from(7.0, RepeatUnit.fromWire(null)))
        assertEquals("every 7d", ChoreRepeat(7, RepeatUnit.DAY).shortLabel())
    }

    @Test
    fun `an interval edited to a non-multiple of its unit falls back to days`() {
        assertEquals(ChoreRepeat(45, RepeatUnit.DAY), ChoreRepeat.from(45.0, RepeatUnit.MONTH))
    }

    @Test
    fun `days are written as a null repeat unit`() {
        assertNull(RepeatUnit.DAY.wire)
        assertEquals(listOf("week", "month", "year"), RepeatUnit.storedValues)
    }

    // ── Status and badge ─────────────────────────────────────────────────────

    private fun dated(dueInDays: Long, repeat: ChoreRepeat? = yearly, lastLoggedAgo: Duration? = null): Chore =
        Chore.from(
            tag = TagDto(
                id = "id1",
                tagId = "tag1",
                label = "House insurance",
                intervalDays = repeat?.intervalDays,
                repeatUnit = repeat?.unit?.wire,
                dueDate = LocalDate.now().plusDays(dueInDays).toString(),
            ),
            lastScanned = lastLoggedAgo?.let { Instant.now().minus(it) },
            lastScanId = null,
        )

    @Test
    fun `a dated chore far from its date is fresh, not never-done`() {
        val chore = dated(dueInDays = 30)
        assertEquals(ChoreStatus.FRESH, chore.status)
        assertEquals("30d left", chore.dueBadgeText())
    }

    @Test
    fun `a dated chore within a week of its date is due soon`() {
        assertEquals(ChoreStatus.AGING, dated(dueInDays = 3).status)
    }

    @Test
    fun `a dated chore is overdue on its date and after`() {
        assertEquals(ChoreStatus.STALE, dated(dueInDays = 0).status)
        assertEquals("due today", dated(dueInDays = 0).dueBadgeText())
        assertEquals(ChoreStatus.STALE, dated(dueInDays = -4).status)
        assertEquals("4d over", dated(dueInDays = -4).dueBadgeText())
    }

    @Test
    fun `logging an overdue yearly chore makes it fresh until next year`() {
        val chore = dated(dueInDays = -4, lastLoggedAgo = Duration.ofHours(1))
        assertEquals(ChoreStatus.FRESH, chore.status)
        assertTrue(chore.nextDueDate!!.isAfter(LocalDate.now().plusDays(300)))
    }

    @Test
    fun `there are no one-off chores, so a due date without a repeat is ignored`() {
        // Timed from its last log like any other chore: a category chore logged 36h ago.
        val chore = dated(dueInDays = -4, repeat = null, lastLoggedAgo = Duration.ofHours(36))
        assertNull(chore.nextDueDate)
        assertEquals(ChoreStatus.FRESH, chore.status)
        assertEquals(dated(dueInDays = 400, repeat = null, lastLoggedAgo = Duration.ofHours(36)).dueBadgeText(), chore.dueBadgeText())
    }

    @Test
    fun `a due date without a repeat and no log reads never done`() {
        assertEquals(ChoreStatus.NEVER, dated(dueInDays = 3, repeat = null).status)
    }

    @Test
    fun `a dated chore more than 60 days out is distant`() {
        assertTrue(dated(dueInDays = 90).isDistant())
        assertTrue(!dated(dueInDays = 20).isDistant())
    }
}
