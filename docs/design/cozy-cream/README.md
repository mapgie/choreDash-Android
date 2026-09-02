# Handoff: ChoreDash — "Cozy Cream" + "Zen Dark" Mobile Redesign

> **Rule zero: never drop functionality silently.** This is a visual redesign, not a feature cut. Every existing behaviour (sort, summary bar, NFC read/write, smart visibility, unsaved-changes guard, widgets, reminders, undo, sharing, calendar export, owner assignment…) must survive unless this README says explicitly to remove it. If a mockup doesn't show a feature, that means it is out of frame — not gone. If something genuinely can't be kept, stop and list it in a "Dropped / needs decision" note in the PR description rather than omitting it.
>
## Implementation rule: nothing is dropped silently
Every control, screen, link and behaviour in the design (turns 5a–7a) and every feature already in the shipped app is **required**. If something can't be built as drawn, keep the existing behaviour and flag it in the PR — never remove it. Specifically retained: Chores sort pill, summary bar ("7 chores · 1 hidden · Done ›"), NFC scan + tag label, group/flat toggle, smart-visibility hidden count, pin-to-widget (chore detail sheet), calendar/share, dirty-state Back guard on every sheet, Undo latest log, category management, colour-by severity/category.

## Turn index (newest first) in `ChoreDash.dc.html`
- **8a** — Final direction: the locked decisions across every turn, in one place (read this first)
- **7a** — Edit sheets (chore + task), "+" speed dial open (New chore · New task · New memo) — built in both Zen Dark and Cozy Cream light
- **6a** — Log / Mark-done sheets, unified
- **5a** — Zen Dark theme: Chores, Tasks, revised Cozy Cream Chores, Settings › Colours, Settings › Categories
- 4a and earlier — legacy; superseded where they conflict with the above

> **Revision 2 (turn 5a) — read this first.** Turn 5a in `ChoreDash.dc.html` supersedes the list-card and bottom-nav specs below, adds the **Zen Dark** theme, and changes the icon strategy. Where 5a and older sections disagree, **5a wins**. The four corrections that motivated it:
> 1. **Dark theme was never specified** — the shipped dark mode used Material's default dark scheme with light tokens pasted in. Use the Zen Dark token column below; never derive dark colors automatically.
> 2. **Icons: NOT Material.** Use **Lucide** (stroke icons, 2px, round caps) — ship them as Android VectorDrawables (or the `com.composables:icons-lucide` Compose package). One icon per *category*, never the same brush glyph on every row. Map: House → `droplet` (water) / `home`; Car → `car`; Laundry → `washing-machine`; Plants → `sprout`; Kitchen → `utensils`; Bathroom → `bath`; Outdoor → `tree-pine`; Admin → `zap`; Errand → `pill`; General → `printer`; Holiday → `plane`. Tab bar: `circle-check` (Tasks), `house-check` (Chores — house outline with a check inside; not brush/broom/spray, which were rejected), `bell` (Memos), `settings` (Settings). Header action row is identical on Chores and Tasks — `search`, `user`, `target` (zen), `layout-grid` — same order, same positions, so the thumb never relearns. The `nfc` scan button sits on the LEFT, beside the "chores." title, and only on Chores. Delete the emoji/Unicode glyphs (✳ ◆ ❋) that appear in older 3a markup — they were placeholders.
> 3. **NFC scan must be reachable from the Chores header** — a tinted round icon button (`nfc` glyph) on the left of the header, immediately after the screen title, plus the existing "Write tag" in the chore-detail sheet. The in-list NFC hint card is dropped.
> 4. **List cards are tighter and have no progress bar.** See "List card (revised)". Right-hand cluster order is **avatar → due badge** (owner left of date, so dates line up for scanning).
> 5. **Chores sort control is required.** The filter row ends with a right-aligned outlined pill `pressure ↓` (Outline token border, radius 999). No arrows. The pill reads the key AND the direction in words — "pressure · worst first", "due · soonest first", "name · A–Z". Tap opens a small sheet: pick key, then direction ("worst first / freshest first", "soonest / latest", "A–Z / Z–A"). Same component on Tasks.
> 6. **Summary bar is required** on Chores and Tasks: a hairline-topped strip between list and nav — left "7 chores · 1 hidden" (Ink faint, 13px/700), right "Done ›" link opening the completed list. Hidden count comes from smart visibility.
> 7. **Settings › Colours** (new screen): segmented "Colour chores by: Severity | Category"; three severity rows (Overdue / Due soon / Fresh) each with a 6-swatch palette `#d9615c #c99a4a #dcb85f #8aa877 #7f9bb3 #b6a3d6` (selected = 2px ring in Ink); live preview card. Severity colour drives spine + icon chip + badge; in Category mode those three take the category colour and the badge text stays neutral (Ink muted).
> 8. **Settings › Categories** (new screen): drag-to-reorder list, "+" in header adds. Tapping a row expands it inline: rename field, icon picker (Lucide set: `washing-machine` `brush` `home` `droplet` `sprout` `utensils` `bath` `tree-pine` `leaf` `lamp`), 7-swatch colour picker (palette above + `#e0b28d`), Delete (rose, states where chores move) and Done. "General" is the default category and cannot be deleted or reordered.
> 5. **Chores sort control is required.** The `pressure ↓` outlined pill at the right end of the filter-chip row (same slot as `due ↑` on Tasks) is a sort menu: pressure · due date · name · category. Tap cycles direction; long-press opens the list. Not optional.
> 6. **Summary bar is required** on both Chores and Tasks, directly above the bottom nav: left `7 chores · 1 hidden` (count + smart-visibility hidden count; omit "· N hidden" when 0), right `Done ›` navigating to the completed list. 13px/700, Ink faint, 8×20 padding, hairline top border. Tasks reads `6 tasks`.

## "+" FAB open state (turn 7a)
Speed dial, not a sheet. Tap: scrim rgba(15,18,13,.62) over the screen (nav included); FAB fill flips to Ink `#ece6d6` and the + rotates 45° into ×. Three pills rise above it (bottom → top: New chore, New task, New memo — amber icon chip for memo; 10px apart, 14px above the FAB), each: 40px tinted icon chip (sage for chore, lavender for task) · 15/800 label. No explanatory hint text — the chore/task distinction is taught once in the first-run splash and repeated under Settings › Help. Pill surface = Card token, radius 999, shadow 0 8 22 rgba(0,0,0,.45). Order is fixed regardless of the active tab; the active tab's item is first (closest to thumb) — on Tasks, swap so New task is lowest. Scrim tap, Back, or × closes. Picking an item opens the matching Edit sheet in "new" mode (eyebrow NEW CHORE / NEW TASK, empty title focused, keyboard up).

## Sheet dismissal & unsaved changes (applies to ALL bottom sheets: Log, Edit, Category editor)
Keep the existing dirty-state guard — do not drop it while rebuilding the sheets. Rules:
- A sheet is **dirty** once any field differs from its opened value (title text, any picker, owner, notes, "Done" segment other than the default).
- If dirty, **system Back, swipe-down, and scrim tap all intercept** and show a confirm sheet (same chrome, Zen Dark tokens): title "Discard changes?", body "Your edits to *Meds* won't be saved.", buttons: outlined **Keep editing** (default/focus) + rose-text **Discard**. Never a silent dismiss.
- If clean, Back/swipe/scrim dismiss immediately.
- Cancel button = same as Back (guarded when dirty). Save/Log it/Mark done always dismiss.
- Draft text survives rotation and process death within the session (rememberSaveable / SavedStateHandle); on reopen of the same item the draft is offered, not auto-applied.
- Do not use predictive-back animation to dismiss a dirty sheet — the intercept must fire before the gesture commits.

## Edit sheet (turn 7a — one component for chores AND tasks)
Same sheet chrome as the Log sheet. **No stacked outlined text fields.** Anatomy:
1. Header: 44px category chip · eyebrow "EDIT CHORE" / "EDIT TASK" · the title is the editable field itself (Lora 30/600, sage 1.5px underline, no box, no floating label).
2. One grouped settings card (`#1b2019`, radius 16, 1px `#2f372b` dividers). Each row: 17px Lucide icon · label (14.5/700) · control on the right. Controls are compact: **value chip** (pill, chevron, opens picker) for Category / Due / Remind; **avatar row** for Owner (ring = selected, "Any" last); **stepper pill** for Repeat every (chores, "28 d"); **3-segment pill** for Priority (tasks); NFC row (chores only) shows the current label as text ("meds" / "no label" in Ink faint) plus a "Rewrite" / "Write tag" chip. Row order — chores: Category, Owner, Repeat every, Remind, NFC tag. Tasks: Category, Owner, Priority, Due, Remind.
3. Notes: a single soft block (`#1b2019`, radius 14) with pencil icon; placeholder in Ink faint. Grows with content.
4. Footer identical to the Log sheet: outlined Cancel + sage Save.
5. Tertiary link row, centred, 13px/800 Ink faint: Add to calendar · Share · **Archive** (chores) / **Delete** (tasks) in rose. No full-width destructive buttons; no "Fewer/More options" disclosure — everything fits.

## Log / Done sheet (turn 6a — one component for chores AND tasks)
Sheet surface `#2a3127` (Cozy Cream: `#fffdf9`), radius 26 top, padding 20, section gap 18, scrim rgba(15,18,13,.55). Anatomy, top to bottom, identical on both:
1. Handle 40×4 `#4d574a`.
2. Header: 44px category icon chip (status-tinted) · eyebrow "CATEGORY · cadence/priority" · Lora 30/600 title · status badge + meta line ("last done 5 Aug · 27d ago" / "added 12 Aug · no reminder") · owner avatar 30px on the right.
3. **DONE** segmented control replaces the "Log at a different time" toggle: Just now | Earlier today | Pick… (opens date-time picker). Selected segment = sage tint.
4. Primary row: outlined Cancel (flex 1) + filled sage action (flex 1.6): "Log it" for chores, "Mark done" for tasks. Sage fill, Ink-on-sage text `#1c2118`, check icon. **No rose primary buttons.**
5. Utility row, 40px circular icon buttons with 11px labels. Chores: Calendar · Pin · Remind · Tag · Edit. Tasks: Calendar · Pin · Remind · Edit (no NFC slot — tasks have no tags; 4 slots spread evenly).
   Chores also show the **tag label** under the meta line: nfc icon + label in `#dfcf90` 12.5/800; chores without a tag show "no label" in Ink faint. This replaces the scattered "Remove latest log / Add reminder… / More options… / Edit task…" text links.
6. Context block: chores → **HISTORY** list (`#1b2019`, rows: dot · date · "Nd ago · owner"; latest row has an inline rose **Undo** chip, which replaces "Remove latest log"; header link "All N ›"). Tasks → **NOTES** block (or omitted if empty).
Never use the old rose `#ffb4a8` pill palette in dark; every colour comes from the Zen Dark column.

## Zen Dark tokens (turn 5a)
| Token | Cozy Cream | Zen Dark |
| --- | --- | --- |
| Ground | `#f2ede2` | `#22281f` |
| Card | `#fffdf9` | `#2f372b` (no shadow) |
| Nav bar bg | `#f7f2e7` | `#1b2019` |
| Hairline / nav border | `#e5ddcd` | `#343d30` |
| Outline (sort pill) | `#ddd3c1` | `#45503f` |
| Ink | `#33302a` | `#ece6d6` |
| Ink muted (captions, header icons) | `#a29885` | `#a7ad98` |
| Ink faint (card meta) | `#a29885` | `#8f978a` |
| Section count | `#c9c0ae` | `#5d6656` |
| Nav inactive icon+label | `#8f8571` | `#b4bba9` |
| Sage accent / FAB | `#8aa877` | `#8aa877` (FAB glyph `#1c2118`) |
| Sage text | `#5f7d52` | `#9fbd8a` |
| Sage tint (chips, nav active) | `#dfe8d3` / `#e7ecdd` | `#3a4634` (nav active text `#c6e0b3`) |
| Rose spine | `#b8524e` | `#d9615c` |
| Rose text | `#b8524e` | `#e8938d` |
| Rose tint | `#f6e3e1` | `#4a2f2e` |
| Amber spine | `#d9a648` | `#dcb85f` |
| Amber text | `#b07f24` | `#dcb85f` |
| Amber tint | `#f3e8d2` | `#3d3624` |
| Lavender accent | `#7a5fa0` | `#b6a3d6` (chip text `#221a2e`) |
| Lavender text | `#5f4a7a` | `#c9b8e6` (nav active `#d4c6ee`) |
| Lavender tint | `#e9e0f2` | `#3b3448` |
| Avatar M | `#e5ddcd` / `#6d6455` | `#3f4a3a` / `#cfe0c2` |
| Avatar A | `#e9e0f2` / `#7a5fa0` | `#4d4a3a` / `#e8dfb8` |
| Header NFC button | `#dfe8d3` / `#5f7d52` | `#3a4634` / `#dfcf90` |
| Top accent strip | `#8aa877 → #e8d9b0 → #d9a648` | `#8aa877 → #dfcf90 → #c99a4a` |

Contrast floor: every text/icon on its surface ≥ 4.5:1 (nav labels included). If a WCAG toggle is on, lift Ink faint to Ink muted.

## List card (revised, turn 5a — replaces the older spec)
Radius **16**, padding **10px 14px 10px 20px**, row gap in list **7px**, spine **5px**. Icon chip **38px** circle, Lucide icon 18px, tinted by status. Title 16px/800, line-height 1.2; meta 12px/700 directly below (2px). Right side is a **single horizontal row** (gap 8): status badge (11.5px/800, radius 7, padding 3×9) then optional 24px avatar. **No progress bar, no second line of badges.** Light cards keep a soft shadow `0 3px 10px rgba(128,110,85,0.10)`; dark cards have none.

## Bottom navigation (revised)
Same five-slot structure. Inactive icon+label must use the "Nav inactive" token (not Ink faint) — this fixes the unreadable dark bar. Active slot: pill tint + accent text per the table. Nav bg is one step *darker* than ground in Zen Dark (`#1b2019`), one step lighter in Cozy Cream.


## 8a — Final direction (read first)
Turn 8a resolves every open decision:
- **Theme:** Zen Dark ships as the default; Cozy Cream is the light alternate. Same tokens, palette swap only.
- **Cards:** 5a's compact revision (no progress bar, colored spine + one Lucide icon/category, cadence line). 3a's original cards are historical, not to be built.
- **Nav:** Tasks · Chores · [+] · Memos · Settings, as below.
- **Edit/log sheets:** one grammar for chores and tasks (7a/6a), now in both themes.
- **Open question for engineering:** Zen Dark's Save button uses sage; Cozy Cream's uses lavender/purple (`#7a5fa0`, matching 4a). Pick one primary-action color before building both themes — flagged, not decided, in the design.

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
- **Icons:** Lucide (see Revision 2). The inline SVGs in the HTML are Lucide-style and can be exported 1:1 as VectorDrawables; do **not** substitute Material icons.
- No raster images or logos — the app icon is the sage scrub-brush glyph.

## Files
- `ChoreDash.dc.html` — the full design (turns 3a + 4a). Open in the authoring tool to inspect; read the markup for exact inline values.
- `android-frame.jsx` / `ios-frame.jsx` — device-bezel wrappers used only for presentation (not part of the app UI).
- `github.md` — links this design to the source repo and maps each screen to the Kotlin files it derives from.
