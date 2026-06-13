package com.mapgie.dash.ui.screens.chores

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.ui.components.ChoreCard
import com.mapgie.dash.ui.components.EditChoreSheet
import com.mapgie.dash.ui.components.LogBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChoreListScreen(
    pendingNfcTagId: String?,
    onNfcConsumed: () -> Unit,
    viewModel: ChoreListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // SheetState hoisted above the composables that use them (GoFlo LESSONS.md)
    val logSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var logTargetChore by remember { mutableStateOf<Chore?>(null) }
    var editTargetChore by remember { mutableStateOf<Chore?>(null) }
    var showLogSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showArchivedSection by remember { mutableStateOf(false) }

    // Handle incoming NFC tag from MainActivity
    LaunchedEffect(pendingNfcTagId, uiState.active.size) {
        val tagId = pendingNfcTagId ?: return@LaunchedEffect
        val chore = uiState.active.find { it.tagId == tagId }
        when {
            chore != null -> {
                logTargetChore = chore
                showLogSheet = true
                viewModel.setPendingNfcTag(tagId)
            }
            uiState.active.isNotEmpty() || uiState.archived.isNotEmpty() -> {
                snackbarHostState.showSnackbar("Unknown tag — add it in the web app")
                onNfcConsumed()
            }
            // else: data still loading; LaunchedEffect will re-run when list populates
        }
    }

    // Show snackbar after a successful log, with Undo action
    LaunchedEffect(uiState.recentScan) {
        val scan = uiState.recentScan ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "${scan.choreLabel} logged",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoLog(scan.scanId)
        }
        viewModel.clearRecentScan()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.loading,
            onRefresh = { viewModel.load() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter chips + toggles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChoreFilter.values().forEach { f ->
                        FilterChip(
                            selected = uiState.filter == f,
                            onClick = { viewModel.setFilter(f) },
                            label = {
                                Text(
                                    when (f) {
                                        ChoreFilter.ALL -> "All"
                                        ChoreFilter.OVERDUE -> "Overdue"
                                        ChoreFilter.SOON -> "Soon"
                                    }
                                )
                            },
                            // High-contrast fill so selected state reads in 100ms (GoFlo LESSONS.md)
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.setGroupBy(!uiState.groupByCategory) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.GridView,
                            contentDescription = if (uiState.groupByCategory)
                                "Ungroup categories" else "Group by category",
                            tint = if (uiState.groupByCategory)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.setOwnerFilter(
                                if (uiState.ownerFilter == OwnerFilter.ME) OwnerFilter.ALL
                                else OwnerFilter.ME
                            )
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = if (uiState.ownerFilter == OwnerFilter.ME)
                                "Show all owners" else "Show my chores",
                            tint = if (uiState.ownerFilter == OwnerFilter.ME)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                when {
                    uiState.error != null -> ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.load() }
                    )

                    !uiState.loading && uiState.active.isEmpty() -> EmptyState(
                        message = if (uiState.owners.isEmpty())
                            "Configure Supabase credentials in Settings to get started"
                        else "No chores found. Add NFC tags in the web app."
                    )

                    else -> {
                        val displayed = uiState.displayed
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            if (uiState.groupByCategory) {
                                val grouped = displayed.groupBy { it.category ?: "Uncategorised" }
                                grouped.forEach { (category, chores) ->
                                    stickyHeader {
                                        Text(
                                            text = category.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.background)
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                    items(chores, key = { it.id }) { chore ->
                                        SwipeToLogCard(
                                            chore = chore,
                                            showOwner = uiState.ownerFilter == OwnerFilter.ALL,
                                            onTap = { logTargetChore = it; showLogSheet = true },
                                            onLongPress = { editTargetChore = it; showEditSheet = true },
                                            onSwipeLog = { viewModel.logChore(it.tagId) }
                                        )
                                    }
                                }
                            } else {
                                items(displayed, key = { it.id }) { chore ->
                                    SwipeToLogCard(
                                        chore = chore,
                                        showOwner = uiState.ownerFilter == OwnerFilter.ALL,
                                        onTap = { logTargetChore = it; showLogSheet = true },
                                        onLongPress = { editTargetChore = it; showEditSheet = true },
                                        onSwipeLog = { viewModel.logChore(it.tagId) }
                                    )
                                }
                            }

                            if (uiState.archived.isNotEmpty()) {
                                item {
                                    TextButton(
                                        onClick = { showArchivedSection = !showArchivedSection },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            if (showArchivedSection)
                                                "Hide archived (${uiState.archived.size})"
                                            else
                                                "Show archived (${uiState.archived.size})"
                                        )
                                    }
                                }
                                if (showArchivedSection) {
                                    items(
                                        uiState.archived,
                                        key = { "archived_${it.id}" }
                                    ) { chore ->
                                        ChoreCard(
                                            chore = chore,
                                            showOwner = uiState.ownerFilter == OwnerFilter.ALL,
                                            modifier = Modifier
                                                .semantics { role = Role.Button }
                                                .combinedClickable(
                                                    onClick = {},
                                                    onLongClick = {
                                                        editTargetChore = chore
                                                        showEditSheet = true
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogSheet && logTargetChore != null) {
        LogBottomSheet(
            chore = logTargetChore!!,
            sheetState = logSheetState,
            onConfirm = { chore, at ->
                viewModel.logChore(chore.tagId, at)
                showLogSheet = false
                if (uiState.pendingNfcTagId != null) {
                    viewModel.clearPendingNfcTag()
                    onNfcConsumed()
                }
            },
            onDismiss = {
                showLogSheet = false
                if (uiState.pendingNfcTagId != null) {
                    viewModel.clearPendingNfcTag()
                    onNfcConsumed()
                }
            }
        )
    }

    if (showEditSheet && editTargetChore != null) {
        EditChoreSheet(
            chore = editTargetChore!!,
            owners = uiState.owners,
            sheetState = editSheetState,
            onSave = { tagId, label, owner, intervalDays ->
                viewModel.updateChore(tagId, label, owner, intervalDays)
                showEditSheet = false
            },
            onArchiveToggle = { chore, archive ->
                viewModel.archiveChore(chore.tagId, archive)
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeToLogCard(
    chore: Chore,
    showOwner: Boolean,
    onTap: (Chore) -> Unit,
    onLongPress: (Chore) -> Unit,
    onSwipeLog: (Chore) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onSwipeLog(chore)
            }
            false // never actually dismiss the item
        },
        positionalThreshold = { it * 0.3f }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Log✔",
                    modifier = Modifier.padding(start = 24.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) {
        ChoreCard(
            chore = chore,
            showOwner = showOwner,
            modifier = Modifier
                .semantics { role = Role.Button }
                .combinedClickable(
                    onClick = { onTap(chore) },
                    onLongClick = { onLongPress(chore) }
                )
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp)
        )
    }
}