package com.mapgie.dash.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.ui.components.core.LocalReminderLabel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private sealed class LinkedItem {
    abstract val label: String

    object None : LinkedItem() {
        override val label: String = "(none)"
    }

    data class ChoreLink(val id: String, override val label: String) : LinkedItem()
    data class TaskLink(val id: String, override val label: String) : LinkedItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderSheet(
    chores: List<Chore>,
    tasks: List<TaskDto>,
    existing: ReminderDto? = null,
    initialChoreId: String? = null,
    initialTaskId: String? = null,
    initialSubject: String? = null,
    onSave: (ReminderInsert) -> Unit,
    onArchiveToggle: ((archived: Boolean) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    // "Reminder", "Alarm" or "Memo": whatever the user calls the feature.
    val featureWord = LocalReminderLabel.current.singular

    val initialSubjectValue = remember { existing?.subject ?: initialSubject ?: "" }
    var subject by remember { mutableStateOf(initialSubjectValue) }

    // Normalized to whole minutes so it matches remindBase's own truncation once the
    // date/time picker touches it (see showDatePicker confirm below), otherwise a
    // stored remindAt with non-zero seconds would look "changed" after a no-op picker use.
    val initialRemind = remember {
        (existing?.remindAtInstant()?.atZone(ZoneId.systemDefault()) ?: ZonedDateTime.now().plusDays(1))
            .withSecond(0).withNano(0)
    }
    var remindBase by remember { mutableStateOf(initialRemind) }
    var remindHour by remember { mutableIntStateOf(initialRemind.hour) }
    var remindMinute by remember { mutableIntStateOf(initialRemind.minute) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = remindBase.toInstant().toEpochMilli()
    )

    val linkOptions = remember(chores, tasks) {
        listOf<LinkedItem>(LinkedItem.None) +
            chores.map { LinkedItem.ChoreLink(it.id, "Chore: ${it.label}") } +
            tasks.map { LinkedItem.TaskLink(it.id, "Task: ${it.title}") }
    }
    val initialLinkedItem = remember {
        when {
            existing?.choreId != null -> linkOptions.filterIsInstance<LinkedItem.ChoreLink>()
                .find { it.id == existing.choreId }
            existing?.taskId != null -> linkOptions.filterIsInstance<LinkedItem.TaskLink>()
                .find { it.id == existing.taskId }
            initialChoreId != null -> linkOptions.filterIsInstance<LinkedItem.ChoreLink>()
                .find { it.id == initialChoreId }
            initialTaskId != null -> linkOptions.filterIsInstance<LinkedItem.TaskLink>()
                .find { it.id == initialTaskId }
            else -> null
        } ?: LinkedItem.None
    }
    var linkedItem by remember { mutableStateOf(initialLinkedItem) }
    var linkExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val displayDateTime = remindBase.withHour(remindHour).withMinute(remindMinute)
    val displayDate = displayDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    val displayTime = displayDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

    fun resolvedRemindAt(): String =
        remindBase.withHour(remindHour).withMinute(remindMinute).withSecond(0).withNano(0)
            .toInstant().toString()

    fun buildInsert() = ReminderInsert(
        subject = subject.trim(),
        remindAt = resolvedRemindAt(),
        choreId = (linkedItem as? LinkedItem.ChoreLink)?.id,
        taskId = (linkedItem as? LinkedItem.TaskLink)?.id
    )

    val isDirty = subject != initialSubjectValue ||
        displayDateTime != initialRemind ||
        linkedItem != initialLinkedItem

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

    // Material 3 builds the sheet's nested-scroll connection once per SheetState and
    // that connection keeps the onDismissRequest it was created with, so a swipe-down
    // on the sheet body would run the very first requestDismiss, whose isDirty was
    // still false (LESSONS.md #45). Read the latest one through State instead.
    val latestRequestDismiss by rememberUpdatedState<() -> Unit>({ requestDismiss() })

    ModalBottomSheet(
        onDismissRequest = { latestRequestDismiss() },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existing != null) "Edit $featureWord" else "New $featureWord",
                style = MaterialTheme.typography.titleLarge
            )

            if (existing != null) {
                ReminderMetadata(existing = existing, displayDate = displayDate, displayTime = displayTime)
            }

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Remind me on",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(displayDate)
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(displayTime)
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = linkExpanded,
                onExpandedChange = { linkExpanded = it }
            ) {
                OutlinedTextField(
                    value = linkedItem.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Related chore or task (optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = linkExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = linkExpanded,
                    onDismissRequest = { linkExpanded = false }
                ) {
                    linkOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = { linkedItem = option; linkExpanded = false }
                        )
                    }
                }
            }

            Button(
                enabled = subject.isNotBlank(),
                onClick = {
                    onSave(buildInsert())
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth(),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(if (existing != null) "Save" else "Add $featureWord")
            }

            if (existing != null && (onArchiveToggle != null || onDelete != null)) {
                HorizontalDivider()
                val isArchived = existing.archivedAt != null
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onArchiveToggle != null) {
                        TextButton(
                            onClick = {
                                sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                                    onArchiveToggle(!isArchived)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isArchived) "Unarchive" else "Archive")
                        }
                    }
                    if (onDelete != null) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${featureWord.lowercase()}?") },
            text = { Text("This ${featureWord.lowercase()} will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDelete?.invoke() }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        remindBase = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .withHour(remindHour).withMinute(remindMinute)
                            .withSecond(0).withNano(0)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = remindHour,
            initialMinute = remindMinute,
            is24Hour = is24Hour,
            onConfirm = { h, m ->
                remindHour = h
                remindMinute = m
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showDiscardConfirm) {
        DiscardChangesDialog(
            onKeepEditing = { showDiscardConfirm = false },
            onDiscard = {
                showDiscardConfirm = false
                sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        )
    }
}

@Composable
private fun ReminderMetadata(existing: ReminderDto, displayDate: String, displayTime: String) {
    val createdDate = remember(existing.createdAt) {
        existing.createdAt.takeIf { it.isNotEmpty() }?.let { raw ->
            runCatching {
                Instant.parse(raw).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            }.getOrNull()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "Scheduled for $displayDate at $displayTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (createdDate != null) {
                Text(
                    "Created $createdDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (existing.archivedAt != null) {
                Text(
                    "Archived",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = { TimePicker(state = state) }
    )
}
