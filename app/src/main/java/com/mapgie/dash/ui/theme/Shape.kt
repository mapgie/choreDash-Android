package com.mapgie.dash.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Cozy Cream shape scale: noticeably rounder than the M3 defaults so cards and
 * sheets read soft. Mapping to design tokens: badges/chips 8, small surfaces 12,
 * cards 20 (medium also covers the swipe-action backgrounds behind cards, which
 * must match the card radius), FABs 20, bottom sheets and dialogs 28.
 *
 * Filter chips are pills (a full stadium); that is set per call site rather than
 * here because `small` is also used by multi-line surfaces.
 */
val DashShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Full-stadium pill shape for filter chips and other pill controls. */
val PillShape = RoundedCornerShape(percent = 50)
