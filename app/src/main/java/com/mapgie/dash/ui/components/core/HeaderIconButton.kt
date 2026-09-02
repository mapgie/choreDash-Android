package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * One control in the page-header icon row (owner filter, zen, search,
 * group/flat): a 20dp glyph inside a 44dp [IconButton], faint outline tint when
 * idle and the primary tint when [active]. Every icon in the row goes through
 * here so the row stays uniform.
 */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}
