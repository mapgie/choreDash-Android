package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.ui.components.core.CardIconChip
import com.mapgie.dash.ui.components.core.MetaCaption
import com.mapgie.dash.ui.components.core.MetaLabel
import com.mapgie.dash.ui.components.core.OwnerAvatar
import com.mapgie.dash.ui.components.core.StatusBadge
import com.mapgie.dash.ui.components.core.highlightedText
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.badgeContainerColor
import com.mapgie.dash.ui.theme.barColor
import com.mapgie.dash.ui.theme.isDarkScheme
import com.mapgie.dash.ui.theme.spineColor
import com.mapgie.dash.ui.theme.statusTone
import com.mapgie.dash.ui.theme.textColor
import com.mapgie.dash.ui.theme.tintColor
import com.mapgie.dash.util.formatAbsoluteDate
import com.mapgie.dash.util.relativeTime
import java.time.Instant

/**
 * The revised (turn 5a) list card for a chore: a 5dp status spine, a 38dp
 * circular chip carrying the category's Lucide [icon], the title with its
 * "every Nd · done Xw ago" meta line directly beneath, and a single right-hand
 * row of owner avatar then due badge, so dates line up for scanning. No progress
 * bar. Light cards keep a soft shadow; dark cards have none.
 *
 * Colour follows Settings › Colours' two axes: the spine and badge take
 * [spineSwatch] (the category colour, badge text neutral) or, when it is null,
 * the status tone; the icon chip does the same with [iconSwatch]. Either way
 * the badge words and the meta line restate the state, so colour is never the
 * only signal.
 *
 * A snoozed chore ([snoozedUntil] set) swaps the chip glyph for a muted bell on
 * a neutral tint and replaces the badge with "Snoozed until <date>"; the spine
 * keeps telling the truth about its status underneath.
 */
@Composable
fun ChoreCard(
    chore: Chore,
    showOwner: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    zenMode: Boolean = false,
    showCategory: Boolean = true,
    spineSwatch: Swatch? = null,
    iconSwatch: Swatch? = null,
    highlightQuery: String? = null,
    snoozedUntil: Instant? = null,
    inset: Dp = Dimens.cardInset,
) {
    val tone = chore.statusTone()
    val accents = LocalTypeAccents.current
    val dark = isDarkScheme()
    val snoozed = snoozedUntil != null

    val spine: Color = when {
        zenMode -> Color.Transparent
        spineSwatch != null -> spineSwatch.spineColor()
        else -> tone.barColor()
    }
    val chipContainer: Color = when {
        zenMode -> Color.Transparent
        snoozed -> MaterialTheme.colorScheme.surfaceContainerHigh
        iconSwatch != null -> iconSwatch.tintColor()
        else -> tone.badgeContainerColor() ?: accents.choreContainer
    }
    val chipContent: Color = when {
        zenMode || snoozed -> MaterialTheme.colorScheme.onSurfaceVariant
        iconSwatch != null -> iconSwatch.textColor()
        else -> tone.textColor()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = inset),
        colors = CardDefaults.cardColors(
            containerColor = if (zenMode) MaterialTheme.colorScheme.surfaceContainerLow
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (dark) 0.dp else 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(Dimens.accentBarWidth)
                    .fillMaxHeight()
                    .background(spine)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = Dimens.cardPadding,
                        end = Dimens.cardPadding,
                        top = Dimens.cardVerticalPadding,
                        bottom = Dimens.cardVerticalPadding,
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardIconChip(
                    icon = if (snoozed) LucideIcons.BellOff else icon,
                    containerColor = chipContainer,
                    contentColor = chipContent,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        highlightedText(chore.label, highlightQuery),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (zenMode) {
                        chore.lastScanned?.let {
                            MetaLabel(text = formatAbsoluteDate(it), style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        MetaCaption(text = choreCaption(chore, showCategory), uppercase = false)
                    }
                }

                // Right cluster: avatar first, then the due badge, so dates line up.
                if (!zenMode || (showOwner && chore.owner != null)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.metaSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showOwner && chore.owner != null) {
                            OwnerAvatar(handle = chore.owner)
                        }
                        if (!zenMode) {
                            if (snoozedUntil != null) {
                                MetaLabel(text = "Snoozed until ${formatAbsoluteDate(snoozedUntil)}")
                            } else {
                                StatusBadge(
                                    text = chore.dueBadgeText(),
                                    tone = tone,
                                    containerOverride = spineSwatch?.tintColor(),
                                    textOverride = if (spineSwatch != null)
                                        MaterialTheme.colorScheme.onSurfaceVariant else null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "kitchen · every 3d · done 5d ago" caption line; "never done" before the first log. */
private fun choreCaption(chore: Chore, showCategory: Boolean): String {
    val parts = mutableListOf<String>()
    if (showCategory && !chore.category.isNullOrBlank()) parts += chore.category.lowercase()
    chore.intervalDays?.let { parts += "every ${it.toInt()}d" }
    parts += chore.lastScanned?.let { "done ${relativeTime(it)}" } ?: "never done"
    return parts.joinToString(" · ")
}
