package com.mapgie.dash.ui.screens.reminder

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The copy on the reminder view: header clock, "due today · about 5 minutes"
 * meta line, and the "next:" footer. Pure functions over fixed instants, so the
 * clock and zone are pinned; offsets sit mid-window (5m30s, 14h30m) so unit
 * truncation cannot flip a boundary while the test runs.
 */
class ReminderViewTextTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    // A Friday, 3pm UTC.
    private val now: Instant = Instant.parse("2026-07-10T15:00:00Z")

    private fun at(offset: Duration): Instant = now.plus(offset)

    @Test
    fun `header shows the reminder clock as 6 00 PM`() {
        assertEquals("6:00 PM", ReminderViewText.headerTime(Instant.parse("2026-07-10T18:00:00Z"), zone))
        assertEquals("9:05 AM", ReminderViewText.headerTime(Instant.parse("2026-07-10T09:05:00Z"), zone))
    }

    @Test
    fun `footer clock is lowercase without a space`() {
        assertEquals("4:30pm", ReminderViewText.footerTime(Instant.parse("2026-07-10T16:30:00Z"), zone))
    }

    @Test
    fun `a nudge five minutes out reads due today about 5 minutes`() {
        val remindAt = at(Duration.ofMinutes(5).plusSeconds(30))
        assertEquals("due today · about 5 minutes", ReminderViewText.metaLine(remindAt, now, zone))
    }

    @Test
    fun `a nudge past midnight reads due tomorrow in N hours`() {
        val remindAt = at(Duration.ofHours(14).plusMinutes(30))
        assertEquals("due tomorrow · in 14 hours", ReminderViewText.metaLine(remindAt, now, zone))
    }

    @Test
    fun `a nudge that already fired reads minutes ago`() {
        val remindAt = at(Duration.ofMinutes(-20).minusSeconds(30))
        assertEquals("due today · 20 minutes ago", ReminderViewText.metaLine(remindAt, now, zone))
    }

    @Test
    fun `single units are not pluralised`() {
        assertEquals("about 1 minute", ReminderViewText.relativeToNow(at(Duration.ofSeconds(90)), now))
        assertEquals("in 1 hour", ReminderViewText.relativeToNow(at(Duration.ofMinutes(90)), now))
        assertEquals("1 hour ago", ReminderViewText.relativeToNow(at(Duration.ofMinutes(-90)), now))
        assertEquals("1 day ago", ReminderViewText.relativeToNow(at(Duration.ofHours(-36)), now))
    }

    @Test
    fun `within a minute either side reads right now or just now`() {
        assertEquals("right now", ReminderViewText.relativeToNow(at(Duration.ofSeconds(20)), now))
        assertEquals("just now", ReminderViewText.relativeToNow(at(Duration.ofSeconds(-20)), now))
    }

    @Test
    fun `days further out fall back to the date`() {
        val remindAt = at(Duration.ofHours(60))
        assertEquals("due 13 Jul · in 2 days", ReminderViewText.metaLine(remindAt, now, zone))
        assertEquals("due yesterday", ReminderViewText.dueDay(at(Duration.ofHours(-30)), now, zone))
    }

    @Test
    fun `next line names the earliest other pending nudge`() {
        val current = UpcomingNudge(ReminderViewKind.REMINDER, "r1", "Pay electricity bill", at(Duration.ofMinutes(5)))
        val later = UpcomingNudge(ReminderViewKind.TASK, "t9", "Book dentist", at(Duration.ofHours(5)))
        val soon = UpcomingNudge(ReminderViewKind.REMINDER, "r2", "Pick up prescription", at(Duration.ofMinutes(90)))
        val next = ReminderViewText.nextAfter(current.kind, current.id, listOf(later, current, soon))
        assertEquals(soon, next)
        assertEquals("next: Pick up prescription · 4:30pm", ReminderViewText.nextLine(next, zone))
    }

    @Test
    fun `next line only excludes the same kind and id`() {
        // A task and a reminder can share an id string; only the exact pair is the current item.
        val reminder = UpcomingNudge(ReminderViewKind.REMINDER, "same", "Reminder", at(Duration.ofHours(1)))
        val task = UpcomingNudge(ReminderViewKind.TASK, "same", "Task", at(Duration.ofHours(2)))
        assertEquals(task, ReminderViewText.nextAfter(ReminderViewKind.REMINDER, "same", listOf(reminder, task)))
    }

    @Test
    fun `nothing else scheduled when no other nudge is pending`() {
        val current = UpcomingNudge(ReminderViewKind.TASK, "t1", "Only one", at(Duration.ofMinutes(5)))
        assertNull(ReminderViewText.nextAfter(current.kind, current.id, listOf(current)))
        assertEquals("nothing else scheduled", ReminderViewText.nextLine(null, zone))
    }
}
