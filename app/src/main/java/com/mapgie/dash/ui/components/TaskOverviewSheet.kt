package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.util.CalendarShareUtils
import com.mapgie.dash.util.calendarEventForDate
import com.mapgie.dash.util.calendarEventForInstant
import com.mapgie.dash.util.calendarEventWithoutTime
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOverviewSheet(
    task: TaskDto,
    isPinned: Boolean,
    sheetState: SheetState,
    onToggleDone: (TaskDto) -> Unit,
    onTogglePin: (TaskDto) -> Unit,
    onEdit: (TaskDto) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isDone = task.completedAt != null

    fun hideAndDismiss() {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

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
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // 24dp after drag handle
            Spacer(Modifier.height(24.dp))

            // Category eyebrow
            if (task.category != null) {
                Text(
                    task.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 4dp between eyebrow and title
                Spacer(Modifier.height(4.dp))
            }

            // Main title — visual anchor
            Text(
                task.title,
                style = MaterialTheme.typography.headlineLarge
            )

            // 8dp between title and metadata
            Spacer(Modifier.height(8.dp))

            // Status metadata
            statusText(task)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action chips: Calendar + Pin grouped together
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        context.startActivity(
                            CalendarShareUtils.buildAddToCalendarIntent(taskCalendarInfo(task))
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
                    onClick = { onTogglePin(task) },
                    label = { Text(if (isPinned) "Unpin" else "Pin") },
                    icon = {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (isPinned) "Unpin from widget" else "Pin to widget"
                        role = Role.Button
                    }
                )
            }

            // 24dp between body and actions
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { hideAndDismiss() },
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                            onToggleDone(task)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.AutoMirrored.Filled.Undo else Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isDone) "Restore task" else "Mark done")
                }
            }

            // 8dp between primary and secondary actions
            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = {
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                        onEdit(task)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Edit task...")
            }
        }
    }
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

private val DUE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val DONE_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM 'at' HH:mm")

private fun statusText(task: TaskDto): String? {
    if (task.completedAt != null) {
        val completedAt = runCatching { Instant.parse(task.completedAt) }.getOrNull()
            ?: return "Done"
        return "Done ${completedAt.atZone(ZoneId.systemDefault()).format(DONE_AT_FORMATTER)}"
    }
    if (task.dueDate != null) {
        val date = runCatching { LocalDate.parse(task.dueDate) }.getOrNull() ?: return null
        return "Due ${date.format(DUE_DATE_FORMATTER)}"
    }
    return when (task.duePeriod) {
        "today" -> "Due today"
        "this_week" -> "Due this week"
        "this_month" -> "Due this month"
        else -> null
    }
}
