package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.isDarkScheme
import com.mapgie.dash.ui.theme.ownerColorFor

/**
 * The one owner indicator, shared by every list card and overview sheet.
 *
 * Same per-person hue everywhere, derived from [ownerColorFor] in the light or
 * dark set for the current brightness. Callers pass only the handle (and, for
 * sheet headers, a larger [size]): no caller decides colour or position again.
 *
 * The initial is always drawn, so colour is decoration only. It is non-interactive,
 * so it carries no `Role`; [clearAndSetSemantics] replaces the lone initial with a
 * spoken "Owner: <handle>" label rather than announcing a bare letter.
 */
@Composable
fun OwnerAvatar(
    handle: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.avatarSize,
) {
    val tone = ownerColorFor(handle, dark = isDarkScheme())
    val initial = handle.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tone.container)
            .clearAndSetSemantics { contentDescription = "Owner: $handle" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (size >= Dimens.sheetAvatarSize) 12.sp else 11.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            color = tone.onContainer
        )
    }
}
