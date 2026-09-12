package com.mapgie.dash.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.BuildConfig
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.ReminderLabelStyle
import com.mapgie.dash.data.preferences.DEFAULT_FAB_ORDER
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashNavUiState(
    val hasOutstandingReminders: Boolean = false,
    val fabOrder: List<AddMenuOption> = DEFAULT_FAB_ORDER,
    val reminderLabel: ReminderLabelStyle = ReminderLabelStyle.REMINDERS,
    /**
     * True until the first-run welcome sheet has been dismissed. The sheet waits
     * for credentials so a fresh install sees the connection screen first.
     */
    val showWelcome: Boolean = false,
)

/**
 * Whether to show the "What's New" changelog on open. True only when the app has
 * been updated in place from a previously recorded version: a fresh install
 * ([lastSeen] == 0) gets the welcome sheet instead, and a same or older version
 * shows nothing. Pure so the rule is covered by [UpdateGateTest].
 */
internal fun showWhatsNewOnUpdate(lastSeen: Int, current: Int): Boolean =
    lastSeen in 1 until current

@HiltViewModel
class DashNavViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<DashNavUiState> = combine(
        reminderRepository.outstandingRemindersFlow.map { it.isNotEmpty() },
        settingsRepository.settings,
    ) { hasOutstandingReminders, settings ->
        DashNavUiState(
            hasOutstandingReminders = hasOutstandingReminders,
            fabOrder = settings.fabOrder,
            reminderLabel = settings.reminderLabel,
            showWelcome = !settings.helpSeen && settings.supabaseUrl.isNotBlank(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashNavUiState())

    private val _showWhatsNew = MutableStateFlow(false)
    /** One-shot: true once per version bump, until dismissed. */
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew.asStateFlow()

    init {
        // Read the last-opened version once, decide whether this launch is an
        // update, then record the current version so it fires only once.
        viewModelScope.launch {
            val lastSeen = settingsRepository.settings.first().lastSeenVersionCode
            if (showWhatsNewOnUpdate(lastSeen, BuildConfig.VERSION_CODE)) {
                _showWhatsNew.value = true
            }
            if (lastSeen != BuildConfig.VERSION_CODE) {
                settingsRepository.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
            }
        }
    }

    fun markWelcomeSeen() {
        viewModelScope.launch { settingsRepository.setHelpSeen(true) }
    }

    fun dismissWhatsNew() {
        _showWhatsNew.value = false
    }
}
