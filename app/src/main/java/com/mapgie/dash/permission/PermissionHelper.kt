package com.mapgie.dash.permission

import android.app.NotificationManager
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
 * right Settings page. Do Not Disturb access likewise has no runtime prompt.
 */
object PermissionHelper {

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Whether this app can let its alarms sound while Do Not Disturb is on. */
    fun isDndAccessGranted(context: Context): Boolean =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .isNotificationPolicyAccessGranted

    /**
     * Whether the Alarm style may turn the screen on with a full-screen alarm.
     * Android 14+ lets the user revoke this per app; earlier versions always allow it.
     */
    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .canUseFullScreenIntent()
    }

    /** Opens this app's "Full-screen notifications" toggle (API 34+). */
    fun fullScreenIntentSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.fromParts("package", context.packageName, null)
        )

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

    /** Opens the system "Do Not Disturb access" page where this app can be allowed. */
    fun dndAccessSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
}
