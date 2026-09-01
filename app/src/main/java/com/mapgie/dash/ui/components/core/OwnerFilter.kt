package com.mapgie.dash.ui.components.core

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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Who a list is scoped to. Shared by the Chores and Tasks screens so the same
 * header control cycles through the same three states everywhere.
 *
 * Cycle order (one tap each): [MINE] → [MINE_AND_UNASSIGNED] → [EVERYONE] → [MINE].
 */
enum class OwnerFilter(
    /** Short label for the state, used in the control's accessibility state. */
    val label: String,
) {
    /** Only items assigned to me. */
    MINE("Mine"),

    /** Items assigned to me, plus items nobody has claimed. */
    MINE_AND_UNASSIGNED("Mine and unassigned"),

    /** Everything, whoever it belongs to. */
    EVERYONE("Everyone");

    val next: OwnerFilter
        get() = entries[(ordinal + 1) % entries.size]

    /**
     * Whether an item with [owner] passes this filter for the current user's
     * [ownerHandle]. With a blank handle there is no "me", so only [EVERYONE]
     * can match anything and the other two fall back to showing everything.
     */
    fun matches(owner: String?, ownerHandle: String): Boolean {
        if (ownerHandle.isBlank()) return true
        return when (this) {
            MINE -> owner == ownerHandle
            MINE_AND_UNASSIGNED -> owner == null || owner == ownerHandle
            EVERYONE -> true
        }
    }

    /** The owner avatar only adds information when other people's items are listed. */
    val showsOwner: Boolean
        get() = this == EVERYONE

    /**
     * Filled person = just me; outlined person = me and unassigned; two people =
     * everyone. Shape changes with state so colour is never the only signal.
     */
    val icon: ImageVector
        get() = when (this) {
            MINE -> Icons.Filled.Person
            MINE_AND_UNASSIGNED -> Icons.Outlined.Person
            EVERYONE -> Icons.Outlined.People
        }
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
