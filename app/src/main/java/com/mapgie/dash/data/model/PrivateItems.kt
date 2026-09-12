package com.mapgie.dash.data.model

import kotlinx.serialization.Serializable

/**
 * Everything in the reserved [PRIVATE_CATEGORY], stored on this phone only
 * (`PrivateItemStore`, DataStore) and never written to Supabase: private tasks,
 * private chores (as the same [TagDto] rows Supabase would hold) and the logs
 * of those chores. One document, so a move in or out of the category is one
 * atomic write.
 *
 * The repositories decide where a row lives from its category alone (see
 * [privateMove]): in the private category it is here, otherwise it is in
 * Supabase. Ids are UUIDs in both places, so a row keeps its id when it moves
 * and its memos and widget pin keep pointing at it.
 */
@Serializable
data class PrivateItems(
    val tasks: List<TaskDto> = emptyList(),
    val chores: List<TagDto> = emptyList(),
    val scans: List<ScanDto> = emptyList(),
) {
    // ── Tasks ─────────────────────────────────────────────────────────────────

    fun hasTask(id: String): Boolean = tasks.any { it.id == id }

    fun task(id: String): TaskDto? = tasks.firstOrNull { it.id == id }

    /** Adds [task], replacing any stored row with the same id. */
    fun withTask(task: TaskDto): PrivateItems =
        copy(tasks = tasks.filterNot { it.id == task.id } + task)

    /** Applies [transform] to the task with [id]; unchanged when there is none. */
    fun updateTask(id: String, transform: (TaskDto) -> TaskDto): PrivateItems =
        copy(tasks = tasks.map { if (it.id == id) transform(it) else it })

    fun withoutTask(id: String): PrivateItems = copy(tasks = tasks.filterNot { it.id == id })

    // ── Chores and their logs ─────────────────────────────────────────────────

    fun hasChore(tagId: String): Boolean = chores.any { it.tagId == tagId }

    fun chore(tagId: String): TagDto? = chores.firstOrNull { it.tagId == tagId }

    /** Adds [chore], replacing any stored row with the same tag id. */
    fun withChore(chore: TagDto): PrivateItems =
        copy(chores = chores.filterNot { it.tagId == chore.tagId } + chore)

    /** Applies [transform] to the chore with [tagId]; unchanged when there is none. */
    fun updateChore(tagId: String, transform: (TagDto) -> TagDto): PrivateItems =
        copy(chores = chores.map { if (it.tagId == tagId) transform(it) else it })

    /** Removes the chore and, like the `scans` foreign key cascade, every log of it. */
    fun withoutChore(tagId: String): PrivateItems = copy(
        chores = chores.filterNot { it.tagId == tagId },
        scans = scans.filterNot { it.tagId == tagId },
    )

    fun hasScan(scanId: String): Boolean = scans.any { it.id == scanId }

    fun withScan(scan: ScanDto): PrivateItems = copy(scans = scans.filterNot { it.id == scan.id } + scan)

    fun withScans(added: List<ScanDto>): PrivateItems = added.fold(this) { acc, scan -> acc.withScan(scan) }

    fun withoutScan(scanId: String): PrivateItems = copy(scans = scans.filterNot { it.id == scanId })

    /** Logs of the chore with [tagId], newest first, the order Supabase queries return them in. */
    fun scansFor(tagId: String): List<ScanDto> =
        scans.filter { it.tagId == tagId }.sortedByDescending { it.scannedAt }
}

/**
 * Where an edited row ends up. A row is stored by its category alone, so an
 * edit that changes the category across the private boundary moves the row:
 * out of Supabase and onto this phone, or the other way round.
 */
enum class PrivateMove {
    /** Already on this phone and staying private: edit in place. */
    STAY_PRIVATE,

    /** Already in Supabase and staying shared: edit in place. */
    STAY_SHARED,

    /** A shared row becoming private: copy it here, then delete it from Supabase. */
    TO_PRIVATE,

    /** A private row becoming shared: insert it in Supabase (same id), then forget it here. */
    TO_SHARED,
}

/** The [PrivateMove] for a row that [isStoredPrivately] now and is being saved with [newCategory]. */
fun privateMove(isStoredPrivately: Boolean, newCategory: String?): PrivateMove {
    val wantsPrivate = isPrivateCategory(newCategory)
    return when {
        isStoredPrivately && wantsPrivate -> PrivateMove.STAY_PRIVATE
        isStoredPrivately -> PrivateMove.TO_SHARED
        wantsPrivate -> PrivateMove.TO_PRIVATE
        else -> PrivateMove.STAY_SHARED
    }
}
