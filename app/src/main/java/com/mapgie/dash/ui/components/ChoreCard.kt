package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.ui.theme.StatusAging
import com.mapgie.dash.ui.theme.StatusFresh
import com.mapgie.dash.ui.theme.StatusStale
import com.mapgie.dash.util.formatAbsoluteDate
import com.mapgie.dash.util.relativeTime

@Composable
fun ChoreCard(
    chore: Chore,
    showOwner: Boolean,
    zenMode: Boolean = false,
    showDueCountdown: Boolean = false,
    showCategory: Boolean = true,
    modifier: Modifier = Modifier
) {
    val statusColor = when (chore.status) {
        ChoreStatus.STALE, ChoreStatus.NEVER -> StatusStale
        ChoreStatus.AGING -> StatusAging
        ChoreStatus.FRESH -> StatusFresh
    }
    val barColor = if (zenMode) Color.Transparent else statusColor
    val dateColor = when (chore.status) {
        ChoreStatus.STALE, ChoreStatus.NEVER -> StatusStale
        ChoreStatus.AGING -> StatusAging
        ChoreStatus.FRESH -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (zenMode) MaterialTheme.colorScheme.surface
                             else lerp(MaterialTheme.colorScheme.surface, statusColor, 0.07f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left status accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(barColor)
            )
            // Single content row, title and dates side-by-side, vertically centred
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: title (and optional category label beneath)
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        chore.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (showCategory && !zenMode && chore.category != null) {
                        Text(
                            chore.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Owner badge (if shown) — placed between title and dates
                if (showOwner && chore.owner != null) {
                    OwnerBadge(initial = chore.owner.firstOrNull()?.uppercaseChar() ?: '?')
                    Spacer(Modifier.width(8.dp))
                }

                // Right: dates column
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        chore.lastScanned == null -> {
                            Text(
                                "Never",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        zenMode -> {
                            Text(
                                formatAbsoluteDate(chore.lastScanned),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            val dueText = chore.nextDueText()
                            if (dueText != null) {
                                Text(
                                    dueText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = dateColor
                                )
                                Text(
                                    relativeTime(chore.lastScanned),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    relativeTime(chore.lastScanned),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = dateColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerBadge(initial: Char) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
