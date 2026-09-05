package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.PillShape
import com.mapgie.dash.ui.theme.isDarkScheme

/**
 * The zen list row (handoff 3a-4): a soft 18dp card with an open 24dp circle,
 * the title and a gentle sub-line ("kitchen · when you're up"). No pressure
 * colours, no counts. A [done] row shows the circle filled with a check, the
 * title struck through, and the whole card faded to 0.62.
 *
 * The circle is a 44dp checkbox target that completes the item; the rest of the
 * card is the caller's tap target (open the sheet) via [modifier].
 */
@Composable
fun ZenRow(
    title: String,
    sub: String,
    done: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDashTokens.current
    val ring = tokens.sectionCount
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (isDarkScheme()) 0.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (done) 0.62f else 1f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 8.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .semantics {
                        role = Role.Checkbox
                        contentDescription = if (done) "Undo $title" else "Done: $title"
                        stateDescription = if (done) "Done" else "Not done"
                    }
                    .toggleable(value = done, onValueChange = { onToggle() }),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (done) ring else Color.Transparent)
                        .border(1.7.dp, ring, CircleShape),
                ) {
                    if (done) {
                        Icon(
                            imageVector = LucideIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = if (done) tokens.inkFaint else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    color = if (done) tokens.sectionCount else tokens.inkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The zen header's "mine | all" scope pill. Two radio cells; the selected one
 * sits on a soft tint so the choice is a shape as well as a shade.
 */
@Composable
fun ZenScopeToggle(
    mine: Boolean,
    onMineChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(PillShape),
    ) {
        ZenScopeCell(label = "mine", selected = mine, onClick = { onMineChange(true) })
        ZenScopeCell(label = "all", selected = !mine, onClick = { onMineChange(false) })
    }
}

@Composable
private fun ZenScopeCell(label: String, selected: Boolean, onClick: () -> Unit) {
    val tokens = LocalDashTokens.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .semantics {
                role = Role.RadioButton
                contentDescription = if (label == "mine") "Only my items" else "Everyone's items"
            }
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            ),
            color = if (selected) MaterialTheme.colorScheme.onSurfaceVariant else tokens.inkFaint,
            modifier = Modifier
                .clip(PillShape)
                .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

