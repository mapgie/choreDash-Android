package com.mapgie.dash.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val supabaseUrl: String = "",
    val supabaseKey: String = "",
    val ownerHandle: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SUPABASE_URL = stringPreferencesKey("supabase_url")
        val SUPABASE_KEY = stringPreferencesKey("supabase_key")
        val OWNER_HANDLE = stringPreferencesKey("owner_handle")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                supabaseUrl = prefs[Keys.SUPABASE_URL] ?: "",
                supabaseKey = prefs[Keys.SUPABASE_KEY] ?: "",
                ownerHandle = prefs[Keys.OWNER_HANDLE] ?: "",
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM
            )
        }

    suspend fun saveCredentials(url: String, key: String, owner: String) {
        context.dataStore.edit {
            it[Keys.SUPABASE_URL] = url.trim()
            it[Keys.SUPABASE_KEY] = key.trim()
            it[Keys.OWNER_HANDLE] = owner.trim()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}
