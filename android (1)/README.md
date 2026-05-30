# choreDash + taskDash — Android

Native Android app combining two household tools:

- **choreDash** — chore tracker with NFC tag support. Tap a tag on the fridge, washing machine, etc. to log that chore as done.
- **taskDash** — shared to-do list with categories, priority, due dates, and per-task reminder notifications.

Both tools read from / write to the same Supabase project used by the web app.  
Credentials are entered once in the **Settings** tab and persisted in DataStore.

---

## Requirements

| Tool | Version |
|------|------|
| Android Studio | Hedgehog 2023.1+ |
| JDK | 17 |
| Android Gradle Plugin | 8.5.x |
| Compile SDK | 35 |
| Min SDK | 26 (Android 8) |

---

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

---

## Project structure

```
android/
  app/src/main/java/com/mapgie/dash/
    DashApplication.kt          # HiltAndroidApp + WorkManager Configuration.Provider
    MainActivity.kt             # NFC foreground dispatch, Compose entry point
    alarm/
      AlarmReceiver.kt          # (data layer) BroadcastReceiver for task reminders
      AlarmScheduler.kt         # (data layer) AlarmManager wrapper
      BootReceiver.kt           # (data layer) re-schedules alarms after reboot
      BootWorker.kt             # HiltWorker: queries pending reminders on boot
      DailyStaleChoreWorker.kt  # HiltWorker: daily stale-chore notification
    data/
      model/                    # Chore.kt, Task.kt, Owner.kt + enums/extension fns
      preferences/              # SettingsRepository (DataStore)
      repository/               # ChoreRepository, TaskRepository (Supabase)
      supabase/                 # SupabaseClientProvider
    di/
      AppModule.kt              # Hilt modules
    notification/
      NotificationHelper.kt     # (data layer) channel creation + show helpers
    nfc/
      NfcHandler.kt             # (data layer) NDEF/URI/raw-hex tag-ID extraction
    ui/
      theme/                    # Color.kt, Theme.kt, Type.kt
      navigation/               # DashNavGraph.kt
      screens/
        chores/                 # ChoreListViewModel + ChoreListScreen
        tasks/                  # TaskListViewModel + TaskListScreen
        settings/               # SettingsViewModel + SettingsScreen
        licenses/               # LicensesScreen
      components/               # ChoreCard, LogBottomSheet, EditChoreSheet,
                                #   TaskCard, EditTaskSheet
```

---

## NFC setup

1. Write NDEF Text records to your NFC tags (any NFC writer app).
2. The tag ID is hashed to identify a chore — see `NfcHandler.extractTagId()`.
3. When the app receives an NFC intent, `LogBottomSheet` opens pre-filled with the matching chore (or shows "Unknown tag" if no chore matches).

---

## Supabase schema

This app connects to the same Supabase project as the web app.  
Expected tables: `chores`, `chore_logs`, `todos`, `owners`.

---

## Open-source licenses

See **Settings → Open-source licenses** inside the app, or [`LICENSES`](LICENSES).
