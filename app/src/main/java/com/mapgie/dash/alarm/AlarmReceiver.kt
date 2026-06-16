package com.mapgie.dash.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.repository.TaskRepository
import com.mapgie.dash.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var reminderRepository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID)
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)

        val channelId = when (intent.getStringExtra("EXTRA_DELIVERY_MODE") ?: "NOTIFICATION") {
            "ALARM" -> NotificationHelper.CHANNEL_TASK_REMINDERS_ALARM
            "SILENT" -> NotificationHelper.CHANNEL_TASK_REMINDERS_SILENT
            else -> NotificationHelper.CHANNEL_TASK_REMINDERS_NOTIF
        }

        when {
            // Check reminderId first: a task-linked reminder alarm carries BOTH extras,
            // and must be handled as a reminder, not an old-style task alarm.
            reminderId != null -> {
                val subject = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_SUBJECT) ?: "Reminder"
                NotificationHelper.showReminderAlert(context, reminderId, subject, channelId)

                val result = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        reminderRepository.markReminded(reminderId)
                        // If this reminder is linked to a task, also mark it reminded in Supabase.
                        taskId?.let { taskRepository.markReminded(it) }
                    } catch (_: Exception) {
                        // Non-fatal: notification already shown; sync will happen on next open.
                    } finally {
                        result.finish()
                    }
                }
            }
            taskId != null -> {
                val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task"
                NotificationHelper.showTaskReminder(context, taskId, taskTitle, channelId)

                // Mark reminded=true in Supabase so other clients know the alert was sent.
                val result = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        taskRepository.markReminded(taskId)
                    } catch (_: Exception) {
                        // Non-fatal: notification already shown; sync will happen on next open.
                    } finally {
                        result.finish()
                    }
                }
            }
        }
    }
}
