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
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val zenMode: Boolean = false,
    val showDueCountdown: Boolean = false,
    val deliveryMode: String = "NOTIFICATION",
    // Colour theme selection — name of AppTheme enum entry
    val appTheme: String = "SYSTEM_DEFAULT",
    // Custom HSL hues for AppTheme.CUSTOM
    val customPrimaryHue: Float = 150f,
    val customSecondaryHue: Float = 120f,
    val customTertiaryHue: Float = 200f,
    // ID of the active saved custom colour profile (-1 = none)
    val customActiveProfileId: Long = -1L,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SUPABASE_URL               = stringPreferencesKey("supabase_url")
        val SUPABASE_KEY               = stringPreferencesKey("supabase_key")
        val OWNER_HANDLE               = stringPreferencesKey("owner_handle")
        val THEME_MODE                 = stringPreferencesKey("theme_mode")
        val ZEN_MODE                   = booleanPreferencesKey("zen_mode")
        val SHOW_DUE_COUNTDOWN         = booleanPreferencesKey("show_due_countdown")
        val DELIVERY_MODE              = stringPreferencesKey("delivery_mode")
        val APP_THEME                  = stringPreferencesKey("app_theme")
        val CUSTOM_PRIMARY_HUE         = floatPreferencesKey("custom_primary_hue")
        val CUSTOM_SECONDARY_HUE       = floatPreferencesKey("custom_secondary_hue")
        val CUSTOM_TERTIARY_HUE        = floatPreferencesKey("custom_tertiary_hue")
        val CUSTOM_ACTIVE_PROFILE_ID   = longPreferencesKey("custom_active_profile_id")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                supabaseUrl             = prefs[Keys.SUPABASE_URL] ?: "",
                supabaseKey             = prefs[Keys.SUPABASE_KEY] ?: "",
                ownerHandle             = prefs[Keys.OWNER_HANDLE] ?: "",
                themeMode               = prefs[Keys.THEME_MODE]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                zenMode                 = prefs[Keys.ZEN_MODE] ?: false,
                showDueCountdown        = prefs[Keys.SHOW_DUE_COUNTDOWN] ?: false,
                deliveryMode            = prefs[Keys.DELIVERY_MODE] ?: "NOTIFICATION",
                appTheme                = prefs[Keys.APP_THEME] ?: "SYSTEM_DEFAULT",
                customPrimaryHue        = prefs[Keys.CUSTOM_PRIMARY_HUE] ?: 150f,
                customSecondaryHue      = prefs[Keys.CUSTOM_SECONDARY_HUE] ?: 120f,
                customTertiaryHue       = prefs[Keys.CUSTOM_TERTIARY_HUE] ?: 200f,
                customActiveProfileId   = prefs[Keys.CUSTOM_ACTIVE_PROFILE_ID] ?: -1L,
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

    suspend fun setZenMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ZEN_MODE] = enabled }
    }

    suspend fun setShowDueCountdown(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_DUE_COUNTDOWN] = enabled }
    }

    suspend fun setDeliveryMode(mode: String) {
        context.dataStore.edit { it[Keys.DELIVERY_MODE] = mode }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { it[Keys.APP_THEME] = theme }
    }

    suspend fun setCustomHues(primaryHue: Float, secondaryHue: Float, tertiaryHue: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_PRIMARY_HUE]   = primaryHue
            prefs[Keys.CUSTOM_SECONDARY_HUE] = secondaryHue
            prefs[Keys.CUSTOM_TERTIARY_HUE]  = tertiaryHue
        }
    }

    suspend fun setCustomActiveProfileId(id: Long) {
        context.dataStore.edit { it[Keys.CUSTOM_ACTIVE_PROFILE_ID] = id }
    }
}
