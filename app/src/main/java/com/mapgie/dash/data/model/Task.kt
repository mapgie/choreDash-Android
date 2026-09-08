package com.mapgie.dash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Serializable
data class TaskDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("owner") val owner: String? = null,
    @SerialName("priority") val priority: String = "normal",
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("due_period") val duePeriod: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
    @SerialName("reminder_at") val reminderAt: String? = null,
    @SerialName("reminded") val reminded: Boolean? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class TaskInsert(
    @SerialName("title") val title: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("owner") val owner: String? = null,
    @SerialName("priority") val priority: String = "normal",
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("due_period") val duePeriod: String? = null,
    @SerialName("reminder_at") val reminderAt: String? = null
)

@Serializable
data class TaskUpdate(
    @SerialName("title") val title: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("owner") val owner: String? = null,
    @SerialName("priority") val priority: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("due_period") val duePeriod: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
    @SerialName("reminder_at") val reminderAt: String? = null,
    @SerialName("reminded") val reminded: Boolean? = null
)

/**
 * Task priority. [wire] is the exact string written to todos.priority and must
 * stay in sync with the todos_priority check constraint in supabase/schema.sql
 * (SchemaSyncTest guards this).
 */
enum class TaskPriority(val wire: String) {
    HIGHER("higher"),
    NORMAL("normal"),
    LOWER("lower");

    companion object {
        fun fromWire(value: String?): TaskPriority = entries.firstOrNull { it.wire == value } ?: NORMAL
    }
}

/**
 * The due "period" buckets a task carries when it has no exact date. [key] is the
 * value written to todos.due_period and the whole set must stay in sync with the
 * todos_due_period check constraint in supabase/schema.sql (SchemaSyncTest guards
 * this). [label] is the word shown in the Due picker and on the card chip.
 */
enum class DuePeriod(val key: String, val label: String) {
    TODAY("today", "Today"),
    THIS_WEEK("this_week", "This week"),
    THIS_MONTH("this_month", "This month"),
    EVENTUALLY("eventually", "Eventually");

    companion object {
        val keys: List<String> = entries.map { it.key }
        fun fromKey(key: String?): DuePeriod? = entries.firstOrNull { it.key == key }
    }
}

enum class TaskUrgency { OVERDUE, TODAY, THIS_WEEK, LATER, NONE }

fun TaskDto.urgency(): TaskUrgency {
    val today = LocalDate.now(ZoneId.systemDefault())
    val endOfWeek = today.plusDays(7)

    if (dueDate != null) {
        val date = runCatching { LocalDate.parse(dueDate) }.getOrNull() ?: return TaskUrgency.NONE
        return when {
            date.isBefore(today) -> TaskUrgency.OVERDUE
            date == today -> TaskUrgency.TODAY
            date.isBefore(endOfWeek) -> TaskUrgency.THIS_WEEK
            else -> TaskUrgency.LATER
        }
    }
    return when (duePeriod) {
        "today" -> TaskUrgency.TODAY
        "this_week" -> TaskUrgency.THIS_WEEK
        "this_month" -> TaskUrgency.LATER
        else -> TaskUrgency.NONE
    }
}

fun TaskDto.reminderInstant(): Instant? =
    reminderAt?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun TaskDto.priorityEnum(): TaskPriority = TaskPriority.fromWire(priority)
