package com.mapgie.dash.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderLabelStyle
import com.mapgie.dash.ui.components.AddReminderSheet
import com.mapgie.dash.ui.components.ReminderCard
import com.mapgie.dash.ui.components.core.PageHeader
import com.mapgie.dash.ui.theme.LocalTypeAccents

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
    var archivedExpanded by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<ReminderDto?>(null) }

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

    val labelStyle = uiState.reminderLabel

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PageHeader(
                title = labelStyle.displayName,
                accent = LocalTypeAccents.current.onReminderContainer,
            )
            if (uiState.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            val active = uiState.active
            val done = uiState.done
            val archived = uiState.archived

            if (active.isEmpty() && done.isEmpty() && archived.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No ${labelStyle.displayName.lowercase()}. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp)
                    )
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(active, key = { it.id }) { reminder ->
                    SwipeToCompleteReminderCard(
                        reminder = reminder,
                        linkedLabel = uiState.linkedLabel(reminder),
                        onClick = { editTarget = reminder },
                        onToggleDone = { viewModel.markDone(reminder.id) },
                        onDelete = { viewModel.deleteReminder(reminder.id) }
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
                            SwipeToCompleteReminderCard(
                                reminder = reminder,
                                linkedLabel = uiState.linkedLabel(reminder),
                                onClick = { editTarget = reminder },
                                onToggleDone = {
                                    if (reminder.completedAt == null) viewModel.markDone(reminder.id)
                                    else viewModel.markUndone(reminder.id)
                                },
                                onDelete = { viewModel.deleteReminder(reminder.id) }
                            )
                        }
                    }
                }

                if (archived.isNotEmpty()) {
                    item(key = "archived_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Archived (${archived.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { archivedExpanded = !archivedExpanded }) {
                                Icon(
                                    if (archivedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (archivedExpanded) "Collapse" else "Expand"
                                )
                            }
                        }
                    }
                    if (archivedExpanded) {
                        items(archived, key = { "archived_${it.id}" }) { reminder ->
                            SwipeToCompleteReminderCard(
                                reminder = reminder,
                                linkedLabel = uiState.linkedLabel(reminder),
                                onClick = { editTarget = reminder },
                                onToggleDone = {
                                    if (reminder.completedAt == null) viewModel.markDone(reminder.id)
                                    else viewModel.markUndone(reminder.id)
                                },
                                onDelete = { viewModel.deleteReminder(reminder.id) }
                            )
                        }
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

    editTarget?.let { reminder ->
        AddReminderSheet(
            chores = uiState.chores,
            tasks = uiState.tasks,
            existing = reminder,
            onSave = { insert -> viewModel.editReminder(reminder.id, insert) },
            onArchiveToggle = { archived -> viewModel.archiveReminder(reminder.id, archived) },
            onDelete = { viewModel.deleteReminder(reminder.id) },
            onDismiss = { editTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToCompleteReminderCard(
    reminder: ReminderDto,
    linkedLabel: String?,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = reminder.completedAt != null
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onToggleDone()
                SwipeToDismissBoxValue.EndToStart -> showDeleteConfirm = true
                SwipeToDismissBoxValue.Settled -> {}
            }
            false // never actually dismiss the item
        },
        positionalThreshold = { it * 0.3f }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            "Delete",
                            modifier = Modifier.padding(end = 24.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            if (isDone) "Undo" else "Done✔",
                            modifier = Modifier.padding(start = 24.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    ) {
        ReminderCard(
            reminder = reminder,
            linkedLabel = linkedLabel,
            onClick = onClick,
            onToggleDone = onToggleDone,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete reminder?") },
            text = { Text("This reminder will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
