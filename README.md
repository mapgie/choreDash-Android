# Urgh + taskDash — Android

A native Android app for a household: three tools in one, sharing one Supabase
project with the taskDash web app.

- **Chores**: recurring jobs tracked by NFC stickers. Tap the tag on the washing
  machine and the chore is logged; the list shows what is overdue, due soon, or
  fresh, with owners, categories, snoozes and smart visibility.
- **Tasks**: a shared to-do list with categories, priority, due dates or periods,
  and per-task reminders.
- **Memos**: on-device reminders in three shapes. A once-only memo rings once. A
  repeating memo rings on chosen weekdays. A **tag-alarm** is a dormant morning
  alarm that a tap on its NFC tag arms for the next morning only, for people whose
  mornings follow no fixed pattern.

Around them: home-screen widgets (Next up, a pinned item, quick-add), five colour
palettes plus a custom theme builder with a WCAG high-contrast mode, and a
Settings tab that also covers notification style, categories, and NFC tag
maintenance.

Current status: beta (`0.x.y`). See [`CHANGELOG.md`](CHANGELOG.md).

---

## Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1+ |
| JDK | 17 |
| Android Gradle Plugin | 8.13.x |
| Compile SDK | 35 |
| Min SDK | 26 (Android 8) |

The phone needs NFC for chores and tag-alarms; everything else works without it.

---

## Build and check

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # JVM unit tests (app/src/test)
./gradlew lintDebug            # Android lint
python3 a11y_check.py          # every clickable carries a semantics role
python3 check_changelog_fragment.py   # the PR has a valid changelog fragment
```

Unit tests are the behaviour spec for the list screens and the memo rules:
each test name states one thing the app guarantees, in plain words. Read
`OwnerFilterTest`, `ChoreUiStateTest`, `TaskUiStateTest`, `ReminderUiStateTest`
and `TagAlarmModelTest` before changing what a list shows or when an alarm rings.

---

## Project structure

```
app/src/main/java/com/mapgie/dash/
  DashApplication.kt          # HiltAndroidApp + WorkManager configuration
  MainActivity.kt             # Single activity: NFC dispatch and routing, notification deep links, theme
  alarm/                      # AlarmScheduler (AlarmManager), AlarmReceiver, AlarmActivity + AlarmRinger
                              #   (the full-screen ring), BootWorker, DailyStaleChoreWorker
  tagalarm/                   # TagAlarmService: the one place a tag-alarm is armed or turned off
  nfc/                        # NfcHandler: read a tag's id, write or erase a tag; NfcWriteRequest
  notification/               # Channels, delivery styles (Alarm / Notification / Silent), permissions
  permission/                 # Settings deep links for exact alarms, notifications, full-screen, DND
  data/
    model/                    # Chore, Task, Owner, Reminder (memo) + the pure rules:
                              #   TagAlarm.kt, ReminderSchedule.kt, OwnerFilter, sort keys, drafts
    repository/               # ChoreRepository, TaskRepository (Supabase); ReminderRepository (on-device)
    preferences/              # DataStore: SettingsRepository, CategoryStyleStore, ChoreSnoozeStore,
                              #   TagStickerStore (which ids this phone has met on a sticker)
    supabase/                 # SupabaseClientProvider, user-facing error trimming
    database/                 # Room (dash.db): saved custom colour themes only
  di/                         # Hilt modules
  ui/
    navigation/               # DashNavGraph: tabs, the speed dial, app-level dialogs
    screens/
      chores/ tasks/ reminders/   # One *ListScreen + *ListViewModel each; *UiState is pure and tested
      reminder/               # The full-screen ring / nudge view a notification opens
      settings/               # SettingsScreen + one *SubScreen.kt per page (Categories, NFC tags, ...)
      licenses/
    components/               # Cards, edit sheets, dialogs; core/ (header, badges, chips), sheet/ (sheet parts)
    theme/                    # Palettes, tokens, typography, colour picker, saved themes
  widget/                     # Glance widgets + refresh workers
  util/                       # Date formatting, .ics export
app/src/test/                 # JUnit tests on the JVM; no Android
supabase/schema.sql           # The shared schema: tables, RLS policies, grants; idempotent
changelog/unreleased/         # One JSON fragment per PR; the release workflow consolidates them
LESSONS.md                    # Numbered lessons from bugs already fixed; check before fixing a new one
.claude/CLAUDE.md             # Working rules and a map of where things live
```

---

## Where data lives

| Data | Where | Shared with other phones |
|---|---|---|
| Chores (`tags`), scans, tasks (`todos`), owners | Supabase | Yes |
| Memos, including tag-alarms and their tag ids | DataStore on the phone | No |
| Settings, credentials, category styles, snoozes, sticker record | DataStore on the phone | No |
| Saved custom themes | Room on the phone | No |

A chore **is** a row in the `tags` table, and its `tag_id` is both its primary key
and the id its NFC sticker carries. Memos never touch Supabase.

---

## NFC

Every tag the app reads resolves to one id: an NDEF text record, the `tag` or
`memo` query parameter of a `chordash://` URI, or the sticker's hardware UID when
it carries nothing. One id space serves chores and tag-alarms, and **a tag has
one job**: an id a chore owns cannot be a tag-alarm's, and the other way round.

- **Chore tags.** Tap a sticker: with the app open, the log sheet opens for that
  chore; from the home screen, the chore is logged and a toast confirms it. A
  sticker no chore knows opens a new chore with the id filled in. Write a chore's
  id to a blank sticker from its edit sheet (`chordash://tag?tag=<id>`).
- **Tag-alarm tags.** Name the tag from the memo's Tag row ("Office A" becomes
  `office-a`), write it to a blank sticker (`chordash://memo?memo=<id>`), or scan
  a card that already carries an id (an office pass). A tap sets the alarm for the
  next time its first ring comes round, today or tomorrow, weekday ignored, and
  the alarm is dormant again after that morning. Tapping again never turns it off.
- **Settings › NFC tags.** Identify any tag (what it belongs to), list every
  chore's and tag-alarm's tag, write an id to a sticker, erase a sticker, unlink a
  tag-alarm, and filter chores by whether this phone has met them on a sticker.

NFC needs the screen on and unlocked. An unformatted sticker works too; the app
formats it on the first write.

---

## Supabase setup

The app has no backend of its own. Each install points at a Supabase project,
optionally the same one as the taskDash web app.

1. Create a project at [supabase.com](https://supabase.com).
2. Open **SQL Editor → New query**, paste all of
   [`supabase/schema.sql`](supabase/schema.sql), and run it. It creates `owners`,
   `tags`, `scans` and `todos` with their row-level security policies and the
   table grants the Data API needs. Every statement is idempotent, so re-running
   it later is safe and never touches rows.
3. Add at least one row to `owners`, for example
   `INSERT INTO owners (handle) VALUES ('alex');`.
4. In Supabase, **Settings → API**: copy the **Project URL** and the publishable
   (anon) key.
5. In the app, **Settings → Supabase connection**: enter both and your owner
   handle, then Save.

If a request ever fails with "permission denied for table ...", the table is
missing its grant: re-run the grants block at the bottom of `schema.sql`. See
[`supabase/README.md`](supabase/README.md) for the optional workflow that applies
the schema automatically on merge.

---

## Contributing

- **Changelog.** Every PR that touches app code adds one fragment at
  `changelog/unreleased/<slug>.json` with a `bump` (`patch` / `minor` / `major`)
  and the user-facing lines. Never edit `CHANGELOG.md` or the version in
  `app/build.gradle.kts` by hand; the **Release** workflow consolidates fragments,
  bumps the version, and publishes the APK.
- **CI on every PR:** build and unit tests, lint, CodeQL, the accessibility role
  check, the changelog fragment check, and a licence screen sync check.
- **Rules of the house** are in [`.claude/CLAUDE.md`](.claude/CLAUDE.md):
  versioning, accessibility (every clickable has a role, colour is never the only
  signal, 44dp targets), no dashes in user-facing text, credentials never logged.
  [`LESSONS.md`](LESSONS.md) holds the numbered lessons from bugs already fixed.

---

## Binary files

`app/debug.keystore` and `gradle/wrapper/gradle-wrapper.jar` are binary files.
After cloning, copy them from a local choreDash checkout or generate a new debug
keystore with:

```bash
keytool -genkey -v -keystore app/debug.keystore -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
```

---

## Open-source licences

See **Settings → About → Open-source licences** inside the app.
