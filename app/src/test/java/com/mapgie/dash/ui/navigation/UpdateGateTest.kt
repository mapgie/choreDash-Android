package com.mapgie.dash.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When the "What's New" changelog shows on open. Only a genuine in-place update
 * from a previously recorded version qualifies: a fresh install (last seen 0)
 * gets the welcome sheet instead, and the same or an older version shows nothing.
 * Pure logic, no Android involved.
 */
class UpdateGateTest {

    @Test
    fun `fresh install does not show whats new`() {
        assertFalse(showWhatsNewOnUpdate(lastSeen = 0, current = 60))
    }

    @Test
    fun `update from an earlier version shows whats new`() {
        assertTrue(showWhatsNewOnUpdate(lastSeen = 59, current = 60))
    }

    @Test
    fun `reopening the same version shows nothing`() {
        assertFalse(showWhatsNewOnUpdate(lastSeen = 60, current = 60))
    }

    @Test
    fun `a downgrade shows nothing`() {
        assertFalse(showWhatsNewOnUpdate(lastSeen = 61, current = 60))
    }

    @Test
    fun `a jump across several versions still shows whats new`() {
        assertTrue(showWhatsNewOnUpdate(lastSeen = 40, current = 60))
    }
}
