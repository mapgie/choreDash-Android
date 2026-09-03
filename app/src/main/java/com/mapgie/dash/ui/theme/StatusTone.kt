package com.mapgie.dash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.Severity
import com.mapgie.dash.data.model.Swatch
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

/**
 * Which palette [Swatch] each signalling tone wears. The user picks these in
 * Settings › Colours; the defaults are the design's rose / amber / sage.
 */
data class SeverityColors(
    val critical: Swatch = Swatch.ROSE,
    val attention: Swatch = Swatch.AMBER,
    val ok: Swatch = Swatch.SAGE,
) {
    fun swatchFor(tone: StatusTone): Swatch? = when (tone) {
        StatusTone.CRITICAL -> critical
        StatusTone.ATTENTION -> attention
        StatusTone.OK -> ok
        StatusTone.NEUTRAL, StatusTone.NONE -> null
    }

    companion object {
        fun from(map: Map<Severity, Swatch>) = SeverityColors(
            critical = map[Severity.OVERDUE] ?: Severity.OVERDUE.defaultSwatch,
            attention = map[Severity.DUE_SOON] ?: Severity.DUE_SOON.defaultSwatch,
            ok = map[Severity.FRESH] ?: Severity.FRESH.defaultSwatch,
        )
    }
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

/** Accent-bar colour for a tone, from the user's severity swatches. */
@Composable
fun StatusTone.barColor(): Color = when (this) {
    StatusTone.NEUTRAL -> MaterialTheme.colorScheme.outline
    StatusTone.NONE -> Color.Transparent
    else -> LocalSeverityColors.current.swatchFor(this)!!.spineColor()
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
