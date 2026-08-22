package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.StatusTone
import com.mapgie.dash.ui.theme.barColor

/**
 * The one list-card shell, shared by the Chore, Task and Memo cards. Cards become
 * thin bindings that fill the slots; the shell owns everything the three screens
 * used to hand-roll separately:
 *
 * - solid, **always opaque** `surfaceVariant` container. A dimmed row blends toward
 *   the surface ([lerp] to `surface`), never drops the container's alpha, so nothing
 *   behind the card (e.g. a swipe panel) can read through it.
 * - one leading status [accent bar][StatusTone], transparent in zen mode.
 * - vertical centring and a [minimum height][Dimens.minRowHeight] that guarantees the
 *   44dp tap target from `CLAUDE.md`.
 * - one horizontal inset from [Dimens], and the `role`/`combinedClickable` plumbing,
 *   applied here once so accessibility is correct everywhere and only when the card is
 *   actually interactive.
 *
 * @param owner rendered rightmost via [OwnerAvatar] when non-null, after [trailing].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashListCard(
    tone: StatusTone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    dimmed: Boolean = false,
    zenMode: Boolean = false,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable ColumnScope.() -> Unit)? = null,
    owner: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerColor = when {
        zenMode -> MaterialTheme.colorScheme.surface
        dimmed -> lerp(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface,
            0.5f
        )
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val barColor = if (zenMode) Color.Transparent else tone.barColor()

    // Only announce (and handle) a click when the card is genuinely interactive.
    // The role sits immediately before combinedClickable so the a11y check finds it.
    val interaction = if (onClick != null || onLongClick != null) {
        Modifier
            .semantics { role = Role.Button }
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick?.let { handler -> { handler() } }
            )
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.cardInset)
            .then(interaction),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .heightIn(min = Dimens.minRowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading status accent bar.
            Box(
                modifier = Modifier
                    .width(Dimens.accentBarWidth)
                    .fillMaxHeight()
                    .background(barColor)
            )
            // Optional leading slot (e.g. a done checkbox).
            leading?.let { slot ->
                Row(verticalAlignment = Alignment.CenterVertically, content = slot)
            }
            // Title + meta slot.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Dimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
            // Optional trailing slot (e.g. due / last-scanned dates), end-aligned.
            trailing?.let { slot ->
                Column(
                    modifier = Modifier.padding(end = Dimens.cardPadding),
                    horizontalAlignment = Alignment.End,
                    content = slot
                )
            }
            // Owner avatar, pinned rightmost so the same person renders identically
            // on every screen.
            owner?.let { handle ->
                OwnerAvatar(
                    handle = handle,
                    modifier = Modifier.padding(end = Dimens.cardPadding)
                )
            }
        }
    }
}
