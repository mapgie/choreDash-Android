package com.mapgie.dash.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter

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
