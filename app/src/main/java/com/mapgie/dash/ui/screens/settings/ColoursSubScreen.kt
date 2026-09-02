package com.mapgie.dash.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ColourChoresBy
import com.mapgie.dash.data.model.Severity
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.data.model.TagDto
import com.mapgie.dash.ui.components.ChoreCard
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.components.core.StatusBadge
import com.mapgie.dash.ui.components.sheet.SheetBlock
import com.mapgie.dash.ui.components.sheet.SheetRowDivider
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.StatusTone
import com.mapgie.dash.ui.theme.spineColor
import java.time.Duration
import java.time.Instant

/**
 * Settings › Colours (handoff 5a): a segmented "Colour chores by: Severity |
 * Category" control, three severity rows each with the six-swatch palette
 * (selected swatch wears a 2dp ink ring), and a live preview card.
 */
@Composable
internal fun ColoursSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val colourBy = settings?.colourChoresBy ?: ColourChoresBy.SEVERITY
    val severitySwatches = settings?.severitySwatches ?: Severity.defaults
    val tokens = LocalDashTokens.current

    val previewChore = remember {
        Chore.from(
            tag = TagDto(
                id = "preview", tagId = "preview", label = "Water softener",
                category = "House", owner = "M", intervalDays = 30.0,
            ),
            lastScanned = Instant.now().minus(Duration.ofDays(49)),
            lastScanId = null,
        )
    }

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
                SectionLabel(text = "Colour chores by", modifier = Modifier.padding(horizontal = 6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ColourChoresBy.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = colourBy == mode,
                            onClick = { viewModel.setColourChoresBy(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ColourChoresBy.entries.size),
                            modifier = Modifier.semantics { role = Role.RadioButton },
                            label = { Text(mode.label) }
                        )
                    }
                }
                Text(
                    "Severity tints the spine, icon and badge by how overdue a chore is. " +
                        "Category uses each category's own colour instead.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.inkFaint,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Severity colours", modifier = Modifier.padding(horizontal = 6.dp))
                SheetBlock {
                    Severity.entries.forEachIndexed { index, severity ->
                        val selected = severitySwatches[severity] ?: severity.defaultSwatch
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
                                StatusBadge(
                                    text = severity.sampleBadge,
                                    tone = when (severity) {
                                        Severity.OVERDUE -> StatusTone.CRITICAL
                                        Severity.DUE_SOON -> StatusTone.ATTENTION
                                        Severity.FRESH -> StatusTone.OK
                                    },
                                )
                            }
                            SwatchRow(
                                swatches = Swatch.severityPalette,
                                selected = selected,
                                onSelect = { viewModel.setSeveritySwatch(severity, it) },
                                groupLabel = severity.label,
                            )
                        }
                        if (index < Severity.entries.lastIndex) SheetRowDivider()
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Preview", modifier = Modifier.padding(horizontal = 6.dp))
                ChoreCard(
                    chore = previewChore,
                    showOwner = true,
                    icon = LucideIcons.Droplet,
                    inset = 0.dp,
                    categorySwatch = if (colourBy == ColourChoresBy.CATEGORY)
                        catalog.effectiveSwatch(previewChore.category) else null,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * A row of 30dp colour swatches; the selected one wears a 2dp ink ring with a
 * 2dp gap. Each swatch is a 44dp radio target named after its colour, so the
 * choice is never colour-only.
 */
@Composable
internal fun SwatchRow(
    swatches: List<Swatch>,
    selected: Swatch?,
    onSelect: (Swatch) -> Unit,
    groupLabel: String,
    modifier: Modifier = Modifier,
) {
    val ring = MaterialTheme.colorScheme.onBackground
    val gap = LocalDashTokens.current.sheetBlock
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = modifier) {
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
                        .background(swatch.spineColor()),
                )
            }
        }
    }
}
