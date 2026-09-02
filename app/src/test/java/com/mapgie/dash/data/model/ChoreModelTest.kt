package com.mapgie.dash.data.model

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chore status, cadence pressure, and countdown logic. Timestamps are chosen
 * mid-window (36h, 300h, ...) rather than on day boundaries so the
 * now-relative arithmetic cannot flip a day during the test run.
 */
class ChoreModelTest {

    private fun chore(
        intervalDays: Double? = null,
        category: String? = null,
        lastScannedAgo: Duration? = null,
    ): Chore = Chore.from(
        tag = TagDto(
            id = "id1",
            tagId = "tag1",
            label = "label",
            category = category,
            intervalDays = intervalDays,
        ),
        lastScanned = lastScannedAgo?.let { Instant.now().minus(it) },
        lastScanId = null,
    )

    // ── Status ────────────────────────────────────────────────────────────────

    @Test
    fun `never scanned is NEVER`() {
        assertEquals(ChoreStatus.NEVER, chore(intervalDays = 10.0).status)
    }

    @Test
    fun `interval chore is fresh within half the interval`() {
        assertEquals(
            ChoreStatus.FRESH,
            chore(intervalDays = 10.0, lastScannedAgo = Duration.ofHours(36)).status,
        )
    }

    @Test
    fun `interval chore ages past the half-way threshold`() {
        assertEquals(
            ChoreStatus.AGING,
            chore(intervalDays = 10.0, lastScannedAgo = Duration.ofHours(180)).status,
        )
    }

    @Test
    fun `interval chore goes stale past the full interval`() {
        assertEquals(
            ChoreStatus.STALE,
            chore(intervalDays = 10.0, lastScannedAgo = Duration.ofHours(300)).status,
        )
    }

    @Test
    fun `category chore uses the per-category thresholds`() {
        assertEquals(
            ChoreStatus.FRESH,
            chore(category = "Kitchen", lastScannedAgo = Duration.ofHours(36)).status,
        )
        assertEquals(
            ChoreStatus.AGING,
            chore(category = "Kitchen", lastScannedAgo = Duration.ofHours(84)).status,
        )
        assertEquals(
            ChoreStatus.STALE,
            chore(category = "Kitchen", lastScannedAgo = Duration.ofHours(156)).status,
        )
    }

    // ── Cadence pressure ──────────────────────────────────────────────────────

    @Test
    fun `pressure is null before the first log`() {
        assertNull(chore(intervalDays = 10.0).pressureFraction())
    }

    @Test
    fun `pressure grows with the share of the interval elapsed`() {
        val pressure = chore(intervalDays = 10.0, lastScannedAgo = Duration.ofHours(36))
            .pressureFraction()!!
        assertEquals(0.15f, pressure, 0.02f)
    }

    @Test
    fun `pressure clamps at 1 once overdue`() {
        val pressure = chore(intervalDays = 10.0, lastScannedAgo = Duration.ofHours(300))
            .pressureFraction()!!
        assertEquals(1f, pressure, 0.0001f)
    }

    @Test
    fun `category chore pressure uses the aging window`() {
        // Kitchen ages out at 5 days; 60h elapsed is half of that window.
        val pressure = chore(category = "Kitchen", lastScannedAgo = Duration.ofHours(60))
            .pressureFraction()!!
        assertEquals(0.5f, pressure, 0.02f)
    }

    // ── Countdown text ────────────────────────────────────────────────────────

    @Test
    fun `countdown is null before the first log`() {
        assertNull(chore(intervalDays = 10.0).nextDueText())
    }

    @Test
    fun `countdown shows days until due`() {
        // Due at half the 10d interval: 36h in, 84h (3.5d) remain.
        assertEquals(
            "in 3d",
            chore(intervalDays = 10.0, lastScannedAgo = Duration.ofHours(36)).nextDueText(),
        )
    }

    @Test
    fun `countdown shows days overdue`() {
        // Due 120h after the log; 300h in means 180h (7.5d) overdue.
        assertEquals(
            "7d overdue",
            chore(intervalDays = 10.0, lastScannedAgo = Duration.ofHours(300)).nextDueText(),
        )
    }

    // ── Due badge ─────────────────────────────────────────────────────────────

    @Test
    fun `badge reads never before the first log`() {
        assertEquals("never", chore(intervalDays = 10.0).dueBadgeText())
    }

    @Test
    fun `badge counts days left against the full interval`() {
        // 10d interval, 36h in: 204h (8.5d) remain.
        assertEquals("8d left", chore(intervalDays = 10.0, lastScannedAgo = Duration.ofHours(36)).dueBadgeText())
    }

    @Test
    fun `badge counts days over once past the full interval`() {
        // 10d interval, 300h in: 60h (2.5d) over.
        assertEquals("2d over", chore(intervalDays = 10.0, lastScannedAgo = Duration.ofHours(300)).dueBadgeText())
    }

    @Test
    fun `badge falls back to hours inside the last day`() {
        // 1d interval, 12h30 in: 11h30 left, truncated to whole hours.
        assertEquals("11h left", chore(intervalDays = 1.0, lastScannedAgo = Duration.ofMinutes(12 * 60 + 30)).dueBadgeText())
    }

    @Test
    fun `category chores fall due at the aging window`() {
        // Kitchen ages out at 5 days; 36h in leaves 84h (3.5d).
        assertEquals("3d left", chore(category = "Kitchen", lastScannedAgo = Duration.ofHours(36)).dueBadgeText())
    }

    // ── Distant ───────────────────────────────────────────────────────────────

    @Test
    fun `only long-interval chores read as distant`() {
        assertTrue(chore(intervalDays = 365.0, lastScannedAgo = Duration.ofHours(36)).isDistant())
        assertFalse(chore(intervalDays = 10.0, lastScannedAgo = Duration.ofHours(36)).isDistant())
        assertFalse(chore(category = "Kitchen", lastScannedAgo = Duration.ofHours(36)).isDistant())
        assertFalse(chore(intervalDays = 365.0).isDistant())
    }
}
