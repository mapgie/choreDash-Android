# choreDash + GoFlo: Cross-App Feature Notes

Generated 2026-06-16. Covers both apps at their current state on branch `claude/goflo-choredash-features-jucifn`.

## Shared Infrastructure (identical or near-identical)

- **Settings screen pattern:** `SettingsNavItem` list + sub-screens with `SettingsSubScreenScaffold` + `SwitchRow`. Both apps share the same composable names and structure — clearly derived from the same template.
- **Changelog / What's New dialog:** Both read from `assets/CHANGELOG.md` (copied by a Gradle task), parse up to 5 entries, render the same markdown subset (`### ` headings, bullets, `**bold**`), and show Close + View full changelog buttons. Nearly copy-paste identical.
- **Versioning scheme:** Both use `MAJOR.MINOR.PATCH-beta.N`. Both use changelog fragment JSON files (`changelog/unreleased/<slug>.json`) with `check_changelog_fragment.py` CI and `consolidate_changelog.py` release automation.
- **Alarm/reminder scheduling:** Both use `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP)` with the same API 31+ `canScheduleExactAlarms()` guard and the same `goAsync()` + coroutine pattern in `BroadcastReceiver`.
- **Boot rescheduling:** Both have `BootReceiver` → `BootWorker` (HiltWorker via WorkManager) that reschedules all pending alarms on boot/update.
- **Accessibility:** Both enforce `.semantics { role = Role.X }` on every clickable, `liveRegion` for feedback, `stateDescription` on switches, `contentDescription` on icon-only controls. Both enforced by `a11y_check.py` in CI.
- **Navigation:** Both use `NavHost` with the same `popUpTo + saveState + restoreState` bottom-tab pattern.
- **DataStore:** Both use `androidx.datastore.preferences` for settings persistence.
- **Hilt DI, WorkManager, Material 3, Compose Navigation** — all the same versions and patterns.

## Key Differences

### Theme System
- **GoFlo:** 26 palette variants (Coral, Teal, Sage, 9 "fun" palettes, High Contrast, Blue & Orange colour-blind variant, custom HSV picker). Palettes stored as `ColorProfile` entities in Room. `CompactThemePicker` composable.
- **choreDash:** Single sage-green palette. SYSTEM/LIGHT/DARK segmented button only.
- **Note:** The choreDash status colours (StatusStale red, StatusAging orange, StatusFresh sage) are exactly the red-green combination that's problematic for deuteranopia (~9% of users). A Blue & Orange alternative palette would be the correct fix.

### Notification Channels
- **GoFlo:** 6 channels separating alarm/notification/silent × standard/custom/DND-bypass. User chooses delivery mode (ALARM, NOTIFICATION, SILENT) per reminder type.
- **choreDash:** 2 channels (`dash_task_reminders_v3` HIGH, `dash_chore_alerts` DEFAULT). Task reminders and standalone reminders share the same channel — users can't control them independently in system settings.

### Notification Actions
- **GoFlo:** `AlarmActionReceiver` handles snooze and log-complete from the notification shade.
- **choreDash:** No actions on notifications.

### Security
- **GoFlo:** Full 4-digit PIN (PBKDF2WithHmacSHA256, 100k iterations, random salt, constant-time compare) + biometric unlock via `BiometricPrompt`. Lock screen shown on resume if PIN set.
- **choreDash:** No security lock. (Intentional — not planned.)

### Data Storage
- **GoFlo:** Room (SQLite) for all data. Fully offline. Never adds internet permission.
- **choreDash:** Supabase (Postgrest) for chores/tasks/owners. DataStore only for reminders and settings. Requires internet.

### Widgets
- **GoFlo:** Two fully implemented widgets (2×1 status, 4×2 quick log). PIN-aware privacy guard.
- **choreDash:** Widget destination routing exists in `DashNavGraph.kt` (`WIDGET_DEST_*` intent extras) but widget `AppWidgetProvider` implementations should be verified as complete.

### Daily Digest Worker
- **choreDash:** `DailyStaleChoreWorker` checks all chores daily, shows "Overdue chores" summary notification.
- **GoFlo:** Added equivalent worker (see GoFlo cross-app notes for what it checks).

### About Section
- **GoFlo:** Includes privacy policy link, medical disclaimer, GPLv3 mention, LLM-use acknowledgement, Discord/GitHub community links.
- **choreDash:** Simpler — description, What's New, licenses, version.

### Onboarding
- **GoFlo:** Single dismissible banner on Home screen.
- **choreDash:** If `supabaseUrl` is blank, app starts directly on Settings screen. No explicit tutorial.

## GoFlo Features Available in choreDash (Post-June-2026 work)

- **Clickable version number on Chores screen** — subtle text in top bar opens What's New dialog.
- **Notification delivery mode** (ALARM/NOTIFICATION/SILENT) — user-selectable per the Reminders settings.
- **Notification actions** — snooze and mark-done from notification shade.
- **Dedicated notification channels** — separate alarm/notification/silent channels so users can tune them independently in system settings.
- **Clickable license links** — LicensesScreen now taps through to license text URLs.

## Theme Library Investigation

As of June 2026, an investigation was kicked off into packaging GoFlo's theme system (26 palettes, HSV custom picker, ColorProfile Room entity) as a standalone Android library module importable by choreDash and future apps. See investigation results when available.

## Files to Know

| App | File | Purpose |
|-----|------|---------|
| choreDash | `ui/screens/settings/SettingsScreen.kt` | All settings UI |
| choreDash | `ui/screens/settings/SettingsComponents.kt` | Shared components incl. ChangelogDialog, parseChangelog |
| choreDash | `data/preferences/SettingsRepository.kt` | DataStore-backed settings |
| choreDash | `alarm/AlarmScheduler.kt` | AlarmManager scheduling |
| choreDash | `alarm/AlarmReceiver.kt` | Alarm broadcast receiver |
| choreDash | `notification/NotificationHelper.kt` | Channels + notification builders |
| choreDash | `ui/navigation/DashNavGraph.kt` | Nav graph, bottom bar, FAB |
| GoFlo | `ui/screens/settings/SettingsScreen.kt` | All settings UI (2685 lines) |
| GoFlo | `alarm/ReminderScheduler.kt` | AlarmManager scheduling |
| GoFlo | `alarm/ReminderReceiver.kt` | Alarm broadcast receiver |
| GoFlo | `ui/theme/Color.kt` | All 26 palette definitions |
| GoFlo | `ui/theme/Theme.kt` | Theme application logic |
