package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LucideIcons

/**
 * Decorative circular icon chip at the left of a list card ([Dimens.iconChipSize],
 * tinted by the card's accent, one Lucide glyph per category). Purely visual: the
 * icon restates the card's category or status, which the card also carries in text.
 */
@Composable
fun CardIconChip(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.iconChipSize,
    glyphSize: Dp = Dimens.iconChipGlyph,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(glyphSize),
        )
    }
}

/**
 * The card icon chip doubling as the done toggle (Tasks, Memos): the category
 * glyph while active, a check once done, on the accent-tinted circle. Keeps the
 * checkbox semantics (role [Role.Checkbox] plus toggleable state) and wraps the
 * 38dp visual in a 44dp touch target so it still clears the `CLAUDE.md` minimum.
 */
@Composable
fun DoneToggleChip(
    isDone: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .semantics {
                role = Role.Checkbox
                contentDescription = if (isDone) "Mark not done" else "Mark done"
            }
            .toggleable(value = isDone, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        CardIconChip(
            icon = if (isDone) LucideIcons.Check else icon,
            containerColor = containerColor,
            contentColor = contentColor,
        )
    }
}
