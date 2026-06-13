package com.mapgie.dash.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapgie.dash.data.preferences.ThemeMode
import com.mapgie.dash.permission.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLicenses: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val owners by viewModel.owners.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var url by rememberSaveable(settings?.supabaseUrl) { mutableStateOf(settings?.supabaseUrl ?: "") }
    var key by rememberSaveable(settings?.supabaseKey) { mutableStateOf(settings?.supabaseKey ?: "") }
    var owner by rememberSaveable(settings?.ownerHandle) { mutableStateOf(settings?.ownerHandle ?: "") }
    var keyVisible by remember { mutableStateOf(false) }
    var ownerExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var exactAlarmsAllowed by remember { mutableStateOf(PermissionHelper.canScheduleExactAlarms(context)) }
    var notificationsEnabled by remember { mutableStateOf(PermissionHelper.areNotificationsEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmsAllowed = PermissionHelper.canScheduleExactAlarms(context)
                notificationsEnabled = PermissionHelper.areNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { viewModel.loadOwners() }

    LaunchedEffect(saveError) {
        saveError?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearSaveError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Supabase connection",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Project URL") },
                placeholder = { Text("https://xxxx.supabase.co") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Anon key") },
                singleLine = true,
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (keyVisible) "Hide key" else "Show key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = ownerExpanded,
                onExpandedChange = { ownerExpanded = it }
            ) {
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("I am") },
                    placeholder = { Text("Your handle") },
                    singleLine = true,
                    trailingIcon = {
                        if (owners.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(expanded = ownerExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                )
                if (owners.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = ownerExpanded,
                        onDismissRequest = { ownerExpanded = false }
                    ) {
                        owners.forEach { o ->
                            DropdownMenuItem(
                                text = { Text(o) },
                                onClick = { owner = o; ownerExpanded = false }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.saveCredentials(url, key, owner) },
                enabled = url.isNotBlank() && key.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            HorizontalDivider()

            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium
            )

            val currentTheme = settings?.themeMode ?: ThemeMode.SYSTEM
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = currentTheme == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size
                        ),
                        label = { Text(mode.label) }
                    )
                }
            }

            HorizontalDivider()

            Text(
                "Reminders & alerts",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "Task reminders use a dedicated alarm so they can sound even when Do Not " +
                    "Disturb is on. If reminders stop arriving, check these settings.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (!notificationsEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Notifications are turned off",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { context.startActivity(PermissionHelper.notificationSettingsIntent(context)) }) {
                        Text("Open settings")
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmsAllowed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Exact alarms are turned off, reminders may be delayed",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { context.startActivity(PermissionHelper.exactAlarmSettingsIntent(context)) }) {
                        Text("Open settings")
                    }
                }
            }

            if (notificationsEnabled && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || exactAlarmsAllowed)) {
                Text(
                    "Reminders are fully enabled.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            Text(
                "About",
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(
                onClick = onNavigateToLicenses,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Open-source licenses",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }