package com.mapgie.dash.ui.screens.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ChoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

enum class ChoreFilter { ALL, OVERDUE, SOON }
enum class OwnerFilter { ME, ALL }

data class RecentScan(val choreLabel: String, val scanId: String)

data class ChoreUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val active: List<Chore> = emptyList(),
    val archived: List<Chore> = emptyList(),
    val owners: List<String> = emptyList(),
    val filter: ChoreFilter = ChoreFilter.ALL,
    val groupByCategory: Boolean = false,
    val ownerFilter: OwnerFilter = OwnerFilter.ALL,
    val ownerHandle: String = "",
    val pendingNfcTagId: String? = null,
    val recentScan: RecentScan? = null
) {
    val displayed: List<Chore>
        get() {
            var result = active
            if (ownerFilter == OwnerFilter.ME && ownerHandle.isNotBlank()) {
                result = result.filter { it.owner == null || it.owner == ownerHandle }
            }
            return when (filter) {
                ChoreFilter.ALL -> result
                ChoreFilter.OVERDUE -> result.filter {
                    it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER
                }
                ChoreFilter.SOON -> result.filter { it.status == ChoreStatus.AGING }
            }
        }
}

@HiltViewModel
class ChoreListViewModel @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChoreUiState(loading = true))
    val uiState: StateFlow<ChoreUiState> = _uiState.asStateFlow()

    init {
        // Keep ownerHandle in sync so ME filter works correctly across settings changes
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(ownerHandle = settings.ownerHandle) }
            }
        }
        load()
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
            }
        }
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
}
