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
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID)

        when {
            taskId != null -> {
                val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task"
                NotificationHelper.showTaskReminder(context, taskId, taskTitle)

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
            reminderId != null -> {
                val subject = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_SUBJECT) ?: "Reminder"
                NotificationHelper.showReminderAlert(context, reminderId, subject)

                val result = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        reminderRepository.markReminded(reminderId)
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