package com.mapgie.dash.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
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
import com.mapgie.dash.data.model.CadenceBucket
import com.mapgie.dash.data.preferences.ThemeMode
import com.mapgie.dash.permission.PermissionHelper
import com.mapgie.dash.ui.theme.AppTheme
import com.mapgie.dash.ui.theme.CompactThemePicker
import com.mapgie.dash.ui.theme.SavedThemesList
import java.io.BufferedReader
import java.io.InputStreamReader

private enum class SettingsSubScreen {
    NONE, CONNECTION, APPEARANCE, DISPLAY, REMINDERS, WIDGET, ABOUT
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
        SettingsSubScreen.DISPLAY -> DisplaySubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
            viewModel = viewModel,
        )
        SettingsSubScreen.REMINDERS -> RemindersSubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
            viewModel = viewModel,
        )
        SettingsSubScreen.WIDGET -> WidgetSubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
            viewModel = viewModel,
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
            SettingsNavItem(
                title = "Display",
                subtitle = "Grouping and visibility filters for chores and tasks",
                icon = Icons.Filled.Tune,
                onClick = { onNavigate(SettingsSubScreen.DISPLAY) }
            )

            HorizontalDivider()

            SettingsSectionHeader("Widgets")
            SettingsNavItem(
                title = "Widget customisation",
                subtitle = "Choose what your home-screen widget shows",
                icon = Icons.Filled.Widgets,
                onClick = { onNavigate(SettingsSubScreen.WIDGET) }
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
    val currentThemeMode = settings?.themeMode ?: ThemeMode.SYSTEM
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (currentThemeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val selectedAppTheme = settings?.let {
        runCatching { AppTheme.valueOf(it.appTheme) }.getOrDefault(AppTheme.MIST)
    } ?: AppTheme.MIST

    val customPrimaryHue          = settings?.customPrimaryHue          ?: 150f
    val customPrimarySaturation   = settings?.customPrimarySaturation   ?: 0.5f
    val customPrimaryLightness    = settings?.customPrimaryLightness    ?: 0.4f
    val customSecondaryHue        = settings?.customSecondaryHue        ?: 120f
    val customSecondarySaturation = settings?.customSecondarySaturation ?: 0.4f
    val customSecondaryLightness  = settings?.customSecondaryLightness  ?: 0.4f
    val customTertiaryHue         = settings?.customTertiaryHue         ?: 200f
    val customTertiarySaturation  = settings?.customTertiarySaturation  ?: 0.4f
    val customTertiaryLightness   = settings?.customTertiaryLightness   ?: 0.4f

    val customColorThemes by viewModel.customColorThemes.collectAsState()
    val activeProfileId = settings?.customActiveProfileId ?: -1L

    var themeName by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(activeProfileId) {
        if (activeProfileId != -1L) {
            val active = customColorThemes.find { it.id == activeProfileId }
            if (active != null) themeName = active.name
        } else {
            themeName = "Theme ${customColorThemes.size + 1}"
        }
    }

    SettingsSubScreenScaffold(title = "Appearance", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Light / Dark / System mode toggle ─────────────────────────────
            Text(
                "Brightness",
                style = MaterialTheme.typography.titleMedium,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = currentThemeMode == mode,
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

            HorizontalDivider()

            // ── Palette / custom colour picker ────────────────────────────────
            CompactThemePicker(
                selectedTheme = selectedAppTheme,
                onThemeSelected = { viewModel.setAppTheme(it) },
                customPrimaryHue = customPrimaryHue,
                customPrimarySaturation = customPrimarySaturation,
                customPrimaryLightness = customPrimaryLightness,
                customSecondaryHue = customSecondaryHue,
                customSecondarySaturation = customSecondarySaturation,
                customSecondaryLightness = customSecondaryLightness,
                customTertiaryHue = customTertiaryHue,
                customTertiarySaturation = customTertiarySaturation,
                customTertiaryLightness = customTertiaryLightness,
                onCustomHSLChange = { pH, pS, pL, sH, sS, sL, tH, tS, tL ->
                    viewModel.setCustomHSL(pH, pS, pL, sH, sS, sL, tH, tS, tL)
                },
                darkTheme = darkTheme,
            )

            // ── Save section (visible only when custom palette is active) ─────
            if (selectedAppTheme == AppTheme.CUSTOM) {
                HorizontalDivider()
                Text(
                    "Save theme",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = themeName,
                    onValueChange = { themeName = it },
                    label = { Text("Theme name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (activeProfileId != -1L) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { if (themeName.isNotBlank()) viewModel.saveCustomColorTheme(themeName.trim()) },
                            enabled = themeName.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) { Text("Save as new") }
                        Button(
                            onClick = { if (themeName.isNotBlank()) viewModel.updateCustomColorTheme(themeName.trim()) },
                            enabled = themeName.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) { Text("Update") }
                    }
                } else {
                    Button(
                        onClick = { if (themeName.isNotBlank()) viewModel.saveCustomColorTheme(themeName.trim()) },
                        enabled = themeName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save") }
                }
            }

            // ── Saved themes list ─────────────────────────────────────────────
            if (customColorThemes.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "Saved themes",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            SavedThemesList(
                themes = customColorThemes,
                activeProfileId = activeProfileId,
                onLoad = { viewModel.loadCustomColorTheme(it) },
                onDelete = { viewModel.deleteCustomColorTheme(it) },
                onRename = { theme, name -> viewModel.renameCustomColorTheme(theme, name) },
            )

            Spacer(Modifier.height(8.dp))
        }
    }

}

@Composable
private fun DisplaySubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()

    val groupChores = settings?.groupChoresByCategory ?: true
    val groupTasks = settings?.groupTasksByCategory ?: true
    val smartVisibility = settings?.smartChoreVisibility ?: true
    val choreLeadDays = settings?.choreLeadDays ?: emptyMap()
    val taskThreshold = settings?.taskHideThresholdDays ?: -1

    var taskThresholdText by rememberSaveable(taskThreshold) {
        mutableStateOf(if (taskThreshold >= 0) taskThreshold.toString() else "14")
    }

    SettingsSubScreenScaffold(title = "Display", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Grouping", style = MaterialTheme.typography.titleMedium)

            SwitchRow(
                label = "Group chores by category",
                subtitle = "Show sticky category headers in the chores list",
                checked = groupChores,
                onCheckedChange = { viewModel.setGroupChoresByCategory(it) }
            )

            SwitchRow(
                label = "Group tasks by category",
                subtitle = "Show sticky category headers in the tasks list",
                checked = groupTasks,
                onCheckedChange = { viewModel.setGroupTasksByCategory(it) }
            )

            HorizontalDivider()

            Text("Visibility", style = MaterialTheme.typography.titleMedium)

            SwitchRow(
                label = "Smart chore visibility",
                subtitle = "Hide chores until they're close to due, based on how often each repeats",
                checked = smartVisibility,
                onCheckedChange = { viewModel.setSmartChoreVisibility(it) }
            )
            if (smartVisibility) {
                Text(
                    "How many days before a chore is due it reappears in the list:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CadenceBucket.entries.forEach { bucket ->
                    LeadTimeStepperRow(
                        bucket = bucket,
                        days = choreLeadDays[bucket] ?: bucket.defaultLeadDays,
                        onDaysChange = { viewModel.setChoreLeadDays(bucket, it) }
                    )
                }
                OutlinedButton(onClick = { viewModel.resetChoreLeadDays() }) {
                    Text("Reset to defaults")
                }
            }

            SwitchRow(
                label = "Hide tasks not due soon",
                subtitle = "Hide tasks whose due date is further away than the threshold",
                checked = taskThreshold >= 0,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        val days = taskThresholdText.toIntOrNull()?.coerceAtLeast(1) ?: 14
                        viewModel.setTaskHideThresholdDays(days)
                    } else {
                        viewModel.setTaskHideThresholdDays(-1)
                    }
                }
            )
            if (taskThreshold >= 0) {
                OutlinedTextField(
                    value = taskThresholdText,
                    onValueChange = { v ->
                        if (v.length <= 4 && v.all { it.isDigit() }) {
                            taskThresholdText = v
                            v.toIntOrNull()?.let { days ->
                                if (days >= 1) viewModel.setTaskHideThresholdDays(days)
                            }
                        }
                    },
                    label = { Text("Days") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * One cadence bucket's lead time control: bucket name and example on the left,
 * a minus/value/plus stepper on the right.
 */
@Composable
private fun LeadTimeStepperRow(
    bucket: CadenceBucket,
    days: Int,
    onDaysChange: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(bucket.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                bucket.example,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onDaysChange((days - 1).coerceAtLeast(0)) },
                enabled = days > 0,
            ) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = "Show ${bucket.label} chores fewer days before due"
                )
            }
            Text(
                text = if (days == 1) "1 day" else "$days days",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 56.dp)
            )
            IconButton(
                onClick = { onDaysChange((days + 1).coerceAtMost(99)) },
                enabled = days < 99,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Show ${bucket.label} chores more days before due"
                )
            }
        }
    }
}

@Composable
private fun RemindersSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val currentDeliveryMode = settings?.deliveryMode ?: "NOTIFICATION"

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

    val deliveryModes = listOf("ALARM", "NOTIFICATION", "SILENT")
    val deliveryModeLabels = listOf("Alarm", "Notification", "Silent")

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
                "Notification style",
                style = MaterialTheme.typography.titleMedium
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                deliveryModes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = currentDeliveryMode == mode,
                        onClick = { viewModel.setDeliveryMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = deliveryModes.size
                        ),
                        modifier = Modifier.semantics { role = Role.RadioButton },
                        label = { Text(deliveryModeLabels[index]) }
                    )
                }
            }

            Text(
                text = when (currentDeliveryMode) {
                    "ALARM" -> "Plays alarm sound, bypasses Do Not Disturb"
                    "SILENT" -> "No sound or vibration"
                    else -> "Standard notification sound"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Text(
                "If reminders stop arriving, check these permissions.",
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
private fun WidgetSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val contentType = settings?.widgetContentType ?: "CHORES"
    val priorityFilter = settings?.widgetPriorityFilter ?: "ALL"
    val ownerFilter = settings?.widgetOwnerFilter ?: "EVERYBODY"

    val contentTypes = listOf("CHORES", "TASKS", "REMINDERS")
    val contentTypeLabels = listOf("Chores", "Tasks", "Reminders")

    val priorityFilters = listOf("ALL", "RED", "AMBER")
    val priorityFilterLabels = listOf("All", "Red", "Amber")

    val ownerFilters = listOf("EVERYBODY", "MINE")
    val ownerFilterLabels = listOf("Everybody's", "Mine")

    SettingsSubScreenScaffold(title = "Widget customisation", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Show", style = MaterialTheme.typography.titleMedium)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                contentTypes.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = contentType == type,
                        onClick = { viewModel.setWidgetContentType(type) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = contentTypes.size
                        ),
                        modifier = Modifier.semantics { role = Role.RadioButton },
                        label = { Text(contentTypeLabels[index]) }
                    )
                }
            }

            HorizontalDivider()

            Text("Priority", style = MaterialTheme.typography.titleMedium)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                priorityFilters.forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = priorityFilter == filter,
                        onClick = { viewModel.setWidgetPriorityFilter(filter) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = priorityFilters.size
                        ),
                        modifier = Modifier.semantics { role = Role.RadioButton },
                        label = { Text(priorityFilterLabels[index]) }
                    )
                }
            }

            HorizontalDivider()

            Text("Whose", style = MaterialTheme.typography.titleMedium)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ownerFilters.forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = ownerFilter == filter,
                        onClick = { viewModel.setWidgetOwnerFilter(filter) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ownerFilters.size
                        ),
                        modifier = Modifier.semantics { role = Role.RadioButton },
                        label = { Text(ownerFilterLabels[index]) }
                    )
                }
            }
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
