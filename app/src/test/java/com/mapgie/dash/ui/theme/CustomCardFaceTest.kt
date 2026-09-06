package com.mapgie.dash.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The custom card face colour is what carries a custom scheme onto the list cards
 * and into Task Zen Mode (whose surface is surfaceContainerLow). Pure colour maths,
 * no Android, like WcagContrastTest.
 */
class CustomCardFaceTest {

    private fun scheme(cardArgb: Int) = buildCustomColorScheme(
        primaryH = 150f, primaryS = 0.5f, primaryL = 0.4f,
        secondaryH = 120f, secondaryS = 0.4f, secondaryL = 0.4f,
        tertiaryH = 200f, tertiaryS = 0.4f, tertiaryL = 0.4f,
        darkTheme = false,
        backgroundArgb = 0,
        cardFaceArgb = cardArgb,
    )

    @Test
    fun `a picked card face becomes both the card and the zen surface`() {
        val custom = scheme(Color(0xFFCC8844).toArgb())
        // The normal card face (surfaceVariant) and the zen surface
        // (surfaceContainerLow) are the same exact picked face.
        assertEquals(custom.surfaceContainerLow, custom.surfaceVariant)
    }

    @Test
    fun `no card face keeps the neutral surface, so a pick actually changes it`() {
        val picked = scheme(Color(0xFFCC8844).toArgb()).surfaceVariant
        val neutral = scheme(0).surfaceVariant
        assertNotEquals(neutral, picked)
    }
}
