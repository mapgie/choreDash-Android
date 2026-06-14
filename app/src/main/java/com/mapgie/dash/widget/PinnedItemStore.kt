package com.mapgie.dash.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_widget_prefs")

enum class PinnedItemType { TASK, CHORE }

data class PinnedWidgetItem(val type: PinnedItemType, val id: String)

/**
 * Stores the single task or chore the user has chosen to "pin" to the
 * Pinned Item widget via the pin action on task/chore cards.
 */
@Singleton
class PinnedItemStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TYPE = stringPreferencesKey("pinned_item_type")
        val ID = stringPreferencesKey("pinned_item_id")
    }

    val pinnedItem: Flow<PinnedWidgetItem?> = context.widgetPrefsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val type = prefs[Keys.TYPE]?.let { runCatching { PinnedItemType.valueOf(it) }.getOrNull() }
            val id = prefs[Keys.ID]
            if (type != null && id != null) PinnedWidgetItem(type, id) else null
        }

    suspend fun setPinned(item: PinnedWidgetItem?) {
        context.widgetPrefsDataStore.edit { prefs ->
            if (item == null) {
                prefs.remove(Keys.TYPE)
                prefs.remove(Keys.ID)
            } else {
                prefs[Keys.TYPE] = item.type.name
                prefs[Keys.ID] = item.id
            }
        }
    }

    suspend fun togglePinned(item: PinnedWidgetItem) {
        val current = pinnedItem.first()
        setPinned(if (current == item) null else item)
    }
}
