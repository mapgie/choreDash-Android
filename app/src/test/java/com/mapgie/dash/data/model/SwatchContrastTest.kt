package com.mapgie.dash.data.model

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The palette's contrast floor: every text tone reads at 4.5:1 or better on its
 * own tint and on the card and page grounds of its brightness, in both Cozy
 * Cream and Zen Dark. Guards the hand-tuned hex values in [Swatch] so a colour
 * tweak that breaks legibility fails here rather than on a phone.
 */
class SwatchContrastTest {

    private companion object {
        const val LIGHT_CARD = 0xFFFFFDF9L
        const val LIGHT_GROUND = 0xFFF2EDE2L
        const val DARK_CARD = 0xFF2F372BL
        const val DARK_GROUND = 0xFF22281FL
        const val FLOOR = 4.5
    }

    private fun assertReads(name: String, text: Long, on: Long, surface: String) {
        val ratio = contrastRatio(text, on)
        assertTrue("$name text on $surface is ${"%.2f".format(ratio)}:1, below $FLOOR:1", ratio >= FLOOR)
    }

    @Test
    fun `every light text tone reads on its tint, the card and the ground`() {
        Swatch.entries.forEach { swatch ->
            val t = swatch.light
            assertReads("${swatch.name} light", t.textArgb, t.tintArgb, "tint")
            assertReads("${swatch.name} light", t.textArgb, LIGHT_CARD, "card")
            assertReads("${swatch.name} light", t.textArgb, LIGHT_GROUND, "ground")
        }
    }

    @Test
    fun `every dark text tone reads on its tint, the card and the ground`() {
        Swatch.entries.forEach { swatch ->
            val t = swatch.dark
            assertReads("${swatch.name} dark", t.textArgb, t.tintArgb, "tint")
            assertReads("${swatch.name} dark", t.textArgb, DARK_CARD, "card")
            assertReads("${swatch.name} dark", t.textArgb, DARK_GROUND, "ground")
        }
    }

    @Test
    fun `severity defaults are the design's rose, amber and sage`() {
        assertTrue(Severity.OVERDUE.defaultSwatch == Swatch.ROSE)
        assertTrue(Severity.DUE_SOON.defaultSwatch == Swatch.AMBER)
        assertTrue(Severity.FRESH.defaultSwatch == Swatch.SAGE)
    }

    @Test
    fun `contrast ratio is symmetric and spans 1 to 21`() {
        assertTrue(contrastRatio(0xFF000000L, 0xFFFFFFFFL) > 20.9)
        assertTrue(contrastRatio(0xFFFFFFFFL, 0xFF000000L) > 20.9)
        assertTrue(contrastRatio(0xFF808080L, 0xFF808080L) == 1.0)
    }
}
