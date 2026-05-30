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

enum class TaskPriority { HIGHER, NORMAL, LOWER }
enum class DuePeriod { TODAY, THIS_WEEK, THIS_MONTH }

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

fun TaskDto.priorityEnum(): TaskPriority = when (priority) {
    "higher" -> TaskPriority.HIGHER
    "lower" -> TaskPriority.LOWER
    else -> TaskPriority.NORMAL
}
