package com.mapgie.dash.notification

/**
 * The system permissions a reminder needs to reach the user, and which of them
 * the chosen delivery mode actually depends on.
 *
 * Kept Android-free (no Context) so the rule "what counts as missing for this
 * style" is pinned by a plain JVM test. `PermissionHelper.reminderGrants` takes
 * the live snapshot; the Memos list turns the result into its banner.
 *
 * Ordered by how badly the gap hurts: no notifications means nothing is ever
 * shown, no exact alarms means it fires late, no full-screen means the Alarm
 * style cannot ring (see below), no Do Not Disturb access means it is muted
 * only while Do Not Disturb is on.
 */
enum class ReminderPermission(val label: String) {
    NOTIFICATIONS("Notifications"),
    EXACT_ALARMS("Exact alarms"),

    /**
     * Android 14+ lets the user revoke full-screen intents per app. Without it
     * the Alarm style cannot launch its ring screen on a locked phone, so the
     * alert falls back to a plain heads-up whose sound lands on the notification
     * stream (LESSONS #52). On most phones that is silence: the alarm arrives,
     * but it does not ring. The other styles never use a full-screen intent.
     */
    FULL_SCREEN("Full-screen alarms"),

    /** Only the Alarm style promises to sound through Do Not Disturb. */
    DND_ACCESS("Do Not Disturb access"),
}

/** A snapshot of the four grants. Platforms below a permission's API level report it as granted. */
data class ReminderPermissionGrants(
    val notifications: Boolean = true,
    val exactAlarms: Boolean = true,
    val fullScreen: Boolean = true,
    val dndAccess: Boolean = true,
) {
    /**
     * The permissions the given delivery mode needs and does not have, most
     * damaging first. Notifications and exact alarms matter to every style; the
     * full-screen and Do Not Disturb grants only to the Alarm style, so a user on
     * Notification or Silent is never nagged about them.
     */
    fun missingFor(deliveryMode: String): List<ReminderPermission> {
        val alarm = DeliveryMode.ringsOnAlarmStream(deliveryMode)
        return buildList {
            if (!notifications) add(ReminderPermission.NOTIFICATIONS)
            if (!exactAlarms) add(ReminderPermission.EXACT_ALARMS)
            if (alarm && !fullScreen) add(ReminderPermission.FULL_SCREEN)
            if (alarm && !dndAccess) add(ReminderPermission.DND_ACCESS)
        }
    }

    /**
     * One line for the list-screen banner, or null when nothing the current style
     * needs is missing. [plural] is the user's word for the feature ("memos",
     * "reminders", "alarms"), lower case. A single gap is named with its effect;
     * several are counted, since the Settings page they open lists each one.
     */
    fun warningFor(deliveryMode: String, plural: String): String? {
        val missing = missingFor(deliveryMode)
        return when (missing.size) {
            0 -> null
            1 -> when (missing.single()) {
                ReminderPermission.NOTIFICATIONS ->
                    "Notifications are off, so $plural cannot alert you. Tap to allow."
                ReminderPermission.EXACT_ALARMS ->
                    "Exact alarms are off, so $plural may ring late. Tap to allow."
                ReminderPermission.FULL_SCREEN ->
                    "Full-screen alarms are off, so $plural may ring silently. Tap to allow."
                ReminderPermission.DND_ACCESS ->
                    "Do Not Disturb access is off, so $plural stay quiet during Do Not Disturb. Tap to allow."
            }
            else -> "${missing.size} permissions are off, so $plural may not ring. Tap to review."
        }
    }

    /**
     * The permission nudge a list screen shows, or null. Stays quiet for a user
     * who has no reminders yet unless this is first run, so an established empty
     * list is never nagged. [hasReminders] is whether any reminder exists;
     * [firstRun] whether the first-run welcome has yet to be dismissed. The text
     * is [warningFor]; [ReminderNudge.fullScreenOnly] lets the banner jump
     * straight to the full-screen system toggle when that is the only gap.
     */
    fun nudgeFor(
        deliveryMode: String,
        plural: String,
        hasReminders: Boolean,
        firstRun: Boolean,
    ): ReminderNudge? {
        if (!hasReminders && !firstRun) return null
        val text = warningFor(deliveryMode, plural) ?: return null
        val fullScreenOnly = missingFor(deliveryMode) == listOf(ReminderPermission.FULL_SCREEN)
        return ReminderNudge(text = text, fullScreenOnly = fullScreenOnly)
    }
}

/**
 * What a list-screen permission banner should say and where its tap should go.
 * [fullScreenOnly] means the sole missing grant is the full-screen one, so the
 * tap can open that system toggle directly instead of the settings page.
 */
data class ReminderNudge(val text: String, val fullScreenOnly: Boolean)
