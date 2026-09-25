package com.mapgie.dash.alarm

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlin.time.Duration.Companion.minutes

/**
 * Owns the Alarm style's ring: a foreground service that posts the alarm
 * notification as its own and loops [AlarmRinger] on the alarm stream until the
 * alarm is answered, dismissed, or times out.
 *
 * Why a service and not [AlarmActivity]: the activity only runs when Android
 * lets it start. It launches the full-screen intent only on a locked or sleeping
 * phone, and it blocks a background activity start from an alarm receiver once
 * the app has been out of sight for more than a few seconds (LESSONS #65). A
 * foreground service started from an exact alarm is a documented exemption, so
 * the ring no longer depends on whether a screen was allowed to open. The ring
 * screen still shows over the lock screen through the notification's
 * full-screen intent; it is the answer UI, not the thing making the sound.
 */
class AlarmRingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var ringer: AlarmRinger? = null
    private var ringingNotifyId: Int? = null
    private val timeout = Runnable { halt(keepNotification = true) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_NOTIFICATION, Notification::class.java) }
        if (intent == null || notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notifyId = intent.getIntExtra(EXTRA_NOTIFY_ID, 0)
        val soundUri = intent.getStringExtra(EXTRA_SOUND)?.let(Uri::parse)

        // A newer alarm replaces a ringing one: the older notification stays behind,
        // unanswered, for Snooze / Done, and the ring restarts with the newer tone.
        if (ringingNotifyId != null && ringingNotifyId != notifyId) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        }
        val started = runCatching {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            } else 0
            ServiceCompat.startForeground(this, notifyId, notification, type)
        }.isSuccess
        if (!started) {
            // Refused after all: the alert still goes out as a plain notification. Its
            // full-screen intent opens the ring screen, which rings itself, on a locked phone.
            postPlain(notifyId, notification)
            stopSelf()
            return START_NOT_STICKY
        }

        ringer?.stop()
        ringer = AlarmRinger(applicationContext).also { it.start(soundUri) }
        ringingNotifyId = notifyId
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, RING_TIMEOUT.inWholeMilliseconds)
        // Not sticky: a ring the system killed must not come back minutes later.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        ringer?.stop()
        ringer = null
        ringingNotifyId = null
        if (instance === this) instance = null
        super.onDestroy()
    }

    // Ends the ring. [keepNotification] leaves the alert in the shade for Snooze /
    // Done (a timeout, the ring screen closed, the app opened from it); false takes
    // it away with the ring (the alarm was answered, or the user swiped it).
    private fun halt(keepNotification: Boolean) {
        if (ringingNotifyId == null) return
        handler.removeCallbacks(timeout)
        ringer?.stop()
        ringer = null
        ringingNotifyId = null
        ServiceCompat.stopForeground(
            this,
            if (keepNotification) ServiceCompat.STOP_FOREGROUND_DETACH else ServiceCompat.STOP_FOREGROUND_REMOVE,
        )
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun postPlain(notifyId: Int, notification: Notification) {
        runCatching { NotificationManagerCompat.from(this).notify(notifyId, notification) }
    }

    companion object {
        /** How long a ring nobody answers keeps going; the notification stays behind. */
        val RING_TIMEOUT = 2.minutes

        private const val EXTRA_NOTIFY_ID = "ring_notify_id"
        private const val EXTRA_NOTIFICATION = "ring_notification"
        private const val EXTRA_SOUND = "ring_sound"

        // Everything that silences a ring (the ring screen, the notification's actions,
        // the in-app nudge) runs in this process, so a reference is all the plumbing
        // needed. Main thread only.
        @Volatile private var instance: AlarmRingService? = null

        /** True while a ring is sounding, so the ring screen does not start a second one. */
        fun isRinging(): Boolean = instance?.ringingNotifyId != null

        /**
         * Starts the ring for the alert posted under [notifyId]. Only call it from a
         * real-time alarm delivery (AlarmReceiver): that is what exempts the start
         * from the background limits. Returns false when Android refuses it, so the
         * caller can post the notification the old way.
         */
        fun start(context: Context, notifyId: Int, notification: Notification, soundUri: String?): Boolean =
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmRingService::class.java)
                        .putExtra(EXTRA_NOTIFY_ID, notifyId)
                        .putExtra(EXTRA_NOTIFICATION, notification)
                        .apply { soundUri?.let { putExtra(EXTRA_SOUND, it) } },
                )
            }.isSuccess

        /**
         * Silences the ring for the alert posted under [notifyId], if that is the one
         * sounding. A ring for a different alert is left alone.
         */
        fun silence(notifyId: Int, keepNotification: Boolean) {
            Handler(Looper.getMainLooper()).post {
                val service = instance ?: return@post
                if (service.ringingNotifyId == notifyId) service.halt(keepNotification)
            }
        }
    }
}
