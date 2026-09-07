package com.mapgie.dash.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the fix for the silent Alarm style (LESSONS #51): the Alarm delivery
 * mode, and only the Alarm mode, must ring on the phone's alarm audio stream.
 *
 * `AlarmReceiver` launches `AlarmActivity` (which runs `AlarmRinger` under
 * `USAGE_ALARM`) only when [DeliveryMode.ringsOnAlarmStream] is true. If that ever
 * stops holding for [DeliveryMode.ALARM], an Alarm-style reminder falls silent on
 * an unlocked phone whose notification volume is muted, which is the exact bug the
 * fix cured. These assertions are the tripwire.
 */
class DeliveryStyleTest {

    @Test
    fun `the alarm mode rings on the alarm stream`() {
        assertTrue(DeliveryMode.ringsOnAlarmStream(DeliveryMode.ALARM))
        assertEquals(DeliveryStyle.ALARM, DeliveryMode.styleOf(DeliveryMode.ALARM))
    }

    @Test
    fun `notification and silent modes do not ring on the alarm stream`() {
        assertFalse(DeliveryMode.ringsOnAlarmStream(DeliveryMode.NOTIFICATION))
        assertFalse(DeliveryMode.ringsOnAlarmStream(DeliveryMode.SILENT))
        assertEquals(DeliveryStyle.NOTIFICATION, DeliveryMode.styleOf(DeliveryMode.NOTIFICATION))
        assertEquals(DeliveryStyle.SILENT, DeliveryMode.styleOf(DeliveryMode.SILENT))
    }

    @Test
    fun `an unknown mode falls back to a plain notification, never the alarm stream`() {
        assertEquals(DeliveryStyle.NOTIFICATION, DeliveryMode.styleOf(""))
        assertEquals(DeliveryStyle.NOTIFICATION, DeliveryMode.styleOf("LOUD"))
        assertFalse(DeliveryMode.ringsOnAlarmStream("anything-else"))
    }

    @Test
    fun `the stored mode strings are exactly what the alarm path expects`() {
        // AlarmReceiver, BootWorker and Settings pass these literals around; a rename
        // here without updating the ring path is the regression this pins.
        assertEquals("ALARM", DeliveryMode.ALARM)
        assertEquals("NOTIFICATION", DeliveryMode.NOTIFICATION)
        assertEquals("SILENT", DeliveryMode.SILENT)
    }
}
