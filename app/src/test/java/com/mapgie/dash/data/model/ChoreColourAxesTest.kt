package com.mapgie.dash.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Settings › Colours' two axes: what the spine+badge and the icon wear under
 * each choice, the defaults a fresh install gets, and how a pre-9a single
 * "colour chores by" choice carries over to both axes.
 */
class ChoreColourAxesTest {

    private val house = Swatch.BLUE

    @Test
    fun `fresh install colours the spine by severity and the icon by category`() {
        val axes = ChoreColourAxes.fromStored(legacy = null, spine = null, icon = null)
        assertEquals(ColourChoresBy.SEVERITY, axes.spineAndBadge)
        assertEquals(ColourChoresBy.CATEGORY, axes.icon)
        assertNull(axes.spineSwatch(house))
        assertEquals(house, axes.iconSwatch(house))
    }

    @Test
    fun `an old single choice applies to both axes until either is changed`() {
        val category = ChoreColourAxes.fromStored(legacy = "CATEGORY", spine = null, icon = null)
        assertEquals(ColourChoresBy.CATEGORY, category.spineAndBadge)
        assertEquals(ColourChoresBy.CATEGORY, category.icon)

        val severity = ChoreColourAxes.fromStored(legacy = "SEVERITY", spine = null, icon = null)
        assertEquals(ColourChoresBy.SEVERITY, severity.spineAndBadge)
        assertEquals(ColourChoresBy.SEVERITY, severity.icon)
    }

    @Test
    fun `a new per-axis choice wins over the old single choice`() {
        val axes = ChoreColourAxes.fromStored(legacy = "CATEGORY", spine = "SEVERITY", icon = null)
        assertEquals(ColourChoresBy.SEVERITY, axes.spineAndBadge)
        assertEquals(ColourChoresBy.CATEGORY, axes.icon)
    }

    @Test
    fun `unknown stored names fall back to the defaults`() {
        val axes = ChoreColourAxes.fromStored(legacy = "rainbow", spine = "bogus", icon = "bogus")
        assertEquals(ColourChoresBy.SEVERITY, axes.spineAndBadge)
        assertEquals(ColourChoresBy.CATEGORY, axes.icon)
    }

    @Test
    fun `the axes can mix freely`() {
        val mixed = ChoreColourAxes(spineAndBadge = ColourChoresBy.CATEGORY, icon = ColourChoresBy.SEVERITY)
        assertEquals(house, mixed.spineSwatch(house))
        assertNull(mixed.iconSwatch(house))
        assertEquals("spine category · icon severity", mixed.caption)
        assertEquals("spine severity · icon category", ChoreColourAxes().caption)
    }
}
