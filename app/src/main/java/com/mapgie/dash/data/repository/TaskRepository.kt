package com.mapgie.dash.data.repository

import com.mapgie.dash.data.model.OwnerDto
import com.mapgie.dash.data.model.PrivateMove
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskInsert
import com.mapgie.dash.data.model.TaskUpdate
import com.mapgie.dash.data.model.isPrivateCategory
import com.mapgie.dash.data.model.privateMove
import com.mapgie.dash.data.preferences.PrivateItemStore
import com.mapgie.dash.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one door to tasks. Shared tasks live in Supabase's `todos` table; tasks
 * in the reserved Private category live only on this phone ([PrivateItemStore])
 * and never reach Supabase. Every method routes by where the row is stored, so
 * the list, the widgets, the alarms and Settings need not know the difference.
 * An edit that changes the category across that boundary moves the row (see
 * [privateMove]) and keeps its id.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
    private val privateStore: PrivateItemStore,
) {
    private suspend fun requireClient() = clientProvider.awaitClient()

    /**
     * Shared and private tasks together, newest first. Should a move ever be
     * left half done (the same id in both places), the private copy wins, so
     * the list never carries two rows with one id.
     */
    suspend fun loadTasks(): List<TaskDto> {
        val client = requireClient()
        val shared = client.from("todos")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<TaskDto>()
        val onDevice = privateStore.current()
        return (shared.filterNot { onDevice.hasTask(it.id) } + onDevice.tasks).sortedByDescending { it.createdAt }
    }

    suspend fun loadOwners(): List<String> {
        val client = requireClient()
        return client.from("owners").select().decodeList<OwnerDto>().map { it.handle }
    }

    suspend fun addTask(task: TaskInsert): TaskDto {
        if (isPrivateCategory(task.category)) {
            val row = privateRow(task.id ?: UUID.randomUUID().toString(), task, createdAt = Instant.now().toString())
            privateStore.update { it.withTask(row) }
            return row
        }
        val client = requireClient()
        return client.from("todos").insert(task) { select() }.decodeSingle<TaskDto>()
    }

    suspend fun updateTask(taskId: String, update: TaskUpdate): TaskDto {
        val stored = privateStore.current().task(taskId)
        return when (privateMove(stored != null, update.category)) {
            PrivateMove.STAY_SHARED -> patchTask(taskId, editTaskPayload(update))
            PrivateMove.STAY_PRIVATE -> updatePrivate(taskId) { it.edited(update) }
            PrivateMove.TO_PRIVATE -> {
                // Copy the shared row onto this phone with its completion, archival
                // and reminded state intact, then delete it from Supabase so the
                // household stops seeing it. The copy is written first so nothing is
                // ever lost; if the delete fails the copy is taken back and the
                // error surfaces, leaving the task shared as it was.
                val shared = fetchShared(taskId) ?: throw IllegalStateException("Task $taskId not found")
                val row = shared.edited(update)
                privateStore.update { it.withTask(row) }
                runCatching { deleteShared(taskId) }.onFailure { e ->
                    privateStore.update { it.withoutTask(taskId) }
                    throw e
                }
                row
            }
            PrivateMove.TO_SHARED -> {
                // Insert in Supabase under the same id, then forget the local copy.
                val client = requireClient()
                val inserted = client.from("todos")
                    .insert(requireNotNull(stored).edited(update).toInsert()) { select() }
                    .decodeSingle<TaskDto>()
                privateStore.update { it.withoutTask(taskId) }
                inserted
            }
        }
    }

    suspend fun markDone(taskId: String, completedAt: Instant = Instant.now()): TaskDto =
        if (isPrivate(taskId)) updatePrivate(taskId) { it.copy(completedAt = completedAt.toString()) }
        else patchTask(taskId, completedAtPayload(completedAt.toString()))

    suspend fun markUndone(taskId: String): TaskDto =
        if (isPrivate(taskId)) updatePrivate(taskId) { it.copy(completedAt = null) }
        else patchTask(taskId, completedAtPayload(null))

    /** Archives (or restores) a task without touching its completion. */
    suspend fun archiveTask(taskId: String, archived: Boolean): TaskDto {
        val at = if (archived) Instant.now().toString() else null
        return if (isPrivate(taskId)) updatePrivate(taskId) { it.copy(archivedAt = at) }
        else patchTask(taskId, archivedAtPayload(at))
    }

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
        if (isPrivate(taskId)) {
            privateStore.update { it.updateTask(taskId) { t -> t.copy(reminded = true) } }
            return
        }
        val client = requireClient()
        client.from("todos").update(
            TaskUpdate(reminded = true)
        ) { filter { eq("id", taskId) } }
    }

    /**
     * Moves every shared task in category [from] to [to] (null clears the column).
     * Used by Settings › Categories for rename and delete. Private tasks are
     * never in any other category, and Private itself cannot be renamed or
     * deleted, so only Supabase is touched.
     */
    suspend fun moveCategory(from: String, to: String?) {
        val client = requireClient()
        client.from("todos").update(mapOf("category" to to)) {
            filter { eq("category", from) }
        }
    }

    suspend fun deleteTask(taskId: String) {
        if (isPrivate(taskId)) {
            privateStore.update { it.withoutTask(taskId) }
            return
        }
        deleteShared(taskId)
    }

    // Includes past-due entries: a reminder whose fire time elapsed while the device
    // was off is still pending until it has actually been shown (reminded flag).
    // BootWorker decides whether to schedule an alarm or deliver immediately.
    suspend fun pendingReminders(): List<TaskDto> {
        val client = requireClient()
        val shared = client.from("todos")
            .select {
                filter { eq("reminded", false) }
            }
            .decodeList<TaskDto>()
        val onDevice = privateStore.current().tasks.filter { it.reminded != true }
        return (shared + onDevice).filter { dto ->
            if (dto.completedAt != null) return@filter false
            dto.reminderAt?.let { runCatching { Instant.parse(it) }.getOrNull() } != null
        }
    }

    private suspend fun isPrivate(taskId: String): Boolean = privateStore.current().hasTask(taskId)

    private suspend fun updatePrivate(taskId: String, transform: (TaskDto) -> TaskDto): TaskDto {
        val next = privateStore.update { it.updateTask(taskId, transform) }
        return requireNotNull(next.task(taskId)) { "Task $taskId not found" }
    }

    private suspend fun fetchShared(taskId: String): TaskDto? {
        val client = requireClient()
        return client.from("todos")
            .select { filter { eq("id", taskId) } }
            .decodeSingleOrNull<TaskDto>()
    }

    private suspend fun deleteShared(taskId: String) {
        val client = requireClient()
        client.from("todos").delete { filter { eq("id", taskId) } }
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

/** Single-column payload flipping archival; null brings the task back to the list. */
internal fun archivedAtPayload(archivedAt: String?): Map<String, String?> =
    mapOf("archived_at" to archivedAt)

/**
 * The same edit-sheet save applied to a stored row: exactly the columns of
 * [editTaskPayload], with completion, archival and reminded state untouched. A
 * changed reminder time is a new reminder, so `reminded` resets like the
 * `reminder_at` mirror memo does in the ViewModel.
 */
internal fun TaskDto.edited(update: TaskUpdate): TaskDto = copy(
    title = update.title ?: title,
    notes = update.notes,
    category = update.category,
    owner = update.owner,
    priority = update.priority ?: priority,
    dueDate = update.dueDate,
    duePeriod = update.duePeriod,
    reminderAt = update.reminderAt,
    reminded = if (update.reminderAt != reminderAt) false else reminded,
)

/** A private row for a New task: the insert's columns under [id], created now, nothing else set. */
internal fun privateRow(id: String, insert: TaskInsert, createdAt: String): TaskDto = TaskDto(
    id = id,
    title = insert.title,
    notes = insert.notes,
    category = insert.category,
    owner = insert.owner,
    priority = insert.priority,
    dueDate = insert.dueDate,
    duePeriod = insert.duePeriod,
    reminderAt = insert.reminderAt,
    reminded = false,
    createdAt = createdAt,
)

/**
 * The row as an insert that keeps its id. Completion and archival travel with
 * it; `reminded` and `created_at` are left to Supabase, as they would be for a
 * fresh insert, so a moved task is simply a new shared row that happens to keep
 * its identity.
 */
internal fun TaskDto.toInsert(): TaskInsert = TaskInsert(
    id = id,
    title = title,
    notes = notes,
    category = category,
    owner = owner,
    priority = priority,
    dueDate = dueDate,
    duePeriod = duePeriod,
    reminderAt = reminderAt,
    completedAt = completedAt,
    archivedAt = archivedAt,
)
