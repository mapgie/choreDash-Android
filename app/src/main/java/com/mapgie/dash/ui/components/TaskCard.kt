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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.priorityEnum
import com.mapgie.dash.data.model.urgency
import com.mapgie.dash.ui.components.core.DoneToggleChip
import com.mapgie.dash.ui.components.core.MetaCaption
import com.mapgie.dash.ui.components.core.MetaLabel
import com.mapgie.dash.ui.components.core.OwnerAvatar
import com.mapgie.dash.ui.components.core.StatusBadge
import com.mapgie.dash.ui.theme.Dimens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.barColor
import com.mapgie.dash.ui.theme.statusTone
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Cozy Cream list card for a task: urgency spine, circular done-toggle chip on the
 * task accent, title with an uppercase "category · priority" caption beneath, and a
 * right column carrying the due badge above the owner avatar.
 *
 * The spine colour means urgency (the app-wide bar meaning, see `StatusTone.kt`);
 * priority is carried by the caption text, never by colour alone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: TaskDto,
    onToggleDone: () -> Unit,
    showCategory: Boolean = true,
    modifier: Modifier = Modifier,
    showOwner: Boolean = true,
    zenMode: Boolean = false
) {
    val isDone = task.completedAt != null
    val accents = LocalTypeAccents.current
    val barColor = if (zenMode) Color.Transparent else task.statusTone().barColor()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                zenMode -> MaterialTheme.colorScheme.surface
                isDone -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Urgency spine
            Box(
                modifier = Modifier
                    .width(Dimens.accentBarWidth)
                    .fillMaxHeight()
                    .background(barColor)
            )
            DoneToggleChip(
                isDone = isDone,
                onToggle = onToggleDone,
                containerColor = if (zenMode) Color.Transparent else accents.taskContainer,
                contentColor = if (zenMode) MaterialTheme.colorScheme.onSurfaceVariant
                               else accents.onTaskContainer,
                modifier = Modifier
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
                    .align(Alignment.CenterVertically)
            )
            // Title + meta caption
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (isDone) TextDecoration.LineThrough else null,
                        color = if (isDone)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (task.reminderAt != null && task.reminded != true && !isDone) {
                        val reminderInstant = remember(task.reminderAt) {
                            runCatching { Instant.parse(task.reminderAt) }.getOrNull()
                        }
                        val isReminderPast = reminderInstant != null && reminderInstant.isBefore(Instant.now())
                        Icon(
                            Icons.Filled.NotificationsActive,
                            contentDescription = if (isReminderPast) "Reminder passed" else "Reminder set",
                            tint = if (isReminderPast)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (zenMode) {
                    ZenDueLabel(task = task)
                } else {
                    val priorityLabel = when (task.priorityEnum()) {
                        TaskPriority.HIGHER -> "higher"
                        TaskPriority.NORMAL -> "normal"
                        TaskPriority.LOWER -> "lower"
                    }
                    val caption = listOfNotNull(
                        task.category?.takeIf { showCategory && it.isNotBlank() },
                        priorityLabel,
                    ).joinToString(" · ")
                    MetaCaption(text = caption)
                }
            }
            // Right column: due badge above the owner avatar, pinned rightmost so
            // the same person renders identically here and on the Chores list.
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(end = 12.dp, top = 12.dp, bottom = 12.dp)
                    .align(Alignment.CenterVertically)
            ) {
                if (!zenMode) DueBadge(task = task)
                task.owner?.takeIf { showOwner && it.isNotBlank() }?.let { owner ->
                    OwnerAvatar(handle = owner, modifier = Modifier.align(Alignment.End))
                }
            }
        }
    }
}

@Composable
private fun DueBadge(task: TaskDto) {
    val text = when (task.urgency()) {
        TaskUrgency.OVERDUE -> "Overdue"
        TaskUrgency.TODAY -> "Today"
        TaskUrgency.THIS_WEEK -> task.dueDate
            ?.let { runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("EEE")) }.getOrNull() }
            ?: "This week"
        TaskUrgency.LATER -> task.dueDate
            ?.let { runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MMM d")) }.getOrNull() }
            ?: "Later"
        TaskUrgency.NONE -> return
    }
    StatusBadge(text = text, tone = task.statusTone())
}

/** Plain, uncoloured due date for zen mode, unlike DueBadge it never signals urgency. */
@Composable
private fun ZenDueLabel(task: TaskDto) {
    val date = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
    MetaLabel(text = date.format(DateTimeFormatter.ofPattern("MMM d")))
}
