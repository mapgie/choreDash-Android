package com.mapgie.dash.ui.components.core

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Shared secondary text: the small, muted labels a card shows beneath or beside its
 * title (relative time, "Never", plain due dates). Centralising it keeps the meta
 * lines on every card consistent in size and colour.
 *
 * Defaults to `labelSmall` in `onSurfaceVariant`; callers override [style] or
 * [color] for the few status-coloured lines.
 */
@Composable
fun MetaLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    italic: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontStyle = if (italic) FontStyle.Italic else null,
    )
}

/**
 * The uppercase caption line under a card title ("GENERAL · NORMAL",
 * "EVERY 3D · DONE 5D AGO"): small, bold, deliberately faint. One style so the
 * meta line reads the same on every card.
 */
@Composable
fun MetaCaption(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.outline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
