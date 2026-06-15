package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ScanDto
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreOverviewSheet(
    chore: Chore,
    isPinned: Boolean,
    scanHistory: List<ScanDto>,
    sheetState: SheetState,
    onConfirmLog: (Chore, Instant?) -> Unit,
    onRemoveLastLog: (Chore) -> Unit,
    onTogglePin: (Chore) -> Unit,
    onMoreOptions: (Chore) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetScope = rememberCoroutineScope()
    var useCustomTime by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedHour by remember { mutableIntStateOf(LocalTime.now().hour) }
    var selectedMinute by remember { mutableIntStateOf(LocalTime.now().minute) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showRemoveLastLogConfirm by remember { mutableStateOf(false) }

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (chore.category != null) {
                        Text(
                            chore.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(chore.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        chore.tagId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned) {
                        Text(
                            "Pinned to widget",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { onTogglePin(chore) }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin from widget" else "Pin to widget",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                lastDoneText(chore.lastScanned),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log at a different time...", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = useCustomTime,
                    onCheckedChange = { useCustomTime = it }
                )
            }

            if (useCustomTime) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Date: $selectedDate")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = selectedHour.toString().padStart(2, '0'),
                        onValueChange = { v ->
                            v.toIntOrNull()?.takeIf { it in 0..23 }?.let { selectedHour = it }
                        },
                        label = { Text("Hour") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = selectedMinute.toString().padStart(2, '0'),
                        onValueChange = { v ->
                            v.toIntOrNull()?.takeIf { it in 0..59 }?.let { selectedMinute = it }
                        },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { hideAndDismiss() },
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        val at = if (useCustomTime) {
                            selectedDate.atTime(selectedHour, selectedMinute)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                        } else null
                        sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                            onConfirmLog(chore, at)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Log it")
                }
            }

            OutlinedButton(
                onClick = { showRemoveLastLogConfirm = true },
                enabled = chore.lastScanId != null,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Remove last log")
            }

            TextButton(
                onClick = {
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                        onMoreOptions(chore)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("More options...") }

            HorizontalDivider()

            Text(
                "RECENT HISTORY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (scanHistory.isEmpty()) {
                Text(
                    "No logs yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    scanHistory.forEach { scan ->
                        val scannedAt = runCatching { Instant.parse(scan.scannedAt) }.getOrNull()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                historyDateText(scannedAt),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                relativeDays(scannedAt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
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
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRemoveLastLogConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveLastLogConfirm = false },
            title = { Text("Remove last log?") },
            text = { Text("This removes the most recent log entry for \"${chore.label}\".") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveLastLogConfirm = false
                    onRemoveLastLog(chore)
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveLastLogConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

private val MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

private fun lastDoneText(lastScanned: Instant?): String {
    if (lastScanned == null) return "Never logged"
    val date = lastScanned.atZone(ZoneId.systemDefault()).format(MONTH_DAY_FORMATTER)
    return "Last done $date · ${relativeDays(lastScanned)}"
}

private fun historyDateText(scannedAt: Instant?): String {
    if (scannedAt == null) return "Unknown date"
    return scannedAt.atZone(ZoneId.systemDefault()).format(MONTH_DAY_FORMATTER)
}

private fun relativeDays(instant: Instant?): String {
    if (instant == null) return ""
    val days = ChronoUnit.DAYS.between(instant, Instant.now())
    return when {
        days <= 0L -> "Today"
        days == 1L -> "Yesterday"
        else -> "$days days ago"
    }
}
