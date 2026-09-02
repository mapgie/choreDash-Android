package com.mapgie.dash.ui.components.core

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.mapgie.dash.data.model.OwnerFilter
import com.mapgie.dash.ui.theme.LucideIcons

/**
 * Filled person = just me; outlined person = me and unassigned; two people =
 * everyone. Shape changes with state so colour is never the only signal.
 */
val OwnerFilter.icon: ImageVector
    get() = when (this) {
        OwnerFilter.MINE -> LucideIcons.UserFilled
        OwnerFilter.MINE_AND_UNASSIGNED -> LucideIcons.User
        OwnerFilter.EVERYONE -> LucideIcons.Users
    }

/**
 * Header icon button that cycles the owner scope. The icon shows the current
 * state; the accessible description names it and the state a tap moves to.
 * Narrowed states (mine, mine and unassigned) take the tab's accent tint, the
 * unfiltered state stays muted, matching the other header toggles.
 */
@Composable
fun OwnerFilterButton(
    filter: OwnerFilter,
    onFilterChange: (OwnerFilter) -> Unit,
    modifier: Modifier = Modifier,
    activeTint: Color = MaterialTheme.colorScheme.primary,
) {
    HeaderIconButton(
        icon = filter.icon,
        contentDescription = "Owner filter. Tap to show: ${filter.next.label.lowercase()}",
        onClick = { onFilterChange(filter.next) },
        active = filter != OwnerFilter.EVERYONE,
        activeTint = activeTint,
        modifier = modifier.semantics { stateDescription = "Showing: ${filter.label.lowercase()}" },
    )
}
