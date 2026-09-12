package com.mapgie.dash.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.mapgie.dash.data.preferences.SettingsRepository
import com.mapgie.dash.data.repository.ReminderRepository
import com.mapgie.dash.permission.PermissionHelper
import com.mapgie.dash.ui.components.core.PermissionBanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * A list-screen banner that appears when a permission an alarm-style reminder
 * needs is missing, and disappears the moment the user grants it (grants are
 * re-read on every resume). Shared by the Memos and Chores lists so both nudge
 * the same way. Stays quiet for an established empty list: it shows only when a
 * reminder already exists or this is first run (see [ReminderPermissionGrants.nudgeFor]).
 *
 * When the only gap is the full-screen grant the tap opens that system toggle
 * directly (one tap); otherwise it opens Settings > Reminders & alerts via
 * [onOpenReminderSettings], which lists every missing grant.
 */
@Composable
fun ReminderPermissionNudge(
    onOpenReminderSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReminderPermissionNudgeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var grants by remember { mutableStateOf(PermissionHelper.reminderGrants(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) grants = PermissionHelper.reminderGrants(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val nudge = grants.nudgeFor(
        deliveryMode = state.deliveryMode,
        plural = state.plural,
        hasReminders = state.hasReminders,
        firstRun = state.firstRun,
    ) ?: return

    PermissionBanner(
        text = nudge.text,
        onClick = {
            if (nudge.fullScreenOnly) {
                context.startActivity(PermissionHelper.fullScreenIntentSettingsIntent(context))
            } else {
                onOpenReminderSettings()
            }
        },
        modifier = modifier,
    )
}

@HiltViewModel
class ReminderPermissionNudgeViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    reminderRepository: ReminderRepository,
) : ViewModel() {

    data class State(
        val deliveryMode: String = "NOTIFICATION",
        val plural: String = "reminders",
        val hasReminders: Boolean = false,
        val firstRun: Boolean = false,
    )

    val state: StateFlow<State> = combine(
        settingsRepository.settings,
        reminderRepository.remindersFlow,
    ) { settings, reminders ->
        State(
            deliveryMode = settings.deliveryMode,
            plural = settings.reminderLabel.displayName.lowercase(),
            hasReminders = reminders.isNotEmpty(),
            firstRun = !settings.helpSeen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())
}
