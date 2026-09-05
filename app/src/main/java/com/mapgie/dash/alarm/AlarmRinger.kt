package com.mapgie.dash.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Loops the device's alarm ringtone on the alarm stream and pulses the vibrator
 * while an [AlarmActivity] is on screen. Both run under alarm usage so they
 * sound at alarm volume and get through Do Not Disturb the way a clock alarm
 * does, independent of the notification channel that posted the alert.
 */
class AlarmRinger(private val context: Context) {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Rings with [preferred] when it can be played, else with the device defaults. */
    fun start(preferred: Uri? = null) {
        if (player != null) return
        startSound(preferred)
        startVibration()
    }

    fun stop() {
        player?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            p.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    // The memo's own tone first (a user-added file it can no longer read simply
    // falls through), then the alarm tone; a device with no alarm tone set falls
    // back to the ringtone, then the notification sound, rather than ringing silently.
    private fun startSound(preferred: Uri?) {
        val candidates = listOfNotNull(preferred) + listOf(
            RingtoneManager.TYPE_ALARM,
            RingtoneManager.TYPE_RINGTONE,
            RingtoneManager.TYPE_NOTIFICATION,
        ).mapNotNull { RingtoneManager.getDefaultUri(it) }
        for (uri in candidates) {
            val started = tryPlay(uri)
            if (started != null) {
                player = started
                return
            }
        }
    }

    private fun tryPlay(uri: Uri): MediaPlayer? {
        val mp = MediaPlayer()
        return runCatching {
            mp.setAudioAttributes(audioAttributes)
            mp.setDataSource(context, uri)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            mp
        }.getOrElse {
            mp.release()
            null
        }
    }

    private fun startVibration() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!v.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(effect, audioAttributes)
            }
        }
        vibrator = v
    }
}
