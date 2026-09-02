package com.mapgie.dash.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared spacing and sizing tokens for the list surfaces (Chores, Tasks, Memos),
 * per the revised (turn 5a) list card: tighter rows, a 5dp spine, a 38dp icon
 * chip and a 24dp avatar in a single right-hand row.
 *
 * These are the Compose analogue of design tokens / SCSS variables: one place that
 * owns the card inset, inter-card gap, accent-bar width, owner-avatar size, and the
 * minimum row height. Screens and the shared card shell read from here instead of
 * hardcoding `.dp` per file, which is how the two list screens drifted apart in the
 * first place.
 */
object Dimens {
    /** Horizontal inset around a list card. */
    val cardInset = 18.dp

    /** Vertical gap between consecutive list cards. */
    val cardGap = 7.dp

    /** Inner content padding inside a card, between the accent bar and the edge. */
    val cardPadding = 14.dp

    /** Vertical padding inside a card. */
    val cardVerticalPadding = 10.dp

    /** Width of the leading status accent bar (the card "spine"). */
    val accentBarWidth = 5.dp

    /** Diameter of the owner avatar on list cards. */
    val avatarSize = 24.dp

    /** Diameter of the owner avatar in sheet headers. */
    val sheetAvatarSize = 30.dp

    /**
     * Diameter of the circular icon chip at the left of every list card. Where the
     * chip doubles as the done toggle it is wrapped in a 44dp minimum touch target
     * (`minimumInteractiveComponentSize`), so the visual stays at 38dp without
     * shrinking the tap area below the `CLAUDE.md` floor.
     */
    val iconChipSize = 38.dp

    /** Glyph size inside [iconChipSize]. */
    val iconChipGlyph = 18.dp

    /** Icon chip in a sheet header. */
    val sheetIconChipSize = 44.dp

    /**
     * Minimum height of a tappable list row. 56dp comfortably clears the 44dp
     * minimum tap target required by `CLAUDE.md`.
     */
    val minRowHeight = 56.dp

    /** Gap between inline meta elements (badges, labels) in a row. */
    val metaSpacing = 8.dp

    /** Horizontal page padding for the header and sheet content. */
    val pagePadding = 20.dp
}
