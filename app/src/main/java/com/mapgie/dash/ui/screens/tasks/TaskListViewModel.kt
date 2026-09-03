package com.mapgie.dash.ui.screens.tasks

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.alarm.AlarmScheduler
import com.mapgie.dash.data.model.CategoryCatalog
import com.mapgie.dash.data.model.DraftStore
import com.mapgie.dash.data.model.OwnerFilter
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.SortOrder
import com.mapgie.dash.data.model.TaskDraft
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskInsert
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskSortKey
import com.mapgie.dash.data.model.TaskUpdate
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.priorityEnum
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.data.model.reminderInstant
import com.mapgie.dash.data.model.urgency
import com.mapgie.dash.data.preferences.CategoryStyleStore
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.repository.TaskRepository
import com.mapgie.dash.widget.PinChooserState
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

/** Label shown on a group header for tasks with no category. */
const val OTHER_CATEGORY_LABEL = "Other"

data class TaskUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val tasks: List<TaskDto> = emptyList(),
    val owners: List<String> = emptyList(),
    val sort: SortOrder<TaskSortKey> = SortOrder(TaskSortKey.PRIORITY),
    val groupByCategory: Boolean = true,
    val ownerFilter: OwnerFilter = OwnerFilter.EVERYONE,
    val ownerHandle: String = "",
    val pinnedTaskId: String? = null,
    val hideThresholdDays: Int = -1,
    val zenMode: Boolean = false,
    val zenSortAscending: Boolean = true,
    val catalog: CategoryCatalog = CategoryCatalog(),
    val pinChooser: PinChooserState? = null,
) {
    val displayed: List<TaskDto>
        get() {
            // Archived tasks never show; open and done tasks are split into the
            // main list and the collapsible Done section by activeTasks/doneTasks.
            val filtered = tasks.filter { task ->
                task.archivedAt == null && ownerFilter.matches(task.owner, ownerHandle)
            }
            if (zenMode) {
                // Zen sort: ascending = most urgent (overdue) first, matching the due sort order
                val byUrgency = filtered.sortedWith(compareBy({ it.urgency().ordinal }, { it.dueDate ?: "" }))
                return if (zenSortAscending) byUrgency else byUrgency.reversed()
            }
            return filtered.sortedForPill(sort)
        }

    /** Open tasks in the main list, minus anything due beyond the hide threshold. */
    val activeTasks: List<TaskDto>
        get() = openTasks.filterNot(::hiddenByThreshold)

    private val openTasks: List<TaskDto>
        get() = displayed.filter { it.completedAt == null }

    private fun hiddenByThreshold(task: TaskDto): Boolean {
        if (hideThresholdDays < 0) return false
        val date = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
        val cutoff = LocalDate.now(ZoneId.systemDefault()).plusDays(hideThresholdDays.toLong())
        return date.isAfter(cutoff)
    }

    /** Open tasks kept out of the list by the far-future hide threshold. */
    val hiddenCount: Int
        get() = openTasks.count(::hiddenByThreshold)

    val doneTasks: List<TaskDto>
        get() = displayed.filter { it.completedAt != null }

    /**
     * The zen list: open tasks in zen order, then anything finished today so a
     * tick stays visible (struck through, faded) instead of vanishing mid-breath.
     */
    val zenRows: List<TaskDto>
        get() = activeTasks + doneTasks.filter { it.completedToday() }

    private fun TaskDto.completedToday(): Boolean {
        val at = completedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
        return at.atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now(ZoneId.systemDefault())
    }

    /**
     * [activeTasks] split into category groups in catalog order (user order,
     * then unlisted names alphabetically, General last); tasks with no category
     * land in an "Other" group at the end.
     */
    val grouped: List<Pair<String, List<TaskDto>>>
        get() {
            val groups = activeTasks.groupBy { it.category?.takeIf { c -> c.isNotBlank() } }
            return groups.entries
                .sortedWith(
                    compareBy<Map.Entry<String?, List<TaskDto>>> { it.key == null }
                        .thenBy { catalog.rankOf(it.key) }
                        .thenBy { it.key?.lowercase() ?: "" }
                )
                .map { (category, tasks) -> (category ?: OTHER_CATEGORY_LABEL) to tasks }
        }

    val categories: List<String>
        get() = catalog.sorted(tasks.mapNotNull { it.category }.filter { it.isNotBlank() })
}

private fun TaskDto.priorityRank(): Int = when (priorityEnum()) {
    TaskPriority.HIGHER -> 0
    TaskPriority.NORMAL -> 1
    TaskPriority.LOWER -> 2
}

/**
 * Orders tasks for the sort pill. Undated tasks always trail dated ones on the
 * due key, in both directions, so reversing never floats "no date" to the top.
 */
fun List<TaskDto>.sortedForPill(order: SortOrder<TaskSortKey>): List<TaskDto> = when (order.key) {
    TaskSortKey.PRIORITY ->
        if (!order.reversed) sortedWith(compareBy({ it.priorityRank() }, { it.urgency().ordinal }))
        else sortedWith(compareByDescending<TaskDto> { it.priorityRank() }.thenBy { it.urgency().ordinal })
    TaskSortKey.DUE -> {
        val (dated, undated) = partition { it.urgency() != TaskUrgency.NONE }
        val soonest = dated.sortedWith(
            compareBy({ it.urgency().ordinal }, { it.dueDate ?: "" }, { it.priorityRank() })
        )
        (if (order.reversed) soonest.reversed() else soonest) + undated.sortedBy { it.priorityRank() }
    }
    TaskSortKey.CREATED ->
        if (!order.reversed) sortedByDescending { it.createdAt } else sortedBy { it.createdAt }
    TaskSortKey.NAME ->
        if (!order.reversed) sortedBy { it.title.lowercase() } else sortedByDescending { it.title.lowercase() }
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val pinnedItemStore: PinnedItemStore,
    private val categoryStyleStore: CategoryStyleStore,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    /**
     * Unsaved Edit task sheet drafts by task id (or NEW_DRAFT_KEY for the New
     * task sheet), kept in saved state so they outlive rotation and process
     * death within the session. The sheet offers them back; it never auto-applies.
     */
    val taskDrafts = DraftStore(savedStateHandle, "task_drafts", TaskDraft.serializer())

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _uiState.update {
                    it.copy(
                        ownerHandle = s.ownerHandle,
                        groupByCategory = s.groupTasksByCategory,
                        hideThresholdDays = s.taskHideThresholdDays,
                        zenMode = s.taskZenMode,
                        sort = s.taskSort,
                    )
                }
            }
        }
        viewModelScope.launch {
            categoryStyleStore.catalog.collect { catalog ->
                _uiState.update { it.copy(catalog = catalog) }
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
        val item = PinnedWidgetItem(PinnedItemType.TASK, taskId)
        viewModelScope.launch {
            val placedWidgetIds = pinnedItemStore.placedAppWidgetIds()
            if (placedWidgetIds.size > 1) {
                _uiState.update { it.copy(pinChooser = PinChooserState(item, placedWidgetIds)) }
                return@launch
            }
            pinnedItemStore.togglePinned(item)
            // With at most one widget placed, that widget should always track the default
            // pin, not a stale per-instance override left over from when more were placed.
            placedWidgetIds.singleOrNull()?.let { pinnedItemStore.setPinnedFor(it, null) }
            WidgetUpdater.updateAll(appContext)
        }
    }

    /** Commits the pin to a specific widget instance chosen from [TaskUiState.pinChooser]. */
    fun pinToWidget(appWidgetId: Int) {
        val chooser = _uiState.value.pinChooser ?: return
        _uiState.update { it.copy(pinChooser = null) }
        viewModelScope.launch {
            pinnedItemStore.togglePinnedFor(appWidgetId, chooser.item)
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun dismissPinChooser() {
        _uiState.update { it.copy(pinChooser = null) }
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

    /** A standalone reminder linked to a task, from the sheet's Remind action. */
    fun addReminderForTask(insert: ReminderInsert) {
        viewModelScope.launch {
            runCatching {
                val reminder = reminderRepository.addReminder(insert)
                reminder.remindAtInstant()?.let { at ->
                    alarmScheduler.scheduleReminder(reminder.id, reminder.subject, at, insert.taskId)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /** Marks a task done, at [at] when the sheet's DONE control picked an earlier time. */
    fun markDone(id: String, at: Instant? = null) {
        viewModelScope.launch {
            runCatching {
                alarmScheduler.cancelTask(id)
                reminderRepository.loadReminders()
                    .filter { it.taskId == id && it.archivedAt == null }
                    .forEach { reminder ->
                        alarmScheduler.cancelReminder(reminder.id)
                        reminderRepository.archiveReminder(reminder.id, true)
                    }
                taskRepository.markDone(id, at ?: Instant.now())
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

    /** Sort pill choice; applied immediately and persisted. */
    fun setSort(order: SortOrder<TaskSortKey>) {
        _uiState.update { it.copy(sort = order) }
        viewModelScope.launch { settingsRepository.setTaskSort(order) }
    }

    fun setGroupBy(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGroupTasksByCategory(enabled) }
    }
    fun setOwnerFilter(f: OwnerFilter) = _uiState.update { it.copy(ownerFilter = f) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun setZenMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTaskZenMode(enabled) }
    }

    fun setZenSort(ascending: Boolean) {
        _uiState.update { it.copy(zenSortAscending = ascending) }
    }
}
