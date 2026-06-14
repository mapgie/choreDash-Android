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
import androidx.glance.Button
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.TaskDto
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit

private sealed interface PinnedData {
    data class Task(val task: TaskDto) : PinnedData
    data class ChoreItem(val chore: Chore) : PinnedData
    data object NotSet : PinnedData
    data object NotFound : PinnedData
    data object Unavailable : PinnedData
}

/** Shows the task or chore the user pinned from its card in the app, with a quick way to complete it. */
class PinnedItemWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(WIDGET_RESPONSIVE_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val data = loadPinnedData(entryPoint)
        provideContent {
            GlanceTheme(colors = DashGlanceTheme.colors) {
                PinnedContent(data)
            }
        }
    }

    private suspend fun loadPinnedData(entryPoint: WidgetEntryPoint): PinnedData {
        val pinned = entryPoint.pinnedItemStore().pinnedItem.first() ?: return PinnedData.NotSet

        return runCatching {
            when (pinned.type) {
                PinnedItemType.TASK -> {
                    val task = entryPoint.taskRepository().loadTasks().find { it.id == pinned.id }
                    task?.let { PinnedData.Task(it) } ?: PinnedData.NotFound
                }
                PinnedItemType.CHORE -> {
                    val result = entryPoint.choreRepository().load()
                    val chore = (result.active + result.archived).find { it.id == pinned.id }
                    chore?.let { PinnedData.ChoreItem(it) } ?: PinnedData.NotFound
                }
            }
        }.getOrElse { PinnedData.Unavailable }
    }
}

class PinnedItemWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PinnedItemWidget()
}

@Composable
private fun PinnedContent(data: PinnedData) {
    val compact = LocalSize.current.height < WIDGET_SIZE_LARGE.height

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        when (data) {
            is PinnedData.Task -> PinnedTaskContent(data.task, compact)
            is PinnedData.ChoreItem -> PinnedChoreContent(data.chore, compact)
            PinnedData.NotSet -> CenteredMessage("Pin a task or chore from the app", WIDGET_DEST_TASKS)
            PinnedData.NotFound -> CenteredMessage("Pinned item no longer exists", WIDGET_DEST_TASKS)
            PinnedData.Unavailable -> CenteredMessage("Open app to connect", WIDGET_DEST_TASKS)
        }
    }
}

@Composable
private fun PinnedTaskContent(task: TaskDto, compact: Boolean) {
    val context = LocalContext.current
    val isDone = task.completedAt != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        CheckBox(
            checked = isDone,
            onCheckedChange = actionRunCallback<ToggleTaskDoneAction>(
                actionParametersOf(TaskIdKey to task.id, MarkDoneKey to !isDone)
            )
        )
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(widgetActivityIntent(context, WIDGET_DEST_TASKS)))
        ) {
            Text(
                text = "Pinned",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
            )
            Text(
                text = task.title,
                maxLines = if (compact) 1 else 2,
                style = TextStyle(
                    color = if (isDone) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                )
            )
            if (!compact) {
                task.category?.takeIf { it.isNotBlank() }?.let { category ->
                    Text(
                        text = category,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedChoreContent(chore: Chore, compact: Boolean) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Pinned",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
        )
        Text(
            text = chore.label,
            maxLines = if (compact) 1 else 2,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        )
        if (!compact) {
            Text(
                text = lastScannedLabel(chore.lastScanned),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            )
        }
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

private fun lastScannedLabel(lastScanned: Instant?): String {
    if (lastScanned == null) return "Never logged"
    val days = ChronoUnit.DAYS.between(lastScanned, Instant.now())
    return when {
        days == 0L -> "Logged today"
        days == 1L -> "Logged yesterday"
        else -> "Logged ${days}d ago"
    }
}
