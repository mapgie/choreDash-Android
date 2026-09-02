package com.mapgie.dash.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What Settings › Categories guarantees: names match regardless of case and
 * spacing, General is always last and can never be deleted or reordered, and
 * unstyled categories still get a sensible default icon.
 */
class CategoryCatalogTest {

    @Test
    fun `styles match case-insensitively and ignore surrounding spaces`() {
        val catalog = CategoryCatalog().withStyle("Kitchen", CategoryStyle(icon = "UTENSILS", swatch = "GOLD"))
        assertEquals(CategoryIcon.UTENSILS, catalog.iconFor(" kitchen "))
        assertEquals(Swatch.GOLD, catalog.swatchFor("KITCHEN"))
    }

    @Test
    fun `unstyled categories get a keyword default icon and no colour`() {
        val catalog = CategoryCatalog()
        assertEquals(CategoryIcon.WASHING_MACHINE, catalog.iconFor("Laundry"))
        assertEquals(CategoryIcon.CAR, catalog.iconFor("Car"))
        assertEquals(CategoryIcon.PRINTER, catalog.iconFor("Something else"))
        assertNull(catalog.swatchFor("Laundry"))
    }

    @Test
    fun `general is always listed last and cannot be added or reordered`() {
        val catalog = CategoryCatalog().added("General").added("Car").withOrder(listOf("General", "Car", "Plants"))
        assertEquals(listOf("Car", "Plants"), catalog.order)
        assertEquals(listOf("Car", "Plants", "Kitchen", GENERAL_CATEGORY), catalog.allCategories(listOf("Kitchen", "General")))
    }

    @Test
    fun `unlisted categories rank after the user order, alphabetically`() {
        val catalog = CategoryCatalog(order = listOf("Plants", "Car"))
        assertEquals(listOf("Plants", "Car", "Admin", "Kitchen"), catalog.sorted(listOf("Kitchen", "Admin", "Car", "Plants")))
    }

    @Test
    fun `rename carries the style and the position across`() {
        val catalog = CategoryCatalog(order = listOf("Car", "Plants"))
            .withStyle("Car", CategoryStyle(icon = "CAR", swatch = "PEACH"))
            .renamed("Car", "Vehicles")
        assertEquals(listOf("Vehicles", "Plants"), catalog.order)
        assertEquals(Swatch.PEACH, catalog.swatchFor("Vehicles"))
        assertNull(catalog.swatchFor("Car"))
    }

    @Test
    fun `delete removes the style and the position`() {
        val catalog = CategoryCatalog(order = listOf("Car", "Plants"))
            .withStyle("Car", CategoryStyle(swatch = "PEACH"))
            .without("car")
        assertEquals(listOf("Plants"), catalog.order)
        assertNull(catalog.swatchFor("Car"))
    }

    @Test
    fun `effective colour is the pick or a stable fallback per name`() {
        val catalog = CategoryCatalog().withStyle("Car", CategoryStyle(swatch = "BLUE"))
        assertEquals(Swatch.BLUE, catalog.effectiveSwatch("Car"))
        assertEquals(catalog.effectiveSwatch("Plants"), catalog.effectiveSwatch("plants"))
    }
}
