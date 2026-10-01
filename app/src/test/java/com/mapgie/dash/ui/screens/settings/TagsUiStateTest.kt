package com.mapgie.dash.ui.screens.settings

import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.NfcTagDto
import com.mapgie.dash.data.model.ReminderDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * What Settings › NFC tags lists and resolves: every saved tag with the chore it
 * logs, every chore with its tags (archived ones last), every unarchived
 * tag-alarm with or without a tag, what a scanned id is, and a free id for a
 * first write.
 */
class TagsUiStateTest {

    private fun chore(key: String, label: String, archived: Boolean = false) = Chore(
        id = "c-$key", tagId = key, label = label, category = null, owner = null,
        intervalDays = null, archivedAt = if (archived) "2026-07-01T00:00:00Z" else null,
        lastScanned = null, lastScanId = null, status = ChoreStatus.NEVER,
    )

    private fun tagAlarm(id: String, subject: String, tagId: String?, archived: Boolean = false) = ReminderDto(
        id = id, subject = subject, remindAt = "2026-07-08T05:15:00Z", tagAlarm = true, tagId = tagId,
        ringTimes = listOf("05:15"), archivedAt = if (archived) "2026-07-01T00:00:00Z" else null,
    )

    private val state = TagsUiState(
        loading = false,
        chores = listOf(
            chore("laundry", "Laundry"),
            chore("bins", "Bins", archived = true),
            chore("3f2c1a4e-0b7d-4c55-9a1e-2d6f8b0c9e11", "Book Santa"),
        ),
        savedTags = listOf(
            NfcTagDto("laundry", "Laundry basket", choreTagId = "laundry", createdAt = "2026-07-01T00:00:00Z"),
            NfcTagDto("utility", "Utility room", choreTagId = "laundry", createdAt = "2026-07-02T00:00:00Z"),
            NfcTagDto("back-door", "Back door", choreTagId = "bins"),
            NfcTagDto("fob", "Blue fob"),
        ),
        reminders = listOf(
            tagAlarm("m1", "Office A", "office-a"),
            tagAlarm("m2", "Home", null),
            tagAlarm("m3", "Old office", "old-office", archived = true),
            ReminderDto(id = "plain", subject = "Water plants", remindAt = "2026-07-08T09:00:00Z"),
        ),
        stickers = mapOf("office-a" to Instant.parse("2026-07-10T08:00:00Z")),
    )

    // ── Saved tags ────────────────────────────────────────────────────────────

    @Test
    fun `saved tags list A to Z with the chore each one logs, or none`() {
        val entries = state.savedTagEntries
        assertEquals(listOf("Back door", "Blue fob", "Laundry basket", "Utility room"), entries.map { it.tag.name })
        assertEquals(listOf("Bins", null, "Laundry", "Laundry"), entries.map { it.choreName })
        assertEquals(listOf(true, false, false, false), entries.map { it.choreArchived })
    }

    @Test
    fun `a saved tag whose chore is gone shows as not attached`() {
        val orphan = state.copy(savedTags = listOf(NfcTagDto("old", "Old sticker", choreTagId = "deleted-chore")))
        assertNull(orphan.savedTagEntries.single().choreName)
    }

    @Test
    fun `a tag can be attached to any unarchived chore, A to Z`() {
        assertEquals(listOf("Book Santa", "Laundry"), state.attachableChores.map { it.label })
    }

    // ── Chores ────────────────────────────────────────────────────────────────

    @Test
    fun `chores list active ones A to Z, then archived, each with all its tags`() {
        assertEquals(listOf("Book Santa", "Laundry", "Bins"), state.choreTags.map { it.name })
        assertEquals(listOf(false, false, true), state.choreTags.map { it.archived })
        assertEquals(listOf("laundry", "utility"), state.choreTags[1].tags.map { it.nfcId })
    }

    @Test
    fun `a chore with no tag lists no tags, never its key`() {
        val santa = state.choreTags.first { it.name == "Book Santa" }
        assertTrue(santa.tags.isEmpty())
        assertEquals("3f2c1a4e-0b7d-4c55-9a1e-2d6f8b0c9e11", santa.choreTagId)
    }

    @Test
    fun `the tag chips split chores by whether they have any tag`() {
        assertEquals(listOf("Laundry", "Bins"), state.copy(choreFilter = ChoreTagFilter.ON_STICKER).filteredChoreTags.map { it.name })
        assertEquals(listOf("Book Santa"), state.copy(choreFilter = ChoreTagFilter.NO_STICKER).filteredChoreTags.map { it.name })
        assertEquals(3, state.choreCount(ChoreTagFilter.ALL))
        assertEquals(2, state.choreCount(ChoreTagFilter.ON_STICKER))
        assertEquals(1, state.choreCount(ChoreTagFilter.NO_STICKER))
    }

    // ── Tag-alarms ────────────────────────────────────────────────────────────

    @Test
    fun `tag-alarms list unarchived ones with or without a tag, never plain memos`() {
        assertEquals(listOf("Home", "Office A"), state.tagAlarms.map { it.name })
        assertEquals(listOf(null, "office-a"), state.tagAlarms.map { it.tagId })
        assertEquals("m2", state.tagAlarms.first().ownerId)
    }

    @Test
    fun `a tag-alarm is on a sticker only when this phone has written or read its id`() {
        assertEquals(listOf(false, true), state.tagAlarms.map { it.onSticker })
    }

    // ── Identify ──────────────────────────────────────────────────────────────

    @Test
    fun `a scanned tag nobody knows can be saved`() {
        val unknown = state.identify("04a1b2c3")
        assertTrue(unknown.canSave)
        assertFalse(unknown.canAttach)
        assertNull(unknown.saved)
    }

    @Test
    fun `a scanned saved tag says its name and the chore it logs`() {
        val utility = state.identify("utility")
        assertEquals("Utility room", utility.saved?.name)
        assertEquals("Laundry", utility.choreName)
        assertFalse(utility.canSave)
        assertFalse(utility.canAttach)
        assertEquals("Bins (archived)", state.identify("back-door").choreName)
    }

    @Test
    fun `a scanned saved tag with no chore can be attached`() {
        val fob = state.identify("fob")
        assertTrue(fob.canAttach)
        assertFalse(fob.canSave)
    }

    @Test
    fun `a tag-alarm's tag is named as such and can be neither saved nor attached`() {
        val office = state.identify("office-a")
        assertEquals("Office A", office.tagAlarm)
        assertFalse(office.canSave)
        assertFalse(office.canAttach)
        // An archived tag-alarm's tag is no longer anyone's.
        assertTrue(state.identify("old-office").canSave)
    }

    // ── Free ids ──────────────────────────────────────────────────────────────

    @Test
    fun `a free tag id comes from the name and steps around saved tags, tag-alarms and chore keys`() {
        assertEquals("home", state.freeTagIdFor("Home"))
        assertEquals("office-a-2", state.freeTagIdFor("Office A"))
        assertEquals("laundry-2", state.freeTagIdFor("Laundry"))
        assertEquals("utility-2", state.freeTagIdFor("Utility"))
        // An older chore's key is the id on its sticker, so it is never handed out again.
        assertEquals("bins-2", state.copy(savedTags = emptyList()).freeTagIdFor("Bins"))
    }
}
