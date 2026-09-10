package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.StatusTone
import com.mapgie.dash.ui.theme.badgeContainerColor
import com.mapgie.dash.ui.theme.textColor

/**
 * A tappable strip above a list warning that a system permission the screen's
 * feature depends on is missing: alert glyph, one line of [text] naming the gap
 * and its effect, and a chevron. Wears the shared ATTENTION (amber) tone, so it
 * matches the "due soon" badges rather than the error red reserved for failures.
 * The glyph and the wording carry the state; the tint is secondary.
 *
 * Tapping goes to wherever the gap can be fixed (the caller decides). Announced
 * politely when it appears, since it usually shows on return from system settings.
 */
@Composable
fun PermissionBanner(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = StatusTone.ATTENTION.badgeContainerColor() ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val content = StatusTone.ATTENTION.textColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.cardInset)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .semantics {
                role = Role.Button
                liveRegion = LiveRegionMode.Polite
            }
            .clickable(onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = LucideIcons.CircleAlert,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = content,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = LucideIcons.ChevronRight,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp),
        )
    }
}
