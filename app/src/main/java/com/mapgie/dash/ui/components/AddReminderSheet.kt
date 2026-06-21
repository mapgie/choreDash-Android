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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

    var subject by remember { mutableStateOf(existing?.subject ?: initialSubject ?: "") }

    val initialRemind = remember {
        existing?.remindAtInstant()?.atZone(ZoneId.systemDefault())
            ?: ZonedDateTime.now().plusDays(1).withSecond(0).withNano(0)
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

    ModalBottomSheet(
        onDismissRequest = {
            sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        },
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
                text = if (existing != null) "Edit Reminder" else "New Reminder",
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
                    if (!hasNotificationPermission || !hasExactAlarmPermission) {
                        showPermissionDialog = true
                    } else {
                        onSave(buildInsert())
                        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(if (existing != null) "Save" else "Add Reminder")
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

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission needed") },
            text = { Text("This reminder will not fire until notification permissions are granted. Open Settings to fix this, or save and fix later.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    onSave(buildInsert())
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
                    onSave(buildInsert())
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) { Text("Save anyway") }
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
