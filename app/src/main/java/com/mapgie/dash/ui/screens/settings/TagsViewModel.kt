package com.mapgie.dash.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.NfcTagDto
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.attachedTo
import com.mapgie.dash.data.model.freeTagId
import com.mapgie.dash.data.model.isTagAlarm
import com.mapgie.dash.data.preferences.TagStickerStore
import com.mapgie.dash.data.repository.ChoreRepository
import com.mapgie.dash.data.repository.NfcTagRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.supabase.userFacingMessage
import com.mapgie.dash.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** What owns an NFC tag id: a chore, or an on-device tag-alarm. */
enum class TagOwnerKind(val label: String) {
    CHORE("Chore"),
    TAG_ALARM("Tag-alarm"),
}

/**
 * One tag-alarm line on Settings › NFC tags: the id its tag answers with (null
 * for one with no tag yet), its name, and its record id for the row's actions.
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

/** One saved tag on the page, with the name of the chore it is attached to (null for none). */
data class SavedTagEntry(
    val tag: NfcTagDto,
    val choreName: String?,
    val choreArchived: Boolean = false,
)

/** One chore on the page with every tag a tap on which logs it. */
data class ChoreTagsEntry(
    val choreTagId: String,
    val name: String,
    val tags: List<NfcTagDto>,
    val archived: Boolean = false,
)

/** What a scanned id is to the app: a saved tag (attached or not), a tag-alarm's, or nothing yet. */
data class TagIdentity(
    val tagId: String,
    val saved: NfcTagDto? = null,
    val choreName: String? = null,
    val tagAlarm: String? = null,
) {
    /** Nothing in the app knows this id, so it can be saved. */
    val canSave: Boolean get() = saved == null && tagAlarm == null

    /** Saved but on no chore, so it can be attached to one. */
    val canAttach: Boolean get() = saved != null && choreName == null && tagAlarm == null

    /** The line under the scanned id on the Identify card. */
    val summary: String
        get() = when {
            tagAlarm != null -> "Tag-alarm: $tagAlarm"
            saved != null && choreName != null -> "Saved as ${saved.name}. Logs $choreName."
            saved != null -> "Saved as ${saved.name}. Not attached to a chore yet."
            else -> "Not saved. Save it with a name to attach it to a chore later."
        }
}

/** The chip row over the Chores list: every chore, only those with a tag, or only those without one. */
enum class ChoreTagFilter(val label: String) {
    ALL("All"),
    ON_STICKER("On a tag"),
    NO_STICKER("No tag"),
}

/**
 * What Settings › NFC tags shows, as pure state so `TagsUiStateTest` can pin it:
 * every saved tag and the chore it logs, every chore with its tags (archived
 * ones last), every tag-alarm with or without a tag, and what a scanned id
 * resolves to.
 */
data class TagsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val chores: List<Chore> = emptyList(),
    /** Every saved tag, attached or not. */
    val savedTags: List<NfcTagDto> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
    /** Tag ids this phone has met on a sticker, with when ([TagStickerStore]). */
    val stickers: Map<String, Instant> = emptyMap(),
    val choreFilter: ChoreTagFilter = ChoreTagFilter.ALL,
) {
    private fun choreFor(tag: NfcTagDto): Chore? = tag.choreTagId?.let { key -> chores.firstOrNull { it.tagId == key } }

    /** Saved tags A to Z, each with the chore it logs. A tag whose chore is gone counts as unattached. */
    val savedTagEntries: List<SavedTagEntry>
        get() = savedTags
            .map { tag ->
                val chore = choreFor(tag)
                SavedTagEntry(tag, chore?.label, chore?.archivedAt != null)
            }
            .sortedWith(compareBy({ it.tag.name.lowercase() }, { it.tag.nfcId }))

    /** Chores A to Z with their tags, archived ones last. */
    val choreTags: List<ChoreTagsEntry>
        get() = chores
            .map { chore ->
                ChoreTagsEntry(chore.tagId, chore.label, savedTags.attachedTo(chore.tagId), archived = chore.archivedAt != null)
            }
            .sortedWith(compareBy({ it.archived }, { it.name.lowercase() }))

    /** The chores under the selected chip: a chore is on a tag when it has at least one. */
    val filteredChoreTags: List<ChoreTagsEntry>
        get() = when (choreFilter) {
            ChoreTagFilter.ALL -> choreTags
            ChoreTagFilter.ON_STICKER -> choreTags.filter { it.tags.isNotEmpty() }
            ChoreTagFilter.NO_STICKER -> choreTags.filter { it.tags.isEmpty() }
        }

    /** How many chores each chip would show, for its "· N". */
    fun choreCount(filter: ChoreTagFilter): Int = when (filter) {
        ChoreTagFilter.ALL -> choreTags.size
        ChoreTagFilter.ON_STICKER -> choreTags.count { it.tags.isNotEmpty() }
        ChoreTagFilter.NO_STICKER -> choreTags.count { it.tags.isEmpty() }
    }

    /** The chores a saved tag can be attached to: unarchived, A to Z. */
    val attachableChores: List<Chore>
        get() = chores.filter { it.archivedAt == null }.sortedBy { it.label.lowercase() }

    val tagAlarms: List<TagEntry>
        get() = reminders
            .filter { it.isTagAlarm && it.archivedAt == null }
            .map { TagEntry(it.tagId, it.subject, TagOwnerKind.TAG_ALARM, it.id, onSticker = it.tagId in stickers) }
            .sortedBy { it.name.lowercase() }

    /**
     * Every id a saved tag or a tag-alarm answers to, plus every chore's key: an
     * older chore's key is the id on its sticker, so a fresh id steers clear of it.
     */
    val takenTagIds: Set<String>
        get() = savedTags.map { it.nfcId }.toSet() + tagAlarms.mapNotNull { it.tagId } + chores.map { it.tagId }

    /** What a scanned id is to the app. A tag-alarm wins, as it does on a tap. */
    fun identify(tagId: String): TagIdentity {
        tagAlarms.firstOrNull { it.tagId == tagId }?.let { return TagIdentity(tagId, tagAlarm = it.name) }
        val saved = savedTags.firstOrNull { it.nfcId == tagId } ?: return TagIdentity(tagId)
        val chore = choreFor(saved)
        val choreName = chore?.let { it.label + if (it.archivedAt != null) " (archived)" else "" }
        return TagIdentity(tagId, saved = saved, choreName = choreName)
    }

    /** A friendly, unused id for a chore or tag-alarm called [subject], for a first write from this page. */
    fun freeTagIdFor(subject: String): String = freeTagId(subject, takenTagIds)
}

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val nfcTagRepository: NfcTagRepository,
    private val reminderRepository: ReminderRepository,
    private val tagStickerStore: TagStickerStore,
    @ApplicationContext private val appContext: Context,
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

    /**
     * Loads the chores and the saved tags. A Supabase failure is reported but
     * leaves the tag-alarms showing. The tags are read on their own so a
     * database without the `nfc_tags` table says so here, where tags are managed.
     */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                val result = choreRepository.load()
                (result.active + result.archived) to nfcTagRepository.all()
            }
                .onSuccess { (chores, saved) -> _uiState.update { it.copy(loading = false, chores = chores, savedTags = saved) } }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.userFacingMessage()) } }
        }
    }

    /** Runs [action], then reloads; a failure is reported on the page. */
    private fun changeTags(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess {
                    load()
                    WidgetUpdater.updateAll(appContext)
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.userFacingMessage()) } }
        }
    }

    /** Saves the scanned tag [tagId] as [name], attached to nothing yet. */
    fun saveTag(tagId: String, name: String) = changeTags {
        // A tag has one job: one a tag-alarm answers to cannot also be saved for chores.
        reminderRepository.findTagAlarmByTagId(tagId)?.let { owner ->
            throw IllegalArgumentException("That tag already belongs to the tag-alarm \"${owner.subject}\". A tag has one job.")
        }
        nfcTagRepository.save(tagId, name.trim())
    }

    fun renameTag(tagId: String, name: String) = changeTags { nfcTagRepository.rename(tagId, name.trim()) }

    /** Forgets a saved tag: a tap on it no longer logs anything. The sticker itself is untouched. */
    fun forgetTag(tagId: String) = changeTags { nfcTagRepository.delete(tagId) }

    /** Attaches the saved tag [tagId] to the chore with key [choreTagId]. */
    fun attachTag(tagId: String, choreTagId: String) = changeTags {
        val name = _uiState.value.savedTags.firstOrNull { it.nfcId == tagId }?.name ?: tagId
        nfcTagRepository.attach(tagId, choreTagId, name)
    }

    /** Lets a saved tag go from its chore. It stays saved, free to attach to another. */
    fun detachTag(tagId: String) = changeTags { nfcTagRepository.detach(tagId) }

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
