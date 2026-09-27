package com.mapgie.dash.data.repository

import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreSchedule
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.PrivateMove
import com.mapgie.dash.data.model.ScanDto
import com.mapgie.dash.data.model.ScanInsert
import com.mapgie.dash.data.model.TagDto
import com.mapgie.dash.data.model.TagInsert
import com.mapgie.dash.data.model.isPrivateCategory
import com.mapgie.dash.data.model.privateMove
import com.mapgie.dash.data.preferences.PrivateItemStore
import com.mapgie.dash.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ChoreLoadResult(
    val active: List<Chore>,
    val archived: List<Chore>,
    val owners: List<String>
)

/**
 * The one door to chores. Shared chores are rows in Supabase's `tags` table
 * with their logs in `scans`; chores in the reserved Private category, and
 * their logs, live only on this phone ([PrivateItemStore]) and never reach
 * Supabase. Every method routes by where the row is stored, so the list, the
 * widgets, an NFC tap and Settings need not know the difference. An edit that
 * changes the category across that boundary moves the chore and its logs (see
 * [privateMove]); the tag id, the chore's key, never changes.
 *
 * A chore's NFC tag is a separate, optional [TagDto.nfcId]: null until a tag is
 * linked, and cleared again by [setNfcId] so the sticker can serve something
 * else. A tag has one job, so an NFC id belongs to at most one chore.
 */
@Singleton
class ChoreRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
    private val privateStore: PrivateItemStore,
) {
    private suspend fun requireClient() = clientProvider.awaitClient()

    suspend fun load(): ChoreLoadResult {
        val client = requireClient()

        val tags = client.from("tags").select().decodeList<TagDto>()
        val scans = client.from("scans")
            .select {
                order("scanned_at", Order.DESCENDING)
            }
            .decodeList<ScanDto>()
        val onDevice = privateStore.current()

        val lastScanByTagId = mutableMapOf<String, ScanDto>()
        for (scan in scans + onDevice.scans.sortedByDescending { it.scannedAt }) {
            if (!lastScanByTagId.containsKey(scan.tagId)) {
                lastScanByTagId[scan.tagId] = scan
            }
        }

        val owners = client.from("owners").select().decodeList<OwnerDto>().map { it.handle }

        val active = mutableListOf<Chore>()
        val archived = mutableListOf<Chore>()

        // Should a move ever be left half done (one tag id in both places), the
        // private copy wins, so the list never carries two rows with one id.
        for (tag in tags.filterNot { onDevice.hasChore(it.tagId) } + onDevice.chores) {
            val lastScan = lastScanByTagId[tag.tagId]
            val chore = Chore.from(
                tag = tag,
                lastScanned = lastScan?.scannedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
                lastScanId = lastScan?.id
            )
            if (tag.archivedAt == null) active.add(chore) else archived.add(chore)
        }

        active.sortWith(compareByDescending<Chore> {
            it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER
        }.thenBy { it.lastScanned ?: Instant.EPOCH })

        return ChoreLoadResult(active = active, archived = archived, owners = owners)
    }

    /** The chore with [tagId], shared or private, or null. */
    suspend fun findByTagId(tagId: String): TagDto? =
        privateStore.current().chore(tagId) ?: findShared(tagId)

    /** The chore whose NFC tag carries [nfcId], shared or private, or null. */
    suspend fun findByNfcId(nfcId: String): TagDto? =
        privateStore.current().choreByNfcId(nfcId) ?: findSharedByNfcId(nfcId)

    private suspend fun findSharedByNfcId(nfcId: String): TagDto? {
        val client = requireClient()
        // A database without the nfc_id column yet rejects the filter: nothing matches.
        return runCatching {
            client.from("tags")
                .select { filter { eq("nfc_id", nfcId) } }
                .decodeSingleOrNull<TagDto>()
        }.getOrNull()
    }

    /**
     * Refuses [nfcId] when another chore's tag already carries it. The shared
     * table's UNIQUE would refuse it too, but with a database error, and it
     * cannot see private chores.
     */
    private suspend fun requireNfcIdFree(nfcId: String?, forTagId: String?) {
        if (nfcId == null) return
        val owner = findByNfcId(nfcId) ?: return
        if (owner.tagId == forTagId) return
        throw IllegalArgumentException("That tag already belongs to the chore \"${owner.label}\". A tag has one job.")
    }

    private suspend fun findShared(tagId: String): TagDto? {
        val client = requireClient()
        return runCatching {
            client.from("tags")
                .select { filter { eq("tag_id", tagId) } }
                .decodeSingleOrNull<TagDto>()
        }.getOrNull()
    }

    suspend fun logChore(tagId: String, scannedAt: Instant = Instant.now()): String {
        if (privateStore.current().hasChore(tagId)) {
            val scan = ScanDto(id = UUID.randomUUID().toString(), tagId = tagId, scannedAt = scannedAt.toString())
            privateStore.update { it.withScan(scan) }
            return scan.id
        }
        val client = requireClient()
        val result = client.from("scans")
            .insert(ScanInsert(tagId = tagId, scannedAt = scannedAt.toString())) { select() }
            .decodeSingle<ScanDto>()
        return result.id
    }

    suspend fun scanHistory(tagId: String, limit: Long = 4): List<ScanDto> {
        val onDevice = privateStore.current()
        if (onDevice.hasChore(tagId)) return onDevice.scansFor(tagId).take(limit.toInt())
        return sharedScanHistory(tagId, limit)
    }

    private suspend fun sharedScanHistory(tagId: String, limit: Long): List<ScanDto> {
        val client = requireClient()
        return client.from("scans")
            .select {
                filter { eq("tag_id", tagId) }
                order("scanned_at", Order.DESCENDING)
                limit(limit)
            }
            .decodeList<ScanDto>()
    }

    suspend fun deleteScan(scanId: String) {
        if (privateStore.current().hasScan(scanId)) {
            privateStore.update { it.withoutScan(scanId) }
            return
        }
        val client = requireClient()
        client.from("scans").delete { filter { eq("id", scanId) } }
    }

    suspend fun updateTag(
        tagId: String,
        label: String,
        category: String?,
        owner: String?,
        schedule: ChoreSchedule,
        nfcId: String?,
    ) {
        val intervalDays = schedule.repeat?.intervalDays
        val repeatUnit = schedule.repeat?.unit?.wire
        val due = schedule.dueDate?.toString()
        requireNfcIdFree(nfcId, forTagId = tagId)
        val stored = privateStore.current().chore(tagId)
        when (privateMove(stored != null, category)) {
            PrivateMove.STAY_SHARED -> {
                // Send the schedule columns only when there is something to set or
                // clear, so plain edits keep working on a database that has not had
                // schema.sql's due_date / repeat_unit columns applied yet. The read
                // costs one round trip per edit and exists only for that window: once
                // every project has the columns, drop it and always send them.
                // nfc_id likewise goes only when the tag changed.
                val shared = findShared(tagId)
                val hadSchedule = shared?.let { it.dueDate != null || it.repeatUnit != null } ?: true
                val tagChanged = if (shared != null) shared.nfcId != nfcId else nfcId != null
                patchShared(
                    tagId,
                    chorePatch(label, category, owner, schedule, includeSchedule = hadSchedule, nfcId = nfcId, includeNfcId = tagChanged),
                )
            }
            PrivateMove.STAY_PRIVATE -> privateStore.update {
                it.updateChore(tagId) { t ->
                    t.copy(
                        label = label, category = category, owner = owner,
                        intervalDays = intervalDays, dueDate = due, repeatUnit = repeatUnit,
                        nfcId = nfcId,
                    )
                }
            }
            PrivateMove.TO_PRIVATE -> {
                // Copy the shared row and its whole log history onto this phone,
                // then delete the row from Supabase (its scans cascade). The copy is
                // written first so nothing is ever lost; if the delete fails the copy
                // is taken back and the error surfaces, leaving the chore shared.
                val shared = findShared(tagId) ?: throw IllegalStateException("Chore $tagId not found")
                val history = sharedScanHistory(tagId, limit = ALL_SCANS)
                val row = shared.copy(
                    label = label, category = category, owner = owner,
                    intervalDays = intervalDays, dueDate = due, repeatUnit = repeatUnit,
                    nfcId = nfcId,
                )
                privateStore.update { it.withChore(row).withScans(history) }
                runCatching { deleteShared(tagId) }.onFailure { e ->
                    privateStore.update { it.withoutChore(tagId) }
                    throw e
                }
            }
            PrivateMove.TO_SHARED -> {
                // Insert in Supabase under the same row id, replay the logs, then
                // forget the local copy.
                val row = requireNotNull(stored)
                val client = requireClient()
                client.from("tags").insert(
                    TagInsert(
                        id = row.id,
                        tagId = tagId,
                        label = label,
                        category = category,
                        owner = owner,
                        intervalDays = intervalDays,
                        dueDate = due,
                        repeatUnit = repeatUnit,
                        nfcId = nfcId,
                    )
                )
                val history = privateStore.current().scansFor(tagId)
                if (history.isNotEmpty()) {
                    client.from("scans").insert(history.map { ScanInsert(tagId = tagId, scannedAt = it.scannedAt) })
                }
                if (row.archivedAt != null) archiveShared(tagId, row.archivedAt)
                privateStore.update { it.withoutChore(tagId) }
            }
        }
    }

    /**
     * Links the chore with [tagId] to the NFC tag carrying [nfcId], or unlinks
     * it with null. The chore, its logs and the sticker itself are untouched; an
     * unlinked sticker's id is free for another chore or a tag-alarm.
     */
    suspend fun setNfcId(tagId: String, nfcId: String?) {
        requireNfcIdFree(nfcId, forTagId = tagId)
        if (privateStore.current().hasChore(tagId)) {
            privateStore.update { it.updateChore(tagId) { t -> t.copy(nfcId = nfcId) } }
            return
        }
        patchShared(tagId, buildJsonObject { put("nfc_id", nfcId) })
    }

    private suspend fun patchShared(tagId: String, patch: JsonObject) {
        val client = requireClient()
        client.from("tags").update(patch) {
            filter { eq("tag_id", tagId) }
        }
    }

    /**
     * Moves every shared chore in category [from] to [to] (null clears the column).
     * Used by Settings › Categories for rename and delete. Private chores are
     * never in any other category, and Private itself cannot be renamed or
     * deleted, so only Supabase is touched.
     */
    suspend fun moveCategory(from: String, to: String?) {
        val client = requireClient()
        client.from("tags").update(mapOf("category" to to)) {
            filter { eq("category", from) }
        }
    }

    /**
     * Adds a chore. Its key is minted here and never shown; [nfcId] is the id
     * of its NFC tag, or null for a chore with no tag.
     */
    suspend fun createTag(
        label: String,
        category: String?,
        owner: String?,
        schedule: ChoreSchedule,
        nfcId: String?,
    ): TagDto {
        val intervalDays = schedule.repeat?.intervalDays
        val tagId = UUID.randomUUID().toString()
        // NFC ids are unique across both stores: a sticker has one job.
        requireNfcIdFree(nfcId, forTagId = null)
        if (isPrivateCategory(category)) {
            val row = TagDto(
                id = UUID.randomUUID().toString(),
                tagId = tagId,
                label = label,
                category = category,
                owner = owner,
                intervalDays = intervalDays,
                dueDate = schedule.dueDate?.toString(),
                repeatUnit = schedule.repeat?.unit?.wire,
                createdAt = Instant.now().toString(),
                nfcId = nfcId,
            )
            privateStore.update { it.withChore(row) }
            return row
        }
        val client = requireClient()
        return client.from("tags")
            .insert(
                TagInsert(
                    tagId = tagId,
                    label = label,
                    category = category,
                    owner = owner,
                    intervalDays = intervalDays,
                    dueDate = schedule.dueDate?.toString(),
                    repeatUnit = schedule.repeat?.unit?.wire,
                    nfcId = nfcId,
                )
            ) { select() }
            .decodeSingle<TagDto>()
    }

    suspend fun archiveTag(tagId: String, archived: Boolean) {
        val value = if (archived) Instant.now().toString() else null
        if (privateStore.current().hasChore(tagId)) {
            privateStore.update { it.updateChore(tagId) { t -> t.copy(archivedAt = value) } }
            return
        }
        archiveShared(tagId, value)
    }

    private suspend fun archiveShared(tagId: String, archivedAt: String?) {
        val client = requireClient()
        client.from("tags").update(
            mapOf("archived_at" to archivedAt)
        ) {
            filter { eq("tag_id", tagId) }
        }
    }

    private suspend fun deleteShared(tagId: String) {
        val client = requireClient()
        client.from("tags").delete { filter { eq("tag_id", tagId) } }
    }

    private companion object {
        /** Enough to carry a chore's whole history across a move; nobody logs this often. */
        const val ALL_SCANS = 10_000L
    }
}

/**
 * The PATCH body for a shared chore edit. An explicit JSON object so a null
 * clears the column instead of being dropped (LESSONS.md #33) while
 * interval_days stays numeric. `due_date` and `repeat_unit` are sent only when
 * [includeSchedule] is set or there is a value to write: a database without
 * those columns rejects any body that names them. `nfc_id` is sent only when
 * [includeNfcId] says the tag changed, for the same reason.
 */
internal fun chorePatch(
    label: String,
    category: String?,
    owner: String?,
    schedule: ChoreSchedule,
    includeSchedule: Boolean,
    nfcId: String? = null,
    includeNfcId: Boolean = false,
): JsonObject = buildJsonObject {
    put("label", label)
    put("category", category)
    put("owner", owner)
    put("interval_days", schedule.repeat?.intervalDays)
    val repeatUnit = schedule.repeat?.unit?.wire
    if (includeSchedule || schedule.dueDate != null || repeatUnit != null) {
        put("due_date", schedule.dueDate?.toString())
        put("repeat_unit", repeatUnit)
    }
    if (includeNfcId) put("nfc_id", nfcId)
}

@kotlinx.serialization.Serializable
private data class OwnerDto(@kotlinx.serialization.SerialName("handle") val handle: String)
