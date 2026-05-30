package com.mapgie.dash.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mapgie.dash.ui.screens.chores.ChoreListScreen
import com.mapgie.dash.ui.screens.licenses.LicensesScreen
import com.mapgie.dash.ui.screens.settings.SettingsScreen
import com.mapgie.dash.ui.screens.tasks.TaskListScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Chores : Screen("chores", "Chores", Icons.Filled.CleaningServices)
    object Tasks : Screen("tasks", "Tasks", Icons.Filled.CheckCircle)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

private val navItems = listOf(Screen.Chores, Screen.Tasks, Screen.Settings)

@Composable
fun DashNavGraph(
    pendingNfcTagId: String?,
    onNfcConsumed: () -> Unit,
    startOnSettings: Boolean = false
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
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
            startDestination = if (startOnSettings) Screen.Settings.route else Screen.Chores.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Chores.route) {
                ChoreListScreen(
                    pendingNfcTagId = pendingNfcTagId,
                    onNfcConsumed = onNfcConsumed
                )
            }
            composable(Screen.Tasks.route) {
                TaskListScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToLicenses = { navController.navigate("licenses") }
                )
            }
            composable("licenses") {
                LicensesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}