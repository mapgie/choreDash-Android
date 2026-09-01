package com.mapgie.dash.ui.screens.chores

import com.mapgie.dash.data.model.CadenceBucket
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.OwnerFilter
import com.mapgie.dash.data.model.TagDto
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the Chores list shows for a given [ChoreUiState]: owner scope, the
 * All / Overdue / Soon chips, the hidden (distant) section, swipe-to-snooze,
 * and the sort orders. Pure state logic, no ViewModel or Android involved.
 *
 * Timestamps sit mid-window (36h, 300h, ...) so day arithmetic cannot flip
 * a bucket while the test runs.
 */
class ChoreUiStateTest {

    private val me = "ana"

    private fun chore(
        id: String,
        owner: String? = null,
        intervalDays: Double = 10.0,
        lastScannedAgo: Duration? = Duration.ofHours(36),
    ): Chore = Chore.from(
        tag = TagDto(id = id, tagId = "tag-$id", label = id, owner = owner, intervalDays = intervalDays),
        lastScanned = lastScannedAgo?.let { Instant.now().minus(it) },
        lastScanId = null,
    )

    private fun ids(chores: List<Chore>) = chores.map { it.id }

    // Fresh: scanned 36h ago on a 10d cadence. Aging: 180h ago. Stale: 300h ago.
    private val fresh = chore("fresh", lastScannedAgo = Duration.ofHours(36))
    private val aging = chore("aging", lastScannedAgo = Duration.ofHours(180))
    private val stale = chore("stale", lastScannedAgo = Duration.ofHours(300))
    private val never = chore("never", lastScannedAgo = null)

    // ── Owner scope ───────────────────────────────────────────────────────────

    private val mine = chore("mine", owner = me)
    private val theirs = chore("theirs", owner = "mo")
    private val unassigned = chore("unassigned", owner = null)
    private val byOwner = listOf(mine, theirs, unassigned)

    @Test
    fun `everyone lists every owner`() {
        val state = ChoreUiState(active = byOwner, ownerHandle = me, ownerFilter = OwnerFilter.EVERYONE)
        assertEquals(listOf("mine", "theirs", "unassigned"), ids(state.displayed))
    }

    @Test
    fun `mine lists only my chores`() {
        val state = ChoreUiState(active = byOwner, ownerHandle = me, ownerFilter = OwnerFilter.MINE)
        assertEquals(listOf("mine"), ids(state.displayed))
    }

    @Test
    fun `mine and unassigned lists my chores and unclaimed ones`() {
        val state = ChoreUiState(active = byOwner, ownerHandle = me, ownerFilter = OwnerFilter.MINE_AND_UNASSIGNED)
        assertEquals(listOf("mine", "unassigned"), ids(state.displayed))
    }

    @Test
    fun `owner scope also applies to the hidden section`() {
        // Due in ~364 days: distant, so it lands in the hidden section.
        val distantTheirs = chore("distant-theirs", owner = "mo", intervalDays = 365.0, lastScannedAgo = Duration.ofHours(36))
        val distantMine = chore("distant-mine", owner = me, intervalDays = 365.0, lastScannedAgo = Duration.ofHours(36))
        val state = ChoreUiState(
            active = listOf(distantTheirs, distantMine),
            ownerHandle = me,
            ownerFilter = OwnerFilter.MINE,
        )
        assertEquals(listOf("distant-mine"), ids(state.hiddenChores))
        assertTrue(state.displayed.isEmpty())
    }

    // ── Status chips ──────────────────────────────────────────────────────────

    private val byStatus = listOf(fresh, aging, stale, never)

    @Test
    fun `all chip shows every status`() {
        val state = ChoreUiState(active = byStatus, filter = ChoreFilter.ALL)
        assertEquals(listOf("fresh", "aging", "stale", "never"), ids(state.displayed))
    }

    @Test
    fun `overdue chip shows stale and never-done chores only`() {
        val state = ChoreUiState(active = byStatus, filter = ChoreFilter.OVERDUE)
        assertEquals(listOf("stale", "never"), ids(state.displayed))
        assertTrue(state.displayed.all { it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER })
    }

    @Test
    fun `soon chip shows aging chores only`() {
        val state = ChoreUiState(active = byStatus, filter = ChoreFilter.SOON)
        assertEquals(listOf("aging"), ids(state.displayed))
    }

    // ── Hidden (distant) section ──────────────────────────────────────────────

    @Test
    fun `distant chores leave the main list for the hidden section`() {
        val distant = chore("distant", intervalDays = 365.0, lastScannedAgo = Duration.ofHours(36))
        val state = ChoreUiState(active = listOf(fresh, distant))
        assertEquals(listOf("fresh"), ids(state.displayed))
        assertEquals(listOf("distant"), ids(state.hiddenChores))
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    fun `zen ascending puts never-done first then oldest scan`() {
        val state = ChoreUiState(active = listOf(fresh, stale, never), zenMode = true, zenSortAscending = true)
        assertEquals(listOf("never", "stale", "fresh"), ids(state.displayed))
    }

    @Test
    fun `zen descending puts most recent scan first and never-done last`() {
        val state = ChoreUiState(active = listOf(never, stale, fresh), zenMode = true, zenSortAscending = false)
        assertEquals(listOf("fresh", "stale", "never"), ids(state.displayed))
    }

    @Test
    fun `due countdown surfaces overdue chores to the top`() {
        val state = ChoreUiState(active = listOf(fresh, aging, stale, never), showDueCountdown = true)
        val shown = ids(state.displayed)
        assertEquals(setOf("never", "stale"), shown.take(2).toSet())
        assertEquals(listOf("aging", "fresh"), shown.drop(2))
    }

    @Test
    fun `without zen or countdown the load order is kept`() {
        val state = ChoreUiState(active = listOf(stale, fresh, never, aging))
        assertEquals(listOf("stale", "fresh", "never", "aging"), ids(state.displayed))
    }

    // ── Swipe-to-snooze ───────────────────────────────────────────────────────

    private val inFuture = Instant.now().plus(Duration.ofHours(36))
    private val inPast = Instant.now().minus(Duration.ofHours(36))

    @Test
    fun `a snoozed chore leaves the main list for the hidden section`() {
        val state = ChoreUiState(active = listOf(fresh, stale), snoozes = mapOf(stale.tagId to inFuture))
        assertEquals(listOf("fresh"), ids(state.displayed))
        assertEquals(listOf("stale"), ids(state.hiddenChores))
        assertEquals(inFuture, state.snoozedUntil(stale))
    }

    @Test
    fun `an expired snooze shows the chore as normal`() {
        val state = ChoreUiState(active = listOf(stale), snoozes = mapOf(stale.tagId to inPast))
        assertEquals(listOf("stale"), ids(state.displayed))
        assertTrue(state.hiddenChores.isEmpty())
        assertEquals(null, state.snoozedUntil(stale))
    }

    @Test
    fun `snoozed count excludes chores hidden only by lead time`() {
        val distant = chore("distant", intervalDays = 365.0, lastScannedAgo = Duration.ofHours(36))
        val state = ChoreUiState(active = listOf(distant, stale), snoozes = mapOf(stale.tagId to inFuture))
        assertEquals(setOf("distant", "stale"), ids(state.hiddenChores).toSet())
        assertEquals(1, state.snoozedCount)
    }

    @Test
    fun `snooze lasts the cadence lead time when smart visibility is on`() {
        // 7d cadence = WEEKLY bucket; default lead 3 days, overridden here to 4.
        val weekly = chore("weekly", intervalDays = 7.0)
        val state = ChoreUiState(smartVisibility = true, choreLeadDays = mapOf(CadenceBucket.WEEKLY to 4))
        assertEquals(Duration.ofDays(4), state.snoozeDurationFor(weekly))
    }

    @Test
    fun `snooze falls back to the bucket default lead time when none is configured`() {
        val weekly = chore("weekly", intervalDays = 7.0)
        val state = ChoreUiState(smartVisibility = true)
        assertEquals(Duration.ofDays(CadenceBucket.WEEKLY.defaultLeadDays.toLong()), state.snoozeDurationFor(weekly))
    }

    @Test
    fun `snooze lasts one day when smart visibility is off`() {
        val weekly = chore("weekly", intervalDays = 7.0)
        val state = ChoreUiState(smartVisibility = false, choreLeadDays = mapOf(CadenceBucket.WEEKLY to 4))
        assertEquals(Duration.ofDays(1), state.snoozeDurationFor(weekly))
    }

    @Test
    fun `snooze lasts one day when the lead time is zero`() {
        val daily = chore("daily", intervalDays = 1.0)
        val state = ChoreUiState(smartVisibility = true, choreLeadDays = mapOf(CadenceBucket.DAILY to 0))
        assertEquals(Duration.ofDays(1), state.snoozeDurationFor(daily))
    }
}
