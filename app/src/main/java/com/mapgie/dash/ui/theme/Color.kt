package com.mapgie.dash.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── AppTheme catalogue ────────────────────────────────────────────────────────
//
// Adding a new palette:
//   1. Add an enum entry here with light and dark preview ARGBs.
//   2. Define private val color schemes below.
//   3. Add branches to colorSchemeFor().
//
// The enum name is persisted to DataStore. Old per-mode entries
// (SYSTEM_DEFAULT, SAGE_LIGHT, SAGE_DARK, CORAL_*, TEAL_*) are normalised
// to the palette name in SettingsRepository for backward compatibility.

enum class AppTheme(
    val displayName: String,
    // Light-mode preview ARGBs (Long) for the 3 colour roles
    val lightPrimaryArgb: Long,
    val lightSecondaryArgb: Long,
    val lightTertiaryArgb: Long,
    // Dark-mode preview ARGBs
    val darkPrimaryArgb: Long,
    val darkSecondaryArgb: Long,
    val darkTertiaryArgb: Long,
) {
    SAGE(
        "Sage",
        lightPrimaryArgb   = 0xFF4A7C59L, lightSecondaryArgb = 0xFF4E6355L, lightTertiaryArgb = 0xFF3A6472L,
        darkPrimaryArgb    = 0xFF9ED1ACL, darkSecondaryArgb  = 0xFFB4CCBBL, darkTertiaryArgb  = 0xFFA2CEDAL,
    ),
    CORAL(
        "Coral",
        lightPrimaryArgb   = 0xFFC35040L, lightSecondaryArgb = 0xFF775652L, lightTertiaryArgb = 0xFF725C2EL,
        darkPrimaryArgb    = 0xFFFFB4ABL, darkSecondaryArgb  = 0xFFE7BDB8L, darkTertiaryArgb  = 0xFFE3C07EL,
    ),
    TEAL(
        "Teal",
        lightPrimaryArgb   = 0xFF00747CL, lightSecondaryArgb = 0xFF4A6365L, lightTertiaryArgb = 0xFF4E6078L,
        darkPrimaryArgb    = 0xFF4DD8E0L, darkSecondaryArgb  = 0xFFB1CBCDL, darkTertiaryArgb  = 0xFFB7C7E2L,
    ),
    CUSTOM(
        "Custom",
        lightPrimaryArgb   = 0xFF888888L, lightSecondaryArgb = 0xFF888888L, lightTertiaryArgb = 0xFF888888L,
        darkPrimaryArgb    = 0xFF888888L, darkSecondaryArgb  = 0xFF888888L, darkTertiaryArgb  = 0xFF888888L,
    ),
}

// ── Sage palette (seed: #4A7C59) ──────────────────────────────────────────────
// Light scheme — existing values kept for backward compatibility

val md_theme_light_primary = Color(0xFF4A7C59)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFCCEDD8)
val md_theme_light_onPrimaryContainer = Color(0xFF002114)
val md_theme_light_secondary = Color(0xFF4E6355)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD0E8D7)
val md_theme_light_onSecondaryContainer = Color(0xFF0B1F15)
val md_theme_light_tertiary = Color(0xFF3A6472)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFBEE9F8)
val md_theme_light_onTertiaryContainer = Color(0xFF001F27)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFBFDF8)
val md_theme_light_onBackground = Color(0xFF191C1A)
val md_theme_light_surface = Color(0xFFFBFDF8)
val md_theme_light_onSurface = Color(0xFF191C1A)
val md_theme_light_surfaceVariant = Color(0xFFDCE5DC)
val md_theme_light_onSurfaceVariant = Color(0xFF404943)
val md_theme_light_outline = Color(0xFF707972)
val md_theme_light_inverseOnSurface = Color(0xFFEFF1EC)
val md_theme_light_inverseSurface = Color(0xFF2E312E)
val md_theme_light_inversePrimary = Color(0xFF9ED1AC)
val md_theme_light_surfaceTint = Color(0xFF4A7C59)
val md_theme_light_outlineVariant = Color(0xFFC0C9C1)
val md_theme_light_scrim = Color(0xFF000000)

// Dark scheme
val md_theme_dark_primary = Color(0xFF9ED1AC)
val md_theme_dark_onPrimary = Color(0xFF003824)
val md_theme_dark_primaryContainer = Color(0xFF335242)
val md_theme_dark_onPrimaryContainer = Color(0xFFCCEDD8)
val md_theme_dark_secondary = Color(0xFFB4CCBB)
val md_theme_dark_onSecondary = Color(0xFF213529)
val md_theme_dark_secondaryContainer = Color(0xFF374B3E)
val md_theme_dark_onSecondaryContainer = Color(0xFFD0E8D7)
val md_theme_dark_tertiary = Color(0xFFA2CEDA)
val md_theme_dark_onTertiary = Color(0xFF01353F)
val md_theme_dark_tertiaryContainer = Color(0xFF1F4D58)
val md_theme_dark_onTertiaryContainer = Color(0xFFBEE9F8)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF191C1A)
val md_theme_dark_onBackground = Color(0xFFE1E3DE)
val md_theme_dark_surface = Color(0xFF191C1A)
val md_theme_dark_onSurface = Color(0xFFE1E3DE)
val md_theme_dark_surfaceVariant = Color(0xFF404943)
val md_theme_dark_onSurfaceVariant = Color(0xFFC0C9C1)
val md_theme_dark_outline = Color(0xFF8A938B)
val md_theme_dark_inverseOnSurface = Color(0xFF191C1A)
val md_theme_dark_inverseSurface = Color(0xFFE1E3DE)
val md_theme_dark_inversePrimary = Color(0xFF4A7C59)
val md_theme_dark_surfaceTint = Color(0xFF9ED1AC)
val md_theme_dark_outlineVariant = Color(0xFF404943)
val md_theme_dark_scrim = Color(0xFF000000)

// Semantic status colours referenced by card left-bars
val StatusStale = Color(0xFFBA1A1A)
val StatusAging = Color(0xFFF29900)
val StatusFresh = Color(0xFF4A7C59)

// ── Sage colour schemes ───────────────────────────────────────────────────────

private val SageLightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

private val SageDarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

// ── Coral palette (seed: #C35040) ─────────────────────────────────────────────

private val CoralLightColors = lightColorScheme(
    primary = Color(0xFFC35040),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775652),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1513),
    tertiary = Color(0xFF725C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDFA7),
    onTertiaryContainer = Color(0xFF271900),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857370),
    inverseOnSurface = Color(0xFFFBEEEC),
    inverseSurface = Color(0xFF362F2E),
    inversePrimary = Color(0xFFFFB4AB),
    surfaceTint = Color(0xFFC35040),
    outlineVariant = Color(0xFFD8C2BF),
    scrim = Color(0xFF000000),
)

private val CoralDarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442927),
    secondaryContainer = Color(0xFF5D3F3C),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFE3C07E),
    onTertiary = Color(0xFF3F2D04),
    tertiaryContainer = Color(0xFF584319),
    onTertiaryContainer = Color(0xFFFFDFA7),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF201A19),
    onBackground = Color(0xFFEDE0DE),
    surface = Color(0xFF201A19),
    onSurface = Color(0xFFEDE0DE),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A),
    inverseOnSurface = Color(0xFF201A19),
    inverseSurface = Color(0xFFEDE0DE),
    inversePrimary = Color(0xFFC35040),
    surfaceTint = Color(0xFFFFB4AB),
    outlineVariant = Color(0xFF534341),
    scrim = Color(0xFF000000),
)

// ── Teal palette (seed: #00747C) ──────────────────────────────────────────────

private val TealLightColors = lightColorScheme(
    primary = Color(0xFF00747C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA0EEFF),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF4A6365),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE7E9),
    onSecondaryContainer = Color(0xFF051F21),
    tertiary = Color(0xFF4E6078),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E3F7),
    onTertiaryContainer = Color(0xFF0B1D31),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFDFD),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFD),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F4849),
    outline = Color(0xFF6F797A),
    inverseOnSurface = Color(0xFFEFF1F1),
    inverseSurface = Color(0xFF2D3131),
    inversePrimary = Color(0xFF4DD8E0),
    surfaceTint = Color(0xFF00747C),
    outlineVariant = Color(0xFFBEC8C9),
    scrim = Color(0xFF000000),
)

private val TealDarkColors = darkColorScheme(
    primary = Color(0xFF4DD8E0),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F55),
    onPrimaryContainer = Color(0xFFA0EEFF),
    secondary = Color(0xFFB1CBCD),
    onSecondary = Color(0xFF1C3436),
    secondaryContainer = Color(0xFF324B4D),
    onSecondaryContainer = Color(0xFFCDE7E9),
    tertiary = Color(0xFFB7C7E2),
    onTertiary = Color(0xFF213248),
    tertiaryContainer = Color(0xFF37495F),
    onTertiaryContainer = Color(0xFFD6E3F7),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C1C),
    onBackground = Color(0xFFE1E3E3),
    surface = Color(0xFF191C1C),
    onSurface = Color(0xFFE1E3E3),
    surfaceVariant = Color(0xFF3F4849),
    onSurfaceVariant = Color(0xFFBEC8C9),
    outline = Color(0xFF899393),
    inverseOnSurface = Color(0xFF191C1C),
    inverseSurface = Color(0xFFE1E3E3),
    inversePrimary = Color(0xFF00747C),
    surfaceTint = Color(0xFF4DD8E0),
    outlineVariant = Color(0xFF3F4849),
    scrim = Color(0xFF000000),
)

// ── Colour scheme router ──────────────────────────────────────────────────────

fun colorSchemeFor(appTheme: AppTheme, darkTheme: Boolean): ColorScheme = when (appTheme) {
    AppTheme.SAGE   -> if (darkTheme) SageDarkColors   else SageLightColors
    AppTheme.CORAL  -> if (darkTheme) CoralDarkColors  else CoralLightColors
    AppTheme.TEAL   -> if (darkTheme) TealDarkColors   else TealLightColors
    AppTheme.CUSTOM -> SageLightColors // placeholder; caller must use buildCustomColorScheme
}

// ── Custom HSL scheme builder ─────────────────────────────────────────────────

/**
 * Builds a Material 3 [ColorScheme] from three sets of HSL values (hue 0..360,
 * saturation 0..1, lightness 0..1).
 *
 * The primary, secondary, and tertiary roles use the caller-supplied H/S/L directly
 * for their main colour. Container, on-, surface, and outline tokens use fixed
 * lightness offsets derived from the primary hue to meet WCAG AA contrast targets.
 */
fun buildCustomColorScheme(
    primaryH: Float,   primaryS: Float,   primaryL: Float,
    secondaryH: Float, secondaryS: Float, secondaryL: Float,
    tertiaryH: Float,  tertiaryS: Float,  tertiaryL: Float,
    darkTheme: Boolean,
): ColorScheme {
    fun hsl(h: Float, s: Float, l: Float) = Color.hsl(h, s, l)

    return if (!darkTheme) {
        lightColorScheme(
            primary                = hsl(primaryH,   primaryS,   primaryL),
            onPrimary              = hsl(primaryH,   0.10f, 0.97f),
            primaryContainer       = hsl(primaryH,   primaryS.coerceAtMost(0.60f), 0.90f),
            onPrimaryContainer     = hsl(primaryH,   primaryS,   0.12f),
            secondary              = hsl(secondaryH, secondaryS, secondaryL),
            onSecondary            = hsl(secondaryH, 0.10f, 0.97f),
            secondaryContainer     = hsl(secondaryH, secondaryS.coerceAtMost(0.40f), 0.88f),
            onSecondaryContainer   = hsl(secondaryH, secondaryS, 0.12f),
            tertiary               = hsl(tertiaryH,  tertiaryS,  tertiaryL),
            onTertiary             = hsl(tertiaryH,  0.10f, 0.97f),
            tertiaryContainer      = hsl(tertiaryH,  tertiaryS.coerceAtMost(0.40f), 0.88f),
            onTertiaryContainer    = hsl(tertiaryH,  tertiaryS,  0.12f),
            error                  = Color(0xFFBA1A1A),
            errorContainer         = Color(0xFFFFDAD6),
            onError                = Color(0xFFFFFFFF),
            onErrorContainer       = Color(0xFF410002),
            background             = hsl(primaryH,   0.10f, 0.98f),
            onBackground           = hsl(primaryH,   0.10f, 0.10f),
            surface                = hsl(primaryH,   0.10f, 0.98f),
            onSurface              = hsl(primaryH,   0.10f, 0.10f),
            surfaceVariant         = hsl(primaryH,   0.20f, 0.88f),
            onSurfaceVariant       = hsl(primaryH,   0.15f, 0.25f),
            outline                = hsl(primaryH,   0.10f, 0.45f),
            inverseOnSurface       = hsl(primaryH,   0.10f, 0.93f),
            inverseSurface         = hsl(primaryH,   0.10f, 0.18f),
            inversePrimary         = hsl(primaryH,   primaryS.coerceAtMost(0.55f), 0.75f),
            surfaceTint            = hsl(primaryH,   primaryS,   primaryL),
            outlineVariant         = hsl(primaryH,   0.15f, 0.75f),
            scrim                  = Color(0xFF000000),
        )
    } else {
        darkColorScheme(
            primary                = hsl(primaryH,   primaryS.coerceAtMost(0.55f), 0.75f),
            onPrimary              = hsl(primaryH,   primaryS,   0.14f),
            primaryContainer       = hsl(primaryH,   primaryS.coerceAtMost(0.40f), 0.22f),
            onPrimaryContainer     = hsl(primaryH,   primaryS.coerceAtMost(0.60f), 0.90f),
            secondary              = hsl(secondaryH, secondaryS.coerceAtMost(0.35f), 0.72f),
            onSecondary            = hsl(secondaryH, secondaryS, 0.14f),
            secondaryContainer     = hsl(secondaryH, secondaryS.coerceAtMost(0.25f), 0.22f),
            onSecondaryContainer   = hsl(secondaryH, secondaryS.coerceAtMost(0.40f), 0.88f),
            tertiary               = hsl(tertiaryH,  tertiaryS.coerceAtMost(0.35f), 0.72f),
            onTertiary             = hsl(tertiaryH,  tertiaryS,  0.14f),
            tertiaryContainer      = hsl(tertiaryH,  tertiaryS.coerceAtMost(0.25f), 0.22f),
            onTertiaryContainer    = hsl(tertiaryH,  tertiaryS.coerceAtMost(0.40f), 0.88f),
            error                  = Color(0xFFFFB4AB),
            errorContainer         = Color(0xFF93000A),
            onError                = Color(0xFF690005),
            onErrorContainer       = Color(0xFFFFDAD6),
            background             = hsl(primaryH,   0.10f, 0.10f),
            onBackground           = hsl(primaryH,   0.10f, 0.88f),
            surface                = hsl(primaryH,   0.10f, 0.10f),
            onSurface              = hsl(primaryH,   0.10f, 0.88f),
            surfaceVariant         = hsl(primaryH,   0.15f, 0.25f),
            onSurfaceVariant       = hsl(primaryH,   0.15f, 0.75f),
            outline                = hsl(primaryH,   0.10f, 0.55f),
            inverseOnSurface       = hsl(primaryH,   0.10f, 0.10f),
            inverseSurface         = hsl(primaryH,   0.10f, 0.88f),
            inversePrimary         = hsl(primaryH,   primaryS,   0.30f),
            surfaceTint            = hsl(primaryH,   primaryS.coerceAtMost(0.55f), 0.75f),
            outlineVariant         = hsl(primaryH,   0.15f, 0.25f),
            scrim                  = Color(0xFF000000),
        )
    }
}
