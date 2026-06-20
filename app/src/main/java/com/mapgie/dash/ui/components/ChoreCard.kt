package com.mapgie.dash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left status bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(barColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        chore.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (showOwner && chore.owner != null) {
                        OwnerBadge(initial = chore.owner.firstOrNull()?.uppercaseChar() ?: '?')
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!zenMode && chore.category != null) {
                        Text(
                            chore.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(Modifier)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (chore.lastScanned == null) {
                            Text(
                                "Never",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                formatAbsoluteDate(chore.lastScanned),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (zenMode) MaterialTheme.colorScheme.onSurfaceVariant else dateColor
                            )
                            if (showDueCountdown && !zenMode) {
                                chore.nextDueText()?.let { dueText ->
                                    Text(
                                        dueText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = dateColor
                                    )
                                }
                            }
                            if (!zenMode) {
                                Text(
                                    relativeTime(chore.lastScanned),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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