package com.mapgie.dash.data.model

/**
 * The gentle sub-line under a zen row (handoff 3a-4): "kitchen · when you're
 * up", "admin · whenever", "outdoor · done, nice". Zen shows no pressure
 * colours and no counts, so the state is carried by soft words instead.
 */
object ZenPhrase {
    const val DONE = "done, nice"

    /** The soft cue for a chore in [status]; [done] wins once it was logged in this zen session. */
    fun forChore(category: String?, status: ChoreStatus, done: Boolean): String =
        join(category, if (done) DONE else choreCue(status))

    /** The soft cue for a task by its [urgency] and [priority]; [done] wins once it was ticked. */
    fun forTask(category: String?, urgency: TaskUrgency, priority: TaskPriority, done: Boolean): String =
        join(category, if (done) DONE else taskCue(urgency, priority))

    fun choreCue(status: ChoreStatus): String = when (status) {
        ChoreStatus.STALE -> "when you're up"
        ChoreStatus.AGING -> "this week"
        ChoreStatus.FRESH -> "anytime"
        ChoreStatus.NEVER -> "whenever"
    }

    fun taskCue(urgency: TaskUrgency, priority: TaskPriority): String = when (urgency) {
        TaskUrgency.OVERDUE, TaskUrgency.TODAY -> "when you're up"
        TaskUrgency.THIS_WEEK -> "this week"
        TaskUrgency.LATER -> "whenever"
        TaskUrgency.NONE -> if (priority == TaskPriority.HIGHER) "when you're up" else "anytime"
    }

    private fun join(category: String?, cue: String): String {
        val cat = category?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
        return if (cat == null) cue else "$cat · $cue"
    }
}
