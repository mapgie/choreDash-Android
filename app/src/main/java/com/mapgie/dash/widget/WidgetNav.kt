package com.mapgie.dash.widget

import android.content.Context
import android.content.Intent
import com.mapgie.dash.MainActivity

/** Intent extra used by widgets to tell MainActivity where to navigate on tap. */
const val WIDGET_DESTINATION_EXTRA = "widget_destination"

const val WIDGET_DEST_QUICK_ADD_TASK = "quick_add_task"
const val WIDGET_DEST_TASKS = "tasks"
const val WIDGET_DEST_CHORES = "chores"
const val WIDGET_DEST_SETTINGS = "settings"

fun widgetActivityIntent(context: Context, destination: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra(WIDGET_DESTINATION_EXTRA, destination)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
