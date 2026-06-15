package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.remindAtInstant
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
    onSave: (ReminderInsert) -> Unit,
    onArchiveToggle: ((archived: Boolean) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()

    var subject by remember { mutableStateOf(existing?.subject ?: "") }

    val initialRemind = remember {
        existing?.remindAtInstant()?.atZone(ZoneId.systemDefault())
            ?: ZonedDateTime.now().plusDays(1).withSecond(0).withNano(0)
    }
    var remindBase by remember { mutableStateOf(initialRemind) }
    var remindHour by remember { mutableStateOf(remindBase.hour.toString().padStart(2, '0')) }
    var remindMinute by remember { mutableStateOf(remindBase.minute.toString().padStart(2, '0')) }
    var showDatePicker by remember { mutableStateOf(false) }
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
            else -> null
        } ?: LinkedItem.None
    }
    var linkedItem by remember { mutableStateOf(initialLinkedItem) }
    var linkExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun resolvedRemindAt(): String {
        val h = remindHour.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val m = remindMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return remindBase.withHour(h).withMinute(m).withSecond(0).withNano(0).toInstant().toString()
    }

    fun buildInsert() = ReminderInsert(
        subject = subject.trim(),
        remindAt = resolvedRemindAt(),
        choreId = (linkedItem as? LinkedItem.ChoreLink)?.id,
        taskId = (linkedItem as? LinkedItem.TaskLink)?.id
    )

    ModalBottomSheet(
        onDismissRequest = {
            sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (existing != null) "Edit Reminder" else "New Reminder",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text(
                    "Remind me on",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(remindBase.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = remindHour,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) remindHour = it },
                        label = { Text("HH") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = remindMinute,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) remindMinute = it },
                        label = { Text("MM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (existing != null) "Save" else "Add")
            }

            if (existing != null) {
                val isArchived = existing.archivedAt != null
                if (onArchiveToggle != null) {
                    OutlinedButton(
                        onClick = {
                            sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                                onArchiveToggle(!isArchived)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArchived) "Unarchive" else "Archive")
                    }
                }
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete reminder?") },
            text = { Text("This reminder will be permanently removed.") },
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
                        val h = remindHour.toIntOrNull()?.coerceIn(0, 23) ?: 9
                        val m = remindMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        remindBase = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .withHour(h).withMinute(m).withSecond(0).withNano(0)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}
