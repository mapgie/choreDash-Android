package com.mapgie.dash.ui.components.core

import androidx.compose.runtime.staticCompositionLocalOf
import com.mapgie.dash.data.model.ReminderLabelStyle

/**
 * The user's chosen name for the reminders feature ("Reminders", "Alarms" or
 * "Memos", Settings › Quick add button). Provided once at the top of the nav
 * graph so every sheet, dialog and caption that names the feature reads the
 * same word without threading it through each ViewModel. Screens hosted
 * outside the graph (the ringing alarm screen) read the setting themselves.
 */
val LocalReminderLabel = staticCompositionLocalOf { ReminderLabelStyle.REMINDERS }
