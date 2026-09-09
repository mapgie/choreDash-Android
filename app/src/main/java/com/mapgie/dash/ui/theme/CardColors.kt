package com.mapgie.dash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Fill for a muted list card (a done task, a done or archived memo): 60% of
 * `surfaceVariant` flattened onto the page background so the card reads as faded
 * next to its active siblings.
 *
 * The blend is composited into an opaque colour on purpose. A translucent
 * `containerColor` on an elevated `Card` lets the platform shadow show through
 * the fill, and because the shadow is drawn as a ring with an unfilled umbra
 * the card picks up a thick grey border around a paler inner rectangle. See
 * `LESSONS.md` #56.
 */
@Composable
fun mutedCardContainer(): Color =
    MaterialTheme.colorScheme.surfaceVariant
        .copy(alpha = 0.6f)
        .compositeOver(MaterialTheme.colorScheme.background)
