package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.ui.unit.dp
import com.mapgie.dash.ui.theme.Dimens

/**
 * Decorative circular icon chip at the left of a list card ([Dimens.iconChipSize],
 * tinted by the card's accent). Purely visual: the icon restates the card's
 * content type or status, which the card also carries in text.
 */
@Composable
fun CardIconChip(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Dimens.iconChipSize)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * The card icon chip doubling as the done toggle (Tasks, Memos): an open circle
 * while active, a filled check once done, on the accent-tinted circle. Replaces
 * the old square Checkbox while keeping the same semantics: role [Role.Checkbox]
 * plus toggleable state, and at [Dimens.iconChipSize] it clears the 44dp minimum
 * tap target.
 */
@Composable
fun DoneToggleChip(
    isDone: Boolean,
    onToggle: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Dimens.iconChipSize)
            .clip(CircleShape)
            .background(containerColor)
            .semantics {
                role = Role.Checkbox
                contentDescription = if (isDone) "Mark not done" else "Mark done"
            }
            .toggleable(value = isDone, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isDone) Icons.Filled.Check else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
    }
}
