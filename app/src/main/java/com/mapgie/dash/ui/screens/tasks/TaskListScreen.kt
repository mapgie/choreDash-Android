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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.ui.components.AddMenuOption
import com.mapgie.dash.ui.components.EditTaskSheet
import com.mapgie.dash.ui.components.TaskCard
import com.mapgie.dash.ui.components.TaskOverviewSheet

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
                // Filter chips + toggles
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (!uiState.zenMode) {
                        IconButton(
                            onClick = { viewModel.setSort(uiState.sort.next()) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.Sort,
                                contentDescription = "Sort: ${uiState.sort.label}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (uiState.ownerHandle.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.setOwnerFilter(
                                        if (uiState.ownerFilter == OwnerFilter.MINE) OwnerFilter.ALL
                                        else OwnerFilter.MINE
                                    )
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = if (uiState.ownerFilter == OwnerFilter.MINE)
                                        "Show all owners" else "Show my tasks",
                                    tint = if (uiState.ownerFilter == OwnerFilter.MINE)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.setZenSort(!uiState.zenSortAscending) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                if (uiState.zenSortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                contentDescription = if (uiState.zenSortAscending)
                                    "Sorted: most urgent first" else "Sorted: least urgent first",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.setZenMode(!uiState.zenMode) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.Spa,
                            contentDescription = if (uiState.zenMode)
                                "Exit zen mode" else "Enter zen mode",
                            tint = if (uiState.zenMode)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
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
                                    Text(
                                        text = cat.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        showOwner = (uiState.ownerFilter != OwnerFilter.MINE),
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
                                    showOwner = (uiState.ownerFilter != OwnerFilter.MINE),
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
                                        showOwner = (uiState.ownerFilter != OwnerFilter.MINE),
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
    zenMode: Boolean = false
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

private val TaskSort.label: String
    get() = when (this) {
        TaskSort.PRIORITY -> "Priority"
        TaskSort.DUE -> "Due date"
        TaskSort.CREATED -> "Created"
    }

private fun TaskSort.next(): TaskSort = when (this) {
    TaskSort.PRIORITY -> TaskSort.DUE
    TaskSort.DUE -> TaskSort.CREATED
    TaskSort.CREATED -> TaskSort.PRIORITY
}
