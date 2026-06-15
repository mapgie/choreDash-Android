package com.mapgie.dash.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.alarm.AlarmScheduler
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.remindAtInstant
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

data class ReminderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val reminders: List<ReminderDto> = emptyList(),
    val chores: List<Chore> = emptyList(),
    val tasks: List<TaskDto> = emptyList()
) {
    val active: List<ReminderDto>
        get() = reminders.filter { it.archivedAt == null && it.completedAt == null }.sortedBy { it.remindAt }

    val done: List<ReminderDto>
        get() = reminders.filter { it.archivedAt == null && it.completedAt != null }.sortedByDescending { it.remindAt }

    val archived: List<ReminderDto>
        get() = reminders.filter { it.archivedAt != null }.sortedByDescending { it.remindAt }

    fun linkedLabel(reminder: ReminderDto): String? {
        reminder.choreId?.let { id -> chores.find { it.id == id }?.let { return "Chore: ${it.label}" } }
        reminder.taskId?.let { id -> tasks.find { it.id == id }?.let { return "Task: ${it.title}" } }
        return null
    }
}

@HiltViewModel
class RemindersListViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val choreRepository: ChoreRepository,
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    init {
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

    fun addReminder(insert: ReminderInsert) {
        viewModelScope.launch {
            runCatching {
                val reminder = reminderRepository.addReminder(insert)
                reminder.remindAtInstant()?.let { at ->
                    alarmScheduler.scheduleReminder(reminder.id, reminder.subject, at)
                }
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun markDone(id: String) {
        viewModelScope.launch {
            runCatching {
                alarmScheduler.cancelReminder(id)
                reminderRepository.markDone(id)
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun markUndone(id: String) {
        viewModelScope.launch {
            runCatching {
                reminderRepository.markUndone(id)
                _uiState.value.reminders.find { it.id == id }?.let { reminder ->
                    reminder.remindAtInstant()?.let { at ->
                        if (at.isAfter(Instant.now())) {
                            alarmScheduler.scheduleReminder(id, reminder.subject, at)
                        }
                    }
                }
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun editReminder(id: String, insert: ReminderInsert) {
        viewModelScope.launch {
            runCatching {
                alarmScheduler.cancelReminder(id)
                val reminder = reminderRepository.updateReminder(id, insert)
                if (reminder.completedAt == null) {
                    reminder.remindAtInstant()?.let { at ->
                        if (at.isAfter(Instant.now())) {
                            alarmScheduler.scheduleReminder(reminder.id, reminder.subject, at)
                        }
                    }
                }
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun archiveReminder(id: String, archived: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (archived) {
                    alarmScheduler.cancelReminder(id)
                } else {
                    _uiState.value.reminders.find { it.id == id }?.let { reminder ->
                        if (reminder.completedAt == null) {
                            reminder.remindAtInstant()?.let { at ->
                                if (at.isAfter(Instant.now())) {
                                    alarmScheduler.scheduleReminder(id, reminder.subject, at)
                                }
                            }
                        }
                    }
                }
                reminderRepository.archiveReminder(id, archived)
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
