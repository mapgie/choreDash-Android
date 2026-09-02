package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.PillShape

/**
 * The hairline-topped strip between a list and the bottom bar: "7 chores · 1
 * hidden" on the left and a "Done ›" link on the right. Same component on
 * Chores and Tasks; the caller supplies the words.
 */
@Composable
fun SummaryBar(
    summary: String,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    val tokens = LocalDashTokens.current
    val style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(start = 20.dp, end = 12.dp),
        ) {
            Text(
                text = summary,
                style = style,
                color = tokens.inkFaint,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (trailingLabel != null && onTrailingClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(PillShape)
                        .semantics { role = Role.Button }
                        .clickable(onClick = onTrailingClick)
                        .padding(horizontal = 8.dp),
                ) {
                    Text(text = trailingLabel, style = style, color = tokens.inkFaint)
                    Icon(
                        imageVector = LucideIcons.ChevronRight,
                        contentDescription = null,
                        tint = tokens.inkFaint,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}
