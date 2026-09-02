package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.BadgeShape
import com.mapgie.dash.ui.theme.StatusTone
import com.mapgie.dash.ui.theme.badgeContainerColor
import com.mapgie.dash.ui.theme.textColor

/**
 * The one status badge, shared by the Chore and Task cards: a small tinted pill
 * ("35d over", "1d left", "due today") whose tint and text colour come from the
 * shared [StatusTone] scale. Tones that don't signal ([StatusTone.NEUTRAL] and
 * [StatusTone.NONE]) render as plain muted text without a pill, so quiet states
 * stay quiet.
 *
 * Colour is a secondary cue: the badge text itself names the state, satisfying the
 * "not colour alone" rule. [containerOverride] / [textOverride] let a card in
 * category-colour mode keep the pill while neutralising its text.
 */
@Composable
fun StatusBadge(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    containerOverride: Color? = null,
    textOverride: Color? = null,
) {
    val container = containerOverride ?: tone.badgeContainerColor()
    val textColor = textOverride ?: tone.textColor()
    if (container == null) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier
                .clip(BadgeShape)
                .background(container)
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold),
                color = textColor,
                maxLines = 1,
            )
        }
    }
}
