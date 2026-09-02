package com.mapgie.dash.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mapgie.dash.data.model.CategoryCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.categoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_categories")

/**
 * Persists the per-device [CategoryCatalog] (Settings › Categories): the order
 * categories are grouped in, each category's icon and colour, and categories
 * created before any item uses them. Stored as one JSON document so a rename or
 * reorder is a single atomic write.
 */
@Singleton
class CategoryStyleStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val CATALOG = stringPreferencesKey("catalog_json")
    }

    val catalog: Flow<CategoryCatalog> = context.categoryDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> decode(prefs[Keys.CATALOG]) }

    private fun decode(raw: String?): CategoryCatalog =
        raw?.let { runCatching { json.decodeFromString<CategoryCatalog>(it) }.getOrNull() } ?: CategoryCatalog()

    /** Applies [transform] to the stored catalog and writes the result back. */
    suspend fun update(transform: (CategoryCatalog) -> CategoryCatalog) {
        context.categoryDataStore.edit { prefs ->
            val next = transform(decode(prefs[Keys.CATALOG]))
            prefs[Keys.CATALOG] = json.encodeToString(next)
        }
    }
}
