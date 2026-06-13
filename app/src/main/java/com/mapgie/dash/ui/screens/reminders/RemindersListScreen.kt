package com.mapgie.dash.ui.screens.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.ui.components.AddMenuOption
import com.mapgie.dash.ui.components.AddReminderSheet
import com.mapgie.dash.ui.components.ReminderCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersListScreen(
    pendingAddIntent: AddMenuOption?,
    onPendingAddIntentConsumed: () -> Unit,
    viewModel: RemindersListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var showAddSheet by remember { mutableStateOf(false) }
    var doneExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(pendingAddIntent) {
        if (pendingAddIntent == AddMenuOption.REMINDER) {
            showAddSheet = true
            onPendingAddIntentConsumed()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(title = { Text("Reminders") })
        }
    ) { innerPadding ->
        if (uiState.loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val active = uiState.active
        val done = uiState.done

        if (active.isEmpty() && done.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No reminders. Tap + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(active, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    linkedLabel = uiState.linkedLabel(reminder),
                    onToggleDone = { viewModel.markDone(reminder.id) }
                )
            }

            if (done.isNotEmpty()) {
                item(key = "done_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Done (${done.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { doneExpanded = !doneExpanded }) {
                            Icon(
                                if (doneExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (doneExpanded) "Collapse" else "Expand"
                            )
                        }
                    }
                }
                if (doneExpanded) {
                    items(done, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            linkedLabel = uiState.linkedLabel(reminder),
                            onToggleDone = { viewModel.markUndone(reminder.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddReminderSheet(
            chores = uiState.chores,
            tasks = uiState.tasks,
            onSave = { insert -> viewModel.addReminder(insert) },
            onDismiss = { showAddSheet = false }
        )
    }
}
