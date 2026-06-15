package com.mapgie.dash.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapgie.dash.BuildConfig
import com.mapgie.dash.data.preferences.ThemeMode
import com.mapgie.dash.permission.PermissionHelper
import java.io.BufferedReader
import java.io.InputStreamReader

private enum class SettingsSubScreen {
    NONE, CONNECTION, APPEARANCE, REMINDERS, ABOUT
}

private const val CHANGELOG_URL = "https://github.com/mapgie/choreDash-Android/blob/main/CHANGELOG.md"

@Composable
fun SettingsScreen(
    onNavigateToLicenses: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var subScreen by rememberSaveable { mutableStateOf(SettingsSubScreen.NONE) }

    BackHandler(enabled = subScreen != SettingsSubScreen.NONE) {
        subScreen = SettingsSubScreen.NONE
    }

    when (subScreen) {
        SettingsSubScreen.NONE -> SettingsMainList(
            onNavigate = { subScreen = it }
        )
        SettingsSubScreen.CONNECTION -> ConnectionSubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
            viewModel = viewModel,
        )
        SettingsSubScreen.APPEARANCE -> AppearanceSubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
            viewModel = viewModel,
        )
        SettingsSubScreen.REMINDERS -> RemindersSubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
        )
        SettingsSubScreen.ABOUT -> AboutSubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
            onNavigateToLicenses = onNavigateToLicenses,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainList(
    onNavigate: (SettingsSubScreen) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            SettingsSectionHeader("Account")
            SettingsNavItem(
                title = "Supabase connection",
                subtitle = "Project URL, anon key, and your owner handle",
                icon = Icons.Filled.Storage,
                onClick = { onNavigate(SettingsSubScreen.CONNECTION) }
            )

            HorizontalDivider()

            SettingsSectionHeader("Personalisation")
            SettingsNavItem(
                title = "Appearance",
                subtitle = "Light, dark, or system theme",
                icon = Icons.Filled.Palette,
                onClick = { onNavigate(SettingsSubScreen.APPEARANCE) }
            )

            HorizontalDivider()

            SettingsSectionHeader("Reminders")
            SettingsNavItem(
                title = "Reminders & alerts",
                subtitle = "Notifications, exact alarms, and Do Not Disturb access",
                icon = Icons.Filled.Notifications,
                onClick = { onNavigate(SettingsSubScreen.REMINDERS) }
            )

            HorizontalDivider()

            SettingsSectionHeader("About")
            SettingsNavItem(
                title = "About choreDash",
                subtitle = "Version, what's new, and licenses",
                icon = Icons.Filled.Info,
                onClick = { onNavigate(SettingsSubScreen.ABOUT) }
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
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

    LaunchedEffect(Unit) { viewModel.loadOwners() }

    LaunchedEffect(saveError) {
        saveError?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearSaveError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Supabase connection") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.semantics { role = Role.Button }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "New install? Each device needs its own Supabase project. " +
                            "Run supabase/schema.sql from the project repo in your " +
                            "Supabase SQL Editor, then enter that project's URL and " +
                            "anon key below. See the README for full setup steps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
        }
    }
}

@Composable
private fun AppearanceSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val currentTheme = settings?.themeMode ?: ThemeMode.SYSTEM

    SettingsSubScreenScaffold(title = "Appearance", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Theme",
                style = MaterialTheme.typography.titleMedium
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = currentTheme == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size
                        ),
                        modifier = Modifier.semantics { role = Role.RadioButton },
                        label = { Text(mode.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RemindersSubScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var exactAlarmsAllowed by remember { mutableStateOf(PermissionHelper.canScheduleExactAlarms(context)) }
    var notificationsEnabled by remember { mutableStateOf(PermissionHelper.areNotificationsEnabled(context)) }
    var dndAccessGranted by remember { mutableStateOf(PermissionHelper.isDndAccessGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmsAllowed = PermissionHelper.canScheduleExactAlarms(context)
                notificationsEnabled = PermissionHelper.areNotificationsEnabled(context)
                dndAccessGranted = PermissionHelper.isDndAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val remindersFullyEnabled = notificationsEnabled &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || exactAlarmsAllowed) &&
        dndAccessGranted

    SettingsSubScreenScaffold(title = "Reminders & alerts", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Task reminders use a dedicated alarm so they can sound even when Do Not " +
                    "Disturb is on. If reminders stop arriving, check these permissions.",
                style = MaterialTheme.typography.bodyMedium
            )

            PermissionRow(
                title = "Notifications",
                granted = notificationsEnabled,
                icon = { Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { context.startActivity(PermissionHelper.notificationSettingsIntent(context)) }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionRow(
                    title = "Exact alarms",
                    granted = exactAlarmsAllowed,
                    icon = { Icon(Icons.Outlined.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { context.startActivity(PermissionHelper.exactAlarmSettingsIntent(context)) }
                )
            }

            PermissionRow(
                title = "Do Not Disturb access",
                subtitle = "Lets alarms sound when Do Not Disturb is on",
                granted = dndAccessGranted,
                icon = { Icon(Icons.Outlined.DoNotDisturbOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { context.startActivity(PermissionHelper.dndAccessSettingsIntent(context)) }
            )

            Text(
                if (remindersFullyEnabled) "Reminders are fully enabled." else "Reminders are not fully enabled.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Composable
private fun AboutSubScreen(
    onBack: () -> Unit,
    onNavigateToLicenses: () -> Unit,
) {
    val context = LocalContext.current
    var showChangelog by remember { mutableStateOf(false) }

    SettingsSubScreenScaffold(title = "About", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "choreDash helps your household share chores and tasks, synced through " +
                    "your own Supabase project.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            FilledTonalButton(
                onClick = { showChangelog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("What's New")
            }

            OutlinedButton(
                onClick = onNavigateToLicenses,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open-source licenses")
            }

            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }

    if (showChangelog) {
        val entries = remember {
            runCatching {
                val text = context.assets.open("CHANGELOG.md").use { input ->
                    BufferedReader(InputStreamReader(input)).readText()
                }
                parseChangelog(text)
            }.getOrDefault(emptyList())
        }
        ChangelogDialog(
            entries = entries,
            onDismiss = { showChangelog = false },
            onViewFullChangelog = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CHANGELOG_URL)))
            }
        )
    }
}

/**
 * A permission entry showing its live grant state. The status is communicated
 * by both an icon (shape) and a text label, never colour alone, so it remains
 * legible for colour-blind users.
 */
@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val statusText = if (granted) "Allowed" else "Tap to allow"
    val statusIcon = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline
    val statusTint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { s -> { Text(s) } },
        leadingContent = {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(statusIcon, contentDescription = null, tint = statusTint, modifier = Modifier.size(18.dp))
                Text(statusText, style = MaterialTheme.typography.labelMedium, color = statusTint)
            }
        },
        modifier = Modifier
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
    )
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
