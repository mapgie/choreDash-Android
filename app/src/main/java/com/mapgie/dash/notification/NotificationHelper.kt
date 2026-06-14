package com.mapgie.dash.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mapgie.dash.MainActivity
import com.mapgie.dash.R

object NotificationHelper {
    // v2: bypasses Do Not Disturb. NotificationChannel settings like importance
    // and bypassDnd are immutable once created, so existing installs need a new
    // channel id for the DND bypass to actually take effect. The old channel is
    // deleted in createChannels() below.
    const val CHANNEL_TASK_REMINDERS = "dash_task_reminders_v2"
    private const val CHANNEL_TASK_REMINDERS_LEGACY = "dash_task_reminders"
    const val CHANNEL_CHORE_ALERTS = "dash_chore_alerts"

    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TITLE = "task_title"
    const val EXTRA_REMINDER_ID = "reminder_id"
    const val EXTRA_REMINDER_SUBJECT = "reminder_subject"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.deleteNotificationChannel(CHANNEL_TASK_REMINDERS_LEGACY)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                context.getString(R.string.channel_task_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_task_reminders_desc)
                enableVibration(true)
                // Only takes effect if the user has granted Do Not Disturb access;
                // see SettingsScreen's "Do Not Disturb access" permission row.
                setBypassDnd(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CHORE_ALERTS,
                context.getString(R.string.channel_chore_alerts_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_chore_alerts_desc)
                enableVibration(false)
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun showTaskReminder(context: Context, taskId: String, taskTitle: String) {
        val openIntent = PendingIntent.getActivity(
            context, taskId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_TASK_ID, taskId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Task reminder")
            .setContentText(taskTitle)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
    }

    @SuppressLint("MissingPermission")
    fun showReminderAlert(context: Context, reminderId: String, subject: String) {
        val notifyId = ("reminder_$reminderId").hashCode()
        val openIntent = PendingIntent.getActivity(
            context, notifyId,
            Intent(context, MainActivity::class.java).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_REMINDER_ID, reminderId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Reminder")
            .setContentText(subject)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(notifyId, notification)
    }

    @SuppressLint("MissingPermission")
    fun showStaleChoresSummary(context: Context, choreLabels: List<String>) {
        if (choreLabels.isEmpty()) return
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                setPackage(context.packageName)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = if (choreLabels.size == 1) {
            "${choreLabels.first()} is overdue"
        } else {
            "${choreLabels.size} chores are overdue: ${choreLabels.take(3).joinToString(", ")}"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_CHORE_ALERTS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Overdue chores")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(1, notification)
    }
}
