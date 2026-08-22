package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.dash.ui.theme.Dimens

/** Colour intent of a swipe action panel. */
enum class SwipeTone { POSITIVE, DESTRUCTIVE }

/**
 * One swipe reveal: the [label] on the panel, its colour [tone], and the [onSwipe]
 * callback fired when the user swipes past the threshold. A destructive action
 * typically opens a confirmation rather than acting immediately; that is the
 * caller's choice inside [onSwipe].
 */
data class SwipeAction(
    val label: String,
    val tone: SwipeTone,
    val onSwipe: () -> Unit,
)

/**
 * The one swipe-to-act wrapper, shared by every list card.
 *
 * Fixes the §5b layering bug centrally: the action panel is composed **only while a
 * swipe is in progress** (gated on [SwipeToDismissBoxValue]), so nothing sits behind
 * a card at rest, and the panel is inset by [Dimens.cardInset] to match the card, so
 * there is no overhanging rim.
 *
 * @param startAction revealed when dragging the row to the right (start-to-end).
 * @param endAction revealed when dragging the row to the left (end-to-start).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActionRow(
    modifier: Modifier = Modifier,
    startAction: SwipeAction? = null,
    endAction: SwipeAction? = null,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> startAction?.onSwipe?.invoke()
                SwipeToDismissBoxValue.EndToStart -> endAction?.onSwipe?.invoke()
                SwipeToDismissBoxValue.Settled -> {}
            }
            false // never actually dismiss; the action owns the state change
        },
        positionalThreshold = { it * 0.3f },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = startAction != null,
        enableDismissFromEndToStart = endAction != null,
        backgroundContent = {
            val action = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> startAction
                SwipeToDismissBoxValue.EndToStart -> endAction
                SwipeToDismissBoxValue.Settled -> null
            }
            if (action != null) {
                SwipePanel(
                    action = action,
                    atEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart,
                )
            }
        },
        content = { content() },
    )
}

@Composable
private fun SwipePanel(action: SwipeAction, atEnd: Boolean) {
    val container = when (action.tone) {
        SwipeTone.POSITIVE -> MaterialTheme.colorScheme.primaryContainer
        SwipeTone.DESTRUCTIVE -> MaterialTheme.colorScheme.errorContainer
    }
    val onContainer = when (action.tone) {
        SwipeTone.POSITIVE -> MaterialTheme.colorScheme.onPrimaryContainer
        SwipeTone.DESTRUCTIVE -> MaterialTheme.colorScheme.onErrorContainer
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.cardInset)
            .background(container, shape = MaterialTheme.shapes.medium),
        contentAlignment = if (atEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = action.label,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.labelMedium,
            color = onContainer,
        )
    }
}
