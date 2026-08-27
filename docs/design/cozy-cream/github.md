repo: mapgie/choreDash-Android
branch: main
path: app/src/main/java/com/mapgie/dash

## Last sync
date: 2026-08-26T00:00:00Z

### Updated in this project
- Redesigned mobile UI in two dark directions (Editorial Dark, Warm Dusk) plus a light "Cozy Cream" direction (turn 3a) on the Modernist design system
- Turn 4a adds deeper screens from new reference shots: Edit Task sheet, Chore-detail sheet, Appearance/themes, Display + smart-visibility steppers, Reminders & alerts, About
- Grounded in real model: NFC chore logging, Fresh/Aging/Stale status, task urgency + priority, A/M assignees, Supabase-per-device sync

## Screen map
| Project screen | Built from repo files |
| --- | --- |
| Home — My Stuff | TaskListScreen.kt, TaskCard.kt, ChoreCard.kt, DashNavGraph.kt |
| Chores list | ChoreListScreen.kt, ChoreCard.kt, Chore.kt, CadenceBucket.kt, StatusTone.kt |
| Task detail | TaskOverviewSheet.kt, EditTaskSheet.kt, Task.kt |
| Bottom navigation | DashNavGraph.kt (Tasks / Chores / Reminders / Settings), AddMenuFab.kt |
| Edit Task sheet (4a) | EditTaskSheet.kt, Task.kt |
| Chore-detail sheet (4a) | ChoreDetailSheet.kt, Chore.kt |
| Appearance / themes (4a) | AppearanceScreen.kt, ThemePalette.kt, SavedThemes.kt |
| Display + smart visibility (4a) | DisplaySettingsScreen.kt, CadenceBucket.kt |
| Reminders & alerts (4a) | ReminderSettingsScreen.kt, PermissionState.kt |
| About (4a) | AboutScreen.kt |
