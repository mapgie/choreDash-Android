package com.mapgie.dash.ui.screens.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.ui.components.AddReminderSheet
import com.mapgie.dash.ui.components.ReminderCard
import com.mapgie.dash.ui.components.core.CollapsibleSectionHeader
import com.mapgie.dash.ui.components.core.DashEmptyState
import com.mapgie.dash.ui.components.core.DashLoadingState
import com.mapgie.dash.ui.components.core.DashScreenHeader
import com.mapgie.dash.ui.components.core.SwipeAction
import com.mapgie.dash.ui.components.core.SwipeActionRow
import com.mapgie.dash.ui.components.core.SwipeTone
import com.mapgie.dash.ui.theme.Dimens
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
    val accents = LocalTypeAccents.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Same thin identity strip as the other tabs, in the Memos accent.
            DashScreenHeader(
                title = labelStyle.displayName,
                containerColor = accents.reminderContainer,
                contentColor = accents.onReminderContainer,
            )

            val active = uiState.active
            val done = uiState.done
            val archived = uiState.archived

            when {
                uiState.loading -> DashLoadingState()

                active.isEmpty() && done.isEmpty() && archived.isEmpty() -> DashEmptyState(
                    "No ${labelStyle.displayName.lowercase()}. Tap + to add one."
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.cardGap)
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
                            CollapsibleSectionHeader(
                                title = "Done",
                                count = done.size,
                                expanded = doneExpanded,
                                onToggle = { doneExpanded = !doneExpanded }
                            )
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
                            CollapsibleSectionHeader(
                                title = "Archived",
                                count = archived.size,
                                expanded = archivedExpanded,
                                onToggle = { archivedExpanded = !archivedExpanded }
                            )
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

    SwipeActionRow(
        startAction = SwipeAction(
            label = if (isDone) "Undo" else "Done✔",
            tone = SwipeTone.POSITIVE,
            onSwipe = onToggleDone
        ),
        endAction = SwipeAction(
            label = "Delete",
            tone = SwipeTone.DESTRUCTIVE,
            onSwipe = { showDeleteConfirm = true }
        )
    ) {
        ReminderCard(
            reminder = reminder,
            linkedLabel = linkedLabel,
            onClick = onClick,
            onToggleDone = onToggleDone,
            modifier = Modifier.padding(horizontal = Dimens.cardInset)
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
