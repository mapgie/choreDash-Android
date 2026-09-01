package com.mapgie.dash.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.choreSnoozeDataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_chore_snoozes")

/**
 * Chores the user has swiped to snooze on this device, keyed by tag id with the
 * instant they wake. Stored locally, not in Supabase: a snooze is "stop showing
 * me this for now", a per-phone display choice like smart visibility, so it
 * never hides a chore from other household members. Expired entries are
 * dropped on every read and write.
 */
@Singleton
class ChoreSnoozeStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SNOOZES = stringSetPreferencesKey("chore_snoozes")
    }

    /** Tag id to wake time for every snooze still in the future. */
    val snoozes: Flow<Map<String, Instant>> = context.choreSnoozeDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> decode(prefs[Keys.SNOOZES].orEmpty()).stillActive() }

    suspend fun snooze(tagId: String, until: Instant) = write { it + (tagId to until) }

    suspend fun clear(tagId: String) = write { it - tagId }

    private suspend fun write(transform: (Map<String, Instant>) -> Map<String, Instant>) {
        context.choreSnoozeDataStore.edit { prefs ->
            val current = decode(prefs[Keys.SNOOZES].orEmpty()).stillActive()
            prefs[Keys.SNOOZES] = encode(transform(current))
        }
    }

    private fun Map<String, Instant>.stillActive(): Map<String, Instant> {
        val now = Instant.now()
        return filterValues { it.isAfter(now) }
    }

    // Entries are "<tagId>|<epochMillis>"; the tag id may itself contain anything but
    // is split at the last separator so a stray '|' in an id cannot corrupt it.
    private fun decode(raw: Set<String>): Map<String, Instant> = raw.mapNotNull { entry ->
        val sep = entry.lastIndexOf('|')
        if (sep <= 0) return@mapNotNull null
        val millis = entry.substring(sep + 1).toLongOrNull() ?: return@mapNotNull null
        entry.substring(0, sep) to Instant.ofEpochMilli(millis)
    }.toMap()

    private fun encode(map: Map<String, Instant>): Set<String> =
        map.map { (tagId, until) -> "$tagId|${until.toEpochMilli()}" }.toSet()
}
