package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
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
 * The sort control, split into two pills (as the handoff asks): the first toggles
 * the direction in place, reading the current key's own words ("highest first" ⇄
 * "lowest first"); the second names the key and opens [SortSheet] to change what
 * the list sorts by. Sits in a 44dp-tall touch target like the old single pill.
 */
@Composable
fun <K : SortKey> SortControls(
    order: SortOrder<K>,
    onOrderChange: (SortOrder<K>) -> Unit,
    onPickKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val direction = (if (order.reversed) order.key.secondDirection else order.key.firstDirection)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortPillButton(
            text = direction,
            contentDescription = "Direction: $direction. Reverse order",
            onClick = { onOrderChange(SortOrder(order.key, reversed = !order.reversed)) },
        )
        SortPillButton(
            text = order.key.label,
            contentDescription = "Sorting by ${order.key.label}. Change what the list sorts by",
            onClick = onPickKey,
        )
    }
}

@Composable
private fun SortPillButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val tokens = LocalDashTokens.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(onClick = onClick),
    ) {
        Text(
            text = text,
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
 * Small sheet opened by the sort key pill: pick what the list sorts by. Direction
 * is chosen separately by the direction pill, so the key choice keeps whatever
 * direction is already set.
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
                            // Direction is owned by the direction pill, so keep it.
                            onClick = {
                                onOrderChange(SortOrder(key, reversed = order.reversed))
                                hideAndDismiss()
                            },
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
        }
    }
}
