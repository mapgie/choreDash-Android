package com.mapgie.dash.data.model

/**
 * The Lucide glyphs a category can wear on its list cards. One icon per
 * category, never the same glyph on every row. The enum name is what gets
 * persisted; the drawable lives in `ui/theme/LucideIcons.kt` and is looked up
 * from the UI layer so this stays plain Kotlin.
 */
enum class CategoryIcon(val label: String) {
    DROPLET("Water"),
    HOME("Home"),
    CAR("Car"),
    WASHING_MACHINE("Laundry"),
    SPROUT("Plants"),
    UTENSILS("Kitchen"),
    BATH("Bathroom"),
    TREE_PINE("Outdoor"),
    ZAP("Admin"),
    PILL("Health"),
    PRINTER("General"),
    PLANE("Holiday"),
    SHIELD("Insurance"),
    CALENDAR("Appointment"),
    BRUSH("Cleaning"),
    LEAF("Leaf"),
    LAMP("Lamp"),
    CIRCLE_ALERT("Other");

    companion object {
        /** The subset offered in the Settings › Categories icon picker, in picker order. */
        val pickerSet: List<CategoryIcon> = listOf(
            WASHING_MACHINE, BRUSH, HOME, DROPLET, SPROUT, UTENSILS, BATH, TREE_PINE, LEAF, LAMP,
            CAR, ZAP, PILL, PRINTER, PLANE, SHIELD, CALENDAR,
        )

        fun fromName(name: String?): CategoryIcon? = name?.let { n -> entries.firstOrNull { it.name == n } }

        /**
         * Default glyph for a category the user has not styled yet, chosen by
         * keyword so common household categories get a sensible icon out of the
         * box. Anything unrecognised gets the General printer glyph.
         */
        fun defaultFor(category: String?): CategoryIcon {
            val key = category?.trim()?.lowercase() ?: return PRINTER
            fun has(vararg words: String) = words.any { key.contains(it) }
            return when {
                has("laundry", "wash", "cloth", "linen", "towel") -> WASHING_MACHINE
                has("car", "vehicle", "bike", "garage") -> CAR
                has("plant", "garden", "flower") -> SPROUT
                has("kitchen", "cook", "food", "fridge", "dish") -> UTENSILS
                has("bath", "toilet", "shower") -> BATH
                has("outdoor", "outside", "yard", "lawn", "patio") -> TREE_PINE
                has("admin", "bill", "finance", "tax", "paper") -> ZAP
                has("health", "med", "pharm", "doctor", "errand") -> PILL
                has("holiday", "travel", "trip", "vacation") -> PLANE
                has("insur") -> SHIELD
                has("clean", "dust", "vacuum", "hoover", "mop") -> BRUSH
                has("water", "filter", "softener", "boiler") -> DROPLET
                has("house", "home", "flat", "apartment") -> HOME
                else -> PRINTER
            }
        }
    }
}
