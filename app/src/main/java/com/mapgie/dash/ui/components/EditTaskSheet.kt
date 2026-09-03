package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.TaskDraft
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskDueType
import com.mapgie.dash.data.model.TaskInsert
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUpdate
import com.mapgie.dash.ui.components.sheet.DraftResumeRow
import com.mapgie.dash.ui.components.sheet.LocalDateStateSaver
import com.mapgie.dash.ui.components.sheet.NotesBlock
import com.mapgie.dash.ui.components.sheet.OwnerAvatarRow
import com.mapgie.dash.ui.components.sheet.SegmentPill
import com.mapgie.dash.ui.components.sheet.SettingsRow
import com.mapgie.dash.ui.components.sheet.SheetBlock
import com.mapgie.dash.ui.components.sheet.SheetHeader
import com.mapgie.dash.ui.components.sheet.SheetPadding
import com.mapgie.dash.ui.components.sheet.SheetPrimaryRow
import com.mapgie.dash.ui.components.sheet.SheetRowDivider
import com.mapgie.dash.ui.components.sheet.SheetTimePickerDialog
import com.mapgie.dash.ui.components.sheet.TertiaryLink
import com.mapgie.dash.ui.components.sheet.TertiaryLinkRow
import com.mapgie.dash.ui.components.sheet.TitleField
import com.mapgie.dash.ui.components.sheet.ValueChip
import com.mapgie.dash.ui.components.sheet.ZonedDateTimeStateSaver
import com.mapgie.dash.ui.components.sheet.enumStateSaver
import com.mapgie.dash.ui.components.sheet.jsonStateSaver
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.util.CalendarShareUtils
import com.mapgie.dash.util.calendarEventForDate
import com.mapgie.dash.util.calendarEventForInstant
import com.mapgie.dash.util.calendarEventWithoutTime
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val DUE_NONE = TaskDueType.NONE
private const val DUE_DATE = TaskDueType.DATE
private const val DUE_PERIOD = TaskDueType.PERIOD

/**
 * The Edit sheet for tasks (handoff 7a), one grammar with the chore sheet: the
 * title is the input, then one grouped settings card of compact rows (Category
 * value chip · Owner avatar row · Priority segments · Due value chip · Remind
 * value chip), a soft notes block, the Cancel + sage Save footer and a centred
 * tertiary row (Add to calendar · Share · Delete). With [task] null it is the
 * New task sheet: eyebrow NEW TASK, empty title focused.
 *
 * Every dismiss vector is guarded when the sheet is dirty (LESSONS.md #27).
 * Fields survive rotation and process death (rememberSaveable) and every change
 * is mirrored to the caller through [onDraftChange]; a [draft] handed back on
 * reopen is offered at the top of the sheet, never applied on its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskSheet(
    task: TaskDto?,
    icon: ImageVector,
    owners: List<String>,
    categories: List<String>,
    onSave: (TaskInsert) -> Unit,
    onUpdate: (TaskUpdate) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    draft: TaskDraft? = null,
    onDraftChange: (TaskDraft) -> Unit = {},
    onDraftClear: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()
    val context = LocalContext.current
    val accents = LocalTypeAccents.current
    val isNew = task == null

    // The values the sheet opened with, snapshotted once so the dirty check
    // compares against what the fields actually started as (LESSONS.md #27).
    // Every field is rememberSaveable so rotation and process death keep edits.
    val opened = remember { TaskDraft.of(task) }
    var title by rememberSaveable { mutableStateOf(opened.title) }
    var notes by rememberSaveable { mutableStateOf(opened.notes) }
    var category by rememberSaveable { mutableStateOf(opened.category) }
    var priority by rememberSaveable(stateSaver = enumStateSaver<TaskPriority>()) {
        mutableStateOf(opened.priorityEnum())
    }
    var owner by rememberSaveable { mutableStateOf(opened.owner) }

    var dueType by rememberSaveable { mutableStateOf(opened.dueType) }
    var dueDate by rememberSaveable(stateSaver = LocalDateStateSaver) { mutableStateOf(opened.dueDate()) }
    var duePeriod by rememberSaveable { mutableStateOf(opened.duePeriod) }
    var showDueDatePicker by rememberSaveable { mutableStateOf(false) }
    val dueDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )

    var reminderEnabled by rememberSaveable { mutableStateOf(opened.reminderEnabled) }
    // The opened reminder is already whole minutes (TaskDraft.of), matching what
    // resolvedReminder() produces, so a stored time with seconds is not "changed".
    var reminderBase by rememberSaveable(stateSaver = ZonedDateTimeStateSaver) {
        mutableStateOf(
            opened.reminderAtEpochMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
                ?: ZonedDateTime.now().plusDays(1).withSecond(0).withNano(0)
        )
    }
    var reminderHour by rememberSaveable { mutableIntStateOf(reminderBase.hour) }
    var reminderMinute by rememberSaveable { mutableIntStateOf(reminderBase.minute) }
    var showReminderDatePicker by rememberSaveable { mutableStateOf(false) }
    var showReminderTimePicker by rememberSaveable { mutableStateOf(false) }
    val reminderPickerState = rememberDatePickerState(
        initialSelectedDateMillis = reminderBase.toInstant().toEpochMilli()
    )

    var categoryMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showNewCategory by rememberSaveable { mutableStateOf(false) }
    var dueMenuOpen by rememberSaveable { mutableStateOf(false) }
    var remindMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showShareChoice by rememberSaveable { mutableStateOf(false) }
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }

    // A stored draft is offered once, at open, and never applied on its own. The
    // offer itself is saved so a rotation does not repeat it for edits that
    // rememberSaveable already brought back.
    var offeredDraft by rememberSaveable(stateSaver = jsonStateSaver(TaskDraft.serializer())) {
        mutableStateOf(draft?.takeIf { it.differsFrom(opened) })
    }

    fun resolvedReminder(): ZonedDateTime =
        reminderBase.withHour(reminderHour).withMinute(reminderMinute).withSecond(0).withNano(0)

    fun resolvedReminderInstant(): String? {
        if (!reminderEnabled) return null
        return resolvedReminder().toInstant().toString()
    }

    val currentDraft = TaskDraft(
        title = title,
        notes = notes,
        category = category,
        owner = owner,
        priority = priority.name,
        dueType = dueType,
        dueDateEpochDay = dueDate?.toEpochDay(),
        duePeriod = duePeriod,
        reminderEnabled = reminderEnabled,
        reminderAtEpochMillis = if (reminderEnabled) resolvedReminder().toInstant().toEpochMilli() else null,
    )
    val isDirty = currentDraft.differsFrom(opened)

    // Mirror every change into the draft store while the sheet is dirty.
    LaunchedEffect(currentDraft) {
        if (isDirty) onDraftChange(currentDraft)
    }

    fun restoreDraft(restored: TaskDraft) {
        title = restored.title
        notes = restored.notes
        category = restored.category
        owner = restored.owner
        priority = restored.priorityEnum()
        dueType = restored.dueType
        dueDate = restored.dueDate()
        duePeriod = restored.duePeriod
        dueDatePickerState.selectedDateMillis = dueDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        reminderEnabled = restored.reminderEnabled
        restored.reminderAtEpochMillis?.let { millis ->
            val at = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            reminderBase = at
            reminderHour = at.hour
            reminderMinute = at.minute
            reminderPickerState.selectedDateMillis = millis
        }
        offeredDraft = null
    }

    fun forgetDraft() {
        offeredDraft = null
        if (isDirty) onDraftChange(currentDraft) else onDraftClear()
    }

    /** Nothing changed: drop this sheet's draft, but keep an offer the user has not answered. */
    fun settleDraftOnCleanDismiss() {
        val offered = offeredDraft
        if (offered != null) onDraftChange(offered) else onDraftClear()
    }

    // Guard every dismiss vector (back, scrim tap, swipe-down all funnel through
    // onDismissRequest per LESSONS.md #1/#2). If dirty, bounce the sheet back to
    // visible (sheetState.show()) instead of letting it finish hiding, so we never
    // hit the stuck-invisible-overlay bug while still warning before data loss.
    fun requestDismiss() {
        if (isDirty) {
            sheetScope.launch { sheetState.show() }
            showDiscardConfirm = true
        } else {
            settleDraftOnCleanDismiss()
            sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        }
    }

    // Material 3 builds the sheet's nested-scroll connection once per SheetState and
    // that connection keeps the onDismissRequest it was created with, so a swipe-down
    // on the sheet body would run the very first requestDismiss, whose isDirty was
    // still false (LESSONS.md #45). Read the latest one through State instead.
    val latestRequestDismiss by rememberUpdatedState<() -> Unit>({ requestDismiss() })

    fun priorityString() = when (priority) {
        TaskPriority.HIGHER -> "higher"; TaskPriority.LOWER -> "lower"; TaskPriority.NORMAL -> "normal"
    }

    fun buildInsert() = TaskInsert(
        title = title.trim(),
        notes = notes.trim().ifBlank { null },
        category = category.trim().ifBlank { null },
        owner = owner.trim().ifBlank { null },
        priority = priorityString(),
        dueDate = if (dueType == DUE_DATE) dueDate?.toString() else null,
        duePeriod = if (dueType == DUE_PERIOD) duePeriod else null,
        reminderAt = resolvedReminderInstant()
    )

    fun buildUpdate() = TaskUpdate(
        title = title.trim(),
        notes = notes.trim().ifBlank { null },
        category = category.trim().ifBlank { null },
        owner = owner.trim().ifBlank { null },
        priority = priorityString(),
        dueDate = if (dueType == DUE_DATE) dueDate?.toString() else null,
        duePeriod = if (dueType == DUE_PERIOD) duePeriod else null,
        reminderAt = resolvedReminderInstant()
    )

    fun calendarInfo() = when {
        dueType == DUE_DATE && dueDate != null ->
            calendarEventForDate(title = title.trim(), description = notes.trim().ifBlank { null }, date = dueDate!!)
        reminderEnabled ->
            calendarEventForInstant(
                title = title.trim(),
                description = notes.trim().ifBlank { null },
                instant = Instant.parse(resolvedReminderInstant())
            )
        else -> calendarEventWithoutTime(title = title.trim(), description = notes.trim().ifBlank { null })
    }

    val dueChipText = when (dueType) {
        DUE_DATE -> dueDate?.format(DateTimeFormatter.ofPattern("EEE d MMM")) ?: "Pick a date"
        DUE_PERIOD -> when (duePeriod) {
            "this_week" -> "This week"
            "this_month" -> "This month"
            else -> "Today"
        }
        else -> "None"
    }
    val remindChipText = if (reminderEnabled) {
        reminderBase.withHour(reminderHour).withMinute(reminderMinute)
            .format(DateTimeFormatter.ofPattern("d MMM HH:mm"))
    } else "Off"

    ModalBottomSheet(
        onDismissRequest = { latestRequestDismiss() },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = SheetPadding)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(
                icon = icon,
                chipContainer = accents.taskContainer,
                chipContent = accents.onTaskContainer,
                eyebrow = if (isNew) "New task" else "Edit task",
            ) {
                TitleField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Task title",
                    autoFocus = isNew,
                )
            }

            offeredDraft?.let { offered ->
                DraftResumeRow(
                    itemName = task?.title ?: offered.displayName() ?: "a new task",
                    onRestore = { restoreDraft(offered) },
                    onForget = { forgetDraft() },
                )
            }

            SheetBlock {
                SettingsRow(icon = LucideIcons.LayoutGrid, label = "Category") {
                    Box {
                        ValueChip(
                            text = category.ifBlank { "None" },
                            onClick = { categoryMenuOpen = true },
                            contentDescription = "Category: ${category.ifBlank { "none" }}. Change category",
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        CategoryMenu(
                            expanded = categoryMenuOpen,
                            categories = categories,
                            onPick = { category = it; categoryMenuOpen = false },
                            onNew = { categoryMenuOpen = false; showNewCategory = true },
                            onDismiss = { categoryMenuOpen = false },
                        )
                    }
                }
                SheetRowDivider()
                SettingsRow(icon = LucideIcons.User, label = "Owner") {
                    OwnerAvatarRow(
                        owners = owners,
                        selected = owner.ifBlank { null },
                        onSelect = { owner = it ?: "" },
                    )
                }
                SheetRowDivider()
                SettingsRow(icon = LucideIcons.Zap, label = "Priority") {
                    SegmentPill(
                        options = listOf(TaskPriority.LOWER, TaskPriority.NORMAL, TaskPriority.HIGHER),
                        selected = priority,
                        label = { p ->
                            when (p) {
                                TaskPriority.LOWER -> "Low"
                                TaskPriority.NORMAL -> "Normal"
                                TaskPriority.HIGHER -> "High"
                            }
                        },
                        onSelect = { priority = it },
                    )
                }
                SheetRowDivider()
                SettingsRow(icon = LucideIcons.Calendar, label = "Due") {
                    Box {
                        ValueChip(
                            text = dueChipText,
                            onClick = { dueMenuOpen = true },
                            contentDescription = "Due: $dueChipText. Change due",
                        )
                        DropdownMenu(expanded = dueMenuOpen, onDismissRequest = { dueMenuOpen = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { dueType = DUE_NONE; dueMenuOpen = false })
                            DropdownMenuItem(
                                text = { Text("Pick a date…") },
                                onClick = { dueMenuOpen = false; showDueDatePicker = true },
                            )
                            listOf("today" to "Today", "this_week" to "This week", "this_month" to "This month")
                                .forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { dueType = DUE_PERIOD; duePeriod = key; dueMenuOpen = false },
                                    )
                                }
                        }
                    }
                }
                SheetRowDivider()
                SettingsRow(icon = LucideIcons.Bell, label = "Remind") {
                    Box {
                        ValueChip(
                            text = remindChipText,
                            onClick = { remindMenuOpen = true },
                            contentDescription = "Reminder: $remindChipText. Change reminder",
                            content = if (reminderEnabled) MaterialTheme.colorScheme.onSurface
                                      else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DropdownMenu(expanded = remindMenuOpen, onDismissRequest = { remindMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Off") },
                                onClick = { reminderEnabled = false; remindMenuOpen = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Pick date and time…") },
                                onClick = { remindMenuOpen = false; showReminderDatePicker = true },
                            )
                        }
                    }
                }
            }

            NotesBlock(
                value = notes,
                onValueChange = { notes = it },
                placeholder = "Notes",
            )

            SheetPrimaryRow(
                actionLabel = "Save",
                actionEnabled = title.isNotBlank(),
                onCancel = { requestDismiss() },
                onAction = {
                    onDraftClear()
                    if (task == null) onSave(buildInsert())
                    else onUpdate(buildUpdate())
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
            )

            TertiaryLinkRow(
                links = listOfNotNull(
                    TertiaryLink(
                        icon = LucideIcons.Calendar, label = "Add to calendar",
                        onClick = { context.startActivity(CalendarShareUtils.buildAddToCalendarIntent(calendarInfo())) },
                    ),
                    TertiaryLink(icon = LucideIcons.Share, label = "Share", onClick = { showShareChoice = true }),
                    if (task != null && onDelete != null) TertiaryLink(
                        icon = LucideIcons.Trash, label = "Delete",
                        onClick = { showDeleteConfirm = true },
                        destructive = true,
                    ) else null,
                ),
            )
        }
    }

    if (showNewCategory) {
        NewCategoryDialog(
            onCreate = { category = it; showNewCategory = false },
            onDismiss = { showNewCategory = false },
        )
    }

    if (showDueDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDatePickerState.selectedDateMillis?.let { millis ->
                        dueDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        dueType = DUE_DATE
                    }
                    showDueDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = dueDatePickerState) }
    }

    if (showReminderDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    reminderPickerState.selectedDateMillis?.let { millis ->
                        reminderBase = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .withHour(reminderHour).withMinute(reminderMinute).withSecond(0).withNano(0)
                    }
                    showReminderDatePicker = false
                    showReminderTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = reminderPickerState) }
    }

    if (showReminderTimePicker) {
        SheetTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onConfirm = { h, m ->
                reminderHour = h
                reminderMinute = m
                reminderEnabled = true
                showReminderTimePicker = false
            },
            onDismiss = { showReminderTimePicker = false }
        )
    }

    if (showShareChoice) {
        AlertDialog(
            onDismissRequest = { showShareChoice = false },
            title = { Text("Share task") },
            text = { Text("Choose how to share “${title.trim()}”.") },
            confirmButton = {
                TextButton(onClick = {
                    showShareChoice = false
                    context.startActivity(CalendarShareUtils.buildShareIcsIntent(context, calendarInfo()))
                }) { Text("As calendar event") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showShareChoice = false
                    context.startActivity(CalendarShareUtils.buildSharePlainTextIntent(calendarInfo()))
                }) { Text("As plain text") }
            }
        )
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete task?") },
            text = { Text("“${title.trim()}” and its reminders will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDraftClear()
                    onDelete()
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDiscardConfirm) {
        DiscardChangesDialog(
            itemName = task?.title ?: title.trim().ifBlank { null },
            onKeepEditing = { showDiscardConfirm = false },
            onDiscard = {
                showDiscardConfirm = false
                onDraftClear()
                sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        )
    }
}
