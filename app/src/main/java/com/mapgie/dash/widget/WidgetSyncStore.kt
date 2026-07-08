package com.mapgie.dash.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetSyncDataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_widget_sync")

enum class WidgetSyncKey { NEXT_UP, PINNED_ITEM }

/**
 * Tracks the last time each auto-refreshing widget successfully loaded data from
 * Supabase, so a connection failure can say how stale the last-shown data is
 * instead of just reporting "unavailable".
 */
@Singleton
class WidgetSyncStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun key(widget: WidgetSyncKey) = stringPreferencesKey("last_synced_${widget.name}")

    suspend fun markSynced(widget: WidgetSyncKey) {
        context.widgetSyncDataStore.edit { it[key(widget)] = Instant.now().toString() }
    }

    suspend fun lastSyncedAt(widget: WidgetSyncKey): Instant? {
        return context.widgetSyncDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs -> prefs[key(widget)]?.let { runCatching { Instant.parse(it) }.getOrNull() } }
            .first()
    }
}
