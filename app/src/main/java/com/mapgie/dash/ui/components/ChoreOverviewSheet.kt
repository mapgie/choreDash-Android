package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ScanDto
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.ui.components.core.StatusBadge
import com.mapgie.dash.ui.components.sheet.DoneWhen
import com.mapgie.dash.ui.components.sheet.DoneWhenControl
import com.mapgie.dash.ui.components.sheet.SheetBlock
import com.mapgie.dash.ui.components.sheet.SheetHeader
import com.mapgie.dash.ui.components.sheet.SheetPadding
import com.mapgie.dash.ui.components.sheet.SheetPrimaryRow
import com.mapgie.dash.ui.components.sheet.SheetRowDivider
import com.mapgie.dash.ui.components.sheet.SheetSectionLabel
import com.mapgie.dash.ui.components.sheet.SheetTimePickerDialog
import com.mapgie.dash.ui.components.sheet.UtilityAction
import com.mapgie.dash.ui.components.sheet.UtilityRow
import com.mapgie.dash.ui.theme.BadgeShape
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.badgeContainerColor
import com.mapgie.dash.ui.theme.statusTone
import com.mapgie.dash.ui.theme.textColor
import com.mapgie.dash.ui.theme.tintColor
import com.mapgie.dash.util.CalendarShareUtils
import com.mapgie.dash.util.calendarEventWithoutTime
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * The Log sheet (handoff 6a), sharing its anatomy with the task Done sheet:
 * header (category chip · eyebrow · title · badge and meta · tag label · owner),
 * the DONE control (Just now / Earlier today / Pick…), Cancel + sage "Log it",
 * the utility row (Calendar · Pin · Remind · Tag · Edit) and the HISTORY block
 * whose latest row carries an inline Undo chip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreOverviewSheet(
    chore: Chore,
    icon: ImageVector,
    /** Category colour for the badge (Settings › Colours spine+badge axis), or null to follow severity. */
    badgeSwatch: Swatch?,
    /** Category colour for the icon chip (icon axis), or null to follow severity. */
    iconSwatch: Swatch?,
    isPinned: Boolean,
    scanHistory: List<ScanDto>,
    sheetState: SheetState,
    onConfirmLog: (Chore, Instant?) -> Unit,
    onRemoveLastLog: (Chore) -> Unit,
    onLoadAllHistory: (Chore) -> Unit,
    onTogglePin: (Chore) -> Unit,
    onAddReminder: (Chore) -> Unit,
    onEdit: (Chore) -> Unit,
    onWriteTag: (Chore) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetScope = rememberCoroutineScope()
    val context = LocalContext.current
    val tokens = LocalDashTokens.current
    val tone = chore.statusTone()

    var doneWhen by remember { mutableStateOf(DoneWhen.JUST_NOW) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedHour by remember { mutableIntStateOf(LocalTime.now().hour) }
    var selectedMinute by remember { mutableIntStateOf(LocalTime.now().minute) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRemoveLastLogConfirm by remember { mutableStateOf(false) }
    var allHistoryRequested by remember { mutableStateOf(false) }

    fun calendarInfo() = calendarEventWithoutTime(
        title = chore.label,
        description = chore.category?.let { "Category: $it" }
    )

    fun hideAndDismiss() {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    fun hideThen(action: () -> Unit) {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    val chipContainer = iconSwatch?.tintColor() ?: (tone.badgeContainerColor() ?: MaterialTheme.colorScheme.secondaryContainer)
    val chipContent = iconSwatch?.textColor() ?: tone.textColor()

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
                    chore.category?.takeIf { it.isNotBlank() } ?: "chore",
                    chore.intervalDays?.let { "every ${it.toInt()}d" },
                ).joinToString(" · "),
                ownerHandle = chore.owner,
            ) {
                Text(
                    text = chore.label,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    StatusBadge(
                        text = chore.dueBadgeText(),
                        tone = tone,
                        containerOverride = badgeSwatch?.tintColor(),
                        textOverride = if (badgeSwatch != null) MaterialTheme.colorScheme.onSurfaceVariant else null,
                    )
                    Text(
                        text = lastDoneText(chore.lastScanned),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = tokens.inkFaint,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = if (chore.tagId.isNotBlank()) "NFC tag ${chore.tagId}" else "No NFC tag"
                        },
                ) {
                    Icon(
                        imageVector = LucideIcons.Nfc,
                        contentDescription = null,
                        tint = if (chore.tagId.isNotBlank()) tokens.tagLabel else tokens.inkFaint,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = chore.tagId.ifBlank { "no label" },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold),
                        color = if (chore.tagId.isNotBlank()) tokens.tagLabel else tokens.inkFaint,
                        maxLines = 1,
                    )
                }
            }

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

            SheetPrimaryRow(
                actionLabel = "Log it",
                onCancel = { hideAndDismiss() },
                onAction = {
                    val at = if (doneWhen == DoneWhen.JUST_NOW) null else {
                        selectedDate.atTime(selectedHour, selectedMinute)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                    }
                    hideThen { onConfirmLog(chore, at) }
                },
            )

            UtilityRow(
                actions = listOf(
                    UtilityAction(
                        icon = LucideIcons.Calendar, label = "Calendar", contentDescription = "Add to calendar",
                        onClick = { context.startActivity(CalendarShareUtils.buildAddToCalendarIntent(calendarInfo())) },
                    ),
                    UtilityAction(
                        icon = if (isPinned) LucideIcons.PinFilled else LucideIcons.Pin,
                        label = "Pin",
                        contentDescription = if (isPinned) "Unpin from widget" else "Pin to widget",
                        onClick = { onTogglePin(chore) },
                        active = isPinned,
                    ),
                    UtilityAction(
                        icon = LucideIcons.Bell, label = "Remind", contentDescription = "Add a reminder",
                        onClick = { hideThen { onAddReminder(chore) } },
                    ),
                    UtilityAction(
                        icon = LucideIcons.NfcScan, label = "Tag", contentDescription = "Write NFC tag",
                        onClick = { hideThen { onWriteTag(chore) } },
                    ),
                    UtilityAction(
                        icon = LucideIcons.Pencil, label = "Edit", contentDescription = "Edit chore",
                        onClick = { hideThen { onEdit(chore) } },
                    ),
                ),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetSectionLabel(
                    text = "History",
                    trailing = if (scanHistory.size >= 4 && !allHistoryRequested) {
                        {
                            Text(
                                text = "All ›",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .semantics { role = Role.Button }
                                    .clickable {
                                        allHistoryRequested = true
                                        onLoadAllHistory(chore)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 14.dp),
                            )
                        }
                    } else null,
                )
                SheetBlock(radius = 14.dp) {
                    if (scanHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No logs yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        scanHistory.forEachIndexed { index, scan ->
                            val scannedAt = runCatching { Instant.parse(scan.scannedAt) }.getOrNull()
                            val latest = index == 0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (latest) MaterialTheme.colorScheme.secondary
                                            else tokens.sectionCount
                                        )
                                )
                                Text(
                                    text = historyDateText(scannedAt),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (latest) FontWeight.ExtraBold else FontWeight.Bold,
                                    ),
                                    color = if (latest) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = relativeDays(scannedAt),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                                    color = tokens.inkFaint,
                                )
                                if (latest) {
                                    UndoChip(onClick = { showRemoveLastLogConfirm = true })
                                }
                            }
                            if (index < scanHistory.lastIndex) SheetRowDivider()
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

    if (showRemoveLastLogConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveLastLogConfirm = false },
            title = { Text("Undo latest log?") },
            text = { Text("This removes the most recent log entry for “${chore.label}”.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveLastLogConfirm = false
                    onRemoveLastLog(chore)
                }) { Text("Undo", color = Swatch.ROSE.textColor()) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveLastLogConfirm = false }) { Text("Keep") }
            }
        )
    }
}

/** The inline rose Undo chip on the latest history row. */
@Composable
private fun UndoChip(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(BadgeShape)
            .semantics {
                role = Role.Button
                contentDescription = "Undo latest log"
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(BadgeShape)
                .background(Swatch.ROSE.tintColor())
                .padding(horizontal = 9.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = LucideIcons.Undo,
                contentDescription = null,
                tint = Swatch.ROSE.textColor(),
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "Undo",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold),
                color = Swatch.ROSE.textColor(),
            )
        }
    }
}

private val MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

private fun lastDoneText(lastScanned: Instant?): String {
    if (lastScanned == null) return "never logged"
    val date = lastScanned.atZone(ZoneId.systemDefault()).format(MONTH_DAY_FORMATTER)
    return "last done $date · ${relativeDays(lastScanned)}"
}

private fun historyDateText(scannedAt: Instant?): String {
    if (scannedAt == null) return "Unknown date"
    return scannedAt.atZone(ZoneId.systemDefault()).format(MONTH_DAY_FORMATTER)
}

private fun relativeDays(instant: Instant?): String {
    if (instant == null) return ""
    val days = ChronoUnit.DAYS.between(instant, Instant.now())
    return when {
        days <= 0L -> "today"
        days == 1L -> "yesterday"
        else -> "${days}d ago"
    }
}
