# Changelog

All notable changes to the Android app are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased]

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
