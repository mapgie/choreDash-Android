package com.mapgie.dash.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.data.repository.TaskRepository
import com.mapgie.dash.notification.NotificationHelper
import com.mapgie.dash.ui.screens.reminder.ReminderViewKind
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
    @Inject lateinit var alarmScheduler: AlarmScheduler

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
                val settings = runCatching { settingsRepository.settings.first() }.getOrNull()
                val deliveryMode = settings?.deliveryMode ?: "NOTIFICATION"
                val featureWord = settings?.reminderLabel?.singular ?: "Reminder"

                when {
                    // Check reminderId first: a task-linked reminder alarm carries BOTH extras,
                    // and must be handled as a reminder, not an old-style task alarm.
                    reminderId != null -> {
                        val subject = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_SUBJECT) ?: featureWord
                        // The memo's own tone is read at fire time so a change applies to an armed alarm.
                        val sound = runCatching {
                            reminderRepository.loadReminders().firstOrNull { it.id == reminderId }?.sound
                        }.getOrNull()
                        NotificationHelper.showReminderAlert(context, reminderId, subject, deliveryMode, taskId, featureWord, sound)
                        // Alarm style only: ring on the alarm stream even when the phone is
                        // unlocked, where Android shows a heads-up rather than launching the
                        // full-screen intent (NotificationHelper.startAlarmRingScreen).
                        NotificationHelper.startAlarmRingScreen(context, deliveryMode, ReminderViewKind.REMINDER, reminderId, subject, sound)
                        try {
                            // A repeating memo comes back with its next ring armed; a
                            // once-only one is now spent and syncReminder just clears it.
                            reminderRepository.recordRing(reminderId)?.let { alarmScheduler.syncReminder(it) }
                            // If this reminder is linked to a task, also mark it reminded in Supabase.
                            taskId?.let { taskRepository.markReminded(it) }
                        } catch (_: Exception) {
                            // Non-fatal: notification already shown; sync will happen on next open.
                        }
                    }
                    taskId != null -> {
                        val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task"
                        NotificationHelper.showTaskReminder(context, taskId, taskTitle, deliveryMode)
                        NotificationHelper.startAlarmRingScreen(context, deliveryMode, ReminderViewKind.TASK, taskId, taskTitle)
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
