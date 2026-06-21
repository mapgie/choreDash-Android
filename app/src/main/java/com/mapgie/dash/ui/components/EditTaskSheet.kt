package com.mapgie.dash.ui.components

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskInsert
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUpdate
import com.mapgie.dash.data.model.priorityEnum
import com.mapgie.dash.util.CalendarShareUtils
import com.mapgie.dash.util.calendarEventForInstant
import com.mapgie.dash.util.calendarEventForDate
import com.mapgie.dash.util.calendarEventWithoutTime
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
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    var showShareChoice by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
            else true
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                else true
                hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
                else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    var showPermissionDialog by remember { mutableStateOf(false) }

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
    var reminderHour by remember { mutableIntStateOf(reminderBase.hour) }
    var reminderMinute by remember { mutableIntStateOf(reminderBase.minute) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    val reminderPickerState = rememberDatePickerState(
        initialSelectedDateMillis = reminderBase.toInstant().toEpochMilli()
    )

    var ownerExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun resolvedReminderInstant(): String? {
        if (!reminderEnabled) return null
        return reminderBase.withHour(reminderHour).withMinute(reminderMinute).withSecond(0).withNano(0).toInstant().toString()
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

    fun calendarInfo() = when {
        dueType == "date" && dueDate != null ->
            calendarEventForDate(title = title.trim(), description = notes.trim().ifBlank { null }, date = dueDate!!)
        reminderEnabled ->
            calendarEventForInstant(
                title = title.trim(),
                description = notes.trim().ifBlank { null },
                instant = Instant.parse(resolvedReminderInstant())
            )
        else -> calendarEventWithoutTime(title = title.trim(), description = notes.trim().ifBlank { null })
    }

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
                .padding(bottom = 32.dp)
        ) {
            // 24dp after drag handle
            Spacer(Modifier.height(24.dp))

            if (task == null) {
                // New task: no eyebrow, just headline
                Text(
                    text = "New Task",
                    style = MaterialTheme.typography.headlineLarge
                )
            } else {
                // Existing task: category eyebrow + headline + action chips
                val categoryLabel = task.category?.takeIf { it.isNotBlank() }
                if (categoryLabel != null) {
                    Text(
                        text = categoryLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 4dp between eyebrow and title
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = "Edit Task",
                    style = MaterialTheme.typography.headlineLarge
                )
                // 8dp between title and action chips
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(
                        onClick = {
                            context.startActivity(
                                CalendarShareUtils.buildAddToCalendarIntent(calendarInfo())
                            )
                        },
                        label = { Text("Calendar") },
                        icon = {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Add to calendar"
                            role = Role.Button
                        }
                    )
                    SuggestionChip(
                        onClick = { showShareChoice = true },
                        label = { Text("Share") },
                        icon = {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Share"
                            role = Role.Button
                        }
                    )
                }
            }

            // 8dp between header and first field
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

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
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
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

            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(16.dp))

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
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
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

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

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
                    OutlinedButton(
                        onClick = { showReminderTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("%02d:%02d".format(reminderHour, reminderMinute))
                    }
                    if (!hasNotificationPermission || !hasExactAlarmPermission) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Permission required",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    val missing = buildList {
                                        if (!hasNotificationPermission) add("notification")
                                        if (!hasExactAlarmPermission) add("exact alarm")
                                    }
                                    Text(
                                        "Grant ${missing.joinToString(" and ")} permission to receive this reminder.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        if (!hasExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            context.startActivity(
                                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                                    .apply { data = Uri.fromParts("package", context.packageName, null) }
                                            )
                                        } else if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                ) {
                                    Text("Fix", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // 32dp before destructive action
            if (task != null && onDelete != null) {
                Spacer(Modifier.height(32.dp))
                TextButton(
                    onClick = {
                        if (showDeleteConfirm) {
                            onDelete()
                            sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        } else {
                            showDeleteConfirm = true
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showDeleteConfirm) "Confirm delete" else "Delete")
                }
                // 8dp between destructive and primary action
                Spacer(Modifier.height(8.dp))
            } else {
                // 24dp between body and actions when no destructive action
                Spacer(Modifier.height(24.dp))
            }

            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    if (reminderEnabled && (!hasNotificationPermission || !hasExactAlarmPermission)) {
                        showPermissionDialog = true
                    } else {
                        if (task == null) onSave(buildInsert())
                        else onUpdate(buildUpdate())
                        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (task == null) "Add" else "Save")
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission needed") },
            text = { Text("This reminder will not fire until notification permissions are granted. Open Settings to fix this, or save and fix later.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    if (task == null) onSave(buildInsert()) else onUpdate(buildUpdate())
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    if (!hasExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .apply { data = Uri.fromParts("package", context.packageName, null) }
                        )
                    } else if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    if (task == null) onSave(buildInsert()) else onUpdate(buildUpdate())
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) { Text("Save anyway") }
            }
        )
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
                        reminderBase = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .withHour(reminderHour).withMinute(reminderMinute).withSecond(0).withNano(0)
                    }
                    showReminderDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = reminderPickerState) }
    }

    if (showShareChoice) {
        AlertDialog(
            onDismissRequest = { showShareChoice = false },
            title = { Text("Share task") },
            text = { Text("Choose how to share \"${title.trim()}\".") },
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

    if (showReminderTimePicker) {
        TimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = is24Hour,
            onConfirm = { h, m ->
                reminderHour = h
                reminderMinute = m
                showReminderTimePicker = false
            },
            onDismiss = { showReminderTimePicker = false }
        )
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
