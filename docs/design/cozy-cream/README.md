# Handoff: ChoreDash — "Cozy Cream" Mobile Redesign

## Overview
A visual redesign of the ChoreDash household chores & tasks app (Android, Kotlin/Compose, Supabase-per-device sync). This package covers the **Cozy Cream** direction: a warm, light, low-pressure aesthetic built on a cream ground with sage / rose / amber / lavender accents, rounded cards with colored left "spines", circular icon chips, and serif page headers. It spans the four main tabs (Tasks, Chores, Memos, Settings) plus the deeper screens behind them (edit sheets, settings sub-pages, appearance/themes).

## About the Design Files
The files in this bundle are **design references created in HTML** — prototypes that show the intended look and behavior. They are **not production code to copy directly.** `ChoreDash.dc.html` is a "Design Component" (a custom HTML format) and won't run outside its authoring environment.

The task is to **recreate these designs in ChoreDash's existing codebase** (`mapgie/choreDash-Android`, Jetpack Compose) using its established patterns — Compose theming, `Scaffold`/`NavHost`, existing `TaskCard`/`ChoreCard` composables, Material 3 components — not to ship the HTML. Treat the HTML as the source of truth for layout, color, type, spacing, and copy; map every value onto Compose tokens (`MaterialTheme.colorScheme`, `Shape`, `Typography`).

## Fidelity
**High-fidelity (hifi).** Colors, typography, spacing, radii, and states are final. Recreate pixel-faithfully within Compose. The one exception: the HTML uses inline SVG glyphs as stand-ins for Material icons — use the equivalent Material/Compose icons rather than reproducing the SVG paths.

## Design Tokens

### Colors
| Token | Hex | Use |
| --- | --- | --- |
| Ground | `#f2ede2` | App background (warm cream) |
| Ground alt (zen) | `#ecebe2` | Zen list background |
| Card | `#fffdf9` | Elevated cards, fields |
| Card alt / sheet | `#fbf7ee` | Bottom-sheet surface |
| Nav bar bg | `#f7f2e7` | Bottom tab bar |
| Ink (text) | `#33302a` | Primary text |
| Ink muted | `#6d6455` | Secondary text |
| Ink faint | `#a29885` / `#b3a996` | Labels, captions |
| Hairline | `#efe8da` | In-card dividers |
| Border | `#ddd3c1` / `#c8bdaf` | Field & outlined-button borders |
| Sage (primary) | `#8aa877` | FAB, positive accent, active chore |
| Sage deep | `#5f7d52` | Sage text/icons |
| Sage tint | `#e7ecdd` / `#dfe8d3` | Sage chip/pill background |
| Rose (overdue) | `#b8524e` | Overdue spine, late badges |
| Rose tint | `#f6e3e1` / `#fbeeed` | Rose card/badge background |
| Amber (soon) | `#d9a648` / `#b07f24` | Due-soon spine & text |
| Amber tint | `#f3e8d2` / `#faf4e6` | Amber card/badge background |
| Lavender | `#7a5fa0` | Primary buttons, task accent, toggles-on |
| Lavender deep | `#5f4a7a` | Lavender text on tint |
| Lavender tint | `#e9e0f2` / `#efeaf5` | Lavender chip/selected background |
| Dark (reminder/zen-dark) | `#272e24` ground, `#dfcf90` accent, `#f0ead9` text | Dark reminder screen |

### Typography
- **Headers:** `Lora`, serif — weights 500/600/700. Page titles are **lowercase** with a colored full-stop, e.g. `settings.` (dot uses an accent color). Sizes 24–30px.
- **Body / UI:** `Nunito`, sans — weights 600 (regular text), 700 (labels), 800 (emphasis/buttons), 900 (primary CTAs).
- **Section labels:** Nunito 800, 11.5px, uppercase, letter-spacing `0.14em`, color `#a29885`.
- **Compose mapping:** Lora → a serif `FontFamily` for `headlineMedium`/`titleLarge`; Nunito → the default body/label families.

### Spacing & Shape
- Screen padding: 18–22px horizontal.
- Card gap in lists: 8–9px.
- Radii: cards `20px`; fields `16px`; small chips/badges `7–8px`; pills/buttons `999px`; bottom sheet top corners `28px`; palette tiles `16px`.
- Card shadow: `0 5px 16px rgba(128,110,85,0.13)`.
- FAB shadow: `0 8px 18px rgba(95,125,82,0.38)`.
- Card **spine**: absolute left bar, `width:7px`, full height, colored by status (rose/amber/sage/lavender).

## Global Components

### Bottom navigation (all main screens)
Five-slot row on `#f7f2e7` with a 1px top border `#e5ddcd`. Order: **Tasks · Chores · [+ FAB] · Memos · Settings**. The center slot is a raised 52px circular FAB, sage `#8aa877`, white `+`, lifted above the bar. Active tab: label + icon in the section's accent, icon sits in a pill-tint chip (`#e9e0f2` for Tasks/lavender, `#dfe8d3` for Chores/sage, etc.). Inactive: `#a29885`, no chip. Icons: Tasks = circle-check, Chores = **scrub brush** (rounded rect head + 4 bristle strokes), Memos = bell, Settings = gear.

### Top accent bar
Most screens open with a 5px full-width gradient strip: `linear-gradient(90deg,#8aa877,#e8d9b0,#d9a648)`.

### List card (Tasks/Chores row)
`#fffdf9`, radius 20, spine on the left, padding `13px 16px 13px 24px`, flex row: 46px circular icon chip (tinted by accent) → title (17px/800) + meta caption (12px/700, `#b3a996`, uppercase category · priority) → right column with a status badge and a 26px assignee avatar (`M` on `#e5ddcd`, `A` on `#e9e0f2`).

### Status badges
Small pill (radius 7–8, 11.5px/800): "2d late"/"3d over" rose on `#f6e3e1`; "1d left"/"20h left" amber on `#f3e8d2`; "due today"/"FRESH" sage on `#e7ecdd`; "LOW" sage. Chore cards also carry a 5px progress bar (`#efe8da` track, accent fill = cadence pressure).

### Toggle
52×30 pill; ON = `#7a5fa0` with knob right; OFF = `#ddd3c1` with knob left; 24px white knob, 3px inset.

### Segmented control
Full-width pill, `1.5px #c8bdaf` border, dividers between cells. Selected cell = tint fill (`#e9e0f2`) + deep-accent text (`#5f4a7a`) + leading `✓`. 3 cells typical (System/Light/Dark, Alarm/Notification/Silent, None/Date/Period).

### Outlined text field
`#fffdf9` fill, `1.5px #ddd3c1` border, radius 16, padding `11px 16px 13px`. The field label is a small caption **inside** the field at the top (11px/800, `#8f8571`, `margin-bottom:3px`), with the value below it — no border-notch / floating label. Selects add a trailing chevron; placeholder-only fields (e.g. Owner) show the label as muted body text as the value line.

## Screens / Views

> The HTML groups screens into two turns. **Turn 3a** = the four tabs + supporting views. **Turn 4a** = deeper screens. Below, each is described for implementation.

### 3a-1 · Tasks
Serif `tasks.` header (lavender dot). Right-side icon row: assignee-filter, zen, search, group/flat. Filter chips (`All` active lavender, `Active`) + a `due ↑` sort control. List grouped by time buckets (`OVERDUE` rose label, `TODAY`, `THIS WEEK`) of list cards. Footer strip "5 tasks · Done ›". Bottom nav, Tasks active.

### 3a-2 · Search + Sort
Search field pill with caret + `Cancel`. Result section label `IN CHORES · 2` with matching-substring highlight in titles/meta. A dimmed scrim + bottom "Sort by" sheet: options Due first (checked, sage), Room, Person, Pressure, Recently added.

### 3a-3 · Chores
Serif `chores.` header (sage dot). Filter chips `All` (sage), `Overdue · 2` (rose), `Soon` (amber) + `pressure ↓` sort. Grouped by room (`KITCHEN`, `BATHROOM`, `OUTDOOR`) with a count on the right of each header. Chore cards show cadence ("every 3d · done 5d ago"), pressure progress bar, left/over badge, avatar. An `NFC` hint card at the bottom. Footer "5 chores · 1 hidden". Bottom nav, Chores active.

### 3a-4 · Zen list
Calmer surface `#ecebe2`, serif `zen.` header. `mine`/`all` toggle + `LEAVE ✕`. Flat list of soft cards, each an open circle + title + gentle sub ("kitchen · when you're up"). Completed items: filled check, strikethrough, 0.62 opacity. **No pressure colors, no counts** — deliberately pressure-free.

### 3a-5 · Reminder view (dark)
Dark screen `#272e24`, gold accent `#dfcf90`. Centered bell in a concentric-glow ring, kicker "YOU ASKED FOR A NUDGE", serif task title, "due today · about 5 minutes", then `Done ✓` (gold, filled) + `Snooze 1h` (outline). Footer "next: … · 4:30pm".

### 3a-6 · Settings
Serif `settings.` + back chevron. Grouped cards under labels: APPEARANCE (theme chooser, list-density segmented, follow-system toggle), ZEN MODE (hide overdue counts, gentle reminders), PERSONALISATION (Display, Quick add button, Widget — chevron rows), REMINDERS, HOUSEHOLD (A & M avatars, NFC tags), ACCOUNT (Supabase connection), ABOUT. Bottom nav, Settings active.

### 4a-1 · Edit Task
Serif `edit task.` + back. Category kicker `BABA`. Calendar & Share chips. Fields: Title (`Buy a Bike`), Category select (`Baba`). Priority buttons (Higher / **Normal** selected lavender / Lower). Due buttons (**None** selected / Date / Period). Owner select (placeholder). Notes field. Reminder toggle (off). Centered red `Delete`. Full-width lavender `Save` pinned bottom.

### 4a-2 · Chore-detail sheet
Chores list dimmed behind a scrim; bottom sheet (`#fbf7ee`, 28px top radius) with grabber. Category kicker `CAR`, serif `Car service` + calendar/share icons. Fields: Label (`Car service`), Owner select. Centered sage `Fewer options` link. Interval field (`365`). Outlined `Write tag`. Row: `Cancel` (outline) / `Save` (lavender). Centered red `Archive`.

### 4a-3 · Appearance
Serif `appearance.` + back. BRIGHTNESS segmented (System / **Light** / Dark). WCAG-colours checkbox (unchecked) + description. COLOUR PALETTE 3-col grid of tiles: **Mist** selected (2.5px lavender border, `#efeaf5` fill, 3 dots with a check on the middle), Sage, Coral, Teal, Custom (bright RGB dots) — each tile = 3 swatch dots + name. SAVED THEMES list: `cray` (Active, lavender-tint row) with pencil / play / red-trash actions; `cray`; a faded `theme`. Bottom nav, Settings active.

### 4a-4 · Display
Serif `display.` + back. GROUPING card: "Group chores by category" (on) + "Group tasks by category" (on), each with sub-caption. VISIBILITY card: "Smart chore visibility" toggle (on) + description, then "How many days before a chore is due it reappears:" and five stepper rows — Daily `0 days` (minus disabled/greyed), Every few days `1 day`, Weekly `3 days`, Fortnightly `3 days`, Monthly and longer `5 days`. Steppers = 32px circular −/+ (minus rose, plus sage) around a centered value. Outlined lavender `Reset to defaults`. Bottom nav, Settings active.

### 4a-5 · Reminders & alerts
Serif `reminders & alerts.` + back. NOTIFICATION STYLE segmented (**Alarm** selected / Notification / Silent) + caption "Plays alarm sound, bypasses Do Not Disturb". Hairline. "If reminders stop arriving, check these permissions." Permission card rows (lavender icon + label + green-check "Allowed"): Notifications, Exact alarms, Do Not Disturb access (with sub-caption). Sage check line "Reminders are fully enabled." Bottom nav, Settings active.

### 4a-6 · About
Serif `about.` + back. Centered 74px sage rounded-square app icon (scrub-brush), serif `choreDash` wordmark, centered description ("choreDash helps your household share chores and tasks, synced through your own Supabase project."). Hairline. `What's New` (lavender-tint filled). `Open-source licenses` (outline). Centered `Version 0.23.2`. Bottom nav, Settings active.

## Interactions & Behavior
- **Tabs:** switch between Tasks / Chores / Memos / Settings; center FAB opens the quick-add menu (order configurable in Settings → Quick add button).
- **List card tap:** opens the corresponding detail sheet (Edit Task / Chore-detail) as a bottom sheet over a scrim.
- **Checkbox / circle tap:** marks a task done (Zen shows strikethrough + fade).
- **Chore complete / NFC tap:** logs a completion, resets the cadence and the pressure bar to full; overdue → fresh.
- **Filters & sort:** chips filter the list; the sort control opens the "Sort by" bottom sheet.
- **Steppers (Display):** −/+ adjust the reappear-lead per cadence bucket; minus disables at 0.
- **Toggles / segmented / checkbox:** standard immediate state change.
- **Snooze/Done (reminder):** Done completes; Snooze reschedules the nudge.
- **Transitions:** bottom sheets slide up over a `rgba(51,48,42,0.3–0.35)` scrim; keep motion gentle (200–300ms ease).

## State Management
- **Tab/nav state** (current destination); quick-add menu open + item order + reminder-feature label ("Reminders"/"Alarms"/"Memos").
- **Filters:** Tasks (All/Active/Done), Chores (All/Overdue/Soon), assignee scope (mine / mine+unassigned / all), sort key, group vs flat, search query.
- **Per item:** title, category/room, priority, due (none/date/period), owner, notes, reminder on/off; chores also cadence interval, last-done timestamp, derived pressure/overdue.
- **Settings:** brightness (System/Light/Dark), WCAG colours, palette selection + saved themes (name, 3 colors, active), list density, follow-system, zen toggles (hide counts, gentle reminders), grouping toggles, smart-visibility on + per-bucket lead days, notification style + permission states.
- **Account:** Supabase project URL, anon key, owner handle.
- **Data:** synced through the user's own Supabase project (per device); fetch tasks/chores/memos and completion logs.

## Assets
- **Fonts:** Lora + Nunito (Google Fonts) — or bundle equivalents in the app.
- **Icons:** all icons are inline SVG placeholders in the HTML; replace with Material/Compose icons (check-circle, brush, bell, gear/settings, search, users, calendar, share, chevrons, alarm, shield/DND, pencil, play, trash, plus/minus).
- No raster images or logos — the app icon is the sage scrub-brush glyph.

## Files
- `ChoreDash.dc.html` — the full design (turns 3a + 4a). Open in the authoring tool to inspect; read the markup for exact inline values.
- `android-frame.jsx` / `ios-frame.jsx` — device-bezel wrappers used only for presentation (not part of the app UI).
- `github.md` — links this design to the source repo and maps each screen to the Kotlin files it derives from.
