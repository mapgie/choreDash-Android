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
import com.mapgie.dash.data.model.armedForNextMorning
import com.mapgie.dash.data.model.disarmed
import com.mapgie.dash.data.model.isTagAlarm
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
            sound = insert.sound,
            colour = insert.colour,
            icon = insert.icon,
            tagAlarm = insert.tagAlarm,
            tagId = insert.tagId,
            ringTimes = insert.ringTimes,
        ).withScheduleAligned(now)
        saveAll(loadReminders() + reminder)
        return reminder
    }

    /** The unarchived tag-alarm linked to [tagId], the one a tap on that tag arms. */
    suspend fun findTagAlarmByTagId(tagId: String): ReminderDto? =
        loadReminders().firstOrNull { it.isTagAlarm && it.tagId == tagId && it.archivedAt == null }

    /**
     * A tap on the tag, or Set for next: arms the tag-alarm's next morning (see
     * [armedForNextMorning]; a ring already ahead is kept). The caller re-syncs
     * the alarm. Null when there is no such record.
     */
    suspend fun armTagAlarm(id: String): ReminderDto? =
        update(id) { if (it.isTagAlarm) it.armedForNextMorning(Instant.now()) else it }

    /** Turn off / Stop for today: the tag-alarm goes dormant. The caller clears its alarm. */
    suspend fun disarmTagAlarm(id: String): ReminderDto? =
        update(id) { if (it.isTagAlarm) it.disarmed() else it }

    /** Links (or, with null, unlinks) the NFC tag a tag-alarm answers to. Nothing else changes. */
    suspend fun setTagAlarmTag(id: String, tagId: String?): ReminderDto? =
        update(id) { if (it.isTagAlarm) it.copy(tagId = tagId?.trim()?.ifBlank { null }) else it }

    /**
     * Done from the notification or ring screen. A once-only memo completes; a
     * repeating one is unchanged, its next ring stays armed (see [afterDone]).
     * The returned record's alarm must be re-synced by the caller.
     */
    suspend fun markDone(id: String): ReminderDto? =
        update(id) { it.afterDone(Instant.now()) }

    /**
     * Reverses a once-only memo's Done (from a swipe's Undo): clears the completed
     * mark and the rung flag so it counts as active again. The caller re-syncs the
     * alarm. A repeating memo has no Done to reverse.
     */
    suspend fun markUndone(id: String): ReminderDto? =
        update(id) { it.copy(completedAt = null, reminded = false) }

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
                sound = insert.sound,
                colour = insert.colour,
                icon = insert.icon,
                tagAlarm = insert.tagAlarm,
                tagId = insert.tagId,
                ringTimes = insert.ringTimes,
                reminded = false,
            ).withScheduleAligned(now)
        }
        return requireNotNull(updated) { "Reminder $id not found" }
    }

    /**
     * Writes [reminder] back over the stored record with the same id (a swipe's
     * snooze, or its Undo restoring the copy taken before). The caller re-syncs
     * the alarm from the returned record.
     */
    suspend fun replace(reminder: ReminderDto): ReminderDto? =
        update(reminder.id) { reminder }

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
