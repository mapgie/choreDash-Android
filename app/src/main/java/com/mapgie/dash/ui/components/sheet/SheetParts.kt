package com.mapgie.dash.ui.components.sheet

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.ui.components.core.OwnerAvatar
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.PillShape
import com.mapgie.dash.ui.theme.isDarkScheme
import com.mapgie.dash.ui.theme.ownerColorFor

/**
 * The shared anatomy of every bottom sheet (handoff 6a / 7a): one header, one
 * grouped block style, one primary row, one utility row, one tertiary link row.
 * Chore and task sheets compose these rather than each drawing their own.
 */

/** Sheet content padding: 20dp sides, section gap 18dp (log) or 16dp (edit). */
val SheetPadding = 20.dp

/**
 * Sheet header: 44dp category chip · eyebrow · [content] (title, badge, meta)
 * · optional owner avatar on the right.
 */
@Composable
fun SheetHeader(
    icon: ImageVector,
    chipContainer: Color,
    chipContent: Color,
    eyebrow: String,
    ownerHandle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(Dimens.sheetIconChipSize)
                .clip(CircleShape)
                .background(chipContainer),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = chipContent, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            SectionLabel(text = eyebrow)
            content()
        }
        if (!ownerHandle.isNullOrBlank()) {
            OwnerAvatar(handle = ownerHandle, size = Dimens.sheetAvatarSize, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** A grouped inner block: sheet-block ground, 16dp radius, hairline outline in light. */
@Composable
fun SheetBlock(
    modifier: Modifier = Modifier,
    radius: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalDashTokens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(tokens.sheetBlock)
            .border(1.dp, tokens.sheetBlockOutline, RoundedCornerShape(radius)),
        content = content,
    )
}

/** Hairline between rows inside a [SheetBlock]. */
@Composable
fun SheetRowDivider() {
    HorizontalDivider(thickness = 1.dp, color = LocalDashTokens.current.sheetDivider)
}

/** Section label with an optional trailing link ("HISTORY   All 14 ›"). */
@Composable
fun SheetSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        SectionLabel(text = text, color = LocalDashTokens.current.inkFaint)
        trailing?.invoke()
    }
}

/** When a completion happened: the DONE segmented control's three cells. */
enum class DoneWhen(val label: String) {
    JUST_NOW("Just now"),
    EARLIER_TODAY("Earlier today"),
    PICK("Pick"),
}

/**
 * The DONE control: three cells on the sheet-block ground, the selected one on
 * the sage tint. "Pick" carries a calendar glyph and opens the date-time picker.
 */
@Composable
fun DoneWhenControl(
    selected: DoneWhen,
    onSelect: (DoneWhen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDashTokens.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetSectionLabel(text = "Done")
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(tokens.sheetBlock)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(3.dp),
        ) {
            DoneWhen.entries.forEach { option ->
                val isSelected = option == selected
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .semantics { role = Role.RadioButton }
                        .selectable(selected = isSelected, onClick = { onSelect(option) })
                        .padding(horizontal = 6.dp),
                ) {
                    if (option == DoneWhen.PICK) {
                        Icon(
                            imageVector = LucideIcons.Calendar,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        text = if (option == DoneWhen.PICK) "Pick…" else option.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Primary row: outlined Cancel (weight 1) and the filled sage action (weight
 * 1.6) with a check glyph. Sage is the one primary-action colour in both
 * themes; there are no rose primary buttons.
 */
@Composable
fun SheetPrimaryRow(
    actionLabel: String,
    onAction: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
    actionIcon: ImageVector = LucideIcons.Check,
) {
    val tokens = LocalDashTokens.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedButton(
            onClick = onCancel,
            shape = PillShape,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, tokens.pillOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
        ) {
            Text("Cancel", fontWeight = FontWeight.ExtraBold)
        }
        Button(
            onClick = onAction,
            enabled = actionEnabled,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            modifier = Modifier
                .weight(1.6f)
                .heightIn(min = 48.dp),
        ) {
            Icon(imageVector = actionIcon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(actionLabel, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** One 40dp circular icon button with an 11sp label beneath, for [UtilityRow]. */
data class UtilityAction(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String,
    val onClick: () -> Unit,
    val active: Boolean = false,
    val enabled: Boolean = true,
)

/** The utility row: actions spread evenly (Calendar · Pin · Remind · Tag · Edit). */
@Composable
fun UtilityRow(actions: List<UtilityAction>, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
    ) {
        actions.forEach { action ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .widthIn(min = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .semantics {
                        role = Role.Button
                        contentDescription = action.contentDescription
                        if (action.active) stateDescription = "On"
                    }
                    .clickable(enabled = action.enabled, onClick = action.onClick)
                    .padding(vertical = 2.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (action.active) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = when {
                            !action.enabled -> MaterialTheme.colorScheme.outline
                            action.active -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(17.dp),
                    )
                }
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (action.enabled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )
            }
        }
    }
}

/** One centred tertiary link ("Add to calendar", "Share", "Archive"). */
data class TertiaryLink(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
)

/** Tertiary link row: centred, 13sp/800 faint ink, destructive links in rose text. */
@Composable
fun TertiaryLinkRow(links: List<TertiaryLink>, modifier: Modifier = Modifier) {
    val tokens = LocalDashTokens.current
    val roseText = Color(Swatch.ROSE.tones(isDarkScheme()).textArgb)
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        links.forEach { link ->
            val colour = if (link.destructive) roseText else tokens.inkFaint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(PillShape)
                    .semantics { role = Role.Button }
                    .clickable(onClick = link.onClick)
                    .padding(horizontal = 6.dp),
            ) {
                Icon(imageVector = link.icon, contentDescription = null, tint = colour, modifier = Modifier.size(14.dp))
                Text(
                    text = link.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                    color = colour,
                    maxLines = 1,
                )
            }
        }
    }
}

// ── Edit-sheet rows and controls ─────────────────────────────────────────────

/** One row of the grouped settings card: 17dp icon · label · [control] on the right. */
@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    control: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f),
        )
        control()
    }
}

/**
 * The compact pill that opens a picker: value text, optional trailing chevron,
 * tinted by the caller ([container]/[content]) or neutral by default.
 */
@Composable
fun ValueChip(
    text: String,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: Color = MaterialTheme.colorScheme.onSurface,
    chevron: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(PillShape)
                .background(container)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold),
                color = content,
                maxLines = 1,
            )
            if (chevron) {
                Icon(imageVector = LucideIcons.ChevronDown, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
            }
        }
    }
}

/** Minus · value · plus in one pill. Tapping the value opens a direct-entry dialog. */
@Composable
fun StepperPill(
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onValueClick: () -> Unit,
    minusDescription: String,
    plusDescription: String,
    valueDescription: String,
    modifier: Modifier = Modifier,
    minusEnabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp),
    ) {
        StepperButton(icon = LucideIcons.Minus, description = minusDescription, enabled = minusEnabled, onClick = onMinus)
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .widthIn(min = 54.dp)
                .heightIn(min = 44.dp)
                .clip(PillShape)
                .semantics {
                    role = Role.Button
                    contentDescription = valueDescription
                }
                .clickable(onClick = onValueClick)
                .padding(horizontal = 4.dp, vertical = 13.dp),
        )
        StepperButton(icon = LucideIcons.Plus, description = plusDescription, enabled = true, onClick = onPlus)
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** A pill of mutually exclusive segments (task priority: Low · Normal · High). */
@Composable
fun <T> SegmentPill(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                text = label(option),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold),
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(PillShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .semantics { role = Role.RadioButton }
                    .selectable(selected = isSelected, onClick = { onSelect(option) })
                    .padding(horizontal = 11.dp, vertical = 13.dp),
            )
        }
    }
}

/**
 * Owner picker: one 30dp avatar per household member plus "Any" (unassigned)
 * last. The selected avatar wears a 2dp sage ring.
 */
@Composable
fun OwnerAvatarRow(
    owners: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isDarkScheme()
    val ring = MaterialTheme.colorScheme.secondary
    val ringGap = LocalDashTokens.current.sheetBlock
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        (owners.map<String, String?> { it } + listOf<String?>(null)).forEach { owner ->
            val isSelected = (owner ?: "") == (selected ?: "")
            val tone = owner?.let { ownerColorFor(it, dark) }
            val container = when {
                isSelected && tone != null -> tone.container
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val content = when {
                isSelected && tone != null -> tone.onContainer
                isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = owner?.let { "Owner: $it" } ?: "Owner: anyone"
                    }
                    .selectable(selected = isSelected, onClick = { onSelect(owner) }),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (isSelected) 38.dp else 30.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ring else Color.Transparent)
                        .padding(if (isSelected) 2.dp else 0.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ringGap else Color.Transparent)
                        .padding(if (isSelected) 2.dp else 0.dp)
                        .clip(CircleShape)
                        .background(container),
                ) {
                    Text(
                        text = owner?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "Any",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (owner == null) 10.sp else 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = content,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * The title as the input: Lora 30/600 with a 1.5dp sage underline, no box, no
 * floating label. Focus is requested when [autoFocus] is set (new items).
 */
@Composable
fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    if (autoFocus) {
        androidx.compose.runtime.LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.headlineLarge,
                    color = LocalDashTokens.current.inkFaint,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = placeholder },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(MaterialTheme.colorScheme.secondary),
        )
    }
}

/** The soft notes block: pencil glyph and a growing multi-line field. */
@Composable
fun NotesBlock(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDashTokens.current
    SheetBlock(modifier = modifier, radius = 14.dp) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = LucideIcons.Pencil,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp),
            )
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                        color = tokens.inkFaint,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Notes" },
                )
            }
        }
    }
}

/** Read-only notes block for the log/done sheet. */
@Composable
fun NotesReadBlock(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetSectionLabel(text = "Notes")
        SheetBlock(radius = 14.dp) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                color = LocalContentColor.current,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

/** Shared time picker dialog for "Earlier today" and "Pick…". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
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
