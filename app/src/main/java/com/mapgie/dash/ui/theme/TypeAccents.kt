package com.mapgie.dash.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Per content-type accent colours (a container tone plus its on-container tone)
 * for the Tasks / Chores / Reminders tabs, used by the bottom nav indicator and
 * the add-menu FABs. Built-in palettes keep the fixed identity tones (light or
 * dark set per brightness) so each content type has a stable colour across
 * palettes; the custom theme maps them onto the user's primary/secondary/tertiary
 * picks so those colours show throughout the UI instead of clashing with
 * unrelated fixed pastels.
 *
 * Plain Kotlin (no composables) so WcagContrastTest can exercise the sets.
 */
data class TypeAccentColors(
    val taskContainer:     Color, val onTaskContainer:     Color,
    val choreContainer:    Color, val onChoreContainer:    Color,
    val reminderContainer: Color, val onReminderContainer: Color,
) {
    /** With the WCAG toggle on, every on-container tone is lifted to 7:1 on its container. */
    fun withWcagContrast(darkTheme: Boolean): TypeAccentColors {
        val darken = !darkTheme
        return copy(
            onTaskContainer     = onTaskContainer.adjustedForContrast(taskContainer, WCAG_TEXT_RATIO, darken),
            onChoreContainer    = onChoreContainer.adjustedForContrast(choreContainer, WCAG_TEXT_RATIO, darken),
            onReminderContainer = onReminderContainer.adjustedForContrast(reminderContainer, WCAG_TEXT_RATIO, darken),
        )
    }
}

/** The light set of the fixed content-type identity tones. */
val LightTypeAccents = TypeAccentColors(
    taskContainer     = TypeTaskContainer,     onTaskContainer     = TypeTaskOnContainer,
    choreContainer    = TypeChoreContainer,    onChoreContainer    = TypeChoreOnContainer,
    reminderContainer = TypeReminderContainer, onReminderContainer = TypeReminderOnContainer,
)

/** The Zen Dark set: same hues, dim containers with pale on-colours for dark surfaces. */
val DarkTypeAccents = TypeAccentColors(
    taskContainer     = TypeTaskContainerDark,     onTaskContainer     = TypeTaskOnContainerDark,
    choreContainer    = TypeChoreContainerDark,    onChoreContainer    = TypeChoreOnContainerDark,
    reminderContainer = TypeReminderContainerDark, onReminderContainer = TypeReminderOnContainerDark,
)

/** Defaults to the light content-type identity tones; [DashTheme] overrides it per brightness and for the custom theme. */
val LocalTypeAccents = staticCompositionLocalOf { LightTypeAccents }

/** True while the "WCAG accessible colours" setting is on; swatch text tones read it to lift themselves. */
val LocalWcagContrast = staticCompositionLocalOf { false }
