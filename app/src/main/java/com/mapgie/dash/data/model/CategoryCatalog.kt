package com.mapgie.dash.data.model

import kotlinx.serialization.Serializable

/** The default category. Cannot be deleted or reordered; always listed last. */
const val GENERAL_CATEGORY = "General"

/** A user's styling for one category: which glyph and which colour it wears. */
@Serializable
data class CategoryStyle(
    val icon: String? = null,
    val swatch: String? = null,
) {
    val iconEnum: CategoryIcon? get() = CategoryIcon.fromName(icon)
    val swatchEnum: Swatch? get() = Swatch.fromName(swatch)
}

/**
 * Everything Settings › Categories manages, stored on this device: the order
 * categories appear in grouped lists, per-category icon and colour, and
 * categories the user created before any chore or task uses them.
 *
 * Categories themselves are still just strings on chores and tasks in
 * Supabase; this catalog decorates them. Names are matched case-insensitively
 * and trimmed so "kitchen" and "Kitchen " style the same group.
 */
@Serializable
data class CategoryCatalog(
    val order: List<String> = emptyList(),
    val styles: Map<String, CategoryStyle> = emptyMap(),
) {
    private fun key(name: String) = name.trim().lowercase()

    fun styleFor(name: String?): CategoryStyle =
        name?.let { n -> styles.entries.firstOrNull { key(it.key) == key(n) }?.value } ?: CategoryStyle()

    fun iconFor(name: String?): CategoryIcon =
        styleFor(name).iconEnum ?: CategoryIcon.defaultFor(name)

    fun swatchFor(name: String?): Swatch? = styleFor(name).swatchEnum

    /**
     * The colour a category wears in "colour chores by category" mode: the
     * user's pick, or a stable fallback hashed from the name so unstyled
     * categories still differ from one another.
     */
    fun effectiveSwatch(name: String?): Swatch {
        swatchFor(name)?.let { return it }
        val palette = Swatch.categoryPalette
        if (name.isNullOrBlank()) return Swatch.SAGE
        var hash = 0
        for (ch in key(name)) hash = hash * 31 + ch.code
        return palette[((hash % palette.size) + palette.size) % palette.size]
    }

    /**
     * Rank used when ordering grouped sections: user order first, then anything
     * unlisted alphabetically, with [GENERAL_CATEGORY] pinned last.
     */
    fun rankOf(name: String?): Int {
        if (name == null || key(name) == key(GENERAL_CATEGORY)) return Int.MAX_VALUE
        val idx = order.indexOfFirst { key(it) == key(name) }
        return if (idx >= 0) idx else order.size
    }

    /** Sorts [names] by [rankOf] then name so the list is stable. */
    fun sorted(names: Collection<String>): List<String> =
        names.distinctBy { key(it) }.sortedWith(compareBy<String> { rankOf(it) }.thenBy { it.lowercase() })

    /**
     * All categories to show in Settings › Categories: the ones in use (from
     * chores and tasks) plus any created but unused, in catalog order, General last.
     */
    fun allCategories(inUse: Collection<String>): List<String> {
        val known = (order + styles.keys + inUse).filter { it.isNotBlank() && key(it) != key(GENERAL_CATEGORY) }
        return sorted(known) + GENERAL_CATEGORY
    }

    fun withStyle(name: String, style: CategoryStyle): CategoryCatalog {
        val cleaned = styles.filterKeys { key(it) != key(name) }
        return copy(styles = cleaned + (name.trim() to style))
    }

    fun withOrder(newOrder: List<String>): CategoryCatalog =
        copy(order = newOrder.map { it.trim() }.filter { it.isNotBlank() && key(it) != key(GENERAL_CATEGORY) }.distinctBy { key(it) })

    fun renamed(from: String, to: String): CategoryCatalog {
        val style = styleFor(from)
        val cleaned = styles.filterKeys { key(it) != key(from) }
        val newOrder = order.map { if (key(it) == key(from)) to.trim() else it }
        val nextStyles = if (style == CategoryStyle()) cleaned else cleaned + (to.trim() to style)
        return copy(order = newOrder, styles = nextStyles)
    }

    fun without(name: String): CategoryCatalog = copy(
        order = order.filter { key(it) != key(name) },
        styles = styles.filterKeys { key(it) != key(name) },
    )

    fun added(name: String): CategoryCatalog {
        val trimmed = name.trim()
        if (trimmed.isBlank() || key(trimmed) == key(GENERAL_CATEGORY)) return this
        if (order.any { key(it) == key(trimmed) }) return this
        return copy(order = order + trimmed)
    }
}
