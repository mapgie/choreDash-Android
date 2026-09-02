package com.mapgie.dash.data.model

/**
 * The three tones one accent colour needs on a surface: the solid [spineArgb]
 * for card spines and icon strokes, the [textArgb] that reads on both the tint
 * and the surface, and the pale/dim [tintArgb] behind badges and icon chips.
 */
data class SwatchTones(
    val spineArgb: Long,
    val textArgb: Long,
    val tintArgb: Long,
)

/**
 * The seven-colour palette the user picks from in Settings › Colours (severity
 * tints) and Settings › Categories (a category's own colour). Plain Kotlin so
 * the persisted name, the settings screens and the unit tests all share it.
 *
 * Each swatch carries a hand-tuned set of tones per brightness, matching the
 * Cozy Cream / Zen Dark handoff. The dark text tones are the design's; the
 * light text tones are deepened so every text-on-tint pair clears 4.5:1
 * (`SwatchContrastTest` pins that).
 */
enum class Swatch(
    val displayName: String,
    val light: SwatchTones,
    val dark: SwatchTones,
) {
    ROSE(
        "Rose",
        light = SwatchTones(spineArgb = 0xFFB8524EL, textArgb = 0xFFA03E3AL, tintArgb = 0xFFF6E3E1L),
        dark = SwatchTones(spineArgb = 0xFFD9615CL, textArgb = 0xFFE8938DL, tintArgb = 0xFF4A2F2EL),
    ),
    GOLD(
        "Gold",
        light = SwatchTones(spineArgb = 0xFFC99A4AL, textArgb = 0xFF7D5A1AL, tintArgb = 0xFFF3E6CCL),
        dark = SwatchTones(spineArgb = 0xFFC99A4AL, textArgb = 0xFFD9B26AL, tintArgb = 0xFF3D3324L),
    ),
    AMBER(
        "Amber",
        light = SwatchTones(spineArgb = 0xFFD9A648L, textArgb = 0xFF7F5B16L, tintArgb = 0xFFF3E8D2L),
        dark = SwatchTones(spineArgb = 0xFFDCB85FL, textArgb = 0xFFDCB85FL, tintArgb = 0xFF3D3624L),
    ),
    SAGE(
        "Sage",
        light = SwatchTones(spineArgb = 0xFF8AA877L, textArgb = 0xFF526E46L, tintArgb = 0xFFE7ECDDL),
        dark = SwatchTones(spineArgb = 0xFF8AA877L, textArgb = 0xFFA6C391L, tintArgb = 0xFF3A4634L),
    ),
    BLUE(
        "Blue",
        light = SwatchTones(spineArgb = 0xFF7F9BB3L, textArgb = 0xFF46627AL, tintArgb = 0xFFE0E8EFL),
        dark = SwatchTones(spineArgb = 0xFF7F9BB3L, textArgb = 0xFFA9B8D6L, tintArgb = 0xFF3B3E4AL),
    ),
    LAVENDER(
        "Lavender",
        light = SwatchTones(spineArgb = 0xFF7A5FA0L, textArgb = 0xFF5F4A7AL, tintArgb = 0xFFE9E0F2L),
        dark = SwatchTones(spineArgb = 0xFFB6A3D6L, textArgb = 0xFFC9B8E6L, tintArgb = 0xFF3B3448L),
    ),
    PEACH(
        "Peach",
        light = SwatchTones(spineArgb = 0xFFE0B28DL, textArgb = 0xFF8A562AL, tintArgb = 0xFFF6E8DCL),
        dark = SwatchTones(spineArgb = 0xFFE0B28DL, textArgb = 0xFFE0B28DL, tintArgb = 0xFF4A3A2EL),
    );

    fun tones(dark: Boolean): SwatchTones = if (dark) this.dark else light

    companion object {
        /** The six swatches offered for severity colours (Settings › Colours). */
        val severityPalette: List<Swatch> = listOf(ROSE, GOLD, AMBER, SAGE, BLUE, LAVENDER)

        /** All seven swatches, offered for category colours (Settings › Categories). */
        val categoryPalette: List<Swatch> = entries.toList()

        fun fromName(name: String?): Swatch? = name?.let { n -> entries.firstOrNull { it.name == n } }
    }
}

/**
 * Relative luminance of an ARGB colour per WCAG 2.x, in plain Kotlin so the
 * palette can be checked in JVM unit tests without Compose.
 */
fun relativeLuminance(argb: Long): Double {
    fun channel(shift: Int): Double {
        val c = ((argb shr shift) and 0xFF).toDouble() / 255.0
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

/** WCAG contrast ratio between two ARGB colours (1.0 to 21.0). */
fun contrastRatio(a: Long, b: Long): Double {
    val la = relativeLuminance(a) + 0.05
    val lb = relativeLuminance(b) + 0.05
    return if (la > lb) la / lb else lb / la
}
