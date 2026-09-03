package com.mapgie.dash.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The handful of Cozy Cream / Zen Dark colours that have no Material 3 role to
 * live in: the bottom bar's own ground and inactive tint, the faint card meta
 * text, section counts, the outlined sort pill, the header NFC button, the tag
 * label gold, the sheet's inner blocks and its scrim.
 *
 * The Cream palette gets the design's exact values per brightness; every other
 * palette (built-in or custom) derives them from its [ColorScheme] so the same
 * components stay on-palette there (see LESSONS.md #30 for why this is a
 * CompositionLocal and not a set of top-level constants).
 */
data class DashTokens(
    /** Bottom bar ground: one step lighter than the page in light, darker in dark. */
    val navBackground: Color,
    /** Inactive tab icon and label. */
    val navInactive: Color,
    /** Card meta line ("every 30d · done 7w ago") and other faint captions. */
    val inkFaint: Color,
    /** The count at the right of a section header. */
    val sectionCount: Color,
    /** Border of the outlined sort pill and the Cancel button. */
    val pillOutline: Color,
    /** Tinted round NFC scan button beside the Chores title. */
    val nfcButtonContainer: Color,
    val nfcButtonContent: Color,
    /** The chore's NFC tag label under the sheet meta line. */
    val tagLabel: Color,
    /** Scrim behind sheets and the open speed dial. */
    val scrim: Color,
    /** Inner grouped block inside a sheet (settings card, notes, history). */
    val sheetBlock: Color,
    /** Outline around a [sheetBlock]; transparent in Zen Dark, a hairline in Cozy Cream. */
    val sheetBlockOutline: Color,
    /** Hairline between rows inside a [sheetBlock]. */
    val sheetDivider: Color,
    /** Sheet drag handle. */
    val handle: Color,
) {
    /**
     * With the WCAG toggle on: every token used as text (faint captions, section
     * counts, inactive tab labels, the tag gold, the NFC button glyph) is lifted
     * to 7:1 on the ground it sits on, and the outlined pill's border to 3:1.
     * Faint ink and the section count adopt the scheme's (already lifted) muted
     * ink outright, matching the handoff's contrast rule. Hairlines, the drag
     * handle and the scrim are decorative and stay as designed.
     */
    fun withWcagContrast(scheme: ColorScheme, dark: Boolean): DashTokens {
        val darken = !dark
        val ground = worstGround(scheme.grounds(), dark)
        return copy(
            navInactive = navInactive.adjustedForContrast(navBackground, WCAG_TEXT_RATIO, darken),
            inkFaint = scheme.onSurfaceVariant,
            sectionCount = scheme.onSurfaceVariant,
            pillOutline = pillOutline.adjustedForContrast(ground, WCAG_UI_RATIO, darken),
            nfcButtonContent = nfcButtonContent.adjustedForContrast(nfcButtonContainer, WCAG_TEXT_RATIO, darken),
            tagLabel = tagLabel.adjustedForContrast(worstGround(listOf(ground, sheetBlock), dark), WCAG_TEXT_RATIO, darken),
        )
    }
}

val CreamLightTokens = DashTokens(
    navBackground = Color(0xFFF7F2E7),
    navInactive = Color(0xFF78705D),
    inkFaint = Color(0xFF7F7560),
    sectionCount = Color(0xFF9B917E),
    pillOutline = Color(0xFFDDD3C1),
    nfcButtonContainer = Color(0xFFDFE8D3),
    nfcButtonContent = Color(0xFF5F7D52),
    tagLabel = Color(0xFF9A6E1C),
    scrim = Color(0x66332F2A),
    sheetBlock = Color(0xFFFFFDF9),
    sheetBlockOutline = Color(0xFFDDD3C1),
    sheetDivider = Color(0xFFECE4D3),
    handle = Color(0xFFDDD3C1),
)

val ZenDarkTokens = DashTokens(
    navBackground = Color(0xFF1B2019),
    navInactive = Color(0xFFB4BBA9),
    inkFaint = Color(0xFF9AA295),
    sectionCount = Color(0xFF7B8474),
    pillOutline = Color(0xFF45503F),
    nfcButtonContainer = Color(0xFF3A4634),
    nfcButtonContent = Color(0xFFDFCF90),
    tagLabel = Color(0xFFDFCF90),
    scrim = Color(0x9E0F120D),
    sheetBlock = Color(0xFF1B2019),
    sheetBlockOutline = Color.Transparent,
    sheetDivider = Color(0xFF2F372B),
    handle = Color(0xFF4D574A),
)

/** Tokens for palettes that are not Cream, derived from the scheme's own roles. */
fun derivedTokens(scheme: ColorScheme, dark: Boolean): DashTokens = DashTokens(
    navBackground = if (dark) scheme.surfaceContainerLowest else scheme.surfaceContainerLow,
    navInactive = scheme.onSurfaceVariant,
    inkFaint = scheme.outline,
    sectionCount = scheme.outlineVariant,
    pillOutline = scheme.outlineVariant,
    nfcButtonContainer = scheme.secondaryContainer,
    nfcButtonContent = scheme.onSecondaryContainer,
    tagLabel = scheme.tertiary,
    scrim = if (dark) Color(0x9E000000) else Color(0x59000000),
    sheetBlock = if (dark) scheme.surfaceContainerLowest else scheme.surfaceContainerLowest,
    sheetBlockOutline = if (dark) Color.Transparent else scheme.outlineVariant,
    sheetDivider = scheme.outlineVariant,
    handle = scheme.outlineVariant,
)

/** Defaults to the Cream light set; [DashTheme] provides the right set per palette and brightness. */
val LocalDashTokens = staticCompositionLocalOf { CreamLightTokens }
