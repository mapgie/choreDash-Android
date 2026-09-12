package com.mapgie.dash.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which missing permissions the Memos list warns about, per delivery style. The
 * Alarm style is the only one that rings through a full-screen intent, so it is
 * the only one told when that grant is gone; every style needs notifications and
 * exact alarms. Pure logic, no Android involved.
 */
class ReminderPermissionsTest {

    private val all = ReminderPermissionGrants()

    @Test
    fun `nothing missing shows no warning in any style`() {
        for (mode in listOf(DeliveryMode.ALARM, DeliveryMode.NOTIFICATION, DeliveryMode.SILENT)) {
            assertTrue(all.missingFor(mode).isEmpty())
            assertNull(all.warningFor(mode, "memos"))
        }
    }

    @Test
    fun `alarm style without full-screen warns that memos may ring silently`() {
        val grants = all.copy(fullScreen = false)
        assertEquals(listOf(ReminderPermission.FULL_SCREEN), grants.missingFor(DeliveryMode.ALARM))
        assertEquals(
            "Full-screen alarms are off, so memos may ring silently. Tap to allow.",
            grants.warningFor(DeliveryMode.ALARM, "memos"),
        )
    }

    @Test
    fun `notification and silent styles ignore full-screen and do not disturb access`() {
        val grants = all.copy(fullScreen = false, dndAccess = false)
        assertTrue(grants.missingFor(DeliveryMode.NOTIFICATION).isEmpty())
        assertTrue(grants.missingFor(DeliveryMode.SILENT).isEmpty())
        assertNull(grants.warningFor(DeliveryMode.NOTIFICATION, "memos"))
    }

    @Test
    fun `every style needs notifications and exact alarms`() {
        val grants = all.copy(notifications = false, exactAlarms = false)
        val expected = listOf(ReminderPermission.NOTIFICATIONS, ReminderPermission.EXACT_ALARMS)
        assertEquals(expected, grants.missingFor(DeliveryMode.SILENT))
        assertEquals(expected, grants.missingFor(DeliveryMode.NOTIFICATION))
        assertEquals(expected, grants.missingFor(DeliveryMode.ALARM))
    }

    @Test
    fun `missing permissions are listed most damaging first`() {
        val none = ReminderPermissionGrants(
            notifications = false, exactAlarms = false, fullScreen = false, dndAccess = false,
        )
        assertEquals(
            listOf(
                ReminderPermission.NOTIFICATIONS,
                ReminderPermission.EXACT_ALARMS,
                ReminderPermission.FULL_SCREEN,
                ReminderPermission.DND_ACCESS,
            ),
            none.missingFor(DeliveryMode.ALARM),
        )
    }

    @Test
    fun `one missing permission is named, several are counted`() {
        assertEquals(
            "Notifications are off, so reminders cannot alert you. Tap to allow.",
            all.copy(notifications = false).warningFor(DeliveryMode.NOTIFICATION, "reminders"),
        )
        assertEquals(
            "Exact alarms are off, so alarms may ring late. Tap to allow.",
            all.copy(exactAlarms = false).warningFor(DeliveryMode.SILENT, "alarms"),
        )
        assertEquals(
            "Do Not Disturb access is off, so memos stay quiet during Do Not Disturb. Tap to allow.",
            all.copy(dndAccess = false).warningFor(DeliveryMode.ALARM, "memos"),
        )
        assertEquals(
            "2 permissions are off, so memos may not ring. Tap to review.",
            all.copy(fullScreen = false, dndAccess = false).warningFor(DeliveryMode.ALARM, "memos"),
        )
    }

    @Test
    fun `an unknown stored mode is treated as a plain notification`() {
        assertTrue(all.copy(fullScreen = false).missingFor("LOUD").isEmpty())
        assertEquals(listOf(ReminderPermission.NOTIFICATIONS), all.copy(notifications = false).missingFor(""))
    }

    @Test
    fun `no nudge for an empty list that is past first run`() {
        val grants = all.copy(notifications = false, fullScreen = false)
        assertNull(grants.nudgeFor(DeliveryMode.ALARM, "memos", hasReminders = false, firstRun = false))
    }

    @Test
    fun `first run nudges even with no reminders yet`() {
        val grants = all.copy(notifications = false)
        val nudge = grants.nudgeFor(DeliveryMode.NOTIFICATION, "memos", hasReminders = false, firstRun = true)
        assertEquals("Notifications are off, so memos cannot alert you. Tap to allow.", nudge?.text)
        assertFalse(nudge!!.fullScreenOnly)
    }

    @Test
    fun `full-screen only gap nudges with the direct-toggle flag set`() {
        val grants = all.copy(fullScreen = false)
        val nudge = grants.nudgeFor(DeliveryMode.ALARM, "memos", hasReminders = true, firstRun = false)
        assertEquals("Full-screen alarms are off, so memos may ring silently. Tap to allow.", nudge?.text)
        assertTrue(nudge!!.fullScreenOnly)
    }

    @Test
    fun `full-screen plus another gap does not set the direct-toggle flag`() {
        val grants = all.copy(notifications = false, fullScreen = false)
        val nudge = grants.nudgeFor(DeliveryMode.ALARM, "memos", hasReminders = true, firstRun = false)
        assertFalse(nudge!!.fullScreenOnly)
    }

    @Test
    fun `nothing missing means no nudge even with reminders`() {
        assertNull(all.nudgeFor(DeliveryMode.ALARM, "memos", hasReminders = true, firstRun = true))
    }
}
