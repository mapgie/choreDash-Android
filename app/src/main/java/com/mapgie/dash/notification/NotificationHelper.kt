package com.mapgie.dash.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mapgie.dash.MainActivity
import com.mapgie.dash.R
import com.mapgie.dash.alarm.AlarmActionReceiver

object NotificationHelper {
    // Three-way split replacing the old single dash_task_reminders_v3 channel.
    // Channel settings (importance, sound, vibration) are immutable once created,
    // so new channel ids are required for changes to take effect on existing installs.
    // Legacy channels are deleted in createChannels() below.
    const val CHANNEL_TASK_REMINDERS_ALARM = "dash_task_reminders_alarm_v1"
    const val CHANNEL_TASK_REMINDERS_NOTIF = "dash_task_reminders_notif_v1"
    const val CHANNEL_TASK_REMINDERS_SILENT = "dash_task_reminders_silent_v1"

    // Legacy channel ids — deleted on first run after upgrade
    private const val CHANNEL_TASK_REMINDERS_V3 = "dash_task_reminders_v3"
    private const val CHANNEL_TASK_REMINDERS_V2 = "dash_task_reminders_v2"
    private const val CHANNEL_TASK_REMINDERS_LEGACY = "dash_task_reminders"

    const val CHANNEL_CHORE_ALERTS = "dash_chore_alerts"

    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TITLE = "task_title"
    const val EXTRA_REMINDER_ID = "reminder_id"
    const val EXTRA_REMINDER_SUBJECT = "reminder_subject"

    // Single place that maps the delivery-mode setting ("ALARM"/"NOTIFICATION"/"SILENT")
    // to a channel. Resolved at delivery time (AlarmReceiver, BootWorker) so the current
    // setting always wins; the mode is never baked into alarm or action intents.
    fun channelForDeliveryMode(deliveryMode: String): String = when (deliveryMode) {
        "ALARM" -> CHANNEL_TASK_REMINDERS_ALARM
        "SILENT" -> CHANNEL_TASK_REMINDERS_SILENT
        else -> CHANNEL_TASK_REMINDERS_NOTIF
    }

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.deleteNotificationChannel(CHANNEL_TASK_REMINDERS_LEGACY)
        nm.deleteNotificationChannel(CHANNEL_TASK_REMINDERS_V2)
        nm.deleteNotificationChannel(CHANNEL_TASK_REMINDERS_V3)

        // Alarm channel: uses alarm sound and bypasses DND (only if access granted)
        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val alarmAudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASK_REMINDERS_ALARM,
                context.getString(R.string.channel_task_reminders_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_task_reminders_alarm_desc)
                setSound(alarmSoundUri, alarmAudioAttributes)
                enableVibration(true)
                // Only takes effect if the user has granted Do Not Disturb access;
                // see SettingsScreen's "Do Not Disturb access" permission row.
                if (nm.isNotificationPolicyAccessGranted) {
                    setBypassDnd(true)
                }
            }
        )

        // Notification channel: uses standard notification sound, no DND bypass
        val notifSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notifAudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASK_REMINDERS_NOTIF,
                context.getString(R.string.channel_task_reminders_notif_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_task_reminders_notif_desc)
                setSound(notifSoundUri, notifAudioAttributes)
                enableVibration(true)
            }
        )

        // Silent channel: low importance, no sound, no vibration
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASK_REMINDERS_SILENT,
                context.getString(R.string.channel_task_reminders_silent_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_task_reminders_silent_desc)
                setSound(null, null)
                enableVibration(false)
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
    fun showTaskReminder(context: Context, taskId: String, taskTitle: String, channelId: String = CHANNEL_TASK_REMINDERS_NOTIF) {
        val openIntent = PendingIntent.getActivity(
            context, taskId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_TASK_ID, taskId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = "com.mapgie.dash.ACTION_SNOOZE_TASK"
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
        }
        val snoozePI = PendingIntent.getBroadcast(
            context, "snooze_$taskId".hashCode(),
            snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = "com.mapgie.dash.ACTION_DONE_TASK"
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val donePI = PendingIntent.getBroadcast(
            context, "done_$taskId".hashCode(),
            doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Task reminder")
            .setContentText(taskTitle)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "Snooze 15 min", snoozePI)
            .addAction(0, "Done", donePI)
            .build()

        NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
    }

    @SuppressLint("MissingPermission")
    fun showReminderAlert(context: Context, reminderId: String, subject: String, channelId: String = CHANNEL_TASK_REMINDERS_NOTIF, taskId: String? = null) {
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

        val snoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = "com.mapgie.dash.ACTION_SNOOZE_REMINDER"
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_SUBJECT, subject)
            taskId?.let { putExtra(EXTRA_TASK_ID, it) }
        }
        val snoozePI = PendingIntent.getBroadcast(
            context, "snooze_reminder_$reminderId".hashCode(),
            snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = "com.mapgie.dash.ACTION_DONE_REMINDER"
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val donePI = PendingIntent.getBroadcast(
            context, "done_reminder_$reminderId".hashCode(),
            doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Reminder")
            .setContentText(subject)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "Snooze 15 min", snoozePI)
            .addAction(0, "Done", donePI)
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
