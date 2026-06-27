package com.mapgie.dash.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.data.database.dao.CustomColorThemeDao
import com.mapgie.dash.data.database.entities.CustomColorTheme
import com.mapgie.dash.data.preferences.AppSettings
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.preferences.ThemeMode
import com.mapgie.dash.data.repository.TaskRepository
import com.mapgie.dash.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val customColorThemeDao: CustomColorThemeDao,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Live list of all user-saved custom colour themes. */
    val customColorThemes: StateFlow<List<CustomColorTheme>> = customColorThemeDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _owners = MutableStateFlow<List<String>>(emptyList())
    val owners: StateFlow<List<String>> = _owners.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    fun saveCredentials(url: String, key: String, owner: String) {
        viewModelScope.launch {
            settingsRepository.saveCredentials(url, key, owner)
            loadOwners()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDeliveryMode(mode: String) {
        viewModelScope.launch { settingsRepository.setDeliveryMode(mode) }
    }

    fun loadOwners() {
        viewModelScope.launch {
            runCatching { _owners.value = taskRepository.loadOwners() }
        }
    }

    fun clearSaveError() { _saveError.value = null }

    // ── Colour theme ──────────────────────────────────────────────────────────

    /** Persists the selected [AppTheme] to DataStore. */
    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setAppTheme(theme.name) }
    }

    /** Persists all nine custom HSL values to DataStore. */
    fun setCustomHSL(
        primaryH: Float,   primaryS: Float,   primaryL: Float,
        secondaryH: Float, secondaryS: Float, secondaryL: Float,
        tertiaryH: Float,  tertiaryS: Float,  tertiaryL: Float,
    ) {
        viewModelScope.launch {
            settingsRepository.setCustomHSL(
                primaryH, primaryS, primaryL,
                secondaryH, secondaryS, secondaryL,
                tertiaryH, tertiaryS, tertiaryL,
            )
        }
    }

    /**
     * Saves the current custom HSL values as a new named theme profile.
     * Sets [customActiveProfileId] to the upserted row ID.
     */
    fun saveCustomColorTheme(name: String) {
        viewModelScope.launch {
            val s = settings.value ?: return@launch
            val theme = CustomColorTheme(
                name                 = name,
                primaryHue           = s.customPrimaryHue,
                primarySaturation    = s.customPrimarySaturation,
                primaryLightness     = s.customPrimaryLightness,
                secondaryHue         = s.customSecondaryHue,
                secondarySaturation  = s.customSecondarySaturation,
                secondaryLightness   = s.customSecondaryLightness,
                tertiaryHue          = s.customTertiaryHue,
                tertiarySaturation   = s.customTertiarySaturation,
                tertiaryLightness    = s.customTertiaryLightness,
                mode                 = "LIGHT",
            )
            val id = customColorThemeDao.upsert(theme)
            settingsRepository.setCustomActiveProfileId(id)
        }
    }

    /**
     * Loads a saved profile: writes its HSL values to DataStore, marks it as active,
     * and switches the app theme to [AppTheme.CUSTOM].
     */
    fun loadCustomColorTheme(theme: CustomColorTheme) {
        viewModelScope.launch {
            settingsRepository.setCustomHSL(
                theme.primaryHue,   theme.primarySaturation,   theme.primaryLightness,
                theme.secondaryHue, theme.secondarySaturation, theme.secondaryLightness,
                theme.tertiaryHue,  theme.tertiarySaturation,  theme.tertiaryLightness,
            )
            settingsRepository.setCustomActiveProfileId(theme.id)
            settingsRepository.setAppTheme(AppTheme.CUSTOM.name)
        }
    }

    /**
     * Deletes a saved profile. If the deleted profile was active, resets
     * [customActiveProfileId] to -1 and reverts the app theme to [AppTheme.SAGE].
     */
    fun deleteCustomColorTheme(theme: CustomColorTheme) {
        viewModelScope.launch {
            customColorThemeDao.delete(theme)
            if (settings.value?.customActiveProfileId == theme.id) {
                settingsRepository.setCustomActiveProfileId(-1L)
                settingsRepository.setAppTheme(AppTheme.SAGE.name)
            }
        }
    }

    /**
     * Renames a saved profile by upserting it with the new name.
     * The active profile ID remains unchanged (same row, same ID).
     */
    fun renameCustomColorTheme(theme: CustomColorTheme, newName: String) {
        viewModelScope.launch {
            customColorThemeDao.upsert(theme.copy(name = newName))
        }
    }

    /**
     * Overwrites the active saved profile with the current HSL values and the given name.
     * No-ops if no profile is currently active.
     */
    fun updateCustomColorTheme(name: String) {
        viewModelScope.launch {
            val s = settings.value ?: return@launch
            val id = s.customActiveProfileId
            if (id == -1L) return@launch
            customColorThemeDao.upsert(
                CustomColorTheme(
                    id                  = id,
                    name                = name,
                    primaryHue          = s.customPrimaryHue,
                    primarySaturation   = s.customPrimarySaturation,
                    primaryLightness    = s.customPrimaryLightness,
                    secondaryHue        = s.customSecondaryHue,
                    secondarySaturation = s.customSecondarySaturation,
                    secondaryLightness  = s.customSecondaryLightness,
                    tertiaryHue         = s.customTertiaryHue,
                    tertiarySaturation  = s.customTertiarySaturation,
                    tertiaryLightness   = s.customTertiaryLightness,
                    mode                = "LIGHT",
                )
            )
        }
    }

    // ── Widget customisation ──────────────────────────────────────────────────

    fun setWidgetContentType(type: String) {
        viewModelScope.launch { settingsRepository.setWidgetContentType(type) }
    }

    fun setWidgetPriorityFilter(filter: String) {
        viewModelScope.launch { settingsRepository.setWidgetPriorityFilter(filter) }
    }

    fun setWidgetOwnerFilter(filter: String) {
        viewModelScope.launch { settingsRepository.setWidgetOwnerFilter(filter) }
    }

    // ── Display ───────────────────────────────────────────────────────────────

    fun setGroupChoresByCategory(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGroupChoresByCategory(enabled) }
    }

    fun setGroupTasksByCategory(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGroupTasksByCategory(enabled) }
    }

    fun setChoreHideThresholdDays(days: Int) {
        viewModelScope.launch { settingsRepository.setChoreHideThresholdDays(days) }
    }

    fun setTaskHideThresholdDays(days: Int) {
        viewModelScope.launch { settingsRepository.setTaskHideThresholdDays(days) }
    }
}
