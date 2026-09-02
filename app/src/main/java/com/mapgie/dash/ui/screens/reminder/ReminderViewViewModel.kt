package com.mapgie.dash.ui.screens.reminder

import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.alarm.AlarmScheduler
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.data.model.reminderInstant
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Route for the full-screen nudge opened from a reminder notification. */
const val REMINDER_VIEW_ROUTE = "reminder/{kind}/{id}"
const val REMINDER_VIEW_ARG_KIND = "kind"
const val REMINDER_VIEW_ARG_ID = "id"

fun reminderViewRoute(kind: ReminderViewKind, id: String): String =
    "reminder/${kind.routeArg}/${Uri.encode(id)}"

data class ReminderViewUiState(
    val loading: Boolean = true,
    /** The record behind the notification no longer exists (or the route was malformed). */
    val missing: Boolean = false,
    val kind: ReminderViewKind? = null,
    val subject: String = "",
    val remindAt: Instant? = null,
    val next: UpcomingNudge? = null,
    /** Set once Done or Snooze has been applied; the screen pops back. */
    val finished: Boolean = false,
    val error: String? = null,
)

/**
 * Backs [ReminderViewScreen]. Done and Snooze mirror the notification's own
 * action buttons in `AlarmActionReceiver` exactly (same repository calls, same
 * notification ids, same task link preserved across a reminder snooze), except
 * the snooze here is one hour rather than fifteen minutes.
 */
@HiltViewModel
class ReminderViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reminderRepository: ReminderRepository,
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val kind: ReminderViewKind? =
        ReminderViewKind.fromRouteArg(savedStateHandle[REMINDER_VIEW_ARG_KIND])
    private val id: String? = savedStateHandle[REMINDER_VIEW_ARG_ID]

    // Task id a standalone reminder is linked to; carried across Snooze so the
    // re-fired alarm still marks the task reminded (see AlarmActionReceiver).
    private var linkedTaskId: String? = null

    private val _uiState = MutableStateFlow(ReminderViewUiState())
    val uiState: StateFlow<ReminderViewUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val kind = kind
        val id = id
        if (kind == null || id.isNullOrBlank()) {
            _uiState.update { it.copy(loading = false, missing = true) }
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                when (kind) {
                    ReminderViewKind.REMINDER -> {
                        val reminder = reminderRepository.loadReminders().firstOrNull { it.id == id }
                        reminder?.let {
                            linkedTaskId = it.taskId
                            it.subject to it.remindAtInstant()
                        }
                    }
                    ReminderViewKind.TASK -> {
                        val task = taskRepository.loadTasks().firstOrNull { it.id == id }
                        task?.let { it.title to it.reminderInstant() }
                    }
                }
            }
            val loaded = result.getOrNull()
            if (loaded == null) {
                _uiState.update {
                    it.copy(loading = false, missing = true, kind = kind, error = result.exceptionOrNull()?.message)
                }
                return@launch
            }
            val (subject, remindAt) = loaded
            _uiState.update {
                it.copy(loading = false, kind = kind, subject = subject, remindAt = remindAt)
            }
            val next = ReminderViewText.nextAfter(kind, id, loadUpcoming())
            _uiState.update { it.copy(next = next) }
        }
    }

    // Each source degrades to empty on its own: a Supabase failure must not hide
    // the on-device reminders from the footer, and vice versa.
    private suspend fun loadUpcoming(): List<UpcomingNudge> = coroutineScope {
        val reminders = async {
            runCatching { reminderRepository.pendingReminders() }.getOrDefault(emptyList())
                .mapNotNull { r ->
                    r.remindAtInstant()?.let { UpcomingNudge(ReminderViewKind.REMINDER, r.id, r.subject, it) }
                }
        }
        val tasks = async {
            runCatching { taskRepository.pendingReminders() }.getOrDefault(emptyList())
                .mapNotNull { t ->
                    t.reminderInstant()?.let { UpcomingNudge(ReminderViewKind.TASK, t.id, t.title, it) }
                }
        }
        reminders.await() + tasks.await()
    }

    fun markDone() {
        val kind = kind ?: return
        val id = id ?: return
        viewModelScope.launch {
            cancelNotification(kind, id)
            runCatching {
                when (kind) {
                    ReminderViewKind.REMINDER -> reminderRepository.markDone(id)
                    ReminderViewKind.TASK -> taskRepository.markReminded(id)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun snoozeOneHour() {
        val kind = kind ?: return
        val id = id ?: return
        val subject = _uiState.value.subject
        cancelNotification(kind, id)
        val fireAt = Instant.now().plus(1, ChronoUnit.HOURS)
        when (kind) {
            ReminderViewKind.REMINDER -> alarmScheduler.scheduleReminder(
                id,
                subject.ifBlank { "Reminder" },
                fireAt,
                linkedTaskId
            )
            ReminderViewKind.TASK -> alarmScheduler.scheduleTask(
                id,
                subject.ifBlank { "Task" },
                fireAt
            )
        }
        _uiState.update { it.copy(finished = true) }
    }

    // Same notify ids NotificationHelper posts under.
    private fun cancelNotification(kind: ReminderViewKind, id: String) {
        val notifyId = when (kind) {
            ReminderViewKind.REMINDER -> ("reminder_$id").hashCode()
            ReminderViewKind.TASK -> id.hashCode()
        }
        NotificationManagerCompat.from(context).cancel(notifyId)
    }
}
