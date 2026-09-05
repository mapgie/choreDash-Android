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
import com.mapgie.dash.data.model.afterDone
import com.mapgie.dash.data.model.afterRing
import com.mapgie.dash.data.model.afterUndone
import com.mapgie.dash.data.model.needsScheduling
import com.mapgie.dash.data.model.withScheduleAligned
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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

    val remindersFlow: Flow<List<ReminderDto>> = context.reminderDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[Keys.REMINDERS]?.let {
                runCatching { json.decodeFromString<List<ReminderDto>>(it) }.getOrNull()
            } ?: emptyList()
        }

    // Reminders still needing attention: not yet archived. Drives whether the
    // Reminders tab should appear at all.
    val outstandingRemindersFlow: Flow<List<ReminderDto>> = remindersFlow.map { reminders ->
        reminders.filter { it.archivedAt == null }
    }

    suspend fun loadReminders(): List<ReminderDto> = remindersFlow.first()

    suspend fun addReminder(insert: ReminderInsert): ReminderDto {
        val now = Instant.now()
        val reminder = ReminderDto(
            id = UUID.randomUUID().toString(),
            subject = insert.subject,
            remindAt = insert.remindAt,
            choreId = insert.choreId,
            taskId = insert.taskId,
            createdAt = now.toString(),
            repeatDays = insert.repeatDays,
        ).withScheduleAligned(now)
        saveAll(loadReminders() + reminder)
        return reminder
    }

    /**
     * Done. A once-only memo completes; a repeating one acknowledges its waiting
     * ring or, with nothing waiting, skips its next ring (see [afterDone]). The
     * returned record's alarm must be re-synced by the caller.
     */
    suspend fun markDone(id: String): ReminderDto? =
        update(id) { it.afterDone(Instant.now()) }

    suspend fun markUndone(id: String): ReminderDto? =
        update(id) { it.afterUndone() }

    /**
     * Records that the memo just rang. A repeating memo comes back with its
     * next occurrence armed in [ReminderDto.remindAt]; the caller schedules it.
     */
    suspend fun recordRing(id: String): ReminderDto? =
        update(id) { it.afterRing(Instant.now()) }

    suspend fun updateReminder(id: String, insert: ReminderInsert): ReminderDto {
        val now = Instant.now()
        val updated = update(id) {
            it.copy(
                subject = insert.subject,
                remindAt = insert.remindAt,
                choreId = insert.choreId,
                taskId = insert.taskId,
                repeatDays = insert.repeatDays,
                reminded = false,
            ).withScheduleAligned(now)
        }
        return requireNotNull(updated) { "Reminder $id not found" }
    }

    suspend fun archiveReminder(id: String, archived: Boolean): ReminderDto? =
        update(id) { it.copy(archivedAt = if (archived) Instant.now().toString() else null) }

    suspend fun deleteReminder(id: String) {
        saveAll(loadReminders().filterNot { it.id == id })
    }

    suspend fun pendingReminders(): List<ReminderDto> =
        loadReminders().filter { it.needsScheduling() }

    /** Applies [transform] to the record with [id] and returns it, or null when there is none. */
    private suspend fun update(id: String, transform: (ReminderDto) -> ReminderDto): ReminderDto? {
        var updated: ReminderDto? = null
        saveAll(loadReminders().map { if (it.id == id) transform(it).also { r -> updated = r } else it })
        return updated
    }

    private suspend fun saveAll(reminders: List<ReminderDto>) {
        context.reminderDataStore.edit { it[Keys.REMINDERS] = json.encodeToString(reminders) }
    }
}
