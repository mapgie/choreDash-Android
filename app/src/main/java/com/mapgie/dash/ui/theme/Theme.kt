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
import com.mapgie.dash.data.model.Severity
import com.mapgie.dash.data.model.Swatch

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
 * the add-menu FABs. Built-in palettes keep the fixed identity tones (light or
 * dark set per brightness) so each content type has a stable colour across
 * palettes; the custom theme maps them onto the user's primary/secondary/tertiary
 * picks so those colours show throughout the UI instead of clashing with
 * unrelated fixed pastels.
 */
data class TypeAccentColors(
    val taskContainer:     Color, val onTaskContainer:     Color,
    val choreContainer:    Color, val onChoreContainer:    Color,
    val reminderContainer: Color, val onReminderContainer: Color,
)

/** The light set of the fixed content-type identity tones. */
private val LightTypeAccents = TypeAccentColors(
    taskContainer     = TypeTaskContainer,     onTaskContainer     = TypeTaskOnContainer,
    choreContainer    = TypeChoreContainer,    onChoreContainer    = TypeChoreOnContainer,
    reminderContainer = TypeReminderContainer, onReminderContainer = TypeReminderOnContainer,
)

/** The Zen Dark set: same hues, dim containers with pale on-colours for dark surfaces. */
private val DarkTypeAccents = TypeAccentColors(
    taskContainer     = TypeTaskContainerDark,     onTaskContainer     = TypeTaskOnContainerDark,
    choreContainer    = TypeChoreContainerDark,    onChoreContainer    = TypeChoreOnContainerDark,
    reminderContainer = TypeReminderContainerDark, onReminderContainer = TypeReminderOnContainerDark,
)

/** Defaults to the light content-type identity tones; [DashTheme] overrides it per brightness and for the custom theme. */
val LocalTypeAccents = staticCompositionLocalOf { LightTypeAccents }

@Composable
fun DashTheme(
    appTheme:  AppTheme = AppTheme.CREAM,
    darkTheme: Boolean  = isSystemInDarkTheme(),
    wcag:      Boolean  = false,
    customHSL: CustomHSL? = null,
    severitySwatches: Map<Severity, Swatch> = Severity.defaults,
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
    // chosen colours; built-in palettes keep their fixed identity tones, in the
    // light or dark set so the on-colours stay readable on dark surfaces.
    val typeAccents = if (appTheme == AppTheme.CUSTOM && customHSL != null) {
        TypeAccentColors(
            taskContainer     = colorScheme.primaryContainer,   onTaskContainer     = colorScheme.onPrimaryContainer,
            choreContainer    = colorScheme.secondaryContainer,  onChoreContainer    = colorScheme.onSecondaryContainer,
            reminderContainer = colorScheme.tertiaryContainer,   onReminderContainer = colorScheme.onTertiaryContainer,
        )
    } else if (darkTheme) {
        DarkTypeAccents
    } else {
        LightTypeAccents
    }

    // Cream gets the handoff's exact non-M3 tokens; every other palette derives
    // them from its own scheme so the same components stay on-palette.
    val baseTokens = when {
        appTheme == AppTheme.CREAM && darkTheme -> ZenDarkTokens
        appTheme == AppTheme.CREAM -> CreamLightTokens
        else -> derivedTokens(colorScheme, darkTheme)
    }
    // Handoff contrast rule: with the WCAG toggle on, ink faint lifts to ink
    // muted so card meta lines and captions clear 4.5:1 with margin.
    val tokens = if (wcag) baseTokens.copy(
        inkFaint = colorScheme.onSurfaceVariant,
        sectionCount = colorScheme.onSurfaceVariant,
    ) else baseTokens

    CompositionLocalProvider(
        LocalTypeAccents provides typeAccents,
        LocalDashTokens provides tokens,
        LocalSeverityColors provides SeverityColors.from(severitySwatches),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = DashTypography,
            shapes      = DashShapes,
            content     = content,
        )
    }
}
