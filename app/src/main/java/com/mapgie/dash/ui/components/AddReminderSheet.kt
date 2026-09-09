package com.mapgie.dash.ui.components

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import com.mapgie.dash.data.model.CategoryIcon
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ReminderDraft
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.ReminderScheduleText
import com.mapgie.dash.data.model.RepeatPreset
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.nextOccurrence
import com.mapgie.dash.data.model.parseRepeatDays
import com.mapgie.dash.ui.components.core.LocalReminderLabel
import com.mapgie.dash.ui.components.core.MetaCaption
import com.mapgie.dash.ui.components.sheet.DraftResumeRow
import com.mapgie.dash.ui.components.sheet.SettingsRow
import com.mapgie.dash.ui.components.sheet.SheetBlock
import com.mapgie.dash.ui.components.sheet.SheetHeader
import com.mapgie.dash.ui.components.sheet.SheetPadding
import com.mapgie.dash.ui.components.sheet.SheetPrimaryRow
import com.mapgie.dash.ui.components.sheet.SheetRowDivider
import com.mapgie.dash.ui.components.sheet.SheetTimePickerDialog
import com.mapgie.dash.ui.components.sheet.TertiaryLink
import com.mapgie.dash.ui.components.sheet.TertiaryLinkRow
import com.mapgie.dash.ui.components.sheet.TitleField
import com.mapgie.dash.ui.components.sheet.ValueChip
import com.mapgie.dash.ui.components.sheet.ZonedDateTimeStateSaver
import com.mapgie.dash.ui.components.sheet.jsonStateSaver
import com.mapgie.dash.ui.screens.settings.CozySwitch
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.spineColor
import com.mapgie.dash.ui.theme.textColor
import com.mapgie.dash.ui.theme.tintColor
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** The day-of-week picker's cells, Sunday first as the design draws them (S M T W T F S). */
private val DAY_CELLS = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

private fun Set<DayOfWeek>.toRaw(): String = sorted().joinToString(",") { it.name }
private fun String.toDays(): Set<DayOfWeek> = parseRepeatDays(split(',').filter { it.isNotBlank() })

/**
 * The edit-alarm sheet (handoff 9a), one grammar with the chore and task
 * sheets: bell chip and eyebrow, the title as the input, a Time card with the
 * large serif time, a Repeat card (toggle, S M T W T F S day cells, Weekdays /
 * Weekends / Every day shortcuts), the Linked chore or task row, a Next ring
 * banner, then the Cancel + sage Save footer and a centred tertiary row
 * (Archive · Delete) for an existing memo. With [existing] null it is the New
 * sheet: eyebrow NEW MEMO, empty title focused.
 *
 * Two things the mockup leaves out of frame are kept: a once-only memo still
 * needs a date, so a Date row appears under Time while Repeat is off; and a
 * memo can hang off a task as well as a chore, so both are offered.
 *
 * Every dismiss vector is guarded when the sheet is dirty (LESSONS.md #27, #49).
 * Fields survive rotation and process death (rememberSaveable) and every change
 * is mirrored to the caller through [onDraftChange]; a [draft] handed back on
 * reopen is offered at the top of the sheet, never applied on its own.
 */
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
    onDismiss: () -> Unit,
    draft: ReminderDraft? = null,
    onDraftChange: (ReminderDraft) -> Unit = {},
    onDraftClear: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()
    val accents = LocalTypeAccents.current
    val tokens = LocalDashTokens.current
    val zone = remember { ZoneId.systemDefault() }
    val isNew = existing == null
    // "Reminder", "Alarm" or "Memo": whatever the user calls the feature.
    val featureWord = LocalReminderLabel.current.singular

    // The values the sheet opened with, snapshotted once so the dirty check
    // compares against what the fields actually started as (LESSONS.md #27).
    val opened = remember { ReminderDraft.of(existing, initialSubject, initialChoreId, initialTaskId) }
    var subject by rememberSaveable { mutableStateOf(opened.subject) }
    var ringAt by rememberSaveable(stateSaver = ZonedDateTimeStateSaver) {
        mutableStateOf(Instant.ofEpochMilli(opened.ringAtEpochMillis).atZone(zone))
    }
    var repeatOn by rememberSaveable { mutableStateOf(opened.repeatDays.isNotEmpty()) }
    // Comma-joined day names: a plain String so rememberSaveable can hold it.
    var repeatDaysRaw by rememberSaveable { mutableStateOf(opened.repeatDays.joinToString(",")) }
    var choreId by rememberSaveable { mutableStateOf(opened.choreId) }
    var taskId by rememberSaveable { mutableStateOf(opened.taskId) }
    // Ringtone URI for the Alarm style; blank is the device's default alarm tone.
    var sound by rememberSaveable { mutableStateOf(opened.sound) }
    // A standalone memo's own accent and glyph (Swatch / CategoryIcon enum names,
    // blank for the default). Ignored while the memo is linked, which inherits its
    // chore's or task's category look on the list card instead.
    var colour by rememberSaveable { mutableStateOf(opened.colour) }
    var glyph by rememberSaveable { mutableStateOf(opened.icon) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showStylePicker by rememberSaveable { mutableStateOf(false) }
    var linkMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = ringAt.toInstant().toEpochMilli())

    // A stored draft is offered once, at open, and never applied on its own.
    var offeredDraft by rememberSaveable(stateSaver = jsonStateSaver(ReminderDraft.serializer())) {
        mutableStateOf(draft?.takeIf { it.differsFrom(opened) })
    }

    val days = repeatDaysRaw.toDays()
    val effectiveDays = if (repeatOn) days else emptySet()
    val now = Instant.now()
    // The ring the record will carry: a repeating memo's first occurrence on a
    // chosen day, a once-only memo's date and time as picked.
    val nextRing: Instant? =
        if (repeatOn) {
            if (days.isEmpty()) null else nextOccurrence(now, ringAt.toLocalTime(), days, zone)
        } else ringAt.toInstant()

    val currentDraft = ReminderDraft(
        subject = subject,
        ringAtEpochMillis = ringAt.withSecond(0).withNano(0).toInstant().toEpochMilli(),
        repeatDays = effectiveDays.sorted().map { it.name },
        choreId = choreId,
        taskId = taskId,
        sound = sound,
        colour = colour,
        icon = glyph,
    )
    val isDirty = currentDraft.differsFrom(opened)
    val canSave = subject.isNotBlank() && !(repeatOn && days.isEmpty())

    LaunchedEffect(currentDraft) {
        if (isDirty) onDraftChange(currentDraft)
    }

    fun restoreDraft(restored: ReminderDraft) {
        subject = restored.subject
        ringAt = Instant.ofEpochMilli(restored.ringAtEpochMillis).atZone(zone)
        datePickerState.selectedDateMillis = restored.ringAtEpochMillis
        repeatOn = restored.repeatDays.isNotEmpty()
        repeatDaysRaw = restored.repeatDays.joinToString(",")
        choreId = restored.choreId
        taskId = restored.taskId
        sound = restored.sound
        colour = restored.colour
        glyph = restored.icon
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

    // Guard every dismiss vector (back, scrim tap, swipe-down all funnel through
    // onDismissRequest per LESSONS.md #1/#2). If dirty, bounce the sheet back to
    // visible instead of letting it finish hiding, so we never hit the
    // stuck-invisible-overlay bug while still warning before data loss.
    fun requestDismiss() {
        if (isDirty) {
            sheetScope.launch { sheetState.show() }
            showDiscardConfirm = true
        } else {
            settleDraftOnCleanDismiss()
            sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        }
    }

    // Swipe-down calls the first onDismissRequest it was built with (LESSONS.md #49).
    val latestRequestDismiss by rememberUpdatedState<() -> Unit>({ requestDismiss() })

    fun buildInsert() = ReminderInsert(
        subject = subject.trim(),
        remindAt = (nextRing ?: ringAt.toInstant()).withSecondsZeroed().toString(),
        choreId = choreId.ifBlank { null },
        taskId = taskId.ifBlank { null },
        repeatDays = effectiveDays.sorted().map { it.name },
        sound = sound.ifBlank { null },
        colour = colour.ifBlank { null },
        icon = glyph.ifBlank { null },
    )

    fun setDays(next: Set<DayOfWeek>) {
        repeatDaysRaw = next.toRaw()
    }

    val linkedName: String? = choreId.ifBlank { null }?.let { id -> chores.find { it.id == id }?.label }
        ?: taskId.ifBlank { null }?.let { id -> tasks.find { it.id == id }?.title }
    val linkLabel = when {
        chores.isNotEmpty() && tasks.isEmpty() -> "Linked chore"
        tasks.isNotEmpty() && chores.isEmpty() -> "Linked task"
        else -> "Linked to"
    }
    val context = LocalContext.current
    val defaultAlarmUri = remember { RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) }
    // The system picker, limited to alarm tones with the default offered and no
    // Silent entry (silence is the Silent delivery style, not a per-memo choice).
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val picked = IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        sound = if (picked == null || picked == defaultAlarmUri) "" else picked.toString()
    }
    val soundTitle = remember(sound) {
        if (sound.isBlank()) "Default"
        else runCatching { RingtoneManager.getRingtone(context, Uri.parse(sound))?.getTitle(context) }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: "Custom"
    }
    fun openSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "$featureWord sound")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultAlarmUri)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                if (sound.isBlank()) defaultAlarmUri else Uri.parse(sound),
            )
        }
        runCatching { soundPicker.launch(intent) }
    }
    val timeText = ReminderScheduleText.time(ringAt.toLocalTime())
    val dateText = ringAt.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

    ModalBottomSheet(
        onDismissRequest = { latestRequestDismiss() },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
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
            // A memo linked to a chore or task borrows that item's colour and glyph on
            // the list card, so the bell chip is only a customiser for a standalone memo.
            val linked = choreId.isNotBlank() || taskId.isNotBlank()
            val ownSwatch = Swatch.fromName(colour)
            val ownGlyph = CategoryIcon.fromName(glyph)
            SheetHeader(
                icon = if (!linked && ownGlyph != null) LucideIcons.forCategory(ownGlyph) else LucideIcons.Bell,
                chipContainer = if (!linked && ownSwatch != null) ownSwatch.tintColor() else accents.reminderContainer,
                chipContent = if (!linked && ownSwatch != null) ownSwatch.textColor() else accents.onReminderContainer,
                eyebrow = if (isNew) "New $featureWord" else "Edit $featureWord",
                onIconClick = if (!linked) ({ showStylePicker = true }) else null,
                iconClickLabel = "Choose $featureWord colour and icon",
            ) {
                TitleField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = "$featureWord title",
                    autoFocus = isNew,
                )
            }
            Text(
                text = if (linked) "Colour and icon are inherited from the linked item."
                       else "Tap the bell to give this ${featureWord.lowercase()} a colour and icon.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.inkFaint,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            offeredDraft?.let { offered ->
                DraftResumeRow(
                    itemName = existing?.subject ?: offered.displayName() ?: "a new ${featureWord.lowercase()}",
                    onRestore = { restoreDraft(offered) },
                    onForget = { forgetDraft() },
                )
            }

            // Time (and, for a once-only memo, the date it rings on).
            SheetBlock {
                SettingsRow(icon = LucideIcons.Clock, label = "Time") {
                    TimeValue(text = timeText, onClick = { showTimePicker = true })
                }
                if (!repeatOn) {
                    SheetRowDivider()
                    SettingsRow(icon = LucideIcons.Calendar, label = "Date") {
                        ValueChip(
                            text = dateText,
                            onClick = { showDatePicker = true },
                            contentDescription = "Date: $dateText. Change date",
                        )
                    }
                }
            }

            // Repeat: toggle, then the day cells and shortcut chips while on.
            SheetBlock {
                Column(modifier = Modifier.padding(bottom = if (repeatOn) 12.dp else 0.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .semantics { role = Role.Switch }
                            .toggleable(
                                value = repeatOn,
                                onValueChange = { on ->
                                    repeatOn = on
                                    // Turning repeat on with no days yet: start from the picked day.
                                    if (on && days.isEmpty()) setDays(setOf(ringAt.dayOfWeek))
                                },
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = LucideIcons.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f),
                        )
                        CozySwitch(
                            checked = repeatOn,
                            onCheckedChange = null,
                            modifier = Modifier.semantics { stateDescription = if (repeatOn) "On" else "Off" },
                        )
                    }
                    if (repeatOn) {
                        DayOfWeekRow(
                            selected = days,
                            onToggle = { day -> setDays(if (day in days) days - day else days + day) },
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
                        ) {
                            RepeatPreset.entries.forEach { preset ->
                                val active = days == preset.days
                                ValueChip(
                                    text = preset.label,
                                    onClick = { setDays(preset.days) },
                                    contentDescription = "Repeat ${preset.label.lowercase()}",
                                    container = if (active) MaterialTheme.colorScheme.secondaryContainer
                                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    content = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                                              else MaterialTheme.colorScheme.onSurfaceVariant,
                                    chevron = false,
                                )
                            }
                        }
                        if (days.isEmpty()) {
                            Text(
                                text = "Pick at least one day, or switch Repeat off.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = tokens.inkFaint,
                                modifier = Modifier
                                    .padding(start = 14.dp, end = 14.dp, top = 8.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                    }
                }
            }

            // Sound, then what the memo hangs off.
            SheetBlock {
                SettingsRow(icon = LucideIcons.Volume2, label = "Sound") {
                    ValueChip(
                        text = soundTitle,
                        onClick = { openSoundPicker() },
                        contentDescription = "Sound: $soundTitle. Change sound",
                    )
                }
                if (chores.isNotEmpty() || tasks.isNotEmpty()) {
                    SheetRowDivider()
                    SettingsRow(icon = LucideIcons.Home, label = linkLabel) {
                        Box {
                            ValueChip(
                                text = linkedName ?: "None",
                                onClick = { linkMenuOpen = true },
                                contentDescription = "$linkLabel: ${linkedName ?: "none"}. Change link",
                            )
                            DropdownMenu(expanded = linkMenuOpen, onDismissRequest = { linkMenuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = { choreId = ""; taskId = ""; linkMenuOpen = false },
                                )
                                chores.forEach { chore ->
                                    DropdownMenuItem(
                                        text = { Text(if (tasks.isEmpty()) chore.label else "Chore: ${chore.label}") },
                                        onClick = { choreId = chore.id; taskId = ""; linkMenuOpen = false },
                                    )
                                }
                                tasks.forEach { task ->
                                    DropdownMenuItem(
                                        text = { Text(if (chores.isEmpty()) task.title else "Task: ${task.title}") },
                                        onClick = { taskId = task.id; choreId = ""; linkMenuOpen = false },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Text(
                text = "The sound plays with the Alarm style. The Notification style uses its channel's sound.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.inkFaint,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            NextRingBanner(nextRing = nextRing, now = now, zone = zone, repeating = repeatOn)

            if (existing != null) {
                ExistingMeta(existing = existing)
            }

            SheetPrimaryRow(
                actionLabel = "Save",
                actionEnabled = canSave,
                onCancel = { requestDismiss() },
                onAction = {
                    onDraftClear()
                    onSave(buildInsert())
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
            )

            if (existing != null && (onArchiveToggle != null || onDelete != null)) {
                val isArchived = existing.archivedAt != null
                TertiaryLinkRow(
                    links = listOfNotNull(
                        if (onArchiveToggle != null) TertiaryLink(
                            icon = LucideIcons.Archive,
                            label = if (isArchived) "Unarchive" else "Archive",
                            onClick = {
                                onDraftClear()
                                sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                                    onArchiveToggle(!isArchived)
                                    onDismiss()
                                }
                            },
                        ) else null,
                        if (onDelete != null) TertiaryLink(
                            icon = LucideIcons.Trash,
                            label = "Delete",
                            onClick = { showDeleteConfirm = true },
                            destructive = true,
                        ) else null,
                    ),
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // The picker reports UTC midnight of the chosen day (see EditTaskSheet).
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        ringAt = ringAt.with(picked)
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
        SheetTimePickerDialog(
            initialHour = ringAt.hour,
            initialMinute = ringAt.minute,
            onConfirm = { h, m ->
                ringAt = ringAt.withHour(h).withMinute(m).withSecond(0).withNano(0)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showStylePicker) {
        ReminderStyleDialog(
            colour = colour,
            icon = glyph,
            featureWord = featureWord,
            onConfirm = { pickedColour, pickedIcon ->
                colour = pickedColour
                glyph = pickedIcon
                showStylePicker = false
            },
            onDismiss = { showStylePicker = false },
        )
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${featureWord.lowercase()}?") },
            text = { Text("This ${featureWord.lowercase()} will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDraftClear()
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDelete()
                        onDismiss()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDiscardConfirm) {
        DiscardChangesDialog(
            itemName = existing?.subject ?: subject.trim().ifBlank { null },
            onKeepEditing = { showDiscardConfirm = false },
            onDiscard = {
                showDiscardConfirm = false
                onDraftClear()
                sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        )
    }
}

private fun Instant.withSecondsZeroed(): Instant = truncatedTo(ChronoUnit.MINUTES)

/** The large serif time on the Time row ("7:00" with a smaller "AM"); tapping opens the picker. */
@Composable
private fun TimeValue(text: String, onClick: () -> Unit) {
    val parts = text.split(' ', limit = 2)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .semantics {
                role = Role.Button
                contentDescription = "Time: $text. Change time"
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = parts[0],
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (parts.size > 1) {
            Text(
                text = parts[1],
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

/**
 * S M T W T F S: seven circular cells, each a 44dp checkbox named after its
 * day; the selected ones fill with sage.
 */
@Composable
private fun DayOfWeekRow(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        DAY_CELLS.forEach { day ->
            val isSelected = day in selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .semantics {
                        role = Role.Checkbox
                        contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    }
                    .toggleable(value = isSelected, onValueChange = { onToggle(day) }),
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Next ring · Wed, 7:00 AM" on the sage tint, with "in 4 days" on the right. */
@Composable
private fun NextRingBanner(nextRing: Instant?, now: Instant, zone: ZoneId, repeating: Boolean) {
    val (headline, relative) = when {
        nextRing == null -> "Next ring · pick a day" to ""
        !nextRing.isAfter(now) && !repeating -> "Rings · ${ReminderScheduleText.bannerWhen(nextRing, zone)}" to "already passed"
        else -> "Next ring · ${ReminderScheduleText.bannerWhen(nextRing, zone)}" to ReminderScheduleText.bannerRelative(nextRing, now)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(
            imageVector = LucideIcons.Bell,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = headline,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
        if (relative.isNotEmpty()) {
            Text(
                text = relative,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/** "added 12 Aug · archived": the record's own dates, kept from the previous sheet. */
@Composable
private fun ExistingMeta(existing: ReminderDto) {
    val added = remember(existing.createdAt) {
        existing.createdAt.takeIf { it.isNotEmpty() }?.let { raw ->
            runCatching {
                Instant.parse(raw).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            }.getOrNull()
        }
    }
    val parts = listOfNotNull(added?.let { "added $it" }, if (existing.archivedAt != null) "archived" else null)
    if (parts.isNotEmpty()) {
        MetaCaption(text = parts.joinToString(" · "), uppercase = false, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

/**
 * The standalone memo's colour + icon picker, opened from the bell chip. A leading
 * "Default" option on each row clears the pick (the bell on the reminder accent).
 * Local state until Save, so Cancel leaves the memo untouched.
 */
@Composable
private fun ReminderStyleDialog(
    colour: String,
    icon: String,
    featureWord: String,
    onConfirm: (colour: String, icon: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pickedColour by remember { mutableStateOf(Swatch.fromName(colour)) }
    var pickedIcon by remember { mutableStateOf(CategoryIcon.fromName(icon)) }
    // The colour the icon preview wears, so it echoes the chip on the list card.
    val previewSwatch = pickedColour
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$featureWord colour & icon") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Colour",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    StyleSwatch(
                        selected = pickedColour == null,
                        label = "Default colour",
                        container = MaterialTheme.colorScheme.surfaceContainerHigh,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                        border = MaterialTheme.colorScheme.outline,
                        glyph = LucideIcons.Bell,
                        onClick = { pickedColour = null },
                    )
                    Swatch.categoryPalette.forEach { swatch ->
                        StyleSwatch(
                            selected = pickedColour == swatch,
                            label = "${swatch.displayName} colour",
                            container = swatch.tintColor(),
                            content = swatch.textColor(),
                            border = swatch.spineColor(),
                            glyph = null,
                            onClick = { pickedColour = swatch },
                        )
                    }
                }
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // The icon chips echo the picked colour's saturated ring so both
                // rows read as one family; with no colour picked they bound
                // themselves against the pale dialog with a neutral outline.
                val iconBorder = previewSwatch?.spineColor() ?: MaterialTheme.colorScheme.outline
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    StyleSwatch(
                        selected = pickedIcon == null,
                        label = "Default bell",
                        container = previewSwatch?.tintColor() ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                        content = previewSwatch?.textColor() ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        border = iconBorder,
                        glyph = LucideIcons.Bell,
                        onClick = { pickedIcon = null },
                    )
                    CategoryIcon.pickerSet.forEach { option ->
                        StyleSwatch(
                            selected = pickedIcon == option,
                            label = "Icon: ${option.label}",
                            container = previewSwatch?.tintColor() ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                            content = previewSwatch?.textColor() ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            border = iconBorder,
                            glyph = LucideIcons.forCategory(option),
                            onClick = { pickedIcon = option },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickedColour?.name ?: "", pickedIcon?.name ?: "") }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * One 44dp option in the memo style picker: a tinted circle, ringed when
 * selected. The tints are pale washes that all but vanish on the equally pale
 * dialog surface, so every chip carries a [border] hairline to bound it. For a
 * colour chip that is the swatch's saturated spine, which also carries the hue
 * the pale fill barely shows; the neutral chips take a scheme outline. Both
 * clear the surface in light, dark and high-contrast, so the chips read as
 * chips whatever the mode.
 */
@Composable
private fun StyleSwatch(
    selected: Boolean,
    label: String,
    container: Color,
    content: Color,
    border: Color,
    glyph: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
) {
    val ring = MaterialTheme.colorScheme.onBackground
    val gap = MaterialTheme.colorScheme.surface
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .semantics {
                role = Role.RadioButton
                contentDescription = label
            }
            .selectable(selected = selected, onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (selected) 40.dp else 34.dp)
                .clip(CircleShape)
                .background(if (selected) ring else Color.Transparent)
                .padding(if (selected) 2.dp else 0.dp)
                .clip(CircleShape)
                .background(if (selected) gap else Color.Transparent)
                .padding(if (selected) 2.dp else 0.dp)
                .clip(CircleShape)
                .background(container)
                .border(1.5.dp, border, CircleShape),
        ) {
            if (glyph != null) {
                Icon(imageVector = glyph, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            }
        }
    }
}
