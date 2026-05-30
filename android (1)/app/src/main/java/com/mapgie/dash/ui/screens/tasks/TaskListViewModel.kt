package com.mapgie.dash.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.alarm.AlarmScheduler
import com.mapgie.dash.data.model.DuePeriod
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskInsert
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUpdate
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.priorityEnum
import com.mapgie.dash.data.model.reminderInstant
import com.mapgie.dash.data.model.urgency
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

enum class TaskFilter { ALL, ACTIVE, DONE }
enum class TaskSort { PRIORITY, DUE, CREATED }
enum class OwnerFilter { ALL, MINE, UNASSIGNED }

data class TaskUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val tasks: List<TaskDto> = emptyList(),
    val owners: List<String> = emptyList(),
    val filter: TaskFilter = TaskFilter.ACTIVE,
    val sort: TaskSort = TaskSort.PRIORITY,
    val groupByCategory: Boolean = false,
    val ownerFilter: OwnerFilter = OwnerFilter.ALL,
    val ownerHandle: String = ""
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
                    OwnerFilter.MINE -> task.owner == ownerHandle
                    OwnerFilter.UNASSIGNED -> task.owner.isNullOrBlank()
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

    val activeTasks: List<TaskDto>
        get() = displayed.filter { it.completedAt == null }

    val doneTasks: List<TaskDto>
        get() = displayed.filter { it.completedAt != null }

    val categories: List<String>
        get() = tasks.mapNotNull { it.category }.filter { it.isNotBlank() }.distinct().sorted()
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _uiState.update { it.copy(ownerHandle = s.ownerHandle) }
            }
        }
        load()
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
                task.reminderInstant()?.let { at ->
                    alarmScheduler.scheduleTask(task.id, task.title, at)
                }
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateTask(id: String, update: TaskUpdate) {
        viewModelScope.launch {
            runCatching {
                // Cancel any existing alarm before applying the update
                _uiState.value.tasks.find { it.id == id }?.let { old ->
                    if (old.reminderAt != null) alarmScheduler.cancelTask(id)
                }
                val task = taskRepository.updateTask(id, update)
                task.reminderInstant()?.let { at ->
                    alarmScheduler.scheduleTask(task.id, task.title, at)
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
                alarmScheduler.cancelTask(id)
                taskRepository.markDone(id)
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun markUndone(id: String) {
        viewModelScope.launch {
            runCatching {
                taskRepository.markUndone(id)
                // Re-schedule reminder if it's still in the future
                taskRepository.loadTasks().find { it.id == id }?.let { task ->
                    task.reminderInstant()?.let { at ->
                        if (at.isAfter(Instant.now())) {
                            alarmScheduler.scheduleTask(id, task.title, at)
                        }
                    }
                }
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            runCatching {
                alarmScheduler.cancelTask(id)
                taskRepository.deleteTask(id)
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setFilter(f: TaskFilter) = _uiState.update { it.copy(filter = f) }
    fun setSort(s: TaskSort) = _uiState.update { it.copy(sort = s) }
    fun setGroupBy(enabled: Boolean) = _uiState.update { it.copy(groupByCategory = enabled) }
    fun setOwnerFilter(f: OwnerFilter) = _uiState.update { it.copy(ownerFilter = f) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
