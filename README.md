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
    database/                 # Room DB (AppDatabase + dao/ + entities/) for saved custom themes
  di/
    AppModule.kt              # Hilt module (app-wide dependencies)
    SupabaseModule.kt         # Hilt module (Supabase client wiring)
  notification/
    NotificationHelper.kt     # Channel creation + show helpers
  permission/
    PermissionHelper.kt       # Settings deep links for exact alarms + notifications
  nfc/
    NfcHandler.kt             # NDEF/URI/raw-hex tag-ID extraction
  util/                       # DateFormat, CalendarShareUtils (.ics export)
  widget/                     # Glance home-screen widgets + update workers
  ui/
    theme/                    # Color.kt, Theme.kt, Type.kt, colour picker + saved-theme UI
    navigation/               # DashNavGraph.kt
    screens/
      chores/                 # ChoreListViewModel + ChoreListScreen
      tasks/                  # TaskListViewModel + TaskListScreen
      reminders/              # RemindersListViewModel + RemindersListScreen
      settings/               # SettingsViewModel + SettingsScreen
      licenses/               # LicensesScreen
    components/               # ChoreCard, LogBottomSheet, EditChoreSheet,
                              #   TaskCard, EditTaskSheet, AddMenuFab
```

---

## NFC setup

1. Write NDEF Text records to your NFC tags (any NFC writer app).
2. The tag ID is used to identify a chore — see `NfcHandler.extractTagId()`.
3. When the app receives an NFC intent, `LogBottomSheet` opens pre-filled with the matching chore (or shows "Unknown tag" if no chore matches).

---

## Supabase setup

This app has no backend of its own. It reads/writes a Supabase project directly
(optionally the same project as the taskDash web app). Each install needs its own
project and credentials:

1. Create a free project at [supabase.com](https://supabase.com).
2. Open **SQL Editor → New query**, paste the entire contents of
   [`supabase/schema.sql`](supabase/schema.sql), and run it. This creates the
   `owners`, `tags`, `scans`, and `todos` tables with the columns and row-level
   security policies the app expects.
3. Add at least one row to `owners` (e.g. `INSERT INTO owners (handle) VALUES ('alex');`)
   so the "I am" picker in Settings has something to show.
4. In Supabase, go to **Settings → API** and copy the **Project URL** and the
   **anon / public key**.
5. In the app, open **Settings → Supabase connection** and enter the Project URL,
   anon key, and your owner handle, then tap **Save**.

If you're pairing this app with the taskDash web app, point both at the same
Supabase project; `schema.sql` covers both apps' tables (including `todos` and
`owners`), so you only need to run it once.

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
