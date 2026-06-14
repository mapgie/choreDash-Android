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
 * Extracts the chore tag_id from an NFC intent.
 *
 * Priority order:
 * 1. NDEF text record payload — matches the string written by Tasker / NFC Tools
 * 2. NDEF URI record — extracts ?tag= query param or last path segment
 * 3. Raw hardware tag ID as lowercase hex — fallback for unformatted tags
 */
object NfcHandler {

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
    fun writeTagId(tag: Tag, tagId: String): NfcWriteResult {
        val message = NdefMessage(arrayOf(NdefRecord.createUri("chordash://tag?tag=$tagId")))
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
                uri.getQueryParameter("tag") ?: uri.lastPathSegment
            }
        }
        else -> null
    }
}