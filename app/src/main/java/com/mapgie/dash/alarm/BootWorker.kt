package com.mapgie.dash.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.data.model.reminderInstant
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BootWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val pendingTasks = taskRepository.pendingReminders()
        pendingTasks.forEach { task ->
            val reminderAt = task.reminderInstant() ?: return@forEach
            alarmScheduler.scheduleTask(task.id, task.title, reminderAt)
        }
        val pendingReminders = reminderRepository.pendingReminders()
        pendingReminders.forEach { reminder ->
            val remindAt = reminder.remindAtInstant() ?: return@forEach
            alarmScheduler.scheduleReminder(reminder.id, reminder.subject, remindAt)
        }
        Result.success()
    }.getOrElse { Result.retry() }
}