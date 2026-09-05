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
## [0.32.0] - 2026-09-05

### Added
- Settings > Reminders & alerts has a Sound check: it shows what Android holds for the alert channel (sound, vibration, importance, Do Not Disturb bypass) with a tap through to the system page, and a button that rings a test alarm in ten seconds through the real alarm path

### Changed
- A memo that has rung is done: it moves to the Done list with "rang 2h ago" instead of waiting to be ticked, and memo cards are no longer tickable or swipe-to-done. Repeating memos always show their next ring
- Leaving zen mode is the same target icon that entered it, in place of the LEAVE label
- A task ticked in the zen list moves to Done straight away rather than lingering struck through
- The WCAG accessible colours toggle is available on custom palettes too, and lifts their text the same way

### Fixed
- The Settings tab shows a gear again instead of a sun, and the Chores tab's house is drawn at the same size as the other tab icons

---
## [0.31.0] - 2026-09-05

### Added
- Memos can repeat on chosen days of the week: the edit sheet has a Repeat toggle, S M T W T F S day cells and Weekdays / Weekends / Every day shortcuts, and a Next ring banner shows when it will go off
- Memos can now carry a weekly repeat schedule: a repeating memo re-arms itself for the next chosen day after it rings, and Done acknowledges the ring that is waiting instead of retiring the memo
- Each memo can have its own alarm sound: a Sound row in the memo sheet opens the device's alarm-tone picker, and the Alarm style rings with the chosen tone (the Notification style keeps its channel sound)

### Changed
- Settings > Colours now sets the spine and due badge and the round icon independently: each can follow severity or the category colour, so the default shows urgency on the spine and what the chore is on the icon
- The Colours preview shows three real cards (overdue, due soon, fresh) with the current choice named, and a swatch row to try a category colour against the severity tints
- An existing "colour chores by" choice carries over to both elements, so nothing changes until you touch the new setting
- The memo sheet now uses the same layout as the chore and task sheets: the title is the input, the time is a large serif value, and Archive and Delete are centred links under Cancel and Save
- Unsaved memo edits survive rotation and process death and are offered back when the sheet is reopened, like chore and task edits
- The Memos list now uses the same layout as Chores and Tasks: a search action in the header, Active / Done / All filter chips (Active by default, with a count) and a sort pill (next ring, name, or added)
- Memo cards show the schedule as their caption ("Weekdays · 8:00 PM", "Once · doesn't repeat") and the next ring as their badge ("rings tomorrow 9 AM"), coloured rose, amber or sage by how soon it is
- A memo that has rung stays in the Active list with a "rang 2h ago" badge until you mark it done, instead of dropping straight into Done
- The collapsible Archived section is gone; archived memos appear, muted, under the All chip, and archiving still lives in the edit sheet

### Fixed
- The name chosen for the reminders feature (Reminders, Alarms or Memos) is now used everywhere it is named: the add and edit sheet, delete confirmations, the notification title, the ringing screen, the Next Up widget and the Settings captions
- Snoozing a memo, then answering it, no longer risks the alarm being re-armed inconsistently: every save, Done, ring and archive now re-syncs the alarm the same way

---
## [0.30.0] - 2026-09-04

### Added
- Editing a category now shows a live preview card, so you can see the icon and colour applied the way they read in the list.
- Marking a task done from the list now shows a brief Undo, so an accidental tap or swipe is one tap to reverse.

### Changed
- The center + button now adds an item for the page you're on with a short press. Long-press it to open the full New chore / task / memo menu.
- The Memos tab is now always shown in the bottom bar, so the five slots never reshape.
- The Done section on Tasks now lists the most recently completed task first, so you can see what you just ticked off.
- Swipe-to-complete on a task now needs a more deliberate swipe across the card, to avoid accidental completions.

### Fixed
- Swiping down on a New chore, New task or reminder sheet with unsaved changes now asks before discarding, instead of closing silently and losing what you typed.

---
## [0.29.0] - 2026-09-04

### Added
- The Alarm notification style now rings like a clock alarm: a reminder turns the screen on over the lock screen, plays the alarm tone and vibrates until you tap Done or Snooze (or two minutes pass), and gets through Do Not Disturb
- Settings > Reminders & alerts shows a Full-screen alarms permission row on Android 14 and later, where the system lets you switch that off per app

### Changed
- WCAG accessible colours now lifts every colour the app draws text with to 7:1 (AAA): page ink and captions, the accent colours, section counts, inactive tabs, the tag label, badge and status text, and the Tasks, Chores and Memos tints, in every built-in palette and both brightnesses. Previously most of the screen was left at the palette's designed contrast, so on Zen Dark the toggle changed almost nothing
- Settings lists Help above About choreDash
- The summary strip under the Tasks and Chores lists ("6 chores, 6 hidden", Done, Archived) is gone; the Done and Archived sections already open from inside the lists

### Fixed
- Chores can be created without an NFC tag ID; Save no longer requires typing one

---
## [0.28.0] - 2026-09-02

### Added
- Tapping a reminder notification now opens a full-screen reminder view showing what the nudge was for, how far off its time is, Done and Snooze 1h buttons, and the next nudge that is scheduled
- A first-run welcome sheet explains the difference between chores, tasks and reminders; the same text lives under Settings › Help
- Unsaved edits in the chore and task sheets survive rotation, and are offered back (never applied silently) when the same item is reopened

### Changed
- Settings and its Appearance, Display, Reminders & alerts, About, Quick add, Widget and Connection pages now use the Cozy Cream grouped cards, pill segmented controls, tinted toggles, circular steppers and pill buttons from the design handoff
- Zen mode is now its own calm list: open circles to tick things off, soft cues like "kitchen · when you're up" instead of colours and counts, a mine/all switch and a Leave control in the header
- The search row is the design's card-coloured pill with a sage caret and a text Cancel action
- With WCAG colours on, faint captions lift to the muted ink so they clear 4.5:1 with margin

### Fixed
- Rotating the phone no longer closes an open Edit sheet or loses what was typed in it
- The tab screens no longer pad for the status and navigation bars twice, which left a dead band under the accent strip and another above the bottom bar

---
## [0.27.0] - 2026-09-02

### Added
- Zen Dark theme: dark mode now uses the handoff's warm charcoal-green palette instead of Material's default dark scheme, and ships as the default brightness (System and Light remain available under Appearance)
- Sort control on the Chores and Tasks lists: an outlined pill that reads the key and direction in words (pressure, due, name, category; priority, due, added, name) and opens a small picker sheet. The choice is remembered per list
- Summary bar above the bottom navigation on Chores and Tasks, showing the count and how many items are hidden, with a link to the archived chores or done tasks
- NFC scan button beside the Chores title that explains how to log a chore by tapping its tag
- Settings > Colours: colour chore cards by severity or by category, and choose which palette swatch each severity wears
- Settings > Categories: reorder categories, rename them, give each an icon and a colour, or delete one (its chores and tasks move to General)
- Unified Log and Mark done sheets for chores and tasks: a Done control (Just now, Earlier today, Pick), a utility row (Calendar, Pin, Remind, Tag, Edit), and chore history with an inline Undo on the latest log
- Unified Edit sheets for chores and tasks: the title is the field, settings sit in one grouped card with compact controls, and the new chore sheet uses the same layout
- The + button opens a speed dial over the whole screen with New chore, New task and New memo; the active tab's item sits closest to your thumb

### Changed
- List cards are tighter: 16dp corners, a 5dp spine, a 38dp icon chip with one Lucide icon per category, the meta line directly under the title, and the owner avatar left of the due badge. The cadence progress bar is gone
- Icons throughout the app switch to the Lucide set; the Chores tab uses a house-with-check glyph
- The header action row is the same on Chores and Tasks: search, owner, zen, group/flat
- Chore due badges always show the countdown (for example 35d over, 1d left); the separate due-countdown header toggle is folded into the sort pill
- Chore category can now be changed from the edit sheet

### Fixed
- Bottom bar tabs, card captions and section counts use dedicated ink tones so they read at 4.5:1 in both themes

---
## [0.26.0] - 2026-09-02

### Added
- Swipe a chore to the left to snooze it. It moves to the hidden section for its smart visibility lead time, or one day when smart visibility is off, and swiping again wakes it. Logging a chore ends its snooze. Snoozes are per device.

### Changed
- The Tasks screen no longer shows All, Active and Done filter chips. Done tasks always live in the collapsible Done section below the list.
- Chores now use the Cozy Cream scrub-brush icon in the bottom bar, list cards and add menu
- The Chores and Tasks header icons (owner filter, zen, search) now match the Cozy Cream design, with a new group/flat list toggle
- The Chores list now ends with a card reminding you that a chore can be logged by tapping its NFC tag
- The owner filter on the Chores and Tasks screens now cycles through three states: just mine (filled person), mine and unassigned (outlined person), and everyone (two people). Previously it toggled between mine-plus-unassigned and everyone.
- The owner filter button on Chores is hidden until an owner handle is set, matching Tasks.

### Fixed
- Bottom bar tab labels and page header accents are now readable in dark mode

---
## [0.25.0] - 2026-08-28

### Added
- Search on the Tasks and Chores lists: tap the search icon in the header for a pill search field with live results and highlighted matches

### Changed
- New bottom utility bar in the Cozy Cream style: flat with a hairline top edge, the round sage add button docked in its centre, and the active tab's icon in a tinted pill
- Icons across the navigation, headers and cards switch to the design's outline style
- List screen actions (search, zen, owner filter, countdown) move up into the page header; task sorting is now a small "due" text control beside the filter chips
- The Cozy Cream design handoff is checked in under docs/design/cozy-cream for future reference

---
## [0.24.1] - 2026-08-27

### Fixed
- Restoring a task marked done in error works again (it used to fail with a "list is empty" error)
- Clearing a task's owner, notes, category, due date or reminder in the edit sheet now actually clears it

---
## [0.24.0] - 2026-08-26

### Added
- Chore cards show a slim cadence-pressure bar that fills as the chore approaches due
- New default "Cream" colour palette: warm cream ground with lavender, sage and amber accents, plus a matching warm dark mode

### Changed
- List cards redesigned: a circular icon chip on the left (also the done toggle for tasks and reminders), an uppercase category and cadence caption under the title, and the status badge above the owner avatar on the right
- Task card spines now show urgency (matching chores); priority moved into the caption text
- The add button is now a round sage button docked at the centre of the bottom bar
- Overdue reminders use the shared rose status style instead of error colours
- Main tabs now open with a serif lowercase page title and a slim gradient accent strip
- Softer rose, amber and sage status colours; due dates and countdowns now show as small tinted badges
- Rounder cards, sheets and filter chips, a wider card status spine, and bolder card titles
- Section and group labels restyled as small spaced capitals; settings screens use flat serif headers instead of coloured app bars

---
## [0.23.2] - 2026-08-22

### Changed
- Owner initials on the Chores and Tasks lists now use one consistent size, sit in the same rightmost position, and give each person a stable colour so the same owner looks the same on both screens.

### Fixed
- Alarm-style reminders now bypass Do Not Disturb even when you grant Do Not Disturb access after first launch. Previously the alarm notification channel was created once without bypass and could never gain it, so alarms stayed silenced under Do Not Disturb.

---
## [0.23.1] - 2026-07-14

### Changed
- Custom themes now colour the Tasks, Chores, and Reminders accents (bottom nav indicator and add menu) from your primary, secondary, and tertiary picks, and give list screens a consistent top bar, so your chosen colours show throughout the app instead of clashing with fixed pastels.

### Fixed
- Task and reminder alarms can no longer cancel or overwrite each other in the rare case of colliding internal ids

---
## [0.23.0] - 2026-07-12

### Added
- Multiple Pinned Item widgets placed on the same home screen can now each pin a different task or chore, with a chooser shown when pinning while 2+ are placed

### Changed
- The collapsed section at the bottom of the chores list now reveals everything hidden by smart visibility, not just chores due 60+ days out, and its label reflects why chores are hidden
- The Add Task and Add Chore widgets now show the same icon and accent colour used for Tasks and Chores elsewhere in the app, instead of a generic plus sign

### Fixed
- Overdue chore notifications now respect the Alarm/Notification/Silent delivery mode and Do Not Disturb bypass setting, matching task and reminder alerts instead of using a single fixed channel
- Chores hidden by smart visibility could not be viewed or logged from the chores screen

---
## [0.22.0] - 2026-07-11

### Added
- Settings > Quick add button lets you reorder the + menu's Task/Chore/Reminder items by drag-and-drop or with up/down buttons
- Settings > Quick add button lets you rename the reminders feature to Reminders, Alarms, or Memos throughout the app
- Zen mode for Tasks, matching the calmer, decluttered view already available for Chores

### Changed
- The Reminders tab only appears in the bottom bar when there are outstanding (non-archived) reminders to manage
- The quick add menu's button text is now just Task, Chore, or your chosen reminder name
- Removed the version footer from the Chore page; the version is now shown in Settings > About
- Moved Supabase connection to the penultimate position in Settings, since it is typically a one-time setup

### Fixed
- Custom themes with vivid backgrounds no longer render muddy, low-contrast card surfaces; neutral surface roles are now derived cleanly and the full Material 3 surface-container palette is set.

---
## [0.21.0] - 2026-07-11

### Added
- WCAG accessible colours toggle: high-contrast variants of every built-in palette
- Custom theme: pick exact colours with a full colour picker (saturation/brightness square, hue bar, and hex entry) instead of raw HSL sliders
- Custom theme: light and dark background colours can now be set directly, with an Auto option that derives them from the primary colour
- Smart chore visibility: chores now hide until they are close to due, based on how often each one repeats, with per-cadence lead times (daily, every few days, weekly, fortnightly, monthly) configurable under Settings > Display > Visibility

### Changed
- Custom theme colours are now applied exactly as picked in both light and dark mode; text colours adapt automatically to stay readable
- Saved themes now record the brightness mode and background overrides, and restore them on load
- Tapping a saved theme row now loads it
- The single "Hide chores not due soon" day threshold has been replaced by smart chore visibility; an existing threshold is carried over as the starting lead time for every cadence

### Fixed
- Dark mode ignored the picked lightness and washed every custom theme out to the same pastel look
- Deleting the active saved theme no longer resets the whole app back to the Mist palette

---
## [0.20.0] - 2026-07-08

### Added
- New default "Mist" colour palette: a light periwinkle primary over blue-violet-grey neutrals, replacing green as the default look (the green "Sage" palette is still available in Settings)
- Chores, Tasks, and Reminders now each get their own colour tone on the bottom nav and the add-menu button

### Changed
- Tasks is now the first tab instead of Chores
- The add-menu button now lists Task, Chore, then Reminder
- The Next Up and Pinned Item widgets now distinguish 'Supabase isn't connected yet' from 'can't refresh right now' and, when they can't refresh, say how old the last-shown data is instead of a generic unavailable message

### Fixed
- Editing a chore, task, or reminder now warns before discarding unsaved changes when you press back, tap outside the sheet, or swipe it down, instead of silently losing your edits

---
## [0.19.0] - 2026-07-07

### Changed
- The Next Up and Pinned Item widgets now support true 1x1 launcher cells, dropping the inline checkbox/button in favour of a single tap-to-open line when there isn't room for them

### Fixed
- The background widget refresh job no longer retries pointlessly when the device has no network connection

---
## [0.18.0] - 2026-07-06

### Added
- 1x1 home screen widgets to quick-add a task or a chore, visually distinguishable by shape and colour
- New tasks and chores now start with a default category so you aren't forced to pick one before saving

### Changed
- The reminder delivery mode setting now applies immediately to reminders that were scheduled before the setting was changed

### Fixed
- Snoozing a reminder now keeps its alarm or silent delivery mode instead of coming back as a standard notification
- Snoozing a task-linked reminder no longer loses the link to its task
- Reminders that came due while the phone was off are now delivered at the next boot instead of being dropped
- Reminders are no longer silently dropped when the exact-alarm permission is revoked: they fall back to an approximate alarm
- The Next Up widget now actually respects the Widget customisation settings (Show / Priority / Whose), which previously had no effect on any widget

---
## [0.17.0] - 2026-06-27

### Added
- Display settings sub-screen with toggles for grouping chores and tasks by category, and per-list visibility filters to hide items not due within a configurable number of days

### Changed
- Group-by-category toggle moved from the chores and tasks toolbars into Settings > Display; the setting is now persisted across sessions
- Zen mode lotus icon enlarged for easier tapping

---
## [0.16.0] - 2026-06-27

### Added
- Widget customisation screen in Settings: choose whether the widget shows Chores, Tasks, or Reminders; filter by priority (All, Red, Amber); and show items for everyone or just yourself

---
## [0.15.7] - 2026-06-26

### Changed
- Tapping the "coming up next" widget when all tasks are done now opens the add-task sheet instead of the task list

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
