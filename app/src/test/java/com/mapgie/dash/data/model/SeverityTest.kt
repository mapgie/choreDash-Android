package com.mapgie.dash.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** How a Settings › Colours choice, including "None", survives the round trip through DataStore. */
class SeverityTest {

    @Test
    fun `nothing stored means the design default`() {
        Severity.entries.forEach { assertEquals(it.defaultSwatch, Severity.fromStored(it, null)) }
    }

    @Test
    fun `a chosen swatch round-trips by name`() {
        val stored = Severity.storedName(Swatch.LAVENDER)
        assertEquals(Swatch.LAVENDER, Severity.fromStored(Severity.FRESH, stored))
    }

    @Test
    fun `none round-trips as no swatch`() {
        val stored = Severity.storedName(null)
        assertNull(Severity.fromStored(Severity.OVERDUE, stored))
    }

    @Test
    fun `a swatch name that no longer exists falls back to the default`() {
        assertEquals(Swatch.SAGE, Severity.fromStored(Severity.FRESH, "MAUVE"))
    }

    @Test
    fun `the defaults cover every severity`() {
        assertEquals(Severity.entries.toSet(), Severity.defaults.keys)
        assertEquals(Swatch.BLUE, Severity.defaults[Severity.LOW])
    }
}
