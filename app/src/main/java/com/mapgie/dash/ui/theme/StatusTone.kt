package com.mapgie.dash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.isPast
import com.mapgie.dash.data.model.urgency

/**
 * One status vocabulary shared by Chores, Tasks and Memos so the same colour means
 * the same thing on every screen. The per-domain mappers below translate each
 * domain's own state into this common scale; the shared card shell then reads only
 * [StatusTone], never the domain enum.
 *
 * The scale is deliberately about **urgency** (how soon must this happen), the only
 * axis all three domains share. Chores and memos have no priority, so priority can
 * never be an app-wide bar colour and is carried by a non-colour marker instead.
 */
enum class StatusTone {
    /** Overdue / action required. */
    CRITICAL,

    /** Due soon / attention needed. */
    ATTENTION,

    /** Healthy / on track. */
    OK,

    /** Backgrounded: done, distant, or otherwise not signalling. */
    NEUTRAL,

    /** No state to show at all; the accent bar is transparent. */
    NONE,
}

/** Accent-bar colour for a tone. Reuses the documented status palette in `Color.kt`. */
@Composable
fun StatusTone.barColor(): Color = when (this) {
    StatusTone.CRITICAL -> StatusStale
    StatusTone.ATTENTION -> StatusAging
    StatusTone.OK -> StatusFresh
    StatusTone.NEUTRAL -> MaterialTheme.colorScheme.outline
    StatusTone.NONE -> Color.Transparent
}

/** True when the current scheme reads as dark (works for custom themes too). */
@Composable
private fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * Text colour for a status-coloured label (e.g. a due badge) matching the tone.
 * Light surfaces use the deep tones (the mid amber/sage are too pale for text);
 * dark surfaces use the mid tones, which read as the lighter variant there.
 */
@Composable
fun StatusTone.textColor(): Color {
    val dark = isDarkScheme()
    return when (this) {
        StatusTone.CRITICAL -> if (dark) StatusStale else StatusStaleDeep
        StatusTone.ATTENTION -> if (dark) StatusAging else StatusAgingDeep
        StatusTone.OK -> if (dark) StatusFresh else StatusFreshDeep
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        StatusTone.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * Background tint for a status badge pill. Returns null for tones that don't
 * signal (the label then renders as plain text without a pill).
 */
@Composable
fun StatusTone.badgeContainerColor(): Color? {
    val dark = isDarkScheme()
    return when (this) {
        StatusTone.CRITICAL -> if (dark) StatusStaleTintDark else StatusStaleTint
        StatusTone.ATTENTION -> if (dark) StatusAgingTintDark else StatusAgingTint
        StatusTone.OK -> if (dark) StatusFreshTintDark else StatusFreshTint
        StatusTone.NEUTRAL, StatusTone.NONE -> null
    }
}

/** Chore staleness mapped onto the shared scale. */
fun Chore.statusTone(): StatusTone = when (status) {
    ChoreStatus.STALE, ChoreStatus.NEVER -> StatusTone.CRITICAL
    ChoreStatus.AGING -> StatusTone.ATTENTION
    ChoreStatus.FRESH -> StatusTone.OK
}

/**
 * Task tone from [urgency], not priority. The bar therefore means the same thing
 * as on Chores and Memos; priority is carried separately by a non-colour marker.
 */
fun TaskDto.statusTone(): StatusTone = when (urgency()) {
    TaskUrgency.OVERDUE -> StatusTone.CRITICAL
    TaskUrgency.TODAY -> StatusTone.ATTENTION
    TaskUrgency.THIS_WEEK -> StatusTone.OK
    TaskUrgency.LATER -> StatusTone.NEUTRAL
    TaskUrgency.NONE -> StatusTone.NONE
}

/**
 * Memo tone by time-to-ring. Overdue reads as [StatusTone.CRITICAL], replacing the
 * `errorContainer`/`error` usage that `CLAUDE.md` reserves for genuine errors.
 */
fun ReminderDto.statusTone(): StatusTone = when {
    completedAt != null -> StatusTone.NEUTRAL
    isPast() -> StatusTone.CRITICAL
    else -> StatusTone.NEUTRAL
}
