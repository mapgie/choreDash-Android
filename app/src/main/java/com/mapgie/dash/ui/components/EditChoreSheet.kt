package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreDraft
import com.mapgie.dash.data.model.ChoreRepeat
import com.mapgie.dash.data.model.ChoreSchedule
import com.mapgie.dash.data.model.RepeatUnit
import com.mapgie.dash.data.model.formatDueDate
import com.mapgie.dash.data.model.GENERAL_CATEGORY
import com.mapgie.dash.data.model.PRIVATE_CATEGORY
import com.mapgie.dash.data.model.isPrivateCategory
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.ui.components.sheet.DraftResumeRow
import com.mapgie.dash.ui.components.sheet.LocalDateStateSaver
import com.mapgie.dash.ui.components.sheet.enumStateSaver
import com.mapgie.dash.ui.components.sheet.OwnerAvatarRow
import com.mapgie.dash.ui.components.sheet.PrivateNote
import com.mapgie.dash.ui.components.sheet.privateNoteFor
import com.mapgie.dash.ui.components.sheet.SettingsRow
import com.mapgie.dash.ui.components.sheet.SheetBlock
import com.mapgie.dash.ui.components.sheet.SheetHeader
import com.mapgie.dash.ui.components.sheet.SheetPadding
import com.mapgie.dash.ui.components.sheet.SheetPrimaryRow
import com.mapgie.dash.ui.components.sheet.SheetRowDivider
import com.mapgie.dash.ui.components.sheet.StepperPill
import com.mapgie.dash.ui.components.sheet.TertiaryLink
import com.mapgie.dash.ui.components.sheet.TertiaryLinkRow
import com.mapgie.dash.ui.components.sheet.TitleField
import com.mapgie.dash.ui.components.sheet.ValueChip
import com.mapgie.dash.ui.components.sheet.jsonStateSaver
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.badgeContainerColor
import com.mapgie.dash.ui.theme.statusTone
import com.mapgie.dash.ui.theme.textColor
import com.mapgie.dash.ui.theme.tintColor
import com.mapgie.dash.util.CalendarShareUtils
import com.mapgie.dash.util.calendarEventForDate
import com.mapgie.dash.util.calendarEventWithoutTime
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

/**
 * The Edit sheet for chores (handoff 7a), one grammar with the task sheet: the
 * title is the input, then one grouped settings card of compact rows (Category
 * value chip · Owner avatar row · Repeat every stepper and its unit · Due date
 * chip · Show from chip · NFC tag), the same Cancel + sage Save footer as the Log sheet, and a
 * centred tertiary row (Add to calendar · Share · Archive). With [chore] null
 * it is the New chore sheet:
 * eyebrow NEW CHORE, empty title focused, and a tag ID field on the NFC row.
 *
 * Every dismiss vector is guarded when the sheet is dirty (LESSONS.md #27).
 * Fields survive rotation and process death (rememberSaveable) and every change
 * is mirrored to the caller through [onDraftChange]; a [draft] handed back on
 * reopen is offered at the top of the sheet, never applied on its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChoreSheet(
    chore: Chore?,
    icon: ImageVector,
    /** Category colour for the category value chip (spine+badge axis), or null. */
    badgeSwatch: Swatch?,
    /** Category colour for the header icon chip (icon axis), or null to follow severity. */
    iconSwatch: Swatch?,
    owners: List<String>,
    categories: List<String>,
    sheetState: SheetState,
    /** [leadDays] is this phone's "show from" for the chore; null means automatic. */
    /** [nfcId] is the chore's NFC tag id, or null for a chore with no tag. */
    onSave: (nfcId: String?, label: String, category: String?, owner: String?, schedule: ChoreSchedule, leadDays: Int?) -> Unit,
    onArchiveToggle: (chore: Chore, archive: Boolean) -> Unit,
    onWriteTag: (nfcId: String) -> Unit,
    onDismiss: () -> Unit,
    /** The id of a tag just scanned, for a new chore made from an unknown tag. */
    initialNfcId: String = "",
    /** This phone's "show from" for [chore], or null when it follows the automatic rule. */
    initialLeadDays: Int? = null,
    draft: ChoreDraft? = null,
    onDraftChange: (ChoreDraft) -> Unit = {},
    onDraftClear: () -> Unit = {},
    /** The id the phone read while this sheet was listening for a tag ([onStartScan]). */
    scannedTagId: String? = null,
    onStartScan: () -> Unit = {},
    onCancelScan: () -> Unit = {},
    onScanConsumed: () -> Unit = {},
) {
    val sheetScope = rememberCoroutineScope()
    val context = LocalContext.current
    val tokens = LocalDashTokens.current
    val accents = LocalTypeAccents.current
    val isNew = chore == null
    val isArchived = chore?.archivedAt != null

    // The values the sheet opened with, snapshotted once so the dirty check
    // compares against what the fields actually started as (LESSONS.md #27).
    // Every field is rememberSaveable so rotation and process death keep edits.
    val opened = remember { ChoreDraft.of(chore, initialNfcId, initialLeadDays) }
    var label by rememberSaveable { mutableStateOf(opened.label) }
    var category by rememberSaveable { mutableStateOf(opened.category) }
    var owner by rememberSaveable { mutableStateOf(opened.owner) }
    var interval by rememberSaveable { mutableStateOf(opened.repeatEvery) }
    var repeatUnit by rememberSaveable(stateSaver = enumStateSaver<RepeatUnit>()) { mutableStateOf(opened.repeatUnitEnum()) }
    var dueDate by rememberSaveable(stateSaver = LocalDateStateSaver) { mutableStateOf(opened.dueDate()) }
    var leadDays by rememberSaveable { mutableStateOf(opened.leadDays) }
    var nfcId by rememberSaveable { mutableStateOf(opened.nfcId) }
    var scanning by rememberSaveable { mutableStateOf(false) }

    var categoryMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showNewCategory by rememberSaveable { mutableStateOf(false) }
    var showIntervalEntry by rememberSaveable { mutableStateOf(false) }
    var unitMenuOpen by rememberSaveable { mutableStateOf(false) }
    var dueMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showDueDatePicker by rememberSaveable { mutableStateOf(false) }
    var showFromMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showLeadEntry by rememberSaveable { mutableStateOf(false) }
    // The picker works in UTC midnights, like the task sheet's.
    val dueDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )
    var showArchiveConfirm by rememberSaveable { mutableStateOf(false) }
    var showShareChoice by rememberSaveable { mutableStateOf(false) }
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }

    // A stored draft is offered once, at open, and never applied on its own. The
    // offer itself is saved so a rotation does not repeat it for edits that
    // rememberSaveable already brought back.
    var offeredDraft by rememberSaveable(stateSaver = jsonStateSaver(ChoreDraft.serializer())) {
        mutableStateOf(draft?.takeIf { it.differsFrom(opened) })
    }

    val currentDraft = ChoreDraft(
        label = label,
        category = category,
        owner = owner,
        repeatEvery = interval,
        nfcId = nfcId,
        repeatUnit = repeatUnit.name,
        dueDateEpochDay = dueDate?.toEpochDay(),
        leadDays = leadDays,
    )
    val isDirty = currentDraft.differsFrom(opened)

    // Scanning links a sticker as it is, without writing it: the way to give a
    // chore a tag another chore or a tag-alarm let go of. The activity's "capture
    // the next tag" request follows [scanning] and is withdrawn with the sheet.
    LaunchedEffect(scanning) {
        if (scanning) onStartScan() else onCancelScan()
    }
    DisposableEffect(Unit) {
        onDispose { onCancelScan() }
    }
    LaunchedEffect(scannedTagId) {
        val scanned = scannedTagId ?: return@LaunchedEffect
        if (scanning) {
            scanning = false
            nfcId = scanned
        }
        onScanConsumed()
    }

    // Mirror every change into the draft store while the sheet is dirty.
    LaunchedEffect(currentDraft) {
        if (isDirty) onDraftChange(currentDraft)
    }

    fun restoreDraft(restored: ChoreDraft) {
        label = restored.label
        category = restored.category
        owner = restored.owner
        interval = restored.repeatEvery
        repeatUnit = restored.repeatUnitEnum()
        dueDate = restored.dueDate()
        leadDays = restored.leadDays
        dueDatePickerState.selectedDateMillis = dueDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        // A new chore's tag that arrived with an NFC scan wins over a draft that has none.
        nfcId = if (isNew) restored.nfcId.ifBlank { nfcId } else restored.nfcId
        offeredDraft = null
    }

    fun forgetDraft() {
        offeredDraft = null
        if (isDirty) onDraftChange(currentDraft) else onDraftClear()
    }

    /** Nothing changed: drop this sheet's draft, but keep an offer the user has not answered. */
    fun settleDraftOnCleanDismiss() {
        val offered = offeredDraft
        if (offered != null) onDraftChange(offered) else onDraftClear()
    }

    fun requestDismiss() {
        if (isDirty) {
            sheetScope.launch { sheetState.show() }
            showDiscardConfirm = true
        } else {
            settleDraftOnCleanDismiss()
            sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        }
    }

    // Material 3 builds the sheet's nested-scroll connection once per SheetState and
    // that connection keeps the onDismissRequest it was created with, so a swipe-down
    // on the sheet body would run the very first requestDismiss, whose isDirty was
    // still false (LESSONS.md #45). Read the latest one through State instead.
    val latestRequestDismiss by rememberUpdatedState<() -> Unit>({ requestDismiss() })

    fun hideThen(action: () -> Unit) {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    fun calendarInfo() = currentDraft.schedule().dueDate.let { date ->
        val title = label.trim().ifBlank { chore?.label ?: "" }
        val description = category.trim().ifBlank { null }?.let { "Category: $it" }
        if (date != null) calendarEventForDate(title = title, description = description, date = date)
        else calendarEventWithoutTime(title = title, description = description)
    }

    val tone = chore?.statusTone()
    val chipContainer = iconSwatch?.tintColor() ?: tone?.badgeContainerColor() ?: accents.choreContainer
    val chipContent = iconSwatch?.textColor() ?: tone?.textColor() ?: accents.onChoreContainer
    val canSave = label.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = { latestRequestDismiss() },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(
                icon = icon,
                chipContainer = chipContainer,
                chipContent = chipContent,
                eyebrow = when {
                    isNew -> "New chore"
                    isArchived -> "Archived chore"
                    else -> "Edit chore"
                },
            ) {
                TitleField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = "Chore name",
                    autoFocus = isNew,
                )
            }

            offeredDraft?.let { offered ->
                DraftResumeRow(
                    itemName = chore?.label ?: offered.displayName() ?: "a new chore",
                    onRestore = { restoreDraft(offered) },
                    onForget = { forgetDraft() },
                )
            }

            SheetBlock {
                SettingsRow(icon = LucideIcons.LayoutGrid, label = "Category") {
                    Box {
                        ValueChip(
                            text = category.ifBlank { "None" },
                            onClick = { categoryMenuOpen = true },
                            contentDescription = "Category: ${category.ifBlank { "none" }}. Change category",
                            container = badgeSwatch?.tintColor() ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                            content = badgeSwatch?.textColor() ?: MaterialTheme.colorScheme.onSurface,
                        )
                        CategoryMenu(
                            expanded = categoryMenuOpen,
                            categories = categories,
                            onPick = { category = it; categoryMenuOpen = false },
                            onNew = { categoryMenuOpen = false; showNewCategory = true },
                            onDismiss = { categoryMenuOpen = false },
                        )
                    }
                }
                SheetRowDivider()
                SettingsRow(icon = LucideIcons.User, label = "Owner") {
                    OwnerAvatarRow(
                        owners = owners,
                        selected = owner.ifBlank { null },
                        onSelect = { owner = it ?: "" },
                    )
                }
                SheetRowDivider()
                SettingsRow(icon = LucideIcons.Repeat, label = "Repeat every") {
                    StepperPill(
                        valueText = interval?.let { "$it ${repeatUnit.shortSuffix()}" } ?: "none",
                        onMinus = { interval = interval?.let { if (it <= 1) null else it - 1 } },
                        onPlus = { interval = (interval ?: 0) + 1 },
                        onValueClick = { showIntervalEntry = true },
                        minusEnabled = interval != null,
                        minusDescription = "Repeat one ${repeatUnit.singular} less often",
                        plusDescription = "Repeat one ${repeatUnit.singular} more often",
                        valueDescription = "Repeat ${interval?.let { ChoreRepeat(it, repeatUnit).longLabel() } ?: "with no set interval"}. Type a number",
                    )
                }
                if (interval != null) {
                    SheetRowDivider()
                    SettingsRow(icon = LucideIcons.Clock, label = "Counted in") {
                        Box {
                            ValueChip(
                                text = repeatUnit.plural.replaceFirstChar { it.uppercase() },
                                onClick = { unitMenuOpen = true },
                                contentDescription = "Repeat counted in ${repeatUnit.plural}. Change unit",
                            )
                            DropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                                RepeatUnit.entries.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.plural.replaceFirstChar { it.uppercase() }) },
                                        onClick = { repeatUnit = unit; unitMenuOpen = false },
                                    )
                                }
                            }
                        }
                    }
                }
                // Every chore repeats, so a due date is only offered alongside a repeat.
                if (interval != null) {
                    SheetRowDivider()
                    SettingsRow(icon = LucideIcons.Calendar, label = "Due date") {
                        val dueText = dueDate?.let { formatDueDate(it) } ?: "None"
                        Box {
                            ValueChip(
                                text = dueText,
                                onClick = { dueMenuOpen = true },
                                contentDescription = "Due date: ${dueText.lowercase()}. Change due date",
                            )
                            DropdownMenu(expanded = dueMenuOpen, onDismissRequest = { dueMenuOpen = false }) {
                                DropdownMenuItem(text = { Text("None") }, onClick = { dueDate = null; dueMenuOpen = false })
                                DropdownMenuItem(
                                    text = { Text("Pick a date…") },
                                    onClick = { dueMenuOpen = false; showDueDatePicker = true },
                                )
                            }
                        }
                    }
                }
                SheetRowDivider()
                SettingsRow(icon = LucideIcons.Target, label = "Show from") {
                    val showFromText = leadDays?.let { leadDaysText(it) } ?: "Auto"
                    Box {
                        ValueChip(
                            text = showFromText,
                            onClick = { showFromMenuOpen = true },
                            contentDescription = "Show from: ${showFromText.lowercase()}. Change when this chore appears in the list",
                        )
                        DropdownMenu(expanded = showFromMenuOpen, onDismissRequest = { showFromMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Auto") },
                                onClick = { leadDays = null; showFromMenuOpen = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Days before due…") },
                                onClick = { showFromMenuOpen = false; showLeadEntry = true },
                            )
                        }
                    }
                }
                SheetRowDivider()
                SettingsRow(icon = LucideIcons.NfcScan, label = "NFC tag") {
                    // Blank means no tag. Clearing it unlinks the tag on Save; the
                    // sticker keeps its id, free for another chore or a tag-alarm.
                    TagIdField(
                        value = nfcId,
                        onValueChange = { nfcId = it },
                        placeholder = if (scanning) "Hold a tag" else "No tag",
                    )
                    if (nfcId.isBlank()) {
                        ValueChip(
                            text = if (scanning) "Cancel" else "Scan",
                            onClick = { scanning = !scanning },
                            contentDescription = if (scanning) "Stop listening for a tag" else "Scan a tag to link it to this chore",
                            chevron = false,
                        )
                    } else {
                        ValueChip(
                            text = "Write",
                            onClick = { hideThen { onWriteTag(nfcId.trim()) } },
                            contentDescription = "Write this tag id to an NFC tag",
                            chevron = false,
                        )
                        ValueChip(
                            text = if (opened.nfcId.isNotBlank()) "Unlink" else "Clear",
                            onClick = { nfcId = "" },
                            contentDescription = "Remove the NFC tag from this chore",
                            chevron = false,
                        )
                    }
                }
            }

            privateNoteFor(
                wasPrivate = isPrivateCategory(opened.category),
                isPrivateNow = isPrivateCategory(category),
            )?.let { PrivateNote(text = it) }

            SheetPrimaryRow(
                actionLabel = "Save",
                actionEnabled = canSave,
                onCancel = { requestDismiss() },
                onAction = {
                    val schedule = currentDraft.schedule()
                    val lead = leadDays
                    val ownerValue = owner.trim().ifBlank { null }
                    val categoryValue = category.trim().ifBlank { null }
                    onDraftClear()
                    val tag = currentDraft.nfcIdOrNull()
                    hideThen { onSave(tag, label.trim(), categoryValue, ownerValue, schedule, lead) }
                },
            )

            TertiaryLinkRow(
                links = listOfNotNull(
                    TertiaryLink(
                        icon = LucideIcons.Calendar, label = "Add to calendar",
                        onClick = { context.startActivity(CalendarShareUtils.buildAddToCalendarIntent(calendarInfo())) },
                    ),
                    TertiaryLink(icon = LucideIcons.Share, label = "Share", onClick = { showShareChoice = true }),
                    if (chore != null) TertiaryLink(
                        icon = LucideIcons.Archive,
                        label = if (isArchived) "Unarchive" else "Archive",
                        onClick = { showArchiveConfirm = true },
                        destructive = !isArchived,
                    ) else null,
                ),
            )
        }
    }

    if (showNewCategory) {
        NewCategoryDialog(
            onCreate = { category = it; showNewCategory = false },
            onDismiss = { showNewCategory = false },
        )
    }

    if (showIntervalEntry) {
        var text by rememberSaveable { mutableStateOf(interval?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showIntervalEntry = false },
            title = { Text("Repeat every") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) text = v },
                    label = { Text("${repeatUnit.plural.replaceFirstChar { it.uppercase() }} (blank for none)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    interval = text.toIntOrNull()?.takeIf { it > 0 }
                    showIntervalEntry = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showIntervalEntry = false }) { Text("Cancel") }
            }
        )
    }

    if (showLeadEntry) {
        var text by rememberSaveable { mutableStateOf(leadDays?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showLeadEntry = false },
            title = { Text("Show from") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "On this phone, keep this chore in the hidden section until this many days " +
                            "before it is due. 0 shows it on the day. Blank goes back to Auto.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> if (v.length <= 3 && v.all { it.isDigit() }) text = v },
                        label = { Text("Days before due") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    leadDays = text.toIntOrNull()
                    showLeadEntry = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showLeadEntry = false }) { Text("Cancel") }
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

    if (showShareChoice) {
        AlertDialog(
            onDismissRequest = { showShareChoice = false },
            title = { Text("Share chore") },
            text = { Text("Choose how to share “${label.trim()}”.") },
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

    if (showArchiveConfirm && chore != null) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text(if (isArchived) "Unarchive chore?" else "Archive chore?") },
            text = {
                Text(
                    if (isArchived)
                        "This chore will reappear in your active list."
                    else
                        "This chore will be hidden from your active list."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showArchiveConfirm = false
                    onDraftClear()
                    hideThen { onArchiveToggle(chore, !isArchived) }
                }) { Text(if (isArchived) "Unarchive" else "Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDiscardConfirm) {
        DiscardChangesDialog(
            itemName = chore?.label ?: label.trim().ifBlank { null },
            onKeepEditing = { showDiscardConfirm = false },
            onDiscard = {
                showDiscardConfirm = false
                onDraftClear()
                sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        )
    }
}

/** The Show from chip: "On the day", "1 day before", "14 days before". */
private fun leadDaysText(days: Int): String = when (days) {
    0 -> "On the day"
    1 -> "1 day before"
    else -> "$days days before"
}

/** The stepper's unit suffix: "3 d", "2 w", "1 mo", "1 y". */
private fun RepeatUnit.shortSuffix(): String = when (this) {
    RepeatUnit.DAY -> "d"
    RepeatUnit.WEEK -> "w"
    RepeatUnit.MONTH -> "mo"
    RepeatUnit.YEAR -> "y"
}

/** Compact inline field for the chore's NFC tag id, on the NFC row; empty reads "No tag". */
@Composable
private fun TagIdField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val tokens = LocalDashTokens.current
    Box(
        modifier = Modifier
            .widthIn(min = 72.dp, max = 120.dp)
            .padding(vertical = 4.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = tokens.inkFaint,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelSmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = tokens.tagLabel,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "NFC tag id" },
        )
    }
}

/**
 * Category picker shared by both edit sheets: the known categories, General
 * when it is not among them, Private (always offered, with its padlock, so a
 * phone-only item is one pick away) and "New category…".
 */
@Composable
internal fun CategoryMenu(
    expanded: Boolean,
    categories: List<String>,
    onPick: (String) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val withGeneral = if (categories.any { it.equals(GENERAL_CATEGORY, ignoreCase = true) }) categories
                      else categories + GENERAL_CATEGORY
    val options = if (withGeneral.any { isPrivateCategory(it) }) withGeneral else withGeneral + PRIVATE_CATEGORY
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                leadingIcon = if (isPrivateCategory(option)) {
                    { Icon(LucideIcons.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                onClick = { onPick(option) },
            )
        }
        DropdownMenuItem(
            text = { Text("New category…", color = MaterialTheme.colorScheme.onSecondaryContainer) },
            onClick = onNew,
        )
    }
}

/** Small dialog to type a category that does not exist yet. */
@Composable
internal fun NewCategoryDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New category") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(text.trim()) }, enabled = text.isNotBlank()) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
