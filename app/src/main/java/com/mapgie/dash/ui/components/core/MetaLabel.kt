package com.mapgie.dash.ui.components.core

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle

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
