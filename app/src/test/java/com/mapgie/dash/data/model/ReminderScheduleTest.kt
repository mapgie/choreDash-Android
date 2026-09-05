package com.mapgie.dash.data.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The words and buckets a memo's schedule produces: its next occurrence, the
 * card's meta line and badge, the sheet's banner, and which urgency bucket it
 * sits in. Pure Kotlin, fixed clock, fixed zone. "Now" is Friday 10 July 2026
 * at 09:30 UTC; the ring times sit mid-window so no bucket can flip mid-run.
 */
class ReminderScheduleTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: Instant = Instant.parse("2026-07-10T09:30:00Z") // Friday

    private val monWedFri = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    private val weekdays = RepeatPreset.WEEKDAYS_ONLY.days

    private fun memo(
        remindAt: String,
        repeatDays: List<String> = emptyList(),
        reminded: Boolean = false,
        completedAt: String? = null,
        lastRangAt: String? = null,
        acknowledgedAt: String? = null,
    ) = ReminderDto(
        id = "m1", subject = "Water the plants", remindAt = remindAt,
        repeatDays = repeatDays, reminded = reminded, completedAt = completedAt,
        lastRangAt = lastRangAt, acknowledgedAt = acknowledgedAt,
    )

    private fun names(days: Set<DayOfWeek>) = days.map { it.name }

    // ── Next occurrence ───────────────────────────────────────────────────────

    @Test
    fun `next occurrence later the same day counts when the time has not passed`() {
        val next = nextOccurrence(now, LocalTime.of(19, 0), monWedFri, zone)
        assertEquals(Instant.parse("2026-07-10T19:00:00Z"), next)
    }

    @Test
    fun `next occurrence skips today once its time has passed`() {
        val next = nextOccurrence(now, LocalTime.of(7, 0), monWedFri, zone)
        assertEquals(Instant.parse("2026-07-13T07:00:00Z"), next) // Monday
    }

    @Test
    fun `next occurrence wraps to the same weekday next week for a one-day schedule`() {
        val next = nextOccurrence(now, LocalTime.of(7, 0), setOf(DayOfWeek.FRIDAY), zone)
        assertEquals(Instant.parse("2026-07-17T07:00:00Z"), next)
    }

    @Test
    fun `next occurrence is null without any repeat day`() {
        assertNull(nextOccurrence(now, LocalTime.of(7, 0), emptySet(), zone))
    }

    // ── Days label ────────────────────────────────────────────────────────────

    @Test
    fun `preset day sets read as their chip label`() {
        assertEquals("Weekdays", ReminderScheduleText.daysLabel(weekdays))
        assertEquals("Weekends", ReminderScheduleText.daysLabel(RepeatPreset.WEEKENDS_ONLY.days))
        assertEquals("Every day", ReminderScheduleText.daysLabel(DayOfWeek.values().toSet()))
    }

    @Test
    fun `other day sets list the days Monday first`() {
        assertEquals("Mon · Wed · Fri", ReminderScheduleText.daysLabel(monWedFri))
        assertEquals("Tue · Sun", ReminderScheduleText.daysLabel(setOf(DayOfWeek.SUNDAY, DayOfWeek.TUESDAY)))
    }

    // ── Schedule line (card meta) ─────────────────────────────────────────────

    @Test
    fun `once-only memo reads Once, doesn't repeat`() {
        assertEquals("Once · doesn't repeat", ReminderScheduleText.scheduleLine(memo("2026-07-11T10:00:00Z"), zone))
    }

    @Test
    fun `repeating memo reads its days and time`() {
        val m = memo("2026-07-10T20:00:00Z", names(weekdays))
        assertEquals("Weekdays · 8:00 PM", ReminderScheduleText.scheduleLine(m, zone))
        val m2 = memo("2026-07-13T07:00:00Z", names(monWedFri))
        assertEquals("Mon · Wed · Fri · 7:00 AM", ReminderScheduleText.scheduleLine(m2, zone))
    }

    @Test
    fun `a linked memo says what it hangs off`() {
        val m = memo("2026-07-10T20:00:00Z", names(weekdays))
        assertEquals("Weekdays · 8:00 PM · linked to chore", ReminderScheduleText.scheduleLine(m, zone, linkedTo = "chore"))
    }

    // ── Status bucket ─────────────────────────────────────────────────────────

    @Test
    fun `a ring nobody answered is RANG`() {
        val once = memo("2026-07-10T07:30:00Z", reminded = true, lastRangAt = "2026-07-10T07:30:00Z")
        assertEquals(ReminderStatus.RANG, ReminderScheduleText.status(once, now))
        val repeating = memo("2026-07-13T07:00:00Z", names(monWedFri), lastRangAt = "2026-07-10T07:00:00Z")
        assertEquals(ReminderStatus.RANG, ReminderScheduleText.status(repeating, now))
    }

    @Test
    fun `a missed once-only ring the device slept through is RANG`() {
        assertEquals(ReminderStatus.RANG, ReminderScheduleText.status(memo("2026-07-10T06:30:00Z"), now))
    }

    @Test
    fun `within 24 hours is DUE_SOON, beyond it UPCOMING`() {
        assertEquals(ReminderStatus.DUE_SOON, ReminderScheduleText.status(memo("2026-07-10T21:30:00Z"), now))
        assertEquals(ReminderStatus.UPCOMING, ReminderScheduleText.status(memo("2026-07-11T21:30:00Z"), now))
    }

    @Test
    fun `an acknowledged repeating ring is judged by its next ring`() {
        val m = memo(
            "2026-07-13T07:00:00Z", names(monWedFri),
            lastRangAt = "2026-07-10T07:00:00Z", acknowledgedAt = "2026-07-10T07:05:00Z",
        )
        assertEquals(ReminderStatus.UPCOMING, ReminderScheduleText.status(m, now))
    }

    @Test
    fun `a completed once-only memo is DONE`() {
        val m = memo("2026-07-10T07:30:00Z", reminded = true, completedAt = "2026-07-10T08:00:00Z")
        assertEquals(ReminderStatus.DONE, ReminderScheduleText.status(m, now))
    }

    // ── Badge ─────────────────────────────────────────────────────────────────

    @Test
    fun `badge says how long ago an unanswered ring was`() {
        val m = memo("2026-07-13T07:00:00Z", names(monWedFri), lastRangAt = "2026-07-10T07:30:00Z")
        assertEquals("rang 2h ago", ReminderScheduleText.nextRingBadge(m, now, zone))
    }

    @Test
    fun `badge names today, tomorrow, the weekday within a week, then the date`() {
        assertEquals("rings today 8 PM", ReminderScheduleText.nextRingBadge(memo("2026-07-10T20:00:00Z"), now, zone))
        assertEquals("rings tomorrow 9 AM", ReminderScheduleText.nextRingBadge(memo("2026-07-11T09:00:00Z"), now, zone))
        assertEquals("rings Wed 7 AM", ReminderScheduleText.nextRingBadge(memo("2026-07-15T07:00:00Z"), now, zone))
        assertEquals("rings Thu 7:30 AM", ReminderScheduleText.nextRingBadge(memo("2026-07-16T07:30:00Z"), now, zone))
        assertEquals("rings 12 Oct 9 AM", ReminderScheduleText.nextRingBadge(memo("2026-10-12T09:00:00Z"), now, zone))
    }

    @Test
    fun `badge says due when a once-only ring was missed and done once completed`() {
        assertEquals("due 3h ago", ReminderScheduleText.nextRingBadge(memo("2026-07-10T06:30:00Z"), now, zone))
        val done = memo("2026-07-10T06:30:00Z", reminded = true, completedAt = "2026-07-10T07:00:00Z")
        assertEquals("done", ReminderScheduleText.nextRingBadge(done, now, zone))
    }

    // ── Sheet banner ──────────────────────────────────────────────────────────

    @Test
    fun `banner shows weekday and full time with a relative distance`() {
        val at = Instant.parse("2026-07-15T07:00:00Z")
        assertEquals("Wed, 7:00 AM", ReminderScheduleText.bannerWhen(at, zone))
        assertEquals("in 4 days", ReminderScheduleText.bannerRelative(at, now))
        assertEquals("in 3 hours", ReminderScheduleText.bannerRelative(Instant.parse("2026-07-10T12:45:00Z"), now))
        assertEquals("in 20 minutes", ReminderScheduleText.bannerRelative(Instant.parse("2026-07-10T09:50:00Z"), now))
    }
}
