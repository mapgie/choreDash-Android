package com.mapgie.dash

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
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
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.preferences.ThemeMode
import com.mapgie.dash.nfc.NfcHandler
import com.mapgie.dash.ui.navigation.DashNavGraph
import com.mapgie.dash.ui.theme.DashTheme
import com.mapgie.dash.widget.WIDGET_DESTINATION_EXTRA
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null

    // Compose state: survives recompositions, drives the NFC sheet trigger
    private var pendingNfcTagId by mutableStateOf<String?>(null)

    // Compose state: set when launched from a home screen widget, drives navigation
    private var pendingWidgetDestination by mutableStateOf<String?>(null)

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

        NfcHandler.extractTagId(intent)?.let { pendingNfcTagId = it }
        pendingWidgetDestination = intent.getStringExtra(WIDGET_DESTINATION_EXTRA)

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = null)
            val darkTheme = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            DashTheme(darkTheme = darkTheme) {
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
                    startOnSettings = settings!!.supabaseUrl.isBlank()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NfcHandler.extractTagId(intent)?.let { pendingNfcTagId = it }
        intent.getStringExtra(WIDGET_DESTINATION_EXTRA)?.let { pendingWidgetDestination = it }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
}