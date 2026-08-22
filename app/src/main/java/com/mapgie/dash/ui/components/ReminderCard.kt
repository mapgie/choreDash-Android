package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.isPast
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.ui.components.core.DashListCard
import com.mapgie.dash.ui.components.core.MetaLabel
import com.mapgie.dash.ui.components.core.SourceChip
import com.mapgie.dash.ui.components.core.SourceKind
import com.mapgie.dash.ui.theme.statusTone
import com.mapgie.dash.ui.theme.textColor
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Thin binding of a [ReminderDto] onto the shared [DashListCard].
 *
 * Memos is a collation surface, so this fills only the slots that make sense here:
 * a done checkbox, the subject, the fire time, and a [SourceChip] naming where the
 * alarm came from. No owner avatar, no zen mode, no trailing column (§5c).
 *
 * Overdue no longer uses the reserved `error` palette: it maps through [statusTone]
 * to the shared overdue vocabulary (a red accent bar and red "Overdue" text), and
 * the container stays opaque, so an archived overdue Memo no longer leaks the swipe
 * panel behind it.
 */
@Composable
fun ReminderCard(
    reminder: ReminderDto,
    linkedLabel: String?,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = reminder.completedAt != null
    val isOverdue = reminder.isPast() && !isDone
    val tone = reminder.statusTone()

    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm") }
    val whenLabel = reminder.remindAtInstant()
        ?.atZone(ZoneId.systemDefault())
        ?.format(formatter)
    val timeText = if (isOverdue) whenLabel?.let { "Overdue: $it" } ?: "Overdue" else whenLabel
    val timeColor = when {
        isDone -> MaterialTheme.colorScheme.onSurfaceVariant
        isOverdue -> tone.textColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val sourceKind = when {
        reminder.choreId != null -> SourceKind.CHORE
        reminder.taskId != null -> SourceKind.TASK
        else -> null
    }

    DashListCard(
        tone = tone,
        modifier = modifier,
        onClick = onClick,
        dimmed = isDone,
        leading = {
            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggleDone() },
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    ) {
        Text(
            text = reminder.subject,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (isDone) TextDecoration.LineThrough else null,
            color = if (isDone)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            timeText?.let {
                MetaLabel(
                    text = it,
                    color = timeColor,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (sourceKind != null && linkedLabel != null) {
                SourceChip(kind = sourceKind, label = linkedLabel)
            }
        }
    }
}
