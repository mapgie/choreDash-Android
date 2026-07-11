package com.mapgie.dash.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.model.ReminderLabelStyle
import com.mapgie.dash.data.preferences.DEFAULT_FAB_ORDER
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashNavUiState(
    val hasOutstandingReminders: Boolean = false,
    val fabOrder: List<AddMenuOption> = DEFAULT_FAB_ORDER,
    val reminderLabel: ReminderLabelStyle = ReminderLabelStyle.REMINDERS,
)

@HiltViewModel
class DashNavViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<DashNavUiState> = combine(
        reminderRepository.outstandingRemindersFlow.map { it.isNotEmpty() },
        settingsRepository.settings,
    ) { hasOutstandingReminders, settings ->
        DashNavUiState(
            hasOutstandingReminders = hasOutstandingReminders,
            fabOrder = settings.fabOrder,
            reminderLabel = settings.reminderLabel,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashNavUiState())
}
