package com.mapgie.dash.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * How a memo's card resolves its colour and glyph: a standalone memo uses its own
 * pick, a linked memo borrows its chore's or task's category look and follows the
 * user's Settings › Colours axes just like the chore card does.
 */
class ReminderAppearanceTest {

    private fun memo(colour: String? = null, icon: String? = null) =
        ReminderDto(id = "m", subject = "m", remindAt = "2026-07-10T09:00:00Z", colour = colour, icon = icon)

    private val kitchen = CategoryStyle(icon = CategoryIcon.UTENSILS.name, swatch = Swatch.PEACH.name)
    private val catalog = CategoryCatalog(styles = mapOf("Kitchen" to kitchen))

    // Defaults: spine by severity, icon by category.
    private val defaultAxes = ChoreColourAxes()
    private val allCategory = ChoreColourAxes(ColourChoresBy.CATEGORY, ColourChoresBy.CATEGORY)

    @Test
    fun `a standalone memo uses its own picked colour and icon`() {
        val a = ReminderAppearance.of(
            memo(colour = Swatch.LAVENDER.name, icon = CategoryIcon.PLANE.name),
            linkedCategory = null, catalog = catalog, axes = defaultAxes,
        )
        assertEquals(Swatch.LAVENDER, a.spineSwatch)
        assertEquals(Swatch.LAVENDER, a.iconSwatch)
        assertEquals(CategoryIcon.PLANE, a.icon)
    }

    @Test
    fun `a standalone memo with nothing picked falls back to the default bell and ring tone`() {
        val a = ReminderAppearance.of(memo(), linkedCategory = null, catalog = catalog, axes = defaultAxes)
        assertNull(a.spineSwatch)
        assertNull(a.iconSwatch)
        assertNull(a.icon)
    }

    @Test
    fun `a linked memo takes the category glyph and, colouring by category, its swatch`() {
        val a = ReminderAppearance.of(
            memo(colour = Swatch.LAVENDER.name), // own pick is ignored while linked
            linkedCategory = LinkedCategory("Kitchen"), catalog = catalog, axes = allCategory,
        )
        assertEquals(CategoryIcon.UTENSILS, a.icon)
        assertEquals(Swatch.PEACH, a.spineSwatch)
        assertEquals(Swatch.PEACH, a.iconSwatch)
    }

    @Test
    fun `a linked memo colouring by severity keeps the category glyph but no fixed swatch`() {
        val a = ReminderAppearance.of(
            memo(), linkedCategory = LinkedCategory("Kitchen"), catalog = catalog, axes = defaultAxes,
        )
        // Icon axis is category by default, so the glyph is the category's...
        assertEquals(CategoryIcon.UTENSILS, a.icon)
        assertEquals(Swatch.PEACH, a.iconSwatch)
        // ...but the spine axis is severity, so the spine follows the ring tone (null here).
        assertNull(a.spineSwatch)
    }

    @Test
    fun `a linked memo whose chore is uncategorised still inherits, not its own pick`() {
        val a = ReminderAppearance.of(
            memo(icon = CategoryIcon.PLANE.name),
            linkedCategory = LinkedCategory(null), catalog = catalog, axes = allCategory,
        )
        // Not the memo's own PLANE glyph: an unstyled category resolves to a default.
        assertEquals(CategoryIcon.defaultFor(null), a.icon)
        assertEquals(catalog.effectiveSwatch(null), a.spineSwatch)
    }
}
