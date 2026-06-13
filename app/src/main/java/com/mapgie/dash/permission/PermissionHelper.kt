package com.mapgie.dash.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Settings deep links for the permissions AlarmScheduler and NotificationHelper depend on.
 * Neither "exact alarms" nor "notifications" can be re-requested with a runtime permission
 * dialog once denied, so reminders silently stop working unless the user is sent to the
 * right Settings page.
 */
object PermissionHelper {

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Opens the per-app "Alarms & reminders" page (API 31+). */
    fun exactAlarmSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.fromParts("package", context.packageName, null)
        )

    /** Opens this app's notification settings page. */
    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
}
