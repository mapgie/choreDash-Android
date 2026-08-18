package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * The one category pill, shared by the Chore and Task cards.
 *
 * Previously this composable was duplicated byte-for-byte in `ChoreCard.kt` and
 * `TaskCard.kt`; two copies is exactly how the screens drifted apart. It now lives
 * here so a change lands in one place.
 */
@Composable
fun CategoryBadge(category: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = category.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
