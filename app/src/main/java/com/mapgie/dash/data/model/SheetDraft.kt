package com.mapgie.dash.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Draft key for the New chore / New task sheets, which have no item id yet. */
const val NEW_DRAFT_KEY = "new"

/** The draft key for an Edit sheet: the item's id, or [NEW_DRAFT_KEY] while creating. */
fun draftKeyFor(itemId: String?): String = itemId ?: NEW_DRAFT_KEY

/**
 * Everything the Edit chore sheet can change, as plain values so it can be
 * saved through rotation and process death and offered back on reopen.
 * Blank strings stand for "none" the way the sheet fields do.
 */
@Serializable
data class ChoreDraft(
    val label: String = "",
    val category: String = "",
    val owner: String = "",
    val intervalDays: Int? = null,
    val tagId: String = "",
) {
    /** True when any field differs from [opened], the values the sheet started with. */
    fun differsFrom(opened: ChoreDraft): Boolean =
        label != opened.label ||
            category != opened.category ||
            owner != opened.owner ||
            intervalDays != opened.intervalDays ||
            tagId != opened.tagId

    /** The name to say when offering this draft back: the title typed so far, if any. */
    fun displayName(): String? = label.trim().ifBlank { null }

    companion object {
        /**
         * The values the sheet opens with: [chore]'s own fields, or the New chore
         * defaults (General, [initialTagId] from an NFC scan) when [chore] is null.
         */
        fun of(chore: Chore?, initialTagId: String = ""): ChoreDraft =
            if (chore == null) {
                ChoreDraft(category = GENERAL_CATEGORY, tagId = initialTagId)
            } else {
                ChoreDraft(
                    label = chore.label,
                    category = chore.category ?: "",
                    owner = chore.owner ?: "",
                    intervalDays = chore.intervalDays?.toInt(),
                    tagId = chore.tagId,
                )
            }
    }
}

/** The three shapes of a task's Due row, stored by name in a [TaskDraft]. */
object TaskDueType {
    const val NONE = "none"
    const val DATE = "date"
    const val PERIOD = "period"
}

/**
 * Everything the Edit task sheet can change, as plain values. Dates are epoch
 * days, the reminder is epoch millis at whole-minute precision and is null
 * whenever the reminder is off, so two drafts compare equal when the sheet
 * would show the same thing.
 */
@Serializable
data class TaskDraft(
    val title: String = "",
    val notes: String = "",
    val category: String = "",
    val owner: String = "",
    val priority: String = TaskPriority.NORMAL.name,
    val dueType: String = TaskDueType.NONE,
    val dueDateEpochDay: Long? = null,
    val duePeriod: String = "today",
    val reminderEnabled: Boolean = false,
    val reminderAtEpochMillis: Long? = null,
) {
    /** True when any field differs from [opened], the values the sheet started with. */
    fun differsFrom(opened: TaskDraft): Boolean = this != opened

    /** The name to say when offering this draft back: the title typed so far, if any. */
    fun displayName(): String? = title.trim().ifBlank { null }

    fun priorityEnum(): TaskPriority =
        runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.NORMAL)

    fun dueDate(): LocalDate? = dueDateEpochDay?.let { LocalDate.ofEpochDay(it) }

    companion object {
        /**
         * The values the sheet opens with: [task]'s own fields, or the New task
         * defaults (General, normal priority, no due, no reminder) when null.
         */
        fun of(task: TaskDto?): TaskDraft {
            if (task == null) return TaskDraft(category = GENERAL_CATEGORY)
            return TaskDraft(
                title = task.title,
                notes = task.notes ?: "",
                category = task.category ?: "",
                owner = task.owner ?: "",
                priority = task.priorityEnum().name,
                dueType = when {
                    task.dueDate != null -> TaskDueType.DATE
                    task.duePeriod != null -> TaskDueType.PERIOD
                    else -> TaskDueType.NONE
                },
                dueDateEpochDay = task.dueDate?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() },
                duePeriod = task.duePeriod ?: "today",
                reminderEnabled = task.reminderAt != null,
                // Whole minutes, matching what the sheet resolves on save, so a stored
                // time with seconds does not look changed the moment the sheet opens.
                reminderAtEpochMillis = task.reminderAt?.let {
                    runCatching { Instant.parse(it).truncatedTo(ChronoUnit.MINUTES).toEpochMilli() }.getOrNull()
                },
            )
        }
    }
}
