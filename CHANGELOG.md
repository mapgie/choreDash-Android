# Changelog

## Versioning policy

Format: `MAJOR.MINOR.PATCH-beta.N` (pre-release) or `MAJOR.MINOR.PATCH` (release).

| Bump | When |
|---|---|
| MAJOR | Breaking change: removes or changes behaviour users depend on, incompatible Supabase schema change, incompatible export/backup format change |
| MINOR | Backward-compatible addition: new feature, new screen, new setting, deprecation of existing behaviour |
| PATCH | Backward-compatible fix: bug fix, copy change, performance improvement, internal refactor with no user-visible impact |

Rules:
- MINOR bump resets PATCH to 0 (`1.4.2 -> 1.5.0`); MAJOR resets MINOR and PATCH (`1.4.2 -> 2.0.0`)
- Released versions are immutable - never re-tag, never amend, never delete an entry

### Contributing a change (every PR)

Do **not** edit this file or bump `versionCode`/`versionName` in `app/build.gradle.kts`
directly. Instead, add a fragment file at `changelog/unreleased/<short-slug>.json`
describing the change and its bump level - see `changelog/unreleased/README.md` for the
format. New fragment files never conflict between PRs.

### Cutting a release

Run the "Prepare release" GitHub Actions workflow (`workflow_dispatch`). It consolidates
all pending fragments in `changelog/unreleased/` into a single new entry below, bumps
`versionCode` (+1) and `versionName` accordingly, removes the consumed fragments, and opens
a PR for review. The bump increments the corresponding version digit (PATCH/MINOR/MAJOR per
the table above) and resets `-beta.N` to `beta.1`. Promoting out of beta (dropping the
`-beta.N` suffix) remains a manual edit.

---
## [0.15.6] - 2026-06-24

### Fixed
- Tasks showing an error on first navigation after a cold start when Chores loaded before Tasks

---
## [0.15.4] - 2026-06-22

### Fixed
- App no longer crashes on launch on devices where the Google Fonts provider is slow to respond or unavailable (non-GMS devices, restricted work profiles). Nunito and Lora are now bundled with the app instead of downloaded at runtime, so fonts always load regardless of network or GMS availability.

---
## [0.15.3] - 2026-06-22

### Fixed
- Home screen widgets (Quick Add, Next Up, Pinned Item) no longer silently fail to update on Android 12 and above. All three AppWidget receivers were incorrectly marked exported=false, blocking the system update broadcast.

---
## [0.15.2] - 2026-06-21

### Changed
- Renamed "Remove last log" to "Remove latest log" for clarity
- Replaced manual hour/minute text fields and text-input time picker with native clock time pickers for chore logging and task reminder times
- Category labels on chore and task cards now appear as small tinted badges for faster scanning
- Task owner badge is now smaller and hidden when the 'mine' filter is active, reducing visual noise
- Chore cards now share the same visual style as task cards (surface colour, circular owner badge)

### Fixed
- "Remove latest log" now always deletes the most recent log entry; previously it could delete an older entry when a log was added after the screen last loaded

---
## [0.15.0] - 2026-06-21

### Changed
- Theme palette picker now shows palette name only (Light/Dark/System is controlled by the mode toggle above)
- Each palette card now shows primary, secondary, and tertiary colour swatches
- Custom colour editor now exposes full HSL control (hue, saturation, lightness) per colour role
- Custom theme name and save controls are now inline in the colour editor. Load a saved theme to get Update and Save as new options.

---
## [0.14.2] - 2026-06-21

### Fixed
- Chore slug tag displayed in monospace to visually distinguish it from the chore name

---
## [0.14.1] - 2026-06-21

### Changed
- High-priority task cards now use amber instead of red on the left strip, reserving red exclusively for overdue items across both chores and tasks
- Chore cards show the last-scan date in normal view; the overdue/countdown text is now shown only when the bolt mode is active
- Chore cards show a very subtle status-colour background tint in normal mode
- Archived and distant-chore section toggle buttons use a subtler, label-style appearance

---
## [0.14.0] - 2026-06-21

### Added
- Chores not due for 60+ days are hidden from the main list. A tap-to-reveal button at the bottom shows how many are hidden, and tapping it expands or collapses them.

### Changed
- App typography now uses Nunito (body and UI text) and Lora (headings and sheet titles) to match the web app
- Sheet and modal titles enlarged to 32sp serif for clearer visual hierarchy
- Category labels, status text, and metadata use secondary colour roles for layered emphasis
- Action buttons in chore and task sheets grouped as labelled chips for better discoverability
- Destructive actions (Archive, Delete, Remove log) demoted to text buttons so primary actions dominate
- Advanced fields in edit sheets moved behind progressive disclosure to reduce visual clutter
- Log history wrapped in a tonal surface card matching the web app aesthetic
- Spacing throughout sheets follows an 8pt rhythm for consistent visual breathing room

---
## [0.13.0] - 2026-06-21

### Changed
- Theme palette picker now shows palette name only (Light/Dark/System is controlled by the mode toggle above)
- Each palette card now shows primary, secondary, and tertiary colour swatches
- Custom colour editor now exposes full HSL control (hue, saturation, lightness) per colour role

---
## [0.12.0] - 2026-06-20

### Added
- Palette theme selection (multiple colour palettes with light, dark, and system variants)
- Custom HSL colour theme with primary, secondary, and tertiary hue sliders
- Save, load, rename, and delete multiple named custom colour themes

### Changed
- Chore cards redesigned: single-row layout with title vertically centred, prominent due date, and subtle last-done timestamp
- Removed outline border from chore and task cards that was added in a previous change
- App typography now uses Nunito (body and UI text) and Lora (screen titles) to match the web app

---
## [0.11.2] - 2026-06-20

### Changed
- Chore filter chips are now always visible (including in zen mode) and use the same implementation pattern as task filter chips
- Zen mode now hides all action bar buttons except the exit control and a due-date sort toggle (most overdue first / recently done first)
- The due countdown (thunderbolt) button is now hidden in zen mode

---
## [0.11.1] - 2026-06-20

### Changed
- Chore and task lists now default to grouped-by-category view
- Category labels are hidden on individual cards when the grouped view is active, since the section header already shows the category
- Chore and task cards now have a subtle outline border to improve separation from the background
- Chore cards now show at most two date representations instead of three, reducing visual noise

### Fixed
- Chore list no longer hides the last item behind the floating action button

---
## [0.11.0] - 2026-06-19

### Added
- NFC tags written by choreDash now open the app and log the chore as done automatically, even when the app is closed or in the background. A toast confirms the log.

### Changed
- Licences screen redesigned to group libraries by licence type, show copyright holders, and use the primaryContainer top bar colour.
- Version number moved from the filter bar to a footer at the bottom of the chore list.

---
## [0.10.0] - 2026-06-18

### Added
- Notification delivery mode setting: choose Alarm (bypasses Do Not Disturb), Notification, or Silent
- Snooze (15 min) and Done actions directly from task and reminder notifications
- Version number in the Chores screen header opens the What's New changelog

### Changed
- Task reminders and standalone reminders now use separate notification channels, allowing independent control in system settings
- Open-source license entries now link directly to their license texts

---
## [0.9.0] - 2026-06-16

### Added
- Chores now support reminders: tap 'Add reminder...' in the chore view sheet to create a reminder linked to a chore
- Task reminders now appear in the Reminders list, so all scheduled reminders are visible in one place
- Add to calendar button is now available in the task and chore view sheets, not just the edit sheets

### Changed
- Reminder edit sheet now uses a native time picker instead of separate HH and MM text fields
- Archive and Delete actions on the reminder edit sheet are now lower-emphasis text buttons to better reflect their secondary role
- Reminder edit sheet shows a metadata summary (scheduled date, creation date, archived state) when editing an existing reminder

### Fixed
- Past and overdue reminders in the Reminders list now display with a distinct style (red timestamp prefix and warm background) instead of looking like scheduled future alarms
- Past task reminder bell icons on task cards now appear muted instead of highlighted, reflecting that the alarm time has passed
- Reminder notifications now reliably play a sound on all devices

---
## [0.8.0] - 2026-06-15

### Added
- Zen mode toggle on the Chores screen for a distraction-free view that hides filters, categories, and badges
- Due countdown toggle on the Chores screen showing "in Xd" / "Xd overdue" and sorting the most urgent chores to the top
- Documented Supabase setup for new installs, including a ready-to-run schema.sql covering owners, tags, scans, and todos
- Added a setup hint to Settings > Supabase connection pointing new users to the schema and README

### Changed
- Chore cards now show the last-scanned date alongside a relative time (e.g. "2w ago"), matching the choreDash web app

---
## [0.7.1] - 2026-06-15

### Changed
- The task overview sheet now shows icons on the Mark done/Restore task and Edit task actions, matching the taskDash web app
- The chore overview sheet now shows the category before the chore name and its tag ID, icons on the Log it and Remove last log actions, and a Recent History list styled to match the taskDash web app

---
## [0.7.0] - 2026-06-15

### Added
- Tapping a task now opens an overview sheet showing its due status, a mark done/restore action, and an edit option
- Long-pressing a task card opens its edit sheet directly, matching chore cards

### Changed
- Pinning a task to the home screen widget now happens from the overview sheet instead of an icon on the task card

---
## [0.6.0] - 2026-06-15

### Added
- Add a chore or task to your calendar directly from its edit screen
- Share a chore or task as a calendar event (.ics file) or as plain text
- Tapping a chore now opens an overview sheet with last-done info, a custom-time log option, recent history (last 4 logs), and a remove-last-log action
- Reminders can now be edited, archived, and unarchived by tapping a reminder
- Swipe a reminder left to reveal a delete option

### Changed
- Pinning a chore to the home screen widget now happens from the overview sheet instead of an icon on the chore card
- The Reminders screen now shows an archived section for hidden reminders
- Swiping a reminder right toggles done/undone, matching tasks and chores
- Reminders whose time has passed now move to the Done section even if not checked off

---
## [0.5.1] - 2026-06-14

### Fixed
- Added the ACCESS_NOTIFICATION_POLICY permission so the app can actually be granted Do Not Disturb access; without it the permission row in Settings could never be granted

---
## [0.5.0] - 2026-06-14

### Changed
- Settings screen now uses a flat, grouped list with dedicated sub-screens for Connection, Appearance, Reminders & Alerts, and About
- Replaced the full-screen Changelog screen with a "What's New" dialog on the About sub-screen showing the 5 most recent changelog entries

---
## [0.4.0] - 2026-06-14

### Added
- Write a chore's tag ID to an NFC tag directly from the edit sheet

### Changed
- Settings now shows live status for Notifications, Exact alarms, and Do Not Disturb access, and only says reminders are fully enabled when all three are granted
- Tasks screen now matches the Chores screen layout: pull to refresh, sticky category headers, and icon-button controls for sort, grouping, and owner filtering
- Swipe a task to mark it done (or undo), mirroring the swipe-to-log gesture on Chores
- Task owner filter is now a two-state My tasks / All toggle, matching the Chores owner filter

### Fixed
- Task reminder alarms now request to bypass Do Not Disturb, matching the claim in Settings
- Task reminder notification channel is recreated so the Do Not Disturb bypass takes effect on existing installs (notification channel settings cannot be changed after creation)

---
## [0.3.0] - 2026-06-14

### Added
- New expandable "Add" FAB on the Chores, Tasks, and Reminders tabs to quickly create a chore, task, or reminder from anywhere in the app
- New "Reminders" tab for simple, on-device-only reminders (not synced to Supabase)
- Home screen widgets: Quick Add Task, Next Up (your most urgent task or chore), and a Pinned Task/Chore widget, all resizable from small to large
- Pin a task or chore from its card in the app to show it in the Pinned widget, with a checkbox or Log Now button to complete it without opening the app
- Settings now shows whether notifications and exact alarms are enabled, with links to the system settings screens to fix them

### Fixed
- PendingIntents for task/reminder alarms and notifications now set an explicit package to satisfy Android's implicit-intent security requirements
- Excluded the java/android/pending-intents CodeQL query, which produces false positives on Kotlin's `or` flag syntax even when FLAG_IMMUTABLE is present

---
## [0.1.2] - 2026-06-13

### Added
- Settings screen now shows the app version below "Open-source licenses".
  Tapping it opens a Changelog screen listing the 5 most recent entries
  from `CHANGELOG.md`.
- "Add chore" FAB on the Chores tab opens a new sheet to create a chore
  (tag ID, label, category, owner, interval). Scanning an unrecognised NFC
  tag now opens the same sheet pre-filled with that tag's ID, instead of
  pointing at the retired web app.
- `.claude/CLAUDE.md` with project-specific versioning, architecture, and accessibility guidance
- `a11y_check.py` accessibility role checker (ported from Android App Template)

### Fixed
- Restored a consistent debug signing config pointing at the committed
  `app/debug.keystore` (unique to this app). Previously CI relied on AGP's
  auto-generated debug keystore, so every clean build produced a different
  signing certificate, making sideloaded installs repeatedly look like a
  brand-new "unrecognized developer" to Play Protect.
- `ChoreRepository.logChore` and `TaskRepository.addTask`/`updateTask` now
  request `select()` on insert/update so Supabase returns the affected row
  instead of an empty body - fixes "Expected start of the array '[', but
  had 'EOF'" when logging a chore or saving a task.
- `EditChoreSheet`, `LogBottomSheet`, and `EditTaskSheet` now allow the
  system back button to dismiss the sheet, and route swipe/scrim/back
  dismissal through `hide()` + `onDismiss()` so the sheet always closes
  cleanly (previously the back button did nothing).
- Added missing `.semantics { role = Role.Button }` to the two `.combinedClickable` modifiers in `ChoreListScreen.kt` (active and archived chore rows)
- Removed stale `<receiver android:name=".alarm.DailyChoreCheckReceiver">` entry from
  AndroidManifest.xml - the class was never implemented; the daily overdue-chore
  check is already handled by `DailyStaleChoreWorker` via WorkManager.
- Suppressed `MissingPermission` lint errors on `NotificationManagerCompat.notify()` calls - `POST_NOTIFICATIONS` is already declared in the manifest and requested at runtime; lint cannot see across that boundary in these helper/worker classes.
- Fixed `ModalBottomSheetProperties` constructor call in `EditChoreSheet`, `EditTaskSheet`, and `LogBottomSheet`. The `(isFocusable, shouldDismissOnBackPress)` overload without `securePolicy` does not exist in Material3 1.3.0; removed `isFocusable` (defaults to true).
- CI: provision Gradle 8.13 directly via `gradle/actions/setup-gradle` instead of
  relying on `gradle-wrapper.jar`, which cannot be committed through the GitHub API
- Compose BOM bumped to 2024.09.00 (Material3 1.3.0) to gain `PullToRefreshBox`
  and align `ModalBottomSheetProperties` signature (removed `securePolicy` param)
- Added missing `lifecycle-runtime-compose` dependency for `collectAsStateWithLifecycle`
- `TaskRepository.pendingReminders`: replaced unavailable `isNull()` DSL call with
  a Kotlin-side `completedAt == null` filter
- `EditTaskSheet`: fixed `SecureFlagPolicy` import (`material3` -> `compose.ui.window`)
- `SettingsViewModel.clearSaveError`: fixed assignment-as-expression syntax error
- Removed custom `debug.keystore` signing config; CI now uses the default Android
  debug keystore so packaging does not fail in a clean environment
- `menuAnchor()` calls updated to `menuAnchor(MenuAnchorType)` in `EditChoreSheet`,
  `SettingsScreen`, and `EditTaskSheet` - Material3 1.3.0 deprecated the
  parameterless overload at `DeprecationLevel.ERROR`

---
## [1.0.0] — 2026-05-30

### Added
- Initial Android app combining choreDash (NFC chore tracker) and taskDash
  (shared to-do list), connecting to the same Supabase project as the web app
- choreDash tab: chore list with staleness colour bars, NFC tap-to-log,
  swipe-to-log with undo snackbar, group-by-category, archive/unarchive
- taskDash tab: task list with priority/due/created sort, All/Active/Done
  filter chips, group-by-category, collapsible done section, per-task
  AlarmManager reminders, owner filter
- Settings tab: Supabase URL + anon-key, owner dropdown, light/dark/system
  theme toggle, open-source licenses screen
- NFC foreground dispatch in MainActivity; NDEF text/URI/raw-hex tag-ID extraction
- DailyStaleChoreWorker (WorkManager periodic): daily overdue-chore notification
- BootWorker: re-schedules pending task reminders after reboot
- Material3 theme seeded from `#4A7C59` (sage green)
- CI workflows: build + release APK, changelog check, license-sync check, CodeQL
