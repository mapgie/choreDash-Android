package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * One control in the page-header icon row (search, owner filter, zen,
 * group/flat): a 20dp Lucide glyph inside a 44dp [IconButton], muted ink when
 * idle and the tab's accent ([activeTint]) when [active]. Every icon in the row
 * goes through here so the row stays uniform, and the row is identical on Chores
 * and Tasks so the thumb never relearns it.
 */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    activeTint: Color = MaterialTheme.colorScheme.primary,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) activeTint else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
