package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.ui.components.core.CardIconChip
import com.mapgie.dash.ui.components.core.MetaCaption
import com.mapgie.dash.ui.components.core.MetaLabel
import com.mapgie.dash.ui.components.core.OwnerAvatar
import com.mapgie.dash.ui.components.core.StatusBadge
import com.mapgie.dash.ui.components.core.highlightedText
import com.mapgie.dash.ui.theme.DashIcons
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.StatusTone
import com.mapgie.dash.ui.theme.badgeContainerColor
import com.mapgie.dash.ui.theme.barColor
import com.mapgie.dash.ui.theme.statusTone
import com.mapgie.dash.ui.theme.textColor
import com.mapgie.dash.util.formatAbsoluteDate
import com.mapgie.dash.util.relativeTime

/**
 * Cozy Cream list card for a chore: status spine, circular brush chip tinted by
 * the chore's status, title with an uppercase "every Nd · done Xd ago" caption,
 * a right column with the countdown badge above the owner avatar, and a slim
 * cadence-pressure bar along the bottom (track fills as the chore approaches
 * due; full = overdue). The bar restates the spine/badge status, so colour is
 * never the only signal.
 */
@Composable
fun ChoreCard(
    chore: Chore,
    showOwner: Boolean,
    zenMode: Boolean = false,
    showDueCountdown: Boolean = false,
    showCategory: Boolean = true,
    modifier: Modifier = Modifier,
    highlightQuery: String? = null
) {
    val tone = chore.statusTone()
    val accents = LocalTypeAccents.current
    val barColor = if (zenMode) Color.Transparent else tone.barColor()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            // Same card ground as the Tasks list; status is carried by the spine,
            // the chip tint, the badge, and the pressure bar.
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
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CardIconChip(
                        icon = DashIcons.Brush,
                        containerColor = if (zenMode) Color.Transparent
                                         else tone.badgeContainerColor() ?: accents.choreContainer,
                        contentColor = if (zenMode) MaterialTheme.colorScheme.onSurfaceVariant
                                       else tone.textColor(),
                    )

                    // Title + cadence caption
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            highlightedText(chore.label, highlightQuery),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (zenMode) {
                            chore.lastScanned?.let {
                                MetaLabel(text = formatAbsoluteDate(it), style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            MetaCaption(text = choreCaption(chore, showCategory))
                        }
                    }

                    // Right column: status badge above the owner avatar, pinned
                    // rightmost so the same person renders identically here and
                    // on the Tasks list.
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!zenMode) {
                            when {
                                chore.lastScanned == null ->
                                    StatusBadge(text = "Never", tone = tone)
                                showDueCountdown && chore.nextDueText() != null ->
                                    StatusBadge(text = chore.nextDueText()!!, tone = tone)
                                else ->
                                    MetaLabel(
                                        text = formatAbsoluteDate(chore.lastScanned),
                                        color = if (tone == StatusTone.OK)
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else tone.textColor()
                                    )
                            }
                        }
                        if (showOwner && chore.owner != null) {
                            OwnerAvatar(handle = chore.owner, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }

                // Cadence-pressure bar: fills as the chore approaches due.
                val pressure = if (zenMode) null else chore.pressureFraction()
                if (pressure != null) {
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp, end = 12.dp, bottom = 10.dp)
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pressure)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(percent = 50))
                                .background(tone.barColor())
                        )
                    }
                }
            }
        }
    }
}

/** "kitchen · every 3d · done 5d ago" caption line; "never done" before the first log. */
private fun choreCaption(chore: Chore, showCategory: Boolean): String {
    val parts = mutableListOf<String>()
    if (showCategory && !chore.category.isNullOrBlank()) parts += chore.category
    chore.intervalDays?.let { parts += "every ${it.toInt()}d" }
    parts += chore.lastScanned?.let { "done ${relativeTime(it)}" } ?: "never done"
    return parts.joinToString(" · ")
}
