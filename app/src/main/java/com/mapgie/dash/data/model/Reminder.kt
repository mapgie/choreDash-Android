package com.mapgie.dash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ReminderDto(
    @SerialName("id") val id: String,
    @SerialName("subject") val subject: String,
    @SerialName("remind_at") val remindAt: String,
    @SerialName("chore_id") val choreId: String? = null,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("reminded") val reminded: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("archived_at") val archivedAt: String? = null
)

@Serializable
data class ReminderInsert(
    @SerialName("subject") val subject: String,
    @SerialName("remind_at") val remindAt: String,
    @SerialName("chore_id") val choreId: String? = null,
    @SerialName("task_id") val taskId: String? = null
)

fun ReminderDto.remindAtInstant(): Instant? =
    runCatching { Instant.parse(remindAt) }.getOrNull()

fun ReminderDto.isPast(): Boolean =
    remindAtInstant()?.isBefore(Instant.now()) ?: false

// True when this reminder still needs an alarm or immediate delivery: never shown,
// not completed, not archived, and carries a parseable fire time. Past-due entries
// are included deliberately — a reminder that came due while the device was off is
// still pending, and BootWorker decides between scheduling and immediate delivery.
fun ReminderDto.needsScheduling(): Boolean =
    !reminded && completedAt == null && archivedAt == null && remindAtInstant() != null
