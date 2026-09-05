package com.mapgie.dash.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderSortKey
import com.mapgie.dash.data.model.repeats
import com.mapgie.dash.ui.components.AddReminderSheet
import com.mapgie.dash.ui.components.ReminderCard
import com.mapgie.dash.ui.components.core.HeaderIconButton
import com.mapgie.dash.ui.components.core.LocalReminderLabel
import com.mapgie.dash.ui.components.core.PageHeader
import com.mapgie.dash.ui.components.core.SearchRow
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.components.core.SortPill
import com.mapgie.dash.ui.components.core.SortSheet
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.PillShape

/**
 * The Memos tab (handoff 9a), on the same chrome as Chores and Tasks: the serif
 * header with the search action, the filter-chip row (Active · N / Done / All,
 * Active by default) ending in the sort pill, and a flat list of shared-format
 * cards. There is no Archived tab; archiving lives in the edit sheet and archived
 * memos show, muted, under All.
 *
 * The owner ("mine / all") header action the design shows is not built: memos
 * carry no owner, so there is nothing to filter by.
 */
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
    var showSortSheet by remember { mutableStateOf(false) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
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
    val plural = labelStyle.displayName.lowercase()
    val accent = LocalTypeAccents.current.onReminderContainer

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PageHeader(
                title = labelStyle.displayName,
                accent = accent,
                actions = {
                    HeaderIconButton(
                        icon = LucideIcons.Search,
                        contentDescription = if (searchActive) "Close search" else "Search $plural",
                        onClick = {
                            searchActive = !searchActive
                            if (!searchActive) searchQuery = ""
                        },
                        active = searchActive,
                        activeTint = accent,
                    )
                },
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

            if (searchActive) {
                SearchRow(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onCancel = { searchActive = false; searchQuery = "" },
                    placeholder = "Search $plural",
                )
                val query = searchQuery.trim()
                val results = if (query.isEmpty()) emptyList() else
                    uiState.all.filter { it.subject.contains(query, ignoreCase = true) }
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
                    modifier = Modifier.weight(1f)
                ) {
                    item(key = "search_count") {
                        SectionLabel(
                            text = "in $plural · ${results.size}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                    items(results, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            linkedTo = uiState.linkedTo(reminder),
                            onClick = { editTarget = reminder },
                            onToggleDone = { viewModel.markDone(reminder.id) },
                            highlightQuery = query,
                        )
                    }
                }
                return@Scaffold
            }

            // Filter chips, then the sort pill pinned to the right.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReminderFilter.entries.forEach { f ->
                    val selected = uiState.filter == f
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setFilter(f) },
                        label = {
                            Text(
                                text = if (f == ReminderFilter.ACTIVE && uiState.activeCount > 0)
                                    "${f.label} · ${uiState.activeCount}" else f.label,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        },
                        shape = PillShape,
                        border = null,
                        // Explicit high-contrast fills (LESSONS.md #3).
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                SortPill(order = uiState.sort, onClick = { showSortSheet = true })
            }

            val displayed = uiState.displayed
            if (displayed.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (uiState.filter) {
                            ReminderFilter.ACTIVE -> if (uiState.reminders.isEmpty()) "No $plural. Tap + to add one."
                                                     else "Nothing active. Tap + to add one."
                            ReminderFilter.DONE -> "Nothing done yet."
                            ReminderFilter.ALL -> "No $plural. Tap + to add one."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp)
                    )
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.cardGap)
            ) {
                items(displayed, key = { it.id }) { reminder ->
                    SwipeToCompleteReminderCard(
                        reminder = reminder,
                        linkedTo = uiState.linkedTo(reminder),
                        onClick = { editTarget = reminder },
                        onToggleDone = {
                            if (!reminder.repeats && reminder.completedAt != null) viewModel.markUndone(reminder.id)
                            else viewModel.markDone(reminder.id)
                        },
                        onDelete = { viewModel.deleteReminder(reminder.id) }
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        SortSheet(
            title = "Sort $plural by",
            keys = ReminderSortKey.entries,
            order = uiState.sort,
            onOrderChange = { viewModel.setSort(it) },
            onDismiss = { showSortSheet = false },
        )
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
    linkedTo: String?,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = !reminder.repeats && reminder.completedAt != null
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
                            .padding(horizontal = Dimens.cardInset)
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
                            .padding(horizontal = Dimens.cardInset)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            if (isDone) "Undo" else "Done",
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
            linkedTo = linkedTo,
            onClick = onClick,
            onToggleDone = onToggleDone,
        )
    }

    if (showDeleteConfirm) {
        val featureWord = LocalReminderLabel.current.singular.lowercase()
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete $featureWord?") },
            text = { Text("This $featureWord will be permanently removed.") },
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
