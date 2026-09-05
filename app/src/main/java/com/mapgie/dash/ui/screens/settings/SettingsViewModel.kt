package com.mapgie.dash.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.data.database.dao.CustomColorThemeDao
import com.mapgie.dash.data.database.entities.CustomColorTheme
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.CadenceBucket
import com.mapgie.dash.data.model.CategoryCatalog
import com.mapgie.dash.data.model.CategoryStyle
import com.mapgie.dash.data.model.ColourChoresBy
import com.mapgie.dash.data.model.GENERAL_CATEGORY
import com.mapgie.dash.data.model.ReminderLabelStyle
import com.mapgie.dash.data.model.Severity
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.data.preferences.AppSettings
import com.mapgie.dash.data.preferences.CategoryStyleStore
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.preferences.ThemeMode
import com.mapgie.dash.data.repository.ChoreRepository
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

/** How many chores and tasks currently carry a category, for Settings › Categories. */
data class CategoryUsage(val chores: Int = 0, val tasks: Int = 0) {
    val total: Int get() = chores + tasks

    /** "2 chores · 1 task", or "No chores yet". */
    val label: String
        get() {
            val parts = listOfNotNull(
                chores.takeIf { it > 0 }?.let { if (it == 1) "1 chore" else "$it chores" },
                tasks.takeIf { it > 0 }?.let { if (it == 1) "1 task" else "$it tasks" },
            )
            return if (parts.isEmpty()) "No chores yet" else parts.joinToString(" · ")
        }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val choreRepository: ChoreRepository,
    private val categoryStyleStore: CategoryStyleStore,
    private val customColorThemeDao: CustomColorThemeDao,
) : ViewModel() {

    /** The per-device category catalog (order, icons, colours). */
    val catalog: StateFlow<CategoryCatalog> = categoryStyleStore.catalog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryCatalog())

    private val _categoryUsage = MutableStateFlow<Map<String, CategoryUsage>>(emptyMap())
    /** Category name (as stored) to how many chores and tasks use it. */
    val categoryUsage: StateFlow<Map<String, CategoryUsage>> = _categoryUsage.asStateFlow()

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

    fun setWcagMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWcagMode(enabled) }
    }

    fun setHelpSeen(seen: Boolean) {
        viewModelScope.launch { settingsRepository.setHelpSeen(seen) }
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

    /** Persists the custom background overrides (ARGB; 0 = Auto) to DataStore. */
    fun setCustomBackgroundArgbs(lightArgb: Int, darkArgb: Int) {
        viewModelScope.launch {
            settingsRepository.setCustomBackgroundArgbs(lightArgb, darkArgb)
        }
    }

    /**
     * Saves the current custom colours as a new named theme profile, recording
     * the active brightness mode so loading restores the same look.
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
                mode                 = s.themeMode.name,
                lightBackgroundArgb  = s.customLightBackgroundArgb,
                darkBackgroundArgb   = s.customDarkBackgroundArgb,
            )
            val id = customColorThemeDao.upsert(theme)
            settingsRepository.setCustomActiveProfileId(id)
        }
    }

    /**
     * Loads a saved profile: writes its colours to DataStore, restores the
     * brightness mode it was saved under, marks it as active, and switches the
     * app theme to [AppTheme.CUSTOM].
     */
    fun loadCustomColorTheme(theme: CustomColorTheme) {
        viewModelScope.launch {
            settingsRepository.setCustomHSL(
                theme.primaryHue,   theme.primarySaturation,   theme.primaryLightness,
                theme.secondaryHue, theme.secondarySaturation, theme.secondaryLightness,
                theme.tertiaryHue,  theme.tertiarySaturation,  theme.tertiaryLightness,
            )
            settingsRepository.setCustomBackgroundArgbs(
                theme.lightBackgroundArgb, theme.darkBackgroundArgb,
            )
            runCatching { ThemeMode.valueOf(theme.mode) }.getOrNull()
                ?.let { settingsRepository.setThemeMode(it) }
            settingsRepository.setCustomActiveProfileId(theme.id)
            settingsRepository.setAppTheme(AppTheme.CUSTOM.name)
        }
    }

    /**
     * Deletes a saved profile. If the deleted profile was active, only the
     * active-profile pointer is cleared; the current custom colours stay
     * applied (they live in DataStore, not the deleted row).
     */
    fun deleteCustomColorTheme(theme: CustomColorTheme) {
        viewModelScope.launch {
            customColorThemeDao.delete(theme)
            if (settings.value?.customActiveProfileId == theme.id) {
                settingsRepository.setCustomActiveProfileId(-1L)
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
                    mode                = s.themeMode.name,
                    lightBackgroundArgb = s.customLightBackgroundArgb,
                    darkBackgroundArgb  = s.customDarkBackgroundArgb,
                )
            )
        }
    }

    // ── Colours ───────────────────────────────────────────────────────────────

    fun setColourSpineBy(mode: ColourChoresBy) {
        viewModelScope.launch { settingsRepository.setColourSpineBy(mode) }
    }

    fun setColourIconBy(mode: ColourChoresBy) {
        viewModelScope.launch { settingsRepository.setColourIconBy(mode) }
    }

    fun setSeveritySwatch(severity: Severity, swatch: Swatch) {
        viewModelScope.launch { settingsRepository.setSeveritySwatch(severity, swatch) }
    }

    // ── Categories ────────────────────────────────────────────────────────────

    /** Counts chores and tasks per category so rows can say "2 chores · 1 task". */
    fun loadCategoryUsage() {
        viewModelScope.launch {
            runCatching {
                val chores = choreRepository.load().let { it.active + it.archived }
                val tasks = taskRepository.loadTasks().filter { it.archivedAt == null }
                val usage = mutableMapOf<String, CategoryUsage>()
                fun key(name: String) = name.trim().lowercase()
                val names = mutableMapOf<String, String>()
                chores.mapNotNull { it.category?.takeIf { c -> c.isNotBlank() } }.forEach { c ->
                    names.putIfAbsent(key(c), c.trim())
                    usage[key(c)] = (usage[key(c)] ?: CategoryUsage()).let { u -> u.copy(chores = u.chores + 1) }
                }
                tasks.mapNotNull { it.category?.takeIf { c -> c.isNotBlank() } }.forEach { c ->
                    names.putIfAbsent(key(c), c.trim())
                    usage[key(c)] = (usage[key(c)] ?: CategoryUsage()).let { u -> u.copy(tasks = u.tasks + 1) }
                }
                _categoryUsage.value = usage.mapKeys { (k, _) -> names[k] ?: k }
            }
        }
    }

    fun setCategoryOrder(order: List<String>) {
        viewModelScope.launch { categoryStyleStore.update { it.withOrder(order) } }
    }

    fun setCategoryStyle(name: String, style: CategoryStyle) {
        viewModelScope.launch { categoryStyleStore.update { it.withStyle(name, style) } }
    }

    fun addCategory(name: String) {
        viewModelScope.launch { categoryStyleStore.update { it.added(name) } }
    }

    /** Renames the category on every chore and task that carries it, then in the catalog. */
    fun renameCategory(from: String, to: String) {
        val target = to.trim()
        if (target.isBlank() || target.equals(from, ignoreCase = false)) return
        viewModelScope.launch {
            runCatching {
                choreRepository.moveCategory(from, target)
                taskRepository.moveCategory(from, target)
                categoryStyleStore.update { it.renamed(from, target) }
                loadCategoryUsage()
            }.onFailure { e -> _saveError.value = e.message }
        }
    }

    /** Deletes a category: its chores and tasks move to General, then the catalog forgets it. */
    fun deleteCategory(name: String) {
        if (name.equals(GENERAL_CATEGORY, ignoreCase = true)) return
        viewModelScope.launch {
            runCatching {
                choreRepository.moveCategory(name, GENERAL_CATEGORY)
                taskRepository.moveCategory(name, GENERAL_CATEGORY)
                categoryStyleStore.update { it.without(name) }
                loadCategoryUsage()
            }.onFailure { e -> _saveError.value = e.message }
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

    fun setSmartChoreVisibility(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSmartChoreVisibility(enabled) }
    }

    fun setChoreLeadDays(bucket: CadenceBucket, days: Int) {
        viewModelScope.launch { settingsRepository.setChoreLeadDays(bucket, days) }
    }

    fun resetChoreLeadDays() {
        viewModelScope.launch { settingsRepository.resetChoreLeadDays() }
    }

    fun setTaskHideThresholdDays(days: Int) {
        viewModelScope.launch { settingsRepository.setTaskHideThresholdDays(days) }
    }

    // ── Quick add (FAB) ──────────────────────────────────────────────────────

    fun setFabOrder(order: List<AddMenuOption>) {
        viewModelScope.launch { settingsRepository.setFabOrder(order) }
    }

    fun setReminderLabel(style: ReminderLabelStyle) {
        viewModelScope.launch { settingsRepository.setReminderLabel(style) }
    }
}
