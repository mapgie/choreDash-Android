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
    const val CHANNEL_TASK_REMINDERS = "dash_task_reminders"
    const val CHANNEL_CHORE_ALERTS = "dash_chore_alerts"

    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TITLE = "task_title"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                context.getString(R.string.channel_task_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_task_reminders_desc)
                enableVibration(true)
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
    fun showStaleChoresSummary(context: Context, choreLabels: List<String>) {
        if (choreLabels.isEmpty()) return
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
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
