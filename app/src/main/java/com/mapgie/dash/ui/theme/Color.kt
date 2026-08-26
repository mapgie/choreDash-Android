package com.mapgie.dash.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

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
    CREAM(
        "Cream",
        lightPrimaryArgb   = 0xFF7A5FA0L, lightSecondaryArgb = 0xFF5F7D52L, lightTertiaryArgb = 0xFFB07F24L,
        darkPrimaryArgb    = 0xFFC9B8E8L, darkSecondaryArgb  = 0xFFA9C494L, darkTertiaryArgb  = 0xFFDFCF90L,
    ),
    MIST(
        "Mist",
        lightPrimaryArgb   = 0xFF5B5FA6L, lightSecondaryArgb = 0xFF5D5C72L, lightTertiaryArgb = 0xFF79556CL,
        darkPrimaryArgb    = 0xFFC4C1FFL, darkSecondaryArgb  = 0xFFC6C4DDL, darkTertiaryArgb  = 0xFFE9B9D6L,
    ),
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

// Semantic status colours used on card strips, due badges, and date text.
// Rose  (StatusStale) = overdue / action required
// Amber (StatusAging) = approaching due / attention needed
// Sage  (StatusFresh) = healthy / no action needed
//
// Each status has three fixed tones from the Cozy Cream design system:
//   - the mid tone for accent bars and spines,
//   - a deep tone for text on light surfaces (the mid amber/sage are too pale
//     to read as text; in dark mode text falls back to the mid tone),
//   - a light tint for badge-pill backgrounds on light surfaces (dark surfaces
//     use a dimmed tint so the pill stays subtle).
val StatusStale = Color(0xFFB8524E)
val StatusAging = Color(0xFFD9A648)
val StatusFresh = Color(0xFF8AA877)

val StatusStaleDeep = Color(0xFFA03E3A)
val StatusAgingDeep = Color(0xFF9A6E1C)
val StatusFreshDeep = Color(0xFF57724B)

val StatusStaleTint = Color(0xFFF6E3E1)
val StatusAgingTint = Color(0xFFF3E8D2)
val StatusFreshTint = Color(0xFFE7ECDD)

val StatusStaleTintDark = Color(0xFF4A302E)
val StatusAgingTintDark = Color(0xFF48402A)
val StatusFreshTintDark = Color(0xFF39422F)

// Content-type accent tones. Fixed across all AppTheme palettes (light or dark)
// so Chores, Tasks, and Reminders keep a stable colour identity no matter which
// palette the user has picked — used on the bottom nav indicator and the add-menu
// FAB. Colour is a secondary cue only: icon shape and text label already
// distinguish the three types, satisfying the "not colour alone" rule.
val TypeTaskContainer = Color(0xFFE9E0F2)
val TypeTaskOnContainer = Color(0xFF4A3866)
val TypeChoreContainer = Color(0xFFDFE8D3)
val TypeChoreOnContainer = Color(0xFF33471F)
val TypeReminderContainer = Color(0xFFF3E8D2)
val TypeReminderOnContainer = Color(0xFF5C4413)

// Owner avatar palette. A person's handle hashes to one of these six tones via
// ownerColorFor(), so the same owner shows the same colour on every screen and in
// the overview sheets. Containers are pastel with dark on-colours and, like the
// Type* accents above, are deliberately fixed across light and dark palettes so a
// person's identity colour stays stable. The initial letter is always drawn and the
// avatar carries a "Owner: <handle>" description, so colour is never the only signal.
data class AvatarTone(val container: Color, val onContainer: Color)

val AvatarTones: List<AvatarTone> = listOf(
    AvatarTone(Color(0xFFF8D7DD), Color(0xFF6E2434)), // rose
    AvatarTone(Color(0xFFF6E4C0), Color(0xFF614100)), // amber
    AvatarTone(Color(0xFFD6E8D0), Color(0xFF244420)), // green
    AvatarTone(Color(0xFFC9E7E6), Color(0xFF104A48)), // teal
    AvatarTone(Color(0xFFD5E2FA), Color(0xFF1B3B6B)), // blue
    AvatarTone(Color(0xFFE7D9F7), Color(0xFF432B63)), // violet
)

/**
 * Stable mapping from an owner handle to one of [AvatarTones]. Plain Kotlin (no
 * Compose) so the Glance widgets can share it. Case- and whitespace-insensitive, so
 * "Alice", "alice", and " Alice " all resolve to the same tone.
 */
fun ownerColorFor(handle: String): AvatarTone {
    val key = handle.trim().lowercase()
    var hash = 0
    for (ch in key) hash = hash * 31 + ch.code
    val index = ((hash % AvatarTones.size) + AvatarTones.size) % AvatarTones.size
    return AvatarTones[index]
}

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

// ── Mist palette (seed: #5B5FA6) ──────────────────────────────────────────────
// A light periwinkle primary over blue-violet-grey neutrals. Former default,
// superseded by Cream; kept for users who picked it explicitly.

val MistLightColors = lightColorScheme(
    primary = Color(0xFF5B5FA6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2DFFF),
    onPrimaryContainer = Color(0xFF14134B),
    secondary = Color(0xFF5D5C72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3DFF9),
    onSecondaryContainer = Color(0xFF1A1836),
    tertiary = Color(0xFF79556C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EC),
    onTertiaryContainer = Color(0xFF2E1225),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F7FC),
    onBackground = Color(0xFF1B1B23),
    surface = Color(0xFFF7F7FC),
    onSurface = Color(0xFF1B1B23),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF767680),
    inverseOnSurface = Color(0xFFF1EFF9),
    inverseSurface = Color(0xFF302F38),
    inversePrimary = Color(0xFFC4C1FF),
    surfaceTint = Color(0xFF5B5FA6),
    outlineVariant = Color(0xFFC7C5D0),
    scrim = Color(0xFF000000),
)

val MistDarkColors = darkColorScheme(
    primary = Color(0xFFC4C1FF),
    onPrimary = Color(0xFF29295C),
    primaryContainer = Color(0xFF403F8D),
    onPrimaryContainer = Color(0xFFE2DFFF),
    secondary = Color(0xFFC6C4DD),
    onSecondary = Color(0xFF2F2E42),
    secondaryContainer = Color(0xFF464559),
    onSecondaryContainer = Color(0xFFE3DFF9),
    tertiary = Color(0xFFE9B9D6),
    onTertiary = Color(0xFF46293C),
    tertiaryContainer = Color(0xFF5F3F53),
    onTertiaryContainer = Color(0xFFFFD8EC),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF90909A),
    inverseOnSurface = Color(0xFF131318),
    inverseSurface = Color(0xFFE5E1E9),
    inversePrimary = Color(0xFF5B5FA6),
    surfaceTint = Color(0xFFC4C1FF),
    outlineVariant = Color(0xFF46464F),
    scrim = Color(0xFF000000),
)

// ── Cream palette ("Cozy Cream" design system) ────────────────────────────────
// Default palette. Warm cream ground with white cards, a lavender primary
// (buttons, toggles, selection), a sage secondary (positive accent), and an
// amber tertiary. The surfaceContainer ramp is declared explicitly so sheets
// and the nav bar land on the design's warm neutrals rather than the M3
// baseline's cool ones. Dark mode reuses the design's dark reminder screen:
// a deep green-brown ground with gold and soft-lavender accents.

val CreamLightColors = lightColorScheme(
    primary = Color(0xFF7A5FA0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9E0F2),
    onPrimaryContainer = Color(0xFF43315E),
    secondary = Color(0xFF5F7D52),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE8D3),
    onSecondaryContainer = Color(0xFF324528),
    tertiary = Color(0xFFB07F24),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E8D2),
    onTertiaryContainer = Color(0xFF5C3F12),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2EDE2),
    onBackground = Color(0xFF33302A),
    surface = Color(0xFFF7F2E7),
    onSurface = Color(0xFF33302A),
    surfaceVariant = Color(0xFFFFFDF9),
    onSurfaceVariant = Color(0xFF6D6455),
    surfaceBright = Color(0xFFFFFDF9),
    surfaceDim = Color(0xFFE8E0CF),
    surfaceContainerLowest = Color(0xFFFFFDF9),
    surfaceContainerLow = Color(0xFFFBF7EE),
    surfaceContainer = Color(0xFFF7F2E7),
    surfaceContainerHigh = Color(0xFFF2EBDC),
    surfaceContainerHighest = Color(0xFFEFE8DA),
    outline = Color(0xFF8F8571),
    inverseOnSurface = Color(0xFFF5F1E6),
    inverseSurface = Color(0xFF33302A),
    inversePrimary = Color(0xFFCDBBE8),
    surfaceTint = Color(0xFF7A5FA0),
    outlineVariant = Color(0xFFDDD3C1),
    scrim = Color(0xFF000000),
)

val CreamDarkColors = darkColorScheme(
    primary = Color(0xFFC9B8E8),
    onPrimary = Color(0xFF3A2A54),
    primaryContainer = Color(0xFF55407A),
    onPrimaryContainer = Color(0xFFE9E0F2),
    secondary = Color(0xFFA9C494),
    onSecondary = Color(0xFF2A3A22),
    secondaryContainer = Color(0xFF47583C),
    onSecondaryContainer = Color(0xFFDFE8D3),
    tertiary = Color(0xFFDFCF90),
    onTertiary = Color(0xFF3B3213),
    tertiaryContainer = Color(0xFF57491D),
    onTertiaryContainer = Color(0xFFF5EBC9),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF272E24),
    onBackground = Color(0xFFF0EAD9),
    surface = Color(0xFF2C3328),
    onSurface = Color(0xFFF0EAD9),
    surfaceVariant = Color(0xFF363E33),
    onSurfaceVariant = Color(0xFFC2BBA6),
    surfaceBright = Color(0xFF3A4236),
    surfaceDim = Color(0xFF1E2419),
    surfaceContainerLowest = Color(0xFF1C2118),
    surfaceContainerLow = Color(0xFF262C22),
    surfaceContainer = Color(0xFF2C3328),
    surfaceContainerHigh = Color(0xFF333B2F),
    surfaceContainerHighest = Color(0xFF3A4236),
    outline = Color(0xFF918A76),
    inverseOnSurface = Color(0xFF2B3227),
    inverseSurface = Color(0xFFF0EAD9),
    inversePrimary = Color(0xFF7A5FA0),
    surfaceTint = Color(0xFFC9B8E8),
    outlineVariant = Color(0xFF454D3F),
    scrim = Color(0xFF000000),
)

// ── Colour scheme router ──────────────────────────────────────────────────────

fun colorSchemeFor(appTheme: AppTheme, darkTheme: Boolean, wcag: Boolean = false): ColorScheme {
    val base = when (appTheme) {
        AppTheme.CREAM  -> if (darkTheme) CreamDarkColors  else CreamLightColors
        AppTheme.MIST   -> if (darkTheme) MistDarkColors   else MistLightColors
        AppTheme.SAGE   -> if (darkTheme) SageDarkColors   else SageLightColors
        AppTheme.CORAL  -> if (darkTheme) CoralDarkColors  else CoralLightColors
        AppTheme.TEAL   -> if (darkTheme) TealDarkColors   else TealLightColors
        AppTheme.CUSTOM -> CreamLightColors // placeholder; caller must use buildCustomColorScheme
    }
    return if (wcag) base.withWcagContrast(darkTheme) else base
}

// ── WCAG high-contrast transform ──────────────────────────────────────────────

private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/**
 * Steps a colour's HSL lightness away from [against] until the WCAG contrast
 * ratio reaches [target]. [darken] chooses the direction (darken on light
 * backgrounds, lighten on dark ones).
 */
private fun Color.adjustedForContrast(against: Color, target: Float, darken: Boolean): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    var current = this
    var steps = 0
    while (contrastRatio(current, against) < target && steps < 20) {
        hsl[2] = (hsl[2] + if (darken) -0.05f else 0.05f).coerceIn(0f, 1f)
        current = Color.hsl(hsl[0], hsl[1], hsl[2])
        steps++
    }
    return current
}

/**
 * Derives a WCAG high-contrast variant of a palette programmatically: accent
 * roles are pushed to at least 7:1 against the background (AAA for normal
 * text), supporting text to 7:1, and outlines to 4.5:1. Backgrounds and
 * surfaces are left untouched so the palette keeps its identity.
 */
fun ColorScheme.withWcagContrast(darkTheme: Boolean): ColorScheme {
    val darken = !darkTheme
    val fixedPrimary   = primary.adjustedForContrast(background, 7f, darken)
    val fixedSecondary = secondary.adjustedForContrast(background, 7f, darken)
    val fixedTertiary  = tertiary.adjustedForContrast(background, 7f, darken)
    return copy(
        primary          = fixedPrimary,
        secondary        = fixedSecondary,
        tertiary         = fixedTertiary,
        surfaceTint      = fixedPrimary,
        onSurfaceVariant = onSurfaceVariant.adjustedForContrast(surfaceVariant, 7f, darken),
        outline          = outline.adjustedForContrast(background, 4.5f, darken),
        onPrimaryContainer   = onPrimaryContainer.adjustedForContrast(primaryContainer, 7f, darken),
        onSecondaryContainer = onSecondaryContainer.adjustedForContrast(secondaryContainer, 7f, darken),
        onTertiaryContainer  = onTertiaryContainer.adjustedForContrast(tertiaryContainer, 7f, darken),
    )
}

// ── Custom HSL scheme builder ─────────────────────────────────────────────────

/** Near-black on bright colours, white on dark colours — keeps text legible on any pick. */
private fun contrastingOn(color: Color): Color =
    if (color.luminance() > 0.35f) Color(0xFF1C1B1F) else Color.White

/**
 * Builds a Material 3 [ColorScheme] from three sets of HSL values (hue 0..360,
 * saturation 0..1, lightness 0..1).
 *
 * The picked colours are applied **verbatim** to the primary, secondary, and
 * tertiary roles in both light and dark mode; on-colours are chosen by relative
 * luminance so text stays legible whatever the user picks. Containers are
 * derived from the picked hues with mode-appropriate lightness. Neutrals and
 * the full `surfaceContainer*` ramp are derived from the background's *hue
 * only*, at a near-zero fixed saturation and absolute per-mode lightness
 * values, so a vivid background can never turn the neutral surfaces muddy.
 *
 * [backgroundArgb] overrides background/surface for the current mode (0 keeps
 * the background derived from the primary hue).
 */
fun buildCustomColorScheme(
    primaryH: Float,   primaryS: Float,   primaryL: Float,
    secondaryH: Float, secondaryS: Float, secondaryL: Float,
    tertiaryH: Float,  tertiaryS: Float,  tertiaryL: Float,
    darkTheme: Boolean,
    backgroundArgb: Int = 0,
): ColorScheme {
    val primary   = Color.hsl(primaryH,   primaryS,   primaryL)
    val secondary = Color.hsl(secondaryH, secondaryS, secondaryL)
    val tertiary  = Color.hsl(tertiaryH,  tertiaryS,  tertiaryL)

    val background = when {
        backgroundArgb != 0 -> Color(backgroundArgb)
        darkTheme           -> Color.hsl(primaryH, 0.10f, 0.10f)
        else                -> Color.hsl(primaryH, 0.10f, 0.98f)
    }
    val bg = FloatArray(3).also { ColorUtils.colorToHSL(background.toArgb(), it) }
    val bgIsLight = background.luminance() > 0.35f

    // Neutrals are derived from the background's hue only, at a near-zero fixed
    // saturation and an absolute per-mode lightness. This is deliberately
    // independent of the background's own HSL saturation/lightness (bg[1]/bg[2]):
    // for a vivid mid-lightness hue like yellow those diverge badly from
    // luminance-based "is this light or dark" and used to produce olive mud.
    val bgHue = bg[0]
    fun neutral(l: Float) = Color.hsl(bgHue, 0.04f, l.coerceIn(0f, 1f))

    val surface          = background
    val onBackground     = contrastingOn(background)
    val onSurface        = contrastingOn(surface)

    val surfaceContainerLowest  = if (bgIsLight) neutral(0.99f) else neutral(0.06f)
    val surfaceContainerLow     = if (bgIsLight) neutral(0.97f) else neutral(0.10f)
    val surfaceContainer        = if (bgIsLight) neutral(0.95f) else neutral(0.12f)
    val surfaceContainerHigh    = if (bgIsLight) neutral(0.93f) else neutral(0.15f)
    val surfaceContainerHighest = if (bgIsLight) neutral(0.91f) else neutral(0.17f)
    val surfaceVariant          = if (bgIsLight) neutral(0.90f) else neutral(0.17f)
    val surfaceBright           = if (bgIsLight) neutral(0.99f) else neutral(0.24f)
    val surfaceDim              = if (bgIsLight) neutral(0.87f) else neutral(0.06f)
    val onSurfaceVariant        = if (bgIsLight) neutral(0.30f) else neutral(0.80f)
    val outline                 = if (bgIsLight) neutral(0.50f) else neutral(0.55f)
    val outlineVariant          = if (bgIsLight) neutral(0.80f) else neutral(0.30f)
    val inverseSurface          = if (bgIsLight) neutral(0.20f) else neutral(0.90f)
    val inverseOnSurface        = if (bgIsLight) neutral(0.95f) else neutral(0.10f)

    return if (!darkTheme) {
        lightColorScheme(
            primary                = primary,
            onPrimary              = contrastingOn(primary),
            primaryContainer       = Color.hsl(primaryH, primaryS.coerceAtMost(0.60f), 0.90f),
            onPrimaryContainer     = Color.hsl(primaryH, primaryS, 0.12f),
            secondary              = secondary,
            onSecondary            = contrastingOn(secondary),
            secondaryContainer     = Color.hsl(secondaryH, secondaryS.coerceAtMost(0.40f), 0.88f),
            onSecondaryContainer   = Color.hsl(secondaryH, secondaryS, 0.12f),
            tertiary               = tertiary,
            onTertiary             = contrastingOn(tertiary),
            tertiaryContainer      = Color.hsl(tertiaryH, tertiaryS.coerceAtMost(0.40f), 0.88f),
            onTertiaryContainer    = Color.hsl(tertiaryH, tertiaryS, 0.12f),
            error                  = Color(0xFFBA1A1A),
            errorContainer         = Color(0xFFFFDAD6),
            onError                = Color(0xFFFFFFFF),
            onErrorContainer       = Color(0xFF410002),
            background             = background,
            onBackground           = onBackground,
            surface                = surface,
            onSurface              = onSurface,
            surfaceVariant         = surfaceVariant,
            onSurfaceVariant       = onSurfaceVariant,
            surfaceContainerLowest  = surfaceContainerLowest,
            surfaceContainerLow     = surfaceContainerLow,
            surfaceContainer        = surfaceContainer,
            surfaceContainerHigh    = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceBright          = surfaceBright,
            surfaceDim             = surfaceDim,
            outline                = outline,
            inverseOnSurface       = inverseOnSurface,
            inverseSurface         = inverseSurface,
            inversePrimary         = Color.hsl(primaryH, primaryS.coerceAtMost(0.55f), 0.75f),
            surfaceTint            = primary,
            outlineVariant         = outlineVariant,
            scrim                  = Color(0xFF000000),
        )
    } else {
        darkColorScheme(
            primary                = primary,
            onPrimary              = contrastingOn(primary),
            primaryContainer       = Color.hsl(primaryH, primaryS.coerceAtMost(0.40f), 0.22f),
            onPrimaryContainer     = Color.hsl(primaryH, primaryS.coerceAtMost(0.60f), 0.90f),
            secondary              = secondary,
            onSecondary            = contrastingOn(secondary),
            secondaryContainer     = Color.hsl(secondaryH, secondaryS.coerceAtMost(0.25f), 0.22f),
            onSecondaryContainer   = Color.hsl(secondaryH, secondaryS.coerceAtMost(0.40f), 0.88f),
            tertiary               = tertiary,
            onTertiary             = contrastingOn(tertiary),
            tertiaryContainer      = Color.hsl(tertiaryH, tertiaryS.coerceAtMost(0.25f), 0.22f),
            onTertiaryContainer    = Color.hsl(tertiaryH, tertiaryS.coerceAtMost(0.40f), 0.88f),
            error                  = Color(0xFFFFB4AB),
            errorContainer         = Color(0xFF93000A),
            onError                = Color(0xFF690005),
            onErrorContainer       = Color(0xFFFFDAD6),
            background             = background,
            onBackground           = onBackground,
            surface                = surface,
            onSurface              = onSurface,
            surfaceVariant         = surfaceVariant,
            onSurfaceVariant       = onSurfaceVariant,
            surfaceContainerLowest  = surfaceContainerLowest,
            surfaceContainerLow     = surfaceContainerLow,
            surfaceContainer        = surfaceContainer,
            surfaceContainerHigh    = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceBright          = surfaceBright,
            surfaceDim             = surfaceDim,
            outline                = outline,
            inverseOnSurface       = inverseOnSurface,
            inverseSurface         = inverseSurface,
            inversePrimary         = Color.hsl(primaryH, primaryS, 0.30f),
            surfaceTint            = primary,
            outlineVariant         = outlineVariant,
            scrim                  = Color(0xFF000000),
        )
    }
}
