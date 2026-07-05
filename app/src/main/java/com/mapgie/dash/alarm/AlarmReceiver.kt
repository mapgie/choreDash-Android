package com.mapgie.dash.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.repository.TaskRepository
import com.mapgie.dash.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID)
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
        if (reminderId == null && taskId == null) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Delivery mode is presentation: resolve it from the current setting at
                // fire time so a settings change applies to already-scheduled alarms.
                // A settings read failure must never cost the user the notification.
                val channelId = NotificationHelper.channelForDeliveryMode(
                    runCatching { settingsRepository.settings.first().deliveryMode }
                        .getOrDefault("NOTIFICATION")
                )

                when {
                    // Check reminderId first: a task-linked reminder alarm carries BOTH extras,
                    // and must be handled as a reminder, not an old-style task alarm.
                    reminderId != null -> {
                        val subject = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_SUBJECT) ?: "Reminder"
                        NotificationHelper.showReminderAlert(context, reminderId, subject, channelId, taskId)
                        try {
                            reminderRepository.markReminded(reminderId)
                            // If this reminder is linked to a task, also mark it reminded in Supabase.
                            taskId?.let { taskRepository.markReminded(it) }
                        } catch (_: Exception) {
                            // Non-fatal: notification already shown; sync will happen on next open.
                        }
                    }
                    taskId != null -> {
                        val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task"
                        NotificationHelper.showTaskReminder(context, taskId, taskTitle, channelId)
                        try {
                            // Mark reminded=true in Supabase so other clients know the alert was sent.
                            taskRepository.markReminded(taskId)
                        } catch (_: Exception) {
                            // Non-fatal: notification already shown; sync will happen on next open.
                        }
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}
