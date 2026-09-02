package com.mapgie.dash.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.ReminderLabelStyle
import com.mapgie.dash.ui.components.HelpContent

/** Settings › Help: the chores / tasks / memos explanation plus a few tips. */
@Composable
internal fun HelpSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val reminderLabel = (settings?.reminderLabel ?: ReminderLabelStyle.REMINDERS).displayName

    SettingsSubScreenScaffold(title = "Help", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsSectionLabel("What goes where")
            HelpContent(reminderLabel = reminderLabel)
            Spacer(Modifier.height(8.dp))
        }
    }
}
