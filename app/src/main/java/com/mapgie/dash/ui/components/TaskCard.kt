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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.priorityEnum
import com.mapgie.dash.data.model.urgency
import com.mapgie.dash.ui.theme.StatusAging
import com.mapgie.dash.ui.theme.StatusFresh
import com.mapgie.dash.ui.theme.StatusStale
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val priorityColor = when (task.priorityEnum()) {
        TaskPriority.HIGHER -> StatusAging
        TaskPriority.NORMAL -> StatusFresh
        TaskPriority.LOWER -> MaterialTheme.colorScheme.outline
    }
    val barColor = if (zenMode) Color.Transparent else priorityColor

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
            // Priority bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(barColor)
            )
            // Done checkbox
            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggleDone() },
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showCategory && !zenMode) {
                        task.category?.takeIf { it.isNotBlank() }?.let { cat ->
                            CategoryBadge(cat)
                        }
                    }
                    if (zenMode) ZenDueLabel(task = task) else DueBadge(task = task)
                }
            }
            // Owner badge
            task.owner?.takeIf { showOwner && it.isNotBlank() }?.let { owner ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .align(Alignment.CenterVertically)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = owner.take(1).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun DueBadge(task: TaskDto) {
    val urgency = task.urgency()
    val (text, color) = when (urgency) {
        TaskUrgency.OVERDUE -> "Overdue" to StatusStale
        TaskUrgency.TODAY -> "Today" to StatusAging
        TaskUrgency.THIS_WEEK -> {
            val label = task.dueDate
                ?.let { runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("EEE")) }.getOrNull() }
                ?: "This week"
            label to StatusFresh
        }
        TaskUrgency.LATER -> {
            val label = task.dueDate
                ?.let { runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MMM d")) }.getOrNull() }
                ?: "Later"
            label to MaterialTheme.colorScheme.onSurfaceVariant
        }
        TaskUrgency.NONE -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

/** Plain, uncoloured due date for zen mode, unlike DueBadge it never signals urgency. */
@Composable
private fun ZenDueLabel(task: TaskDto) {
    val date = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
    Text(
        text = date.format(DateTimeFormatter.ofPattern("MMM d")),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CategoryBadge(category: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = category.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
