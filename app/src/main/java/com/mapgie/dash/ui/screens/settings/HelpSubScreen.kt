package com.mapgie.dash.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.BuildConfig
import com.mapgie.dash.data.model.ReminderLabelStyle
import com.mapgie.dash.ui.components.HelpContent
import com.mapgie.dash.ui.components.HelpGettingAround
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.PillShape

/**
 * Settings › Help: two pages behind a segmented header. "What goes where" is
 * the chores / tasks / memos explanation; "Getting around" tours the controls.
 */
@Composable
internal fun HelpSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val reminderLabel = (settings?.reminderLabel ?: ReminderLabelStyle.REMINDERS).displayName
    var page by rememberSaveable { mutableIntStateOf(0) }
    var showChangelog by remember { mutableStateOf(false) }
    val tabs = listOf("What goes where", "Getting around")

    SettingsSubScreenScaffold(title = "Help", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 6.dp),
        ) {
            HelpPageTabs(selected = page, labels = tabs, onSelect = { page = it })
            Spacer(Modifier.height(14.dp))
            // The pages scroll; the version footer stays pinned below them.
            Box(modifier = Modifier.weight(1f)) {
                // A fresh scroll state per page so each opens at the top.
                key(page) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (page) {
                            0 -> HelpContent(reminderLabel = reminderLabel)
                            else -> HelpGettingAround(reminderLabel = reminderLabel)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            HelpVersionFooter(onClick = { showChangelog = true })
        }
    }

    if (showChangelog) {
        WhatsNewDialog(onDismiss = { showChangelog = false })
    }
}

/** Tappable "Version x.y.z" line at the foot of Help; opens What's New. */
@Composable
private fun HelpVersionFooter(onClick: () -> Unit) {
    val tokens = LocalDashTokens.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(vertical = 8.dp)
            .wrapContentHeight(Alignment.CenterVertically),
    ) {
        Text(
            "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
            color = tokens.inkFaint,
        )
        Text(
            "What's new ›",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Segmented header, two full-width cells; mirrors the zen "mine | all" pill. */
@Composable
private fun HelpPageTabs(
    selected: Int,
    labels: List<String>,
    onSelect: (Int) -> Unit,
) {
    val tokens = LocalDashTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(tokens.sheetBlock)
            .border(1.dp, tokens.sheetBlockOutline, PillShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            HelpPageTab(
                label = label,
                selected = index == selected,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HelpPageTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDashTokens.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .semantics { role = Role.RadioButton }
            .selectable(selected = selected, onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            ),
            color = if (selected) MaterialTheme.colorScheme.onSurfaceVariant else tokens.inkFaint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        )
    }
}
