package com.mapgie.dash.data.model

/**
 * The three chore severities the user can recolour in Settings › Colours.
 * Maps one-to-one onto the signalling tones of the shared status scale
 * (`StatusTone.CRITICAL` / `ATTENTION` / `OK` in `ui/theme`); kept as its own
 * plain-Kotlin enum so settings persistence never depends on UI code.
 */
enum class Severity(
    val label: String,
    /** Example badge shown beside the label in Settings › Colours. */
    val sampleBadge: String,
    val defaultSwatch: Swatch,
) {
    OVERDUE("Overdue", "35d over", Swatch.ROSE),
    DUE_SOON("Due soon", "1d left", Swatch.AMBER),
    FRESH("Fresh", "12d left", Swatch.SAGE);

    companion object {
        val defaults: Map<Severity, Swatch> = entries.associateWith { it.defaultSwatch }
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
