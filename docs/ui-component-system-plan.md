# UI Component System: modularity and cohesion plan

Written 2026-08-08. Target: make Chores, Tasks and Reminders look and behave like one app
by making them literally share the same composables, instead of each screen hand-rolling
its own version of the same widget.

## The Kotlin answer to "like Blazor or Angular components"

There is no missing framework here. A `@Composable` function *is* the component. What the
codebase is missing is the discipline around them:

| Concept elsewhere | Compose equivalent | Current state |
|---|---|---|
| Component with `<ng-content>` / `RenderFragment` children | **Slot API**: composable taking `content: @Composable () -> Unit` parameters | Not used. Cards are monolithic. |
| Shared design tokens / SCSS variables | `MaterialTheme` + a project `Dimens` object + `CompositionLocal` | Colours partly done (`LocalTypeAccents`); spacing and sizing hardcoded per file. |
| Storybook | `@Preview` composables, ideally one gallery file | Zero previews in the project. |
| Component library folder | `ui/components/` | Exists, but the components are screen-specific, not shared. |

So the work is not "adopt a new pattern", it is "extract the shared shell, give it slots,
and delete the copies".

## What is actually inconsistent today

Concrete, file-by-file. This is the evidence behind the plan.

### 1. Two different card shells

| | `ChoreCard.kt` | `TaskCard.kt` |
|---|---|---|
| Container colour | `lerp(surface, statusColor, 0.07f)` (tinted by status) | `surfaceVariant` (solid) |
| Elevation | `1.dp` | none |
| Horizontal inset | `16.dp`, applied *inside* the card | `12.dp`, applied by the *screen* |
| Vertical rhythm | `4.dp` padding per card | `spacedBy(6.dp)` on the LazyColumn |
| Layout | single centred row, everything vertically centred | column of title + meta row, top-weighted |
| Category badge | in a `Column` under the title | in the meta row beside the due label |

The user preference is explicit: **keep the Tasks solid container, keep the Chores vertical
centring.** Both come free once there is one shell.

### 2. `CategoryBadge` is duplicated verbatim

`ChoreCard.kt:166-179` and `TaskCard.kt:200-213` are byte-identical private functions. Two
copies means two places to change, which is exactly how the screens drifted apart.

### 3. The owner indicator differs in size, colour, and position

- `ChoreCard.kt:147-163` — 28dp circle, `secondaryContainer`, placed **between the title and
  the dates**.
- `TaskCard.kt:140-156` — 20dp circle, `primaryContainer`, placed **rightmost**.

Same person renders as two different colours on two screens. Requested fix: rightmost on
both, one size, and colours consistent across screens.

### 4. The status accent bar means two different things

Both cards use the same three colours from `Color.kt:128-130` (`StatusStale` red,
`StatusAging` amber, `StatusFresh` green), but:

- On Chores the bar encodes **freshness/overdue** (`ChoreCard.kt:32-36`).
- On Tasks the bar encodes **priority** (`TaskCard.kt:57-61`) — so a green bar means
  "normal priority", not "not due", while the *text* beside it still encodes urgency
  (`DueBadge`, `TaskCard.kt:162-186`).

This is the single biggest reason the two screens read as different apps: the same red
stripe means "act now" on one screen and "high priority, due next month" on the other.

### 5. `ReminderCard` uses the error palette for a non-error

`ReminderCard.kt:50` uses `errorContainer` and `:57` uses `colorScheme.error` for a merely
overdue reminder. `CLAUDE.md` reserves `error` for genuine errors and destructive
confirmations. It should use the same overdue vocabulary as the other two cards.

### 5b. Translucent card containers leak the swipe background (visible bug)

An archived, overdue Memo renders as a purple card with a red rim and the word "Done✔"
printed underneath its own title. That is not a colour-scheme choice, it is a layering bug:

- `SwipeToDismissBox` composes `backgroundContent` **at all times**, not only mid-swipe. At
  rest, the `primaryContainer` panel with the "Done✔" label sits directly behind every card
  (`RemindersListScreen.kt:284-302`).
- `ReminderCard` containers are semi-transparent: `surfaceVariant.copy(alpha = 0.5f)` when
  done, `errorContainer.copy(alpha = 0.25f)` when overdue (`ReminderCard.kt:48-52`). A 25%
  alpha fill lets the purple panel and its label read straight through.
- The rim appears because the swipe panel is inset `vertical = 4.dp` while the card is not,
  so the card's tinted edge overhangs the panel against the page background.

`TaskCard.kt:69` has the same `alpha = 0.5f` container for done tasks, so completed tasks
leak the purple panel too — less obviously, because 50% hides more than 25%.

Two rules follow, and the shared shell enforces both:

1. **Card containers are always opaque.** To dim a done or archived row, blend toward the
   surface (`lerp(surfaceVariant, surface, 0.5f)`) or drop content alpha, never the
   container's.
2. **Compose the swipe panel only while a swipe is in progress**, gated on
   `dismissState.dismissDirection != Settled` (or `targetValue`/progress), so nothing sits
   behind a resting card at all.

### 5c. Memos is missing the features the other two tabs have

No filter chips, no zen mode, no owner indicator, no sort control. Memos gets a `TopAppBar`
instead, which is the one thing the other two do not have. The tab reads as an
afterthought because structurally it is one.

### 6. Screen scaffolding is copy-pasted and has already drifted

- Sticky category header: `ChoreListScreen.kt:246-256` and `TaskListScreen.kt:208-218` are
  identical code.
- Swipe-to-action wrapper: `ChoreListScreen.kt:489-546` and `TaskListScreen.kt:333-392` are
  the same composable with different insets (16/4 vs 12/4) and different labels.
- Collapse/expand section headers render three different ways: a full-width `TextButton`
  for archived chores (`ChoreListScreen.kt:322-336`), a `Row` + `IconButton` for done tasks
  (`TaskListScreen.kt:248-268`), and a third variant in `RemindersListScreen`.
- Empty and error states: `ChoreListScreen.kt:548-576` has private composables;
  `TaskListScreen.kt:191-198` inlines a `Box`; `RemindersListScreen.kt:100-116` inlines
  another.
- The filter chip row plus 48dp icon toggles is duplicated between the two list screens.
- Reminders has a `TopAppBar` styled with `primaryContainer`; Chores and Tasks have nothing.
  That is a third distinct treatment of the top of a screen, and it costs roughly 64dp of
  list space to display one word.

### 7. Every screen re-declares its own accessibility plumbing

`.semantics { role = Role.Button }.combinedClickable { }` is repeated at every call site
(`ChoreListScreen.kt:348-356`, `:538-544`, `TaskListScreen.kt:383-389`). Put the click
handling inside the shared card and it is correct once, everywhere, permanently.

## Target architecture

```
ui/
  theme/
    Dimens.kt          NEW  spacing, sizes, bar width, avatar size, min row height
    Color.kt           +    owner avatar palette
    StatusTone.kt      NEW  the one status vocabulary + per-domain mappers
  components/
    core/
      DashListCard.kt      NEW  the slot-based card shell
      SwipeActionRow.kt    NEW  one swipe-to-act wrapper
      OwnerAvatar.kt       NEW  the one owner indicator
      CategoryBadge.kt     NEW  moved out of the two cards
      MetaLabel.kt         NEW  secondary text, due labels, relative time
      ListSectionHeader.kt NEW  sticky header + collapsible header
      DashStates.kt        NEW  empty / error / loading
      DashFilterBar.kt     NEW  filter chips + trailing icon actions
      DashScreenHeader.kt  NEW  the thin type-coloured identity strip
      Gallery.kt           NEW  @Preview gallery of every component above
    ChoreCard.kt         becomes a thin binding: Chore -> DashListCard slots
    TaskCard.kt          becomes a thin binding: TaskDto -> DashListCard slots
    ReminderCard.kt      becomes a thin binding: ReminderDto -> DashListCard slots
```

### `DashListCard` — the shell

```kotlin
@Composable
fun DashListCard(
    tone: StatusTone,                                   // drives the accent bar
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,                            // done / archived
    leading: @Composable (RowScope.() -> Unit)? = null,  // checkbox slot
    trailing: @Composable (ColumnScope.() -> Unit)? = null, // dates / due slot
    owner: String? = null,                              // always rendered rightmost
    content: @Composable ColumnScope.() -> Unit,        // title + meta slot
)
```

Fixed by the shell, not by callers:

- container `surfaceVariant`, dimmed to `alpha 0.5f` when `dimmed` (the Tasks look)
- everything vertically centred, `heightIn(min = 56.dp)` (the Chores alignment, and it
  guarantees the 44dp tap target from `CLAUDE.md`)
- 4dp accent bar on the leading edge, `Color.Transparent` in zen mode
- one horizontal inset and one inter-card gap from `Dimens`
- `owner` slot pinned rightmost, after `trailing`
- `.semantics { role = Role.Button }` + `.combinedClickable { }` applied internally, only
  when `onClick`/`onLongClick` are non-null (archived chores currently pass `onClick = {}`,
  which announces a button that does nothing)

The status tint on chore cards goes away. Nothing is lost: the accent bar *and* the
"311d overdue" text both still carry the state, so the colour-alone rule stays satisfied.

### `StatusTone` — one vocabulary

```kotlin
enum class StatusTone { CRITICAL, ATTENTION, OK, NEUTRAL, NONE }

@Composable fun StatusTone.barColor(): Color
@Composable fun StatusTone.textColor(): Color

fun Chore.statusTone(): StatusTone
fun TaskDto.statusTone(): StatusTone       // from urgency(), not priorityEnum()
fun ReminderDto.statusTone(): StatusTone   // replaces the errorContainer usage
```

**Decision needed, with a recommendation.** The task bar should switch from encoding
*priority* to encoding *urgency*, so red means "act now" on every screen. Priority then
needs a non-colour carrier — recommend a small leading marker on the title (`!` for higher,
nothing for normal, a downward chevron for lower), which is also better for the ~9% of
users with red-green colour blindness than the current colour-only priority. The
alternative — keep priority on the bar and change Chores instead — is worse, because
overdue is the more urgent signal and deserves the loudest channel. Either way, the two
screens must agree.

### `OwnerAvatar` — rightmost, consistent, per-person

```kotlin
@Composable
fun OwnerAvatar(handle: String, modifier: Modifier = Modifier)
```

- one size (`Dimens.avatarSize`, 24dp) on every surface
- colour from `ownerColorFor(handle)`: a stable hash of the handle into a fixed palette of
  six avatar hues defined in `Color.kt`, each with a paired on-colour that meets contrast
  in both light and dark. Same person, same colour, on every screen and in the overview
  sheets.
- the initial is always drawn, and `contentDescription = "Owner: <handle>"`, so the colour
  is decoration and never the only signal
- callers pass the handle; no caller decides colour, size, or position again

### `DashScreenHeader` — the thin identity strip

Replaces the Reminders `TopAppBar` and is added to all three list tabs, so every tab opens
the same way.

```kotlin
@Composable
fun DashScreenHeader(title: String, modifier: Modifier = Modifier)  // colour from the route
```

- **Colour comes from `LocalTypeAccents`**, the same pair already driving the bottom nav
  indicator pill and the add-menu FAB (`DashNavGraph.kt:69-76`): lavender for Tasks
  (`TypeTaskContainer`), green for Chores, peach for Memos. The strip at the top and the lit
  tab at the bottom then match, which is exactly the "where am I" cue being asked for, and
  it needs no new colours. Text uses the paired `on*Container` tone.
- **Thin.** Roughly 28dp of content height against the current `TopAppBar`'s 64dp,
  absorbing the status bar inset so the colour runs cleanly to the top edge.
- **Administrative, not decorative.** `labelMedium`, letter-spaced, not a display heading.
  It names the page; it does not compete with the list.
- **Title source.** `Screen.label` for Tasks and Chores; for Memos it must come from
  `ReminderLabelStyle.displayName` in settings, since the user renames that tab.
- The `Type*Container` tones are deliberately fixed across light and dark palettes
  (`Color.kt:132-143`), so the strip keeps its identity in dark mode. That is already true
  of the nav pill, so the two stay in step.

## Sequenced work

Five PRs. Each is independently shippable and independently revertable, and each needs a
`changelog/unreleased/<slug>.json` fragment because they all touch `.kt` files.

### PR 1 — Tokens and primitives (`patch`)
Add `ui/theme/Dimens.kt`, the owner avatar palette in `Color.kt`, `ui/theme/StatusTone.kt`
with the three domain mappers, and `components/core/` with `OwnerAvatar`, `CategoryBadge`,
`MetaLabel`. Point the existing cards at the new `CategoryBadge` and `OwnerAvatar` and
delete the private copies. Visible change: owner circles become one size, one position
(rightmost), and per-person colours consistent across both screens.
*Touches:* `ChoreCard.kt`, `TaskCard.kt`, `Color.kt`, new files.

### PR 2 — The card shell (`minor`)
Add `DashListCard` and rewrite `ChoreCard` and `TaskCard` as bindings onto it. This is
where the two screens actually converge: solid container, centred alignment, shared insets,
shared accent bar. Enforces the opaque-container rule from §5b, which fixes the leaking
swipe panel on done tasks. Includes the priority-vs-urgency decision from above.
*Touches:* `ChoreCard.kt`, `TaskCard.kt`, `ChoreListScreen.kt`, `TaskListScreen.kt`.

### PR 3 — List scaffolding and the identity strip (`minor`)
Add `SwipeActionRow` (gating the panel on an in-progress swipe, §5b),
`ListSectionHeader`, `CollapsibleSectionHeader`, `DashStates`, `DashFilterBar`, and
`DashScreenHeader`; delete the per-screen copies. Drops the Reminders `TopAppBar` in favour
of the thin strip on all three tabs.
*Touches:* `ChoreListScreen.kt`, `TaskListScreen.kt`, `RemindersListScreen.kt`,
`DashNavGraph.kt`.

### PR 4 — Memos catches up (`minor`)
`ReminderCard` onto `DashListCard`, dropping the `errorContainer` misuse and fixing the
archived-card rendering. Bring Memos to parity with the other tabs: filter chips, zen mode,
and the shared collapsible section headers (§5c). Overview sheets (`ChoreOverviewSheet`,
`TaskOverviewSheet`) adopt `OwnerAvatar` and `CategoryBadge` so the detail views match the
list views.
*Touches:* `ReminderCard.kt`, `RemindersListScreen.kt`, `RemindersListViewModel.kt`,
`ChoreOverviewSheet.kt`, `TaskOverviewSheet.kt`.

### PR 5 — Gallery, guardrails, docs (`patch`)
A `Gallery.kt` of `@Preview`s covering every component in light/dark, zen/normal,
done/active, high-contrast — the Storybook analogue, and the thing that makes drift visible
in review. Add `ui/components/README.md` stating the rule: **screens compose components,
screens do not draw**. Optionally a debug-only gallery route off Settings.
*Touches:* new files only, plus `README`.

### Out of scope, deliberately
The Glance widgets (`widget/NextUpWidget.kt`, `widget/PinnedItemWidget.kt`) cannot use
Compose UI composables — Glance is a separate composable world. They should share the
*logic* (`StatusTone` mappers, `ownerColorFor`, which are plain Kotlin) but will keep their
own rendering. Worth a follow-up to align their colours with `StatusTone` once it exists.

## How to tell it worked

- `CategoryBadge`, the owner circle, the swipe wrapper, the sticky header, and the empty
  state each exist exactly once in the codebase.
- No `Card(` call outside `components/core/`.
- No hardcoded `.dp` spacing inside screen files.
- `python3 a11y_check.py` passes with the clickable declared in one place rather than five.
- No `Color.copy(alpha = ...)` on any card container, anywhere.
- Screenshot all three tabs side by side: same card silhouette, same avatar treatment in the
  same corner, same meaning for the same colour, same thin coloured strip at the top
  matching the lit tab at the bottom.
