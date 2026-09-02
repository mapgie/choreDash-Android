package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.data.model.SortKey
import com.mapgie.dash.data.model.SortOrder
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.PillShape
import kotlinx.coroutines.launch

/**
 * The outlined sort control at the right end of the filter-chip row, shared by
 * Chores and Tasks. It reads the key and the direction in words ("pressure ·
 * worst first"), never as an arrow; tapping it opens [SortSheet]. The pill is
 * visually small but sits in a 44dp-tall touch target.
 */
@Composable
fun <K : SortKey> SortPill(
    order: SortOrder<K>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDashTokens.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .semantics {
                role = Role.Button
                contentDescription = "Sorted by ${order.pillLabel}. Change sort"
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
    ) {
        Text(
            text = order.pillLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier
                .border(1.5.dp, tokens.pillOutline, PillShape)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/**
 * Small sheet opened by [SortPill]: pick the key, then the direction. Each key
 * names its own two directions ("worst first / freshest first", "A to Z / Z to
 * A") so the choice never needs an arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <K : SortKey> SortSheet(
    title: String,
    keys: List<K>,
    order: SortOrder<K>,
    onOrderChange: (SortOrder<K>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun hideAndDismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = { hideAndDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            SectionLabel(text = title, modifier = Modifier.padding(bottom = 8.dp))
            keys.forEach { key ->
                val selected = order.key == key
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .semantics { role = Role.RadioButton }
                        .selectable(
                            selected = selected,
                            onClick = { onOrderChange(SortOrder(key, reversed = if (selected) order.reversed else false)) },
                        )
                        .padding(horizontal = 4.dp),
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(
                        text = key.label.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionLabel(text = "Direction", modifier = Modifier.padding(bottom = 8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(false, true).forEachIndexed { index, reversed ->
                    SegmentedButton(
                        selected = order.reversed == reversed,
                        onClick = { onOrderChange(SortOrder(order.key, reversed)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        modifier = Modifier.semantics { role = Role.RadioButton },
                        label = {
                            Text(
                                (if (reversed) order.key.secondDirection else order.key.firstDirection)
                                    .replaceFirstChar { it.uppercase() },
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
        }
    }
}
