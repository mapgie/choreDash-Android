package com.mapgie.dash.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.SwipeAction
import com.mapgie.dash.data.model.SwipeDirection
import com.mapgie.dash.data.model.SwipePair
import com.mapgie.dash.data.model.SwipeSettings
import com.mapgie.dash.data.model.SwipeSubject
import com.mapgie.dash.ui.components.core.LocalReminderLabel

/**
 * Settings › Swipe actions: one card per list (chores, tasks, memos) with a
 * row of pill choices for the left swipe and another for the right. The
 * choices on offer differ per list (see [SwipeSubject.offered]); the words
 * match what the card shows mid-swipe.
 */
@Composable
fun SwipeSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val swipe = settings?.swipeActions ?: SwipeSettings()
    val memoWord = LocalReminderLabel.current

    SettingsSubScreenScaffold(title = "Swipe actions", onBack = onBack) { innerPadding ->
        SubScreenColumn(innerPadding) {
            SettingsCaption(
                "Swipe a card sideways to act on it without opening it. " +
                "Log, Done, Snooze and Archive can be undone from the message that appears. " +
                "Delete asks first. Choose Nothing to turn that swipe off."
            )

            SwipeSubject.entries.forEach { subject ->
                SettingsSectionLabel(
                    when (subject) {
                        SwipeSubject.CHORES -> "Chores"
                        SwipeSubject.TASKS -> "Tasks"
                        SwipeSubject.MEMOS -> memoWord.displayName
                    }
                )
                SettingsCard {
                    SwipeDirectionRow(
                        direction = SwipeDirection.LEFT,
                        subject = subject,
                        pair = swipe[subject],
                        onSelect = { viewModel.setSwipeAction(subject, SwipeDirection.LEFT, it) },
                    )
                    SettingsHairline()
                    SwipeDirectionRow(
                        direction = SwipeDirection.RIGHT,
                        subject = subject,
                        pair = swipe[subject],
                        onSelect = { viewModel.setSwipeAction(subject, SwipeDirection.RIGHT, it) },
                    )
                }
                if (subject == SwipeSubject.MEMOS) {
                    SettingsCaption(
                        "Snooze pushes the next ring back an hour. A tag-alarm keeps its morning, " +
                        "so Snooze does nothing on one of those."
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SwipeDirectionRow(
    direction: SwipeDirection,
    subject: SwipeSubject,
    pair: SwipePair,
    onSelect: (SwipeAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    ) {
        SettingsRowText(title = direction.displayName)
        Spacer(Modifier.height(10.dp))
        CozyChoiceChips(
            options = subject.offered,
            selected = pair.action(direction),
            onSelect = onSelect,
            label = { subject.label(it) },
        )
    }
}
