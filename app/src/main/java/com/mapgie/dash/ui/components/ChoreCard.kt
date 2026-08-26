package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.ui.components.core.CategoryBadge
import com.mapgie.dash.ui.components.core.MetaLabel
import com.mapgie.dash.ui.components.core.OwnerAvatar
import com.mapgie.dash.ui.components.core.StatusBadge
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.barColor
import com.mapgie.dash.ui.theme.statusTone
import com.mapgie.dash.ui.theme.textColor
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
    val tone = chore.statusTone()
    val barColor = if (zenMode) Color.Transparent else tone.barColor()
    val dateColor = when (chore.status) {
        ChoreStatus.STALE, ChoreStatus.NEVER, ChoreStatus.AGING -> tone.textColor()
        ChoreStatus.FRESH -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            // Same card ground as the Tasks list; status is carried by the spine,
            // the badge, and the date text rather than a whole-card wash.
            containerColor = if (zenMode) MaterialTheme.colorScheme.surface
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left status accent bar
            Box(
                modifier = Modifier
                    .width(Dimens.accentBarWidth)
                    .fillMaxHeight()
                    .background(barColor)
            )
            // Single content row, title and dates side-by-side, vertically centred
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: title (and optional category pill beneath)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        chore.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (showCategory && !zenMode && chore.category != null) {
                        CategoryBadge(chore.category)
                    }
                }

                // Middle: dates column
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        chore.lastScanned == null -> {
                            MetaLabel(
                                text = "Never",
                                style = MaterialTheme.typography.bodySmall,
                                italic = true
                            )
                        }
                        zenMode -> {
                            MetaLabel(
                                text = formatAbsoluteDate(chore.lastScanned),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        else -> {
                            if (showDueCountdown) {
                                val dueText = chore.nextDueText()
                                if (dueText != null) {
                                    StatusBadge(text = dueText, tone = tone)
                                    MetaLabel(text = relativeTime(chore.lastScanned))
                                } else {
                                    MetaLabel(
                                        text = relativeTime(chore.lastScanned),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = dateColor
                                    )
                                }
                            } else {
                                MetaLabel(
                                    text = formatAbsoluteDate(chore.lastScanned),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = dateColor
                                )
                                MetaLabel(text = relativeTime(chore.lastScanned))
                            }
                        }
                    }
                }

                // Right: owner avatar, pinned rightmost so the same person renders
                // identically here and on the Tasks list.
                if (showOwner && chore.owner != null) {
                    OwnerAvatar(handle = chore.owner)
                }
            }
        }
    }
}
