package com.mapgie.dash.ui.screens.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.alarm.AlarmScheduler
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.DraftStore
import com.mapgie.dash.data.model.ReminderDraft
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.ReminderLabelStyle
import com.mapgie.dash.data.model.ReminderSortKey
import com.mapgie.dash.data.model.SortOrder
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.isDone
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.data.model.repeats
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ChoreRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** The filter-chip row on the Memos list (handoff 9a): Active by default, one plain Done state, All. */
enum class ReminderFilter(val label: String) {
    ACTIVE("Active"),
    DONE("Done"),
    ALL("All"),
}

/**
 * What the Memos list shows. Pure state so `ReminderUiStateTest` can pin the
 * buckets and orders without a ViewModel or Android.
 *
 * Active means "still has a ring to give": a once-only memo until it rings, a
 * repeating memo until it is archived. Done holds once-only memos that have rung
 * (or were dismissed as done). All is everything, archived included; there is
 * no Archived tab (archiving lives in the edit sheet).
 */
data class ReminderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val reminders: List<ReminderDto> = emptyList(),
    val chores: List<Chore> = emptyList(),
    val tasks: List<TaskDto> = emptyList(),
    val reminderLabel: ReminderLabelStyle = ReminderLabelStyle.REMINDERS,
    val filter: ReminderFilter = ReminderFilter.ACTIVE,
    val sort: SortOrder<ReminderSortKey> = SortOrder(ReminderSortKey.NEXT_RING),
) {
    val active: List<ReminderDto>
        get() = sorted(reminders.filter { it.archivedAt == null && !it.isDone })

    val done: List<ReminderDto>
        get() = sorted(reminders.filter { it.archivedAt == null && it.isDone })

    val all: List<ReminderDto>
        get() = sorted(reminders)

    /** The list under the selected chip. */
    val displayed: List<ReminderDto>
        get() = when (filter) {
            ReminderFilter.ACTIVE -> active
            ReminderFilter.DONE -> done
            ReminderFilter.ALL -> all
        }

    /** For the "Active · N" chip. */
    val activeCount: Int
        get() = active.size

    /** "chore" or "task" for the card's "linked to …" suffix, or null when standalone. */
    fun linkedTo(reminder: ReminderDto): String? = when {
        reminder.choreId != null -> "chore"
        reminder.taskId != null -> "task"
        else -> null
    }

    /** The linked chore's or task's name, for the sheet. */
    fun linkedLabel(reminder: ReminderDto): String? {
        reminder.choreId?.let { id -> chores.find { it.id == id }?.let { return "Chore: ${it.label}" } }
        reminder.taskId?.let { id -> tasks.find { it.id == id }?.let { return "Task: ${it.title}" } }
        return null
    }

    private fun sorted(list: List<ReminderDto>): List<ReminderDto> {
        val ordered = when (sort.key) {
            ReminderSortKey.NEXT_RING -> list.sortedBy { it.remindAtInstant() ?: Instant.MAX }
            ReminderSortKey.NAME -> list.sortedBy { it.subject.lowercase() }
            ReminderSortKey.CREATED -> list.sortedByDescending { it.createdAt }
        }
        return if (sort.reversed) ordered.asReversed() else ordered
    }
}

@HiltViewModel
class RemindersListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reminderRepository: ReminderRepository,
    private val choreRepository: ChoreRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    /**
     * Unsaved edit-alarm sheet drafts by memo id (or NEW_DRAFT_KEY for the New
     * sheet), kept in saved state so they outlive rotation and process death
     * within the session. The sheet offers them back; it never auto-applies.
     */
    val reminderDrafts = DraftStore(savedStateHandle, "reminder_drafts", ReminderDraft.serializer())

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(reminderLabel = settings.reminderLabel, sort = settings.reminderSort) }
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                val reminders = reminderRepository.loadReminders()
                val chores = choreRepository.load().active
                val tasks = taskRepository.loadTasks()
                _uiState.update {
                    it.copy(loading = false, reminders = reminders, chores = chores, tasks = tasks)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun setFilter(filter: ReminderFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setSort(order: SortOrder<ReminderSortKey>) {
        _uiState.update { it.copy(sort = order) }
        viewModelScope.launch { settingsRepository.setReminderSort(order) }
    }

    fun addReminder(insert: ReminderInsert) {
        viewModelScope.launch {
            runCatching {
                alarmScheduler.syncReminder(reminderRepository.addReminder(insert))
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun editReminder(id: String, insert: ReminderInsert) {
        viewModelScope.launch {
            runCatching {
                alarmScheduler.syncReminder(reminderRepository.updateReminder(id, insert))
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun archiveReminder(id: String, archived: Boolean) {
        viewModelScope.launch {
            runCatching {
                // Archiving disarms the alarm; unarchiving re-arms whatever is still pending.
                reminderRepository.archiveReminder(id, archived)?.let { alarmScheduler.syncReminder(it) }
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Marks a memo done from a swipe (or undoes it). A once-only memo completes;
     * a repeating memo has no Done, so it is archived (turned off) instead. Either
     * way its alarm is cancelled, or re-armed on undo.
     */
    fun setReminderDone(id: String, done: Boolean) {
        viewModelScope.launch {
            runCatching {
                val repeats = _uiState.value.reminders.find { it.id == id }?.repeats == true
                if (done) {
                    alarmScheduler.cancelReminder(id)
                    if (repeats) reminderRepository.archiveReminder(id, true)
                    else reminderRepository.markDone(id)
                } else {
                    val restored = if (repeats) reminderRepository.archiveReminder(id, false)
                                   else reminderRepository.markUndone(id)
                    restored?.let { alarmScheduler.syncReminder(it) }
                }
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            runCatching {
                alarmScheduler.cancelReminder(id)
                reminderRepository.deleteReminder(id)
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
