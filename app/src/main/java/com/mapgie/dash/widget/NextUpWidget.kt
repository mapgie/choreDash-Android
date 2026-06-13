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
import androidx.glance.appwidget.Button
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.defaultWeight
import androidx.glance.material3.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.data.model.TaskUrgency
import com.mapgie.dash.data.model.urgency
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private sealed interface NextUpData {
    data class Task(val task: TaskDto) : NextUpData
    data class ChoreItem(val chore: Chore) : NextUpData
    data object Empty : NextUpData
    data object Unavailable : NextUpData
}

/** Shows the single most urgent task (or, failing that, the stalest chore), with a quick way to complete it. */
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

    private suspend fun loadNextUpData(entryPoint: WidgetEntryPoint): NextUpData = runCatching {
        val tasks = entryPoint.taskRepository().loadTasks()
            .filter { it.completedAt == null && it.archivedAt == null }
        val next = tasks.minWithOrNull(
            compareBy({ it.urgency().ordinal }, { it.dueDate ?: "9999-12-31" })
        )
        if (next != null) return@runCatching NextUpData.Task(next)

        val active = entryPoint.choreRepository().load().active
        val due = active.firstOrNull { it.status == ChoreStatus.STALE || it.status == ChoreStatus.NEVER }
            ?: active.firstOrNull { it.status == ChoreStatus.AGING }
        if (due != null) NextUpData.ChoreItem(due) else NextUpData.Empty
    }.getOrElse { NextUpData.Unavailable }
}

class NextUpWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextUpWidget()
}

@Composable
private fun NextUpContent(data: NextUpData) {
    val compact = LocalSize.current.height < WIDGET_SIZE_LARGE.height

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        when (data) {
            is NextUpData.Task -> NextUpTaskContent(data.task, compact)
            is NextUpData.ChoreItem -> NextUpChoreContent(data.chore, compact)
            NextUpData.Empty -> CenteredMessage("All caught up", WIDGET_DEST_TASKS)
            NextUpData.Unavailable -> CenteredMessage("Open app to connect", WIDGET_DEST_TASKS)
        }
    }
}

@Composable
private fun NextUpTaskContent(task: TaskDto, compact: Boolean) {
    val context = LocalContext.current
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
                .defaultWeight()
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
private fun NextUpChoreContent(chore: Chore, compact: Boolean) {
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
private fun CenteredMessage(message: String, destination: String) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(widgetActivityIntent(context, destination))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
        )
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
