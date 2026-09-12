package com.mapgie.dash.data.model

/**
 * What a horizontal swipe on a list card does. Chosen per list and per
 * direction under Settings › Swipe actions. [displayName] is the resting word;
 * a card may reword it for its current state (Wake, Restore, Turn off).
 */
enum class SwipeAction(val displayName: String) {
    /** The swipe is turned off for that direction. */
    NONE("Nothing"),
    /** Log a chore, tick a task, mark a memo done (turn a tag-alarm off). */
    DONE("Done"),
    /** Snooze a chore for its default duration; push a memo's next ring back an hour. */
    SNOOZE("Snooze"),
    /** Archive without logging or completing; the item leaves the list but keeps its history. */
    ARCHIVE("Archive"),
    /** Remove for good, behind a confirmation. */
    DELETE("Delete");

    companion object {
        fun fromName(name: String?): SwipeAction? = entries.firstOrNull { it.name == name }
    }
}

/** The two horizontal swipes in the user's words, independent of the Compose enum names (LESSONS #51). */
enum class SwipeDirection(val displayName: String) {
    LEFT("Swipe left"),
    RIGHT("Swipe right"),
}

/** The two swipes on one list's cards. */
data class SwipePair(val left: SwipeAction, val right: SwipeAction) {
    fun action(direction: SwipeDirection): SwipeAction = when (direction) {
        SwipeDirection.LEFT -> left
        SwipeDirection.RIGHT -> right
    }

    fun with(direction: SwipeDirection, action: SwipeAction): SwipePair = when (direction) {
        SwipeDirection.LEFT -> copy(left = action)
        SwipeDirection.RIGHT -> copy(right = action)
    }

    /** Whether the card should let a drag start in [direction] at all. */
    fun enabled(direction: SwipeDirection): Boolean = action(direction) != SwipeAction.NONE

    /** This pair with [action] turned off wherever it appears (a tag-alarm's card has no snooze). */
    fun without(action: SwipeAction): SwipePair = SwipePair(
        left = if (left == action) SwipeAction.NONE else left,
        right = if (right == action) SwipeAction.NONE else right,
    )
}

/**
 * Each list with swipeable cards: the actions it can offer (a chore has no
 * delete, it retires by archiving; a task has no snooze) and what it does out
 * of the box, which matches the behaviour before the setting existed.
 */
enum class SwipeSubject(
    val key: String,
    val offered: List<SwipeAction>,
    val default: SwipePair,
) {
    CHORES(
        key = "chores",
        offered = listOf(SwipeAction.NONE, SwipeAction.DONE, SwipeAction.SNOOZE, SwipeAction.ARCHIVE),
        default = SwipePair(left = SwipeAction.SNOOZE, right = SwipeAction.DONE),
    ),
    TASKS(
        key = "tasks",
        offered = listOf(SwipeAction.NONE, SwipeAction.DONE, SwipeAction.ARCHIVE, SwipeAction.DELETE),
        default = SwipePair(left = SwipeAction.NONE, right = SwipeAction.DONE),
    ),
    MEMOS(
        key = "memos",
        offered = listOf(SwipeAction.NONE, SwipeAction.DONE, SwipeAction.SNOOZE, SwipeAction.ARCHIVE, SwipeAction.DELETE),
        default = SwipePair(left = SwipeAction.DONE, right = SwipeAction.DELETE),
    );

    /** The resting word for [action] on this list's cards and in Settings ("Log" for a chore's Done). */
    fun label(action: SwipeAction): String = when {
        this == CHORES && action == SwipeAction.DONE -> "Log"
        else -> action.displayName
    }

    /** [pair] with anything this list does not offer replaced by its default for that direction. */
    fun sanitise(pair: SwipePair): SwipePair = SwipePair(
        left = pair.left.takeIf { it in offered } ?: default.left,
        right = pair.right.takeIf { it in offered } ?: default.right,
    )

    /** Stored names back into a pair: a missing or unknown name falls back to the default for that direction. */
    fun resolve(leftName: String?, rightName: String?): SwipePair = sanitise(
        SwipePair(
            left = SwipeAction.fromName(leftName) ?: default.left,
            right = SwipeAction.fromName(rightName) ?: default.right,
        )
    )
}

/** Settings › Swipe actions: one pair per list. */
data class SwipeSettings(
    val chores: SwipePair = SwipeSubject.CHORES.default,
    val tasks: SwipePair = SwipeSubject.TASKS.default,
    val memos: SwipePair = SwipeSubject.MEMOS.default,
) {
    operator fun get(subject: SwipeSubject): SwipePair = when (subject) {
        SwipeSubject.CHORES -> chores
        SwipeSubject.TASKS -> tasks
        SwipeSubject.MEMOS -> memos
    }

    fun with(subject: SwipeSubject, pair: SwipePair): SwipeSettings = when (subject) {
        SwipeSubject.CHORES -> copy(chores = subject.sanitise(pair))
        SwipeSubject.TASKS -> copy(tasks = subject.sanitise(pair))
        SwipeSubject.MEMOS -> copy(memos = subject.sanitise(pair))
    }
}
