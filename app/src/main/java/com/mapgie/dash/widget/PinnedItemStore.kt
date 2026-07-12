package com.mapgie.dash.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_widget_prefs")

enum class PinnedItemType { TASK, CHORE }

data class PinnedWidgetItem(val type: PinnedItemType, val id: String)

/** Pending "which widget?" choice, shown when pinning while 2+ instances are placed. */
data class PinChooserState(val item: PinnedWidgetItem, val widgetIds: List<Int>)

/**
 * Stores the task or chore the user has chosen to "pin" to the Pinned Item
 * widget via the pin action on task/chore cards.
 *
 * There's a single default pin (unchanged from before), which is what the
 * in-app pin icon reflects and what any widget instance shows unless it has
 * its own override. When a user has 2+ Pinned Item widgets placed, a specific
 * instance can be given its own override via the `...For(appWidgetId, ...)`
 * methods, so each placed widget can show something different.
 */
@Singleton
class PinnedItemStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TYPE = stringPreferencesKey("pinned_item_type")
        val ID = stringPreferencesKey("pinned_item_id")
    }

    private fun typeKey(appWidgetId: Int) = stringPreferencesKey("pinned_item_type_$appWidgetId")
    private fun idKey(appWidgetId: Int) = stringPreferencesKey("pinned_item_id_$appWidgetId")

    /** The default pin: shown by the in-app pin icon and by any widget instance with no override. */
    val pinnedItem: Flow<PinnedWidgetItem?> = context.widgetPrefsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> readItem(prefs, Keys.TYPE, Keys.ID) }

    /** What widget instance [appWidgetId] should show: its own override, falling back to the default. */
    fun pinnedItemFor(appWidgetId: Int): Flow<PinnedWidgetItem?> = context.widgetPrefsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            readItem(prefs, typeKey(appWidgetId), idKey(appWidgetId)) ?: readItem(prefs, Keys.TYPE, Keys.ID)
        }

    private fun readItem(
        prefs: Preferences,
        typeKey: Preferences.Key<String>,
        idKey: Preferences.Key<String>
    ): PinnedWidgetItem? {
        val type = prefs[typeKey]?.let { runCatching { PinnedItemType.valueOf(it) }.getOrNull() }
        val id = prefs[idKey]
        return if (type != null && id != null) PinnedWidgetItem(type, id) else null
    }

    /** appWidgetId of every Pinned Item widget instance currently on a home screen, in a stable order. */
    suspend fun placedAppWidgetIds(): List<Int> = withContext(Dispatchers.IO) {
        val manager = AppWidgetManager.getInstance(context) ?: return@withContext emptyList()
        val component = ComponentName(context, PinnedItemWidgetReceiver::class.java)
        manager.getAppWidgetIds(component).sorted()
    }

    /** Sets the default pin. */
    suspend fun setPinned(item: PinnedWidgetItem?) {
        writePinned(Keys.TYPE, Keys.ID, item)
    }

    /** Sets the override for widget instance [appWidgetId] specifically. */
    suspend fun setPinnedFor(appWidgetId: Int, item: PinnedWidgetItem?) {
        writePinned(typeKey(appWidgetId), idKey(appWidgetId), item)
    }

    private suspend fun writePinned(
        typeKey: Preferences.Key<String>,
        idKey: Preferences.Key<String>,
        item: PinnedWidgetItem?
    ) {
        context.widgetPrefsDataStore.edit { prefs ->
            if (item == null) {
                prefs.remove(typeKey)
                prefs.remove(idKey)
            } else {
                prefs[typeKey] = item.type.name
                prefs[idKey] = item.id
            }
        }
    }

    /** Toggles the default pin. */
    suspend fun togglePinned(item: PinnedWidgetItem) {
        val current = pinnedItem.first()
        setPinned(if (current == item) null else item)
    }

    /** Toggles the override for widget instance [appWidgetId] specifically. */
    suspend fun togglePinnedFor(appWidgetId: Int, item: PinnedWidgetItem) {
        val current = pinnedItemFor(appWidgetId).first()
        setPinnedFor(appWidgetId, if (current == item) null else item)
    }
}
