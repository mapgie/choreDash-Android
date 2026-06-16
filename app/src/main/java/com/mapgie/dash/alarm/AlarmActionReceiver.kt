package com.mapgie.dash.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.repository.TaskRepository
import com.mapgie.dash.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActionReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var reminderRepository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val nm = NotificationManagerCompat.from(context)

        when (intent.action) {
            "com.mapgie.dash.ACTION_SNOOZE_TASK" -> {
                val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
                val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task"
                val deliveryMode = intent.getStringExtra("EXTRA_DELIVERY_MODE") ?: "NOTIFICATION"
                nm.cancel(taskId.hashCode())
                alarmScheduler.scheduleTask(
                    taskId,
                    taskTitle,
                    Instant.now().plus(15, ChronoUnit.MINUTES),
                    deliveryMode
                )
            }

            "com.mapgie.dash.ACTION_DONE_TASK" -> {
                val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
                nm.cancel(taskId.hashCode())
                val result = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        taskRepository.markReminded(taskId)
                    } catch (_: Exception) {
                        // Non-fatal: notification already dismissed; sync will happen on next open.
                    } finally {
                        result.finish()
                    }
                }
            }

            "com.mapgie.dash.ACTION_SNOOZE_REMINDER" -> {
                val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID) ?: return
                val subject = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_SUBJECT) ?: "Reminder"
                nm.cancel(("reminder_$reminderId").hashCode())
                alarmScheduler.scheduleReminder(
                    reminderId,
                    subject,
                    Instant.now().plus(15, ChronoUnit.MINUTES)
                )
            }

            "com.mapgie.dash.ACTION_DONE_REMINDER" -> {
                val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID) ?: return
                nm.cancel(("reminder_$reminderId").hashCode())
                val result = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        reminderRepository.markDone(reminderId)
                    } catch (_: Exception) {
                        // Non-fatal: notification already dismissed; sync will happen on next open.
                    } finally {
                        result.finish()
                    }
                }
            }
        }
    }
}
