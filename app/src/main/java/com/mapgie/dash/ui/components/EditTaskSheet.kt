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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskInsert
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUpdate
import com.mapgie.dash.data.model.priorityEnum
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
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.util.CalendarShareUtils
import com.mapgie.dash.util.calendarEventForDate
import com.mapgie.dash.util.calendarEventForInstant
import com.mapgie.dash.util.calendarEventWithoutTime
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val DUE_NONE = "none"
private const val DUE_DATE = "date"
private const val DUE_PERIOD = "period"

/**
 * The Edit sheet for tasks (handoff 7a), one grammar with the chore sheet: the
 * title is the input, then one grouped settings card of compact rows (Category
 * value chip · Owner avatar row · Priority segments · Due value chip · Remind
 * value chip), a soft notes block, the Cancel + sage Save footer and a centred
 * tertiary row (Add to calendar · Share · Delete). With [task] null it is the
 * New task sheet: eyebrow NEW TASK, empty title focused.
 *
 * Every dismiss vector is guarded when the sheet is dirty (LESSONS.md #27).
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
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()
    val context = LocalContext.current
    val accents = LocalTypeAccents.current
    val isNew = task == null

    val initialTitle = remember { task?.title ?: "" }
    var title by remember { mutableStateOf(initialTitle) }
    val initialNotes = remember { task?.notes ?: "" }
    var notes by remember { mutableStateOf(initialNotes) }
    val initialCategory = remember { if (task != null) task.category ?: "" else DEFAULT_CATEGORY }
    var category by remember { mutableStateOf(initialCategory) }
    val initialPriority = remember { task?.priorityEnum() ?: TaskPriority.NORMAL }
    var priority by remember { mutableStateOf(initialPriority) }
    val initialOwner = remember { task?.owner ?: "" }
    var owner by remember { mutableStateOf(initialOwner) }

    val initialDueType = remember {
        when {
            task?.dueDate != null -> DUE_DATE
            task?.duePeriod != null -> DUE_PERIOD
            else -> DUE_NONE
        }
    }
    var dueType by remember { mutableStateOf(initialDueType) }
    val initialDueDate = remember {
        task?.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
    var dueDate by remember { mutableStateOf(initialDueDate) }
    val initialDuePeriod = remember { task?.duePeriod ?: "today" }
    var duePeriod by remember { mutableStateOf(initialDuePeriod) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    val dueDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )

    val initialReminderEnabled = remember { task?.reminderAt != null }
    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }
    // Normalized to whole minutes (matching resolvedReminderInstant()'s own truncation
    // below) so a stored reminderAt with non-zero seconds doesn't look "changed" on open.
    val initialReminderAt = remember {
        task?.reminderAt?.let {
            runCatching {
                Instant.parse(it).atZone(ZoneId.systemDefault()).withSecond(0).withNano(0).toInstant().toString()
            }.getOrNull()
        }
    }
    var reminderBase by remember {
        mutableStateOf(
            task?.reminderAt
                ?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()) }.getOrNull() }
                ?: ZonedDateTime.now().plusDays(1).withSecond(0).withNano(0)
        )
    }
    var reminderHour by remember { mutableIntStateOf(reminderBase.hour) }
    var reminderMinute by remember { mutableIntStateOf(reminderBase.minute) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    val reminderPickerState = rememberDatePickerState(
        initialSelectedDateMillis = reminderBase.toInstant().toEpochMilli()
    )

    var categoryMenuOpen by remember { mutableStateOf(false) }
    var showNewCategory by remember { mutableStateOf(false) }
    var dueMenuOpen by remember { mutableStateOf(false) }
    var remindMenuOpen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showShareChoice by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    fun resolvedReminderInstant(): String? {
        if (!reminderEnabled) return null
        return reminderBase.withHour(reminderHour).withMinute(reminderMinute).withSecond(0).withNano(0).toInstant().toString()
    }

    val isDirty = title != initialTitle ||
        notes != initialNotes ||
        category != initialCategory ||
        priority != initialPriority ||
        owner != initialOwner ||
        dueType != initialDueType ||
        dueDate != initialDueDate ||
        duePeriod != initialDuePeriod ||
        reminderEnabled != initialReminderEnabled ||
        (reminderEnabled && resolvedReminderInstant() != initialReminderAt)

    // Guard every dismiss vector (back, scrim tap, swipe-down all funnel through
    // onDismissRequest per LESSONS.md #1/#2). If dirty, bounce the sheet back to
    // visible (sheetState.show()) instead of letting it finish hiding, so we never
    // hit the stuck-invisible-overlay bug while still warning before data loss.
    fun requestDismiss() {
        if (isDirty) {
            sheetScope.launch { sheetState.show() }
            showDiscardConfirm = true
        } else {
            sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        }
    }

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
        onDismissRequest = { requestDismiss() },
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
                sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        )
    }
}
