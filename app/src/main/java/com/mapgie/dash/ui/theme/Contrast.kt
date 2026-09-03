package com.mapgie.dash.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Pure-Kotlin WCAG contrast maths shared by the colour schemes, the design
// tokens, the content-type accents and the status swatches. Deliberately free
// of android.* calls (no ColorUtils) so WcagContrastTest can run it on the JVM.

/** WCAG 2.x contrast ratio between two opaque colours, 1..21. */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/** Hue 0..360, saturation 0..1, lightness 0..1 of an sRGB colour. */
fun Color.toHsl(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val l = (max + min) / 2f
    if (delta == 0f) return floatArrayOf(0f, 0f, l)
    val s = delta / (1f - kotlin.math.abs(2f * l - 1f))
    var h = when (max) {
        r -> ((g - b) / delta) % 6f
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    } * 60f
    if (h < 0f) h += 360f
    return floatArrayOf(h, s.coerceIn(0f, 1f), l)
}

/**
 * Steps a colour's HSL lightness away from [against] until the WCAG contrast
 * ratio reaches [target]. [darken] chooses the direction (darken on light
 * grounds, lighten on dark ones). A colour that already meets the target is
 * returned unchanged, so the transform is idempotent.
 */
fun Color.adjustedForContrast(against: Color, target: Float, darken: Boolean): Color {
    val hsl = toHsl()
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
 * The ground that text in this palette has the hardest time against: the
 * darkest surface tone of a light palette, the lightest of a dark one. Text
 * lifted to a ratio against this reads at least as well on every other ground.
 */
fun worstGround(grounds: List<Color>, darkTheme: Boolean): Color =
    if (darkTheme) grounds.maxBy { it.luminance() } else grounds.minBy { it.luminance() }
