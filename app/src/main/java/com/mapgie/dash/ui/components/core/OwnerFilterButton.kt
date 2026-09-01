package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.OwnerFilter

/**
 * Filled person = just me; outlined person = me and unassigned; two people =
 * everyone. Shape changes with state so colour is never the only signal.
 */
val OwnerFilter.icon: ImageVector
    get() = when (this) {
        OwnerFilter.MINE -> Icons.Filled.Person
        OwnerFilter.MINE_AND_UNASSIGNED -> Icons.Outlined.Person
        OwnerFilter.EVERYONE -> Icons.Outlined.People
    }

/**
 * Header icon button that cycles the owner scope. The icon shows the current
 * state; the accessible description names it and the state a tap moves to.
 * Narrowed states (mine, mine and unassigned) take the primary tint, the
 * unfiltered state stays neutral, matching the other header toggles.
 */
@Composable
fun OwnerFilterButton(
    filter: OwnerFilter,
    onFilterChange: (OwnerFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onFilterChange(filter.next) },
        modifier = modifier
            .size(48.dp)
            .semantics { stateDescription = "Showing: ${filter.label.lowercase()}" }
    ) {
        Icon(
            imageVector = filter.icon,
            contentDescription = "Owner filter. Tap to show: ${filter.next.label.lowercase()}",
            tint = if (filter == OwnerFilter.EVERYONE)
                MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary
        )
    }
}
