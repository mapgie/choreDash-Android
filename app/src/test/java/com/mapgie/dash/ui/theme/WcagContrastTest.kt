package com.mapgie.dash.ui.theme

import androidx.compose.ui.graphics.Color
import com.mapgie.dash.data.model.Swatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the "WCAG accessible colours" toggle guarantees, for every built-in
 * palette in both brightnesses: every colour the screens draw text with reads
 * at 7:1 (AAA) on every ground it can sit on, and non-text UI outlines at 3:1.
 * The transform is derived, not hand-tuned, so this is the spec that keeps a
 * palette tweak from quietly undoing it.
 */
class WcagContrastTest {

    private companion object {
        const val TEXT_FLOOR = 7f
        const val LABEL_ON_ACCENT_FLOOR = 4.5f
        const val UI_FLOOR = 3f
        // Cozy Cream / Zen Dark page grounds, the surfaces status badges sit on.
        val LIGHT_GROUND = Color(0xFFF2EDE2)
        val DARK_GROUND = Color(0xFF22281F)
    }

    private data class Case(val theme: AppTheme, val dark: Boolean) {
        val label get() = "${theme.name} ${if (dark) "dark" else "light"}"
        val scheme get() = colorSchemeFor(theme, dark, wcag = true)
        val tokens get() = when {
            theme == AppTheme.CREAM && dark -> ZenDarkTokens
            theme == AppTheme.CREAM -> CreamLightTokens
            else -> derivedTokens(scheme, dark)
        }.withWcagContrast(scheme, dark)
    }

    private val cases = AppTheme.entries
        .filter { it != AppTheme.CUSTOM }
        .flatMap { listOf(Case(it, dark = false), Case(it, dark = true)) }

    private fun assertReads(what: String, text: Color, on: Color, floor: Float) {
        val ratio = contrastRatio(text, on)
        assertTrue("$what is ${"%.2f".format(ratio)}:1, below $floor:1", ratio >= floor)
    }

    @Test
    fun `wcag mode lifts every text role to 7 to 1 on every ground`() {
        cases.forEach { case ->
            val s = case.scheme
            val textRoles = listOf(
                "onBackground" to s.onBackground,
                "onSurface" to s.onSurface,
                "onSurfaceVariant" to s.onSurfaceVariant,
                "primary" to s.primary,
                "secondary" to s.secondary,
                "tertiary" to s.tertiary,
                "error" to s.error,
            )
            textRoles.forEach { (role, colour) ->
                s.grounds().forEachIndexed { i, ground ->
                    assertReads("${case.label} $role on ground #$i", colour, ground, TEXT_FLOOR)
                }
            }
        }
    }

    @Test
    fun `wcag mode lifts on-container text to 7 to 1 on its container`() {
        cases.forEach { case ->
            val s = case.scheme
            assertReads("${case.label} onPrimaryContainer", s.onPrimaryContainer, s.primaryContainer, TEXT_FLOOR)
            assertReads("${case.label} onSecondaryContainer", s.onSecondaryContainer, s.secondaryContainer, TEXT_FLOOR)
            assertReads("${case.label} onTertiaryContainer", s.onTertiaryContainer, s.tertiaryContainer, TEXT_FLOOR)
            assertReads("${case.label} onErrorContainer", s.onErrorContainer, s.errorContainer, TEXT_FLOOR)
        }
    }

    @Test
    fun `wcag mode keeps button labels readable on the lifted accents`() {
        cases.forEach { case ->
            val s = case.scheme
            assertReads("${case.label} onPrimary", s.onPrimary, s.primary, LABEL_ON_ACCENT_FLOOR)
            assertReads("${case.label} onSecondary", s.onSecondary, s.secondary, LABEL_ON_ACCENT_FLOOR)
            assertReads("${case.label} onTertiary", s.onTertiary, s.tertiary, LABEL_ON_ACCENT_FLOOR)
        }
    }

    @Test
    fun `wcag mode lifts the outline to 4 and a half to 1 on every ground`() {
        cases.forEach { case ->
            val s = case.scheme
            s.grounds().forEachIndexed { i, ground ->
                assertReads("${case.label} outline on ground #$i", s.outline, ground, LABEL_ON_ACCENT_FLOOR)
            }
        }
    }

    @Test
    fun `wcag mode lifts the design tokens used as text`() {
        cases.forEach { case ->
            val s = case.scheme
            val t = case.tokens
            assertReads("${case.label} navInactive", t.navInactive, t.navBackground, TEXT_FLOOR)
            assertReads("${case.label} nfcButtonContent", t.nfcButtonContent, t.nfcButtonContainer, TEXT_FLOOR)
            assertReads("${case.label} tagLabel on sheet block", t.tagLabel, t.sheetBlock, TEXT_FLOOR)
            s.grounds().forEachIndexed { i, ground ->
                assertReads("${case.label} inkFaint on ground #$i", t.inkFaint, ground, TEXT_FLOOR)
                assertReads("${case.label} sectionCount on ground #$i", t.sectionCount, ground, TEXT_FLOOR)
                assertReads("${case.label} tagLabel on ground #$i", t.tagLabel, ground, TEXT_FLOOR)
                assertReads("${case.label} pillOutline on ground #$i", t.pillOutline, ground, UI_FLOOR)
            }
        }
    }

    @Test
    fun `wcag mode lifts the content-type accents to 7 to 1`() {
        listOf(false to LightTypeAccents, true to DarkTypeAccents).forEach { (dark, base) ->
            val a = base.withWcagContrast(dark)
            val label = if (dark) "dark" else "light"
            assertReads("$label onTaskContainer", a.onTaskContainer, a.taskContainer, TEXT_FLOOR)
            assertReads("$label onChoreContainer", a.onChoreContainer, a.choreContainer, TEXT_FLOOR)
            assertReads("$label onReminderContainer", a.onReminderContainer, a.reminderContainer, TEXT_FLOOR)
        }
    }

    @Test
    fun `wcag mode lifts every swatch text tone to 7 to 1 on its tint and the page`() {
        Swatch.entries.forEach { swatch ->
            listOf(false to LIGHT_GROUND, true to DARK_GROUND).forEach { (dark, ground) ->
                val tones = if (dark) swatch.dark else swatch.light
                val tint = Color(tones.tintArgb)
                val text = wcagSwatchText(Color(tones.textArgb), tint, ground, dark)
                val label = "${swatch.name} ${if (dark) "dark" else "light"}"
                assertReads("$label text on tint", text, tint, TEXT_FLOOR)
                assertReads("$label text on page", text, ground, TEXT_FLOOR)
            }
        }
    }

    @Test
    fun `wcag mode leaves a colour that already passes untouched`() {
        assertEquals(Color.Black, Color.Black.adjustedForContrast(Color.White, TEXT_FLOOR, darken = true))
        assertEquals(Color.White, Color.White.adjustedForContrast(Color.Black, TEXT_FLOOR, darken = false))
    }

    @Test
    fun `wcag mode only nudges lightness, never hue`() {
        val lifted = Color(0xFF7A5FA0).adjustedForContrast(Color(0xFFF2EDE2), TEXT_FLOOR, darken = true)
        assertEquals(Color(0xFF7A5FA0).toHsl()[0], lifted.toHsl()[0], 1f)
        assertTrue(lifted.toHsl()[2] < Color(0xFF7A5FA0).toHsl()[2])
    }

    @Test
    fun `with the toggle off the designed palettes are left as they are`() {
        cases.forEach { case ->
            val designed = colorSchemeFor(case.theme, case.dark)
            val off = colorSchemeFor(case.theme, case.dark, wcag = false)
            assertEquals(case.label, designed.primary, off.primary)
            assertEquals(case.label, designed.onSurfaceVariant, off.onSurfaceVariant)
        }
        // And the toggle is not a no-op: Cream's lavender sits under 7:1 on the page.
        assertNotEquals(colorSchemeFor(AppTheme.CREAM, false).primary, colorSchemeFor(AppTheme.CREAM, false, wcag = true).primary)
        assertNotEquals(colorSchemeFor(AppTheme.CREAM, true).primary, colorSchemeFor(AppTheme.CREAM, true, wcag = true).primary)
    }
}
