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
    private suspend fun requireClient() = clientProvider.awaitClient()

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
        return client.from("todos").insert(task) { select() }.decodeSingle<TaskDto>()
    }

    suspend fun updateTask(taskId: String, update: TaskUpdate): TaskDto =
        patchTask(taskId, editTaskPayload(update))

    suspend fun markDone(taskId: String): TaskDto =
        patchTask(taskId, completedAtPayload(Instant.now().toString()))

    suspend fun markUndone(taskId: String): TaskDto =
        patchTask(taskId, completedAtPayload(null))

    private suspend fun patchTask(taskId: String, payload: Map<String, String?>): TaskDto {
        val client = requireClient()
        return client.from("todos")
            .update(payload) {
                select()
                filter { eq("id", taskId) }
            }
            .decodeSingle<TaskDto>()
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

    // Includes past-due entries: a reminder whose fire time elapsed while the device
    // was off is still pending until it has actually been shown (reminded flag).
    // BootWorker decides whether to schedule an alarm or deliver immediately.
    suspend fun pendingReminders(): List<TaskDto> {
        val client = requireClient()
        return client.from("todos")
            .select {
                filter { eq("reminded", false) }
            }
            .decodeList<TaskDto>()
            .filter { dto ->
                if (dto.completedAt != null) return@filter false
                dto.reminderAt?.let { runCatching { Instant.parse(it) }.getOrNull() } != null
            }
    }
}

// PATCH payloads are built as maps rather than by serializing [TaskUpdate]:
// kotlinx.serialization omits properties equal to their default (encodeDefaults
// is false in the Supabase client), and every TaskUpdate field defaults to null,
// so "set this column to null" was silently dropped from the request body.
// That turned clearing a field into a no-op, and made un-completing a task an
// empty PATCH whose response failed decodeSingle with "List is empty".
// Map entries have no defaults, so a null value is sent as an explicit JSON null
// (the same pattern ChoreRepository.archiveTag already uses). See LESSONS.md #33.

/**
 * Full edit-sheet save: the sheet owns exactly these columns and always submits
 * all of them, with null meaning "clear". Completion, archival, and reminded
 * state are deliberately absent so an edit never clobbers them.
 */
internal fun editTaskPayload(update: TaskUpdate): Map<String, String?> = mapOf(
    "title" to update.title,
    "notes" to update.notes,
    "category" to update.category,
    "owner" to update.owner,
    "priority" to update.priority,
    "due_date" to update.dueDate,
    "due_period" to update.duePeriod,
    "reminder_at" to update.reminderAt,
)

/** Single-column payload flipping completion; null restores the task to active. */
internal fun completedAtPayload(completedAt: String?): Map<String, String?> =
    mapOf("completed_at" to completedAt)
