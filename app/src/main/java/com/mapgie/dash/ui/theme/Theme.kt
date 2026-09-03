package com.mapgie.dash.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
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

// TypeAccentColors, LightTypeAccents, DarkTypeAccents, LocalTypeAccents and
// LocalWcagContrast live in TypeAccents.kt so the unit tests can reach them.

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
    val baseTypeAccents = if (appTheme == AppTheme.CUSTOM && customHSL != null) {
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

    // The WCAG toggle only applies to the built-in palettes (custom colours are
    // applied exactly as picked). It reaches every colour the screens actually
    // draw text with, not just the Material roles: the design tokens (faint
    // captions, section counts, inactive tabs, the tag gold, the outlined pill)
    // and the content-type accents lift alongside the scheme; the status
    // swatches read LocalWcagContrast and lift themselves in StatusTone.kt.
    val wcagActive = wcag && appTheme != AppTheme.CUSTOM
    val tokens = if (wcagActive) baseTokens.withWcagContrast(colorScheme, darkTheme) else baseTokens
    val typeAccents = if (wcagActive) baseTypeAccents.withWcagContrast(darkTheme) else baseTypeAccents

    CompositionLocalProvider(
        LocalTypeAccents provides typeAccents,
        LocalDashTokens provides tokens,
        LocalSeverityColors provides SeverityColors.from(severitySwatches),
        LocalWcagContrast provides wcagActive,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = DashTypography,
            shapes      = DashShapes,
            content     = content,
        )
    }
}
