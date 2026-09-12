package com.mapgie.dash.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.freeTagId
import com.mapgie.dash.data.model.isTagAlarm
import com.mapgie.dash.data.preferences.TagStickerStore
import com.mapgie.dash.data.repository.ChoreRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.supabase.userFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** What owns an NFC tag id: a chore row in Supabase, or an on-device tag-alarm. */
enum class TagOwnerKind(val label: String) {
    CHORE("Chore"),
    TAG_ALARM("Tag-alarm"),
}

/**
 * One line on Settings › NFC tags: the id a tag answers with (null for a
 * tag-alarm that has no tag yet), what it belongs to, and that owner's id
 * (a chore's tag id, a tag-alarm's record id) for the row's actions.
 */
data class TagEntry(
    val tagId: String?,
    val name: String,
    val kind: TagOwnerKind,
    val ownerId: String,
    val archived: Boolean = false,
    /** This phone has written the id to a sticker, or read it off one. */
    val onSticker: Boolean = false,
)

/** The chip row over the Chores list: every chore, only those on a sticker, or only those without. */
enum class ChoreTagFilter(val label: String) {
    ALL("All"),
    ON_STICKER("On a sticker"),
    NO_STICKER("No sticker"),
}

/**
 * What Settings › NFC tags shows, as pure state so `TagsUiStateTest` can pin it:
 * every chore's tag (archived ones last), every tag-alarm with or without a
 * tag, and what a scanned id resolves to.
 */
data class TagsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val chores: List<Chore> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
    /** Tag ids this phone has met on a sticker, with when ([TagStickerStore]). */
    val stickers: Map<String, Instant> = emptyMap(),
    val choreFilter: ChoreTagFilter = ChoreTagFilter.ALL,
) {
    val choreTags: List<TagEntry>
        get() = chores
            .map {
                TagEntry(
                    it.tagId, it.label, TagOwnerKind.CHORE, it.tagId,
                    archived = it.archivedAt != null,
                    onSticker = it.tagId in stickers,
                )
            }
            .sortedWith(compareBy({ it.archived }, { it.name.lowercase() }))

    /** The chores under the selected chip. */
    val filteredChoreTags: List<TagEntry>
        get() = when (choreFilter) {
            ChoreTagFilter.ALL -> choreTags
            ChoreTagFilter.ON_STICKER -> choreTags.filter { it.onSticker }
            ChoreTagFilter.NO_STICKER -> choreTags.filterNot { it.onSticker }
        }

    /** How many chores each chip would show, for its "· N". */
    fun choreCount(filter: ChoreTagFilter): Int = when (filter) {
        ChoreTagFilter.ALL -> choreTags.size
        ChoreTagFilter.ON_STICKER -> choreTags.count { it.onSticker }
        ChoreTagFilter.NO_STICKER -> choreTags.count { !it.onSticker }
    }

    val tagAlarms: List<TagEntry>
        get() = reminders
            .filter { it.isTagAlarm && it.archivedAt == null }
            .map { TagEntry(it.tagId, it.subject, TagOwnerKind.TAG_ALARM, it.id, onSticker = it.tagId in stickers) }
            .sortedBy { it.name.lowercase() }

    /** Every id some chore or tag-alarm answers to. */
    val takenTagIds: Set<String>
        get() = (choreTags + tagAlarms).mapNotNull { it.tagId }.toSet()

    /** What a scanned id belongs to, or null when nothing in the app knows it. */
    fun identify(tagId: String): TagEntry? =
        (choreTags + tagAlarms).firstOrNull { it.tagId == tagId }

    /** A friendly, unused id for a tag-alarm called [subject], for a first write from this page. */
    fun freeTagIdFor(subject: String): String = freeTagId(subject, takenTagIds)
}

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val reminderRepository: ReminderRepository,
    private val tagStickerStore: TagStickerStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    init {
        // Tag-alarms are on-device and change under this page (a link written from
        // the memo sheet, say), so they are followed rather than loaded once.
        viewModelScope.launch {
            reminderRepository.remindersFlow.collect { reminders ->
                _uiState.update { it.copy(reminders = reminders) }
            }
        }
        // A tap or a write on this page changes the sticker evidence at once.
        viewModelScope.launch {
            tagStickerStore.seen.collect { stickers ->
                _uiState.update { it.copy(stickers = stickers) }
            }
        }
        load()
    }

    fun setChoreFilter(filter: ChoreTagFilter) {
        _uiState.update { it.copy(choreFilter = filter) }
    }

    /** Loads the chores. A Supabase failure is reported but leaves the tag-alarms showing. */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { choreRepository.load().let { it.active + it.archived } }
                .onSuccess { chores -> _uiState.update { it.copy(loading = false, chores = chores) } }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.userFacingMessage()) } }
        }
    }

    /** Unlinks a tag-alarm from its tag; the tag itself is untouched and can be written again. */
    fun unlink(memoId: String) {
        viewModelScope.launch {
            runCatching { reminderRepository.setTagAlarmTag(memoId, null) }
                .onFailure { e -> _uiState.update { it.copy(error = e.userFacingMessage()) } }
        }
    }

    /**
     * Gives a tag-alarm with no tag an id to write, then calls [then] with it. The
     * id is minted from its name and kept clear of every id already in use.
     */
    fun assignTagThen(memoId: String, subject: String, then: (String) -> Unit) {
        val tagId = _uiState.value.freeTagIdFor(subject)
        viewModelScope.launch {
            runCatching { reminderRepository.setTagAlarmTag(memoId, tagId) }
                .onSuccess { then(tagId) }
                .onFailure { e -> _uiState.update { it.copy(error = e.userFacingMessage()) } }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
