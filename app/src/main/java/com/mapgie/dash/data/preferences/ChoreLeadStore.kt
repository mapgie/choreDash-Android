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
import javax.inject.Inject
import javax.inject.Singleton

private val Context.choreLeadDataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_chore_lead_days")

/**
 * Each chore's own "show from" on this device: hide it until this many days
 * before it is due, keyed by tag id. Stored locally, not in Supabase, like the
 * per-bucket lead times in Settings and swipe-to-snooze: when a chore shows up
 * is a per-phone display choice, so one person's setting never hides a chore
 * from the rest of the household. A chore with no entry follows the automatic rule.
 */
@Singleton
class ChoreLeadStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LEAD_DAYS = stringSetPreferencesKey("chore_lead_days")
    }

    /** Tag id to days before due, for every chore with its own setting. */
    val leadDays: Flow<Map<String, Int>> = context.choreLeadDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> decode(prefs[Keys.LEAD_DAYS].orEmpty()) }

    /** Sets [tagId]'s own lead time, or removes it (back to automatic) when [days] is null. */
    suspend fun set(tagId: String, days: Int?) {
        context.choreLeadDataStore.edit { prefs ->
            val current = decode(prefs[Keys.LEAD_DAYS].orEmpty())
            val updated = if (days == null || days < 0) current - tagId else current + (tagId to days)
            prefs[Keys.LEAD_DAYS] = encode(updated)
        }
    }

    // Entries are "<tagId>|<days>", split at the last separator so a stray '|'
    // in a tag id cannot corrupt it (same scheme as ChoreSnoozeStore).
    private fun decode(raw: Set<String>): Map<String, Int> = raw.mapNotNull { entry ->
        val sep = entry.lastIndexOf('|')
        if (sep <= 0) return@mapNotNull null
        val days = entry.substring(sep + 1).toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
        entry.substring(0, sep) to days
    }.toMap()

    private fun encode(map: Map<String, Int>): Set<String> =
        map.map { (tagId, days) -> "$tagId|$days" }.toSet()
}
