package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.mapgie.dash.data.model.GENERAL_CATEGORY
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.ui.components.sheet.DraftResumeRow
import com.mapgie.dash.ui.components.sheet.OwnerAvatarRow
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
import com.mapgie.dash.util.calendarEventWithoutTime
import kotlinx.coroutines.launch

/**
 * The Edit sheet for chores (handoff 7a), one grammar with the task sheet: the
 * title is the input, then one grouped settings card of compact rows (Category
 * value chip · Owner avatar row · Repeat every stepper · NFC tag), the same
 * Cancel + sage Save footer as the Log sheet, and a centred tertiary row (Add
 * to calendar · Share · Archive). With [chore] null it is the New chore sheet:
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
    categorySwatch: Swatch?,
    owners: List<String>,
    categories: List<String>,
    sheetState: SheetState,
    onSave: (tagId: String, label: String, category: String?, owner: String?, intervalDays: Double?) -> Unit,
    onArchiveToggle: (chore: Chore, archive: Boolean) -> Unit,
    onWriteTag: (tagId: String) -> Unit,
    onDismiss: () -> Unit,
    initialTagId: String = "",
    draft: ChoreDraft? = null,
    onDraftChange: (ChoreDraft) -> Unit = {},
    onDraftClear: () -> Unit = {},
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
    val opened = remember { ChoreDraft.of(chore, initialTagId) }
    var label by rememberSaveable { mutableStateOf(opened.label) }
    var category by rememberSaveable { mutableStateOf(opened.category) }
    var owner by rememberSaveable { mutableStateOf(opened.owner) }
    var interval by rememberSaveable { mutableStateOf(opened.intervalDays) }
    var tagId by rememberSaveable { mutableStateOf(opened.tagId) }

    var categoryMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showNewCategory by rememberSaveable { mutableStateOf(false) }
    var showIntervalEntry by rememberSaveable { mutableStateOf(false) }
    var showArchiveConfirm by rememberSaveable { mutableStateOf(false) }
    var showShareChoice by rememberSaveable { mutableStateOf(false) }
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }

    // A stored draft is offered once, at open, and never applied on its own. The
    // offer itself is saved so a rotation does not repeat it for edits that
    // rememberSaveable already brought back.
    var offeredDraft by rememberSaveable(stateSaver = jsonStateSaver(ChoreDraft.serializer())) {
        mutableStateOf(draft?.takeIf { it.differsFrom(opened) })
    }

    val currentDraft = ChoreDraft(label = label, category = category, owner = owner, intervalDays = interval, tagId = tagId)
    val isDirty = currentDraft.differsFrom(opened)

    // Mirror every change into the draft store while the sheet is dirty.
    LaunchedEffect(currentDraft) {
        if (isDirty) onDraftChange(currentDraft)
    }

    fun restoreDraft(restored: ChoreDraft) {
        label = restored.label
        category = restored.category
        owner = restored.owner
        interval = restored.intervalDays
        // A tag ID that arrived with an NFC scan wins over a draft that has none.
        tagId = restored.tagId.ifBlank { tagId }
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

    fun hideThen(action: () -> Unit) {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    fun calendarInfo() = calendarEventWithoutTime(
        title = label.trim().ifBlank { chore?.label ?: "" },
        description = category.trim().ifBlank { null }?.let { "Category: $it" }
    )

    val tone = chore?.statusTone()
    val chipContainer = categorySwatch?.tintColor() ?: tone?.badgeContainerColor() ?: accents.choreContainer
    val chipContent = categorySwatch?.textColor() ?: tone?.textColor() ?: accents.onChoreContainer
    val canSave = label.isNotBlank() && tagId.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = { requestDismiss() },
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
                            container = categorySwatch?.tintColor() ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                            content = categorySwatch?.textColor() ?: MaterialTheme.colorScheme.onSurface,
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
                        valueText = interval?.let { "$it d" } ?: "none",
                        onMinus = { interval = interval?.let { if (it <= 1) null else it - 1 } },
                        onPlus = { interval = (interval ?: 0) + 1 },
                        onValueClick = { showIntervalEntry = true },
                        minusEnabled = interval != null,
                        minusDescription = "Repeat one day less often",
                        plusDescription = "Repeat one day more often",
                        valueDescription = "Repeat every ${interval?.let { "$it days" } ?: "no set interval"}. Type a number",
                    )
                }
                SheetRowDivider()
                SettingsRow(icon = LucideIcons.NfcScan, label = "NFC tag") {
                    if (isNew) {
                        TagIdField(value = tagId, onValueChange = { tagId = it })
                    } else {
                        Text(
                            text = tagId.ifBlank { "no label" },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            color = if (tagId.isNotBlank()) tokens.tagLabel else tokens.inkFaint,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 120.dp),
                        )
                    }
                    if (tagId.isNotBlank()) {
                        ValueChip(
                            text = if (isNew) "Write tag" else "Rewrite",
                            onClick = { hideThen { onWriteTag(tagId.trim()) } },
                            contentDescription = "Write this chore's ID to an NFC tag",
                            chevron = false,
                        )
                    }
                }
            }

            SheetPrimaryRow(
                actionLabel = "Save",
                actionEnabled = canSave,
                onCancel = { requestDismiss() },
                onAction = {
                    val intervalDays = interval?.toDouble()
                    val ownerValue = owner.trim().ifBlank { null }
                    val categoryValue = category.trim().ifBlank { null }
                    onDraftClear()
                    hideThen { onSave(tagId.trim(), label.trim(), categoryValue, ownerValue, intervalDays) }
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
                    label = { Text("Days (blank for none)") },
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

/** Compact inline field for a new chore's tag ID, on the NFC row. */
@Composable
private fun TagIdField(value: String, onValueChange: (String) -> Unit) {
    val tokens = LocalDashTokens.current
    Box(
        modifier = Modifier
            .widthIn(min = 90.dp, max = 150.dp)
            .padding(vertical = 4.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = "Tag ID",
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
                .semantics { contentDescription = "Tag ID" },
        )
    }
}

/**
 * Category picker shared by both edit sheets: the known categories, General
 * when it is not among them, and "New category…".
 */
@Composable
internal fun CategoryMenu(
    expanded: Boolean,
    categories: List<String>,
    onPick: (String) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val options = if (categories.any { it.equals(GENERAL_CATEGORY, ignoreCase = true) }) categories
                  else categories + GENERAL_CATEGORY
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        options.forEach { option ->
            DropdownMenuItem(text = { Text(option) }, onClick = { onPick(option) })
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
