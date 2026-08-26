package com.mapgie.dash.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared spacing and sizing tokens for the list surfaces (Chores, Tasks, Memos).
 *
 * These are the Compose analogue of design tokens / SCSS variables: one place that
 * owns the card inset, inter-card gap, accent-bar width, owner-avatar size, and the
 * minimum row height. Screens and the shared card shell read from here instead of
 * hardcoding `.dp` per file, which is how the two list screens drifted apart in the
 * first place.
 */
object Dimens {
    /** Horizontal inset around a list card. */
    val cardInset = 16.dp

    /** Vertical gap between consecutive list cards. */
    val cardGap = 8.dp

    /** Inner content padding inside a card, between the accent bar and the edge. */
    val cardPadding = 12.dp

    /** Width of the leading status accent bar (the card "spine"). */
    val accentBarWidth = 7.dp

    /** Diameter of the owner avatar on every surface. */
    val avatarSize = 26.dp

    /**
     * Minimum height of a tappable list row. 56dp comfortably clears the 44dp
     * minimum tap target required by `CLAUDE.md`.
     */
    val minRowHeight = 56.dp

    /** Gap between inline meta elements (badges, labels) in a row. */
    val metaSpacing = 8.dp
}
