# choreDash + taskDash — Claude Code Instructions

When fixing a bug or solving a non-obvious problem, check `LESSONS.md` for prior art. If the fix produces a transferable lesson, add it to `LESSONS.md` in the same commit.

## Versioning and changelog

Every PR that changes app code (`.kt`, `.xml`, `.gradle.kts`, `gradle/libs.versions.toml`) **must** update `CHANGELOG.md`. CI (`changelog-check.yml`) fails the PR otherwise.

### Scheme: `MAJOR.MINOR.PATCH[-prerelease]`

Version numbers communicate **compatibility risk**, not effort or importance.

| Bump | When to use |
|---|---|
| MAJOR | Breaking change: removes or changes behaviour users depend on, incompatible Supabase schema change, incompatible export/backup format change |
| MINOR | Backward-compatible addition: new feature, new screen, new setting, deprecation of existing behaviour |
| PATCH | Backward-compatible fix: bug fix, copy change, performance improvement, internal refactor with no user-visible impact |

When in doubt between MINOR and MAJOR, ask: can a user who doesn't update keep using the app against the same Supabase project without anything breaking? If yes → MINOR.

Pre-release suffix: `-beta.N`. Current status: **beta** (`0.x.y`) — versioning conventions are still settling.

### How to bump (every PR)

1. Update `versionCode` (+1) and `versionName` in `app/build.gradle.kts`
2. Add an entry under `## [Unreleased]` (or a new version heading) at the top of `CHANGELOG.md`, following the existing [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format (`### Added` / `### Changed` / `### Fixed`)
3. Include both changes in the same commit as the feature/fix

### Changelog immutability rules — NO EXCEPTIONS

- **Never edit an existing entry.** Once a changelog entry is committed, its version string and change list are frozen. Treat them like a released tag.
- **Never reuse a version string.** Released versions are immutable.
- **Never delete an entry.** Even if a feature was reverted, keep the original entry and add a new entry describing the revert.
- **Merge conflicts in CHANGELOG.md must preserve both sides.** Keep all entries from both branches, newest at top.

## Working in the web/remote environment

- This container has no Android SDK and no Gradle wrapper jar, so the app cannot be compiled here. Do not attempt Gradle builds, and do not report build failures caused by the missing toolchain. CI is the build check.
- Do not include "I couldn't compile, so I verified by inspection instead" style disclaimers in chat replies or PR descriptions. Just make the change and state what it does.

## Architecture Notes

- **UI layer:** Jetpack Compose + Material 3, MVVM with ViewModels; navigation via Compose Navigation (single Activity, `DashNavGraph.kt`)
- **DI:** Hilt (`di/AppModule.kt`, `di/SupabaseModule.kt`)
- **Data layer:** No local database. `ChoreRepository` and `TaskRepository` read/write a shared Supabase project (Postgrest) for `chores`, `chore_logs`, `todos`, `owners`. `SettingsRepository` (DataStore) persists Supabase credentials and user preferences locally.
- **Theme:** Single sage-green Material 3 palette (`ui/theme/Color.kt`) with light, dark, and system-following variants (`DashTheme` in `ui/theme/Theme.kt`). Body and UI text use the system default font (`ui/theme/Type.kt`) — no brand font.
- **Background work:** WorkManager (`BootWorker`, `DailyStaleChoreWorker`) + AlarmManager (`AlarmScheduler`, `AlarmReceiver`) for task reminders, scheduled via Hilt-injected workers.
- **NFC:** `MainActivity` handles NFC foreground dispatch; `NfcHandler` extracts tag IDs to match against chores.
- **Permissions:** `NFC`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `INTERNET` (required for Supabase). Do not add new permissions without discussion, and document the reason for each one in the manifest.

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
