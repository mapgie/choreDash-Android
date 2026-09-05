package com.mapgie.dash.ui.screens.chores

import android.nfc.NfcAdapter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.NEW_DRAFT_KEY
import com.mapgie.dash.data.model.ChoreSortKey
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.nfc.NfcWriteResult
import com.mapgie.dash.ui.components.AddReminderSheet
import com.mapgie.dash.ui.components.ChoreCard
import com.mapgie.dash.ui.components.ChoreOverviewSheet
import com.mapgie.dash.ui.components.EditChoreSheet
import com.mapgie.dash.ui.components.PinWidgetChooserDialog
import com.mapgie.dash.ui.components.WriteTagDialog
import com.mapgie.dash.ui.components.core.HeaderIconButton
import com.mapgie.dash.ui.components.core.NfcScanButton
import com.mapgie.dash.ui.components.core.NfcScanDialog
import com.mapgie.dash.ui.components.core.OwnerFilterButton
import com.mapgie.dash.ui.components.core.PageHeader
import com.mapgie.dash.ui.components.core.SearchRow
import com.mapgie.dash.ui.components.core.SectionHeaderRow
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.components.core.SortPill
import com.mapgie.dash.ui.components.core.SortSheet
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.components.LeaveZenButton
import com.mapgie.dash.ui.components.ZenRow
import com.mapgie.dash.ui.components.ZenScopeToggle
import com.mapgie.dash.data.model.OwnerFilter
import com.mapgie.dash.data.model.ZenPhrase
import com.mapgie.dash.ui.theme.PillShape
import com.mapgie.dash.ui.theme.StatusTone
import com.mapgie.dash.ui.theme.badgeContainerColor
import com.mapgie.dash.ui.theme.textColor
import com.mapgie.dash.util.formatAbsoluteDate
import java.time.Instant

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
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // SheetState hoisted above the composables that use them (GoFlo LESSONS.md)
    val logSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var logTargetChore by remember { mutableStateOf<Chore?>(null) }
    var editTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var showLogSheet by remember { mutableStateOf(false) }
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var showArchivedSection by remember { mutableStateOf(false) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showNfcDialog by remember { mutableStateOf(false) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var addSheetTagId by rememberSaveable { mutableStateOf("") }
    var reminderTargetChore by remember { mutableStateOf<Chore?>(null) }
    val context = LocalContext.current
    val hasNfc = remember { NfcAdapter.getDefaultAdapter(context) != null }
    val choreAccent = LocalTypeAccents.current.onChoreContainer

    fun iconFor(chore: Chore): ImageVector =
        LucideIcons.forCategory(uiState.catalog.iconFor(chore.category))

    // Settings › Colours: the spine+badge and the icon each follow severity or the category colour.
    fun spineSwatchFor(chore: Chore): Swatch? =
        uiState.colourAxes.spineSwatch(uiState.catalog.effectiveSwatch(chore.category))

    fun iconSwatchFor(chore: Chore): Swatch? =
        uiState.colourAxes.iconSwatch(uiState.catalog.effectiveSwatch(chore.category))

    // The Edit sheet's target is kept by id and resolved from uiState so the open
    // sheet survives rotation and process death; it closes cleanly if the chore
    // is gone once the list has loaded.
    val editTargetChore = editTargetId?.let { id -> (uiState.active + uiState.archived).find { it.id == id } }
    LaunchedEffect(showEditSheet, editTargetChore, uiState.loading) {
        if (showEditSheet && editTargetChore == null && !uiState.loading) {
            showEditSheet = false
            editTargetId = null
        }
    }

    // Handle incoming NFC tag from MainActivity
    LaunchedEffect(pendingNfcTagId, uiState.active.size) {
        val tagId = pendingNfcTagId ?: return@LaunchedEffect
        showNfcDialog = false
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

    // Keep the overview sheet's chore data fresh after actions like Undo
    LaunchedEffect(uiState.active, uiState.archived) {
        val current = logTargetChore ?: return@LaunchedEffect
        (uiState.active + uiState.archived).find { it.id == current.id }?.let { fresh ->
            if (fresh != current) logTargetChore = fresh
        }
    }

    // Load recent scan history whenever the overview sheet is opened for a chore
    LaunchedEffect(showLogSheet, logTargetChore?.id) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (uiState.zenMode) MaterialTheme.colorScheme.surfaceContainerLow
                        else MaterialTheme.colorScheme.background
                    )
            ) {
                PageHeader(
                    title = if (uiState.zenMode) "zen" else "chores",
                    accent = if (uiState.zenMode) LocalDashTokens.current.sectionCount else choreAccent,
                    leading = {
                        if (hasNfc && !uiState.zenMode) {
                            NfcScanButton(onClick = { showNfcDialog = true })
                        }
                    },
                    actions = {
                        if (uiState.zenMode) {
                            // Zen header (3a-4): mine | all, the sort arrow, LEAVE.
                            if (uiState.ownerHandle.isNotBlank()) {
                                ZenScopeToggle(
                                    mine = uiState.ownerFilter == OwnerFilter.MINE,
                                    onMineChange = { mine ->
                                        viewModel.setOwnerFilter(if (mine) OwnerFilter.MINE else OwnerFilter.EVERYONE)
                                    },
                                )
                            }
                            HeaderIconButton(
                                icon = if (uiState.zenSortAscending) LucideIcons.ArrowUp else LucideIcons.ArrowDown,
                                contentDescription = if (uiState.zenSortAscending)
                                    "Sorted: most overdue first" else "Sorted: recently done first",
                                onClick = { viewModel.setZenSort(!uiState.zenSortAscending) },
                            )
                            LeaveZenButton(onClick = { viewModel.setZenMode(false) })
                        } else {
                            // Same row as Tasks: search, owner, zen, group/flat.
                            HeaderIconButton(
                                icon = LucideIcons.Search,
                                contentDescription = if (searchActive) "Close search" else "Search chores",
                                onClick = {
                                    searchActive = !searchActive
                                    if (!searchActive) searchQuery = ""
                                },
                                active = searchActive,
                                activeTint = choreAccent,
                            )
                            if (uiState.ownerHandle.isNotBlank()) {
                                OwnerFilterButton(
                                    filter = uiState.ownerFilter,
                                    onFilterChange = viewModel::setOwnerFilter,
                                    activeTint = choreAccent,
                                )
                            }
                            HeaderIconButton(
                                icon = LucideIcons.Target,
                                contentDescription = "Enter zen mode",
                                onClick = { viewModel.setZenMode(true) },
                            )
                            HeaderIconButton(
                                icon = if (uiState.groupByCategory) LucideIcons.LayoutGrid
                                       else LucideIcons.List,
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
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
                        modifier = Modifier.weight(1f)
                    ) {
                        item(key = "search_count") {
                            SectionLabel(
                                text = "in chores · ${results.size}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                        items(results, key = { it.id }) { chore ->
                            SwipeToLogCard(
                                chore = chore,
                                icon = iconFor(chore),
                                spineSwatch = spineSwatchFor(chore),
                                iconSwatch = iconSwatchFor(chore),
                                showOwner = uiState.ownerFilter.showsOwner,
                                zenMode = false,
                                showCategory = true,
                                onTap = { logTargetChore = it; showLogSheet = true },
                                onLongPress = { editTargetId = it.id; showEditSheet = true },
                                onSwipeLog = { viewModel.logChore(it.tagId) },
                                onSwipeSnooze = { viewModel.toggleSnooze(it) },
                                snoozedUntil = uiState.snoozedUntil(chore),
                                highlightQuery = query
                            )
                        }
                    }
                } else {
                    // Filter chips, then the sort pill pinned to the right.
                    if (!uiState.zenMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChoreFilter.entries.forEach { f ->
                                val tone = when (f) {
                                    ChoreFilter.ALL -> null
                                    ChoreFilter.OVERDUE -> StatusTone.CRITICAL
                                    ChoreFilter.SOON -> StatusTone.ATTENTION
                                }
                                val selected = uiState.filter == f
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setFilter(f) },
                                    label = {
                                        Text(
                                            text = if (f == ChoreFilter.OVERDUE && uiState.overdueCount > 0)
                                                "${f.label} · ${uiState.overdueCount}" else f.label,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                    },
                                    shape = PillShape,
                                    border = null,
                                    // Explicit high-contrast fills (LESSONS.md #3): the All chip
                                    // takes the sage accent, the status chips their own tint.
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = tone?.badgeContainerColor()
                                            ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                                        labelColor = tone?.textColor()
                                            ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = tone?.badgeContainerColor()
                                            ?: MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = tone?.textColor()
                                            ?: MaterialTheme.colorScheme.onSecondary,
                                    )
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            SortPill(order = uiState.sort, onClick = { showSortSheet = true })
                        }
                    }

                    when {
                        uiState.error != null -> ErrorState(
                            message = uiState.error!!,
                            onRetry = { viewModel.load() },
                            modifier = Modifier.weight(1f),
                        )

                        !uiState.loading && uiState.active.isEmpty() -> EmptyState(
                            message = if (uiState.owners.isEmpty())
                                "Configure Supabase credentials in Settings to get started"
                            else "No chores found. Tap + to add one, or scan an NFC tag.",
                            modifier = Modifier.weight(1f),
                        )

                        else -> {
                            val displayed = uiState.displayed
                            val hiddenChores = uiState.hiddenChores
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
                            ) {
                                if (uiState.groupByCategory && !uiState.zenMode) {
                                    uiState.grouped.forEach { (category, chores) ->
                                        stickyHeader(key = "group_$category") {
                                            SectionHeaderRow(
                                                text = category,
                                                count = chores.size,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.background)
                                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                                            )
                                        }
                                        items(chores, key = { it.id }) { chore ->
                                            SwipeToLogCard(
                                                chore = chore,
                                                icon = iconFor(chore),
                                                spineSwatch = spineSwatchFor(chore),
                                iconSwatch = iconSwatchFor(chore),
                                                showOwner = uiState.ownerFilter.showsOwner,
                                                zenMode = uiState.zenMode,
                                                showCategory = false,
                                                onTap = { logTargetChore = it; showLogSheet = true },
                                                onLongPress = { editTargetId = it.id; showEditSheet = true },
                                                onSwipeLog = { viewModel.logChore(it.tagId) },
                                                onSwipeSnooze = { viewModel.toggleSnooze(it) },
                                                snoozedUntil = uiState.snoozedUntil(chore)
                                            )
                                        }
                                    }
                                } else if (uiState.zenMode) {
                                    // Zen rows (3a-4): open circle, title, gentle cue; no colours, no counts.
                                    items(displayed, key = { it.id }) { chore ->
                                        val zenDone = chore.id in uiState.zenDoneIds
                                        ZenRow(
                                            title = chore.label,
                                            sub = ZenPhrase.forChore(chore.category, chore.status, zenDone),
                                            done = zenDone,
                                            onToggle = { if (!zenDone) viewModel.logChoreInZen(chore) },
                                            modifier = Modifier
                                                .padding(horizontal = Dimens.cardInset)
                                                .semantics { role = Role.Button }
                                                .combinedClickable(
                                                    onClick = { logTargetChore = chore; showLogSheet = true },
                                                    onLongClick = { editTargetId = chore.id; showEditSheet = true }
                                                ),
                                        )
                                    }
                                } else {
                                    items(displayed, key = { it.id }) { chore ->
                                        SwipeToLogCard(
                                            chore = chore,
                                            icon = iconFor(chore),
                                            spineSwatch = spineSwatchFor(chore),
                                iconSwatch = iconSwatchFor(chore),
                                            showOwner = uiState.ownerFilter.showsOwner,
                                            zenMode = uiState.zenMode,
                                            showCategory = !uiState.groupByCategory,
                                            onTap = { logTargetChore = it; showLogSheet = true },
                                            onLongPress = { editTargetId = it.id; showEditSheet = true },
                                            onSwipeLog = { viewModel.logChore(it.tagId) },
                                            onSwipeSnooze = { viewModel.toggleSnooze(it) },
                                            snoozedUntil = uiState.snoozedUntil(chore)
                                        )
                                    }
                                }

                                if (hiddenChores.isNotEmpty()) {
                                    item(key = "hidden_toggle") {
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
                                                icon = iconFor(chore),
                                                spineSwatch = spineSwatchFor(chore),
                                iconSwatch = iconSwatchFor(chore),
                                                showOwner = uiState.ownerFilter.showsOwner,
                                                zenMode = uiState.zenMode,
                                                showCategory = !uiState.groupByCategory,
                                                onTap = { logTargetChore = it; showLogSheet = true },
                                                onLongPress = { editTargetId = it.id; showEditSheet = true },
                                                onSwipeLog = { viewModel.logChore(it.tagId) },
                                                onSwipeSnooze = { viewModel.toggleSnooze(it) },
                                                snoozedUntil = uiState.snoozedUntil(chore)
                                            )
                                        }
                                    }
                                }

                                if (uiState.archived.isNotEmpty()) {
                                    item(key = "archived_toggle") {
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
                                                icon = iconFor(chore),
                                                spineSwatch = spineSwatchFor(chore),
                                iconSwatch = iconSwatchFor(chore),
                                                showOwner = uiState.ownerFilter.showsOwner,
                                                zenMode = uiState.zenMode,
                                                showCategory = !uiState.groupByCategory,
                                                modifier = Modifier
                                                    .semantics { role = Role.Button }
                                                    .combinedClickable(
                                                        onClick = {},
                                                        onLongClick = {
                                                            editTargetId = chore.id
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

    if (showSortSheet) {
        SortSheet(
            title = "Sort chores by",
            keys = ChoreSortKey.entries,
            order = uiState.sort,
            onOrderChange = { viewModel.setSort(it) },
            onDismiss = { showSortSheet = false },
        )
    }

    if (showNfcDialog) {
        NfcScanDialog(onDismiss = { showNfcDialog = false })
    }

    if (showLogSheet && logTargetChore != null) {
        val chore = logTargetChore!!
        ChoreOverviewSheet(
            chore = chore,
            icon = iconFor(chore),
            badgeSwatch = spineSwatchFor(chore),
            iconSwatch = iconSwatchFor(chore),
            isPinned = chore.id == uiState.pinnedChoreId,
            scanHistory = uiState.scanHistory,
            sheetState = logSheetState,
            onConfirmLog = { c, at ->
                viewModel.logChore(c.tagId, at)
                showLogSheet = false
                if (uiState.pendingNfcTagId != null) {
                    viewModel.clearPendingNfcTag()
                    onNfcConsumed()
                }
            },
            onRemoveLastLog = { c -> viewModel.removeLastLog(c) },
            onLoadAllHistory = { c -> viewModel.loadScanHistory(c.tagId, limit = 100) },
            onTogglePin = { c -> viewModel.togglePin(c.id) },
            onAddReminder = { c ->
                showLogSheet = false
                reminderTargetChore = c
            },
            onEdit = { c ->
                showLogSheet = false
                editTargetId = c.id
                showEditSheet = true
            },
            onWriteTag = { c ->
                showLogSheet = false
                onStartNfcWrite(c.tagId)
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
        val chore = editTargetChore
        EditChoreSheet(
            chore = chore,
            icon = iconFor(chore),
            badgeSwatch = spineSwatchFor(chore),
            iconSwatch = iconSwatchFor(chore),
            owners = uiState.owners,
            categories = uiState.categories,
            sheetState = editSheetState,
            draft = remember(chore.id) { viewModel.choreDrafts.get(chore.id) },
            onDraftChange = { viewModel.choreDrafts.put(chore.id, it) },
            onDraftClear = { viewModel.choreDrafts.clear(chore.id) },
            onSave = { tagId, label, category, owner, intervalDays ->
                viewModel.updateChore(tagId, label, category, owner, intervalDays)
                showEditSheet = false
            },
            onArchiveToggle = { c, archive ->
                viewModel.archiveChore(c.tagId, archive)
                showEditSheet = false
            },
            onWriteTag = { tagId ->
                showEditSheet = false
                onStartNfcWrite(tagId)
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
        EditChoreSheet(
            chore = null,
            initialTagId = addSheetTagId,
            icon = LucideIcons.HouseCheck,
            badgeSwatch = null,
            iconSwatch = null,
            owners = uiState.owners,
            categories = uiState.categories,
            sheetState = addSheetState,
            draft = remember { viewModel.choreDrafts.get(NEW_DRAFT_KEY) },
            onDraftChange = { viewModel.choreDrafts.put(NEW_DRAFT_KEY, it) },
            onDraftClear = { viewModel.choreDrafts.clear(NEW_DRAFT_KEY) },
            onSave = { tagId, label, category, owner, intervalDays ->
                viewModel.addChore(tagId, label, category, owner, intervalDays)
                showAddSheet = false
                if (uiState.pendingNfcTagId != null) {
                    viewModel.clearPendingNfcTag()
                    onNfcConsumed()
                }
            },
            onArchiveToggle = { _, _ -> },
            onWriteTag = { tagId ->
                showAddSheet = false
                onStartNfcWrite(tagId)
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
    icon: ImageVector,
    spineSwatch: Swatch?,
    iconSwatch: Swatch?,
    showOwner: Boolean,
    zenMode: Boolean,
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
                        .padding(horizontal = Dimens.cardInset)
                        .background(
                            if (snoozing) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = if (snoozing) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Text(
                        when {
                            !snoozing -> "Log"
                            snoozedUntil != null -> "Wake"
                            else -> "Snooze"
                        },
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (snoozing) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    ) {
        ChoreCard(
            chore = chore,
            icon = icon,
            spineSwatch = spineSwatch,
            iconSwatch = iconSwatch,
            showOwner = showOwner,
            zenMode = zenMode,
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
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp)
        )
    }
}
