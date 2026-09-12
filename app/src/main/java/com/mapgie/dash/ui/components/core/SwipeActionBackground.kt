package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.SwipeAction
import com.mapgie.dash.data.model.SwipeDirection
import com.mapgie.dash.ui.theme.Dimens

/**
 * The user's swipe for a Compose dismiss value, or null at rest. In an LTR
 * layout the content follows the finger, so StartToEnd is the swipe to the
 * right and EndToStart the swipe to the left (LESSONS #51).
 */
@OptIn(ExperimentalMaterial3Api::class)
fun SwipeToDismissBoxValue.toSwipeDirection(): SwipeDirection? = when (this) {
    SwipeToDismissBoxValue.StartToEnd -> SwipeDirection.RIGHT
    SwipeToDismissBoxValue.EndToStart -> SwipeDirection.LEFT
    SwipeToDismissBoxValue.Settled -> null
}

/**
 * The tinted panel a card reveals mid-swipe, with [label] on the edge the card
 * is leaving. Drawn only while a swipe is in progress so nothing sits behind a
 * resting card. Colour follows the action, and the word says it too: delete is
 * the one destructive action, so it alone uses the error container.
 */
@Composable
fun SwipeActionBackground(
    direction: SwipeDirection?,
    action: SwipeAction,
    label: String,
) {
    if (direction == null || action == SwipeAction.NONE) return
    val container = when (action) {
        SwipeAction.DONE -> MaterialTheme.colorScheme.secondaryContainer
        SwipeAction.SNOOZE, SwipeAction.ARCHIVE -> MaterialTheme.colorScheme.tertiaryContainer
        SwipeAction.DELETE -> MaterialTheme.colorScheme.errorContainer
        SwipeAction.NONE -> return
    }
    val content = when (action) {
        SwipeAction.DONE -> MaterialTheme.colorScheme.onSecondaryContainer
        SwipeAction.SNOOZE, SwipeAction.ARCHIVE -> MaterialTheme.colorScheme.onTertiaryContainer
        SwipeAction.DELETE -> MaterialTheme.colorScheme.onErrorContainer
        SwipeAction.NONE -> return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.cardInset)
            .background(container, shape = MaterialTheme.shapes.medium),
        // A right swipe exposes the left edge, and the other way round.
        contentAlignment = if (direction == SwipeDirection.RIGHT) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = content,
        )
    }
}
