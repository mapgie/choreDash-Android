package com.mapgie.dash.data.repository

import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ScanDto
import com.mapgie.dash.data.model.ScanInsert
import com.mapgie.dash.data.model.TagDto
import com.mapgie.dash.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class ChoreLoadResult(
    val active: List<Chore>,
    val archived: List<Chore>,
    val owners: List<String>
)

@Singleton
class ChoreRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider
) {
    private fun requireClient() = clientProvider.currentClient()
        ?: error("Supabase client not configured — enter credentials in Settings")

    suspend fun load(): ChoreLoadResult {
        val client = requireClient()

        val tags = client.from("tags").select().decodeList<TagDto>()
        val scans = client.from("scans")
            .select {
                order("scanned_at", Order.DESCENDING)
            }
            .decodeList<ScanDto>()

        val lastScanByTagId = mutableMapOf<String, ScanDto>()
        for (scan in scans) {
            if (!lastScanByTagId.containsKey(scan.tagId)) {
                lastScanByTagId[scan.tagId] = scan
            }
        }

        val owners = client.from("owners").select().decodeList<OwnerDto>().map { it.handle }

        val active = mutableListOf<Chore>()
        val archived = mutableListOf<Chore>()

        for (tag in tags) {
            val lastScan = lastScanByTagId[tag.tagId]
            val chore = Chore.from(
                tag = tag,
                lastScanned = lastScan?.scannedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
                lastScanId = lastScan?.id
            )
            if (tag.archivedAt == null) active.add(chore) else archived.add(chore)
        }

        active.sortWith(compareByDescending<Chore> {
            it.status == com.mapgie.dash.data.model.ChoreStatus.STALE ||
                it.status == com.mapgie.dash.data.model.ChoreStatus.NEVER
        }.thenBy { it.lastScanned ?: Instant.EPOCH })

        return ChoreLoadResult(active = active, archived = archived, owners = owners)
    }

    suspend fun findByTagId(tagId: String): TagDto? {
        val client = requireClient()
        return runCatching {
            client.from("tags")
                .select { filter { eq("tag_id", tagId) } }
                .decodeSingleOrNull<TagDto>()
        }.getOrNull()
    }

    suspend fun logChore(tagId: String, scannedAt: Instant = Instant.now()): String {
        val client = requireClient()
        val result = client.from("scans")
            .insert(ScanInsert(tagId = tagId, scannedAt = scannedAt.toString()))
            .decodeSingle<ScanDto>()
        return result.id
    }

    suspend fun deleteScan(scanId: String) {
        val client = requireClient()
        client.from("scans").delete { filter { eq("id", scanId) } }
    }

    suspend fun updateTag(tagId: String, label: String, owner: String?, intervalDays: Double?) {
        val client = requireClient()
        client.from("tags").update(
            TagUpdateDto(label = label, owner = owner, intervalDays = intervalDays)
        ) {
            filter { eq("tag_id", tagId) }
        }
    }

    suspend fun archiveTag(tagId: String, archived: Boolean) {
        val client = requireClient()
        val value = if (archived) Instant.now().toString() else null
        client.from("tags").update(
            mapOf("archived_at" to value)
        ) {
            filter { eq("tag_id", tagId) }
        }
    }
}

@kotlinx.serialization.Serializable
private data class OwnerDto(@kotlinx.serialization.SerialName("handle") val handle: String)

@kotlinx.serialization.Serializable
private data class TagUpdateDto(
    @kotlinx.serialization.SerialName("label") val label: String,
    @kotlinx.serialization.SerialName("owner") val owner: String?,
    @kotlinx.serialization.SerialName("interval_days") val intervalDays: Double?
)
