package com.mapgie.dash.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Refreshes the content of every choreDash home screen widget. */
object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        QuickAddTaskWidget().updateAll(context)
        NextUpWidget().updateAll(context)
        PinnedItemWidget().updateAll(context)
    }
}
