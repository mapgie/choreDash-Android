package com.mapgie.dash.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The edit sheet's time row: a once-only memo's ring rolls forward to the next
 * occurrence of the picked time instead of landing in the past.
 */
class ReminderPickedTimeTest {

    private val zone = ZoneId.of("Europe/London")
    // Friday 25 September 2026, 18:00 local.
    private val now = ZonedDateTime.of(2026, 9, 25, 18, 0, 0, 0, zone).toInstant()

    private fun at(date: LocalDate, hour: Int, minute: Int) =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), zone)

    private val yesterday = LocalDate.of(2026, 9, 24)
    private val today = LocalDate.of(2026, 9, 25)
    private val tomorrow = LocalDate.of(2026, 9, 26)
    private val nextTuesday = LocalDate.of(2026, 9, 29)

    @Test
    fun `a time still ahead today on yesterday's memo rings today`() {
        val ring = ringForPickedTime(at(yesterday, 22, 33), LocalTime.of(18, 35), now)
        assertEquals(at(today, 18, 35), ring)
    }

    @Test
    fun `a time already gone today rings tomorrow`() {
        val ring = ringForPickedTime(at(yesterday, 22, 33), LocalTime.of(7, 0), now)
        assertEquals(at(tomorrow, 7, 0), ring)
    }

    @Test
    fun `a memo set for a future day keeps its day when the time changes`() {
        val ring = ringForPickedTime(at(nextTuesday, 9, 0), LocalTime.of(7, 30), now)
        assertEquals(at(nextTuesday, 7, 30), ring)
    }

    @Test
    fun `a time later today on today's memo stays today`() {
        val ring = ringForPickedTime(at(today, 12, 0), LocalTime.of(21, 15), now)
        assertEquals(at(today, 21, 15), ring)
    }

    @Test
    fun `picking the current minute rolls to tomorrow rather than ringing in the past`() {
        val ring = ringForPickedTime(at(today, 12, 0), LocalTime.of(18, 0), now)
        assertEquals(at(tomorrow, 18, 0), ring)
    }

    @Test
    fun `a ring still ahead can be saved`() {
        val opened = at(yesterday, 22, 33).toInstant()
        assertTrue(onceOnlyRingSavable(at(today, 18, 35).toInstant(), opened, now))
    }

    @Test
    fun `a ring moved into the past cannot be saved`() {
        val opened = at(tomorrow, 9, 0).toInstant()
        assertFalse(onceOnlyRingSavable(at(yesterday, 9, 0).toInstant(), opened, now))
    }

    @Test
    fun `a memo that has already rung can still be saved with its ring unchanged`() {
        val opened = at(yesterday, 22, 33).toInstant()
        assertTrue(onceOnlyRingSavable(opened, opened, now))
    }
}
