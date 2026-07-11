package com.mapgie.dash.data.model

/** User-chosen wording for the reminders feature, applied wherever it's named in the UI. */
enum class ReminderLabelStyle(val displayName: String, val singular: String) {
    REMINDERS("Reminders", "Reminder"),
    ALARMS("Alarms", "Alarm"),
    MEMOS("Memos", "Memo"),
}
