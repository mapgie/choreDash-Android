package com.mapgie.dash.ui.screens.chores

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.alarm.AlarmScheduler
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.ScanDto
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ChoreRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.widget.PinnedItemStore
import com.mapgie.dash.widget.PinnedItemType
import com.mapgie.dash.widget.PinnedWidgetItem
import com.mapgie.dash.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

enum class ChoreFilter(val label: String) {
    ALL("All"),
    OVERDUE("Overdue"),
    SOON("Soon")
}
enum class OwnerFilter { ME, ALL }

data class RecentScan(val choreLabel: String, val scanId: String)

data class ChoreUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val active: List<Chore> = emptyList(),
    val archived: List<Chore> = emptyList(),
    val owners: List<String> = emptyList(),
    val filter: ChoreFilter = ChoreFilter.ALL,
    val groupByCategory: Boolean = true,
    val ownerFilter: OwnerFilter = OwnerFilter.ALL,
    val ownerHandle: String = "",
    val zenMode: Boolean = false,
    val zenSortAscending: Boolean = true,
    val showDueCountdown: Boolean = false,
    val pendingNfcTagId: String? = null,
    val recentScan: RecentScan? = null,
    val pinnedChoreId: String? = null,
    val scanHistory: List<ScanDto> = emptyList()
) {
    val displayed: List<Chore>
        get() {
            var result = active
            if (ownerFilter == OwnerFilter.ME && ownerHandle.isNotBlank()) {
                result = result.filter { it.owner == null || it.owner == ownerHandle }
            }
            result = when (filter) {
                ChoreFilter.ALL -> result
                ChoreFilter.OVERDUE -> result.filter {
                    it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER
                }
                ChoreFilter.SOON -> result.filter { it.status == ChoreStatus.AGING }
            }
            if (zenMode) {
                // Zen sort: ascending = most overdue first (null/oldest lastScanned first)
                result = if (zenSortAscending) {
                    result.sortedWith(compareBy(nullsFirst<Instant>()) { it.lastScanned })
                } else {
                    result.sortedWith(compareByDescending(nullsLast<Instant>()) { it.lastScanned })
                }
            } else if (showDueCountdown) {
                // When showing the due countdown, surface the most urgent chores first
                // (matches choreDash web's due-button behaviour of sorting overdue to the top)
                result = result.sortedWith(
                    compareBy(
                        { it.status != ChoreStatus.NEVER && it.status != ChoreStatus.STALE },
                        { it.lastScanned ?: Instant.MIN }
                    )
                )
            }
            return result
        }

    val categories: List<String>
        get() = (active + archived).mapNotNull { it.category }.filter { it.isNotBlank() }.distinct().sorted()
}

@HiltViewModel
class ChoreListViewModel @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val pinnedItemStore: PinnedItemStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChoreUiState(loading = true))
    val uiState: StateFlow<ChoreUiState> = _uiState.asStateFlow()

    init {
        // Keep ownerHandle in sync so ME filter works correctly across settings changes
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        ownerHandle = settings.ownerHandle,
                        zenMode = settings.zenMode,
                        showDueCountdown = settings.showDueCountdown
                    )
                }
            }
        }
        viewModelScope.launch {
            pinnedItemStore.pinnedItem.collect { pinned ->
                val pinnedChoreId = pinned?.takeIf { it.type == PinnedItemType.CHORE }?.id
                _uiState.update { it.copy(pinnedChoreId = pinnedChoreId) }
            }
        }
        load()
    }

    fun togglePin(choreId: String) {
        viewModelScope.launch {
            pinnedItemStore.togglePinned(PinnedWidgetItem(PinnedItemType.CHORE, choreId))
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { choreRepository.load() }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            active = result.active,
                            archived = result.archived,
                            owners = result.owners
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, error = e.message ?: "Failed to load chores")
                    }
                }
        }
    }

    fun logChore(tagId: String, at: Instant? = null) {
        viewModelScope.launch {
            val choreLabel = (_uiState.value.active + _uiState.value.archived)
                .find { it.tagId == tagId }?.label ?: tagId
            runCatching {
                val scanId = choreRepository.logChore(tagId, at ?: Instant.now())
                load()
                _uiState.update { it.copy(recentScan = RecentScan(choreLabel, scanId)) }
                WidgetUpdater.updateAll(appContext)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun undoLog(scanId: String) {
        viewModelScope.launch {
            runCatching {
                choreRepository.deleteScan(scanId)
                load()
                WidgetUpdater.updateAll(appContext)
            }
        }
    }

    fun removeLastLog(chore: Chore) {
        val scanId = chore.lastScanId ?: return
        viewModelScope.launch {
            runCatching {
                choreRepository.deleteScan(scanId)
                load()
                loadScanHistory(chore.tagId)
                WidgetUpdater.updateAll(appContext)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun loadScanHistory(tagId: String) {
        viewModelScope.launch {
            runCatching { choreRepository.scanHistory(tagId) }
                .onSuccess { history -> _uiState.update { it.copy(scanHistory = history) } }
        }
    }

    fun clearScanHistory() {
        _uiState.update { it.copy(scanHistory = emptyList()) }
    }

    fun clearRecentScan() {
        _uiState.update { it.copy(recentScan = null) }
    }

    fun updateChore(tagId: String, label: String, owner: String?, intervalDays: Double?) {
        viewModelScope.launch {
            runCatching {
                choreRepository.updateTag(tagId, label, owner, intervalDays)
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addChore(tagId: String, label: String, category: String?, owner: String?, intervalDays: Double?) {
        viewModelScope.launch {
            runCatching {
                choreRepository.createTag(tagId, label, category, owner, intervalDays)
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addReminderForChore(insert: ReminderInsert) {
        viewModelScope.launch {
            runCatching {
                val reminder = reminderRepository.addReminder(insert)
                reminder.remindAtInstant()?.let { at ->
                    alarmScheduler.scheduleReminder(reminder.id, reminder.subject, at)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun archiveChore(tagId: String, archived: Boolean) {
        viewModelScope.launch {
            runCatching {
                choreRepository.archiveTag(tagId, archived)
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setPendingNfcTag(tagId: String) {
        _uiState.update { it.copy(pendingNfcTagId = tagId) }
    }

    fun clearPendingNfcTag() {
        _uiState.update { it.copy(pendingNfcTagId = null) }
    }

    fun setFilter(filter: ChoreFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setGroupBy(groupByCategory: Boolean) {
        _uiState.update { it.copy(groupByCategory = groupByCategory) }
    }

    fun setOwnerFilter(ownerFilter: OwnerFilter) {
        _uiState.update { it.copy(ownerFilter = ownerFilter) }
    }

    fun setZenMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setZenMode(enabled) }
    }

    fun setZenSort(ascending: Boolean) {
        _uiState.update { it.copy(zenSortAscending = ascending) }
    }

    fun setShowDueCountdown(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowDueCountdown(enabled) }
    }
}