package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.ui.theme.StatusAging
import com.mapgie.dash.ui.theme.StatusFresh
import com.mapgie.dash.ui.theme.StatusStale
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun ChoreCard(
    chore: Chore,
    showOwner: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = when (chore.status) {
        ChoreStatus.STALE, ChoreStatus.NEVER -> StatusStale
        ChoreStatus.AGING -> StatusAging
        ChoreStatus.FRESH -> StatusFresh
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (chore.category != null) {
                        Text(
                            chore.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        lastScannedText(chore.lastScanned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

private fun lastScannedText(lastScanned: Instant?): String {
    if (lastScanned == null) return "Never"
    val days = ChronoUnit.DAYS.between(lastScanned, Instant.now())
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        else -> "${days}d ago"
    }
}
