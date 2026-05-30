# choreDash + taskDash — Android

Native Android app combining two household tools:

- **choreDash** — chore tracker with NFC tag support. Tap a tag on the fridge, washing machine, etc. to log that chore as done.
- **taskDash** — shared to-do list with categories, priority, due dates, and per-task reminder notifications.

Both tools read from / write to the same Supabase project used by the web app.  
Credentials are entered once in the **Settings** tab and persisted in DataStore.

---

## Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1+ |
| JDK | 17 |
| Android Gradle Plugin | 8.13.x |
| Compile SDK | 35 |
| Min SDK | 26 (Android 8) |

---

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Lint
./gradlew lintDebug
```

---

## Project structure

```
app/src/main/java/com/mapgie/dash/
  DashApplication.kt          # HiltAndroidApp + WorkManager Configuration.Provider
  MainActivity.kt             # NFC foreground dispatch, Compose entry point
  alarm/
    AlarmReceiver.kt          # BroadcastReceiver for task reminders
    AlarmScheduler.kt         # AlarmManager wrapper
    BootReceiver.kt           # Re-schedules alarms after reboot
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
    NotificationHelper.kt     # Channel creation + show helpers
  nfc/
    NfcHandler.kt             # NDEF/URI/raw-hex tag-ID extraction
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
2. The tag ID is used to identify a chore — see `NfcHandler.extractTagId()`.
3. When the app receives an NFC intent, `LogBottomSheet` opens pre-filled with the matching chore (or shows "Unknown tag" if no chore matches).

---

## Supabase schema

This app connects to the same Supabase project as the web app.  
Expected tables: `chores`, `chore_logs`, `todos`, `owners`.

---

## Binary files

`app/debug.keystore` and `gradle/wrapper/gradle-wrapper.jar` are binary files.  
After cloning, copy them from a local choreDash checkout or generate a new debug keystore with:

```bash
keytool -genkey -v -keystore app/debug.keystore -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
```

---

## Open-source licenses

See **Settings → Open-source licenses** inside the app.
