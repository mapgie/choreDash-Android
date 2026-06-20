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

    /** Persists all three custom hues to DataStore. */
    fun setCustomHues(primary: Float, secondary: Float, tertiary: Float) {
        viewModelScope.launch {
            settingsRepository.setCustomHues(primary, secondary, tertiary)
        }
    }

    /**
     * Saves the current custom hues as a new named theme profile.
     * Sets [customActiveProfileId] to the upserted row ID.
     */
    fun saveCustomColorTheme(name: String) {
        viewModelScope.launch {
            val s = settings.value ?: return@launch
            val theme = CustomColorTheme(
                name         = name,
                primaryHue   = s.customPrimaryHue,
                secondaryHue = s.customSecondaryHue,
                tertiaryHue  = s.customTertiaryHue,
                mode         = "LIGHT",
            )
            val id = customColorThemeDao.upsert(theme)
            settingsRepository.setCustomActiveProfileId(id)
        }
    }

    /**
     * Loads a saved profile: writes its hues to DataStore, marks it as active,
     * and switches the app theme to [AppTheme.CUSTOM].
     */
    fun loadCustomColorTheme(theme: CustomColorTheme) {
        viewModelScope.launch {
            settingsRepository.setCustomHues(theme.primaryHue, theme.secondaryHue, theme.tertiaryHue)
            settingsRepository.setCustomActiveProfileId(theme.id)
            settingsRepository.setAppTheme(AppTheme.CUSTOM.name)
        }
    }

    /**
     * Deletes a saved profile. If the deleted profile was active, resets
     * [customActiveProfileId] to -1 and reverts the app theme to [AppTheme.SYSTEM_DEFAULT].
     */
    fun deleteCustomColorTheme(theme: CustomColorTheme) {
        viewModelScope.launch {
            customColorThemeDao.delete(theme)
            if (settings.value?.customActiveProfileId == theme.id) {
                settingsRepository.setCustomActiveProfileId(-1L)
                settingsRepository.setAppTheme(AppTheme.SYSTEM_DEFAULT.name)
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
}
