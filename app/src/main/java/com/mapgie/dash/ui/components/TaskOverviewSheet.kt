package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.priorityEnum
import com.mapgie.dash.data.model.urgency
import com.mapgie.dash.ui.components.core.StatusBadge
import com.mapgie.dash.ui.components.sheet.DoneWhen
import com.mapgie.dash.ui.components.sheet.DoneWhenControl
import com.mapgie.dash.ui.components.sheet.NotesReadBlock
import com.mapgie.dash.ui.components.sheet.SheetHeader
import com.mapgie.dash.ui.components.sheet.SheetPadding
import com.mapgie.dash.ui.components.sheet.SheetPrimaryRow
import com.mapgie.dash.ui.components.sheet.SheetTimePickerDialog
import com.mapgie.dash.ui.components.sheet.UtilityAction
import com.mapgie.dash.ui.components.sheet.UtilityRow
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.StatusTone
import com.mapgie.dash.ui.theme.badgeContainerColor
import com.mapgie.dash.ui.theme.statusTone
import com.mapgie.dash.ui.theme.textColor
import com.mapgie.dash.util.CalendarShareUtils
import com.mapgie.dash.util.calendarEventForDate
import com.mapgie.dash.util.calendarEventForInstant
import com.mapgie.dash.util.calendarEventWithoutTime
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * The task Done sheet (handoff 6a), the same anatomy as the chore Log sheet:
 * header (category chip · eyebrow · title · due badge and meta · owner), the
 * DONE control, Cancel + sage "Mark done", the utility row (Calendar · Pin ·
 * Remind · Edit) and the NOTES block when there are notes. A task that is
 * already done offers "Restore" instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOverviewSheet(
    task: TaskDto,
    icon: ImageVector,
    isPinned: Boolean,
    sheetState: SheetState,
    onMarkDone: (TaskDto, Instant?) -> Unit,
    onRestore: (TaskDto) -> Unit,
    onTogglePin: (TaskDto) -> Unit,
    onAddReminder: (TaskDto) -> Unit,
    onEdit: (TaskDto) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetScope = rememberCoroutineScope()
    val context = LocalContext.current
    val tokens = LocalDashTokens.current
    val accents = LocalTypeAccents.current
    val isDone = task.completedAt != null
    val tone = task.statusTone()

    var doneWhen by remember { mutableStateOf(DoneWhen.JUST_NOW) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedHour by remember { mutableIntStateOf(LocalTime.now().hour) }
    var selectedMinute by remember { mutableIntStateOf(LocalTime.now().minute) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun hideAndDismiss() {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    fun hideThen(action: () -> Unit) {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    val signalling = tone != StatusTone.NEUTRAL && tone != StatusTone.NONE
    val chipContainer = if (signalling) tone.badgeContainerColor()!! else accents.taskContainer
    val chipContent = if (signalling) tone.textColor() else accents.onTaskContainer

    ModalBottomSheet(
        onDismissRequest = { hideAndDismiss() },
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SheetHeader(
                icon = icon,
                chipContainer = chipContainer,
                chipContent = chipContent,
                eyebrow = listOfNotNull(
                    task.category?.takeIf { it.isNotBlank() } ?: "task",
                    priorityLabel(task),
                ).joinToString(" · "),
                ownerHandle = task.owner,
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    dueBadgeText(task)?.let { StatusBadge(text = it, tone = if (isDone) StatusTone.NEUTRAL else tone) }
                    Text(
                        text = metaText(task),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = tokens.inkFaint,
                    )
                }
            }

            if (!isDone) {
                DoneWhenControl(
                    selected = doneWhen,
                    onSelect = { option ->
                        doneWhen = option
                        when (option) {
                            DoneWhen.JUST_NOW -> Unit
                            DoneWhen.EARLIER_TODAY -> {
                                selectedDate = LocalDate.now()
                                showTimePicker = true
                            }
                            DoneWhen.PICK -> showDatePicker = true
                        }
                    },
                )
            }

            SheetPrimaryRow(
                actionLabel = if (isDone) "Restore" else "Mark done",
                actionIcon = if (isDone) LucideIcons.Undo else LucideIcons.Check,
                onCancel = { hideAndDismiss() },
                onAction = {
                    if (isDone) {
                        hideThen { onRestore(task) }
                    } else {
                        val at = if (doneWhen == DoneWhen.JUST_NOW) null else {
                            selectedDate.atTime(selectedHour, selectedMinute)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                        }
                        hideThen { onMarkDone(task, at) }
                    }
                },
            )

            UtilityRow(
                actions = listOf(
                    UtilityAction(
                        icon = LucideIcons.Calendar, label = "Calendar", contentDescription = "Add to calendar",
                        onClick = {
                            context.startActivity(
                                CalendarShareUtils.buildAddToCalendarIntent(taskCalendarInfo(task))
                            )
                        },
                    ),
                    UtilityAction(
                        icon = if (isPinned) LucideIcons.PinFilled else LucideIcons.Pin,
                        label = "Pin",
                        contentDescription = if (isPinned) "Unpin from widget" else "Pin to widget",
                        onClick = { onTogglePin(task) },
                        active = isPinned,
                    ),
                    UtilityAction(
                        icon = LucideIcons.Bell, label = "Remind", contentDescription = "Add a reminder",
                        onClick = { hideThen { onAddReminder(task) } },
                    ),
                    UtilityAction(
                        icon = LucideIcons.Pencil, label = "Edit", contentDescription = "Edit task",
                        onClick = { hideThen { onEdit(task) } },
                    ),
                ),
            )

            task.notes?.takeIf { it.isNotBlank() }?.let { NotesReadBlock(text = it) }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        SheetTimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onConfirm = { h, m ->
                selectedHour = h
                selectedMinute = m
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

private fun priorityLabel(task: TaskDto): String = when (task.priorityEnum()) {
    TaskPriority.HIGHER -> "high"
    TaskPriority.NORMAL -> "normal"
    TaskPriority.LOWER -> "low"
}

private fun taskCalendarInfo(task: TaskDto) = when {
    task.dueDate != null -> {
        val date = runCatching { LocalDate.parse(task.dueDate) }.getOrNull()
        if (date != null) calendarEventForDate(task.title, task.notes, date)
        else calendarEventWithoutTime(task.title, task.notes)
    }
    task.reminderAt != null -> {
        val instant = runCatching { Instant.parse(task.reminderAt) }.getOrNull()
        if (instant != null) calendarEventForInstant(task.title, task.notes, instant)
        else calendarEventWithoutTime(task.title, task.notes)
    }
    else -> calendarEventWithoutTime(task.title, task.notes)
}

private val DAY_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val DONE_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM 'at' HH:mm")
private val REMINDER_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

/** "2d late", "due today", "due Thu", "due 12 Sep", "due this week", or null. */
private fun dueBadgeText(task: TaskDto): String? {
    val today = LocalDate.now(ZoneId.systemDefault())
    val date = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    return when (task.urgency()) {
        TaskUrgency.OVERDUE -> "${date?.let { ChronoUnit.DAYS.between(it, today) } ?: 1}d late"
        TaskUrgency.TODAY -> "due today"
        TaskUrgency.THIS_WEEK -> date?.let { "due ${it.format(DateTimeFormatter.ofPattern("EEE"))}" } ?: "due this week"
        TaskUrgency.LATER -> date?.let { "due ${it.format(DAY_MONTH_FORMATTER)}" } ?: "due this month"
        TaskUrgency.NONE -> null
    }
}

/** "added 12 Aug · no reminder" / "reminder 4 Sep 09:00" / "done Thu 4 Sep at 10:15". */
private fun metaText(task: TaskDto): String {
    task.completedAt?.let { raw ->
        val completedAt = runCatching { Instant.parse(raw) }.getOrNull() ?: return "done"
        return "done ${completedAt.atZone(ZoneId.systemDefault()).format(DONE_AT_FORMATTER)}"
    }
    val added = runCatching { Instant.parse(task.createdAt) }.getOrNull()
        ?.atZone(ZoneId.systemDefault())?.format(DAY_MONTH_FORMATTER)
    val reminder = task.reminderAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?.atZone(ZoneId.systemDefault())?.format(REMINDER_FORMATTER)
    return listOfNotNull(
        added?.let { "added $it" },
        reminder?.let { "reminder $it" } ?: "no reminder",
    ).joinToString(" · ")
}
