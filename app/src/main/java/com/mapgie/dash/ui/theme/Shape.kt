package com.mapgie.dash.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Cozy Cream shape scale, per the revised (turn 5a) list card: badges/chips 8,
 * small surfaces 12, cards 16 (medium also covers the swipe-action backgrounds
 * behind cards, which must match the card radius; large covers grouped sheet
 * blocks), and bottom sheets / dialogs 26.
 *
 * Filter chips are pills (a full stadium); that is set per call site rather than
 * here because `small` is also used by multi-line surfaces.
 */
val DashShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/** Full-stadium pill shape for filter chips and other pill controls. */
val PillShape = RoundedCornerShape(percent = 50)

/** Status badge corner radius (7dp per the handoff). */
val BadgeShape = RoundedCornerShape(7.dp)
