# UI components

**Screens compose components; screens do not draw.**

A screen's job is to wire a ViewModel's state to shared composables and route
callbacks back. It should not hand-roll a card, a badge, a header, a swipe wrapper,
or an empty state. When two screens need the same widget, that widget lives here, in
one place, so the screens cannot drift apart.

## Layout

```
ui/components/
  core/                     the shared, screen-agnostic building blocks
    DashListCard.kt         the one list-card shell (slots: leading / content / trailing / owner)
    SwipeActionRow.kt       the one swipe-to-act wrapper
    OwnerAvatar.kt          the one owner indicator (per-person colour, everywhere)
    CategoryBadge.kt        the one category pill
    SourceChip.kt           a Memo's alarm-origin chip
    MetaLabel.kt            secondary / muted text
    ListSectionHeader.kt    sticky category header + collapsible section header
    DashStates.kt           empty / error / loading
    DashFilterBar.kt        filter-chips-plus-actions bar scaffold
    DashScreenHeader.kt     the thin type-coloured identity strip
    Gallery.kt              @Preview gallery of everything above
  ChoreCard.kt              thin binding: Chore     -> DashListCard slots
  TaskCard.kt               thin binding: TaskDto   -> DashListCard slots
  ReminderCard.kt           thin binding: ReminderDto -> DashListCard slots
  (sheets, dialogs, add/edit forms ...)
```

## Rules

- **No `Card(` outside `core/`.** List rows go through `DashListCard`; it owns the
  container, the status accent bar, vertical centring, the tap target, the inset, the
  owner slot, and the `role` + `combinedClickable` plumbing (so accessibility is
  correct in one place).
- **No hardcoded spacing in screen files.** Sizes come from `ui/theme/Dimens.kt`.
- **Card containers are always opaque.** Dim a done/archived row by blending toward
  the surface or lowering *content* alpha, never the container's alpha. See
  `LESSONS.md` #32.
- **One status vocabulary.** Colour-coded state goes through `StatusTone`
  (`ui/theme/StatusTone.kt`), keyed on urgency, so the same colour means the same
  thing on every tab. Every colour-coded state must also carry a shape or label
  (never colour alone).
- **Every clickable declares a `Role`.** The shell does this for cards; anything new
  that is clickable must too (`a11y_check.py` enforces it).

## Adding or changing a component

1. Put it in `core/` if more than one screen could use it.
2. Add or update its `@Preview` in `Gallery.kt`, covering light/dark and any
   meaningful state (done, zen, high-contrast).
3. Point the screens at it and delete their private copy.
