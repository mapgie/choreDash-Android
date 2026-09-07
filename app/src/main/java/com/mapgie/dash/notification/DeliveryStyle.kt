package com.mapgie.dash.notification

/**
 * How an alert reaches the user, chosen in Settings › Reminders & alerts and
 * stored as a plain string on the delivery-mode setting.
 *
 * Kept Android-free (no Context, Notification, Intent) so the one decision that
 * fixes the silent-alarm bug can be pinned by a plain JVM unit test without
 * Robolectric: **which stored mode must ring on the phone's alarm audio stream.**
 * That is the crux of LESSONS #51 — a posted notification's sound can land on the
 * (often muted) notification stream whatever `USAGE_ALARM` the channel declares,
 * so for the Alarm mode `AlarmReceiver` launches `AlarmActivity` itself and
 * `AlarmRinger` plays under `USAGE_ALARM`. If the mapping below ever stops
 * treating [DeliveryMode.ALARM] as an alarm, an unlocked phone falls silent again.
 */
enum class DeliveryStyle { ALARM, NOTIFICATION, SILENT }

object DeliveryMode {
    /**
     * The exact strings a delivery mode is stored and passed around as (Settings,
     * `AlarmReceiver`, `BootWorker`, `NotificationHelper`). The tests pin these so a
     * rename can't quietly break the ring path.
     */
    const val ALARM = "ALARM"
    const val NOTIFICATION = "NOTIFICATION"
    const val SILENT = "SILENT"

    /** The style a stored mode maps to; anything unrecognised is a plain notification. */
    fun styleOf(deliveryMode: String): DeliveryStyle = when (deliveryMode) {
        ALARM -> DeliveryStyle.ALARM
        SILENT -> DeliveryStyle.SILENT
        else -> DeliveryStyle.NOTIFICATION
    }

    /**
     * True for the one mode that must sound on the **alarm** audio stream rather
     * than the notification stream. `AlarmReceiver` gates its direct
     * `startAlarmRingScreen` launch on this, so keep it in lockstep with the real
     * ring behaviour: this returning false for [ALARM] is exactly the regression
     * that made the Alarm style silent on an unlocked phone (LESSONS #51).
     */
    fun ringsOnAlarmStream(deliveryMode: String): Boolean = styleOf(deliveryMode) == DeliveryStyle.ALARM
}
