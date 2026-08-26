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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.isPast
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.ui.components.core.DoneToggleChip
import com.mapgie.dash.ui.components.core.MetaLabel
import com.mapgie.dash.ui.components.core.StatusBadge
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.StatusTone
import com.mapgie.dash.ui.theme.barColor
import com.mapgie.dash.ui.theme.statusTone
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Cozy Cream list card for a reminder/memo: same shell as the Task card, with the
 * circular done-toggle chip on the reminder accent and an "Overdue" badge (rose
 * on tint, per the shared status scale) instead of the reserved error colours.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val accents = LocalTypeAccents.current

    val formatter = remember(reminder.remindAt) {
        DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm")
    }
    val whenLabel = reminder.remindAtInstant()
        ?.atZone(ZoneId.systemDefault())
        ?.format(formatter)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Spine signals only overdue; quiet states stay quiet.
            Box(
                modifier = Modifier
                    .width(Dimens.accentBarWidth)
                    .fillMaxHeight()
                    .background(if (isOverdue) tone.barColor() else Color.Transparent)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            ) {
                DoneToggleChip(
                    isDone = isDone,
                    onToggle = onToggleDone,
                    containerColor = accents.reminderContainer,
                    contentColor = accents.onReminderContainer,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = reminder.subject,
                        style = MaterialTheme.typography.titleMedium,
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
                        whenLabel?.let {
                            MetaLabel(
                                text = it,
                                color = if (isDone)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        linkedLabel?.let {
                            MetaLabel(text = it, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (isOverdue) {
                    StatusBadge(text = "Overdue", tone = StatusTone.CRITICAL)
                }
            }
        }
    }
}
