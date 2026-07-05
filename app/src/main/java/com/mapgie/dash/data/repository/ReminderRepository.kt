package com.mapgie.dash.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.remindAtInstant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_reminders")

// Reminders are stored on-device only, never synced to Supabase.
@Singleton
class ReminderRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val REMINDERS = stringPreferencesKey("reminders")
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadReminders(): List<ReminderDto> {
        return context.reminderDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                prefs[Keys.REMINDERS]?.let {
                    runCatching { json.decodeFromString<List<ReminderDto>>(it) }.getOrNull()
                } ?: emptyList()
            }
            .first()
    }

    suspend fun addReminder(insert: ReminderInsert): ReminderDto {
        val reminder = ReminderDto(
            id = UUID.randomUUID().toString(),
            subject = insert.subject,
            remindAt = insert.remindAt,
            choreId = insert.choreId,
            taskId = insert.taskId,
            createdAt = Instant.now().toString()
        )
        saveAll(loadReminders() + reminder)
        return reminder
    }

    suspend fun markDone(id: String) {
        update(id) { it.copy(completedAt = Instant.now().toString()) }
    }

    suspend fun markUndone(id: String) {
        update(id) { it.copy(completedAt = null) }
    }

    suspend fun markReminded(id: String) {
        update(id) { it.copy(reminded = true) }
    }

    suspend fun updateReminder(id: String, insert: ReminderInsert): ReminderDto {
        var updated: ReminderDto? = null
        update(id) {
            it.copy(
                subject = insert.subject,
                remindAt = insert.remindAt,
                choreId = insert.choreId,
                taskId = insert.taskId,
                reminded = false
            ).also { reminder -> updated = reminder }
        }
        return requireNotNull(updated) { "Reminder $id not found" }
    }

    suspend fun archiveReminder(id: String, archived: Boolean) {
        update(id) { it.copy(archivedAt = if (archived) Instant.now().toString() else null) }
    }

    suspend fun deleteReminder(id: String) {
        saveAll(loadReminders().filterNot { it.id == id })
    }

    // Includes past-due entries: a reminder whose fire time elapsed while the device
    // was off is still pending until it has actually been shown (reminded flag).
    // BootWorker decides whether to schedule an alarm or deliver immediately.
    suspend fun pendingReminders(): List<ReminderDto> {
        return loadReminders().filter { dto ->
            if (dto.reminded || dto.completedAt != null || dto.archivedAt != null) return@filter false
            dto.remindAtInstant() != null
        }
    }

    private suspend fun update(id: String, transform: (ReminderDto) -> ReminderDto) {
        saveAll(loadReminders().map { if (it.id == id) transform(it) else it })
    }

    private suspend fun saveAll(reminders: List<ReminderDto>) {
        context.reminderDataStore.edit { it[Keys.REMINDERS] = json.encodeToString(reminders) }
    }
}
