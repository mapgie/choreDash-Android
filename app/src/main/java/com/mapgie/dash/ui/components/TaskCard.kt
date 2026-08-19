package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskPriority
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.priorityEnum
import com.mapgie.dash.data.model.urgency
import com.mapgie.dash.ui.components.core.CategoryBadge
import com.mapgie.dash.ui.components.core.DashListCard
import com.mapgie.dash.ui.components.core.MetaLabel
import com.mapgie.dash.ui.theme.StatusAging
import com.mapgie.dash.ui.theme.StatusFresh
import com.mapgie.dash.ui.theme.StatusStale
import com.mapgie.dash.ui.theme.statusTone
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Thin binding of a [TaskDto] onto the shared [DashListCard].
 *
 * The accent bar now encodes **urgency** (via [statusTone]), the same axis it
 * encodes on Chores and Memos, so the same colour means the same thing everywhere.
 * Priority, which does not move on its own, is carried by a non-colour [marker] on
 * the title so it stays readable for red-green colour-blind users.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: TaskDto,
    onToggleDone: () -> Unit,
    showCategory: Boolean = true,
    modifier: Modifier = Modifier,
    showOwner: Boolean = true,
    zenMode: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val isDone = task.completedAt != null

    DashListCard(
        tone = task.statusTone(),
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        dimmed = isDone,
        zenMode = zenMode,
        owner = task.owner?.takeIf { showOwner && it.isNotBlank() },
        leading = {
            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggleDone() },
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!zenMode) PriorityMarker(task.priorityEnum())
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
}

/**
 * Non-colour priority marker on the title: "!" for higher, a downward chevron for
 * lower, nothing for normal. Tinted neutrally, because colour now carries urgency.
 */
@Composable
private fun PriorityMarker(priority: TaskPriority) {
    val (icon, description) = when (priority) {
        TaskPriority.HIGHER -> Icons.Filled.PriorityHigh to "High priority"
        TaskPriority.LOWER -> Icons.Filled.KeyboardArrowDown to "Low priority"
        TaskPriority.NORMAL -> return
    }
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp)
    )
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
    MetaLabel(text = date.format(DateTimeFormatter.ofPattern("MMM d")))
}
