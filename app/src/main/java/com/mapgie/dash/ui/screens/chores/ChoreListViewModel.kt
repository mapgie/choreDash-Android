package com.mapgie.dash.ui.screens.chores

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.alarm.AlarmScheduler
import com.mapgie.dash.data.model.CadenceBucket
import com.mapgie.dash.data.model.CategoryCatalog
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreSortKey
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.ColourChoresBy
import com.mapgie.dash.data.model.OwnerFilter
import com.mapgie.dash.data.model.ReminderInsert
import com.mapgie.dash.data.model.ScanDto
import com.mapgie.dash.data.model.SortOrder
import com.mapgie.dash.data.model.defaultSnoozeDuration
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.data.preferences.CategoryStyleStore
import com.mapgie.dash.data.preferences.ChoreSnoozeStore
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ChoreRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.widget.PinChooserState
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
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class ChoreFilter(val label: String) {
    ALL("All"),
    OVERDUE("Overdue"),
    SOON("Soon")
}
data class RecentScan(val choreLabel: String, val scanId: String)

/**
 * The last swipe-to-snooze, for the Undo snackbar. [until] is null when the
 * swipe woke the chore; [previousUntil] is what Undo restores (null = awake).
 */
data class RecentSnooze(
    val choreLabel: String,
    val tagId: String,
    val until: Instant?,
    val previousUntil: Instant?,
)

/** Label shown on a group header for chores with no category. */
const val UNCATEGORISED_LABEL = "Uncategorised"

data class ChoreUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val active: List<Chore> = emptyList(),
    val archived: List<Chore> = emptyList(),
    val owners: List<String> = emptyList(),
    val filter: ChoreFilter = ChoreFilter.ALL,
    val groupByCategory: Boolean = true,
    val ownerFilter: OwnerFilter = OwnerFilter.EVERYONE,
    val ownerHandle: String = "",
    val zenMode: Boolean = false,
    val zenSortAscending: Boolean = true,
    val sort: SortOrder<ChoreSortKey> = SortOrder(ChoreSortKey.PRESSURE),
    val showHidden: Boolean = false,
    // Off until settings load so chores aren't hidden with unconfigured lead times
    val smartVisibility: Boolean = false,
    val choreLeadDays: Map<CadenceBucket, Int> = emptyMap(),
    val colourChoresBy: ColourChoresBy = ColourChoresBy.SEVERITY,
    val catalog: CategoryCatalog = CategoryCatalog(),
    val pendingNfcTagId: String? = null,
    val recentScan: RecentScan? = null,
    /** Tag id to wake time for chores snoozed on this device. */
    val snoozes: Map<String, Instant> = emptyMap(),
    val recentSnooze: RecentSnooze? = null,
    val pinnedChoreId: String? = null,
    val scanHistory: List<ScanDto> = emptyList(),
    val pinChooser: PinChooserState? = null
) {
    private val ownerFiltered: List<Chore>
        get() = active.filter { ownerFilter.matches(it.owner, ownerHandle) }

    /** When [chore] wakes from a swipe-to-snooze, or null if it is not snoozed. */
    fun snoozedUntil(chore: Chore): Instant? =
        snoozes[chore.tagId]?.takeIf { it.isAfter(Instant.now()) }

    /** How long a swipe-to-snooze on [chore] lasts under the current visibility settings. */
    fun snoozeDurationFor(chore: Chore): Duration =
        defaultSnoozeDuration(chore.intervalDays, smartVisibility, choreLeadDays)

    private val awake: List<Chore>
        get() = ownerFiltered.filter { snoozedUntil(it) == null }

    private val snoozed: List<Chore>
        get() = ownerFiltered.filter { snoozedUntil(it) != null }

    /** How many of [hiddenChores] are there because of a snooze rather than lead time. */
    val snoozedCount: Int
        get() = snoozed.size

    /** True if this chore belongs in the main list under its cadence bucket's lead time. */
    private fun withinLeadTime(chore: Chore): Boolean {
        val last = chore.lastScanned ?: return true
        val intervalDays = chore.intervalDays ?: return true
        val bucket = CadenceBucket.forInterval(intervalDays)
        val leadDays = choreLeadDays[bucket] ?: bucket.defaultLeadDays
        val dueInstant = last.plus((intervalDays * 24).toLong(), ChronoUnit.HOURS)
        return Duration.between(Instant.now(), dueInstant).toDays() <= leadDays
    }

    /**
     * Chores kept out of the main list but revealable via the collapsed section:
     * everything beyond its lead time when smart visibility is on, otherwise only
     * the legacy distant (due 60+ days out) chores, plus anything snoozed.
     */
    val hiddenChores: List<Chore>
        get() {
            val beyondLeadTime = if (smartVisibility) {
                awake.filterNot(::withinLeadTime)
            } else {
                awake.filter { it.isDistant() }
            }
            return beyondLeadTime + snoozed
        }

    val displayed: List<Chore>
        get() {
            var result = if (smartVisibility) {
                awake.filter(::withinLeadTime)
            } else {
                awake.filter { !it.isDistant() }
            }
            result = when (filter) {
                ChoreFilter.ALL -> result
                ChoreFilter.OVERDUE -> result.filter {
                    it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER
                }
                ChoreFilter.SOON -> result.filter { it.status == ChoreStatus.AGING }
            }
            return if (zenMode) {
                // Zen sort: ascending = most overdue first (never-done, then oldest scan);
                // descending = recently done first, never-done last. Never-done is
                // ordered by an explicit first key: reversing a nulls-aware comparator
                // reverses its null placement too (see LESSONS.md).
                if (zenSortAscending) {
                    result.sortedWith(compareBy(nullsFirst<Instant>()) { it.lastScanned })
                } else {
                    result.sortedWith(
                        compareBy<Chore> { it.lastScanned == null }.thenByDescending { it.lastScanned }
                    )
                }
            } else {
                result.sortedForPill(sort)
            }
        }

    /**
     * [displayed] split into category groups in catalog order (user order, then
     * unlisted names alphabetically, General last), each group keeping the pill
     * sort. Chores with no category land in an "Uncategorised" group at the end.
     */
    val grouped: List<Pair<String, List<Chore>>>
        get() {
            val groups = displayed.groupBy { it.category?.takeIf { c -> c.isNotBlank() } }
            return groups.entries
                .sortedWith(
                    compareBy<Map.Entry<String?, List<Chore>>> { it.key == null }
                        .thenBy { catalog.rankOf(it.key) }
                        .thenBy { it.key?.lowercase() ?: "" }
                )
                .map { (category, chores) -> (category ?: UNCATEGORISED_LABEL) to chores }
        }

    /** Overdue chores in scope, shown on the Overdue chip ("Overdue · 5"). */
    val overdueCount: Int
        get() = ownerFiltered.count { it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER }

    /** "7 chores · 1 hidden" for the summary bar; the hidden part is omitted at zero. */
    val summaryLabel: String
        get() {
            val shown = displayed.size
            val hidden = hiddenChores.size
            val base = if (shown == 1) "1 chore" else "$shown chores"
            return if (hidden > 0) "$base · $hidden hidden" else base
        }

    val categories: List<String>
        get() = catalog.sorted(
            (active + archived).mapNotNull { it.category }.filter { it.isNotBlank() }
        )
}

/**
 * Orders chores for the sort pill. Every key names both directions in words, so
 * the reversed branch is written out rather than calling `reversed()` on a
 * nulls-aware comparator (LESSONS.md #35).
 */
fun List<Chore>.sortedForPill(order: SortOrder<ChoreSortKey>): List<Chore> = when (order.key) {
    ChoreSortKey.PRESSURE ->
        if (!order.reversed) {
            // Worst first: never-done, then the largest share of the window elapsed,
            // then the oldest log among ties (all overdue chores clamp at 1).
            sortedWith(
                compareBy<Chore> { it.lastScanned != null }
                    .thenByDescending { it.pressureFraction() ?: 2f }
                    .thenBy { it.lastScanned ?: Instant.MIN }
            )
        } else {
            sortedWith(
                compareBy<Chore> { it.lastScanned == null }
                    .thenBy { it.pressureFraction() ?: 2f }
                    .thenByDescending { it.lastScanned ?: Instant.MIN }
            )
        }
    ChoreSortKey.DUE ->
        if (!order.reversed) {
            sortedWith(
                compareBy<Chore> { it.dueInstant() != null }
                    .thenBy { it.dueInstant() ?: Instant.MIN }
            )
        } else {
            sortedWith(
                compareBy<Chore> { it.dueInstant() == null }
                    .thenByDescending { it.dueInstant() ?: Instant.MIN }
            )
        }
    ChoreSortKey.NAME ->
        if (!order.reversed) sortedBy { it.label.lowercase() }
        else sortedByDescending { it.label.lowercase() }
    ChoreSortKey.CATEGORY ->
        if (!order.reversed) {
            sortedWith(
                compareBy<Chore> { it.category.isNullOrBlank() }
                    .thenBy { it.category?.lowercase() ?: "" }
                    .thenBy { it.label.lowercase() }
            )
        } else {
            sortedWith(
                compareBy<Chore> { it.category.isNullOrBlank() }
                    .thenByDescending { it.category?.lowercase() ?: "" }
                    .thenBy { it.label.lowercase() }
            )
        }
}

@HiltViewModel
class ChoreListViewModel @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val pinnedItemStore: PinnedItemStore,
    private val choreSnoozeStore: ChoreSnoozeStore,
    private val categoryStyleStore: CategoryStyleStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChoreUiState(loading = true))
    val uiState: StateFlow<ChoreUiState> = _uiState.asStateFlow()

    init {
        // Keep ownerHandle in sync so the owner filter works correctly across settings changes
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        ownerHandle = settings.ownerHandle,
                        zenMode = settings.zenMode,
                        sort = settings.choreSort,
                        groupByCategory = settings.groupChoresByCategory,
                        smartVisibility = settings.smartChoreVisibility,
                        choreLeadDays = settings.choreLeadDays,
                        colourChoresBy = settings.colourChoresBy,
                    )
                }
            }
        }
        viewModelScope.launch {
            categoryStyleStore.catalog.collect { catalog ->
                _uiState.update { it.copy(catalog = catalog) }
            }
        }
        viewModelScope.launch {
            choreSnoozeStore.snoozes.collect { snoozes ->
                _uiState.update { it.copy(snoozes = snoozes) }
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
        val item = PinnedWidgetItem(PinnedItemType.CHORE, choreId)
        viewModelScope.launch {
            val placedWidgetIds = pinnedItemStore.placedAppWidgetIds()
            if (placedWidgetIds.size > 1) {
                _uiState.update { it.copy(pinChooser = PinChooserState(item, placedWidgetIds)) }
                return@launch
            }
            pinnedItemStore.togglePinned(item)
            // With at most one widget placed, that widget should always track the default
            // pin, not a stale per-instance override left over from when more were placed.
            placedWidgetIds.singleOrNull()?.let { pinnedItemStore.setPinnedFor(it, null) }
            WidgetUpdater.updateAll(appContext)
        }
    }

    /** Commits the pin to a specific widget instance chosen from [ChoreUiState.pinChooser]. */
    fun pinToWidget(appWidgetId: Int) {
        val chooser = _uiState.value.pinChooser ?: return
        _uiState.update { it.copy(pinChooser = null) }
        viewModelScope.launch {
            pinnedItemStore.togglePinnedFor(appWidgetId, chooser.item)
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun dismissPinChooser() {
        _uiState.update { it.copy(pinChooser = null) }
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
                // Doing the chore ends any snooze on it.
                choreSnoozeStore.clear(tagId)
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
        viewModelScope.launch {
            runCatching {
                // Fetch the latest scan fresh from the DB rather than relying on
                // chore.lastScanId, which can be stale if a log was added after the
                // last load() completed (e.g. race between swipe-log and sheet open).
                val latestScanId = choreRepository.scanHistory(chore.tagId, limit = 1)
                    .firstOrNull()?.id ?: return@runCatching
                choreRepository.deleteScan(latestScanId)
                load()
                loadScanHistory(chore.tagId)
                WidgetUpdater.updateAll(appContext)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /** Recent history for the log sheet; [limit] grows when the user asks for all of it. */
    fun loadScanHistory(tagId: String, limit: Long = 4) {
        viewModelScope.launch {
            runCatching { choreRepository.scanHistory(tagId, limit) }
                .onSuccess { history -> _uiState.update { it.copy(scanHistory = history) } }
        }
    }

    fun clearScanHistory() {
        _uiState.update { it.copy(scanHistory = emptyList()) }
    }

    fun clearRecentScan() {
        _uiState.update { it.copy(recentScan = null) }
    }

    fun updateChore(tagId: String, label: String, category: String?, owner: String?, intervalDays: Double?) {
        viewModelScope.launch {
            runCatching {
                choreRepository.updateTag(tagId, label, category, owner, intervalDays)
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
        viewModelScope.launch { settingsRepository.setGroupChoresByCategory(groupByCategory) }
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

    /** Sort pill choice; applied immediately and persisted. */
    fun setSort(order: SortOrder<ChoreSortKey>) {
        _uiState.update { it.copy(sort = order) }
        viewModelScope.launch { settingsRepository.setChoreSort(order) }
    }

    fun toggleShowHidden() {
        _uiState.update { it.copy(showHidden = !it.showHidden) }
    }

    /**
     * Swipe left: snooze an awake chore for its default duration, or wake a
     * snoozed one. Records the change so the snackbar can offer Undo.
     */
    fun toggleSnooze(chore: Chore) {
        val state = _uiState.value
        val previous = state.snoozedUntil(chore)
        val until = if (previous == null) Instant.now().plus(state.snoozeDurationFor(chore)) else null
        viewModelScope.launch {
            applySnooze(chore.tagId, until)
            _uiState.update { it.copy(recentSnooze = RecentSnooze(chore.label, chore.tagId, until, previous)) }
        }
    }

    fun undoSnooze(snooze: RecentSnooze) {
        viewModelScope.launch { applySnooze(snooze.tagId, snooze.previousUntil) }
    }

    fun clearRecentSnooze() {
        _uiState.update { it.copy(recentSnooze = null) }
    }

    private suspend fun applySnooze(tagId: String, until: Instant?) {
        if (until == null) choreSnoozeStore.clear(tagId) else choreSnoozeStore.snooze(tagId, until)
    }
}
