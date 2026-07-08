package com.mapgie.dash.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mapgie.dash.nfc.NfcWriteResult
import com.mapgie.dash.ui.components.AddMenuFab
import com.mapgie.dash.ui.components.AddMenuOption
import com.mapgie.dash.ui.screens.chores.ChoreListScreen
import com.mapgie.dash.ui.screens.licenses.LicensesScreen
import com.mapgie.dash.ui.screens.reminders.RemindersListScreen
import com.mapgie.dash.ui.screens.settings.SettingsScreen
import com.mapgie.dash.ui.screens.tasks.TaskListScreen
import com.mapgie.dash.widget.WIDGET_DEST_CHORES
import com.mapgie.dash.widget.WIDGET_DEST_QUICK_ADD_CHORE
import com.mapgie.dash.widget.WIDGET_DEST_QUICK_ADD_REMINDER
import com.mapgie.dash.widget.WIDGET_DEST_QUICK_ADD_TASK
import com.mapgie.dash.widget.WIDGET_DEST_REMINDERS
import com.mapgie.dash.widget.WIDGET_DEST_TASKS

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Chores : Screen("chores", "Chores", Icons.Filled.CleaningServices)
    object Tasks : Screen("tasks", "Tasks", Icons.Filled.CheckCircle)
    object Reminders : Screen("reminders", "Reminders", Icons.Filled.Notifications)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

private val navItems = listOf(Screen.Tasks, Screen.Chores, Screen.Reminders, Screen.Settings)

private val Screen.addMenuOption: AddMenuOption?
    get() = when (this) {
        Screen.Chores -> AddMenuOption.CHORE
        Screen.Tasks -> AddMenuOption.TASK
        Screen.Reminders -> AddMenuOption.REMINDER
        else -> null
    }

@Composable
fun DashNavGraph(
    pendingNfcTagId: String?,
    onNfcConsumed: () -> Unit,
    pendingWidgetDestination: String? = null,
    onWidgetDestinationConsumed: () -> Unit = {},
    nfcWriteRequest: String?,
    nfcWriteResult: NfcWriteResult?,
    onStartNfcWrite: (String) -> Unit,
    onCancelNfcWrite: () -> Unit,
    onNfcWriteResultConsumed: () -> Unit,
    startOnSettings: Boolean = false
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var fabExpanded by remember { mutableStateOf(false) }
    var pendingAddIntent by remember { mutableStateOf<AddMenuOption?>(null) }

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
            else -> null
        }
        if (targetRoute != null) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onWidgetDestinationConsumed()
        }
    }

    val showFab = navItems
        .filter { it.addMenuOption != null }
        .any { screen -> currentDestination?.hierarchy?.any { it.route == screen.route } == true }

    Scaffold(
        floatingActionButton = {
            if (showFab) {
                AddMenuFab(
                    expanded = fabExpanded,
                    onExpandedChange = { fabExpanded = it },
                    onSelect = { option ->
                        pendingAddIntent = option
                        val targetRoute = when (option) {
                            AddMenuOption.CHORE -> Screen.Chores.route
                            AddMenuOption.TASK -> Screen.Tasks.route
                            AddMenuOption.REMINDER -> Screen.Reminders.route
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                navItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(screen.icon, contentDescription = screen.label)
                        },
                        label = {
                            Text(screen.label, style = MaterialTheme.typography.labelMedium)
                        },
                        selected = selected,
                        // Mirror GaMeD LESSONS.md lesson #4: all programmatic tab nav
                        // must use the same popUpTo + saveState + restoreState pattern
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startOnSettings) Screen.Settings.route else Screen.Tasks.route,
            modifier = Modifier.padding(innerPadding)
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
        }
    }
}