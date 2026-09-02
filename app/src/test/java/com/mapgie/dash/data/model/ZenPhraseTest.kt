package com.mapgie.dash.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ZenPhraseTest {

    @Test
    fun `a stale chore is nudged for when you're up`() {
        assertEquals("kitchen · when you're up", ZenPhrase.forChore("Kitchen", ChoreStatus.STALE, done = false))
    }

    @Test
    fun `an aging chore is this week and a fresh one is anytime`() {
        assertEquals("bathroom · this week", ZenPhrase.forChore("Bathroom", ChoreStatus.AGING, done = false))
        assertEquals("bathroom · anytime", ZenPhrase.forChore("Bathroom", ChoreStatus.FRESH, done = false))
    }

    @Test
    fun `a chore never done is whenever`() {
        assertEquals("whenever", ZenPhrase.forChore(null, ChoreStatus.NEVER, done = false))
    }

    @Test
    fun `a chore logged in zen reads done nice whatever its status`() {
        assertEquals("outdoor · done, nice", ZenPhrase.forChore("Outdoor", ChoreStatus.STALE, done = true))
    }

    @Test
    fun `blank categories are dropped from the sub-line`() {
        assertEquals("anytime", ZenPhrase.forChore("  ", ChoreStatus.FRESH, done = false))
    }

    @Test
    fun `overdue and today tasks are for when you're up`() {
        assertEquals("admin · when you're up", ZenPhrase.forTask("Admin", TaskUrgency.OVERDUE, TaskPriority.NORMAL, done = false))
        assertEquals("admin · when you're up", ZenPhrase.forTask("Admin", TaskUrgency.TODAY, TaskPriority.LOWER, done = false))
    }

    @Test
    fun `undated tasks are anytime unless they are high priority`() {
        assertEquals("errand · anytime", ZenPhrase.forTask("Errand", TaskUrgency.NONE, TaskPriority.NORMAL, done = false))
        assertEquals("errand · when you're up", ZenPhrase.forTask("Errand", TaskUrgency.NONE, TaskPriority.HIGHER, done = false))
    }

    @Test
    fun `later tasks are whenever and this week tasks say so`() {
        assertEquals("whenever", ZenPhrase.forTask(null, TaskUrgency.LATER, TaskPriority.NORMAL, done = false))
        assertEquals("this week", ZenPhrase.forTask(null, TaskUrgency.THIS_WEEK, TaskPriority.NORMAL, done = false))
    }

    @Test
    fun `a ticked task reads done nice`() {
        assertEquals("done, nice", ZenPhrase.forTask(null, TaskUrgency.OVERDUE, TaskPriority.HIGHER, done = true))
    }
}
