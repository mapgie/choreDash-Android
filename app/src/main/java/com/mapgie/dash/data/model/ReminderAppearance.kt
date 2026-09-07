package com.mapgie.dash.data.model

/**
 * How a memo's list card is coloured and iconed. Two rules, kept in plain Kotlin
 * so `ReminderAppearanceTest` can pin them without Compose:
 *
 * - **Linked to a chore or task:** the memo borrows that item's category look, so
 *   at a glance you can see which chore a memo belongs to. It follows the user's
 *   Settings › Colours axes exactly as the chore card does: with "colour by
 *   category" the spine and chip wear the category swatch; with "colour by
 *   severity" they stay null and the card falls back to the memo's own ring-urgency
 *   tone. Either way the chip shows the linked category's glyph.
 * - **Standalone:** the memo shows its own picked [ReminderDto.colour] / [icon]
 *   (from the bell chip in the edit sheet). Null swatch falls back to the ring tone;
 *   null icon falls back to the bell.
 *
 * [spineSwatch] / [iconSwatch] are `Swatch?`: null means "no fixed colour, use the
 * card's status tone", the same contract `ChoreCard` uses. [icon] is a
 * [CategoryIcon] or null for the default bell; the UI layer maps it to a glyph.
 */
data class ReminderAppearance(
    val icon: CategoryIcon?,
    val spineSwatch: Swatch?,
    val iconSwatch: Swatch?,
) {
    companion object {
        /**
         * @param linkedCategory the linked chore's or task's category, when the memo is
         *   linked to one that still exists; null for a standalone memo (or a link whose
         *   target was deleted, which is treated as standalone).
         */
        fun of(
            reminder: ReminderDto,
            linkedCategory: LinkedCategory?,
            catalog: CategoryCatalog,
            axes: ChoreColourAxes,
        ): ReminderAppearance {
            if (linkedCategory != null) {
                val swatch = catalog.effectiveSwatch(linkedCategory.name)
                return ReminderAppearance(
                    icon = catalog.iconFor(linkedCategory.name),
                    spineSwatch = axes.spineSwatch(swatch),
                    iconSwatch = axes.iconSwatch(swatch),
                )
            }
            val own = Swatch.fromName(reminder.colour)
            return ReminderAppearance(
                icon = CategoryIcon.fromName(reminder.icon),
                spineSwatch = own,
                iconSwatch = own,
            )
        }
    }
}

/**
 * Marks that a memo is linked to an existing chore or task, carrying that item's
 * [name] category. A distinct wrapper (rather than a bare `String?`) so "linked to
 * an uncategorised chore" is told apart from "standalone": the former still inherits
 * (a blank category resolves to a stable fallback swatch and glyph), the latter uses
 * the memo's own pick.
 */
data class LinkedCategory(val name: String?)
