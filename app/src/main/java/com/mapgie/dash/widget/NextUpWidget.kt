package com.mapgie.dash.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.appwidget.CheckBox
import androidx.glance.Button
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.remindAtInstant
import com.mapgie.dash.data.model.urgency
import com.mapgie.dash.data.preferences.AppSettings
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private sealed interface NextUpData {
    data class Task(val task: TaskDto) : NextUpData
    data class ChoreItem(val chore: Chore) : NextUpData
    data class ReminderItem(val reminder: ReminderDto) : NextUpData
    data class Empty(val contentType: String) : NextUpData
    data object NotConfigured : NextUpData
    data class Unavailable(val lastSyncedAt: Instant?) : NextUpData
}

/**
 * Shows the single most urgent item from whichever source the user picked in
 * Settings > Widget customisation (Show / Priority / Whose), falling back to
 * chores by default. Tap opens the app; a checkbox/button lets the shown item
 * be completed inline.
 */
class NextUpWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(WIDGET_RESPONSIVE_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val data = loadNextUpData(entryPoint)
        provideContent {
            GlanceTheme(colors = DashGlanceTheme.colors) {
                NextUpContent(data)
            }
        }
    }

    private suspend fun loadNextUpData(entryPoint: WidgetEntryPoint): NextUpData {
        val settings = entryPoint.settingsRepository().settings.first()
        // Reminders are on-device only (ReminderRepository never touches Supabase), so
        // that content type works with no credentials configured; Chores/Tasks do need them.
        val needsSupabase = settings.widgetContentType != "REMINDERS"
        if (needsSupabase && (settings.supabaseUrl.isBlank() || settings.supabaseKey.isBlank())) {
            return NextUpData.NotConfigured
        }
        return runCatching {
            val data = when (settings.widgetContentType) {
                "TASKS" -> nextTask(entryPoint, settings) ?: NextUpData.Empty("TASKS")
                "REMINDERS" -> nextReminder(entryPoint) ?: NextUpData.Empty("REMINDERS")
                else -> nextChore(entryPoint, settings) ?: NextUpData.Empty("CHORES")
            }
            entryPoint.widgetSyncStore().markSynced(WidgetSyncKey.NEXT_UP)
            data
        }.getOrElse {
            NextUpData.Unavailable(entryPoint.widgetSyncStore().lastSyncedAt(WidgetSyncKey.NEXT_UP))
        }
    }

    private suspend fun nextTask(entryPoint: WidgetEntryPoint, settings: AppSettings): NextUpData.Task? {
        val candidates = entryPoint.taskRepository().loadTasks()
            .filter { it.completedAt == null && it.archivedAt == null }
            .filter { matchesOwner(it.owner, settings) }
            .filter { matchesTaskPriority(it.urgency(), settings.widgetPriorityFilter) }
        val next = candidates.minWithOrNull(
            compareBy({ it.urgency().ordinal }, { it.dueDate ?: "9999-12-31" })
        )
        return next?.let { NextUpData.Task(it) }
    }

    private suspend fun nextChore(entryPoint: WidgetEntryPoint, settings: AppSettings): NextUpData.ChoreItem? {
        val active = entryPoint.choreRepository().load().active
            .filter { matchesOwner(it.owner, settings) }
        val due = nextChoreCandidate(active, settings.widgetPriorityFilter)
        return due?.let { NextUpData.ChoreItem(it) }
    }

    private suspend fun nextReminder(entryPoint: WidgetEntryPoint): NextUpData.ReminderItem? {
        // Reminders carry no owner/urgency of their own, so the Priority and Whose
        // filters don't apply to this content type; just surface the soonest one due.
        val next = entryPoint.reminderRepository().pendingReminders()
            .minByOrNull { it.remindAtInstant() ?: Instant.MAX }
        return next?.let { NextUpData.ReminderItem(it) }
    }
}

private fun matchesOwner(owner: String?, settings: AppSettings): Boolean =
    settings.widgetOwnerFilter != "MINE" ||
        settings.ownerHandle.isBlank() ||
        owner == null ||
        owner == settings.ownerHandle

private fun matchesTaskPriority(urgency: TaskUrgency, filter: String): Boolean = when (filter) {
    "RED" -> urgency == TaskUrgency.OVERDUE
    "AMBER" -> urgency == TaskUrgency.TODAY
    else -> true
}

private fun nextChoreCandidate(active: List<Chore>, filter: String): Chore? = when (filter) {
    "RED" -> active.firstOrNull { it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER }
    "AMBER" -> active.firstOrNull { it.status == ChoreStatus.AGING }
    else -> active.firstOrNull { it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER }
        ?: active.firstOrNull { it.status == ChoreStatus.AGING }
}

class NextUpWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextUpWidget()
}

@Composable
private fun NextUpContent(data: NextUpData) {
    val compact = LocalSize.current.height < WIDGET_SIZE_LARGE.height
    // True 1x1 launcher cells have no room for a checkbox/button plus text, so below
    // WIDGET_SIZE_SMALL we drop the inline action and fall back to a single tap-to-open line.
    val tiny = LocalSize.current.width < WIDGET_SIZE_SMALL.width

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        when (data) {
            is NextUpData.Task -> NextUpTaskContent(data.task, compact, tiny)
            is NextUpData.ChoreItem -> NextUpChoreContent(data.chore, compact, tiny)
            is NextUpData.ReminderItem -> NextUpReminderContent(data.reminder, compact)
            is NextUpData.Empty -> NextUpEmptyContent(data.contentType, compact)
            NextUpData.NotConfigured -> CenteredMessage("Connect Supabase in Settings to use this widget", WIDGET_DEST_SETTINGS)
            is NextUpData.Unavailable -> CenteredMessage(unavailableMessage(data.lastSyncedAt), WIDGET_DEST_TASKS)
        }
    }
}

@Composable
private fun NextUpTaskContent(task: TaskDto, compact: Boolean, tiny: Boolean) {
    val context = LocalContext.current
    if (tiny) {
        TinyTapToOpen(task.title, WIDGET_DEST_TASKS)
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        CheckBox(
            checked = false,
            onCheckedChange = actionRunCallback<ToggleTaskDoneAction>(
                actionParametersOf(TaskIdKey to task.id, MarkDoneKey to true)
            )
        )
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(widgetActivityIntent(context, WIDGET_DEST_TASKS)))
        ) {
            Text(
                text = "Next up",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
            )
            Text(
                text = task.title,
                maxLines = if (compact) 1 else 2,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            )
            if (!compact) {
                Text(
                    text = dueLabel(task),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                )
            }
        }
    }
}

@Composable
private fun NextUpChoreContent(chore: Chore, compact: Boolean, tiny: Boolean) {
    if (tiny) {
        TinyTapToOpen(chore.label, WIDGET_DEST_CHORES)
        return
    }
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Needs attention",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
        )
        Text(
            text = chore.label,
            maxLines = if (compact) 1 else 2,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Button(
            text = "Log now",
            onClick = actionRunCallback<LogChoreAction>(actionParametersOf(ChoreTagIdKey to chore.tagId))
        )
    }
}

@Composable
private fun NextUpReminderContent(reminder: ReminderDto, compact: Boolean) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(widgetActivityIntent(context, WIDGET_DEST_REMINDERS))),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Reminder",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
        )
        Text(
            text = reminder.subject,
            maxLines = if (compact) 1 else 2,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        )
        if (!compact) {
            Text(
                text = reminderTimeLabel(reminder),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun NextUpEmptyContent(contentType: String, compact: Boolean) {
    val context = LocalContext.current
    val destination = when (contentType) {
        "TASKS" -> WIDGET_DEST_QUICK_ADD_TASK
        "REMINDERS" -> WIDGET_DEST_QUICK_ADD_REMINDER
        else -> WIDGET_DEST_QUICK_ADD_CHORE
    }
    val subtitle = when (contentType) {
        "TASKS" -> "Tap to add a task"
        "REMINDERS" -> "Tap to add a reminder"
        else -> "Tap to add a chore"
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(widgetActivityIntent(context, destination))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "All caught up",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            )
            if (!compact) {
                Text(
                    text = subtitle,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                )
            }
        }
    }
}

private fun dueLabel(task: TaskDto): String = when (task.urgency()) {
    TaskUrgency.OVERDUE -> "Overdue"
    TaskUrgency.TODAY -> "Due today"
    TaskUrgency.THIS_WEEK -> task.dueDate
        ?.let { runCatching { "Due " + LocalDate.parse(it).format(DateTimeFormatter.ofPattern("EEE")) }.getOrNull() }
        ?: "Due this week"
    TaskUrgency.LATER -> task.dueDate
        ?.let { runCatching { "Due " + LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MMM d")) }.getOrNull() }
        ?: "Due later"
    TaskUrgency.NONE -> ""
}

private fun reminderTimeLabel(reminder: ReminderDto): String {
    val zoned = reminder.remindAtInstant()?.atZone(ZoneId.systemDefault()) ?: return ""
    val today = LocalDate.now(ZoneId.systemDefault())
    return when (zoned.toLocalDate()) {
        today -> "Today " + zoned.format(DateTimeFormatter.ofPattern("HH:mm"))
        today.plusDays(1) -> "Tomorrow " + zoned.format(DateTimeFormatter.ofPattern("HH:mm"))
        else -> zoned.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    }
}
