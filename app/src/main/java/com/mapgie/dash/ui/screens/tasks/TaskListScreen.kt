package com.mapgie.dash.ui.screens.tasks

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.ui.components.EditTaskSheet
import com.mapgie.dash.ui.components.PinWidgetChooserDialog
import com.mapgie.dash.ui.components.TaskCard
import com.mapgie.dash.ui.components.TaskOverviewSheet
import com.mapgie.dash.ui.components.core.HeaderIconButton
import com.mapgie.dash.ui.components.core.OwnerFilterButton
import com.mapgie.dash.ui.components.core.PageHeader
import com.mapgie.dash.ui.components.core.SearchRow
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.theme.DashIcons
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskListScreen(
    pendingAddIntent: AddMenuOption? = null,
    onPendingAddIntentConsumed: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var showTaskSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskDto?>(null) }
    var doneExpanded by remember { mutableStateOf(false) }

    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var showOverviewSheet by remember { mutableStateOf(false) }
    var overviewTask by remember { mutableStateOf<TaskDto?>(null) }
    val overviewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(pendingAddIntent) {
        if (pendingAddIntent == AddMenuOption.TASK) {
            editingTask = null
            showTaskSheet = true
            onPendingAddIntentConsumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) }
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
                    title = if (uiState.zenMode) "zen" else "tasks",
                    accent = LocalTypeAccents.current.onTaskContainer,
                    actions = {
                        // Design order: owner filter, zen, search, group/flat.
                        if (!uiState.zenMode) {
                            if (uiState.ownerHandle.isNotBlank()) {
                                OwnerFilterButton(
                                    filter = uiState.ownerFilter,
                                    onFilterChange = viewModel::setOwnerFilter,
                                )
                            }
                        } else {
                            HeaderIconButton(
                                icon = if (uiState.zenSortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                contentDescription = if (uiState.zenSortAscending)
                                    "Sorted: most urgent first" else "Sorted: least urgent first",
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
                                contentDescription = if (searchActive) "Close search" else "Search tasks",
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
                        placeholder = "Search tasks",
                    )
                    val query = searchQuery.trim()
                    val results = if (query.isEmpty()) emptyList() else
                        (uiState.activeTasks + uiState.doneTasks).filter { t ->
                            t.title.contains(query, ignoreCase = true) ||
                                t.category?.contains(query, ignoreCase = true) == true ||
                                t.owner?.contains(query, ignoreCase = true) == true
                        }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "search_count") {
                            SectionLabel(
                                text = "in tasks · ${results.size}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                        items(results, key = { it.id }) { task ->
                            SwipeToCompleteCard(
                                task = task,
                                onTap = { overviewTask = it; showOverviewSheet = true },
                                onLongPress = { editingTask = it; showTaskSheet = true },
                                onToggleDone = {
                                    if (task.completedAt != null) viewModel.markUndone(task.id)
                                    else viewModel.markDone(task.id)
                                },
                                onSwipeToggleDone = {
                                    if (task.completedAt != null) viewModel.markUndone(task.id)
                                    else viewModel.markDone(task.id)
                                },
                                highlightQuery = query
                            )
                        }
                    }
                } else {
                    // Filter chips + the "due ↑" sort control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TaskFilter.entries.forEach { f ->
                            FilterChip(
                                selected = uiState.filter == f,
                                onClick = { viewModel.setFilter(f) },
                                label = { Text(f.label) },
                                shape = PillShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (!uiState.zenMode) {
                            TextButton(onClick = { viewModel.setSort(uiState.sort.next()) }) {
                                Text(
                                    "${uiState.sort.shortLabel} ↑",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val active = uiState.activeTasks
                    val done = uiState.doneTasks

                    if (!uiState.loading && active.isEmpty() && done.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No tasks",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.groupByCategory && !uiState.zenMode && uiState.filter != TaskFilter.DONE) {
                                val grouped = active.groupBy { it.category?.takeIf(String::isNotBlank) ?: "Other" }
                                grouped.forEach { (cat, tasks) ->
                                    stickyHeader {
                                        SectionLabel(
                                            text = cat,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.background)
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                    items(tasks, key = { it.id }) { task ->
                                        SwipeToCompleteCard(
                                            task = task,
                                            onTap = { overviewTask = it; showOverviewSheet = true },
                                            onLongPress = { editingTask = it; showTaskSheet = true },
                                            onToggleDone = { viewModel.markDone(task.id) },
                                            onSwipeToggleDone = { viewModel.markDone(task.id) },
                                            showCategory = false,
                                            showOwner = uiState.ownerFilter.showsOwner,
                                            zenMode = uiState.zenMode
                                        )
                                    }
                                }
                            } else {
                                items(active, key = { it.id }) { task ->
                                    SwipeToCompleteCard(
                                        task = task,
                                        onTap = { overviewTask = it; showOverviewSheet = true },
                                        onLongPress = { editingTask = it; showTaskSheet = true },
                                        onToggleDone = { viewModel.markDone(task.id) },
                                        onSwipeToggleDone = { viewModel.markDone(task.id) },
                                        showCategory = !uiState.groupByCategory,
                                        showOwner = uiState.ownerFilter.showsOwner,
                                        zenMode = uiState.zenMode
                                    )
                                }
                            }

                            if (done.isNotEmpty()) {
                                item(key = "done_header") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
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
                                    items(done, key = { it.id }) { task ->
                                        SwipeToCompleteCard(
                                            task = task,
                                            onTap = { overviewTask = it; showOverviewSheet = true },
                                            onLongPress = { editingTask = it; showTaskSheet = true },
                                            onToggleDone = { viewModel.markUndone(task.id) },
                                            onSwipeToggleDone = { viewModel.markUndone(task.id) },
                                            showCategory = !uiState.groupByCategory,
                                            showOwner = uiState.ownerFilter.showsOwner,
                                            zenMode = uiState.zenMode
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

    if (showTaskSheet) {
        EditTaskSheet(
            task = editingTask,
            owners = uiState.owners,
            categories = uiState.categories,
            onSave = { insert -> viewModel.addTask(insert) },
            onUpdate = { update -> editingTask?.id?.let { viewModel.updateTask(it, update) } },
            onDelete = { editingTask?.id?.let { viewModel.deleteTask(it) } },
            onDismiss = { showTaskSheet = false; editingTask = null }
        )
    }

    if (showOverviewSheet && overviewTask != null) {
        TaskOverviewSheet(
            task = overviewTask!!,
            isPinned = overviewTask!!.id == uiState.pinnedTaskId,
            sheetState = overviewSheetState,
            onToggleDone = { task ->
                if (task.completedAt != null) viewModel.markUndone(task.id)
                else viewModel.markDone(task.id)
                showOverviewSheet = false
            },
            onTogglePin = { task -> viewModel.togglePin(task.id) },
            onEdit = { task ->
                showOverviewSheet = false
                editingTask = task
                showTaskSheet = true
            },
            onDismiss = { showOverviewSheet = false }
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
private fun SwipeToCompleteCard(
    task: TaskDto,
    onTap: (TaskDto) -> Unit,
    onLongPress: (TaskDto) -> Unit,
    onToggleDone: () -> Unit,
    onSwipeToggleDone: () -> Unit,
    showCategory: Boolean = true,
    showOwner: Boolean = true,
    zenMode: Boolean = false,
    highlightQuery: String? = null
) {
    val isDone = task.completedAt != null
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onSwipeToggleDone()
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
    ) {
        TaskCard(
            task = task,
            onToggleDone = onToggleDone,
            showCategory = showCategory,
            showOwner = showOwner,
            zenMode = zenMode,
            highlightQuery = highlightQuery,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .semantics { role = Role.Button }
                .combinedClickable(
                    onClick = { onTap(task) },
                    onLongClick = { onLongPress(task) }
                )
        )
    }
}

private val TaskFilter.label: String
    get() = when (this) {
        TaskFilter.ALL -> "All"
        TaskFilter.ACTIVE -> "Active"
        TaskFilter.DONE -> "Done"
    }

private val TaskSort.shortLabel: String
    get() = when (this) {
        TaskSort.PRIORITY -> "priority"
        TaskSort.DUE -> "due"
        TaskSort.CREATED -> "newest"
    }


private fun TaskSort.next(): TaskSort = when (this) {
    TaskSort.PRIORITY -> TaskSort.DUE
    TaskSort.DUE -> TaskSort.CREATED
    TaskSort.CREATED -> TaskSort.PRIORITY
}
