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
import androidx.compose.material3.Button
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
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.TaskDto
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
    onSave: (ReminderInsert) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()

    var subject by remember { mutableStateOf("") }

    var remindBase by remember {
        mutableStateOf(ZonedDateTime.now().plusDays(1).withSecond(0).withNano(0))
    }
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
    var linkedItem by remember { mutableStateOf<LinkedItem>(LinkedItem.None) }
    var linkExpanded by remember { mutableStateOf(false) }

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
                text = "New Reminder",
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
                Text("Add")
            }
        }
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
