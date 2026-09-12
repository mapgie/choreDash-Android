package com.mapgie.dash.data.model

/**
 * The four severities the user can recolour in Settings › Colours. Maps
 * one-to-one onto the signalling tones of the shared status scale
 * (`StatusTone.CRITICAL` / `ATTENTION` / `OK` / `LOW` in `ui/theme`); kept as
 * its own plain-Kotlin enum so settings persistence never depends on UI code.
 *
 * Chores and memos use the first three. Tasks use all four, because on Tasks
 * priority folds into the same ladder: a high-priority task counts as due soon
 * and a low-priority one with nothing pressing wears [LOW].
 *
 * Any severity can also be set to no colour at all; that is stored as
 * [NO_COLOUR] rather than a swatch name and resolves to a null swatch.
 */
enum class Severity(
    val label: String,
    /** Example badge shown beside the label in Settings › Colours. */
    val sampleBadge: String,
    val defaultSwatch: Swatch,
) {
    OVERDUE("Overdue", "35d over", Swatch.ROSE),
    DUE_SOON("Due soon", "1d left", Swatch.AMBER),
    FRESH("Fresh", "12d left", Swatch.SAGE),

    /** Tasks only: low priority with no pressing due date. */
    LOW("Low priority", "eventually", Swatch.BLUE);

    companion object {
        val defaults: Map<Severity, Swatch?> = entries.associateWith { it.defaultSwatch }

        /** Stored in place of a swatch name when the user picks "None" for a severity. */
        const val NO_COLOUR = "NONE"

        /** The value written to settings for a severity's choice; null is "None". */
        fun storedName(swatch: Swatch?): String = swatch?.name ?: NO_COLOUR

        /**
         * Resolves a stored choice: nothing stored means the design default, [NO_COLOUR]
         * means no colour, and a name that no longer exists falls back to the default.
         */
        fun fromStored(severity: Severity, raw: String?): Swatch? = when (raw) {
            null -> severity.defaultSwatch
            NO_COLOUR -> null
            else -> Swatch.fromName(raw) ?: severity.defaultSwatch
        }
    }
}

/** What drives a chore card's spine, icon chip and badge tint. */
enum class ColourChoresBy(val label: String) {
    /** How overdue the chore is (rose / amber / sage by default). */
    SEVERITY("Severity"),

    /** The chore's category colour; the badge text stays neutral. */
    CATEGORY("Category");

    companion object {
        fun fromName(name: String?): ColourChoresBy =
            entries.firstOrNull { it.name == name } ?: SEVERITY
    }
}

/**
 * Settings › Colours (handoff 9a): two independent axes. The spine and due
 * badge follow one, the round icon chip the other, so the common setup is
 * spine by severity (urgency) with the icon by category (what it is). Neither
 * is forced to follow the other.
 */
data class ChoreColourAxes(
    val spineAndBadge: ColourChoresBy = ColourChoresBy.SEVERITY,
    val icon: ColourChoresBy = ColourChoresBy.CATEGORY,
) {
    /** The category swatch the spine and badge should wear, or null to follow severity. */
    fun spineSwatch(categorySwatch: Swatch): Swatch? =
        if (spineAndBadge == ColourChoresBy.CATEGORY) categorySwatch else null

    /** The category swatch the icon chip should wear, or null to follow severity. */
    fun iconSwatch(categorySwatch: Swatch): Swatch? =
        if (icon == ColourChoresBy.CATEGORY) categorySwatch else null

    /** "spine severity · icon category", the preview caption. */
    val caption: String
        get() = "spine ${spineAndBadge.label.lowercase()} · icon ${icon.label.lowercase()}"

    companion object {
        /**
         * Resolves the stored axes. Before 9a one key drove spine, badge and icon
         * together; a user who set it keeps that look on both axes until they
         * touch either one. A user who never set it gets the new defaults.
         */
        fun fromStored(legacy: String?, spine: String?, icon: String?): ChoreColourAxes {
            fun parse(name: String?): ColourChoresBy? = name?.let { n -> ColourChoresBy.entries.firstOrNull { it.name == n } }
            val legacyChoice = parse(legacy)
            return ChoreColourAxes(
                spineAndBadge = parse(spine) ?: legacyChoice ?: ColourChoresBy.SEVERITY,
                icon = parse(icon) ?: legacyChoice ?: ColourChoresBy.CATEGORY,
            )
        }
    }
}
