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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * The revised (turn 5a) list card for a task: urgency spine, a circular
 * done-toggle chip carrying the category's Lucide [icon] on the urgency tint,
 * title with an uppercase "CATEGORY · HIGH" caption beneath, and a single
 * right-hand row of owner avatar then due badge.
 *
 * The spine colour means urgency (the app-wide bar meaning, see `StatusTone.kt`);
 * priority is carried by the caption text, never by colour alone.
 */
@Composable
fun TaskCard(
    task: TaskDto,
    onToggleDone: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    showCategory: Boolean = true,
    showOwner: Boolean = true,
    zenMode: Boolean = false,
    isPinned: Boolean = false,
    highlightQuery: String? = null
) {
    val isDone = task.completedAt != null
    val accents = LocalTypeAccents.current
    val tone = task.statusTone()
    val dark = isDarkScheme()
    val barColor = if (zenMode) Color.Transparent else tone.barColor()

    val chipContainer = when {
        zenMode -> Color.Transparent
        isDone -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> tone.badgeContainerColor() ?: accents.taskContainer
    }
    val chipContent = when {
        zenMode || isDone -> MaterialTheme.colorScheme.onSurfaceVariant
        tone == StatusTone.NEUTRAL || tone == StatusTone.NONE -> accents.onTaskContainer
        else -> tone.textColor()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.cardInset),
        colors = CardDefaults.cardColors(
            containerColor = when {
                zenMode -> MaterialTheme.colorScheme.surfaceContainerLow
                isDone -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (dark) 0.dp else 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(Dimens.accentBarWidth)
                    .fillMaxHeight()
                    .background(barColor)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    // The toggle chip's 44dp touch target is 3dp wider than the
                    // visual chip on each side; trim the inset so the glyph lines
                    // up with the Chores cards.
                    .padding(
                        start = Dimens.cardPadding - 3.dp,
                        end = Dimens.cardPadding,
                        top = Dimens.cardVerticalPadding - 3.dp,
                        bottom = Dimens.cardVerticalPadding - 3.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DoneToggleChip(
                    isDone = isDone,
                    onToggle = onToggleDone,
                    icon = icon,
                    containerColor = chipContainer,
                    contentColor = chipContent,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = highlightedText(task.title, highlightQuery),
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (isDone) TextDecoration.LineThrough else null,
                            color = if (isDone)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPinned) {
                            Icon(
                                imageVector = LucideIcons.PinFilled,
                                contentDescription = "Pinned to widget",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (task.reminderAt != null && task.reminded != true && !isDone) {
                            val reminderInstant = remember(task.reminderAt) {
                                runCatching { Instant.parse(task.reminderAt) }.getOrNull()
                            }
                            val isReminderPast = reminderInstant != null && reminderInstant.isBefore(Instant.now())
                            Icon(
                                imageVector = LucideIcons.Bell,
                                contentDescription = if (isReminderPast) "Reminder passed" else "Reminder set",
                                tint = if (isReminderPast)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (zenMode) {
                        ZenDueLabel(task = task)
                    } else {
                        val priorityLabel = when (task.priorityEnum()) {
                            TaskPriority.HIGHER -> "high"
                            TaskPriority.NORMAL -> null
                            TaskPriority.LOWER -> "low"
                        }
                        val caption = listOfNotNull(
                            task.category?.takeIf { showCategory && it.isNotBlank() },
                            priorityLabel,
                        ).joinToString(" · ")
                        if (caption.isNotBlank()) MetaCaption(text = caption)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.metaSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    task.owner?.takeIf { showOwner && it.isNotBlank() }?.let { owner ->
                        OwnerAvatar(handle = owner)
                    }
                    if (!zenMode && !isDone) DueBadge(task = task)
                }
            }
        }
    }
}

/**
 * "2d late" on the rose tint, "today" on amber, then plain "Thu" / "12 Sep" for
 * anything further out, matching the handoff's right-hand cluster.
 */
@Composable
private fun DueBadge(task: TaskDto) {
    val today = LocalDate.now(ZoneId.systemDefault())
    val date = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    // "Eventually" carries no urgency but is still worth showing, so it never reads
    // as a task with no due at all.
    if (task.dueDate == null && task.duePeriod == "eventually") {
        StatusBadge(text = "eventually", tone = StatusTone.NEUTRAL)
        return
    }
    when (task.urgency()) {
        TaskUrgency.OVERDUE -> {
            val late = date?.let { ChronoUnit.DAYS.between(it, today) } ?: 1L
            StatusBadge(text = "${late}d late", tone = StatusTone.CRITICAL)
        }
        TaskUrgency.TODAY -> StatusBadge(text = "today", tone = StatusTone.ATTENTION)
        TaskUrgency.THIS_WEEK -> StatusBadge(
            text = date?.format(DateTimeFormatter.ofPattern("EEE")) ?: "this week",
            tone = StatusTone.NEUTRAL,
        )
        TaskUrgency.LATER -> StatusBadge(
            text = date?.format(DateTimeFormatter.ofPattern("d MMM")) ?: "this month",
            tone = StatusTone.NEUTRAL,
        )
        TaskUrgency.NONE -> Unit
    }
}

/** Plain, uncoloured due date for zen mode, unlike DueBadge it never signals urgency. */
@Composable
private fun ZenDueLabel(task: TaskDto) {
    val date = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
    MetaLabel(text = date.format(DateTimeFormatter.ofPattern("d MMM")))
}
