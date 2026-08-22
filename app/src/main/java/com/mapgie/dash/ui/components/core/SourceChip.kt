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
import com.mapgie.dash.ui.theme.LocalTypeAccents

/** Where a Memo's alarm came from. Drives the [SourceChip] colour. */
enum class SourceKind { CHORE, TASK }

/**
 * A small pill on a Memo row naming where the alarm came from, tinted from
 * `LocalTypeAccents` in the same vocabulary as the nav bar: green for a
 * chore-sourced alarm, lavender for a task-sourced one. The [label] (e.g.
 * "Chore: Air Plant") carries the meaning on its own, so the colour is a
 * secondary cue only.
 */
@Composable
fun SourceChip(kind: SourceKind, label: String, modifier: Modifier = Modifier) {
    val accents = LocalTypeAccents.current
    val container = when (kind) {
        SourceKind.CHORE -> accents.choreContainer
        SourceKind.TASK -> accents.taskContainer
    }
    val onContainer = when (kind) {
        SourceKind.CHORE -> accents.onChoreContainer
        SourceKind.TASK -> accents.onTaskContainer
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(container)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = onContainer
        )
    }
}
