package com.mapgie.dash.data.repository

import com.mapgie.dash.data.model.OwnerDto
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskInsert
import com.mapgie.dash.data.model.TaskUpdate
import com.mapgie.dash.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider
) {
    private fun requireClient() = clientProvider.currentClient()
        ?: error("Supabase client not configured — enter credentials in Settings")

    suspend fun loadTasks(): List<TaskDto> {
        val client = requireClient()
        return client.from("todos")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<TaskDto>()
    }

    suspend fun loadOwners(): List<String> {
        val client = requireClient()
        return client.from("owners").select().decodeList<OwnerDto>().map { it.handle }
    }

    suspend fun addTask(task: TaskInsert): TaskDto {
        val client = requireClient()
        return client.from("todos").insert(task).decodeSingle<TaskDto>()
    }

    suspend fun updateTask(taskId: String, update: TaskUpdate): TaskDto {
        val client = requireClient()
        return client.from("todos")
            .update(update) { filter { eq("id", taskId) } }
            .decodeSingle<TaskDto>()
    }

    suspend fun markDone(taskId: String): TaskDto {
        return updateTask(taskId, TaskUpdate(completedAt = Instant.now().toString()))
    }

    suspend fun markUndone(taskId: String): TaskDto {
        return updateTask(taskId, TaskUpdate(completedAt = null))
    }

    suspend fun markReminded(taskId: String) {
        val client = requireClient()
        client.from("todos").update(
            TaskUpdate(reminded = true)
        ) { filter { eq("id", taskId) } }
    }

    suspend fun deleteTask(taskId: String) {
        val client = requireClient()
        client.from("todos").delete { filter { eq("id", taskId) } }
    }

    suspend fun pendingReminders(): List<TaskDto> {
        val client = requireClient()
        return client.from("todos")
            .select {
                filter { eq("reminded", false) }
            }
            .decodeList<TaskDto>()
            .filter { dto ->
                if (dto.completedAt != null) return@filter false
                val reminderAt = dto.reminderAt?.let {
                    runCatching { Instant.parse(it) }.getOrNull()
                } ?: return@filter false
                reminderAt.isAfter(Instant.now())
            }
    }
}
