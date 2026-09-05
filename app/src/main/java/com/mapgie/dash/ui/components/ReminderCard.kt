package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.ReminderScheduleText
import com.mapgie.dash.data.model.repeats
import com.mapgie.dash.ui.components.core.DoneToggleChip
import com.mapgie.dash.ui.components.core.MetaCaption
import com.mapgie.dash.ui.components.core.StatusBadge
import com.mapgie.dash.ui.components.core.highlightedText
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.StatusTone
import com.mapgie.dash.ui.theme.badgeContainerColor
import com.mapgie.dash.ui.theme.barColor
import com.mapgie.dash.ui.theme.isDarkScheme
import com.mapgie.dash.ui.theme.statusTone
import com.mapgie.dash.ui.theme.textColor
import java.time.Instant

/**
 * List card for a memo (handoff 9a), in the shared card format: a 5dp spine,
 * the 38dp bell chip that doubles as the done toggle, the title, the schedule
 * as the meta line ("Weekdays · 8:00 PM · linked to chore", "Once · doesn't
 * repeat") and the next ring as the right-hand badge ("rings tomorrow 9 AM",
 * "rang 2h ago"). Spine, chip and badge take the memo's status tone; a done
 * or archived memo is one plain muted state with no colour and no strikethrough.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    reminder: ReminderDto,
    linkedTo: String?,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
    highlightQuery: String? = null,
    inset: Dp = Dimens.cardInset,
    now: Instant = Instant.now(),
) {
    val isDone = !reminder.repeats && reminder.completedAt != null
    val isArchived = reminder.archivedAt != null
    val muted = isDone || isArchived
    val tone = reminder.statusTone(now)
    val accents = LocalTypeAccents.current
    val dark = isDarkScheme()

    val scheduleLine = remember(reminder, linkedTo) {
        ReminderScheduleText.scheduleLine(reminder, linkedTo = linkedTo)
    }
    val badgeText = remember(reminder, now) {
        if (isArchived) "archived" else ReminderScheduleText.nextRingBadge(reminder, now)
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = inset),
        colors = CardDefaults.cardColors(
            containerColor = if (muted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (dark) 0.dp else 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(Dimens.accentBarWidth)
                    .fillMaxHeight()
                    .background(tone.barColor())
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = Dimens.cardPadding - 3.dp,
                        end = Dimens.cardPadding,
                        top = Dimens.cardVerticalPadding - 3.dp,
                        bottom = Dimens.cardVerticalPadding - 3.dp,
                    )
            ) {
                // The bell keeps the memo accent while quiet; a signalling tone takes over.
                val signalling = tone == StatusTone.CRITICAL || tone == StatusTone.ATTENTION || tone == StatusTone.OK
                DoneToggleChip(
                    isDone = isDone,
                    onToggle = onToggleDone,
                    icon = LucideIcons.Bell,
                    containerColor = when {
                        muted -> MaterialTheme.colorScheme.surfaceContainerHigh
                        signalling -> tone.badgeContainerColor() ?: accents.reminderContainer
                        else -> accents.reminderContainer
                    },
                    contentColor = when {
                        muted -> MaterialTheme.colorScheme.onSurfaceVariant
                        signalling -> tone.textColor()
                        else -> accents.onReminderContainer
                    },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = highlightedText(reminder.subject, highlightQuery),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (muted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    MetaCaption(text = scheduleLine, uppercase = false)
                }
                StatusBadge(text = badgeText, tone = if (muted) StatusTone.NEUTRAL else tone)
            }
        }
    }
}
