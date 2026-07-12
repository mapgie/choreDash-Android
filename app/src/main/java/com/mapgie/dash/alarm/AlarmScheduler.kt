package com.mapgie.dash.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.mapgie.dash.notification.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// Delivery mode (ALARM/NOTIFICATION/SILENT) is deliberately NOT part of the alarm
// intent: AlarmReceiver resolves it from the current setting at fire time, so a
// settings change applies to alarms that are already scheduled.
//
// Every alarm intent is stamped with a unique data URI. PendingIntent identity is
// (request code + Intent.filterEquals), and filterEquals ignores extras — with only
// hashCode-derived request codes, two ids whose hashes collide would silently cancel
// or clobber each other's alarms. The URI makes every task/reminder a distinct
// intent regardless of request code (same defence GaMeD uses).
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
        val intent = Intent(context, AlarmReceiver::class.java)
            .setPackage(context.packageName)
            .setData(taskAlarmUri(taskId))
        val pending = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
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
        val intent = Intent(context, AlarmReceiver::class.java)
            .setPackage(context.packageName)
            .setData(reminderAlarmUri(reminderId))
        val pending = PendingIntent.getBroadcast(
            context,
            reminderRequestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    /**
     * Cancels alarms registered by app versions that built PendingIntents without a
     * data URI. Those legacy intents are not filterEquals-matched by the URI-stamped
     * form, so after an update they would stay armed alongside the new alarms and
     * double-fire. Called from BootWorker, which runs on MY_PACKAGE_REPLACED.
     */
    fun cancelLegacyAlarms(taskIds: List<String>, reminderIds: List<String>) {
        val bareIntent = Intent(context, AlarmReceiver::class.java).setPackage(context.packageName)
        taskIds.forEach { taskId ->
            alarmManager.cancel(
                PendingIntent.getBroadcast(
                    context, taskId.hashCode(), bareIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        reminderIds.forEach { reminderId ->
            alarmManager.cancel(
                PendingIntent.getBroadcast(
                    context, reminderRequestCode(reminderId), bareIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
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

    private fun taskAlarmUri(taskId: String): Uri = Uri.parse("choredash://alarm/task/$taskId")

    private fun reminderAlarmUri(reminderId: String): Uri = Uri.parse("choredash://alarm/reminder/$reminderId")

    private fun buildPendingIntent(taskId: String, taskTitle: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            setPackage(context.packageName)
            data = taskAlarmUri(taskId)
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
            data = reminderAlarmUri(reminderId)
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
