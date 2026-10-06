package com.mapgie.dash.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What saving a chore's tag list does to the saved tags: a chore can have any
 * number of tags, a tag can sit saved with no chore, and a tag logs one chore
 * at most.
 */
class NfcTagTest {

    private val library = listOf(
        NfcTagDto("front-door", "Front door", choreTagId = "bins"),
        NfcTagDto("back-door", "Back door", choreTagId = "bins"),
        NfcTagDto("spare", "Spare fob"),
        NfcTagDto("sill", "Window sill", choreTagId = "plants"),
    )

    @Test
    fun `a saved tag with no chore is unattached and offered A to Z`() {
        assertEquals(listOf("spare"), library.unattached().map { it.nfcId })
        assertEquals(listOf("back-door", "front-door"), library.attachedTo("bins").map { it.nfcId }.sorted())
    }

    @Test
    fun `adding a saved tag to a chore keeps the name it was saved under`() {
        val changes = nfcTagChanges("bins", library, listOf("front-door", "back-door", "spare"), defaultName = "Bins")
        assertEquals(listOf(NfcTagDto("spare", "Spare fob", choreTagId = "bins")), changes.attach)
        assertEquals(emptyList<String>(), changes.detach)
    }

    @Test
    fun `a tag nobody saved is saved under the chore's name`() {
        val changes = nfcTagChanges("bins", library, listOf("front-door", "back-door", "04a1b2"), defaultName = "Bins")
        assertEquals(listOf(NfcTagDto("04a1b2", "Bins", choreTagId = "bins")), changes.attach)
    }

    @Test
    fun `a tag the chore lets go of is detached but stays saved`() {
        val changes = nfcTagChanges("bins", library, listOf("front-door"), defaultName = "Bins")
        assertEquals(listOf("back-door"), changes.detach)
        assertTrue(changes.attach.isEmpty())
    }

    @Test
    fun `a tag another chore has is refused rather than moved`() {
        val changes = nfcTagChanges("bins", library, listOf("front-door", "back-door", "sill"), defaultName = "Bins")
        assertEquals(listOf("sill"), changes.taken.map { it.nfcId })
        assertTrue(changes.attach.isEmpty())
    }

    @Test
    fun `an unchanged tag list changes nothing, whatever the spacing and repeats`() {
        val changes = nfcTagChanges("bins", library, listOf(" back-door", "front-door", "front-door", ""), defaultName = "Bins")
        assertTrue(changes.isEmpty)
        assertTrue(changes.taken.isEmpty())
    }
}
