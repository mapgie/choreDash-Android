package com.mapgie.dash.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** How each chore's on-device "Show from" is stored and changed. */
class ChoreLeadCodecTest {

    @Test
    fun `show-from settings round-trip through storage`() {
        val leads = mapOf("washer" to 3, "insurance" to 30, "bins" to 0)
        assertEquals(leads, ChoreLeadCodec.decode(ChoreLeadCodec.encode(leads)))
    }

    @Test
    fun `a tag id containing the separator survives storage`() {
        val leads = mapOf("office|a" to 7)
        assertEquals(leads, ChoreLeadCodec.decode(ChoreLeadCodec.encode(leads)))
    }

    @Test
    fun `unreadable or negative entries are dropped`() {
        val decoded = ChoreLeadCodec.decode(setOf("washer|3", "no-separator", "|5", "bins|soon", "old|-2"))
        assertEquals(mapOf("washer" to 3), decoded)
    }

    @Test
    fun `setting a chore back to Auto removes its entry`() {
        val current = mapOf("washer" to 3, "bins" to 1)
        assertEquals(mapOf("bins" to 1), ChoreLeadCodec.withLead(current, "washer", null))
        assertTrue(ChoreLeadCodec.withLead(mapOf("washer" to 3), "washer", -1).isEmpty())
    }

    @Test
    fun `setting a chore's show-from replaces its old value`() {
        assertEquals(mapOf("washer" to 14), ChoreLeadCodec.withLead(mapOf("washer" to 3), "washer", 14))
    }
}
