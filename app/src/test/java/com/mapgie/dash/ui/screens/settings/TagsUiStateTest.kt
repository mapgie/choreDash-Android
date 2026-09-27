package com.mapgie.dash.ui.screens.settings

import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.ReminderDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * What Settings › NFC tags lists and resolves: every chore's tag with archived
 * ones last, every unarchived tag-alarm with or without a tag, what a scanned
 * id belongs to, and a free id for a tag-alarm's first write.
 */
class TagsUiStateTest {

    private fun chore(key: String, label: String, archived: Boolean = false, nfcId: String? = key) = Chore(
        id = "c-$key", tagId = key, label = label, category = null, owner = null,
        intervalDays = null, archivedAt = if (archived) "2026-07-01T00:00:00Z" else null,
        lastScanned = null, lastScanId = null, status = ChoreStatus.NEVER, nfcId = nfcId,
    )

    private fun tagAlarm(id: String, subject: String, tagId: String?, archived: Boolean = false) = ReminderDto(
        id = id, subject = subject, remindAt = "2026-07-08T05:15:00Z", tagAlarm = true, tagId = tagId,
        ringTimes = listOf("05:15"), archivedAt = if (archived) "2026-07-01T00:00:00Z" else null,
    )

    private val state = TagsUiState(
        loading = false,
        chores = listOf(chore("laundry", "Laundry"), chore("bins", "Bins", archived = true), chore("car-service", "Car service")),
        reminders = listOf(
            tagAlarm("m1", "Office A", "office-a"),
            tagAlarm("m2", "Home", null),
            tagAlarm("m3", "Old office", "old-office", archived = true),
            ReminderDto(id = "plain", subject = "Water plants", remindAt = "2026-07-08T09:00:00Z"),
        ),
    )

    @Test
    fun `chore tags list active ones A to Z, then archived`() {
        assertEquals(listOf("Car service", "Laundry", "Bins"), state.choreTags.map { it.name })
        assertEquals(listOf(false, false, true), state.choreTags.map { it.archived })
        assertEquals("car-service", state.choreTags.first().ownerId)
    }

    @Test
    fun `tag-alarms list unarchived ones with or without a tag, never plain memos`() {
        assertEquals(listOf("Home", "Office A"), state.tagAlarms.map { it.name })
        assertEquals(listOf(null, "office-a"), state.tagAlarms.map { it.tagId })
        assertEquals("m2", state.tagAlarms.first().ownerId)
    }

    @Test
    fun `identify names the chore or tag-alarm a scanned id belongs to`() {
        assertEquals(TagOwnerKind.CHORE, state.identify("laundry")?.kind)
        assertEquals("Office A", state.identify("office-a")?.name)
        assertNull(state.identify("waterloo"))
        // An archived tag-alarm's tag is no longer anyone's.
        assertNull(state.identify("old-office"))
    }

    // ── On a tag ──────────────────────────────────────────────────────────────

    private val mixed = state.copy(
        chores = listOf(
            chore("laundry", "Laundry"),
            chore("3f2c1a4e-0b7d-4c55-9a1e-2d6f8b0c9e11", "Book Santa", nfcId = null),
            chore("car-service", "Car service", nfcId = null),
        ),
        stickers = mapOf("laundry" to Instant.parse("2026-07-10T08:00:00Z"), "office-a" to Instant.parse("2026-07-10T08:00:00Z")),
    )

    @Test
    fun `a chore with no tag lists no tag id, never its key`() {
        val santa = mixed.choreTags.first { it.name == "Book Santa" }
        assertNull(santa.tagId)
        assertEquals("3f2c1a4e-0b7d-4c55-9a1e-2d6f8b0c9e11", santa.ownerId)
    }

    @Test
    fun `the tag chips split chores by whether they are linked to a tag`() {
        assertEquals(listOf("Laundry"), mixed.copy(choreFilter = ChoreTagFilter.ON_STICKER).filteredChoreTags.map { it.name })
        assertEquals(listOf("Book Santa", "Car service"), mixed.copy(choreFilter = ChoreTagFilter.NO_STICKER).filteredChoreTags.map { it.name })
        assertEquals(3, mixed.choreCount(ChoreTagFilter.ALL))
        assertEquals(1, mixed.choreCount(ChoreTagFilter.ON_STICKER))
        assertEquals(2, mixed.choreCount(ChoreTagFilter.NO_STICKER))
    }

    @Test
    fun `a tag unlinked from a chore belongs to nothing`() {
        assertNull(mixed.identify("car-service"))
        assertEquals("Laundry", mixed.identify("laundry")?.name)
    }

    @Test
    fun `a tag-alarm is on a sticker only when this phone has written or read its id`() {
        assertEquals(listOf(false, true), mixed.tagAlarms.map { it.onSticker })
    }

    @Test
    fun `a fresh tag id steers clear of a chore's key even after its tag is unlinked`() {
        // An older chore's key is the id on its sticker, so it is never handed out again.
        assertEquals("car-service-2", mixed.freeTagIdFor("Car service"))
        assertEquals("book-santa", mixed.freeTagIdFor("Book Santa"))
    }

    @Test
    fun `a free tag id comes from the name and steps around ids in use`() {
        assertEquals("home", state.freeTagIdFor("Home"))
        assertEquals("office-a-2", state.freeTagIdFor("Office A"))
        assertEquals("laundry-2", state.freeTagIdFor("Laundry"))
    }
}
