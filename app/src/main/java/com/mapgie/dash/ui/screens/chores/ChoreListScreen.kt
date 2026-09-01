package com.mapgie.dash.ui.screens.chores

import android.nfc.NfcAdapter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.nfc.NfcWriteResult
import com.mapgie.dash.ui.components.AddChoreSheet
import com.mapgie.dash.ui.components.AddReminderSheet
import com.mapgie.dash.ui.components.ChoreCard
import com.mapgie.dash.ui.components.ChoreOverviewSheet
import com.mapgie.dash.ui.components.EditChoreSheet
import com.mapgie.dash.ui.components.NfcHintCard
import com.mapgie.dash.ui.components.PinWidgetChooserDialog
import com.mapgie.dash.ui.components.WriteTagDialog
import com.mapgie.dash.ui.components.core.HeaderIconButton
import com.mapgie.dash.ui.components.core.PageHeader
import com.mapgie.dash.ui.components.core.OwnerFilterButton
import com.mapgie.dash.ui.components.core.SearchRow
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.theme.DashIcons
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.PillShape
import com.mapgie.dash.util.formatAbsoluteDate
import java.time.Instant
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChoreListScreen(
    pendingNfcTagId: String?,
    onNfcConsumed: () -> Unit,
    nfcWriteRequest: String?,
    nfcWriteResult: NfcWriteResult?,
    onStartNfcWrite: (String) -> Unit,
    onCancelNfcWrite: () -> Unit,
    onNfcWriteResultConsumed: () -> Unit,
    pendingAddIntent: AddMenuOption? = null,
    onPendingAddIntentConsumed: () -> Unit = {},
    viewModel: ChoreListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // SheetState hoisted above the composables that use them (GoFlo LESSONS.md)
    val logSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var logTargetChore by remember { mutableStateOf<Chore?>(null) }
    var editTargetChore by remember { mutableStateOf<Chore?>(null) }
    var showLogSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showArchivedSection by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var addSheetTagId by remember { mutableStateOf("") }
    var reminderTargetChore by remember { mutableStateOf<Chore?>(null) }
    val context = LocalContext.current
    val hasNfc = remember { NfcAdapter.getDefaultAdapter(context) != null }

    // Handle incoming NFC tag from MainActivity
    LaunchedEffect(pendingNfcTagId, uiState.active.size) {
        val tagId = pendingNfcTagId ?: return@LaunchedEffect
        val chore = (uiState.active + uiState.archived).find { it.tagId == tagId }
        when {
            chore != null -> {
                logTargetChore = chore
                showLogSheet = true
                viewModel.setPendingNfcTag(tagId)
            }
            uiState.active.isNotEmpty() || uiState.archived.isNotEmpty() || !uiState.loading -> {
                addSheetTagId = tagId
                showAddSheet = true
                viewModel.setPendingNfcTag(tagId)
            }
            // else: data still loading; LaunchedEffect will re-run when list populates
        }
    }

    LaunchedEffect(pendingAddIntent) {
        if (pendingAddIntent == AddMenuOption.CHORE) {
            addSheetTagId = ""
            showAddSheet = true
            onPendingAddIntentConsumed()
        }
    }

    // Keep the overview sheet's chore data fresh after actions like "Remove last log"
    LaunchedEffect(uiState.active, uiState.archived) {
        val current = logTargetChore ?: return@LaunchedEffect
        (uiState.active + uiState.archived).find { it.id == current.id }?.let { fresh ->
            if (fresh != current) logTargetChore = fresh
        }
    }

    // Load recent scan history whenever the overview sheet is opened for a chore
    LaunchedEffect(showLogSheet, logTargetChore) {
        val chore = logTargetChore
        if (showLogSheet && chore != null) {
            viewModel.loadScanHistory(chore.tagId)
        } else {
            viewModel.clearScanHistory()
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

    // Show snackbar after a swipe-to-snooze (or wake), with Undo action
    LaunchedEffect(uiState.recentSnooze) {
        val snooze = uiState.recentSnooze ?: return@LaunchedEffect
        val message = snooze.until?.let { "${snooze.choreLabel} snoozed until ${formatAbsoluteDate(it)}" }
            ?: "${snooze.choreLabel} is back"
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoSnooze(snooze)
        }
        viewModel.clearRecentSnooze()
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
                PageHeader(
                    title = if (uiState.zenMode) "zen" else "chores",
                    accent = LocalTypeAccents.current.onChoreContainer,
                    actions = {
                        // Design order: owner filter, due countdown, zen, search, group/flat.
                        if (!uiState.zenMode) {
                            if (uiState.ownerHandle.isNotBlank()) {
                                OwnerFilterButton(
                                    filter = uiState.ownerFilter,
                                    onFilterChange = viewModel::setOwnerFilter,
                                )
                            }
                            HeaderIconButton(
                                icon = Icons.Outlined.Timer,
                                contentDescription = if (uiState.showDueCountdown)
                                    "Hide due countdown" else "Show due countdown",
                                onClick = { viewModel.setShowDueCountdown(!uiState.showDueCountdown) },
                                active = uiState.showDueCountdown,
                            )
                        } else {
                            HeaderIconButton(
                                icon = if (uiState.zenSortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                contentDescription = if (uiState.zenSortAscending)
                                    "Sorted: most overdue first" else "Sorted: recently done first",
                                onClick = { viewModel.setZenSort(!uiState.zenSortAscending) },
                            )
                        }
                        HeaderIconButton(
                            icon = DashIcons.Zen,
                            contentDescription = if (uiState.zenMode)
                                "Exit zen mode" else "Enter zen mode",
                            onClick = { viewModel.setZenMode(!uiState.zenMode) },
                            active = uiState.zenMode,
                        )
                        if (!uiState.zenMode) {
                            HeaderIconButton(
                                icon = Icons.Outlined.Search,
                                contentDescription = if (searchActive) "Close search" else "Search chores",
                                onClick = {
                                    searchActive = !searchActive
                                    if (!searchActive) searchQuery = ""
                                },
                                active = searchActive,
                            )
                            HeaderIconButton(
                                icon = if (uiState.groupByCategory) Icons.Outlined.GridView
                                       else Icons.Outlined.ViewAgenda,
                                contentDescription = if (uiState.groupByCategory)
                                    "Show as flat list" else "Group by category",
                                onClick = { viewModel.setGroupBy(!uiState.groupByCategory) },
                            )
                        }
                    },
                )
                if (searchActive && !uiState.zenMode) {
                    SearchRow(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onCancel = { searchActive = false; searchQuery = "" },
                        placeholder = "Search chores",
                    )
                    val query = searchQuery.trim()
                    val results = if (query.isEmpty()) emptyList() else
                        uiState.active.filter { c ->
                            c.label.contains(query, ignoreCase = true) ||
                                c.category?.contains(query, ignoreCase = true) == true ||
                                c.owner?.contains(query, ignoreCase = true) == true
                        }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "search_count") {
                            SectionLabel(
                                text = "in chores · ${results.size}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                        items(results, key = { it.id }) { chore ->
                            SwipeToLogCard(
                                chore = chore,
                                showOwner = uiState.ownerFilter.showsOwner,
                                zenMode = false,
                                showDueCountdown = uiState.showDueCountdown,
                                showCategory = true,
                                onTap = { logTargetChore = it; showLogSheet = true },
                                onLongPress = { editTargetChore = it; showEditSheet = true },
                                onSwipeLog = { viewModel.logChore(it.tagId) },
                                onSwipeSnooze = { viewModel.toggleSnooze(it) },
                                snoozedUntil = uiState.snoozedUntil(chore),
                                highlightQuery = query
                            )
                        }
                    }
                } else {
                    // Filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChoreFilter.entries.forEach { f ->
                            FilterChip(
                                selected = uiState.filter == f,
                                onClick = { viewModel.setFilter(f) },
                                label = { Text(f.label) },
                                shape = PillShape,
                                // High-contrast fill so selected state reads in 100ms (GoFlo LESSONS.md)
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
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
                            else "No chores found. Tap + to add one, or scan an NFC tag."
                        )

                        else -> {
                            val displayed = uiState.displayed
                            val hiddenChores = uiState.hiddenChores
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 88.dp)
                            ) {
                                if (uiState.groupByCategory && !uiState.zenMode) {
                                    val grouped = displayed.groupBy { it.category ?: "Uncategorised" }
                                    grouped.forEach { (category, chores) ->
                                        stickyHeader {
                                            SectionLabel(
                                                text = category,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.background)
                                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                            )
                                        }
                                        items(chores, key = { it.id }) { chore ->
                                            SwipeToLogCard(
                                                chore = chore,
                                                showOwner = uiState.ownerFilter.showsOwner,
                                                zenMode = uiState.zenMode,
                                                showDueCountdown = uiState.showDueCountdown,
                                                showCategory = !uiState.groupByCategory,
                                                onTap = { logTargetChore = it; showLogSheet = true },
                                                onLongPress = { editTargetChore = it; showEditSheet = true },
                                                onSwipeLog = { viewModel.logChore(it.tagId) },
                                                onSwipeSnooze = { viewModel.toggleSnooze(it) },
                                                snoozedUntil = uiState.snoozedUntil(chore)
                                            )
                                        }
                                    }
                                } else {
                                    items(displayed, key = { it.id }) { chore ->
                                        SwipeToLogCard(
                                            chore = chore,
                                            showOwner = uiState.ownerFilter.showsOwner,
                                            zenMode = uiState.zenMode,
                                            showDueCountdown = uiState.showDueCountdown,
                                            showCategory = !uiState.groupByCategory,
                                            onTap = { logTargetChore = it; showLogSheet = true },
                                            onLongPress = { editTargetChore = it; showEditSheet = true },
                                            onSwipeLog = { viewModel.logChore(it.tagId) },
                                            onSwipeSnooze = { viewModel.toggleSnooze(it) },
                                            snoozedUntil = uiState.snoozedUntil(chore)
                                        )
                                    }
                                }

                                if (hiddenChores.isNotEmpty()) {
                                    item {
                                        TextButton(
                                            onClick = { viewModel.toggleShowHidden() },
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                hiddenSectionLabel(
                                                    total = hiddenChores.size,
                                                    snoozed = uiState.snoozedCount,
                                                    expanded = uiState.showHidden,
                                                    smartVisibility = uiState.smartVisibility,
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (uiState.showHidden) {
                                        items(hiddenChores, key = { "hidden_${it.id}" }) { chore ->
                                            SwipeToLogCard(
                                                chore = chore,
                                                showOwner = uiState.ownerFilter.showsOwner,
                                                zenMode = uiState.zenMode,
                                                showDueCountdown = uiState.showDueCountdown,
                                                showCategory = !uiState.groupByCategory,
                                                onTap = { logTargetChore = it; showLogSheet = true },
                                                onLongPress = { editTargetChore = it; showEditSheet = true },
                                                onSwipeLog = { viewModel.logChore(it.tagId) },
                                                onSwipeSnooze = { viewModel.toggleSnooze(it) },
                                                snoozedUntil = uiState.snoozedUntil(chore)
                                            )
                                        }
                                    }
                                }

                                if (hasNfc && !uiState.zenMode) {
                                    item(key = "nfc_hint") { NfcHintCard() }
                                }

                                if (uiState.archived.isNotEmpty()) {
                                    item {
                                        TextButton(
                                            onClick = { showArchivedSection = !showArchivedSection },
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                if (showArchivedSection)
                                                    "Hide archived (${uiState.archived.size})"
                                                else
                                                    "Show archived (${uiState.archived.size})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                showOwner = uiState.ownerFilter.showsOwner,
                                                zenMode = uiState.zenMode,
                                                showDueCountdown = uiState.showDueCountdown,
                                                showCategory = !uiState.groupByCategory,
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
    }

    if (showLogSheet && logTargetChore != null) {
        ChoreOverviewSheet(
            chore = logTargetChore!!,
            isPinned = logTargetChore!!.id == uiState.pinnedChoreId,
            scanHistory = uiState.scanHistory,
            sheetState = logSheetState,
            onConfirmLog = { chore, at ->
                viewModel.logChore(chore.tagId, at)
                showLogSheet = false
                if (uiState.pendingNfcTagId != null) {
                    viewModel.clearPendingNfcTag()
                    onNfcConsumed()
                }
            },
            onRemoveLastLog = { chore -> viewModel.removeLastLog(chore) },
            onTogglePin = { chore -> viewModel.togglePin(chore.id) },
            onAddReminder = { chore ->
                showLogSheet = false
                reminderTargetChore = chore
            },
            onMoreOptions = { chore ->
                showLogSheet = false
                editTargetChore = chore
                showEditSheet = true
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
            onWriteTag = { chore ->
                showEditSheet = false
                onStartNfcWrite(chore.tagId)
            },
            onDismiss = { showEditSheet = false }
        )
    }

    if (nfcWriteRequest != null) {
        WriteTagDialog(
            result = nfcWriteResult,
            onDismiss = {
                if (nfcWriteResult != null) onNfcWriteResultConsumed() else onCancelNfcWrite()
            }
        )
    }

    reminderTargetChore?.let { chore ->
        AddReminderSheet(
            chores = uiState.active,
            tasks = emptyList(),
            initialChoreId = chore.id,
            initialSubject = chore.label,
            onSave = { insert ->
                viewModel.addReminderForChore(
                    ReminderInsert(
                        subject = insert.subject,
                        remindAt = insert.remindAt,
                        choreId = chore.id,
                        taskId = null
                    )
                )
            },
            onDismiss = { reminderTargetChore = null }
        )
    }

    if (showAddSheet) {
        AddChoreSheet(
            initialTagId = addSheetTagId,
            owners = uiState.owners,
            categories = uiState.categories,
            sheetState = addSheetState,
            onSave = { tagId, label, category, owner, intervalDays ->
                viewModel.addChore(tagId, label, category, owner, intervalDays)
                showAddSheet = false
                if (uiState.pendingNfcTagId != null) {
                    viewModel.clearPendingNfcTag()
                    onNfcConsumed()
                }
            },
            onDismiss = {
                showAddSheet = false
                if (uiState.pendingNfcTagId != null) {
                    viewModel.clearPendingNfcTag()
                    onNfcConsumed()
                }
            }
        )
    }

    uiState.pinChooser?.let { chooser ->
        PinWidgetChooserDialog(
            widgetIds = chooser.widgetIds,
            onChoose = { appWidgetId -> viewModel.pinToWidget(appWidgetId) },
            onDismiss = { viewModel.dismissPinChooser() }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeToLogCard(
    chore: Chore,
    showOwner: Boolean,
    zenMode: Boolean,
    showDueCountdown: Boolean,
    showCategory: Boolean,
    onTap: (Chore) -> Unit,
    onLongPress: (Chore) -> Unit,
    onSwipeLog: (Chore) -> Unit,
    onSwipeSnooze: (Chore) -> Unit,
    snoozedUntil: Instant? = null,
    highlightQuery: String? = null
) {
    // Swipe right (start to end) logs the chore; swipe left (end to start)
    // snoozes it, or wakes it if it is already snoozed.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onSwipeLog(chore)
                SwipeToDismissBoxValue.EndToStart -> onSwipeSnooze(chore)
                SwipeToDismissBoxValue.Settled -> Unit
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
            // Only drawn mid-swipe so nothing sits behind a resting card.
            val direction = dismissState.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                val snoozing = direction == SwipeToDismissBoxValue.EndToStart
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(
                            if (snoozing) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = if (snoozing) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Text(
                        when {
                            !snoozing -> "Log✔"
                            snoozedUntil != null -> "Wake"
                            else -> "Snooze"
                        },
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (snoozing) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    ) {
        ChoreCard(
            chore = chore,
            showOwner = showOwner,
            zenMode = zenMode,
            showDueCountdown = showDueCountdown,
            showCategory = showCategory,
            highlightQuery = highlightQuery,
            snoozedUntil = snoozedUntil,
            modifier = Modifier
                .semantics { role = Role.Button }
                .combinedClickable(
                    onClick = { onTap(chore) },
                    onLongClick = { onLongPress(chore) }
                )
        )
    }
}

/**
 * Label for the collapsed hidden section: lead-time hidden and snoozed counts
 * stated separately so the user knows why each group is out of sight.
 */
private fun hiddenSectionLabel(
    total: Int,
    snoozed: Int,
    expanded: Boolean,
    smartVisibility: Boolean,
): String {
    if (expanded) return "Collapse hidden chores ($total)"
    val beyondLeadTime = total - snoozed
    val parts = mutableListOf<String>()
    if (beyondLeadTime > 0) {
        parts += if (smartVisibility) "$beyondLeadTime hidden until closer to due"
                 else "$beyondLeadTime not due for 60+ days"
    }
    if (snoozed > 0) parts += "$snoozed snoozed"
    return parts.joinToString(" · ")
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