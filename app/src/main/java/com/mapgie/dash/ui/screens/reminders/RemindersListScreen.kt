package com.mapgie.dash.ui.screens.reminders

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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.NEW_DRAFT_KEY
import com.mapgie.dash.data.model.ReminderAppearance
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderSortKey
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.data.model.SwipeAction
import com.mapgie.dash.data.model.SwipeDirection
import com.mapgie.dash.data.model.SwipePair
import com.mapgie.dash.data.model.SwipeSubject
import com.mapgie.dash.data.model.isDone
import com.mapgie.dash.data.model.isTagAlarm
import com.mapgie.dash.ui.components.AddReminderSheet
import com.mapgie.dash.ui.components.ReminderCard
import com.mapgie.dash.ui.components.TagAlarmConflictDialog
import com.mapgie.dash.ui.components.WriteTagDialog
import com.mapgie.dash.nfc.NfcWriteResult
import com.mapgie.dash.ui.components.core.HeaderIconButton
import com.mapgie.dash.ui.components.core.LocalReminderLabel
import com.mapgie.dash.ui.components.core.PageHeader
import com.mapgie.dash.ui.components.core.SearchRow
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.components.core.SortControls
import com.mapgie.dash.ui.components.core.SortSheet
import com.mapgie.dash.ui.components.core.SwipeActionBackground
import com.mapgie.dash.ui.components.core.toSwipeDirection
import com.mapgie.dash.ui.components.ReminderPermissionNudge
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.PillShape
import kotlinx.coroutines.launch

/**
 * The Memos tab (handoff 9a), on the same chrome as Chores and Tasks: the serif
 * header with the search action, the filter-chip row (Active · N / Done / All,
 * Active by default) ending in the sort pill, and a flat list of shared-format
 * cards. There is no Archived tab; archiving lives in the edit sheet and archived
 * memos show, muted, under All.
 *
 * The owner ("mine / all") header action the design shows is not built: memos
 * carry no owner, so there is nothing to filter by.
 *
 * Above the filter row sits the shared [ReminderPermissionNudge]: a banner shown
 * whenever a system grant the chosen notification style depends on is missing
 * (full-screen alarms for the Alarm style, say), since a memo that fires silently
 * is worse than none. The full-screen case taps straight to that system toggle;
 * anything else opens Settings › Reminders & alerts via [onOpenReminderSettings].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersListScreen(
    pendingAddIntent: AddMenuOption?,
    onPendingAddIntentConsumed: () -> Unit,
    nfcCapturedTagId: String? = null,
    onStartNfcCapture: () -> Unit = {},
    onCancelNfcCapture: () -> Unit = {},
    onNfcCaptureConsumed: () -> Unit = {},
    /** A tag-alarm's "Write tag" is waiting for a tag (or has its result); shows the write dialog. */
    memoTagWritePending: Boolean = false,
    nfcWriteResult: NfcWriteResult? = null,
    onStartMemoTagWrite: (tagId: String) -> Unit = {},
    onCancelNfcWrite: () -> Unit = {},
    onNfcWriteResultConsumed: () -> Unit = {},
    onOpenReminderSettings: () -> Unit,
    viewModel: RemindersListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Swipe-to-done leaves a brief Undo, so a stray swipe is one tap to reverse.
    // On a tag-alarm the swipe turns it off, and Undo arms it again.
    fun markReminderDoneWithUndo(reminder: ReminderDto) {
        viewModel.setReminderDone(reminder.id, true)
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            val result = snackbarHost.showSnackbar(
                message = if (reminder.isTagAlarm) "“${reminder.subject}” turned off"
                          else "“${reminder.subject}” marked done",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.setReminderDone(reminder.id, false)
        }
    }

    // Settings › Swipe actions decides what each direction does. Delete is
    // confirmed inside the card before it reaches here; snooze and archive
    // each leave an Undo like Done does.
    fun swipeReminder(action: SwipeAction, reminder: ReminderDto) {
        when (action) {
            SwipeAction.DONE ->
                if (!reminder.isTagAlarm && reminder.isDone) viewModel.setReminderDone(reminder.id, false)
                else markReminderDoneWithUndo(reminder)
            SwipeAction.SNOOZE -> {
                if (reminder.isTagAlarm) return
                viewModel.snoozeReminder(reminder.id)
                scope.launch {
                    snackbarHost.currentSnackbarData?.dismiss()
                    val result = snackbarHost.showSnackbar(
                        message = "“${reminder.subject}” snoozed for an hour",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreReminder(reminder)
                }
            }
            SwipeAction.ARCHIVE -> {
                val restoring = reminder.archivedAt != null
                viewModel.archiveReminder(reminder.id, !restoring)
                scope.launch {
                    snackbarHost.currentSnackbarData?.dismiss()
                    val result = snackbarHost.showSnackbar(
                        message = if (restoring) "“${reminder.subject}” restored" else "“${reminder.subject}” archived",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.archiveReminder(reminder.id, restoring)
                }
            }
            SwipeAction.DELETE -> viewModel.deleteReminder(reminder.id)
            SwipeAction.NONE -> Unit
        }
    }

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // The id, not the record, so the open sheet survives process death and
    // always shows the freshest copy after a reload.
    var editTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    // Why the pending tag write is happening (a renamed tag), for the write dialog.
    var writeNote by rememberSaveable { mutableStateOf<String?>(null) }

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
                        val look = reminderAppearance(reminder, uiState)
                        ReminderCard(
                            reminder = reminder,
                            linkedTo = uiState.linkedTo(reminder),
                            onClick = { editTargetId = reminder.id },
                            highlightQuery = query,
                            icon = look.glyph(),
                            spineSwatch = look.spineSwatch,
                            iconSwatch = look.iconSwatch,
                        )
                    }
                }
                return@Scaffold
            }

            ReminderPermissionNudge(
                onOpenReminderSettings = onOpenReminderSettings,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            )

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
                SortControls(
                    order = uiState.sort,
                    onOrderChange = { viewModel.setSort(it) },
                    onPickKey = { showSortSheet = true },
                )
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
                    val look = reminderAppearance(reminder, uiState)
                    SwipeReminderCard(
                        reminder = reminder,
                        linkedTo = uiState.linkedTo(reminder),
                        icon = look.glyph(),
                        spineSwatch = look.spineSwatch,
                        iconSwatch = look.iconSwatch,
                        onClick = { editTargetId = reminder.id },
                        swipe = uiState.swipe,
                        onSwipe = { swipeReminder(it, reminder) },
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
            onDismiss = { showAddSheet = false },
            draft = remember { viewModel.reminderDrafts.get(NEW_DRAFT_KEY) },
            onDraftChange = { viewModel.reminderDrafts.put(NEW_DRAFT_KEY, it) },
            onDraftClear = { viewModel.reminderDrafts.clear(NEW_DRAFT_KEY) },
            takenTagIds = uiState.takenTagIds(),
            scannedTagId = nfcCapturedTagId,
            onStartScan = onStartNfcCapture,
            onCancelScan = onCancelNfcCapture,
            onScanConsumed = onNfcCaptureConsumed,
            onWriteTag = { tagId, note -> writeNote = note; onStartMemoTagWrite(tagId) },
        )
    }

    val editTarget = editTargetId?.let { id -> uiState.reminders.find { it.id == id } }
    editTarget?.let { reminder ->
        AddReminderSheet(
            chores = uiState.chores,
            tasks = uiState.tasks,
            existing = reminder,
            onSave = { insert -> viewModel.editReminder(reminder.id, insert) },
            onArchiveToggle = { archived -> viewModel.archiveReminder(reminder.id, archived) },
            onDelete = { viewModel.deleteReminder(reminder.id) },
            onDismiss = { editTargetId = null },
            draft = remember(reminder.id) { viewModel.reminderDrafts.get(reminder.id) },
            onDraftChange = { viewModel.reminderDrafts.put(reminder.id, it) },
            onDraftClear = { viewModel.reminderDrafts.clear(reminder.id) },
            takenTagIds = uiState.takenTagIds(editingId = reminder.id),
            scannedTagId = nfcCapturedTagId,
            onStartScan = onStartNfcCapture,
            onCancelScan = onCancelNfcCapture,
            onScanConsumed = onNfcCaptureConsumed,
            onArmTagAlarm = { viewModel.armTagAlarm(reminder.id) },
            onDisarmTagAlarm = { viewModel.disarmTagAlarm(reminder.id) },
            onWriteTag = { tagId, note -> writeNote = note; onStartMemoTagWrite(tagId) },
        )
    }

    if (memoTagWritePending) {
        WriteTagDialog(
            result = nfcWriteResult,
            note = writeNote,
            onDismiss = {
                writeNote = null
                if (nfcWriteResult != null) onNfcWriteResultConsumed() else onCancelNfcWrite()
            }
        )
    }

    if (uiState.tagAlarmConflicts.isNotEmpty()) {
        TagAlarmConflictDialog(
            conflicts = uiState.tagAlarmConflicts,
            onTurnOff = { viewModel.resolveTagAlarmConflicts(true) },
            onKeep = { viewModel.resolveTagAlarmConflicts(false) },
        )
    }
}

/** The colour and glyph a memo's card wears: inherited from its link, or its own pick. */
private fun reminderAppearance(reminder: ReminderDto, uiState: ReminderUiState): ReminderAppearance =
    ReminderAppearance.of(reminder, uiState.linkedCategory(reminder), uiState.catalog, uiState.colourAxes)

/** The memo's chip glyph: its resolved category icon, or the default bell. */
private fun ReminderAppearance.glyph(): ImageVector =
    icon?.let { LucideIcons.forCategory(it) } ?: LucideIcons.Bell

/**
 * A memo card whose two swipes do what Settings › Swipe actions says (out of
 * the box: left marks it done or turns a tag-alarm off, right deletes behind a
 * confirm). Delete is the only action confirmed here; the others are reversible
 * from the Undo snackbar the caller shows. A tag-alarm has no snooze, so that
 * direction is turned off on its card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeReminderCard(
    reminder: ReminderDto,
    linkedTo: String?,
    icon: ImageVector,
    spineSwatch: Swatch?,
    iconSwatch: Swatch?,
    onClick: () -> Unit,
    swipe: SwipePair,
    onSwipe: (SwipeAction) -> Unit,
) {
    val effective = if (reminder.isTagAlarm) swipe.without(SwipeAction.SNOOZE) else swipe
    // The dismiss state is remembered once per card, so its callback reads the
    // latest setting and handler through rememberUpdatedState (LESSONS #49).
    val currentEffective by rememberUpdatedState(effective)
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    fun labelFor(action: SwipeAction): String = when (action) {
        SwipeAction.DONE -> when {
            reminder.isTagAlarm -> "Turn off"
            reminder.isDone -> "Restore"
            else -> "Done"
        }
        SwipeAction.ARCHIVE -> if (reminder.archivedAt != null) "Restore" else "Archive"
        else -> SwipeSubject.MEMOS.label(action)
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            value.toSwipeDirection()?.let { direction ->
                when (val action = currentEffective.action(direction)) {
                    SwipeAction.DELETE -> showDeleteConfirm = true
                    else -> currentOnSwipe(action)
                }
            }
            false // never actually dismiss the item
        },
        positionalThreshold = { it * 0.4f }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = effective.enabled(SwipeDirection.RIGHT),
        enableDismissFromEndToStart = effective.enabled(SwipeDirection.LEFT),
        backgroundContent = {
            val direction = dismissState.dismissDirection.toSwipeDirection()
            val action = direction?.let { effective.action(it) } ?: SwipeAction.NONE
            SwipeActionBackground(direction = direction, action = action, label = labelFor(action))
        }
    ) {
        ReminderCard(
            reminder = reminder,
            linkedTo = linkedTo,
            onClick = onClick,
            icon = icon,
            spineSwatch = spineSwatch,
            iconSwatch = iconSwatch,
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
                    currentOnSwipe(SwipeAction.DELETE)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
