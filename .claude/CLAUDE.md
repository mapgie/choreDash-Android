# choreDash + taskDash — Claude Code Instructions

When fixing a bug or solving a non-obvious problem, check `LESSONS.md` for prior art. If the fix produces a transferable lesson, add it to `LESSONS.md` in the same commit.

## Versioning and changelog

Every PR that touches app code (`.kt`, `.xml`, `.gradle.kts`, `gradle/libs.versions.toml`) **must** add a changelog fragment. No exceptions.

### Scheme: `MAJOR.MINOR.PATCH[-prerelease]`

Version numbers communicate **compatibility risk**, not effort or importance.

| Bump | When to use |
|---|---|
| MAJOR | Breaking change: removes or changes behaviour users depend on, incompatible Supabase schema change, incompatible export/backup format change |
| MINOR | Backward-compatible addition: new feature, new screen, new setting, deprecation of existing behaviour |
| PATCH | Backward-compatible fix: bug fix, copy change, performance improvement, internal refactor with no user-visible impact |

When in doubt between MINOR and MAJOR, ask: can a user who doesn't update keep using the app against the same Supabase project without anything breaking? If yes → MINOR.

Pre-release suffix: `-beta.N`. Current status: **beta** (`0.x.y`) — versioning conventions are still settling.

### How to record a change (every PR)

Do **not** edit `CHANGELOG.md` or `app/build.gradle.kts`'s `versionCode`/`versionName`
directly — these are owned by the release automation and editing them in a feature PR is
the main source of merge conflicts. Instead, add **one** fragment file at
`changelog/unreleased/<short-slug>.json`:

```json
{
  "bump": "patch",
  "added": ["..."],
  "changed": ["..."],
  "fixed": ["..."]
}
```

`bump` is required (`patch`/`minor`/`major`); include only the `added`/`changed`/`fixed`
sections that apply, each a list of one-line user-facing descriptions. CI
(`changelog-check.yml`, via `check_changelog_fragment.py`) fails the PR if no valid
fragment is added. See `changelog/unreleased/README.md` for details.

### Cutting a release

The "Release" GitHub Actions workflow (`workflow_dispatch`,
`.github/workflows/release.yml`) first runs `consolidate_changelog.py`, which:
- gathers all fragments in `changelog/unreleased/`
- computes the overall bump as the highest severity among them
- bumps `versionCode` (+1) and `versionName` in `app/build.gradle.kts` — increments the
  PATCH/MINOR/MAJOR digit per the bump and resets `-beta.N` to `beta.1`
- writes one consolidated entry at the top of `CHANGELOG.md`
- deletes the consumed fragments

This commit is pushed directly to `main` (no PR), and the workflow then builds, tests,
lints, and creates a GitHub Release from that commit. If there are no fragments to
consolidate, the workflow fails immediately rather than reporting success with nothing
released, since this workflow is only run manually when a release is expected.

Promoting out of beta (dropping the `-beta.N` suffix) remains a manual edit.

### Changelog immutability rules — NO EXCEPTIONS

- **Never edit an existing entry.** Once a changelog entry is committed, its version string and change list are frozen. Treat them like a released tag.
- **Never reuse a version string.** Released versions are immutable — never re-tag, amend, or reuse a version string.
- **Never delete an entry.** Even if a feature was reverted, keep the original entry and add a new entry at the top describing the revert.
- **The "What's New" dialog shows only the 5 most recent entries.** The full list in `CHANGELOG.md` is the permanent record; users see a summary.

## Working in the web/remote environment

- This container has no Android SDK and no Gradle wrapper jar, so the app cannot be compiled here. Do not attempt Gradle builds, and do not report build failures caused by the missing toolchain. CI is the build check.
- Do not include "I couldn't compile, so I verified by inspection instead" style disclaimers in chat replies or PR descriptions. Just make the change and state what it does.

## Regression guards (unit tests)

`app/src/test` holds plain JUnit tests that run on the JVM in CI (`build.yml`, "Unit tests").
They are the feature list: each test name states one behaviour the app guarantees, in plain
words. Read `OwnerFilterTest`, `ChoreUiStateTest` and `TaskUiStateTest` as the spec for the
list screens.

- Keep list-screen behaviour (filtering, scoping, sorting, hiding, section splits) in pure
  Kotlin: the `*UiState` data classes and `data/model` enums. Never put that logic inside a
  composable or a ViewModel method where it can only be checked by hand.
- Any change to that logic adds or updates a test in the same PR, named for the behaviour
  ("`mine excludes unassigned items`"), not the method.
- Composable-only files (`ui/components`, `ui/screens/*Screen.kt`) must not be imported by
  tests; keep icon and colour choices out of the enums the tests exercise.
- Choose test timestamps mid-window (36h, 180h, 300h) so now-relative arithmetic cannot flip
  a bucket during the run.

## Where to start (a map for a fresh session)

The repo has ~145 Kotlin files. Most tasks touch one seam. Find it here before
reading code.

| To change... | Start at | Then |
|---|---|---|
| What a list shows (filter, sort, sections, hiding) | `ui/screens/<tab>/*ListViewModel.kt`, the `*UiState` class at the top | Its test in `app/src/test/.../ui/screens/<tab>/` |
| A card's look | `ui/components/<Thing>Card.kt`; badges/chips in `ui/components/core/` | Tones in `ui/theme/StatusTone.kt` |
| An edit sheet | `ui/components/Edit<Thing>Sheet.kt` / `AddReminderSheet.kt`; shared rows in `ui/components/sheet/SheetParts.kt` | Drafts in `data/model/SheetDraft.kt` |
| When a memo or tag-alarm rings, arms, advances | `data/model/Reminder.kt` (memo lifecycle) and `data/model/TagAlarm.kt` (tag-alarm rules); `ReminderSchedule.kt` for the words | `ReminderModelTest`, `TagAlarmModelTest`, `ReminderScheduleTest` |
| Arming / turning off a tag-alarm from anywhere | `tagalarm/TagAlarmService.kt` (the only entry point) | Callers: `MainActivity`, `RemindersListViewModel`, `ReminderViewViewModel` |
| What happens on an NFC tap | `MainActivity.handleNfcIntent` → `routeScannedTag` (tag-alarm first, then chore paths) | `nfc/NfcHandler.kt` for reading, writing, erasing |
| Scheduling, ringing, boot, snooze | `alarm/AlarmScheduler.kt` (`syncReminder` after every mutation), `AlarmReceiver`, `AlarmActionReceiver`, `BootWorker`, `AlarmActivity` + `AlarmRinger` | `notification/NotificationHelper.kt` for channels and the full-screen intent |
| A Settings page | `ui/screens/settings/SettingsScreen.kt` (the `SettingsSubScreen` enum and dispatch) + one `<Name>SubScreen.kt`; controls in `CozyControls.kt` | Its own `<Name>ViewModel.kt` if it has state worth testing (`TagsViewModel` is the pattern) |
| Supabase reads/writes | `data/repository/ChoreRepository.kt`, `TaskRepository.kt` | `supabase/schema.sql` for tables, RLS, grants |
| Private (phone-only) chores and tasks | `data/model/PrivateItems.kt` (the document and `privateMove`), `data/preferences/PrivateItemStore.kt` | The routing in both repositories; `PrivateItemsTest` |
| Widgets | `widget/` (Glance); destinations in `WidgetNav.kt` | `WidgetUpdater.updateAll` after data changes |
| Theme, palettes, contrast | `ui/theme/Theme.kt`, `Color.kt`, `DashTokens.kt`, `Contrast.kt` | |

Facts that save a detour:

- A **chore is a row in the `tags` table**; its `tagId` is the primary key and the
  NFC id. Chore ids and tag-alarm tag ids share one id space; a tag has one job.
- **Memos are on-device** (`ReminderRepository`, DataStore). They never reach
  Supabase. So are settings, category styles, snoozes and the sticker record.
- **Private chores and tasks are on-device too** (`PrivateItemStore`, DataStore):
  anything in the reserved `Private` category (`PRIVATE_CATEGORY`,
  `isPrivateCategory`). `TaskRepository` and `ChoreRepository` route every read
  and write by where the row lives, so widgets, alarms, NFC taps and Settings
  need no private-specific code. An edit across the boundary moves the row and
  keeps its id (`privateMove`).
- **State that crosses tabs lives on `MainActivity`** as `mutableStateOf` and is
  handed through `DashNavGraph` as parameters plus "consumed" callbacks: pending
  NFC tag, NFC write request, NFC capture, notification deep link, tag-alarm
  conflict. Follow that pattern rather than a new bus.
- **Every alarm mutation ends with `alarmScheduler.syncReminder(record)`.** It
  cancels and re-arms from the record, so the receiver, boot and snooze paths
  need no per-feature alarm code (LESSONS #57).
- **Pure state, tested.** List logic lives in `*UiState` data classes and
  `data/model`, never in composables. Tests are named for behaviours.
- **Errors shown to users go through `userFacingMessage()`** (`data/supabase/`),
  which keeps the reason and drops the request dump.
- **No build here.** The web/remote container has no Android SDK; CI is the check.
  `python3 a11y_check.py` and `python3 check_changelog_fragment.py` do run.
- `LESSONS.md` is long: `grep -n "^## [0-9]" LESSONS.md` lists the headings;
  read only the ones your task touches.

## Architecture Notes

- **UI layer:** Jetpack Compose + Material 3, MVVM with ViewModels; navigation via Compose Navigation (single Activity, `DashNavGraph.kt`)
- **DI:** Hilt (`di/AppModule.kt`, `di/SupabaseModule.kt`)
- **Data layer:** `ChoreRepository` and `TaskRepository` read/write a shared Supabase project (Postgrest) for `chores`, `chore_logs`, `todos`, `owners` — no local database for chore/task data. `SettingsRepository` (DataStore) persists Supabase credentials and user preferences locally. A small Room database (`data/database/AppDatabase.kt`, `dash.db`) stores saved custom colour themes only — schema changes need an explicit migration, never `fallbackToDestructiveMigration`.
- **Theme:** Five built-in Material 3 palettes (Cream default, implementing the "Cozy Cream" design system; Mist, Sage, Coral, Teal in `ui/theme/Color.kt`) plus a custom theme with per-role colour pickers and background overrides; light/dark/system brightness and a WCAG high-contrast toggle (`DashTheme` in `ui/theme/Theme.kt`). Headers use Lora (serif), body/UI text uses Nunito (`ui/theme/Type.kt`); shared shape/spacing tokens live in `ui/theme/Shape.kt` and `ui/theme/Dimens.kt`, and the fixed status tones (rose/amber/sage) in `ui/theme/Color.kt` + `ui/theme/StatusTone.kt`.
- **Background work:** WorkManager (`BootWorker`, `DailyStaleChoreWorker`) + AlarmManager (`AlarmScheduler`, `AlarmReceiver`, `AlarmActivity` for the full-screen ring) for task reminders and memos, scheduled via Hilt-injected workers. A tag-alarm's morning (first ring plus follow-ups) advances through the memo's `remindAt` under one alarm identity.
- **NFC:** `MainActivity` handles NFC foreground dispatch and routes a scanned id: a tag-alarm's tag arms it (`TagAlarmService`), anything else goes to the chore paths. `NfcHandler` reads ids (text record, `chordash://tag?tag=` or `chordash://memo?memo=` URI, hardware UID), writes them, and erases stickers. Settings › NFC tags (`TagsSubScreen`) is the maintenance page.
- **Permissions:** `NFC`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `INTERNET` (required for Supabase), `ACCESS_NOTIFICATION_POLICY` (lets the app appear in Settings > Do Not Disturb access and lets reminder alarms bypass Do Not Disturb). Do not add new permissions without discussion, and document the reason for each one in the manifest.

## Key Rules

- `MaterialTheme.colorScheme.error` is reserved for genuine errors and destructive confirmations — do not repurpose for general UI states
- All colour-coded states must also communicate via shape or label (not colour alone) — roughly 9% of users have red-green colour blindness
- Minimum tap target: 44x44dp
- Never hardcode colours in `TextStyle` / typography — let `MaterialTheme` propagate `LocalContentColor`
- Supabase credentials live only in DataStore via `SettingsRepository` — never log them or write them to the changelog/commit messages
- **Never use en dashes (–) or em dashes (—) in user-facing text.** They read as robotic. Use a period, colon, or reword the sentence instead. Hyphens in genuine compound words ("in-app", "4-digit", "built-in", "30-day") are fine.

## Accessibility Rules (enforced by `a11y_check.py` in CI)

Every `.clickable {}` or `.combinedClickable {}` modifier **must** carry a matching `.semantics { role = Role.<Type> }` in the same modifier chain. Use the role that best describes the element:

| Role | Use for |
|---|---|
| `Role.Button` | Navigation, generic action, expand/collapse, dialog dismiss |
| `Role.RadioButton` | Mutually exclusive single-select (theme pickers, filter selectors) |
| `Role.Checkbox` | Toggle with two named states where the element acts as a row wrapping a Checkbox |
| `Role.Switch` | Toggle with two named states; pair with `stateDescription` to announce current state |

Additional rules:
- Place `.semantics { role = }` **before** `.clickable {}` / `.combinedClickable {}` in the chain when the clickable lambda is longer than a few lines, so the CI window check can find it.
- When the parent Row/Box handles the click, set the inner `Checkbox` / `RadioButton` to `onClick = null` to prevent double-focus.
- `clearAndSetSemantics { }` must also include `role = Role.<Type>` — it replaces all child semantics, so the role must be re-declared there.
- Status text that appears or changes in response to user action needs `Modifier.semantics { liveRegion = LiveRegionMode.Assertive }` (errors) or `LiveRegionMode.Polite` (non-urgent feedback).
- Icon-only interactive controls (FABs, icon-only buttons outside of `IconButton`) need `Modifier.semantics { contentDescription = "<action label>" }` on the container itself.
- Run `python3 a11y_check.py` locally before pushing to confirm no new violations.
