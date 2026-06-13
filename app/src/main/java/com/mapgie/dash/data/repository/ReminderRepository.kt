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

    suspend fun deleteReminder(id: String) {
        saveAll(loadReminders().filterNot { it.id == id })
    }

    suspend fun pendingReminders(): List<ReminderDto> {
        return loadReminders().filter { dto ->
            if (dto.reminded || dto.completedAt != null) return@filter false
            val remindAt = dto.remindAtInstant() ?: return@filter false
            remindAt.isAfter(Instant.now())
        }
    }

    private suspend fun update(id: String, transform: (ReminderDto) -> ReminderDto) {
        saveAll(loadReminders().map { if (it.id == id) transform(it) else it })
    }

    private suspend fun saveAll(reminders: List<ReminderDto>) {
        context.reminderDataStore.edit { it[Keys.REMINDERS] = json.encodeToString(reminders) }
    }
}
