package com.mapgie.dash.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.data.model.reminderInstant
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BootWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val deliveryMode = settingsRepository.settings.first().deliveryMode

        val pendingReminders = reminderRepository.pendingReminders()
        // Tasks that already have a linked ReminderDto use scheduleReminder() below;
        // only reschedule the old-style task alarm for tasks without one.
        val taskIdsWithReminder = pendingReminders.mapNotNull { it.taskId }.toSet()

        val pendingTasks = taskRepository.pendingReminders()
        pendingTasks.forEach { task ->
            if (task.id !in taskIdsWithReminder) {
                val reminderAt = task.reminderInstant() ?: return@forEach
                alarmScheduler.scheduleTask(task.id, task.title, reminderAt, deliveryMode)
            }
        }

        pendingReminders.forEach { reminder ->
            val remindAt = reminder.remindAtInstant() ?: return@forEach
            alarmScheduler.scheduleReminder(reminder.id, reminder.subject, remindAt, reminder.taskId, deliveryMode)
        }
        Result.success()
    }.getOrElse { Result.retry() }
}
