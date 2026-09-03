package com.mapgie.dash.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.mapgie.dash.ui.screens.reminder.ReminderViewScreen
import com.mapgie.dash.ui.theme.DashTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

/**
 * The screen the Alarm delivery mode throws up when a reminder fires: launched
 * by the notification's full-screen intent, so on a locked or sleeping phone it
 * turns the screen on and shows over the lock screen like a clock alarm, and
 * rings and vibrates (see [AlarmRinger]) until the user acts or it times out.
 * On an unlocked phone that is in use, Android shows the heads-up notification
 * instead and this activity never starts; the channel's alarm sound covers that.
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
        ringer.start()
    }

    // Leaving the screen for any reason (Done, Snooze, back, the power button,
    // opening the app from the notification) is the end of the ring. A rotation
    // also passes through onStop; that one keeps the alarm and re-rings on start.
    override fun onStop() {
        ringer.stop()
        super.onStop()
        if (!isChangingConfigurations) finish()
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
        val RING_TIMEOUT = 2.minutes
    }
}
