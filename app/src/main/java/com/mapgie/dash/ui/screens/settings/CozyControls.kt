package com.mapgie.dash.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.PillShape
import com.mapgie.dash.ui.theme.isDarkScheme

/*
 * The Cozy Cream settings grammar (handoff 3a-6 and the 4a sub-screens): a
 * section label, then one grouped card per section whose rows are separated by
 * hairlines. Controls are the design's own: a pill segmented control with a
 * check on the selected cell, an accent-tinted toggle, a rounded-square
 * checkbox, circular steppers and full-width pill buttons. Everything reads
 * its colour from the theme so the same components sit on Zen Dark unchanged.
 */

/** Card corner radius for grouped settings sections (20dp per 3a). */
private val SettingsCardShape = RoundedCornerShape(20.dp)

/**
 * A grouped settings card: the Card token surface with a soft shadow in light
 * and none in dark. Rows inside get [contentPadding] horizontally so their
 * hairlines run edge to edge of the content area, as drawn.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = SettingsCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (isDarkScheme()) 0.dp else 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Hairline between rows in a [SettingsCard]. */
@Composable
fun SettingsHairline(modifier: Modifier = Modifier) {
    HorizontalDivider(thickness = 1.dp, color = LocalDashTokens.current.sheetDivider, modifier = modifier)
}

/** The title and faint subtitle stack every settings row starts with. */
@Composable
fun SettingsRowText(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    titleSize: Int = 14,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = (titleSize + 0.5f).sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = LocalDashTokens.current.inkFaint,
            )
        }
    }
}

/**
 * A row inside a [SettingsCard]: optional [leading] slot, title + subtitle, then
 * [trailing] on the right. Non-interactive on its own; wrap or use
 * [SettingsNavRow] / [SettingsToggleRow] for the tappable variants.
 */
@Composable
fun SettingsCardRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 12.dp),
    ) {
        leading?.invoke()
        SettingsRowText(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** A row that opens a sub-screen: title, subtitle and a faint chevron. */
@Composable
fun SettingsNavRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    SettingsCardRow(
        title = title,
        subtitle = subtitle,
        leading = leading,
        modifier = modifier
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = LucideIcons.ChevronRight,
            contentDescription = null,
            tint = LocalDashTokens.current.sectionCount,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** The design's toggle: accent track when on, outline track when off, card-coloured knob. */
@Composable
fun CozySwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDashTokens.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
            checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = tokens.pillOutline,
            uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}

/** A toggle row inside a [SettingsCard]; the whole row is the switch. */
@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCardRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier
            .semantics { role = Role.Switch }
            .toggleable(value = checked, onValueChange = onCheckedChange),
    ) {
        CozySwitch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.semantics { stateDescription = if (checked) "On" else "Off" },
        )
    }
}

/**
 * The design's segmented control: a full-width pill with a 1.5dp outline,
 * hairline dividers between cells, and the selected cell filled with the
 * accent tint and led by a check glyph. Each cell is a 44dp+ radio target.
 */
@Composable
fun <T> CozySegmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDashTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .border(1.5.dp, tokens.pillOutline, PillShape),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            if (index > 0) {
                Box(
                    Modifier
                        .width(1.5.dp)
                        .height(46.dp)
                        .background(tokens.pillOutline),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .semantics { role = Role.RadioButton }
                    .selectable(selected = isSelected, onClick = { onSelect(option) })
                    .padding(horizontal = 6.dp),
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = LucideIcons.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(15.dp),
                    )
                    Box(Modifier.width(5.dp))
                }
                Text(
                    label(option),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A checkbox row: 24dp rounded square with a 2dp accent border, filled with a check when on. */
@Composable
fun CozyCheckboxRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { role = Role.Checkbox }
            .toggleable(value = checked, onValueChange = onCheckedChange)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) accent else Color.Transparent)
                .border(2.dp, accent, RoundedCornerShape(6.dp)),
        ) {
            if (checked) {
                Icon(
                    imageVector = LucideIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        SettingsRowText(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
    }
}

/** Full-width pill filled with the accent tint (the "What's New" button). */
@Composable
fun TintPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.5.sp)
    }
}

/** Full-width pill filled with the accent itself (the Save button on forms). */
@Composable
fun AccentPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.5.sp)
    }
}

/** Full-width outlined pill with accent text (the "Reset to defaults" button). */
@Composable
fun OutlinePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(1.5.dp, LocalDashTokens.current.pillOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

/**
 * One 32dp circular stepper button (minus in rose, plus in sage) inside a 44dp
 * touch target. Disabled buttons fade to the section-count ink, so "can't go
 * lower" is visible as well as announced.
 */
@Composable
fun StepperCircle(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDashTokens.current
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(44.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.5.dp, tokens.pillOutline, CircleShape),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) tint else tokens.sectionCount,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Minus · value · plus, the Display screen's lead-time control. */
@Composable
fun StepperControl(
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementDescription: String,
    incrementDescription: String,
    modifier: Modifier = Modifier,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        StepperCircle(
            icon = LucideIcons.Minus,
            contentDescription = decrementDescription,
            tint = MaterialTheme.colorScheme.error,
            enabled = canDecrement,
            onClick = onDecrement,
        )
        Text(
            valueText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .width(56.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepperCircle(
            icon = LucideIcons.Plus,
            contentDescription = incrementDescription,
            tint = MaterialTheme.colorScheme.secondary,
            enabled = canIncrement,
            onClick = onIncrement,
        )
    }
}

/**
 * Sub-screen header (4a): the 5dp accent strip, then a back chevron and the
 * lowercase serif title with an accent-tinted full stop ("appearance.").
 */
@Composable
fun SubScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    )
                )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 2.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = LucideIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = buildAnnotatedString {
                    append(title.lowercase())
                    withStyle(SpanStyle(color = accent)) { append(".") }
                },
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
    }
}

/** Section label positioned for a sub-screen's 18dp content inset. */
@Composable
fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    SectionLabel(
        text = text,
        color = LocalDashTokens.current.inkFaint,
        modifier = modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp),
    )
}

/** Caption text under a control, in the faint ink. */
@Composable
fun SettingsCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
        color = LocalDashTokens.current.inkFaint,
        modifier = modifier.padding(horizontal = 6.dp),
    )
}
