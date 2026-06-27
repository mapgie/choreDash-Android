package com.mapgie.dash.ui.screens.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.alarm.AlarmScheduler
import com.mapgie.dash.data.model.DuePeriod
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskInsert
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUpdate
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.priorityEnum
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.data.model.reminderInstant
import com.mapgie.dash.data.model.urgency
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.repository.TaskRepository
import com.mapgie.dash.widget.PinnedItemStore
import com.mapgie.dash.widget.PinnedItemType
import com.mapgie.dash.widget.PinnedWidgetItem
import com.mapgie.dash.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class TaskFilter { ALL, ACTIVE, DONE }
enum class TaskSort { PRIORITY, DUE, CREATED }
enum class OwnerFilter { ALL, MINE }

data class TaskUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val tasks: List<TaskDto> = emptyList(),
    val owners: List<String> = emptyList(),
    val filter: TaskFilter = TaskFilter.ACTIVE,
    val sort: TaskSort = TaskSort.PRIORITY,
    val groupByCategory: Boolean = true,
    val ownerFilter: OwnerFilter = OwnerFilter.ALL,
    val ownerHandle: String = "",
    val pinnedTaskId: String? = null,
    val hideThresholdDays: Int = -1,
    val showDistantTasks: Boolean = false,
) {
    val displayed: List<TaskDto>
        get() {
            val filtered = tasks.filter { task ->
                val matchesStatus = when (filter) {
                    TaskFilter.ALL -> task.archivedAt == null
                    TaskFilter.ACTIVE -> task.completedAt == null && task.archivedAt == null
                    TaskFilter.DONE -> task.completedAt != null && task.archivedAt == null
                }
                val matchesOwner = when (ownerFilter) {
                    OwnerFilter.ALL -> true
                    OwnerFilter.MINE -> task.owner == null || task.owner == ownerHandle
                }
                matchesStatus && matchesOwner
            }
            return when (sort) {
                TaskSort.PRIORITY -> filtered.sortedWith(
                    compareBy(
                        { when (it.priorityEnum()) { TaskPriority.HIGHER -> 0; TaskPriority.NORMAL -> 1; TaskPriority.LOWER -> 2 } },
                        { it.urgency().ordinal }
                    )
                )
                TaskSort.DUE -> filtered.sortedWith(
                    compareBy(
                        { it.urgency().ordinal },
                        { when (it.priorityEnum()) { TaskPriority.HIGHER -> 0; TaskPriority.NORMAL -> 1; TaskPriority.LOWER -> 2 } }
                    )
                )
                TaskSort.CREATED -> filtered.sortedByDescending { it.createdAt }
            }
        }

    val distantTasks: List<TaskDto>
        get() {
            if (hideThresholdDays < 0) return emptyList()
            val cutoff = LocalDate.now(ZoneId.systemDefault()).plusDays(hideThresholdDays.toLong())
            return displayed.filter { task ->
                task.completedAt == null &&
                    task.dueDate != null &&
                    runCatching { LocalDate.parse(task.dueDate) }.getOrNull()?.isAfter(cutoff) == true
            }
        }

    val activeTasks: List<TaskDto>
        get() {
            val distantIds = distantTasks.map { it.id }.toSet()
            return displayed.filter { it.completedAt == null && it.id !in distantIds }
        }

    val doneTasks: List<TaskDto>
        get() = displayed.filter { it.completedAt != null }

    val categories: List<String>
        get() = tasks.mapNotNull { it.category }.filter { it.isNotBlank() }.distinct().sorted()
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val pinnedItemStore: PinnedItemStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _uiState.update {
                    it.copy(
                        ownerHandle = s.ownerHandle,
                        groupByCategory = s.groupTasksByCategory,
                        hideThresholdDays = s.taskHideThresholdDays,
                    )
                }
            }
        }
        viewModelScope.launch {
            pinnedItemStore.pinnedItem.collect { pinned ->
                val pinnedTaskId = pinned?.takeIf { it.type == PinnedItemType.TASK }?.id
                _uiState.update { it.copy(pinnedTaskId = pinnedTaskId) }
            }
        }
        load()
    }

    fun togglePin(taskId: String) {
        viewModelScope.launch {
            pinnedItemStore.togglePinned(PinnedWidgetItem(PinnedItemType.TASK, taskId))
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                val tasks = taskRepository.loadTasks()
                val owners = taskRepository.loadOwners()
                _uiState.update { it.copy(loading = false, tasks = tasks, owners = owners) }
            }.onFailure { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun addTask(insert: TaskInsert) {
        viewModelScope.launch {
            runCatching {
                val task = taskRepository.addTask(insert)
                if (task.reminderAt != null) {
                    task.reminderInstant()?.let { at ->
                        val reminder = reminderRepository.addReminder(
                            ReminderInsert(subject = task.title, remindAt = task.reminderAt, taskId = task.id)
                        )
                        alarmScheduler.scheduleReminder(reminder.id, reminder.subject, at, task.id)
                    }
                }
                load()
                WidgetUpdater.updateAll(appContext)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateTask(id: String, update: TaskUpdate) {
        viewModelScope.launch {
            runCatching {
                // Cancel old-style task alarm (backward compat for reminders created before this change)
                _uiState.value.tasks.find { it.id == id }?.let { old ->
                    if (old.reminderAt != null) alarmScheduler.cancelTask(id)
                }
                // Cancel and delete any existing ReminderDto linked to this task
                reminderRepository.loadReminders()
                    .filter { it.taskId == id && it.archivedAt == null }
                    .forEach { reminder ->
                        alarmScheduler.cancelReminder(reminder.id)
                        reminderRepository.deleteReminder(reminder.id)
                    }
                val task = taskRepository.updateTask(id, update)
                if (task.reminderAt != null) {
                    task.reminderInstant()?.let { at ->
                        val reminder = reminderRepository.addReminder(
                            ReminderInsert(subject = task.title, remindAt = task.reminderAt, taskId = task.id)
                        )
                        alarmScheduler.scheduleReminder(reminder.id, reminder.subject, at, task.id)
                    }
                }
                load()
                WidgetUpdater.updateAll(appContext)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun markDone(id: String) {
        viewModelScope.launch {
            runCatching {
                alarmScheduler.cancelTask(id)
                reminderRepository.loadReminders()
                    .filter { it.taskId == id && it.archivedAt == null }
                    .forEach { reminder ->
                        alarmScheduler.cancelReminder(reminder.id)
                        reminderRepository.archiveReminder(reminder.id, true)
                    }
                taskRepository.markDone(id)
                load()
                WidgetUpdater.updateAll(appContext)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun markUndone(id: String) {
        viewModelScope.launch {
            runCatching {
                taskRepository.markUndone(id)
                val archivedReminders = reminderRepository.loadReminders()
                    .filter { it.taskId == id && it.archivedAt != null }
                archivedReminders.forEach { reminder ->
                    reminderRepository.archiveReminder(reminder.id, false)
                    if (!reminder.reminded && reminder.completedAt == null) {
                        reminder.remindAtInstant()?.let { at ->
                            if (at.isAfter(Instant.now())) {
                                alarmScheduler.scheduleReminder(reminder.id, reminder.subject, at, id)
                            }
                        }
                    }
                }
                if (archivedReminders.isEmpty()) {
                    // Old-style task: reschedule via task alarm
                    taskRepository.loadTasks().find { it.id == id }?.let { task ->
                        task.reminderInstant()?.let { at ->
                            if (at.isAfter(Instant.now())) {
                                alarmScheduler.scheduleTask(id, task.title, at)
                            }
                        }
                    }
                }
                load()
                WidgetUpdater.updateAll(appContext)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            runCatching {
                alarmScheduler.cancelTask(id)
                reminderRepository.loadReminders()
                    .filter { it.taskId == id }
                    .forEach { reminder ->
                        alarmScheduler.cancelReminder(reminder.id)
                        reminderRepository.deleteReminder(reminder.id)
                    }
                taskRepository.deleteTask(id)
                if (_uiState.value.pinnedTaskId == id) {
                    pinnedItemStore.setPinned(null)
                }
                load()
                WidgetUpdater.updateAll(appContext)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleShowDistantTasks() = _uiState.update { it.copy(showDistantTasks = !it.showDistantTasks) }
    fun setFilter(f: TaskFilter) = _uiState.update { it.copy(filter = f) }
    fun setSort(s: TaskSort) = _uiState.update { it.copy(sort = s) }
    fun setGroupBy(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGroupTasksByCategory(enabled) }
    }
    fun setOwnerFilter(f: OwnerFilter) = _uiState.update { it.copy(ownerFilter = f) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
