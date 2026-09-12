package com.mapgie.dash.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreColourAxes
import com.mapgie.dash.data.model.ColourChoresBy
import com.mapgie.dash.data.model.Severity
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.data.model.TagDto
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.ui.components.ChoreCard
import com.mapgie.dash.ui.components.TaskCard
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.components.core.StatusBadge
import com.mapgie.dash.ui.components.sheet.SegmentPill
import com.mapgie.dash.ui.components.sheet.SheetBlock
import com.mapgie.dash.ui.components.sheet.SheetRowDivider
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.spineColor
import com.mapgie.dash.ui.theme.tone
import java.time.Duration
import java.time.Instant

/**
 * Settings › Colours (handoff 9a). Two independent axes in one grouped card,
 * COLOUR EACH ELEMENT BY: "Spine + badge" (the left bar and due pill, default
 * Severity) and "Icon" (the round category glyph, default Category), each a
 * two-cell Severity | Category pill. Below it the four SEVERITY COLOURS rows
 * with the six-swatch palette plus a None cell (selected cell wears a 2dp ink
 * ring), then a PREVIEW of three real chore cards (overdue / due soon / fresh)
 * and one low-priority task card, rendered with the current axes, captioned
 * "spine severity · icon category", with a swatch row above them to try the
 * icon's category colour against the severity spine. That row is preview-only;
 * it changes no category's real colour.
 */
@Composable
internal fun ColoursSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val axes = settings?.colourAxes ?: ChoreColourAxes()
    val severitySwatches = settings?.severitySwatches ?: Severity.defaults
    val tokens = LocalDashTokens.current

    // Three real cards, one per severity: intervals and last-done times chosen
    // mid-window so the badges read "35d over", "1d left" and "12d left" all day.
    val previews = remember {
        listOf(
            previewChore("overdue", "Water softener", intervalDays = 30.0, doneHoursAgo = 65 * 24 + 6),
            previewChore("soon", "Trash & recycling", intervalDays = 7.0, doneHoursAgo = 5 * 24 + 18),
            previewChore("fresh", "Vacuum living room", intervalDays = 14.0, doneHoursAgo = 24 + 18),
        )
    }
    // The fourth severity only ever shows on a task, so the fourth preview is one.
    val previewTask = remember {
        TaskDto(id = "preview-low", title = "Sort the loft", category = "House", priority = "lower", duePeriod = "eventually")
    }
    var previewSwatch by rememberSaveable { mutableStateOf(catalog.effectiveSwatch("House").name) }
    val contrastSwatch = Swatch.fromName(previewSwatch) ?: Swatch.GOLD

    SettingsSubScreenScaffold(title = "Colours", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Colour each element by", modifier = Modifier.padding(horizontal = 6.dp))
                SheetBlock {
                    AxisRow(
                        title = "Spine + badge",
                        subtitle = "the left bar & due pill",
                        selected = axes.spineAndBadge,
                        onSelect = { viewModel.setColourSpineBy(it) },
                    )
                    SheetRowDivider()
                    AxisRow(
                        title = "Icon",
                        subtitle = "the round category glyph",
                        selected = axes.icon,
                        onSelect = { viewModel.setColourIconBy(it) },
                    )
                }
                Text(
                    "Set the spine & badge and the icon independently. For example, spine by severity " +
                        "to show urgency, icon by category to show what it is.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.inkFaint,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Severity colours", modifier = Modifier.padding(horizontal = 6.dp))
                SheetBlock {
                    Severity.entries.forEachIndexed { index, severity ->
                        // Null means the user chose None for this severity.
                        val selected = severitySwatches[severity]
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    severity.label,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                                )
                                StatusBadge(text = severity.sampleBadge, tone = severity.tone())
                            }
                            SwatchRow(
                                swatches = Swatch.severityPalette,
                                selected = selected,
                                onSelect = { viewModel.setSeveritySwatch(severity, it) },
                                onSelectNone = { viewModel.setSeveritySwatch(severity, null) },
                                groupLabel = severity.label,
                            )
                        }
                        if (index < Severity.entries.lastIndex) SheetRowDivider()
                    }
                }
                Text(
                    "Tasks use these too: fresh unless the due date is close, and priority joins in. " +
                        "High priority counts as due soon, low priority wears the fourth colour. " +
                        "None keeps that element plain.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.inkFaint,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                ) {
                    SectionLabel(text = "Preview")
                    Text(
                        text = axes.caption,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = tokens.inkFaint,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Text(
                    "Contrast icon category colour",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                SwatchRow(
                    swatches = Swatch.categoryPalette,
                    selected = contrastSwatch,
                    onSelect = { previewSwatch = it.name },
                    groupLabel = "Preview category",
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    previews.forEach { chore ->
                        ChoreCard(
                            chore = chore,
                            showOwner = false,
                            icon = LucideIcons.Droplet,
                            inset = 0.dp,
                            spineSwatch = axes.spineSwatch(contrastSwatch),
                            iconSwatch = axes.iconSwatch(contrastSwatch),
                        )
                    }
                    TaskCard(
                        task = previewTask,
                        icon = LucideIcons.Home,
                        showOwner = false,
                        inset = 0.dp,
                        spineSwatch = axes.spineSwatch(contrastSwatch),
                        iconSwatch = axes.iconSwatch(contrastSwatch),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** One axis of the COLOUR EACH ELEMENT BY card: title and caption, then the Severity | Category pill. */
@Composable
private fun AxisRow(
    title: String,
    subtitle: String,
    selected: ColourChoresBy,
    onSelect: (ColourChoresBy) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = LocalDashTokens.current.inkFaint,
            )
        }
        SegmentPill(
            options = ColourChoresBy.entries,
            selected = selected,
            label = { it.label },
            onSelect = onSelect,
        )
    }
}

private fun previewChore(id: String, label: String, intervalDays: Double, doneHoursAgo: Long): Chore =
    Chore.from(
        tag = TagDto(
            id = "preview-$id", tagId = "preview-$id", label = label,
            category = "House", owner = null, intervalDays = intervalDays,
        ),
        lastScanned = Instant.now().minus(Duration.ofHours(doneHoursAgo)),
        lastScanId = null,
    )

/**
 * A row of 30dp colour swatches; the selected one wears a 2dp ink ring with a
 * 2dp gap. Each swatch is a 44dp radio target named after its colour, so the
 * choice is never colour-only. With [onSelectNone] the row ends in a None cell
 * (an outlined circle with a dash), selected when [selected] is null.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SwatchRow(
    swatches: List<Swatch>,
    selected: Swatch?,
    onSelect: (Swatch) -> Unit,
    groupLabel: String,
    modifier: Modifier = Modifier,
    onSelectNone: (() -> Unit)? = null,
) {
    val ring = MaterialTheme.colorScheme.onBackground
    val gap = LocalDashTokens.current.sheetBlock
    // Eleven 44dp targets are wider than a phone, so the row wraps rather than
    // clipping the last swatches to slivers at the edge.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        swatches.forEach { swatch ->
            val isSelected = swatch == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = "$groupLabel colour: ${swatch.displayName}"
                    }
                    .selectable(selected = isSelected, onClick = { onSelect(swatch) }),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 38.dp else 30.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ring else Color.Transparent)
                        .padding(if (isSelected) 2.dp else 0.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) gap else Color.Transparent)
                        .padding(if (isSelected) 2.dp else 0.dp)
                        .clip(CircleShape)
                        .background(swatch.spineColor()),
                )
            }
        }
        if (onSelectNone != null) {
            val isSelected = selected == null
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = "$groupLabel colour: None"
                    }
                    .selectable(selected = isSelected, onClick = onSelectNone),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (isSelected) 38.dp else 30.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ring else Color.Transparent)
                        .padding(if (isSelected) 2.dp else 0.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) gap else Color.Transparent)
                        .padding(if (isSelected) 2.dp else 0.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                ) {
                    Icon(
                        imageVector = LucideIcons.Minus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
