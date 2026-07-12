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
import com.mapgie.dash.notification.NotificationHelper
import com.mapgie.dash.notification.ReminderKind
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
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
        val channelId = NotificationHelper.channelId(ReminderKind.TASK_REMINDER, deliveryMode)
        val now = Instant.now()

        val pendingReminders = reminderRepository.pendingReminders()
        // Tasks that already have a linked ReminderDto use scheduleReminder() below;
        // only reschedule the old-style task alarm for tasks without one.
        val taskIdsWithReminder = pendingReminders.mapNotNull { it.taskId }.toSet()

        val pendingTasks = taskRepository.pendingReminders()
        pendingTasks.forEach { task ->
            if (task.id !in taskIdsWithReminder) {
                val reminderAt = task.reminderInstant() ?: return@forEach
                if (reminderAt.isAfter(now)) {
                    alarmScheduler.scheduleTask(task.id, task.title, reminderAt)
                } else {
                    // Fire time elapsed while the device was off (or the alarm was lost):
                    // deliver late rather than never.
                    NotificationHelper.showTaskReminder(applicationContext, task.id, task.title, channelId)
                    taskRepository.markReminded(task.id)
                }
            }
        }

        pendingReminders.forEach { reminder ->
            val remindAt = reminder.remindAtInstant() ?: return@forEach
            if (remindAt.isAfter(now)) {
                alarmScheduler.scheduleReminder(reminder.id, reminder.subject, remindAt, reminder.taskId)
            } else {
                NotificationHelper.showReminderAlert(
                    applicationContext, reminder.id, reminder.subject, channelId, reminder.taskId
                )
                reminderRepository.markReminded(reminder.id)
                reminder.taskId?.let { taskRepository.markReminded(it) }
            }
        }
        Result.success()
    }.getOrElse { Result.retry() }
}
