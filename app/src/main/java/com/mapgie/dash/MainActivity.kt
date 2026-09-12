package com.mapgie.dash

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.Severity
import com.mapgie.dash.data.model.TagAlarmText
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.preferences.TagStickerStore
import com.mapgie.dash.data.preferences.ThemeMode
import com.mapgie.dash.data.repository.ChoreRepository
import com.mapgie.dash.nfc.NfcHandler
import com.mapgie.dash.nfc.NfcWriteRequest
import com.mapgie.dash.nfc.NfcWriteResult
import com.mapgie.dash.notification.NotificationHelper
import com.mapgie.dash.tagalarm.TagAlarmService
import com.mapgie.dash.ui.navigation.DashNavGraph
import com.mapgie.dash.ui.screens.reminder.ReminderViewKind
import com.mapgie.dash.ui.theme.AppTheme
import com.mapgie.dash.ui.theme.CustomHSL
import com.mapgie.dash.ui.theme.DashTheme
import com.mapgie.dash.widget.WIDGET_DESTINATION_EXTRA
import com.mapgie.dash.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var choreRepository: ChoreRepository
    @Inject lateinit var tagAlarmService: TagAlarmService
    @Inject lateinit var tagStickerStore: TagStickerStore

    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null
    private var isActivityResumed = false

    // Compose state: survives recompositions, drives the NFC sheet trigger
    private var pendingNfcTagId by mutableStateOf<String?>(null)

    // Compose state: set when launched from a home screen widget, drives navigation
    private var pendingWidgetDestination by mutableStateOf<String?>(null)

    // Compose state: set when launched from a reminder notification, as
    // (route kind, record id); drives navigation to the full-screen reminder view.
    private var pendingReminderView by mutableStateOf<Pair<String, String>?>(null)

    // When set, the next scanned tag is written with this chore or memo id instead of being read
    private var nfcWriteRequest by mutableStateOf<NfcWriteRequest?>(null)
    private var nfcWriteResult by mutableStateOf<NfcWriteResult?>(null)

    // While true, the next scanned tag's id is captured for the memo sheet's "link
    // tag" flow instead of arming a tag-alarm or logging a chore.
    private var nfcCaptureRequested by mutableStateOf(false)
    private var nfcCapturedTagId by mutableStateOf<String?>(null)

    // Set after a tap armed a tag-alarm while other tag-alarms were already set for
    // the same morning; drives the "Turn them off?" dialog.
    private var tagAlarmConflicts by mutableStateOf<List<ReminderDto>?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        handleNfcIntent(intent, fromForeground = false)
        handleReminderIntent(intent)

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = null)
            val darkTheme = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            val appTheme = settings?.let {
                runCatching { AppTheme.valueOf(it.appTheme) }.getOrDefault(AppTheme.CREAM)
            } ?: AppTheme.CREAM
            val customHSL = settings?.let {
                CustomHSL(
                    primaryH   = it.customPrimaryHue,
                    primaryS   = it.customPrimarySaturation,
                    primaryL   = it.customPrimaryLightness,
                    secondaryH = it.customSecondaryHue,
                    secondaryS = it.customSecondarySaturation,
                    secondaryL = it.customSecondaryLightness,
                    tertiaryH  = it.customTertiaryHue,
                    tertiaryS  = it.customTertiarySaturation,
                    tertiaryL  = it.customTertiaryLightness,
                    lightBackgroundArgb = it.customLightBackgroundArgb,
                    darkBackgroundArgb  = it.customDarkBackgroundArgb,
                    lightCardFaceArgb   = it.customLightCardFaceArgb,
                    darkCardFaceArgb    = it.customDarkCardFaceArgb,
                )
            }
            DashTheme(
                appTheme  = appTheme,
                darkTheme = darkTheme,
                wcag      = settings?.wcagMode ?: false,
                customHSL = customHSL,
                severitySwatches = settings?.severitySwatches ?: Severity.defaults,
            ) {
                // Hold blank screen until first DataStore emission (<10 ms)
                // to avoid flashing wrong theme or credentials state.
                if (settings == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                    return@DashTheme
                }
                DashNavGraph(
                    pendingNfcTagId = pendingNfcTagId,
                    onNfcConsumed = { pendingNfcTagId = null },
                    pendingWidgetDestination = pendingWidgetDestination,
                    onWidgetDestinationConsumed = { pendingWidgetDestination = null },
                    pendingReminderView = pendingReminderView,
                    onReminderViewConsumed = {
                        pendingReminderView = null
                        // Strip the extras so a configuration change does not
                        // re-deliver the launch intent and reopen the view.
                        intent?.removeExtra(NotificationHelper.EXTRA_REMINDER_ID)
                        intent?.removeExtra(NotificationHelper.EXTRA_TASK_ID)
                    },
                    nfcWriteRequest = nfcWriteRequest,
                    nfcWriteResult = nfcWriteResult,
                    onStartNfcWriteRequest = { request ->
                        nfcWriteRequest = request
                        nfcWriteResult = null
                    },
                    onCancelNfcWrite = {
                        nfcWriteRequest = null
                        nfcWriteResult = null
                    },
                    onNfcWriteResultConsumed = {
                        nfcWriteRequest = null
                        nfcWriteResult = null
                    },
                    nfcCapturedTagId = nfcCapturedTagId,
                    onStartNfcCapture = {
                        nfcCaptureRequested = true
                        nfcCapturedTagId = null
                    },
                    onCancelNfcCapture = { nfcCaptureRequested = false },
                    onNfcCaptureConsumed = { nfcCapturedTagId = null },
                    tagAlarmConflicts = tagAlarmConflicts,
                    onTagAlarmConflictResolved = { turnOff ->
                        val others = tagAlarmConflicts.orEmpty()
                        tagAlarmConflicts = null
                        if (turnOff) turnOffTagAlarms(others)
                    },
                    startOnSettings = settings!!.supabaseUrl.isBlank()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent, fromForeground = isActivityResumed)
        handleReminderIntent(intent)
    }

    // Reminder notifications open MainActivity with EXTRA_REMINDER_ID (standalone
    // reminder) or EXTRA_TASK_ID (a task's own reminder). Reminder wins when both
    // are present, matching AlarmReceiver.
    private fun handleReminderIntent(intent: Intent?) {
        if (intent == null) return
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID)
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
        pendingReminderView = when {
            !reminderId.isNullOrBlank() -> ReminderViewKind.REMINDER.routeArg to reminderId
            !taskId.isNullOrBlank() -> ReminderViewKind.TASK.routeArg to taskId
            else -> return
        }
    }

    private fun handleNfcIntent(intent: Intent, fromForeground: Boolean) {
        val writeRequest = nfcWriteRequest
        if (writeRequest != null) {
            val tag = intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                val uri = writeRequest.uri
                if (uri == null) {
                    // An erase: note which id is leaving the sticker, then wipe it.
                    val leaving = NfcHandler.currentTagId(tag)
                    val result = NfcHandler.eraseTag(tag)
                    nfcWriteResult = result
                    if (result == NfcWriteResult.Success && leaving != null) forgetSticker(leaving)
                } else {
                    val result = NfcHandler.writeUri(tag, uri)
                    nfcWriteResult = result
                    if (result == NfcWriteResult.Success) recordSticker(writeRequest.id)
                }
            }
            return
        }
        val tagId = NfcHandler.extractTagId(intent)
        if (tagId != null) {
            // Whatever happens next, this id was just read off a real sticker.
            recordSticker(tagId)
            when {
                nfcCaptureRequested -> {
                    nfcCaptureRequested = false
                    nfcCapturedTagId = tagId
                }
                else -> routeScannedTag(tagId, fromForeground)
            }
        }
        intent.getStringExtra(WIDGET_DESTINATION_EXTRA)?.let { pendingWidgetDestination = it }
    }

    private fun recordSticker(tagId: String) {
        lifecycleScope.launch { runCatching { tagStickerStore.record(tagId) } }
    }

    private fun forgetSticker(tagId: String) {
        lifecycleScope.launch { runCatching { tagStickerStore.forget(tagId) } }
    }

    // A tag-alarm's tag is resolved first, on-device and offline, so the chore path
    // never sees it: before this, a background tap wrote a Supabase scan row for
    // any id at all. Only a tag no tag-alarm owns goes on to the chore flows.
    private fun routeScannedTag(tagId: String, fromForeground: Boolean) {
        lifecycleScope.launch {
            val alarm = runCatching { tagAlarmService.findByTagId(tagId) }.getOrNull()
            when {
                alarm != null -> armTagAlarm(alarm)
                fromForeground -> pendingNfcTagId = tagId
                else -> autoLogChore(tagId)
            }
        }
    }

    // The tap's whole feedback is a toast naming the alarm and its first ring; the
    // app is already in front (Android routes NFC through the activity) but it asks
    // nothing unless another tag-alarm is set for the same morning.
    private suspend fun armTagAlarm(alarm: ReminderDto) {
        val armed = runCatching { tagAlarmService.arm(alarm.id) }.getOrElse {
            Toast.makeText(this, "Could not set ${alarm.subject}", Toast.LENGTH_SHORT).show()
            return
        } ?: return
        Toast.makeText(this, TagAlarmText.armedToast(armed.alarm, Instant.now()), Toast.LENGTH_LONG).show()
        if (armed.conflicts.isNotEmpty()) tagAlarmConflicts = armed.conflicts
        WidgetUpdater.updateAll(applicationContext)
    }

    private fun turnOffTagAlarms(others: List<ReminderDto>) {
        lifecycleScope.launch {
            runCatching { tagAlarmService.disarmAll(others.map { it.id }) }
            WidgetUpdater.updateAll(applicationContext)
        }
    }

    private fun autoLogChore(tagId: String) {
        lifecycleScope.launch {
            runCatching {
                val label = choreRepository.findByTagId(tagId)?.label ?: tagId
                choreRepository.logChore(tagId)
                WidgetUpdater.updateAll(applicationContext)
                Toast.makeText(this@MainActivity, "$label logged", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this@MainActivity, "Could not log chore", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
        nfcAdapter?.disableForegroundDispatch(this)
    }
}