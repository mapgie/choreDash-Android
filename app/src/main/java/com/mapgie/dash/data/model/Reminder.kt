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
    @SerialName("reminded") val reminded: Boolean? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ReminderInsert(
    @SerialName("subject") val subject: String,
    @SerialName("remind_at") val remindAt: String,
    @SerialName("chore_id") val choreId: String? = null,
    @SerialName("task_id") val taskId: String? = null
)

@Serializable
data class ReminderUpdate(
    @SerialName("subject") val subject: String? = null,
    @SerialName("remind_at") val remindAt: String? = null,
    @SerialName("chore_id") val choreId: String? = null,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("reminded") val reminded: Boolean? = null
)

fun ReminderDto.remindAtInstant(): Instant? =
    runCatching { Instant.parse(remindAt) }.getOrNull()
