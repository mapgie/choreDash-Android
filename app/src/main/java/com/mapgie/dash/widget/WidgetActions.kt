package com.mapgie.dash.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

val TaskIdKey = ActionParameters.Key<String>("task_id")
val MarkDoneKey = ActionParameters.Key<Boolean>("mark_done")
val ChoreTagIdKey = ActionParameters.Key<String>("chore_tag_id")

private fun widgetEntryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)

/** Marks a task done or undone from the Next Up / Pinned widgets. */
class ToggleTaskDoneAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val markDone = parameters[MarkDoneKey] ?: true
        val taskRepository = widgetEntryPoint(context).taskRepository()
        runCatching {
            if (markDone) taskRepository.markDone(taskId) else taskRepository.markUndone(taskId)
        }
        WidgetUpdater.updateAll(context)
    }
}

/** Logs a chore scan from the Next Up / Pinned widgets. */
class LogChoreAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val tagId = parameters[ChoreTagIdKey] ?: return
        val choreRepository = widgetEntryPoint(context).choreRepository()
        runCatching { choreRepository.logChore(tagId) }
        WidgetUpdater.updateAll(context)
    }
}
