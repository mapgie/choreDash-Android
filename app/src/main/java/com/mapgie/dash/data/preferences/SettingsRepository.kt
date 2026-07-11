package com.mapgie.dash.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.CadenceBucket
import com.mapgie.dash.data.model.ReminderLabelStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

val DEFAULT_FAB_ORDER = listOf(AddMenuOption.TASK, AddMenuOption.CHORE, AddMenuOption.REMINDER)

data class AppSettings(
    val supabaseUrl: String = "",
    val supabaseKey: String = "",
    val ownerHandle: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // High-contrast (WCAG) variants of the built-in palettes
    val wcagMode: Boolean = false,
    val zenMode: Boolean = false,
    val showDueCountdown: Boolean = false,
    val deliveryMode: String = "NOTIFICATION",
    // Colour theme selection — name of AppTheme enum entry (SAGE, CORAL, TEAL, CUSTOM)
    val appTheme: String = "MIST",
    // Custom HSL values for AppTheme.CUSTOM
    val customPrimaryHue: Float = 150f,
    val customPrimarySaturation: Float = 0.5f,
    val customPrimaryLightness: Float = 0.4f,
    val customSecondaryHue: Float = 120f,
    val customSecondarySaturation: Float = 0.4f,
    val customSecondaryLightness: Float = 0.4f,
    val customTertiaryHue: Float = 200f,
    val customTertiarySaturation: Float = 0.4f,
    val customTertiaryLightness: Float = 0.4f,
    // Custom background overrides per mode (ARGB; 0 = derived from primary hue)
    val customLightBackgroundArgb: Int = 0,
    val customDarkBackgroundArgb: Int = 0,
    // ID of the active saved custom colour profile (-1 = none)
    val customActiveProfileId: Long = -1L,
    // Widget customisation
    val widgetContentType: String = "CHORES",    // CHORES | TASKS | REMINDERS
    val widgetPriorityFilter: String = "ALL",    // ALL | RED | AMBER
    val widgetOwnerFilter: String = "EVERYBODY", // EVERYBODY | MINE
    // Display — grouping
    val groupChoresByCategory: Boolean = true,
    val groupTasksByCategory: Boolean = true,
    // Display — smart chore visibility: hide chores until they are within the per-cadence
    // lead time (days before due) of their next due date
    val smartChoreVisibility: Boolean = true,
    val choreLeadDays: Map<CadenceBucket, Int> =
        CadenceBucket.entries.associateWith { it.defaultLeadDays },
    // Display — hide tasks not due soon (-1 = disabled, positive = threshold in days)
    val taskHideThresholdDays: Int = -1,
    // Order the quick-add FAB's menu items appear in, top to bottom
    val fabOrder: List<AddMenuOption> = DEFAULT_FAB_ORDER,
    // Wording used for the reminders feature throughout the UI
    val reminderLabel: ReminderLabelStyle = ReminderLabelStyle.REMINDERS,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SUPABASE_URL                   = stringPreferencesKey("supabase_url")
        val SUPABASE_KEY                   = stringPreferencesKey("supabase_key")
        val OWNER_HANDLE                   = stringPreferencesKey("owner_handle")
        val THEME_MODE                     = stringPreferencesKey("theme_mode")
        val WCAG_MODE                      = booleanPreferencesKey("wcag_mode")
        val ZEN_MODE                       = booleanPreferencesKey("zen_mode")
        val SHOW_DUE_COUNTDOWN             = booleanPreferencesKey("show_due_countdown")
        val DELIVERY_MODE                  = stringPreferencesKey("delivery_mode")
        val APP_THEME                      = stringPreferencesKey("app_theme")
        val CUSTOM_PRIMARY_HUE             = floatPreferencesKey("custom_primary_hue")
        val CUSTOM_PRIMARY_SATURATION      = floatPreferencesKey("custom_primary_saturation")
        val CUSTOM_PRIMARY_LIGHTNESS       = floatPreferencesKey("custom_primary_lightness")
        val CUSTOM_SECONDARY_HUE           = floatPreferencesKey("custom_secondary_hue")
        val CUSTOM_SECONDARY_SATURATION    = floatPreferencesKey("custom_secondary_saturation")
        val CUSTOM_SECONDARY_LIGHTNESS     = floatPreferencesKey("custom_secondary_lightness")
        val CUSTOM_TERTIARY_HUE            = floatPreferencesKey("custom_tertiary_hue")
        val CUSTOM_TERTIARY_SATURATION     = floatPreferencesKey("custom_tertiary_saturation")
        val CUSTOM_TERTIARY_LIGHTNESS      = floatPreferencesKey("custom_tertiary_lightness")
        val CUSTOM_LIGHT_BG_ARGB           = intPreferencesKey("custom_light_bg_argb")
        val CUSTOM_DARK_BG_ARGB            = intPreferencesKey("custom_dark_bg_argb")
        val CUSTOM_ACTIVE_PROFILE_ID       = longPreferencesKey("custom_active_profile_id")
        val WIDGET_CONTENT_TYPE            = stringPreferencesKey("widget_content_type")
        val WIDGET_PRIORITY_FILTER         = stringPreferencesKey("widget_priority_filter")
        val WIDGET_OWNER_FILTER            = stringPreferencesKey("widget_owner_filter")
        val GROUP_CHORES_BY_CATEGORY       = booleanPreferencesKey("group_chores_by_category")
        val GROUP_TASKS_BY_CATEGORY        = booleanPreferencesKey("group_tasks_by_category")
        // Legacy single-day chore threshold; read only to migrate into smart visibility
        val CHORE_HIDE_THRESHOLD_DAYS      = intPreferencesKey("chore_hide_threshold_days")
        val TASK_HIDE_THRESHOLD_DAYS       = intPreferencesKey("task_hide_threshold_days")
        val SMART_CHORE_VISIBILITY         = booleanPreferencesKey("smart_chore_visibility")
        val FAB_ORDER                      = stringPreferencesKey("fab_order")
        val REMINDER_LABEL                 = stringPreferencesKey("reminder_label")

        fun choreLeadDays(bucket: CadenceBucket) =
            intPreferencesKey("chore_lead_days_${bucket.name.lowercase()}")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            // Migration from the legacy single-day chore threshold: an explicitly disabled
            // legacy setting (-1) keeps smart visibility off; a configured legacy value
            // seeds every bucket's lead time until the user tunes them individually.
            val legacyChoreThreshold = prefs[Keys.CHORE_HIDE_THRESHOLD_DAYS]
            AppSettings(
                supabaseUrl                 = prefs[Keys.SUPABASE_URL] ?: "",
                supabaseKey                 = prefs[Keys.SUPABASE_KEY] ?: "",
                ownerHandle                 = prefs[Keys.OWNER_HANDLE] ?: "",
                themeMode                   = prefs[Keys.THEME_MODE]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                wcagMode                    = prefs[Keys.WCAG_MODE] ?: false,
                zenMode                     = prefs[Keys.ZEN_MODE] ?: false,
                showDueCountdown            = prefs[Keys.SHOW_DUE_COUNTDOWN] ?: false,
                deliveryMode                = prefs[Keys.DELIVERY_MODE] ?: "NOTIFICATION",
                // Normalise old per-mode entries to palette names for backward compatibility
                appTheme                    = when (val raw = prefs[Keys.APP_THEME] ?: "MIST") {
                    "SYSTEM_DEFAULT", "SAGE_LIGHT", "SAGE_DARK" -> "SAGE"
                    "CORAL_LIGHT", "CORAL_DARK", "CORAL_SYSTEM" -> "CORAL"
                    "TEAL_LIGHT", "TEAL_DARK", "TEAL_SYSTEM"   -> "TEAL"
                    else -> raw
                },
                customPrimaryHue            = prefs[Keys.CUSTOM_PRIMARY_HUE] ?: 150f,
                customPrimarySaturation     = prefs[Keys.CUSTOM_PRIMARY_SATURATION] ?: 0.5f,
                customPrimaryLightness      = prefs[Keys.CUSTOM_PRIMARY_LIGHTNESS] ?: 0.4f,
                customSecondaryHue          = prefs[Keys.CUSTOM_SECONDARY_HUE] ?: 120f,
                customSecondarySaturation   = prefs[Keys.CUSTOM_SECONDARY_SATURATION] ?: 0.4f,
                customSecondaryLightness    = prefs[Keys.CUSTOM_SECONDARY_LIGHTNESS] ?: 0.4f,
                customTertiaryHue           = prefs[Keys.CUSTOM_TERTIARY_HUE] ?: 200f,
                customTertiarySaturation    = prefs[Keys.CUSTOM_TERTIARY_SATURATION] ?: 0.4f,
                customTertiaryLightness     = prefs[Keys.CUSTOM_TERTIARY_LIGHTNESS] ?: 0.4f,
                customLightBackgroundArgb   = prefs[Keys.CUSTOM_LIGHT_BG_ARGB] ?: 0,
                customDarkBackgroundArgb    = prefs[Keys.CUSTOM_DARK_BG_ARGB] ?: 0,
                customActiveProfileId       = prefs[Keys.CUSTOM_ACTIVE_PROFILE_ID] ?: -1L,
                widgetContentType           = prefs[Keys.WIDGET_CONTENT_TYPE] ?: "CHORES",
                widgetPriorityFilter        = prefs[Keys.WIDGET_PRIORITY_FILTER] ?: "ALL",
                widgetOwnerFilter           = prefs[Keys.WIDGET_OWNER_FILTER] ?: "EVERYBODY",
                groupChoresByCategory       = prefs[Keys.GROUP_CHORES_BY_CATEGORY] ?: true,
                groupTasksByCategory        = prefs[Keys.GROUP_TASKS_BY_CATEGORY] ?: true,
                smartChoreVisibility        = prefs[Keys.SMART_CHORE_VISIBILITY]
                    ?: (legacyChoreThreshold == null || legacyChoreThreshold >= 0),
                choreLeadDays               = CadenceBucket.entries.associateWith { bucket ->
                    prefs[Keys.choreLeadDays(bucket)]
                        ?: legacyChoreThreshold?.takeIf { it >= 0 }
                        ?: bucket.defaultLeadDays
                },
                taskHideThresholdDays       = prefs[Keys.TASK_HIDE_THRESHOLD_DAYS] ?: -1,
                fabOrder                    = prefs[Keys.FAB_ORDER]?.let { parseFabOrder(it) } ?: DEFAULT_FAB_ORDER,
                reminderLabel               = prefs[Keys.REMINDER_LABEL]
                    ?.let { runCatching { ReminderLabelStyle.valueOf(it) }.getOrNull() }
                    ?: ReminderLabelStyle.REMINDERS,
            )
        }

    // Tolerates unknown/duplicate/missing entries (e.g. after an app update adds a
    // new AddMenuOption) by de-duplicating and appending any missing options in
    // default order, rather than discarding a saved order outright.
    private fun parseFabOrder(raw: String): List<AddMenuOption> {
        val saved = raw.split(",").mapNotNull { name -> runCatching { AddMenuOption.valueOf(name) }.getOrNull() }.distinct()
        val missing = DEFAULT_FAB_ORDER.filterNot { it in saved }
        return saved + missing
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

    suspend fun setWcagMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WCAG_MODE] = enabled }
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

    suspend fun setCustomHSL(
        primaryH: Float,   primaryS: Float,   primaryL: Float,
        secondaryH: Float, secondaryS: Float, secondaryL: Float,
        tertiaryH: Float,  tertiaryS: Float,  tertiaryL: Float,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_PRIMARY_HUE]          = primaryH
            prefs[Keys.CUSTOM_PRIMARY_SATURATION]   = primaryS
            prefs[Keys.CUSTOM_PRIMARY_LIGHTNESS]    = primaryL
            prefs[Keys.CUSTOM_SECONDARY_HUE]        = secondaryH
            prefs[Keys.CUSTOM_SECONDARY_SATURATION] = secondaryS
            prefs[Keys.CUSTOM_SECONDARY_LIGHTNESS]  = secondaryL
            prefs[Keys.CUSTOM_TERTIARY_HUE]         = tertiaryH
            prefs[Keys.CUSTOM_TERTIARY_SATURATION]  = tertiaryS
            prefs[Keys.CUSTOM_TERTIARY_LIGHTNESS]   = tertiaryL
        }
    }

    suspend fun setCustomBackgroundArgbs(lightArgb: Int, darkArgb: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_LIGHT_BG_ARGB] = lightArgb
            prefs[Keys.CUSTOM_DARK_BG_ARGB]  = darkArgb
        }
    }

    suspend fun setCustomActiveProfileId(id: Long) {
        context.dataStore.edit { it[Keys.CUSTOM_ACTIVE_PROFILE_ID] = id }
    }

    suspend fun setWidgetContentType(type: String) {
        context.dataStore.edit { it[Keys.WIDGET_CONTENT_TYPE] = type }
    }

    suspend fun setWidgetPriorityFilter(filter: String) {
        context.dataStore.edit { it[Keys.WIDGET_PRIORITY_FILTER] = filter }
    }

    suspend fun setWidgetOwnerFilter(filter: String) {
        context.dataStore.edit { it[Keys.WIDGET_OWNER_FILTER] = filter }
    }

    suspend fun setGroupChoresByCategory(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GROUP_CHORES_BY_CATEGORY] = enabled }
    }

    suspend fun setGroupTasksByCategory(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GROUP_TASKS_BY_CATEGORY] = enabled }
    }

    suspend fun setSmartChoreVisibility(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SMART_CHORE_VISIBILITY] = enabled }
    }

    suspend fun setChoreLeadDays(bucket: CadenceBucket, days: Int) {
        context.dataStore.edit { it[Keys.choreLeadDays(bucket)] = days }
    }

    suspend fun resetChoreLeadDays() {
        // Write defaults explicitly rather than removing the keys, so the reset also
        // clears any lead times seeded from the legacy threshold migration.
        context.dataStore.edit { prefs ->
            CadenceBucket.entries.forEach { bucket ->
                prefs[Keys.choreLeadDays(bucket)] = bucket.defaultLeadDays
            }
        }
    }

    suspend fun setTaskHideThresholdDays(days: Int) {
        context.dataStore.edit { it[Keys.TASK_HIDE_THRESHOLD_DAYS] = days }
    }

    suspend fun setFabOrder(order: List<AddMenuOption>) {
        context.dataStore.edit { it[Keys.FAB_ORDER] = order.joinToString(",") { option -> option.name } }
    }

    suspend fun setReminderLabel(style: ReminderLabelStyle) {
        context.dataStore.edit { it[Keys.REMINDER_LABEL] = style.name }
    }
}
