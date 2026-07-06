package com.mapgie.dash.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mapgie.dash.notification.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// Delivery mode (ALARM/NOTIFICATION/SILENT) is deliberately NOT part of the alarm
// intent: AlarmReceiver resolves it from the current setting at fire time, so a
// settings change applies to alarms that are already scheduled.
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTask(taskId: String, taskTitle: String, reminderAt: Instant) {
        if (reminderAt.isBefore(Instant.now())) return

        val pending = buildPendingIntent(taskId, taskTitle)
        setAlarm(reminderAt, pending)
    }

    fun cancelTask(taskId: String) {
        val pending = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            Intent(context, AlarmReceiver::class.java).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun scheduleReminder(reminderId: String, subject: String, remindAt: Instant, taskId: String? = null) {
        if (remindAt.isBefore(Instant.now())) return

        val pending = buildReminderPendingIntent(reminderId, subject, taskId)
        setAlarm(remindAt, pending)
    }

    fun cancelReminder(reminderId: String) {
        val pending = PendingIntent.getBroadcast(
            context,
            reminderRequestCode(reminderId),
            Intent(context, AlarmReceiver::class.java).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms()
        else true

    // Exact when permitted; otherwise an inexact alarm. A reminder delayed by batching
    // still beats one that is silently dropped because the exact-alarm permission was
    // revoked between scheduling opportunities.
    private fun setAlarm(fireAt: Instant, pending: PendingIntent) {
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt.toEpochMilli(), pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt.toEpochMilli(), pending)
        }
    }

    private fun buildPendingIntent(taskId: String, taskTitle: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            setPackage(context.packageName)
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
            putExtra(NotificationHelper.EXTRA_TASK_TITLE, taskTitle)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildReminderPendingIntent(reminderId: String, subject: String, taskId: String? = null): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            setPackage(context.packageName)
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationHelper.EXTRA_REMINDER_SUBJECT, subject)
            taskId?.let { putExtra(NotificationHelper.EXTRA_TASK_ID, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            reminderRequestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Distinct request-code namespace so reminder alarms never collide with task alarms
    private fun reminderRequestCode(reminderId: String): Int = ("reminder_$reminderId").hashCode()
}
