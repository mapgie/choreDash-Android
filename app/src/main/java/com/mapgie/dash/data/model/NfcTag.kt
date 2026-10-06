package com.mapgie.dash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One physical NFC tag the app has saved: a row in Supabase's `nfc_tags` table,
 * or the same shape on this phone when it is attached to a private chore.
 *
 * [nfcId] is what the tag answers with (its written id, or its hardware UID for
 * a tag nobody wrote). [name] is what the person called it ("Back door").
 * [choreTagId] is the key of the chore a tap logs, or null while the tag is only
 * saved. A chore can have any number of tags; a tag belongs to one chore at most.
 */
@Serializable
data class NfcTagDto(
    @SerialName("nfc_id") val nfcId: String,
    @SerialName("name") val name: String,
    @SerialName("chore_tag_id") val choreTagId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    val isAttached: Boolean get() = choreTagId != null
}

/** The tags attached to the chore with key [choreTagId], oldest first. */
fun List<NfcTagDto>.attachedTo(choreTagId: String): List<NfcTagDto> =
    filter { it.choreTagId == choreTagId }.sortedWith(compareBy({ it.createdAt ?: "" }, { it.name.lowercase() }))

/** The saved tags no chore has yet, A to Z: what the chore sheet offers to attach. */
fun List<NfcTagDto>.unattached(): List<NfcTagDto> =
    filter { it.choreTagId == null }.sortedBy { it.name.lowercase() }

/**
 * What saving a chore's tag list does to the saved tags.
 *
 * [attach] are the rows to write with the chore as their owner: saved tags keep
 * their name, a tag nobody saved yet is named after the chore. [detach] are the
 * ids the chore had and lets go of; they stay saved, free for another chore.
 * [taken] are wanted tags another chore already has: a tag has one job, so the
 * save is refused rather than quietly moving them.
 */
data class NfcTagChanges(
    val attach: List<NfcTagDto>,
    val detach: List<String>,
    val taken: List<NfcTagDto>,
) {
    val isEmpty: Boolean get() = attach.isEmpty() && detach.isEmpty()
}

/**
 * The changes that give the chore [choreTagId] exactly the tags [wantedIds],
 * against every saved tag in [library]. Blank and repeated ids are ignored; a
 * new tag is called [defaultName].
 */
fun nfcTagChanges(
    choreTagId: String,
    library: List<NfcTagDto>,
    wantedIds: List<String>,
    defaultName: String,
): NfcTagChanges {
    val wanted = wantedIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val byId = library.associateBy { it.nfcId }
    val current = library.filter { it.choreTagId == choreTagId }.map { it.nfcId }.toSet()
    val added = wanted.filterNot { it in current }
    val taken = added.mapNotNull { id -> byId[id]?.takeIf { it.choreTagId != null } }
    val attach = added
        .filter { id -> taken.none { it.nfcId == id } }
        .map { id -> byId[id]?.copy(choreTagId = choreTagId) ?: NfcTagDto(id, defaultName, choreTagId) }
    return NfcTagChanges(
        attach = attach,
        detach = current.filterNot { it in wanted }.sorted(),
        taken = taken,
    )
}
