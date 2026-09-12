package com.mapgie.dash.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

/** Result of attempting to write a chore tag ID onto a physical NFC tag. */
sealed class NfcWriteResult {
    object Success : NfcWriteResult()
    object NotWritable : NfcWriteResult()
    object TooSmall : NfcWriteResult()
    data class Error(val message: String?) : NfcWriteResult()
}

/**
 * A tag the app is waiting to write: a chore's tag id (`chordash://tag?tag=<id>`)
 * or a tag-alarm's tag id (`chordash://memo?memo=<id>`). Both read back through
 * [NfcHandler.extractTagId] as the bare id, so one id space serves chores and
 * tag-alarms alike; the host only says which kind minted it. [fromSettings]
 * marks a write started on Settings › NFC tags, which shows its own dialog.
 */
data class NfcWriteRequest(val kind: Kind, val id: String, val fromSettings: Boolean = false) {
    /** What to put on the tag; [ERASE] wipes it instead, [id] unused. */
    enum class Kind { CHORE, MEMO, ERASE }

    /** The URI to write; null for an erase. */
    val uri: String?
        get() = when (kind) {
            Kind.CHORE -> NfcHandler.choreTagUri(id)
            Kind.MEMO -> NfcHandler.memoTagUri(id)
            Kind.ERASE -> null
        }
}

/**
 * Extracts the id an NFC tag stands for from an NFC intent.
 *
 * Priority order:
 * 1. NDEF text record payload — matches the string written by Tasker / NFC Tools
 * 2. NDEF URI record — the ?tag= (chore) or ?memo= (tag-alarm) query param, else
 *    the last path segment
 * 3. Raw hardware tag ID as lowercase hex — fallback for unformatted tags
 */
object NfcHandler {

    fun choreTagUri(tagId: String): String = "chordash://tag?tag=$tagId"

    fun memoTagUri(memoId: String): String = "chordash://memo?memo=$memoId"

    fun extractTagId(intent: Intent?): String? {
        intent ?: return null
        if (intent.action !in setOf(
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED
            )
        ) return null

        // Try NDEF message first
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMessages != null) {
            val fromNdef = rawMessages
                .filterIsInstance<NdefMessage>()
                .flatMap { it.records.toList() }
                .firstNotNullOfOrNull { extractFromRecord(it) }
            if (fromNdef != null) return fromNdef
        }

        // Fallback: raw hardware tag ID as hex
        val tag = intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
        return tag?.id?.joinToString("") { "%02x".format(it) }
    }

    /**
     * Writes a chore tag ID onto [tag] as a single NDEF URI record
     * (`chordash://tag?tag=<tagId>`), formatting blank tags if needed.
     */
    fun writeTagId(tag: Tag, tagId: String): NfcWriteResult = writeUri(tag, choreTagUri(tagId))

    /** Writes [uri] onto [tag] as a single NDEF URI record, formatting blank tags if needed. */
    fun writeUri(tag: Tag, uri: String): NfcWriteResult =
        writeMessage(tag, NdefMessage(arrayOf(NdefRecord.createUri(uri))))

    /**
     * The id [tag] currently carries, read from its cached NDEF message without
     * connecting, or null when it has none the app can read. Used before an
     * erase to say which id is leaving the sticker.
     */
    fun currentTagId(tag: Tag): String? =
        Ndef.get(tag)?.cachedNdefMessage?.records?.firstNotNullOfOrNull { extractFromRecord(it) }

    /**
     * Wipes [tag]: its NDEF message becomes a single empty record, so the next
     * read finds nothing and the sticker is ready to be written for something
     * else. A tag that was never formatted has nothing on it and counts as done.
     */
    fun eraseTag(tag: Tag): NfcWriteResult {
        if (Ndef.get(tag) == null) return NfcWriteResult.Success
        val empty = NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0))
        return writeMessage(tag, NdefMessage(arrayOf(empty)))
    }

    private fun writeMessage(tag: Tag, message: NdefMessage): NfcWriteResult {
        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                if (!ndef.isWritable) return NfcWriteResult.NotWritable
                if (ndef.maxSize < message.toByteArray().size) return NfcWriteResult.TooSmall
                ndef.connect()
                try {
                    ndef.writeNdefMessage(message)
                } finally {
                    ndef.close()
                }
            } else {
                val formatable = NdefFormatable.get(tag) ?: return NfcWriteResult.NotWritable
                formatable.connect()
                try {
                    formatable.format(message)
                } finally {
                    formatable.close()
                }
            }
            NfcWriteResult.Success
        } catch (e: Exception) {
            NfcWriteResult.Error(e.message)
        }
    }

    private fun extractFromRecord(record: NdefRecord): String? = when {
        record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
            val payload = record.payload
            val langLen = payload[0].toInt() and 0x3F
            String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8).trim()
                .takeIf { it.isNotEmpty() }
        }
        record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_URI) -> {
            record.toUri()?.let { uri ->
                uri.getQueryParameter("tag") ?: uri.getQueryParameter("memo") ?: uri.lastPathSegment
            }
        }
        else -> null
    }
}