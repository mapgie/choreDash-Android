package com.mapgie.dash.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Settings › Swipe actions as a contract: what each list swipes do out of the
 * box (the behaviour before the setting existed), which actions each list may
 * offer, and how a stored choice survives an unknown or unsupported value.
 */
class SwipeActionTest {

    @Test
    fun `chores log on a right swipe and snooze on a left swipe by default`() {
        assertEquals(SwipeAction.DONE, SwipeSubject.CHORES.default.right)
        assertEquals(SwipeAction.SNOOZE, SwipeSubject.CHORES.default.left)
    }

    @Test
    fun `tasks complete on a right swipe and do nothing on a left swipe by default`() {
        assertEquals(SwipeAction.DONE, SwipeSubject.TASKS.default.right)
        assertEquals(SwipeAction.NONE, SwipeSubject.TASKS.default.left)
    }

    @Test
    fun `memos mark done on a left swipe and delete on a right swipe by default`() {
        assertEquals(SwipeAction.DONE, SwipeSubject.MEMOS.default.left)
        assertEquals(SwipeAction.DELETE, SwipeSubject.MEMOS.default.right)
    }

    @Test
    fun `every list offers its own defaults and a way to turn a swipe off`() {
        SwipeSubject.entries.forEach { subject ->
            assertTrue(subject.name, subject.default.left in subject.offered)
            assertTrue(subject.name, subject.default.right in subject.offered)
            assertTrue(subject.name, SwipeAction.NONE in subject.offered)
        }
    }

    @Test
    fun `chores cannot be deleted by a swipe, they retire by archiving`() {
        assertFalse(SwipeAction.DELETE in SwipeSubject.CHORES.offered)
        assertTrue(SwipeAction.ARCHIVE in SwipeSubject.CHORES.offered)
    }

    @Test
    fun `tasks have no snooze to offer`() {
        assertFalse(SwipeAction.SNOOZE in SwipeSubject.TASKS.offered)
    }

    @Test
    fun `nothing disables that direction only`() {
        val pair = SwipePair(left = SwipeAction.NONE, right = SwipeAction.DONE)
        assertFalse(pair.enabled(SwipeDirection.LEFT))
        assertTrue(pair.enabled(SwipeDirection.RIGHT))
    }

    @Test
    fun `turning one action off leaves the other direction as it was`() {
        val pair = SwipePair(left = SwipeAction.SNOOZE, right = SwipeAction.DONE).without(SwipeAction.SNOOZE)
        assertEquals(SwipePair(left = SwipeAction.NONE, right = SwipeAction.DONE), pair)
        assertEquals(pair, pair.without(SwipeAction.DELETE))
    }

    @Test
    fun `an action a list does not offer falls back to that list's default for the direction`() {
        val pair = SwipePair(left = SwipeAction.DELETE, right = SwipeAction.ARCHIVE)
        val sanitised = SwipeSubject.CHORES.sanitise(pair)
        assertEquals(SwipeAction.SNOOZE, sanitised.left)
        assertEquals(SwipeAction.ARCHIVE, sanitised.right)
    }

    @Test
    fun `a missing or unknown stored name falls back to the default for that direction`() {
        assertEquals(SwipeSubject.TASKS.default, SwipeSubject.TASKS.resolve(null, null))
        assertEquals(SwipeSubject.TASKS.default, SwipeSubject.TASKS.resolve("BANANA", "TELEPORT"))
        assertEquals(
            SwipePair(left = SwipeAction.DELETE, right = SwipeAction.DONE),
            SwipeSubject.TASKS.resolve("DELETE", "garbage"),
        )
    }

    @Test
    fun `a chore's Done reads as Log, everything else keeps its name`() {
        assertEquals("Log", SwipeSubject.CHORES.label(SwipeAction.DONE))
        assertEquals("Done", SwipeSubject.TASKS.label(SwipeAction.DONE))
        assertEquals("Done", SwipeSubject.MEMOS.label(SwipeAction.DONE))
        assertEquals("Nothing", SwipeSubject.CHORES.label(SwipeAction.NONE))
    }

    @Test
    fun `changing one direction on one list leaves the rest untouched`() {
        val settings = SwipeSettings()
            .with(SwipeSubject.TASKS, SwipeSubject.TASKS.default.with(SwipeDirection.LEFT, SwipeAction.DELETE))
        assertEquals(SwipeAction.DELETE, settings.tasks.left)
        assertEquals(SwipeAction.DONE, settings.tasks.right)
        assertEquals(SwipeSubject.CHORES.default, settings[SwipeSubject.CHORES])
        assertEquals(SwipeSubject.MEMOS.default, settings[SwipeSubject.MEMOS])
    }
}
