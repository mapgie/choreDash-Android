package com.mapgie.dash.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The three-state owner scope shared by the Chores and Tasks headers. Each
 * test name states one guaranteed behaviour; together they are the feature
 * list for the owner filter.
 */
class OwnerFilterTest {

    private val me = "ana"
    private val other = "mo"

    // ── Mine ──────────────────────────────────────────────────────────────────

    @Test
    fun `mine keeps only items assigned to me`() {
        assertTrue(OwnerFilter.MINE.matches(me, me))
        assertFalse(OwnerFilter.MINE.matches(other, me))
    }

    @Test
    fun `mine excludes unassigned items`() {
        assertFalse(OwnerFilter.MINE.matches(null, me))
    }

    // ── Mine and unassigned ───────────────────────────────────────────────────

    @Test
    fun `mine and unassigned keeps my items and unclaimed items`() {
        assertTrue(OwnerFilter.MINE_AND_UNASSIGNED.matches(me, me))
        assertTrue(OwnerFilter.MINE_AND_UNASSIGNED.matches(null, me))
    }

    @Test
    fun `mine and unassigned excludes other people's items`() {
        assertFalse(OwnerFilter.MINE_AND_UNASSIGNED.matches(other, me))
    }

    // ── Everyone ──────────────────────────────────────────────────────────────

    @Test
    fun `everyone keeps every item`() {
        assertTrue(OwnerFilter.EVERYONE.matches(me, me))
        assertTrue(OwnerFilter.EVERYONE.matches(other, me))
        assertTrue(OwnerFilter.EVERYONE.matches(null, me))
    }

    // ── No handle configured ─────────────────────────────────────────────────

    @Test
    fun `without an owner handle every state shows everything`() {
        for (state in OwnerFilter.entries) {
            assertTrue("$state / mine", state.matches(me, ""))
            assertTrue("$state / other", state.matches(other, "   "))
            assertTrue("$state / unassigned", state.matches(null, ""))
        }
    }

    // ── Cycling ───────────────────────────────────────────────────────────────

    @Test
    fun `tapping cycles mine, mine and unassigned, everyone, then wraps`() {
        assertEquals(OwnerFilter.MINE_AND_UNASSIGNED, OwnerFilter.MINE.next)
        assertEquals(OwnerFilter.EVERYONE, OwnerFilter.MINE_AND_UNASSIGNED.next)
        assertEquals(OwnerFilter.MINE, OwnerFilter.EVERYONE.next)
    }

    @Test
    fun `three taps return to the starting state`() {
        for (start in OwnerFilter.entries) {
            assertEquals(start, start.next.next.next)
        }
    }

    // ── Avatar visibility ─────────────────────────────────────────────────────

    @Test
    fun `owner avatars show only when everyone is listed`() {
        assertTrue(OwnerFilter.EVERYONE.showsOwner)
        assertFalse(OwnerFilter.MINE.showsOwner)
        assertFalse(OwnerFilter.MINE_AND_UNASSIGNED.showsOwner)
    }
}
