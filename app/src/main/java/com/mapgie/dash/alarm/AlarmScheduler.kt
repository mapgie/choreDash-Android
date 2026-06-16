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

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTask(taskId: String, taskTitle: String, reminderAt: Instant) {
        if (reminderAt.isBefore(Instant.now())) return
        if (!canScheduleExactAlarms()) return

        val pending = buildPendingIntent(taskId, taskTitle)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderAt.toEpochMilli(),
            pending
        )
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
        if (!canScheduleExactAlarms()) return

        val pending = buildReminderPendingIntent(reminderId, subject, taskId)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            remindAt.toEpochMilli(),
            pending
        )
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