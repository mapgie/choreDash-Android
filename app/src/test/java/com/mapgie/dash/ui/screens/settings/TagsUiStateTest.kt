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

    private fun chore(tagId: String, label: String, archived: Boolean = false) = Chore(
        id = "c-$tagId", tagId = tagId, label = label, category = null, owner = null,
        intervalDays = null, archivedAt = if (archived) "2026-07-01T00:00:00Z" else null,
        lastScanned = null, lastScanId = null, status = ChoreStatus.NEVER,
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

    // ── On a sticker ──────────────────────────────────────────────────────────

    private val withStickers = state.copy(
        stickers = mapOf("laundry" to Instant.parse("2026-07-10T08:00:00Z"), "office-a" to Instant.parse("2026-07-10T08:00:00Z")),
    )

    @Test
    fun `a chore is on a sticker only when this phone has written or read its id`() {
        assertEquals(listOf(false, true, false), withStickers.choreTags.map { it.onSticker })
        assertEquals(listOf(false, true), withStickers.tagAlarms.map { it.onSticker })
    }

    @Test
    fun `the sticker chips split the chores and count each side`() {
        assertEquals(listOf("Laundry"), withStickers.copy(choreFilter = ChoreTagFilter.ON_STICKER).filteredChoreTags.map { it.name })
        assertEquals(listOf("Car service", "Bins"), withStickers.copy(choreFilter = ChoreTagFilter.NO_STICKER).filteredChoreTags.map { it.name })
        assertEquals(3, withStickers.choreCount(ChoreTagFilter.ALL))
        assertEquals(1, withStickers.choreCount(ChoreTagFilter.ON_STICKER))
        assertEquals(2, withStickers.choreCount(ChoreTagFilter.NO_STICKER))
        // Without any evidence, nothing is on a sticker and the All chip shows everything.
        assertEquals(3, state.copy(choreFilter = ChoreTagFilter.NO_STICKER).filteredChoreTags.size)
    }

    @Test
    fun `a free tag id comes from the name and steps around ids in use`() {
        assertEquals("home", state.freeTagIdFor("Home"))
        assertEquals("office-a-2", state.freeTagIdFor("Office A"))
        assertEquals("laundry-2", state.freeTagIdFor("Laundry"))
    }
}
