package com.mapgie.dash.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mapgie.dash.BuildConfig
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.CadenceBucket
import com.mapgie.dash.data.model.ReminderLabelStyle
import com.mapgie.dash.data.preferences.DEFAULT_FAB_ORDER
import com.mapgie.dash.data.preferences.ThemeMode
import com.mapgie.dash.notification.NotificationHelper
import com.mapgie.dash.permission.PermissionHelper
import com.mapgie.dash.ui.components.core.PageHeader
import com.mapgie.dash.ui.theme.AppTheme
import com.mapgie.dash.ui.theme.CompactThemePicker
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.SavedThemesList
import com.mapgie.dash.ui.theme.isDarkScheme
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt

private enum class SettingsSubScreen {
    NONE, CONNECTION, APPEARANCE, COLOURS, CATEGORIES, DISPLAY, QUICK_ADD, REMINDERS, WIDGET, ABOUT, HELP
}

private const val CHANGELOG_URL = "https://github.com/mapgie/choreDash-Android/blob/main/CHANGELOG.md"

/** Content inset for every settings page (18dp per the 3a/4a mock-ups). */
private val PageInset = 18.dp

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
        SettingsSubScreen.COLOURS -> ColoursSubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
            viewModel = viewModel,
        )
        SettingsSubScreen.CATEGORIES -> CategoriesSubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
            viewModel = viewModel,
        )
        SettingsSubScreen.DISPLAY -> DisplaySubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
            viewModel = viewModel,
        )
        SettingsSubScreen.QUICK_ADD -> QuickAddSubScreen(
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
        SettingsSubScreen.HELP -> HelpSubScreen(
            onBack = { subScreen = SettingsSubScreen.NONE },
            viewModel = viewModel,
        )
    }
}

/**
 * The Settings tab (handoff 3a-6): the page header, then one grouped card per
 * section (APPEARANCE, PERSONALISATION, REMINDERS, ACCOUNT, ABOUT) whose rows
 * open the sub-screens.
 */
@Composable
private fun SettingsMainList(
    onNavigate: (SettingsSubScreen) -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            PageHeader(
                title = "settings",
                accent = MaterialTheme.colorScheme.secondary,
            )
            Column(
                modifier = Modifier.padding(horizontal = PageInset, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsSectionLabel("Appearance")
                SettingsCard {
                    SettingsNavRow(
                        title = "Appearance",
                        subtitle = "Brightness, colour palette and saved themes",
                        onClick = { onNavigate(SettingsSubScreen.APPEARANCE) }
                    )
                    SettingsHairline()
                    SettingsNavRow(
                        title = "Colours",
                        subtitle = "Colour chores by severity or category, and pick the tints",
                        onClick = { onNavigate(SettingsSubScreen.COLOURS) }
                    )
                    SettingsHairline()
                    SettingsNavRow(
                        title = "Categories",
                        subtitle = "Reorder, rename, and give each category an icon and colour",
                        onClick = { onNavigate(SettingsSubScreen.CATEGORIES) }
                    )
                }

                SettingsSectionLabel("Personalisation")
                SettingsCard {
                    SettingsNavRow(
                        title = "Display",
                        subtitle = "Grouping and visibility for chores and tasks",
                        onClick = { onNavigate(SettingsSubScreen.DISPLAY) }
                    )
                    SettingsHairline()
                    SettingsNavRow(
                        title = "Quick add button",
                        subtitle = "Reorder the + menu, rename reminders",
                        onClick = { onNavigate(SettingsSubScreen.QUICK_ADD) }
                    )
                    SettingsHairline()
                    SettingsNavRow(
                        title = "Widget customisation",
                        subtitle = "Choose what your home-screen widget shows",
                        onClick = { onNavigate(SettingsSubScreen.WIDGET) }
                    )
                }

                SettingsSectionLabel("Reminders")
                SettingsCard {
                    SettingsNavRow(
                        title = "Reminders & alerts",
                        subtitle = "Notifications, exact alarms, Do Not Disturb",
                        onClick = { onNavigate(SettingsSubScreen.REMINDERS) }
                    )
                }

                SettingsSectionLabel("Account")
                SettingsCard {
                    SettingsNavRow(
                        title = "Supabase connection",
                        subtitle = "Project URL, anon key, and your owner handle",
                        onClick = { onNavigate(SettingsSubScreen.CONNECTION) }
                    )
                }

                SettingsSectionLabel("Help & about")
                SettingsCard {
                    SettingsNavRow(
                        title = "Help",
                        subtitle = "What chores, tasks and reminders are for",
                        onClick = { onNavigate(SettingsSubScreen.HELP) }
                    )
                    SettingsHairline()
                    SettingsNavRow(
                        title = "About choreDash",
                        subtitle = "Version, what's new, and licenses",
                        onClick = { onNavigate(SettingsSubScreen.ABOUT) }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Scrolling column every sub-screen uses: page inset, 14dp section gap. */
@Composable
private fun SubScreenColumn(
    innerPadding: PaddingValues,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(innerPadding)
            .padding(horizontal = PageInset, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
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
        topBar = { SubScreenHeader(title = "Supabase connection", onBack = onBack) }
    ) { innerPadding ->
        SubScreenColumn(innerPadding) {
            SettingsCard(contentPadding = PaddingValues(18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = LucideIcons.CircleAlert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "New install? Each device needs its own Supabase project. " +
                            "Run supabase/schema.sql from the project repo in your " +
                            "Supabase SQL Editor, then enter that project's URL and " +
                            "anon key below. See the README for full setup steps.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Anon key") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
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
                    shape = MaterialTheme.shapes.medium,
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

            AccentPillButton(
                text = "Save",
                onClick = { viewModel.saveCredentials(url, key, owner) },
                enabled = url.isNotBlank() && key.isNotBlank(),
            )
        }
    }
}

/**
 * Settings › Appearance (handoff 4a-3): BRIGHTNESS segmented control, the WCAG
 * checkbox, the COLOUR PALETTE tile grid and the SAVED THEMES list.
 */
@Composable
private fun AppearanceSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val currentThemeMode = settings?.themeMode ?: ThemeMode.DARK
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (currentThemeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val selectedAppTheme = settings?.let {
        runCatching { AppTheme.valueOf(it.appTheme) }.getOrDefault(AppTheme.CREAM)
    } ?: AppTheme.CREAM

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
        SubScreenColumn(innerPadding) {
            SettingsSectionLabel("Brightness")
            CozySegmented(
                options = ThemeMode.entries,
                selected = currentThemeMode,
                onSelect = { viewModel.setThemeMode(it) },
                label = { it.label },
            )

            // WCAG toggle applies to the built-in palettes; custom colours are
            // applied exactly as picked, so it is hidden while Custom is active.
            if (selectedAppTheme != AppTheme.CUSTOM) {
                CozyCheckboxRow(
                    title = "WCAG accessible colours",
                    subtitle = "Increases contrast for text and interactive elements",
                    checked = settings?.wcagMode ?: false,
                    onCheckedChange = { viewModel.setWcagMode(it) },
                )
            }

            SettingsSectionLabel("Colour palette")
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
                customLightBackgroundArgb = settings?.customLightBackgroundArgb ?: 0,
                customDarkBackgroundArgb = settings?.customDarkBackgroundArgb ?: 0,
                onCustomBackgroundArgbsChange = { light, dark ->
                    viewModel.setCustomBackgroundArgbs(light, dark)
                },
            )

            // Save section, only while the custom palette is active.
            if (selectedAppTheme == AppTheme.CUSTOM) {
                SettingsSectionLabel("Save theme")
                SettingsCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                    OutlinedTextField(
                        value = themeName,
                        onValueChange = { themeName = it },
                        label = { Text("Theme name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    if (activeProfileId != -1L) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinePillButton(
                                text = "Save as new",
                                onClick = { if (themeName.isNotBlank()) viewModel.saveCustomColorTheme(themeName.trim()) },
                                enabled = themeName.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            )
                            AccentPillButton(
                                text = "Update",
                                onClick = { if (themeName.isNotBlank()) viewModel.updateCustomColorTheme(themeName.trim()) },
                                enabled = themeName.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        AccentPillButton(
                            text = "Save",
                            onClick = { if (themeName.isNotBlank()) viewModel.saveCustomColorTheme(themeName.trim()) },
                            enabled = themeName.isNotBlank(),
                        )
                    }
                }
            }

            if (customColorThemes.isNotEmpty()) {
                SettingsSectionLabel("Saved themes")
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

/**
 * Settings › Display (handoff 4a-4): a GROUPING card with two toggles, a
 * VISIBILITY card with the smart-visibility toggle and one stepper per cadence
 * bucket, and the outlined "Reset to defaults" pill.
 */
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
        SubScreenColumn(innerPadding) {
            SettingsSectionLabel("Grouping")
            SettingsCard {
                SettingsToggleRow(
                    title = "Group chores by category",
                    subtitle = "Show sticky category headers in the chores list",
                    checked = groupChores,
                    onCheckedChange = { viewModel.setGroupChoresByCategory(it) }
                )
                SettingsHairline()
                SettingsToggleRow(
                    title = "Group tasks by category",
                    subtitle = "Show sticky category headers in the tasks list",
                    checked = groupTasks,
                    onCheckedChange = { viewModel.setGroupTasksByCategory(it) }
                )
            }

            SettingsSectionLabel("Visibility")
            SettingsCard {
                SettingsToggleRow(
                    title = "Smart chore visibility",
                    subtitle = "Hide chores until they're close to due, based on how often each repeats",
                    checked = smartVisibility,
                    onCheckedChange = { viewModel.setSmartChoreVisibility(it) }
                )
                if (smartVisibility) {
                    SettingsHairline()
                    Text(
                        "How many days before a chore is due it reappears:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        color = LocalDashTokens.current.inkFaint,
                        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                    )
                    CadenceBucket.entries.forEach { bucket ->
                        LeadTimeStepperRow(
                            bucket = bucket,
                            days = choreLeadDays[bucket] ?: bucket.defaultLeadDays,
                            onDaysChange = { viewModel.setChoreLeadDays(bucket, it) }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                SettingsHairline()
                SettingsToggleRow(
                    title = "Hide tasks not due soon",
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
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    )
                }
            }

            if (smartVisibility) {
                OutlinePillButton(
                    text = "Reset to defaults",
                    onClick = { viewModel.resetChoreLeadDays() },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Settings › Quick add button: the MENU ORDER card and the REMINDER NAME segmented control. */
@Composable
private fun QuickAddSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val fabOrder = settings?.fabOrder ?: DEFAULT_FAB_ORDER
    val reminderLabel = settings?.reminderLabel ?: ReminderLabelStyle.REMINDERS

    SettingsSubScreenScaffold(title = "Quick add button", onBack = onBack) { innerPadding ->
        SubScreenColumn(innerPadding) {
            SettingsSectionLabel("Menu order")
            SettingsCaption("Drag a handle, or use the arrows, to change the order the + button's menu appears in.")
            SettingsCard {
                FabOrderList(
                    order = fabOrder,
                    reminderLabel = reminderLabel.singular,
                    onReorder = { viewModel.setFabOrder(it) }
                )
            }

            SettingsSectionLabel("Reminder name")
            SettingsCaption("Choose what the reminders feature is called throughout the app.")
            CozySegmented(
                options = ReminderLabelStyle.entries,
                selected = reminderLabel,
                onSelect = { viewModel.setReminderLabel(it) },
                label = { it.displayName },
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun AddMenuOption.iconFor(): ImageVector = when (this) {
    AddMenuOption.TASK -> LucideIcons.CircleCheck
    AddMenuOption.CHORE -> LucideIcons.HouseCheck
    AddMenuOption.REMINDER -> LucideIcons.Bell
}

private fun AddMenuOption.labelFor(reminderLabel: String): String = when (this) {
    AddMenuOption.TASK -> "Task"
    AddMenuOption.CHORE -> "Chore"
    AddMenuOption.REMINDER -> reminderLabel
}

/**
 * Reorderable list of the FAB's three menu items: a drag handle for pointer-based
 * reordering, plus up/down buttons so the order can be changed accessibly (TalkBack,
 * keyboard, switch access) without needing to perform a drag gesture.
 */
@Composable
private fun FabOrderList(
    order: List<AddMenuOption>,
    reminderLabel: String,
    onReorder: (List<AddMenuOption>) -> Unit,
) {
    var displayOrder by remember(order) { mutableStateOf(order) }
    var draggedIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 56.dp.toPx() }

    fun moveItem(from: Int, to: Int) {
        if (from == to || from !in displayOrder.indices || to !in displayOrder.indices) return
        displayOrder = displayOrder.toMutableList().apply { add(to, removeAt(from)) }
    }

    Column {
        displayOrder.forEachIndexed { index, option ->
            val isDragged = index == draggedIndex
            val label = option.labelFor(reminderLabel)
            if (index > 0) SettingsHairline()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragged) dragOffsetY else 0f }
                    .background(
                        if (isDragged) MaterialTheme.colorScheme.surfaceContainerHigh
                        else androidx.compose.ui.graphics.Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    option.iconFor(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                )
                IconButton(
                    onClick = {
                        moveItem(index, index - 1)
                        onReorder(displayOrder)
                    },
                    enabled = index > 0,
                ) {
                    Icon(LucideIcons.ArrowUp, contentDescription = "Move $label up", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = {
                        moveItem(index, index + 1)
                        onReorder(displayOrder)
                    },
                    enabled = index < displayOrder.lastIndex,
                ) {
                    Icon(LucideIcons.ArrowDown, contentDescription = "Move $label down", modifier = Modifier.size(20.dp))
                }
                Icon(
                    LucideIcons.GripVertical,
                    contentDescription = "Drag to reorder $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(12.dp)
                        .pointerInput(index, displayOrder.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                    onReorder(displayOrder)
                                },
                                onDragCancel = {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val from = draggedIndex
                                    if (from != -1) {
                                        val moves = (dragOffsetY / rowHeightPx).roundToInt()
                                        if (moves != 0) {
                                            val to = (from + moves).coerceIn(0, displayOrder.lastIndex)
                                            if (to != from) {
                                                moveItem(from, to)
                                                dragOffsetY -= moves * rowHeightPx
                                                draggedIndex = to
                                            }
                                        }
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}

/**
 * One cadence bucket's lead time control (4a-4): bucket name and example on the
 * left, the circular minus/value/plus stepper on the right. Minus is rose,
 * plus is sage, and a stepper at its limit fades rather than vanishing.
 */
@Composable
private fun LeadTimeStepperRow(
    bucket: CadenceBucket,
    days: Int,
    onDaysChange: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsRowText(
            title = bucket.label,
            subtitle = bucket.example,
            titleSize = 13,
            modifier = Modifier.weight(1f),
        )
        StepperControl(
            valueText = if (days == 1) "1 day" else "$days days",
            onDecrement = { onDaysChange((days - 1).coerceAtLeast(0)) },
            onIncrement = { onDaysChange((days + 1).coerceAtMost(99)) },
            canDecrement = days > 0,
            canIncrement = days < 99,
            decrementDescription = "Show ${bucket.label} chores fewer days before due",
            incrementDescription = "Show ${bucket.label} chores more days before due",
        )
    }
}

/**
 * Settings › Reminders & alerts (handoff 4a-5): NOTIFICATION STYLE segmented
 * control with its caption, then the permission card (Notifications, Exact
 * alarms, Do Not Disturb access) and the summary line.
 */
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
    var fullScreenAllowed by remember { mutableStateOf(PermissionHelper.canUseFullScreenIntent(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmsAllowed = PermissionHelper.canScheduleExactAlarms(context)
                notificationsEnabled = PermissionHelper.areNotificationsEnabled(context)
                fullScreenAllowed = PermissionHelper.canUseFullScreenIntent(context)
                val nowDndGranted = PermissionHelper.isDndAccessGranted(context)
                if (nowDndGranted && !dndAccessGranted) {
                    // Access just granted: recreate channels so the alarm channels are made
                    // with Do Not Disturb bypass. Channel settings are immutable once created
                    // (LESSONS #17), so NotificationHelper does this by switching to a new
                    // channel id rather than mutating the existing no-bypass channel.
                    NotificationHelper.createChannels(context)
                }
                dndAccessGranted = nowDndGranted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val remindersFullyEnabled = notificationsEnabled &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || exactAlarmsAllowed) &&
        dndAccessGranted &&
        fullScreenAllowed

    val deliveryModes = listOf("ALARM", "NOTIFICATION", "SILENT")
    val deliveryModeLabels = mapOf("ALARM" to "Alarm", "NOTIFICATION" to "Notification", "SILENT" to "Silent")

    SettingsSubScreenScaffold(title = "Reminders & alerts", onBack = onBack) { innerPadding ->
        SubScreenColumn(innerPadding) {
            SettingsSectionLabel("Notification style")
            CozySegmented(
                options = deliveryModes,
                selected = currentDeliveryMode,
                onSelect = { viewModel.setDeliveryMode(it) },
                label = { deliveryModeLabels[it] ?: it },
            )
            SettingsCaption(
                when (currentDeliveryMode) {
                    "ALARM" -> "Rings like a clock alarm: turns the screen on, sounds and vibrates until you answer, bypasses Do Not Disturb"
                    "SILENT" -> "No sound or vibration"
                    else -> "Standard notification sound"
                }
            )

            SettingsHairline(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                "If reminders stop arriving, check these permissions.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )

            SettingsCard {
                PermissionRow(
                    title = "Notifications",
                    granted = notificationsEnabled,
                    icon = LucideIcons.Bell,
                    onClick = { context.startActivity(PermissionHelper.notificationSettingsIntent(context)) }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsHairline()
                    PermissionRow(
                        title = "Exact alarms",
                        granted = exactAlarmsAllowed,
                        icon = LucideIcons.Zap,
                        onClick = { context.startActivity(PermissionHelper.exactAlarmSettingsIntent(context)) }
                    )
                }
                SettingsHairline()
                PermissionRow(
                    title = "Do Not Disturb access",
                    subtitle = "Lets alarms sound when Do Not Disturb is on",
                    granted = dndAccessGranted,
                    icon = LucideIcons.BellOff,
                    onClick = { context.startActivity(PermissionHelper.dndAccessSettingsIntent(context)) }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SettingsHairline()
                    PermissionRow(
                        title = "Full-screen alarms",
                        subtitle = "Lets the Alarm style turn the screen on and ring",
                        granted = fullScreenAllowed,
                        icon = LucideIcons.Lamp,
                        onClick = { context.startActivity(PermissionHelper.fullScreenIntentSettingsIntent(context)) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                val tone = if (remindersFullyEnabled) MaterialTheme.colorScheme.secondary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(
                    imageVector = if (remindersFullyEnabled) LucideIcons.CircleCheck else LucideIcons.CircleAlert,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    if (remindersFullyEnabled) "Reminders are fully enabled." else "Reminders are not fully enabled.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = tone,
                )
            }
        }
    }
}

/** Settings › Widget customisation: three segmented controls (Show, Priority, Whose). */
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
    val contentTypeLabels = mapOf("CHORES" to "Chores", "TASKS" to "Tasks", "REMINDERS" to "Reminders")

    val priorityFilters = listOf("ALL", "RED", "AMBER")
    val priorityFilterLabels = mapOf("ALL" to "All", "RED" to "Red", "AMBER" to "Amber")

    val ownerFilters = listOf("EVERYBODY", "MINE")
    val ownerFilterLabels = mapOf("EVERYBODY" to "Everybody's", "MINE" to "Mine")

    SettingsSubScreenScaffold(title = "Widget customisation", onBack = onBack) { innerPadding ->
        SubScreenColumn(innerPadding) {
            SettingsSectionLabel("Show")
            CozySegmented(
                options = contentTypes,
                selected = contentType,
                onSelect = { viewModel.setWidgetContentType(it) },
                label = { contentTypeLabels[it] ?: it },
            )

            SettingsSectionLabel("Priority")
            CozySegmented(
                options = priorityFilters,
                selected = priorityFilter,
                onSelect = { viewModel.setWidgetPriorityFilter(it) },
                label = { priorityFilterLabels[it] ?: it },
            )

            SettingsSectionLabel("Whose")
            CozySegmented(
                options = ownerFilters,
                selected = ownerFilter,
                onSelect = { viewModel.setWidgetOwnerFilter(it) },
                label = { ownerFilterLabels[it] ?: it },
            )
        }
    }
}

/**
 * Settings › About (handoff 4a-6): the sage app tile with the house-check glyph,
 * the serif wordmark, the one-line description, then What's New (tint pill),
 * Open-source licenses (outlined pill) and the version.
 */
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
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.secondary,
                shadowElevation = if (isDarkScheme()) 0.dp else 6.dp,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .size(74.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = LucideIcons.HouseCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Text(
                "choreDash",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "choreDash helps your household share chores and tasks, synced through " +
                    "your own Supabase project.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp),
            )

            SettingsHairline(modifier = Modifier.padding(vertical = 22.dp))

            TintPillButton(text = "What's New", onClick = { showChangelog = true })
            Spacer(Modifier.height(12.dp))
            OutlinePillButton(text = "Open-source licenses", onClick = onNavigateToLicenses)

            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                color = LocalDashTokens.current.inkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
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
 * A permission entry showing its live grant state (4a-5): accent icon, label,
 * and on the right a check with "Allowed" in sage or an alert with "Tap to
 * allow" in the faint ink, so the state is a shape and a word, never colour alone.
 */
@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val statusText = if (granted) "Allowed" else "Tap to allow"
    val statusIcon = if (granted) LucideIcons.CircleCheck else LucideIcons.CircleAlert
    val statusTint = if (granted) MaterialTheme.colorScheme.secondary else LocalDashTokens.current.inkFaint

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$title: $statusText"
            }
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        SettingsRowText(title = title, subtitle = subtitle, titleSize = 15, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(statusIcon, contentDescription = null, tint = statusTint, modifier = Modifier.size(16.dp))
            Text(
                statusText,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold),
                color = statusTint,
            )
        }
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
