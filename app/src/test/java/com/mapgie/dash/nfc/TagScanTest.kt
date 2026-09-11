package com.mapgie.dash.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The NFC payload parser: a chordash URI (written as a URI record, or as a plain
 * text record by NFC Tools) reduces to its bare id and kind; a bare payload is a
 * chore tag id. This is the guard that a raw `chordash://…` string never leaks
 * through as an id or a name (the bug that named a memo after its own deep link).
 */
class TagScanTest {

    @Test
    fun `memo deep link yields the memo id`() {
        assertEquals(
            ScannedTag(TagKind.MEMO, "d4e9aff0-9745-404e-8457-e90deecf810d"),
            parseTagPayload("chordash://memo?memo=d4e9aff0-9745-404e-8457-e90deecf810d"),
        )
    }

    @Test
    fun `a memo deep link stored as a plain text record is still reduced to the id`() {
        // NFC Tools writes the URI as a text record; the whole string must not survive.
        assertEquals(
            ScannedTag(TagKind.MEMO, "abc-123"),
            parseTagPayload("  chordash://memo?memo=abc-123\n"),
        )
    }

    @Test
    fun `chore deep link yields a chore tag id`() {
        assertEquals(ScannedTag(TagKind.CHORE, "kitchen-01"), parseTagPayload("chordash://tag?tag=kitchen-01"))
    }

    @Test
    fun `a bare payload is a chore tag id`() {
        assertEquals(ScannedTag(TagKind.CHORE, "04a1b2c3d4"), parseTagPayload("04a1b2c3d4"))
    }

    @Test
    fun `scheme is case-insensitive and host decides the kind`() {
        assertEquals(ScannedTag(TagKind.MEMO, "X"), parseTagPayload("CHORDASH://memo?memo=X"))
    }

    @Test
    fun `a path-style deep link falls back to the last path segment`() {
        assertEquals(ScannedTag(TagKind.CHORE, "xyz"), parseTagPayload("chordash://tag/xyz"))
    }

    @Test
    fun `a url-encoded id is decoded`() {
        assertEquals(ScannedTag(TagKind.MEMO, "a b"), parseTagPayload("chordash://memo?memo=a%20b"))
    }

    @Test
    fun `blank payload is null`() {
        assertNull(parseTagPayload("   "))
    }
}
