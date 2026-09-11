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
 * or a memo's own id (`chordash://memo?memo=<id>`). Both read back through
 * [NfcHandler.extractScan] as the bare id plus its [TagKind]; the host says which
 * kind minted it, so a memo tag can be routed to its memo and a chore tag to its
 * chore.
 */
data class NfcWriteRequest(val kind: TagKind, val id: String) {
    val uri: String
        get() = when (kind) {
            TagKind.CHORE -> NfcHandler.choreTagUri(id)
            TagKind.MEMO -> NfcHandler.memoTagUri(id)
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

    /** The bare id an NFC tag stands for, ignoring its kind. */
    fun extractTagId(intent: Intent?): String? = extractScan(intent)?.id

    /** The id an NFC tag stands for, plus whether a chore or a memo minted it. */
    fun extractScan(intent: Intent?): ScannedTag? {
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

        // Fallback: raw hardware tag ID as hex, treated as a bare chore tag id.
        val tag = intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
        val hex = tag?.id?.joinToString("") { "%02x".format(it) } ?: return null
        return ScannedTag(TagKind.CHORE, hex)
    }

    /**
     * Writes a chore tag ID onto [tag] as a single NDEF URI record
     * (`chordash://tag?tag=<tagId>`), formatting blank tags if needed.
     */
    fun writeTagId(tag: Tag, tagId: String): NfcWriteResult = writeUri(tag, choreTagUri(tagId))

    /** Writes [uri] onto [tag] as a single NDEF URI record, formatting blank tags if needed. */
    fun writeUri(tag: Tag, uri: String): NfcWriteResult {
        val message = NdefMessage(arrayOf(NdefRecord.createUri(uri)))
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

    private fun extractFromRecord(record: NdefRecord): ScannedTag? = when {
        record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
            val payload = record.payload
            val langLen = payload[0].toInt() and 0x3F
            val text = String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
            // A text record may itself hold a chordash URI (NFC Tools writes it that
            // way); parse it down rather than passing the whole string through.
            parseTagPayload(text)
        }
        record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_URI) -> {
            record.toUri()?.toString()?.let { parseTagPayload(it) }
        }
        else -> null
    }
}