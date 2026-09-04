package com.mapgie.dash.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.nfc.NfcWriteResult
import com.mapgie.dash.ui.components.AddMenuButton
import com.mapgie.dash.ui.components.SpeedDialOverlay
import com.mapgie.dash.ui.components.WelcomeSheet
import com.mapgie.dash.ui.screens.chores.ChoreListScreen
import com.mapgie.dash.ui.screens.licenses.LicensesScreen
import com.mapgie.dash.ui.screens.reminder.REMINDER_VIEW_ARG_ID
import com.mapgie.dash.ui.screens.reminder.REMINDER_VIEW_ARG_KIND
import com.mapgie.dash.ui.screens.reminder.REMINDER_VIEW_ROUTE
import com.mapgie.dash.ui.screens.reminder.ReminderViewKind
import com.mapgie.dash.ui.screens.reminder.ReminderViewScreen
import com.mapgie.dash.ui.screens.reminder.reminderViewRoute
import com.mapgie.dash.ui.screens.reminders.RemindersListScreen
import com.mapgie.dash.ui.screens.settings.SettingsScreen
import com.mapgie.dash.ui.screens.tasks.TaskListScreen
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.TypeAccentColors
import com.mapgie.dash.widget.WIDGET_DEST_CHORES
import com.mapgie.dash.widget.WIDGET_DEST_QUICK_ADD_CHORE
import com.mapgie.dash.widget.WIDGET_DEST_QUICK_ADD_REMINDER
import com.mapgie.dash.widget.WIDGET_DEST_QUICK_ADD_TASK
import com.mapgie.dash.widget.WIDGET_DEST_REMINDERS
import com.mapgie.dash.widget.WIDGET_DEST_SETTINGS
import com.mapgie.dash.widget.WIDGET_DEST_TASKS

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Chores : Screen("chores", "Chores", LucideIcons.HouseCheck)
    object Tasks : Screen("tasks", "Tasks", LucideIcons.CircleCheck)
    object Reminders : Screen("reminders", "Reminders", LucideIcons.Bell)
    object Settings : Screen("settings", "Settings", LucideIcons.Settings)
}

// The full set of tabs, independent of which ones are currently visible in the
// bottom bar (e.g. the FAB must still work when reached on a hidden tab via a
// widget deep link).
private val allNavItems = listOf(Screen.Tasks, Screen.Chores, Screen.Reminders, Screen.Settings)

private val Screen.addMenuOption: AddMenuOption?
    get() = when (this) {
        Screen.Chores -> AddMenuOption.CHORE
        Screen.Tasks -> AddMenuOption.TASK
        Screen.Reminders -> AddMenuOption.REMINDER
        else -> null
    }

// Gives each content-type tab its own colour tone (indicator pill + selected
// icon/text), on top of the icon and label that already distinguish them. The
// tones come from [TypeAccentColors] so the custom theme can map them onto the
// user's picks. Settings has no type accent and falls back to the sage secondary.
private fun Screen.accentColors(accents: TypeAccentColors): Pair<Color, Color>? =
    when (this) {
        Screen.Tasks -> accents.taskContainer to accents.onTaskContainer
        Screen.Chores -> accents.choreContainer to accents.onChoreContainer
        Screen.Reminders -> accents.reminderContainer to accents.onReminderContainer
        else -> null
    }

@Composable
fun DashNavGraph(
    pendingNfcTagId: String?,
    onNfcConsumed: () -> Unit,
    pendingWidgetDestination: String? = null,
    onWidgetDestinationConsumed: () -> Unit = {},
    pendingReminderView: Pair<String, String>? = null,
    onReminderViewConsumed: () -> Unit = {},
    nfcWriteRequest: String?,
    nfcWriteResult: NfcWriteResult?,
    onStartNfcWrite: (String) -> Unit,
    onCancelNfcWrite: () -> Unit,
    onNfcWriteResultConsumed: () -> Unit,
    startOnSettings: Boolean = false,
    navViewModel: DashNavViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var fabExpanded by remember { mutableStateOf(false) }
    var pendingAddIntent by remember { mutableStateOf<AddMenuOption?>(null) }

    val navUiState by navViewModel.uiState.collectAsStateWithLifecycle()
    // The Memos/Reminders slot is always present, so the five-slot bar never
    // reshapes under the thumb (handoff: fixed Tasks · Chores · + · Memos · Settings).
    val navItems = allNavItems

    fun navigateTo(route: String) {
        navController.navigate(route) {
            // Mirror GaMeD LESSONS.md lesson #4: all programmatic tab nav
            // must use the same popUpTo + saveState + restoreState pattern
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(pendingWidgetDestination) {
        val targetRoute = when (pendingWidgetDestination) {
            WIDGET_DEST_QUICK_ADD_TASK -> {
                pendingAddIntent = AddMenuOption.TASK
                Screen.Tasks.route
            }
            WIDGET_DEST_QUICK_ADD_CHORE -> {
                pendingAddIntent = AddMenuOption.CHORE
                Screen.Chores.route
            }
            WIDGET_DEST_QUICK_ADD_REMINDER -> {
                pendingAddIntent = AddMenuOption.REMINDER
                Screen.Reminders.route
            }
            WIDGET_DEST_TASKS -> Screen.Tasks.route
            WIDGET_DEST_CHORES -> Screen.Chores.route
            WIDGET_DEST_REMINDERS -> Screen.Reminders.route
            WIDGET_DEST_SETTINGS -> Screen.Settings.route
            else -> null
        }
        if (targetRoute != null) {
            navigateTo(targetRoute)
            onWidgetDestinationConsumed()
        }
    }

    // Opened from a reminder notification: (kind, id) per the "reminder/{kind}/{id}"
    // route. A plain navigate (not the tab helper) so it stacks on top of whatever
    // tab is showing and Done / Snooze / back simply pop it away.
    LaunchedEffect(pendingReminderView) {
        val (kindArg, id) = pendingReminderView ?: return@LaunchedEffect
        val kind = ReminderViewKind.fromRouteArg(kindArg)
        if (kind != null && id.isNotBlank()) {
            navController.navigate(reminderViewRoute(kind, id)) { launchSingleTop = true }
        }
        onReminderViewConsumed()
    }

    val activeScreen = allNavItems.firstOrNull { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }
    val showFab = activeScreen?.addMenuOption != null

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                val typeAccents = LocalTypeAccents.current
                val tabs = navItems.map { screen ->
                    val accent = screen.accentColors(typeAccents)
                    DashTab(
                        label = if (screen == Screen.Reminders) {
                            navUiState.reminderLabel.displayName
                        } else {
                            screen.label
                        },
                        icon = screen.icon,
                        selected = currentDestination?.hierarchy
                            ?.any { it.route == screen.route } == true,
                        activeContainer = accent?.first ?: MaterialTheme.colorScheme.secondaryContainer,
                        activeContent = accent?.second ?: MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { navigateTo(screen.route) },
                    )
                }
                DashBottomBar(
                    tabs = tabs,
                    centerContent = {
                        if (showFab) {
                            AddMenuButton(
                                expanded = fabExpanded,
                                // Short press: new item for the page you're on.
                                onClick = { activeScreen?.addMenuOption?.let { pendingAddIntent = it } },
                                // Long press: open the radial to pick any type.
                                onLongClick = { fabExpanded = true },
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            // The tab screens have Scaffolds of their own (for snackbars). Consume
            // the system-bar insets here once, or each of them pads for the status
            // and navigation bars a second time and the header floats 30dp below
            // the strip with a matching dead band above the bottom bar.
            NavHost(
                navController = navController,
                startDestination = if (startOnSettings) Screen.Settings.route else Screen.Tasks.route,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                composable(Screen.Chores.route) {
                    ChoreListScreen(
                        pendingNfcTagId = pendingNfcTagId,
                        onNfcConsumed = onNfcConsumed,
                        nfcWriteRequest = nfcWriteRequest,
                        nfcWriteResult = nfcWriteResult,
                        onStartNfcWrite = onStartNfcWrite,
                        onCancelNfcWrite = onCancelNfcWrite,
                        onNfcWriteResultConsumed = onNfcWriteResultConsumed,
                        pendingAddIntent = pendingAddIntent,
                        onPendingAddIntentConsumed = { pendingAddIntent = null }
                    )
                }
                composable(Screen.Tasks.route) {
                    TaskListScreen(
                        pendingAddIntent = pendingAddIntent,
                        onPendingAddIntentConsumed = { pendingAddIntent = null }
                    )
                }
                composable(Screen.Reminders.route) {
                    RemindersListScreen(
                        pendingAddIntent = pendingAddIntent,
                        onPendingAddIntentConsumed = { pendingAddIntent = null }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateToLicenses = { navController.navigate("licenses") },
                    )
                }
                composable("licenses") {
                    LicensesScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = REMINDER_VIEW_ROUTE,
                    arguments = listOf(
                        navArgument(REMINDER_VIEW_ARG_KIND) { type = NavType.StringType },
                        navArgument(REMINDER_VIEW_ARG_ID) { type = NavType.StringType },
                    ),
                ) {
                    ReminderViewScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        // First run: the chores / tasks / memos explanation, shown once.
        if (navUiState.showWelcome) {
            WelcomeSheet(
                reminderLabel = navUiState.reminderLabel.displayName,
                onDismiss = { navViewModel.markWelcomeSeen() },
            )
        }

        // The speed dial covers the whole screen, bar included, so it sits
        // outside the Scaffold. Picking an item opens the matching sheet in
        // "new" mode on its own tab.
        if (showFab) {
            SpeedDialOverlay(
                expanded = fabExpanded,
                onExpandedChange = { fabExpanded = it },
                order = navUiState.fabOrder,
                activeOption = activeScreen?.addMenuOption,
                reminderLabel = navUiState.reminderLabel.singular.lowercase(),
                onSelect = { option ->
                    pendingAddIntent = option
                    val targetRoute = when (option) {
                        AddMenuOption.CHORE -> Screen.Chores.route
                        AddMenuOption.TASK -> Screen.Tasks.route
                        AddMenuOption.REMINDER -> Screen.Reminders.route
                    }
                    navigateTo(targetRoute)
                }
            )
        }
    }
}
