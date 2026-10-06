package com.mapgie.dash.data.repository

import com.mapgie.dash.data.model.NfcTagDto
import com.mapgie.dash.data.model.TagDto
import com.mapgie.dash.data.model.nfcTagChanges
import com.mapgie.dash.data.preferences.PrivateItemStore
import com.mapgie.dash.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one door to saved NFC tags ([NfcTagDto]): Supabase's `nfc_tags` table,
 * plus the tags of private chores, which stay on this phone ([PrivateItemStore])
 * like the chores themselves.
 *
 * A tag lives where its chore lives. Attaching it to a chore in the other store
 * moves it (copy first, delete second, so a failure leaves a duplicate rather
 * than a lost tag); detaching leaves it where it is. A tag saved with no chore
 * goes to Supabase, so every phone in the household can attach it.
 */
@Singleton
class NfcTagRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
    private val privateStore: PrivateItemStore,
) {
    private suspend fun requireClient() = clientProvider.awaitClient()

    /** Every saved tag, attached or not, from both stores. */
    suspend fun all(): List<NfcTagDto> {
        val local = privateStore.current().nfcTags
        return sharedAll().filterNot { shared -> local.any { it.nfcId == shared.nfcId } } + local
    }

    /** Only the tags on this phone: what [all] falls back to when Supabase can't be read. */
    suspend fun onDevice(): List<NfcTagDto> = privateStore.current().nfcTags

    private suspend fun sharedAll(): List<NfcTagDto> =
        requireClient().from(TABLE).select().decodeList<NfcTagDto>()

    /** The saved tag [nfcId], or null when nobody saved it. */
    suspend fun find(nfcId: String): NfcTagDto? =
        privateStore.current().nfcTag(nfcId) ?: findShared(nfcId)

    private suspend fun findShared(nfcId: String): NfcTagDto? =
        requireClient().from(TABLE)
            .select { filter { eq("nfc_id", nfcId) } }
            .decodeSingleOrNull<NfcTagDto>()

    /**
     * Saves the tag [nfcId] under [name], attached to nothing. Saving a tag that
     * is already saved renames it and leaves its chore alone.
     */
    suspend fun save(nfcId: String, name: String): NfcTagDto {
        val existing = find(nfcId)
        if (existing != null) {
            rename(nfcId, name)
            return existing.copy(name = name)
        }
        return requireClient().from(TABLE)
            .insert(NfcTagDto(nfcId = nfcId, name = name)) { select() }
            .decodeSingle<NfcTagDto>()
    }

    suspend fun rename(nfcId: String, name: String) {
        if (privateStore.current().nfcTag(nfcId) != null) {
            privateStore.update { items -> items.nfcTag(nfcId)?.let { items.withNfcTag(it.copy(name = name)) } ?: items }
            return
        }
        patchShared(nfcId, buildJsonObject { put("name", name) })
    }

    /** Forgets the tag [nfcId]: a tap on it no longer logs anything, and it leaves the list. */
    suspend fun delete(nfcId: String) {
        if (privateStore.current().nfcTag(nfcId) != null) {
            privateStore.update { it.withoutNfcTag(nfcId) }
            return
        }
        requireClient().from(TABLE).delete { filter { eq("nfc_id", nfcId) } }
    }

    /**
     * Attaches the tag [nfcId] to the chore with key [choreTagId], saving it as
     * [name] when nobody has yet. Refused when another chore has it: a tag has
     * one job, and taking it over is a detach first.
     */
    suspend fun attach(nfcId: String, choreTagId: String, name: String) {
        val existing = find(nfcId)
        val owner = existing?.choreTagId
        if (owner != null && owner != choreTagId) throw takenBy(owner)
        store((existing ?: NfcTagDto(nfcId = nfcId, name = name)).copy(choreTagId = choreTagId))
    }

    /** Refuses [nfcIds] when any of them is attached to a chore other than [choreTagId]. */
    suspend fun requireFree(nfcIds: List<String>, choreTagId: String? = null) {
        val wanted = nfcIds.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (wanted.isEmpty()) return
        val owner = all().firstOrNull { it.nfcId in wanted && it.isAttached && it.choreTagId != choreTagId }
            ?.choreTagId ?: return
        throw takenBy(owner)
    }

    /** Lets the tag [nfcId] go from its chore. It stays saved, free to attach elsewhere. */
    suspend fun detach(nfcId: String) {
        if (privateStore.current().nfcTag(nfcId) != null) {
            privateStore.update { items -> items.nfcTag(nfcId)?.let { items.withNfcTag(it.copy(choreTagId = null)) } ?: items }
            return
        }
        patchShared(nfcId, buildJsonObject { put("chore_tag_id", null as String?) })
    }

    /**
     * Gives the chore [choreTagId] exactly the tags [nfcIds]: the ones it loses
     * are detached (still saved), the new ones attached, a tag nobody saved
     * named [defaultName]. Checked whole before anything is written, so a tag
     * another chore owns refuses the save without a half-applied list.
     */
    suspend fun setChoreTags(choreTagId: String, nfcIds: List<String>, defaultName: String) {
        val changes = nfcTagChanges(choreTagId, all(), nfcIds, defaultName)
        changes.taken.firstOrNull()?.let { throw takenBy(it.choreTagId!!) }
        changes.detach.forEach { detach(it) }
        changes.attach.forEach { store(it) }
    }

    /**
     * Moves every tag of the chore [choreTagId] to the store the chore is moving
     * to, alongside [ChoreRepository]'s own move. To this phone it runs before the
     * shared chore is deleted (whose foreign key would otherwise free them); to
     * Supabase it runs after the shared chore exists, for the same key.
     */
    suspend fun moveChoreTags(choreTagId: String, toPrivate: Boolean) {
        if (toPrivate) {
            val shared = sharedAll().filter { it.choreTagId == choreTagId }
            if (shared.isEmpty()) return
            privateStore.update { items -> shared.fold(items) { acc, tag -> acc.withNfcTag(tag) } }
            requireClient().from(TABLE).delete { filter { eq("chore_tag_id", choreTagId) } }
        } else {
            val local = privateStore.current().nfcTags.filter { it.choreTagId == choreTagId }
            if (local.isEmpty()) return
            requireClient().from(TABLE).upsert(local.map { it.copy(createdAt = null) })
            privateStore.update { items -> local.fold(items) { acc, tag -> acc.withoutNfcTag(tag.nfcId) } }
        }
    }

    /**
     * Writes [tag] to the store its chore lives in, and drops any copy left in
     * the other one. A tag with no chore stays where it already is.
     */
    private suspend fun store(tag: NfcTagDto) {
        val items = privateStore.current()
        val storedHere = items.nfcTag(tag.nfcId) != null
        val goesHere = tag.choreTagId?.let { items.hasChore(it) } ?: storedHere
        if (goesHere) {
            privateStore.update { it.withNfcTag(tag) }
            // Usually nothing to delete (a new tag on a private chore); a copy the
            // delete misses is harmless, since the one on this phone wins in [all].
            if (!storedHere) runCatching { requireClient().from(TABLE).delete { filter { eq("nfc_id", tag.nfcId) } } }
        } else {
            requireClient().from(TABLE).upsert(tag.copy(createdAt = null))
            if (storedHere) privateStore.update { it.withoutNfcTag(tag.nfcId) }
        }
    }

    private suspend fun patchShared(nfcId: String, patch: JsonObject) {
        requireClient().from(TABLE).update(patch) { filter { eq("nfc_id", nfcId) } }
    }

    /** The refusal for a tag the chore with key [owner] already has, naming it when it can. */
    private suspend fun takenBy(owner: String): IllegalArgumentException {
        val label = privateStore.current().chore(owner)?.label
            ?: runCatching {
                requireClient().from("tags")
                    .select { filter { eq("tag_id", owner) } }
                    .decodeSingleOrNull<TagDto>()?.label
            }.getOrNull()
        val whose = label?.let { "the chore \"$it\"" } ?: "another chore"
        return IllegalArgumentException("That tag already belongs to $whose. A tag has one job.")
    }

    private companion object {
        const val TABLE = "nfc_tags"
    }
}
