package com.mapgie.dash.data.model

/**
 * Who a list is scoped to. Shared by the Chores and Tasks screens so the same
 * header control cycles through the same three states everywhere.
 *
 * Cycle order (one tap each): [MINE] → [MINE_AND_UNASSIGNED] → [EVERYONE] → [MINE].
 *
 * Plain Kotlin, no Compose: the icon for each state lives with the button in
 * `ui/components/core/OwnerFilterButton.kt`, so this logic is unit-testable.
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
     * [ownerHandle]. With a blank handle there is no "me", so every state shows
     * everything rather than hiding the whole list.
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
}
