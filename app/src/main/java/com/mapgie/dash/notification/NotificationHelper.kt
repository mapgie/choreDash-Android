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
import com.mapgie.dash.alarm.AlarmActivity
import com.mapgie.dash.ui.screens.reminder.REMINDER_VIEW_ARG_ID
import com.mapgie.dash.ui.screens.reminder.REMINDER_VIEW_ARG_KIND
import com.mapgie.dash.ui.screens.reminder.REMINDER_VIEW_ARG_SOUND
import com.mapgie.dash.ui.screens.reminder.REMINDER_VIEW_ARG_SUBJECT
import com.mapgie.dash.ui.screens.reminder.ReminderViewKind

// What kind of alert is being delivered. Every kind gets the same Alarm/Notification/Silent
// channel trio (see ChannelStyle) so DND-bypass behaviour is identical across chores, tasks,
// and standalone reminders — only the channel copy and base importance differ per kind.
enum class ReminderKind { TASK_REMINDER, CHORE_ALERT }

private enum class ChannelStyle { ALARM, NOTIFICATION, SILENT }

private fun styleForDeliveryMode(deliveryMode: String): ChannelStyle = when (deliveryMode) {
    "ALARM" -> ChannelStyle.ALARM
    "SILENT" -> ChannelStyle.SILENT
    else -> ChannelStyle.NOTIFICATION
}

private data class ChannelDef(
    val kind: ReminderKind,
    val style: ChannelStyle,
    val id: String,
    val nameRes: Int,
    val descRes: Int,
    val importance: Int,
)

object NotificationHelper {
    // Single source of truth for every alert channel. Channel settings (importance, sound,
    // vibration) are immutable once created, so a new "_v1"-style id is required whenever
    // one of these changes for an existing install; bump the id and add the old one to
    // legacyChannelIds below so it gets deleted on next createChannels() run.
    private val channelDefs = listOf(
        ChannelDef(
            ReminderKind.TASK_REMINDER, ChannelStyle.ALARM, "dash_task_reminders_alarm_v1",
            R.string.channel_task_reminders_alarm_name, R.string.channel_task_reminders_alarm_desc,
            NotificationManager.IMPORTANCE_HIGH
        ),
        ChannelDef(
            ReminderKind.TASK_REMINDER, ChannelStyle.NOTIFICATION, "dash_task_reminders_notif_v1",
            R.string.channel_task_reminders_notif_name, R.string.channel_task_reminders_notif_desc,
            NotificationManager.IMPORTANCE_HIGH
        ),
        ChannelDef(
            ReminderKind.TASK_REMINDER, ChannelStyle.SILENT, "dash_task_reminders_silent_v1",
            R.string.channel_task_reminders_silent_name, R.string.channel_task_reminders_silent_desc,
            NotificationManager.IMPORTANCE_LOW
        ),
        ChannelDef(
            ReminderKind.CHORE_ALERT, ChannelStyle.ALARM, "dash_chore_alerts_alarm_v1",
            R.string.channel_chore_alerts_alarm_name, R.string.channel_chore_alerts_alarm_desc,
            NotificationManager.IMPORTANCE_HIGH
        ),
        ChannelDef(
            ReminderKind.CHORE_ALERT, ChannelStyle.NOTIFICATION, "dash_chore_alerts_notif_v1",
            R.string.channel_chore_alerts_notif_name, R.string.channel_chore_alerts_notif_desc,
            NotificationManager.IMPORTANCE_DEFAULT
        ),
        ChannelDef(
            ReminderKind.CHORE_ALERT, ChannelStyle.SILENT, "dash_chore_alerts_silent_v1",
            R.string.channel_chore_alerts_silent_name, R.string.channel_chore_alerts_silent_desc,
            NotificationManager.IMPORTANCE_LOW
        ),
    )

    // Legacy channel ids — deleted on first run after upgrade
    private val legacyChannelIds = listOf(
        "dash_task_reminders", "dash_task_reminders_v2", "dash_task_reminders_v3", "dash_chore_alerts"
    )

    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TITLE = "task_title"
    const val EXTRA_REMINDER_ID = "reminder_id"
    const val EXTRA_REMINDER_SUBJECT = "reminder_subject"

    // Single place that maps (kind, delivery-mode setting) to a channel. Resolved at delivery
    // time (AlarmReceiver, BootWorker, DailyStaleChoreWorker) so the current setting always
    // wins; the mode is never baked into alarm or action intents. Chores, tasks, and standalone
    // reminders all go through this one function, so there is exactly one place that can drift
    // out of sync if a delivery mode is ever added or renamed.
    // DND bypass is only honoured if setBypassDnd was applied when the channel was first
    // created, and channel settings are immutable afterwards (LESSONS #17). So while bypass
    // is active the alarm channels use a distinct "_dnd" id: granting Do Not Disturb access
    // creates that variant fresh (with bypass) rather than trying to mutate the existing
    // no-bypass channel, which the system silently ignores. Cached from the last
    // createChannels() run so channelId() resolves to the same id the channel was created under.
    @Volatile private var dndBypassActive: Boolean = false

    private const val ALARM_DND_SUFFIX = "_dnd"

    private fun alarmIdFor(baseId: String, dndActive: Boolean): String =
        if (dndActive) baseId + ALARM_DND_SUFFIX else baseId

    /**
     * What Android currently holds for the channel a delivery mode posts memos and
     * task reminders on. Channel settings are the system's once created (LESSONS
     * #17), so this is the only honest answer to "why was that silent".
     */
    data class ChannelStatus(
        val id: String,
        val exists: Boolean,
        val importance: Int,
        val hasSound: Boolean,
        val vibrates: Boolean,
        val bypassesDnd: Boolean,
    )

    fun channelStatus(context: Context, deliveryMode: String): ChannelStatus {
        val id = channelId(ReminderKind.TASK_REMINDER, deliveryMode)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(id)
            ?: return ChannelStatus(id, exists = false, importance = 0, hasSound = false, vibrates = false, bypassesDnd = false)
        return ChannelStatus(
            id = id,
            exists = true,
            importance = channel.importance,
            hasSound = channel.sound != null,
            vibrates = channel.shouldVibrate(),
            bypassesDnd = channel.canBypassDnd(),
        )
    }

    fun channelId(kind: ReminderKind, deliveryMode: String): String {
        val style = styleForDeliveryMode(deliveryMode)
        val base = channelDefs.first { it.kind == kind && it.style == style }.id
        return if (style == ChannelStyle.ALARM) alarmIdFor(base, dndBypassActive) else base
    }

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dndActive = nm.isNotificationPolicyAccessGranted
        dndBypassActive = dndActive

        legacyChannelIds.forEach { nm.deleteNotificationChannel(it) }

        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val alarmAudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        val notifSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notifAudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        channelDefs.forEach { def ->
            val effectiveId = if (def.style == ChannelStyle.ALARM) alarmIdFor(def.id, dndActive) else def.id
            if (def.style == ChannelStyle.ALARM) {
                // Drop the opposite-state alarm channel so a stale bypass / no-bypass copy
                // doesn't linger after the user toggles Do Not Disturb access.
                nm.deleteNotificationChannel(alarmIdFor(def.id, !dndActive))
            }
            nm.createNotificationChannel(
                NotificationChannel(effectiveId, context.getString(def.nameRes), def.importance).apply {
                    description = context.getString(def.descRes)
                    when (def.style) {
                        ChannelStyle.ALARM -> {
                            setSound(alarmSoundUri, alarmAudioAttributes)
                            enableVibration(true)
                            // Bypass only sticks if applied at creation time (LESSONS #17);
                            // the "_dnd" channel id above is what makes a fresh, bypassing
                            // channel appear when the user grants Do Not Disturb access.
                            if (dndActive) {
                                setBypassDnd(true)
                            }
                        }
                        ChannelStyle.NOTIFICATION -> {
                            setSound(notifSoundUri, notifAudioAttributes)
                            enableVibration(true)
                        }
                        ChannelStyle.SILENT -> {
                            setSound(null, null)
                            enableVibration(false)
                        }
                    }
                }
            )
        }
    }

    /** True for the delivery mode that rings: full-screen alarm, alarm category. */
    fun isAlarmStyle(deliveryMode: String): Boolean =
        styleForDeliveryMode(deliveryMode) == ChannelStyle.ALARM

    // The Alarm style's full-screen intent: AlarmActivity turns the screen on over
    // the lock screen and rings (AlarmRinger) until answered. Android only launches
    // it when the device is locked or asleep; awake and unlocked it shows the
    // heads-up notification instead, and the channel's alarm sound carries that.
    // CLEAR_TASK replaces a still-ringing alarm with the newer one rather than
    // stacking two ringing screens.
    private fun fullScreenIntent(
        context: Context,
        kind: ReminderViewKind,
        id: String,
        subject: String,
        requestCode: Int,
        soundUri: String? = null,
    ): PendingIntent = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, AlarmActivity::class.java).apply {
            setPackage(context.packageName)
            putExtra(REMINDER_VIEW_ARG_KIND, kind.routeArg)
            putExtra(REMINDER_VIEW_ARG_ID, id)
            putExtra(REMINDER_VIEW_ARG_SUBJECT, subject)
            soundUri?.let { putExtra(REMINDER_VIEW_ARG_SOUND, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * Applies the per-style presentation: the Alarm style is an alarm to the
     * system (full-screen intent, CATEGORY_ALARM); the others are plain
     * reminders on their channel. [fullScreen] is only built for the Alarm style.
     */
    private fun NotificationCompat.Builder.styledFor(
        deliveryMode: String,
        fullScreen: () -> PendingIntent,
    ): NotificationCompat.Builder = if (isAlarmStyle(deliveryMode)) {
        setCategory(NotificationCompat.CATEGORY_ALARM)
        setFullScreenIntent(fullScreen(), true)
    } else {
        setCategory(NotificationCompat.CATEGORY_REMINDER)
    }

    @SuppressLint("MissingPermission")
    fun showTaskReminder(context: Context, taskId: String, taskTitle: String, deliveryMode: String = "NOTIFICATION") {
        val channelId = channelId(ReminderKind.TASK_REMINDER, deliveryMode)
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
            .styledFor(deliveryMode) {
                fullScreenIntent(context, ReminderViewKind.TASK, taskId, taskTitle, "fullscreen_$taskId".hashCode())
            }
            .addAction(0, "Snooze 15 min", snoozePI)
            .addAction(0, "Done", donePI)
            .build()

        NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
    }

    @SuppressLint("MissingPermission")
    /** [title] is the user's word for the feature ("Reminder", "Alarm", "Memo"); callers read it from settings. */
    fun showReminderAlert(
        context: Context,
        reminderId: String,
        subject: String,
        deliveryMode: String = "NOTIFICATION",
        taskId: String? = null,
        title: String = "Reminder",
        soundUri: String? = null,
    ) {
        val channelId = channelId(ReminderKind.TASK_REMINDER, deliveryMode)
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
            .setContentTitle(title)
            .setContentText(subject)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .styledFor(deliveryMode) {
                fullScreenIntent(context, ReminderViewKind.REMINDER, reminderId, subject, "fullscreen_reminder_$reminderId".hashCode(), soundUri)
            }
            .addAction(0, "Snooze 15 min", snoozePI)
            .addAction(0, "Done", donePI)
            .build()

        NotificationManagerCompat.from(context).notify(notifyId, notification)
    }

    @SuppressLint("MissingPermission")
    fun showStaleChoresSummary(context: Context, choreLabels: List<String>, channelId: String = channelId(ReminderKind.CHORE_ALERT, "NOTIFICATION")) {
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
        val notification = NotificationCompat.Builder(context, channelId)
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
