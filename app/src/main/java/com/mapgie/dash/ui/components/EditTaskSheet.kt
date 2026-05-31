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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskInsert
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUpdate
import com.mapgie.dash.data.model.priorityEnum
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskSheet(
    task: TaskDto?,
    owners: List<String>,
    categories: List<String>,
    onSave: (TaskInsert) -> Unit,
    onUpdate: (TaskUpdate) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(task?.title ?: "") }
    var notes by remember { mutableStateOf(task?.notes ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "") }
    var priority by remember { mutableStateOf(task?.priorityEnum() ?: TaskPriority.NORMAL) }
    var owner by remember { mutableStateOf(task?.owner ?: "") }

    var dueType by remember {
        mutableStateOf(
            when {
                task?.dueDate != null -> "date"
                task?.duePeriod != null -> "period"
                else -> "none"
            }
        )
    }
    var dueDate by remember {
        mutableStateOf(
            task?.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        )
    }
    var duePeriod by remember { mutableStateOf(task?.duePeriod ?: "today") }
    var showDueDatePicker by remember { mutableStateOf(false) }
    val dueDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )

    var reminderEnabled by remember { mutableStateOf(task?.reminderAt != null) }
    var reminderBase by remember {
        mutableStateOf(
            task?.reminderAt
                ?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()) }.getOrNull() }
                ?: ZonedDateTime.now().plusDays(1).withSecond(0).withNano(0)
        )
    }
    var reminderHour by remember { mutableStateOf(reminderBase.hour.toString().padStart(2, '0')) }
    var reminderMinute by remember { mutableStateOf(reminderBase.minute.toString().padStart(2, '0')) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    val reminderPickerState = rememberDatePickerState(
        initialSelectedDateMillis = reminderBase.toInstant().toEpochMilli()
    )

    var ownerExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun resolvedReminderInstant(): String? {
        if (!reminderEnabled) return null
        val h = reminderHour.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val m = reminderMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return reminderBase.withHour(h).withMinute(m).withSecond(0).withNano(0).toInstant().toString()
    }

    fun buildInsert() = TaskInsert(
        title = title.trim(),
        notes = notes.trim().ifBlank { null },
        category = category.trim().ifBlank { null },
        owner = owner.trim().ifBlank { null },
        priority = when (priority) {
            TaskPriority.HIGHER -> "higher"; TaskPriority.LOWER -> "lower"; TaskPriority.NORMAL -> "normal"
        },
        dueDate = if (dueType == "date") dueDate?.toString() else null,
        duePeriod = if (dueType == "period") duePeriod else null,
        reminderAt = resolvedReminderInstant()
    )

    fun buildUpdate() = TaskUpdate(
        title = title.trim(),
        notes = notes.trim().ifBlank { null },
        category = category.trim().ifBlank { null },
        owner = owner.trim().ifBlank { null },
        priority = when (priority) {
            TaskPriority.HIGHER -> "higher"; TaskPriority.LOWER -> "lower"; TaskPriority.NORMAL -> "normal"
        },
        dueDate = if (dueType == "date") dueDate?.toString() else null,
        duePeriod = if (dueType == "period") duePeriod else null,
        reminderAt = resolvedReminderInstant()
    )

    ModalBottomSheet(
        onDismissRequest = { sheetScope.launch { sheetState.show() } },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(
            isFocusable = true,
            shouldDismissOnBackPress = false
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
                text = if (task == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it; if (it.isNotBlank()) categoryExpanded = true },
                    label = { Text("Category") },
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                val filtered = categories.filter { it.contains(category, ignoreCase = true) }
                if (filtered.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        filtered.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { category = cat; categoryExpanded = false }
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    "Priority",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TaskPriority.HIGHER to "Higher",
                        TaskPriority.NORMAL to "Normal",
                        TaskPriority.LOWER to "Lower"
                    ).forEach { (p, label) ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Column {
                Text(
                    "Due",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("none" to "None", "date" to "Date", "period" to "Period").forEach { (key, label) ->
                        FilterChip(
                            selected = dueType == key,
                            onClick = { dueType = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                if (dueType == "date") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDueDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            dueDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Pick a date"
                        )
                    }
                }
                if (dueType == "period") {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "today" to "Today",
                            "this_week" to "This week",
                            "this_month" to "This month"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = duePeriod == key,
                                onClick = { duePeriod = key },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                )
                            )
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = ownerExpanded,
                onExpandedChange = { ownerExpanded = it }
            ) {
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Owner") },
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ownerExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                if (owners.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = ownerExpanded,
                        onDismissRequest = { ownerExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("(unassigned)") },
                            onClick = { owner = ""; ownerExpanded = false }
                        )
                        owners.forEach { o ->
                            DropdownMenuItem(
                                text = { Text(o) },
                                onClick = { owner = o; ownerExpanded = false }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Reminder",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
                if (reminderEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showReminderDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(reminderBase.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = reminderHour,
                            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) reminderHour = it },
                            label = { Text("HH") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = reminderMinute,
                            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) reminderMinute = it },
                            label = { Text("MM") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (task != null && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            if (showDeleteConfirm) {
                                onDelete()
                                sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            } else {
                                showDeleteConfirm = true
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (showDeleteConfirm) "Confirm delete" else "Delete")
                    }
                }
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        if (task == null) onSave(buildInsert())
                        else onUpdate(buildUpdate())
                        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (task == null) "Add" else "Save")
                }
            }
        }
    }

    if (showDueDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDatePickerState.selectedDateMillis?.let { millis ->
                        dueDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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
                        val h = reminderHour.toIntOrNull()?.coerceIn(0, 23) ?: 9
                        val m = reminderMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        reminderBase = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .withHour(h).withMinute(m).withSecond(0).withNano(0)
                    }
                    showReminderDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = reminderPickerState) }
    }
}
