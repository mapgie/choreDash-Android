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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskSortKey
import com.mapgie.dash.ui.components.AddReminderSheet
import com.mapgie.dash.ui.components.EditTaskSheet
import com.mapgie.dash.ui.components.PinWidgetChooserDialog
import com.mapgie.dash.ui.components.TaskCard
import com.mapgie.dash.ui.components.TaskOverviewSheet
import com.mapgie.dash.ui.components.core.HeaderIconButton
import com.mapgie.dash.ui.components.core.OwnerFilterButton
import com.mapgie.dash.ui.components.core.PageHeader
import com.mapgie.dash.ui.components.core.SearchRow
import com.mapgie.dash.ui.components.core.SectionHeaderRow
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.components.core.SortPill
import com.mapgie.dash.ui.components.core.SortSheet
import com.mapgie.dash.ui.components.core.SummaryBar
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskListScreen(
    pendingAddIntent: AddMenuOption? = null,
    onPendingAddIntentConsumed: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showTaskSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskDto?>(null) }
    var doneExpanded by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var reminderTargetTask by remember { mutableStateOf<TaskDto?>(null) }

    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var showOverviewSheet by remember { mutableStateOf(false) }
    var overviewTask by remember { mutableStateOf<TaskDto?>(null) }
    val overviewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val taskAccent = LocalTypeAccents.current.onTaskContainer

    fun iconFor(task: TaskDto): ImageVector =
        LucideIcons.forCategory(uiState.catalog.iconFor(task.category))

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

    // Keep the overview sheet's task fresh after a pin toggle or edit.
    LaunchedEffect(uiState.tasks) {
        val current = overviewTask ?: return@LaunchedEffect
        uiState.tasks.find { it.id == current.id }?.let { fresh ->
            if (fresh != current) overviewTask = fresh
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
                    accent = taskAccent,
                    actions = {
                        // Same row as Chores: search, owner, zen, group/flat.
                        if (!uiState.zenMode) {
                            HeaderIconButton(
                                icon = LucideIcons.Search,
                                contentDescription = if (searchActive) "Close search" else "Search tasks",
                                onClick = {
                                    searchActive = !searchActive
                                    if (!searchActive) searchQuery = ""
                                },
                                active = searchActive,
                                activeTint = taskAccent,
                            )
                            if (uiState.ownerHandle.isNotBlank()) {
                                OwnerFilterButton(
                                    filter = uiState.ownerFilter,
                                    onFilterChange = viewModel::setOwnerFilter,
                                    activeTint = taskAccent,
                                )
                            }
                        } else {
                            HeaderIconButton(
                                icon = if (uiState.zenSortAscending) LucideIcons.ArrowUp else LucideIcons.ArrowDown,
                                contentDescription = if (uiState.zenSortAscending)
                                    "Sorted: most urgent first" else "Sorted: least urgent first",
                                onClick = { viewModel.setZenSort(!uiState.zenSortAscending) },
                            )
                        }
                        HeaderIconButton(
                            icon = LucideIcons.Target,
                            contentDescription = if (uiState.zenMode)
                                "Exit zen mode" else "Enter zen mode",
                            onClick = { viewModel.setZenMode(!uiState.zenMode) },
                            active = uiState.zenMode,
                            activeTint = taskAccent,
                        )
                        if (!uiState.zenMode) {
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
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
                        modifier = Modifier.weight(1f)
                    ) {
                        item(key = "search_count") {
                            SectionLabel(
                                text = "in tasks · ${results.size}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                        items(results, key = { it.id }) { task ->
                            SwipeToCompleteCard(
                                task = task,
                                icon = iconFor(task),
                                onTap = { overviewTask = it; showOverviewSheet = true },
                                onLongPress = { editingTask = it; showTaskSheet = true },
                                onToggleDone = {
                                    if (task.completedAt != null) viewModel.markUndone(task.id)
                                    else viewModel.markDone(task.id)
                                },
                                highlightQuery = query
                            )
                        }
                    }
                } else {
                    // The sort pill, right-aligned. No status chips: done tasks live in
                    // the collapsible Done section below the list.
                    if (!uiState.zenMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.weight(1f))
                            SortPill(order = uiState.sort, onClick = { showSortSheet = true })
                        }
                    }

                    val active = uiState.activeTasks
                    val done = uiState.doneTasks

                    if (!uiState.loading && active.isEmpty() && done.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "No tasks",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.groupByCategory && !uiState.zenMode) {
                                uiState.grouped.forEach { (cat, tasks) ->
                                    stickyHeader(key = "group_$cat") {
                                        SectionHeaderRow(
                                            text = cat,
                                            count = tasks.size,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.background)
                                                .padding(horizontal = 24.dp, vertical = 6.dp)
                                        )
                                    }
                                    items(tasks, key = { it.id }) { task ->
                                        SwipeToCompleteCard(
                                            task = task,
                                            icon = iconFor(task),
                                            onTap = { overviewTask = it; showOverviewSheet = true },
                                            onLongPress = { editingTask = it; showTaskSheet = true },
                                            onToggleDone = { viewModel.markDone(task.id) },
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
                                        icon = iconFor(task),
                                        onTap = { overviewTask = it; showOverviewSheet = true },
                                        onLongPress = { editingTask = it; showTaskSheet = true },
                                        onToggleDone = { viewModel.markDone(task.id) },
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
                                            .padding(start = 24.dp, end = 12.dp, top = 8.dp)
                                    ) {
                                        SectionLabel(
                                            text = "Done · ${done.size}",
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { doneExpanded = !doneExpanded }) {
                                            Icon(
                                                if (doneExpanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                                                contentDescription = if (doneExpanded) "Collapse done tasks" else "Expand done tasks"
                                            )
                                        }
                                    }
                                }
                                if (doneExpanded) {
                                    items(done, key = { it.id }) { task ->
                                        SwipeToCompleteCard(
                                            task = task,
                                            icon = iconFor(task),
                                            onTap = { overviewTask = it; showOverviewSheet = true },
                                            onLongPress = { editingTask = it; showTaskSheet = true },
                                            onToggleDone = { viewModel.markUndone(task.id) },
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
                if (!uiState.zenMode && !searchActive) {
                    val doneCount = uiState.doneTasks.size
                    SummaryBar(
                        summary = uiState.summaryLabel,
                        trailingLabel = if (doneCount > 0) "Done" else null,
                        onTrailingClick = if (doneCount > 0) {
                            {
                                doneExpanded = true
                                scope.launch {
                                    val last = listState.layoutInfo.totalItemsCount - 1
                                    if (last >= 0) listState.animateScrollToItem(last)
                                }
                            }
                        } else null,
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        SortSheet(
            title = "Sort tasks by",
            keys = TaskSortKey.entries,
            order = uiState.sort,
            onOrderChange = { viewModel.setSort(it) },
            onDismiss = { showSortSheet = false },
        )
    }

    if (showTaskSheet) {
        EditTaskSheet(
            task = editingTask,
            icon = editingTask?.let { iconFor(it) } ?: LucideIcons.CircleCheck,
            owners = uiState.owners,
            categories = uiState.categories,
            onSave = { insert -> viewModel.addTask(insert) },
            onUpdate = { update -> editingTask?.id?.let { viewModel.updateTask(it, update) } },
            onDelete = { editingTask?.id?.let { viewModel.deleteTask(it) } },
            onDismiss = { showTaskSheet = false; editingTask = null }
        )
    }

    if (showOverviewSheet && overviewTask != null) {
        val task = overviewTask!!
        TaskOverviewSheet(
            task = task,
            icon = iconFor(task),
            isPinned = task.id == uiState.pinnedTaskId,
            sheetState = overviewSheetState,
            onMarkDone = { t, at ->
                viewModel.markDone(t.id, at)
                showOverviewSheet = false
            },
            onRestore = { t ->
                viewModel.markUndone(t.id)
                showOverviewSheet = false
            },
            onTogglePin = { t -> viewModel.togglePin(t.id) },
            onAddReminder = { t ->
                showOverviewSheet = false
                reminderTargetTask = t
            },
            onEdit = { t ->
                showOverviewSheet = false
                editingTask = t
                showTaskSheet = true
            },
            onDismiss = { showOverviewSheet = false }
        )
    }

    reminderTargetTask?.let { task ->
        AddReminderSheet(
            chores = emptyList(),
            tasks = uiState.tasks.filter { it.archivedAt == null && it.completedAt == null },
            initialTaskId = task.id,
            initialSubject = task.title,
            onSave = { insert ->
                viewModel.addReminderForTask(
                    ReminderInsert(
                        subject = insert.subject,
                        remindAt = insert.remindAt,
                        choreId = null,
                        taskId = task.id
                    )
                )
            },
            onDismiss = { reminderTargetTask = null }
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
    icon: ImageVector,
    onTap: (TaskDto) -> Unit,
    onLongPress: (TaskDto) -> Unit,
    onToggleDone: () -> Unit,
    showCategory: Boolean = true,
    showOwner: Boolean = true,
    zenMode: Boolean = false,
    highlightQuery: String? = null
) {
    val isDone = task.completedAt != null
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onToggleDone()
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
            if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.cardInset)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        if (isDone) "Restore" else "Done",
                        modifier = Modifier.padding(start = 24.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    ) {
        TaskCard(
            task = task,
            onToggleDone = onToggleDone,
            icon = icon,
            showCategory = showCategory,
            showOwner = showOwner,
            zenMode = zenMode,
            highlightQuery = highlightQuery,
            modifier = Modifier
                .semantics { role = Role.Button }
                .combinedClickable(
                    onClick = { onTap(task) },
                    onLongClick = { onLongPress(task) }
                )
        )
    }
}
