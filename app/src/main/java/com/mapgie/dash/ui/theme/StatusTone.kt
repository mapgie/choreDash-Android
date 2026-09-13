package com.mapgie.dash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderScheduleText
import com.mapgie.dash.data.model.ReminderStatus
import com.mapgie.dash.data.model.Severity
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.priorityEnum
import java.time.Instant
import com.mapgie.dash.data.model.urgency

/**
 * One status vocabulary shared by Chores, Tasks and Memos so the same colour means
 * the same thing on every screen. The per-domain mappers below translate each
 * domain's own state into this common scale; the shared card shell then reads only
 * [StatusTone], never the domain enum.
 *
 * The scale is about **urgency** (how soon must this happen), the axis all three
 * domains share. On Tasks, priority folds into the same ladder rather than adding
 * a second one: a high priority raises a task that is not yet pressing to
 * [ATTENTION], a low priority with nothing pressing wears [LOW]. Chores and memos
 * have no priority and never produce [LOW]. Every tone is restated in words on
 * the card, so colour is never the only signal.
 */
enum class StatusTone {
    /** Overdue / action required. */
    CRITICAL,

    /** Due soon / attention needed. */
    ATTENTION,

    /** Healthy / on track. */
    OK,

    /** Can wait: a low-priority task with no pressing due date. Tasks only. */
    LOW,

    /** Backgrounded: done, distant, or otherwise not signalling. */
    NEUTRAL,

    /** No state to show at all; the accent bar is transparent. */
    NONE,
}

/** The Settings › Colours row a signalling tone is coloured by; null for the quiet tones. */
val StatusTone.severity: Severity?
    get() = when (this) {
        StatusTone.CRITICAL -> Severity.OVERDUE
        StatusTone.ATTENTION -> Severity.DUE_SOON
        StatusTone.OK -> Severity.FRESH
        StatusTone.LOW -> Severity.LOW
        StatusTone.NEUTRAL, StatusTone.NONE -> null
    }

/** The tone a Settings › Colours row previews. */
fun Severity.tone(): StatusTone = StatusTone.entries.first { it.severity == this }

/**
 * Which palette [Swatch] each signalling tone wears, or null where the user chose
 * "None" for that severity. Picked in Settings › Colours; the defaults are the
 * design's rose / amber / sage, plus blue for the low-priority tone.
 */
data class SeverityColors(
    val swatches: Map<Severity, Swatch?> = Severity.defaults,
) {
    /**
     * The swatch [tone] wears, or null when it has none: a quiet tone, or a
     * severity the user set to "None". Callers fall back to the neutral treatment
     * (outline spine, plain badge text, the type accent chip), never crash.
     */
    fun swatchFor(tone: StatusTone): Swatch? = tone.severity?.let { swatches[it] }
}

/** Provided by [DashTheme] from settings; defaults to the design's tones. */
val LocalSeverityColors = staticCompositionLocalOf { SeverityColors() }

/** True when the current scheme reads as dark (works for custom themes too). */
@Composable
fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Solid tone for spines and icon strokes, per the current brightness. */
@Composable
fun Swatch.spineColor(): Color = Color(tones(isDarkScheme()).spineArgb)

/**
 * Text tone that reads on both the tint and the surface, per the current
 * brightness. With the WCAG toggle on it is lifted to 7:1 on its own tint and
 * on the page ground (see [wcagSwatchText]).
 */
@Composable
fun Swatch.textColor(): Color {
    val dark = isDarkScheme()
    val tones = tones(dark)
    val text = Color(tones.textArgb)
    return if (LocalWcagContrast.current) {
        wcagSwatchText(text, Color(tones.tintArgb), MaterialTheme.colorScheme.background, dark)
    } else text
}

/**
 * The swatch text lift the WCAG toggle applies: darkened (light) or lightened
 * (dark) until it reads at 7:1 on both the badge tint and the page ground.
 * Pure so WcagContrastTest can pin it for every swatch.
 */
fun wcagSwatchText(text: Color, tint: Color, ground: Color, dark: Boolean): Color =
    text.adjustedForContrast(worstGround(listOf(tint, ground), dark), WCAG_TEXT_RATIO, !dark)

/** Pale (light) or dim (dark) tint behind badges and icon chips. */
@Composable
fun Swatch.tintColor(): Color = Color(tones(isDarkScheme()).tintArgb)

/**
 * Accent-bar colour for a tone, from the user's severity swatches. A signalling
 * tone whose severity is set to "None" draws the same quiet outline as neutral,
 * so the card keeps its spine; only a done item's bar is transparent.
 */
@Composable
fun StatusTone.barColor(): Color = when (this) {
    StatusTone.NONE -> Color.Transparent
    else -> LocalSeverityColors.current.swatchFor(this)?.spineColor()
        ?: MaterialTheme.colorScheme.outline
}

/** Text colour for a status-coloured label (e.g. a due badge) matching the tone. */
@Composable
fun StatusTone.textColor(): Color =
    LocalSeverityColors.current.swatchFor(this)?.textColor()
        ?: MaterialTheme.colorScheme.onSurfaceVariant

/**
 * Background tint for a status badge pill. Returns null for tones that don't
 * signal (the label then renders as plain text without a pill).
 */
@Composable
fun StatusTone.badgeContainerColor(): Color? =
    LocalSeverityColors.current.swatchFor(this)?.tintColor()

/** Chore staleness mapped onto the shared scale. */
fun Chore.statusTone(): StatusTone = when (status) {
    ChoreStatus.STALE, ChoreStatus.NEVER -> StatusTone.CRITICAL
    ChoreStatus.AGING -> StatusTone.ATTENTION
    ChoreStatus.FRESH -> StatusTone.OK
}

/**
 * Task tone: the due date when it is close, priority otherwise. An open task
 * always wears a colour, fresh by default, so no card sits on a grey bar.
 *
 * - Overdue is critical and due today is attention, whatever the priority.
 * - Due this week is fresh; a high priority lifts it to attention.
 * - Later or undated follows priority: high is attention, normal is fresh, low
 *   is [StatusTone.LOW], the fourth severity colour (blue by default).
 *
 * A completed task signals nothing: its spine and badge go quiet
 * ([StatusTone.NONE], a transparent bar), the same muted treatment a done memo gets.
 * Without this a task finished while overdue kept its rose spine in the Done list.
 * The caption still says "high" or "low" and the badge still names the date, so
 * the colour is never the only signal.
 */
fun TaskDto.statusTone(): StatusTone {
    if (completedAt != null) return StatusTone.NONE
    val priority = priorityEnum()
    return when (urgency()) {
        TaskUrgency.OVERDUE -> StatusTone.CRITICAL
        TaskUrgency.TODAY -> StatusTone.ATTENTION
        TaskUrgency.THIS_WEEK -> if (priority == TaskPriority.HIGHER) StatusTone.ATTENTION else StatusTone.OK
        TaskUrgency.LATER, TaskUrgency.NONE -> when (priority) {
            TaskPriority.HIGHER -> StatusTone.ATTENTION
            TaskPriority.NORMAL -> StatusTone.OK
            TaskPriority.LOWER -> StatusTone.LOW
        }
    }
}

/**
 * Memo tone from its schedule bucket (handoff 9a): a ring nobody answered is
 * [StatusTone.CRITICAL], a ring within 24 hours [StatusTone.ATTENTION], later
 * rings [StatusTone.OK]; a completed once-only memo, and anything archived, is
 * one plain muted state with no colour.
 */
fun ReminderDto.statusTone(now: Instant = Instant.now()): StatusTone = when {
    archivedAt != null -> StatusTone.NONE
    else -> when (ReminderScheduleText.status(this, now)) {
        ReminderStatus.RANG -> StatusTone.CRITICAL
        ReminderStatus.DUE_SOON -> StatusTone.ATTENTION
        ReminderStatus.UPCOMING -> StatusTone.OK
        ReminderStatus.DONE -> StatusTone.NONE
        ReminderStatus.OFF -> StatusTone.NEUTRAL
    }
}
