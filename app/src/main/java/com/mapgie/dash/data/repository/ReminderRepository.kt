package com.mapgie.dash.data.repository

import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.ReminderUpdate
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider
) {
    private fun requireClient() = clientProvider.currentClient()
        ?: error("Supabase client not configured — enter credentials in Settings")

    suspend fun loadReminders(): List<ReminderDto> {
        val client = requireClient()
        return client.from("reminders")
            .select { order("remind_at", Order.ASCENDING) }
            .decodeList<ReminderDto>()
    }

    suspend fun addReminder(reminder: ReminderInsert): ReminderDto {
        val client = requireClient()
        return client.from("reminders").insert(reminder) { select() }.decodeSingle<ReminderDto>()
    }

    suspend fun updateReminder(id: String, update: ReminderUpdate): ReminderDto {
        val client = requireClient()
        return client.from("reminders")
            .update(update) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<ReminderDto>()
    }

    suspend fun markDone(id: String) {
        updateReminder(id, ReminderUpdate(completedAt = Instant.now().toString()))
    }

    suspend fun markUndone(id: String) {
        updateReminder(id, ReminderUpdate(completedAt = null))
    }

    suspend fun markReminded(id: String) {
        val client = requireClient()
        client.from("reminders").update(
            ReminderUpdate(reminded = true)
        ) { filter { eq("id", id) } }
    }

    suspend fun deleteReminder(id: String) {
        val client = requireClient()
        client.from("reminders").delete { filter { eq("id", id) } }
    }

    suspend fun pendingReminders(): List<ReminderDto> {
        val client = requireClient()
        return client.from("reminders")
            .select { filter { eq("reminded", false) } }
            .decodeList<ReminderDto>()
            .filter { dto ->
                if (dto.completedAt != null) return@filter false
                val remindAt = dto.remindAtInstant() ?: return@filter false
                remindAt.isAfter(Instant.now())
            }
    }
}
