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
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.LocalDashTokens

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
 * The caption line directly under a card title: small, bold, in the faint ink.
 * Tasks use the uppercase, letterspaced form ("CAR · HIGH"); chores use the plain
 * form ("every 30d · done 7w ago"). One composable so both read as the same line.
 */
@Composable
fun MetaCaption(
    text: String,
    modifier: Modifier = Modifier,
    uppercase: Boolean = true,
) {
    Text(
        text = if (uppercase) text.uppercase() else text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = if (uppercase) 11.5.sp else 12.sp,
            letterSpacing = if (uppercase) 0.9.sp else 0.sp,
        ),
        color = LocalDashTokens.current.inkFaint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
