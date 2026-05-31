# Changelog

All notable changes to the Android app are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased]

### Fixed
- CI: provision Gradle 8.13 directly via `gradle/actions/setup-gradle` instead of
  relying on `gradle-wrapper.jar`, which cannot be committed through the GitHub API
- Compose BOM bumped to 2024.09.00 (Material3 1.3.0) to gain `PullToRefreshBox`
  and align `ModalBottomSheetProperties` signature (removed `securePolicy` param)
- Added missing `lifecycle-runtime-compose` dependency for `collectAsStateWithLifecycle`
- `TaskRepository.pendingReminders`: replaced unavailable `isNull()` DSL call with
  a Kotlin-side `completedAt == null` filter
- `EditTaskSheet`: fixed `SecureFlagPolicy` import (`material3` → `compose.ui.window`)
- `SettingsViewModel.clearSaveError`: fixed assignment-as-expression syntax error
- Removed custom `debug.keystore` signing config; CI now uses the default Android
  debug keystore so packaging does not fail in a clean environment

## [1.0.0] — 2026-05-30

### Added
- choreDash tab: chore list with staleness indicators, NFC tap-to-log,
  swipe-to-log with undo, group-by-category, archive/unarchive
- taskDash tab: task list with priority/due/created sort, filter chips
  (All / Active / Done), group-by-category, collapsible done section,
  task reminders via AlarmManager (exact alarms)
- Settings tab: Supabase URL + anon-key entry, owner selection,
  light/dark/system theme toggle
- NFC foreground dispatch in MainActivity; NDEF text/URI/raw-hex tag-ID extraction
- DailyStaleChoreWorker: once-a-day notification summarising overdue chores
- BootWorker: re-schedules pending task reminders after device reboot
- Open-source licenses screen
- Material3 dynamic-color theming seeded from #4A7C59 (sage green)
