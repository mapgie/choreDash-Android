package com.mapgie.dash.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Carries all nine HSL values for the custom colour theme, plus the optional
 * per-mode background overrides (0 = derive from the primary hue), so
 * [DashTheme] can forward them to [buildCustomColorScheme] in one parameter.
 */
data class CustomHSL(
    val primaryH: Float,   val primaryS: Float,   val primaryL: Float,
    val secondaryH: Float, val secondaryS: Float, val secondaryL: Float,
    val tertiaryH: Float,  val tertiaryS: Float,  val tertiaryL: Float,
    val lightBackgroundArgb: Int = 0,
    val darkBackgroundArgb: Int = 0,
)

/**
 * Per content-type accent colours (a container tone plus its on-container tone)
 * for the Tasks / Chores / Reminders tabs, used by the bottom nav indicator and
 * the add-menu FABs. Built-in palettes keep the fixed identity tones so each
 * content type has a stable colour across palettes; the custom theme maps them
 * onto the user's primary/secondary/tertiary picks so those colours show
 * throughout the UI instead of clashing with unrelated fixed pastels.
 */
data class TypeAccentColors(
    val taskContainer:     Color, val onTaskContainer:     Color,
    val choreContainer:    Color, val onChoreContainer:    Color,
    val reminderContainer: Color, val onReminderContainer: Color,
)

/** Defaults to the fixed content-type identity tones; [DashTheme] overrides it for the custom theme. */
val LocalTypeAccents = staticCompositionLocalOf {
    TypeAccentColors(
        taskContainer     = TypeTaskContainer,     onTaskContainer     = TypeTaskOnContainer,
        choreContainer    = TypeChoreContainer,    onChoreContainer    = TypeChoreOnContainer,
        reminderContainer = TypeReminderContainer, onReminderContainer = TypeReminderOnContainer,
    )
}

@Composable
fun DashTheme(
    appTheme:  AppTheme = AppTheme.CREAM,
    darkTheme: Boolean  = isSystemInDarkTheme(),
    wcag:      Boolean  = false,
    customHSL: CustomHSL? = null,
    content:   @Composable () -> Unit,
) {
    val colorScheme = if (appTheme == AppTheme.CUSTOM && customHSL != null) {
        buildCustomColorScheme(
            primaryH   = customHSL.primaryH,
            primaryS   = customHSL.primaryS,
            primaryL   = customHSL.primaryL,
            secondaryH = customHSL.secondaryH,
            secondaryS = customHSL.secondaryS,
            secondaryL = customHSL.secondaryL,
            tertiaryH  = customHSL.tertiaryH,
            tertiaryS  = customHSL.tertiaryS,
            tertiaryL  = customHSL.tertiaryL,
            darkTheme  = darkTheme,
            backgroundArgb = if (darkTheme) customHSL.darkBackgroundArgb
                             else customHSL.lightBackgroundArgb,
        )
    } else {
        colorSchemeFor(appTheme, darkTheme, wcag)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    // For the custom theme, map the content-type accents onto the user's
    // primary/secondary/tertiary picks so Tasks/Chores/Reminders carry the
    // chosen colours; built-in palettes keep their fixed identity tones.
    val typeAccents = if (appTheme == AppTheme.CUSTOM && customHSL != null) {
        TypeAccentColors(
            taskContainer     = colorScheme.primaryContainer,   onTaskContainer     = colorScheme.onPrimaryContainer,
            choreContainer    = colorScheme.secondaryContainer,  onChoreContainer    = colorScheme.onSecondaryContainer,
            reminderContainer = colorScheme.tertiaryContainer,   onReminderContainer = colorScheme.onTertiaryContainer,
        )
    } else {
        TypeAccentColors(
            taskContainer     = TypeTaskContainer,     onTaskContainer     = TypeTaskOnContainer,
            choreContainer    = TypeChoreContainer,    onChoreContainer    = TypeChoreOnContainer,
            reminderContainer = TypeReminderContainer, onReminderContainer = TypeReminderOnContainer,
        )
    }

    CompositionLocalProvider(LocalTypeAccents provides typeAccents) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = DashTypography,
            shapes      = DashShapes,
            content     = content,
        )
    }
}
