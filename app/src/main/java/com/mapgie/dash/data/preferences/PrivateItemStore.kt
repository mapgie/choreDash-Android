package com.mapgie.dash.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mapgie.dash.data.model.PrivateItems
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.privateDataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_private")

/**
 * Persists the [PrivateItems] document: every chore and task in the reserved
 * Private category, with the logs of those chores. On this phone only; nothing
 * here is ever sent to Supabase. `TaskRepository` and `ChoreRepository` are the
 * only readers and writers, so every caller of those (lists, widgets, alarms,
 * an NFC tap) sees private rows without knowing where they live.
 */
@Singleton
class PrivateItemStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val ITEMS = stringPreferencesKey("items_json")
    }

    val items: Flow<PrivateItems> = context.privateDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> decode(prefs[Keys.ITEMS]) }

    suspend fun current(): PrivateItems = items.first()

    private fun decode(raw: String?): PrivateItems =
        raw?.let { runCatching { json.decodeFromString<PrivateItems>(it) }.getOrNull() } ?: PrivateItems()

    /** Applies [transform] to the stored document and writes the result back, returning it. */
    suspend fun update(transform: (PrivateItems) -> PrivateItems): PrivateItems {
        var next = PrivateItems()
        context.privateDataStore.edit { prefs ->
            next = transform(decode(prefs[Keys.ITEMS]))
            prefs[Keys.ITEMS] = json.encodeToString(next)
        }
        return next
    }
}
