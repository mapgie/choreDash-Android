package com.mapgie.dash.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.PillShape

/** One tab in the Cozy Cream bottom bar. */
data class DashTab(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val activeContainer: Color,
    val activeContent: Color,
    val onClick: () -> Unit,
)

/**
 * The Cozy Cream bottom utility bar (handoff §Bottom navigation): a flat strip
 * on the surface tone with a 1dp hairline top border, tabs bottom-aligned in a
 * row split around a raised centre slot holding the add button. The active
 * tab's icon sits in a pill-tint chip and its label takes the tab's accent;
 * inactive tabs are faint.
 *
 * The first two tabs go left of the centre slot and the rest right, matching
 * the handoff's Tasks · Chores · [+] · Memos · Settings order (the Memos tab
 * may be absent, leaving the bar 2 + 1).
 */
@Composable
fun DashBottomBar(
    tabs: List<DashTab>,
    centerContent: @Composable () -> Unit,
) {
    // A plain Column with a background (not Surface) so the centre button's
    // shadow is never clipped.
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 10.dp)
        ) {
            val left = tabs.take(2)
            val right = tabs.drop(2)
            left.forEach { BarTab(it, Modifier.weight(1f)) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 6.dp)
            ) {
                centerContent()
            }
            right.forEach { BarTab(it, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun BarTab(tab: DashTab, modifier: Modifier = Modifier) {
    val inactive = MaterialTheme.colorScheme.outline
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .semantics { role = Role.Tab }
            .selectable(selected = tab.selected, onClick = tab.onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(if (tab.selected) tab.activeContainer else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = if (tab.selected) tab.activeContent else inactive,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = if (tab.selected) tab.activeContent else inactive,
            maxLines = 1,
        )
    }
}
