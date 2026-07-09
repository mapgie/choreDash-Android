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
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.preferences.ThemeMode
import com.mapgie.dash.data.repository.ChoreRepository
import com.mapgie.dash.nfc.NfcHandler
import com.mapgie.dash.nfc.NfcWriteResult
import com.mapgie.dash.ui.navigation.DashNavGraph
import com.mapgie.dash.ui.theme.AppTheme
import com.mapgie.dash.ui.theme.CustomHSL
import com.mapgie.dash.ui.theme.DashTheme
import com.mapgie.dash.widget.WIDGET_DESTINATION_EXTRA
import com.mapgie.dash.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var choreRepository: ChoreRepository

    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null
    private var isActivityResumed = false

    // Compose state: survives recompositions, drives the NFC sheet trigger
    private var pendingNfcTagId by mutableStateOf<String?>(null)

    // Compose state: set when launched from a home screen widget, drives navigation
    private var pendingWidgetDestination by mutableStateOf<String?>(null)

    // When set, the next scanned tag is written with this chore tag ID instead of being read
    private var nfcWriteRequest by mutableStateOf<String?>(null)
    private var nfcWriteResult by mutableStateOf<NfcWriteResult?>(null)

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

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = null)
            val darkTheme = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            val appTheme = settings?.let {
                runCatching { AppTheme.valueOf(it.appTheme) }.getOrDefault(AppTheme.MIST)
            } ?: AppTheme.MIST
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
                )
            }
            DashTheme(
                appTheme  = appTheme,
                darkTheme = darkTheme,
                wcag      = settings?.wcagMode ?: false,
                customHSL = customHSL,
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
                    nfcWriteRequest = nfcWriteRequest,
                    nfcWriteResult = nfcWriteResult,
                    onStartNfcWrite = { tagId ->
                        nfcWriteRequest = tagId
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
                    startOnSettings = settings!!.supabaseUrl.isBlank()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent, fromForeground = isActivityResumed)
    }

    private fun handleNfcIntent(intent: Intent, fromForeground: Boolean) {
        val writeTagId = nfcWriteRequest
        if (writeTagId != null) {
            val tag = intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                nfcWriteResult = NfcHandler.writeTagId(tag, writeTagId)
            }
            return
        }
        val tagId = NfcHandler.extractTagId(intent)
        if (tagId != null) {
            if (fromForeground) {
                pendingNfcTagId = tagId
            } else {
                autoLogChore(tagId)
            }
        }
        intent.getStringExtra(WIDGET_DESTINATION_EXTRA)?.let { pendingWidgetDestination = it }
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