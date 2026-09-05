package com.mapgie.dash.data.model

/**
 * The sort pill at the right end of the filter row reads its key and its
 * direction in words ("pressure · worst first"), never as an arrow. Each key
 * names both of its directions so the pill, the picker sheet and the persisted
 * setting all agree. Plain Kotlin so the list-state tests can exercise it.
 */
interface SortKey {
    val label: String

    /** Label for the default direction of this key. */
    val firstDirection: String

    /** Label for the reversed direction of this key. */
    val secondDirection: String
}

/** Sort keys for the Chores list. */
enum class ChoreSortKey(
    override val label: String,
    override val firstDirection: String,
    override val secondDirection: String,
) : SortKey {
    /** Cadence pressure: how much of the repeat window has elapsed. */
    PRESSURE("pressure", "worst first", "freshest first"),

    /** When the chore next falls due. */
    DUE("due", "soonest first", "latest first"),

    NAME("name", "A to Z", "Z to A"),

    CATEGORY("category", "A to Z", "Z to A");

    companion object {
        fun fromName(name: String?): ChoreSortKey = entries.firstOrNull { it.name == name } ?: PRESSURE
    }
}

/** Sort keys for the Tasks list. */
enum class TaskSortKey(
    override val label: String,
    override val firstDirection: String,
    override val secondDirection: String,
) : SortKey {
    PRIORITY("priority", "highest first", "lowest first"),

    DUE("due", "soonest first", "latest first"),

    /** When the task was added. */
    CREATED("added", "newest first", "oldest first"),

    NAME("name", "A to Z", "Z to A");

    companion object {
        fun fromName(name: String?): TaskSortKey = entries.firstOrNull { it.name == name } ?: PRIORITY
    }
}

/** Sort keys for the Memos list. */
enum class ReminderSortKey(
    override val label: String,
    override val firstDirection: String,
    override val secondDirection: String,
) : SortKey {
    /** When the memo next rings; a ring nobody has answered sorts by when it rang. */
    NEXT_RING("next ring", "soonest first", "latest first"),

    NAME("name", "A to Z", "Z to A"),

    /** When the memo was added. */
    CREATED("added", "newest first", "oldest first");

    companion object {
        fun fromName(name: String?): ReminderSortKey = entries.firstOrNull { it.name == name } ?: NEXT_RING
    }
}

/** A chosen key plus whether it runs in its reversed direction. */
data class SortOrder<K : SortKey>(val key: K, val reversed: Boolean = false) {
    /** "pressure · worst first", as shown on the pill. */
    val pillLabel: String
        get() = "${key.label} · ${if (reversed) key.secondDirection else key.firstDirection}"
}
