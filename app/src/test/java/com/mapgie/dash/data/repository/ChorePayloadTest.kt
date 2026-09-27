package com.mapgie.dash.data.repository

import com.mapgie.dash.data.model.ChoreRepeat
import com.mapgie.dash.data.model.ChoreSchedule
import com.mapgie.dash.data.model.RepeatUnit
import java.time.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChorePayloadTest {

    @Test
    fun `a chore edit without a date leaves the due date columns out`() {
        // A database that has not had the due_date / repeat_unit columns added
        // rejects any PATCH naming them, so a plain edit must not send them.
        val payload = chorePatch("Vacuum", "Cleaning", null, ChoreSchedule(ChoreRepeat(7, RepeatUnit.DAY)), includeSchedule = false)
        assertEquals(setOf("label", "category", "owner", "interval_days"), payload.keys)
        assertEquals(JsonPrimitive(7.0), payload["interval_days"])
    }

    @Test
    fun `a yearly chore with a date writes the date, the unit and 365 days`() {
        val payload = chorePatch(
            "House insurance", "Admin", null,
            ChoreSchedule(ChoreRepeat(1, RepeatUnit.YEAR), LocalDate.of(2026, 10, 1)), includeSchedule = false,
        )
        assertEquals(JsonPrimitive("2026-10-01"), payload["due_date"])
        assertEquals(JsonPrimitive("year"), payload["repeat_unit"])
        assertEquals(JsonPrimitive(365.0), payload["interval_days"])
    }

    @Test
    fun `clearing a chore's date sends explicit nulls`() {
        val payload = chorePatch("House insurance", null, null, ChoreSchedule(), includeSchedule = true)
        assertEquals(JsonNull, payload["due_date"])
        assertEquals(JsonNull, payload["repeat_unit"])
        assertEquals(JsonNull, payload["interval_days"])
    }

    @Test
    fun `a repeat in days never names the repeat unit column on its own`() {
        val payload = chorePatch("Water plants", null, null, ChoreSchedule(ChoreRepeat(3, RepeatUnit.DAY)), includeSchedule = false)
        assertFalse(payload.containsKey("repeat_unit"))
    }

    @Test
    fun `a chore's show-from never reaches the database`() {
        // It is a per-phone setting (ChoreLeadStore), so no edit names lead_days.
        val payload = chorePatch("Vacuum", null, null, ChoreSchedule(dueDate = LocalDate.of(2026, 10, 1)), includeSchedule = true)
        assertFalse(payload.containsKey("lead_days"))
    }

    @Test
    fun `a chore edit that leaves its tag alone never names nfc_id`() {
        // A database without the nfc_id column rejects any PATCH naming it.
        val payload = chorePatch("Vacuum", null, null, ChoreSchedule(), includeSchedule = false, nfcId = "vacuum", includeNfcId = false)
        assertFalse(payload.containsKey("nfc_id"))
    }

    @Test
    fun `unlinking a chore's tag sends an explicit null`() {
        val payload = chorePatch("Vacuum", null, null, ChoreSchedule(), includeSchedule = false, nfcId = null, includeNfcId = true)
        assertEquals(JsonNull, payload["nfc_id"])
    }

    @Test
    fun `linking a chore to a tag writes its id`() {
        val payload = chorePatch("Vacuum", null, null, ChoreSchedule(), includeSchedule = false, nfcId = "hall-cupboard", includeNfcId = true)
        assertEquals(JsonPrimitive("hall-cupboard"), payload["nfc_id"])
    }
}
