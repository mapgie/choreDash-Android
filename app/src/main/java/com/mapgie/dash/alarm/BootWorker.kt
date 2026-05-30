package com.mapgie.dash.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mapgie.dash.data.model.reminderInstant
import com.mapgie.dash.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BootWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val pending = taskRepository.pendingReminders()
        pending.forEach { task ->
            val reminderAt = task.reminderInstant() ?: return@forEach
            alarmScheduler.scheduleTask(task.id, task.title, reminderAt)
        }
        Result.success()
    }.getOrElse { Result.retry() }
}