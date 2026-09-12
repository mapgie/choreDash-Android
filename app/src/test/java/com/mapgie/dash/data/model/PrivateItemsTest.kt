package com.mapgie.dash.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the private (phone-only) store guarantees: a row is stored by its
 * category alone, an edit across the Private boundary moves the row, and
 * removing a private chore takes its logs with it, like the `scans` foreign
 * key cascade would in Supabase.
 */
class PrivateItemsTest {

    private fun task(id: String, category: String? = PRIVATE_CATEGORY) =
        TaskDto(id = id, title = id, category = category)

    private fun chore(tagId: String) =
        TagDto(id = "row-$tagId", tagId = tagId, label = tagId, category = PRIVATE_CATEGORY)

    private fun scan(id: String, tagId: String, at: String) = ScanDto(id = id, tagId = tagId, scannedAt = at)

    // ── The reserved name ──────────────────────────────────────────────────────

    @Test
    fun `private matches regardless of case and surrounding spaces`() {
        assertTrue(isPrivateCategory("Private"))
        assertTrue(isPrivateCategory(" private "))
        assertTrue(isPrivateCategory("PRIVATE"))
        assertFalse(isPrivateCategory("Privates"))
        assertFalse(isPrivateCategory(null))
        assertFalse(isPrivateCategory(""))
    }

    @Test
    fun `a task or chore is private exactly when its category is the reserved name`() {
        assertTrue(task("t", category = "private").isPrivate)
        assertFalse(task("t", category = "Kitchen").isPrivate)
        assertFalse(task("t", category = null).isPrivate)
        val privateChore = Chore.from(chore("c"), lastScanned = null, lastScanId = null)
        assertTrue(privateChore.isPrivate)
        assertFalse(privateChore.copy(category = GENERAL_CATEGORY).isPrivate)
    }

    // ── Where an edit sends a row ──────────────────────────────────────────────

    @Test
    fun `a private row edited to a private category stays on the phone`() {
        assertEquals(PrivateMove.STAY_PRIVATE, privateMove(isStoredPrivately = true, newCategory = "private"))
    }

    @Test
    fun `a private row edited to any other category moves to the shared list`() {
        assertEquals(PrivateMove.TO_SHARED, privateMove(isStoredPrivately = true, newCategory = "Kitchen"))
        assertEquals(PrivateMove.TO_SHARED, privateMove(isStoredPrivately = true, newCategory = null))
    }

    @Test
    fun `a shared row edited to private leaves the shared list for the phone`() {
        assertEquals(PrivateMove.TO_PRIVATE, privateMove(isStoredPrivately = false, newCategory = "Private"))
    }

    @Test
    fun `a shared row edited to another shared category stays shared`() {
        assertEquals(PrivateMove.STAY_SHARED, privateMove(isStoredPrivately = false, newCategory = "Kitchen"))
        assertEquals(PrivateMove.STAY_SHARED, privateMove(isStoredPrivately = false, newCategory = null))
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────

    @Test
    fun `adding a task with an existing id replaces it rather than duplicating`() {
        val items = PrivateItems().withTask(task("a")).withTask(task("a").copy(title = "renamed"))
        assertEquals(1, items.tasks.size)
        assertEquals("renamed", items.task("a")?.title)
        assertTrue(items.hasTask("a"))
        assertFalse(items.hasTask("b"))
    }

    @Test
    fun `updating a task touches only that task and ignores unknown ids`() {
        val items = PrivateItems().withTask(task("a")).withTask(task("b"))
            .updateTask("a") { it.copy(completedAt = "2026-08-01T10:00:00Z") }
            .updateTask("zzz") { it.copy(title = "never") }
        assertEquals("2026-08-01T10:00:00Z", items.task("a")?.completedAt)
        assertNull(items.task("b")?.completedAt)
        assertEquals(2, items.tasks.size)
    }

    @Test
    fun `removing a task leaves the others`() {
        val items = PrivateItems().withTask(task("a")).withTask(task("b")).withoutTask("a")
        assertEquals(listOf("b"), items.tasks.map { it.id })
    }

    // ── Chores and logs ────────────────────────────────────────────────────────

    @Test
    fun `a chore is found by its tag id and replaced by tag id`() {
        val items = PrivateItems().withChore(chore("nfc-1")).withChore(chore("nfc-1").copy(label = "Bins"))
        assertEquals(1, items.chores.size)
        assertEquals("Bins", items.chore("nfc-1")?.label)
        assertTrue(items.hasChore("nfc-1"))
        assertFalse(items.hasChore("nfc-2"))
    }

    @Test
    fun `logs of a chore come back newest first and are capped by the caller`() {
        val items = PrivateItems()
            .withChore(chore("nfc-1"))
            .withScan(scan("s1", "nfc-1", "2026-08-01T10:00:00Z"))
            .withScan(scan("s2", "nfc-1", "2026-08-03T10:00:00Z"))
            .withScan(scan("s3", "nfc-2", "2026-08-05T10:00:00Z"))
        assertEquals(listOf("s2", "s1"), items.scansFor("nfc-1").map { it.id })
        assertEquals(listOf("s2"), items.scansFor("nfc-1").take(1).map { it.id })
    }

    @Test
    fun `removing a chore removes its logs and no others`() {
        val items = PrivateItems()
            .withChore(chore("nfc-1")).withChore(chore("nfc-2"))
            .withScan(scan("s1", "nfc-1", "2026-08-01T10:00:00Z"))
            .withScan(scan("s2", "nfc-2", "2026-08-02T10:00:00Z"))
            .withoutChore("nfc-1")
        assertEquals(listOf("nfc-2"), items.chores.map { it.tagId })
        assertEquals(listOf("s2"), items.scans.map { it.id })
        assertFalse(items.hasScan("s1"))
        assertTrue(items.hasScan("s2"))
    }

    @Test
    fun `undoing a log removes just that log`() {
        val items = PrivateItems()
            .withChore(chore("nfc-1"))
            .withScans(listOf(scan("s1", "nfc-1", "2026-08-01T10:00:00Z"), scan("s2", "nfc-1", "2026-08-02T10:00:00Z")))
            .withoutScan("s2")
        assertEquals(listOf("s1"), items.scansFor("nfc-1").map { it.id })
    }
}
