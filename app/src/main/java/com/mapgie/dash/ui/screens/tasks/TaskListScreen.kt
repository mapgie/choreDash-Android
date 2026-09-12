package com.mapgie.dash.ui.screens.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.isDone
import com.mapgie.dash.data.model.isPrivate
import com.mapgie.dash.data.model.isPrivateCategory
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.data.model.SwipeAction
import com.mapgie.dash.data.model.SwipeDirection
import com.mapgie.dash.data.model.SwipePair
import com.mapgie.dash.data.model.SwipeSubject
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.draftKeyFor
import com.mapgie.dash.data.model.TaskSortKey
import com.mapgie.dash.ui.components.AddReminderSheet
import com.mapgie.dash.ui.components.EditTaskSheet
import com.mapgie.dash.ui.components.PinWidgetChooserDialog
import com.mapgie.dash.ui.components.ReminderPermissionNudge
import com.mapgie.dash.ui.components.TaskCard
import com.mapgie.dash.ui.components.TaskOverviewSheet
import com.mapgie.dash.ui.components.core.HeaderIconButton
import com.mapgie.dash.ui.components.core.OwnerFilterButton
import com.mapgie.dash.ui.components.core.PageHeader
import com.mapgie.dash.ui.components.core.SearchRow
import com.mapgie.dash.ui.components.core.SectionHeaderRow
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.components.core.SortControls
import com.mapgie.dash.ui.components.core.SortSheet
import com.mapgie.dash.ui.components.core.SwipeActionBackground
import com.mapgie.dash.ui.components.core.toSwipeDirection
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.components.ZenRow
import com.mapgie.dash.ui.components.ZenScopeToggle
import com.mapgie.dash.data.model.OwnerFilter
import com.mapgie.dash.data.model.ZenPhrase
import com.mapgie.dash.data.model.priorityEnum
import com.mapgie.dash.data.model.urgency
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskListScreen(
    pendingAddIntent: AddMenuOption? = null,
    onPendingAddIntentConsumed: () -> Unit = {},
    onOpenReminderSettings: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showTaskSheet by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var doneExpanded by remember { mutableStateOf(false) }
    // Category groups the user has tapped to collapse (by category label). Held in
    // composition, like doneExpanded, so it is per-visit rather than persisted.
    val collapsedCategories = remember { mutableStateListOf<String>() }
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

    // Completion stays instant (a tap or a full swipe marks the task done), but a
    // brief "Undo" snackbar makes an accidental one a single tap to reverse.
    // Zen mode keeps its own affordance (the ticked row stays visible), so it is
    // left to call markDone directly.
    fun completeTaskWithUndo(task: TaskDto) {
        viewModel.markDone(task.id)
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            val result = snackbarHost.showSnackbar(
                message = "“${task.title}” marked done",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.markUndone(task.id)
        }
    }

    // Settings › Swipe actions decides what each direction does. Delete is
    // confirmed inside the card before it reaches here; archive gets an Undo.
    fun swipeTask(action: SwipeAction, task: TaskDto) {
        when (action) {
            SwipeAction.DONE ->
                if (task.completedAt != null) viewModel.markUndone(task.id) else completeTaskWithUndo(task)
            SwipeAction.ARCHIVE -> {
                viewModel.archiveTask(task.id, true)
                scope.launch {
                    snackbarHost.currentSnackbarData?.dismiss()
                    val result = snackbarHost.showSnackbar(
                        message = "“${task.title}” archived",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.archiveTask(task.id, false)
                }
            }
            SwipeAction.DELETE -> viewModel.deleteTaskWithUndo(task)
            SwipeAction.SNOOZE, SwipeAction.NONE -> Unit
        }
    }

    // The Edit sheet's target is kept by id and resolved from uiState so the open
    // sheet survives rotation and process death; it closes cleanly if the task
    // is gone once the list has loaded.
    val editingTask = editingTaskId?.let { id -> uiState.tasks.find { it.id == id } }
    LaunchedEffect(showTaskSheet, editingTaskId, editingTask, uiState.loading) {
        if (showTaskSheet && editingTaskId != null && editingTask == null && !uiState.loading) {
            showTaskSheet = false
            editingTaskId = null
        }
    }

    // Error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Undo snackbar for a swipe-deleted task.
    LaunchedEffect(uiState.recentDelete) {
        val recent = uiState.recentDelete ?: return@LaunchedEffect
        snackbarHost.currentSnackbarData?.dismiss()
        val result = snackbarHost.showSnackbar(
            message = "“${recent.task.title}” deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(recent)
        else viewModel.clearRecentDelete()
    }

    LaunchedEffect(pendingAddIntent) {
        if (pendingAddIntent == AddMenuOption.TASK) {
            editingTaskId = null
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (uiState.zenMode) MaterialTheme.colorScheme.surfaceContainerLow
                        else MaterialTheme.colorScheme.background
                    )
            ) {
                PageHeader(
                    title = if (uiState.zenMode) "zen" else "tasks",
                    accent = if (uiState.zenMode) LocalDashTokens.current.sectionCount else taskAccent,
                    actions = {
                        if (uiState.zenMode) {
                            // Zen header (3a-4): mine | all, LEAVE, then the sort arrow. The
                            // Target glyph keeps the same slot it holds outside zen (second
                            // from the right), so a second tap in the same place toggles zen
                            // back off without moving your finger.
                            if (uiState.ownerHandle.isNotBlank()) {
                                ZenScopeToggle(
                                    mine = uiState.ownerFilter == OwnerFilter.MINE,
                                    onMineChange = { mine ->
                                        viewModel.setOwnerFilter(if (mine) OwnerFilter.MINE else OwnerFilter.EVERYONE)
                                    },
                                )
                            }
                            // Zen is a mode, not a place: the same target glyph that entered it leaves it.
                            HeaderIconButton(
                                icon = LucideIcons.Target,
                                contentDescription = "Leave zen mode",
                                onClick = { viewModel.setZenMode(false) },
                                active = true,
                                activeTint = taskAccent,
                            )
                            HeaderIconButton(
                                icon = if (uiState.zenSortAscending) LucideIcons.ArrowUp else LucideIcons.ArrowDown,
                                contentDescription = if (uiState.zenSortAscending)
                                    "Sorted: most urgent first" else "Sorted: least urgent first",
                                onClick = { viewModel.setZenSort(!uiState.zenSortAscending) },
                            )
                        } else {
                            // Same row as Chores: search, owner, zen, group/flat.
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
                                spineSwatch = uiState.spineSwatchFor(task),
                                iconSwatch = uiState.iconSwatchFor(task),
                                reminderCount = uiState.activeReminderCountFor(task.id),
                                onTap = { overviewTask = it; showOverviewSheet = true },
                                onLongPress = { editingTaskId = it.id; showTaskSheet = true },
                                swipe = uiState.swipe,
                                onSwipe = { swipeTask(it, task) },
                                isPinned = task.id == uiState.pinnedTaskId,
                                isPrivate = task.isPrivate,
                                highlightQuery = query
                            )
                        }
                    }
                } else {
                    // The same permission nudge as Memos and Chores: a task's reminder
                    // rings through the same alarm path, so it needs the same grants.
                    if (!uiState.zenMode) {
                        ReminderPermissionNudge(
                            onOpenReminderSettings = onOpenReminderSettings,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
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
                            SortControls(
                                order = uiState.sort,
                                onOrderChange = { viewModel.setSort(it) },
                                onPickKey = { showSortSheet = true },
                            )
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
                                    val collapsed = cat in collapsedCategories
                                    stickyHeader(key = "group_$cat") {
                                        SectionHeaderRow(
                                            text = cat,
                                            count = tasks.size,
                                            collapsed = collapsed,
                                            icon = if (isPrivateCategory(cat)) LucideIcons.Lock else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.background)
                                                .semantics { role = Role.Button }
                                                .clickable {
                                                    if (collapsed) collapsedCategories.remove(cat)
                                                    else collapsedCategories.add(cat)
                                                }
                                                .padding(horizontal = 24.dp, vertical = 6.dp)
                                        )
                                    }
                                    if (!collapsed) items(tasks, key = { it.id }) { task ->
                                        SwipeToCompleteCard(
                                            task = task,
                                            icon = iconFor(task),
                                            spineSwatch = uiState.spineSwatchFor(task),
                                            iconSwatch = uiState.iconSwatchFor(task),
                                            reminderCount = uiState.activeReminderCountFor(task.id),
                                            onTap = { overviewTask = it; showOverviewSheet = true },
                                            onLongPress = { editingTaskId = it.id; showTaskSheet = true },
                                            swipe = uiState.swipe,
                                            onSwipe = { swipeTask(it, task) },
                                            showCategory = false,
                                            showOwner = uiState.ownerFilter.showsOwner,
                                            zenMode = uiState.zenMode,
                                            isPinned = task.id == uiState.pinnedTaskId,
                                            isPrivate = task.isPrivate,
                                        )
                                    }
                                }
                            } else if (uiState.zenMode) {
                                // Zen rows (3a-4): open circle, title, gentle cue; no colours, no counts.
                                items(uiState.zenRows, key = { it.id }) { task ->
                                    val zenDone = task.completedAt != null
                                    ZenRow(
                                        title = task.title,
                                        sub = ZenPhrase.forTask(task.category, task.urgency(), task.priorityEnum(), zenDone),
                                        done = zenDone,
                                        onToggle = {
                                            if (zenDone) viewModel.markUndone(task.id) else completeTaskWithUndo(task)
                                        },
                                        modifier = Modifier
                                            .padding(horizontal = Dimens.cardInset)
                                            .semantics { role = Role.Button }
                                            .combinedClickable(
                                                onClick = { overviewTask = task; showOverviewSheet = true },
                                                onLongClick = { editingTaskId = task.id; showTaskSheet = true }
                                            ),
                                    )
                                }
                            } else {
                                items(active, key = { it.id }) { task ->
                                    SwipeToCompleteCard(
                                        task = task,
                                        icon = iconFor(task),
                                        spineSwatch = uiState.spineSwatchFor(task),
                                        iconSwatch = uiState.iconSwatchFor(task),
                                        reminderCount = uiState.activeReminderCountFor(task.id),
                                        onTap = { overviewTask = it; showOverviewSheet = true },
                                        onLongPress = { editingTaskId = it.id; showTaskSheet = true },
                                        swipe = uiState.swipe,
                                        onSwipe = { swipeTask(it, task) },
                                        showCategory = !uiState.groupByCategory,
                                        showOwner = uiState.ownerFilter.showsOwner,
                                        zenMode = uiState.zenMode,
                                        isPinned = task.id == uiState.pinnedTaskId,
                                        isPrivate = task.isPrivate,
                                    )
                                }
                            }

                            if (done.isNotEmpty() && !uiState.zenMode) {
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
                                            spineSwatch = uiState.spineSwatchFor(task),
                                            iconSwatch = uiState.iconSwatchFor(task),
                                            reminderCount = uiState.activeReminderCountFor(task.id),
                                            onTap = { overviewTask = it; showOverviewSheet = true },
                                            onLongPress = { editingTaskId = it.id; showTaskSheet = true },
                                            swipe = uiState.swipe,
                                            onSwipe = { swipeTask(it, task) },
                                            showCategory = !uiState.groupByCategory,
                                            showOwner = uiState.ownerFilter.showsOwner,
                                            zenMode = uiState.zenMode,
                                            isPrivate = task.isPrivate,
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

    if (showSortSheet) {
        SortSheet(
            title = "Sort tasks by",
            keys = TaskSortKey.entries,
            order = uiState.sort,
            onOrderChange = { viewModel.setSort(it) },
            onDismiss = { showSortSheet = false },
        )
    }

    // Wait for a saved target to resolve rather than opening as "New task" meanwhile.
    if (showTaskSheet && (editingTaskId == null || editingTask != null)) {
        val draftKey = draftKeyFor(editingTaskId)
        EditTaskSheet(
            task = editingTask,
            icon = editingTask?.let { iconFor(it) } ?: LucideIcons.CircleCheck,
            badgeSwatch = editingTask?.let { uiState.spineSwatchFor(it) },
            iconSwatch = editingTask?.let { uiState.iconSwatchFor(it) },
            owners = uiState.owners,
            categories = uiState.categories,
            onSave = { insert -> viewModel.addTask(insert) },
            onUpdate = { update -> editingTaskId?.let { viewModel.updateTask(it, update) } },
            onDelete = { editingTask?.let { viewModel.deleteTaskWithUndo(it) } },
            onDismiss = { showTaskSheet = false; editingTaskId = null },
            draft = remember(draftKey) { viewModel.taskDrafts.get(draftKey) },
            onDraftChange = { viewModel.taskDrafts.put(draftKey, it) },
            onDraftClear = { viewModel.taskDrafts.clear(draftKey) },
        )
    }

    if (showOverviewSheet && overviewTask != null) {
        val task = overviewTask!!
        TaskOverviewSheet(
            task = task,
            icon = iconFor(task),
            badgeSwatch = uiState.spineSwatchFor(task),
            iconSwatch = uiState.iconSwatchFor(task),
            isPinned = task.id == uiState.pinnedTaskId,
            isPrivate = task.isPrivate,
            sheetState = overviewSheetState,
            reminders = uiState.reminders
                .filter { it.taskId == task.id && it.archivedAt == null && !it.isDone }
                .sortedBy { it.remindAt },
            onDeleteReminder = { viewModel.deleteTaskReminder(it.id) },
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
                editingTaskId = t.id
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
    swipe: SwipePair,
    onSwipe: (SwipeAction) -> Unit,
    showCategory: Boolean = true,
    showOwner: Boolean = true,
    zenMode: Boolean = false,
    spineSwatch: Swatch? = null,
    iconSwatch: Swatch? = null,
    reminderCount: Int = 0,
    isPinned: Boolean = false,
    isPrivate: Boolean = false,
    highlightQuery: String? = null
) {
    val isDone = task.completedAt != null
    // Each direction does what Settings › Swipe actions says (out of the box:
    // right completes, left does nothing). Delete asks first, here, so the
    // caller only ever hears about a confirmed one.
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // The dismiss state is remembered once per card, so its callback reads the
    // latest setting and handler through rememberUpdatedState (LESSONS #49).
    val currentSwipe by rememberUpdatedState(swipe)
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    fun labelFor(action: SwipeAction): String = when (action) {
        SwipeAction.DONE -> if (isDone) "Restore" else "Done"
        else -> SwipeSubject.TASKS.label(action)
    }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            value.toSwipeDirection()?.let { direction ->
                when (val action = currentSwipe.action(direction)) {
                    SwipeAction.DELETE -> showDeleteConfirm = true
                    else -> currentOnSwipe(action)
                }
            }
            false // never actually dismiss the item
        },
        // Require a deliberate swipe most of the way across the card before an
        // action registers, so a stray horizontal drag while scrolling the
        // list doesn't silently tick a task off.
        positionalThreshold = { it * 0.6f }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = swipe.enabled(SwipeDirection.RIGHT),
        enableDismissFromEndToStart = swipe.enabled(SwipeDirection.LEFT),
        backgroundContent = {
            val direction = dismissState.dismissDirection.toSwipeDirection()
            val action = direction?.let { swipe.action(it) } ?: SwipeAction.NONE
            SwipeActionBackground(direction = direction, action = action, label = labelFor(action))
        }
    ) {
        TaskCard(
            task = task,
            icon = icon,
            showCategory = showCategory,
            showOwner = showOwner,
            zenMode = zenMode,
            spineSwatch = spineSwatch,
            iconSwatch = iconSwatch,
            reminderCount = reminderCount,
            isPinned = isPinned,
            isPrivate = isPrivate,
            highlightQuery = highlightQuery,
            modifier = Modifier
                .semantics { role = Role.Button }
                .combinedClickable(
                    onClick = { onTap(task) },
                    onLongClick = { onLongPress(task) }
                )
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete task?") },
            text = { Text("“${task.title}” and its reminders will be removed. You can undo straight after.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    currentOnSwipe(SwipeAction.DELETE)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
