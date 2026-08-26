package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapgie.dash.ui.theme.StatusTone
import com.mapgie.dash.ui.theme.badgeContainerColor
import com.mapgie.dash.ui.theme.textColor

/**
 * The one status badge, shared by the Chore and Task cards: a small tinted pill
 * ("Overdue", "Today", "2d left") whose tint and text colour come from the shared
 * [StatusTone] scale. Tones that don't signal ([StatusTone.NEUTRAL] and
 * [StatusTone.NONE]) render as plain muted text without a pill, so quiet states
 * stay quiet.
 *
 * Colour is a secondary cue: the badge text itself names the state, satisfying the
 * "not colour alone" rule.
 */
@Composable
fun StatusBadge(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val container = tone.badgeContainerColor()
    if (container == null) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tone.textColor(),
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(container)
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = tone.textColor(),
            )
        }
    }
}
