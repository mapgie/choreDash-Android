package com.mapgie.dash.alarm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.mapgie.dash.notification.NotificationHelper
import com.mapgie.dash.ui.screens.reminder.REMINDER_VIEW_ARG_ID
import com.mapgie.dash.ui.screens.reminder.REMINDER_VIEW_ARG_KIND
import com.mapgie.dash.ui.screens.reminder.REMINDER_VIEW_ARG_SOUND
import com.mapgie.dash.ui.screens.reminder.ReminderViewKind
import com.mapgie.dash.ui.screens.reminder.ReminderViewScreen
import com.mapgie.dash.ui.theme.DashTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The screen the Alarm delivery mode throws up when a reminder fires: launched
 * by the notification's full-screen intent, so on a locked or sleeping phone it
 * turns the screen on and shows over the lock screen like a clock alarm. On an
 * unlocked phone that is in use, Android shows the heads-up notification instead
 * and this activity never starts.
 *
 * The ring is [AlarmRingService]'s, not this screen's, so it sounds either way
 * (LESSONS #66). This screen only rings itself (see [AlarmRinger]) when nothing
 * else is: an alert delivered late by BootWorker, or one whose service start was
 * refused. Leaving the screen ends the ring, whoever owns it.
 *
 * The body is the same nudge view a notification tap opens: it reads the kind
 * and id from this activity's intent extras (Hilt hands them to the ViewModel's
 * SavedStateHandle) and Done / Snooze finish the activity, which stops the ring.
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    private lateinit var ringer: AlarmRinger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showOverLockScreen()
        ringer = AlarmRinger(applicationContext)

        // A ring nobody answers stops on its own; the notification stays behind
        // for Snooze / Done, so nothing is lost.
        lifecycleScope.launch {
            delay(RING_TIMEOUT)
            finish()
        }

        setContent {
            DashTheme(darkTheme = true) {
                ReminderViewScreen(
                    onBack = { finish() },
                    viewModel = hiltViewModel(),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // The service is already ringing for an on-time alarm; ring here only when it isn't.
        // The memo's own tone when it has one; the default alarm tone otherwise.
        if (!AlarmRingService.isRinging()) {
            ringer.start(intent.getStringExtra(REMINDER_VIEW_ARG_SOUND)?.let(Uri::parse))
        }
    }

    // singleInstance activity: a second launch while the screen is up (a newer alarm's
    // full-screen intent) arrives here instead of creating a second instance. The ring
    // is already going (the service's, or AlarmRinger.start no-ops while a player is
    // live), so this just keeps the extras current; nothing to restart.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // Leaving the screen for any reason (Done, Snooze, back, the power button,
    // opening the app from the notification) is the end of the ring. A rotation
    // also passes through onStop; that one keeps the alarm and re-rings on start.
    override fun onStop() {
        ringer.stop()
        super.onStop()
        if (!isChangingConfigurations) {
            // The notification stays behind for Snooze / Done; Done and Snooze on this
            // screen clear it themselves.
            alertNotifyId()?.let { AlarmRingService.silence(it, keepNotification = true) }
            finish()
        }
    }

    private fun alertNotifyId(): Int? {
        val kind = ReminderViewKind.fromRouteArg(intent.getStringExtra(REMINDER_VIEW_ARG_KIND)) ?: return null
        val id = intent.getStringExtra(REMINDER_VIEW_ARG_ID) ?: return null
        return NotificationHelper.notifyId(kind, id)
    }

    override fun onDestroy() {
        ringer.stop()
        super.onDestroy()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private companion object {
        val RING_TIMEOUT = AlarmRingService.RING_TIMEOUT
    }
}
