# Android lessons learned — choreDash + taskDash

Captures non-obvious issues found during development so the next
project can skip the debugging.

---

## 1. ModalBottomSheetProperties requires all three parameters (Material3 1.2.x)

In Material3 1.2.1 the `ModalBottomSheetProperties` constructor has three
required parameters. Omitting any of them causes a compile error:

```kotlin
// Correct
properties = ModalBottomSheetProperties(
    securePolicy = SecureFlagPolicy.Inherit,
    isFocusable = true,
    shouldDismissOnBackPress = true
)
```

`shouldDismissOnBackPress = false` looked appealing as a way to avoid the
invisible-overlay bug (see #2), but it also swallows the system back
button entirely — users get stuck in the sheet with no way out except
Save/Delete. Keep it `true` and route `onDismissRequest` through the same
hide-then-remove path as the sheet's own buttons (see #2).

---

## 2. ModalBottomSheet: route `onDismissRequest` through `hide()` to prevent stuck overlay

When the user swipes down, taps the scrim, or presses back,
`onDismissRequest` fires. If you call `onDismiss()` directly from there
(removing the composable from the tree while the sheet is still
animating), Material3 1.2.x leaves an invisible transparent overlay that
blocks all touch input.

Fix — call `sheetState.hide()` from `onDismissRequest` (same as the
Cancel / Save button handlers) and only call `onDismiss()` once the hide
animation completes:

```kotlin
ModalBottomSheet(
    onDismissRequest = {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    },
    ...
) {
    // Cancel / Save buttons use the same pattern:
    sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
}
```

---

## 3. FilterChip requires explicit high-contrast colors

The default selected state for `FilterChip` uses `secondaryContainer`,
which looks identical to the unselected surface colour in many themes.
Always override:

```kotlin
FilterChip(
    ...,
    colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor    = MaterialTheme.colorScheme.onPrimary
    )
)
```

---

## 4. Bottom-nav with popUpTo / saveState / restoreState (Navigation Compose 2.7)

Without `saveState = true` / `restoreState = true`, switching tabs resets
the back-stack and loses ViewModel state (scroll position, open sheets, etc.):

```kotlin
navController.navigate(screen.route) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState   = true
}
```

---

## 5. SwipeToDismissBox — use `confirmValueChange = { false }` for "log" swipe

Returning `false` from `confirmValueChange` prevents the item from
actually sliding off-screen while still triggering your side-effect:

```kotlin
SwipeToDismissBox(
    state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onSwipe()
            }
            false  // never actually dismiss
        }
    ),
    ...
)
```

---

## 6. HiltWorker pattern — inject via @AssistedInject

```kotlin
@HiltWorker
class MyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val myDep: MyDependency
) : CoroutineWorker(context, params)
```

The `Application` must implement `Configuration.Provider` and supply
`HiltWorkerFactory` — do **not** call `WorkManager.initialize()` separately
or WorkManager will throw on the second init.

---

## 7. AlarmManager exact alarms — check canScheduleExactAlarms() first

On Android 12+ (API 31+), exact alarms require the
`SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM` permission **and** the
user to grant it in Settings. Always guard:

```kotlin
if (!alarmManager.canScheduleExactAlarms()) return
alarmManager.setExactAndAllowWhileIdle(...)
```

The manifest declares `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />`.

---

## 8. NFC foreground dispatch must be enabled/disabled in onResume/onPause

```kotlin
override fun onResume() {
    super.onResume()
    nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, techLists)
}
override fun onPause() {
    nfcAdapter?.disableForegroundDispatch(this)
    super.onPause()
}
```

Bridging NFC tag IDs from Activity to Compose is done with
`mutableStateOf<String?>` on the Activity and passing it through the
NavGraph as a parameter.

---

## 9. Supabase client must be re-created when credentials change

`SupabaseClientProvider` holds a `MutableStateFlow<SupabaseClient?>` and
replaces it whenever `saveCredentials()` is called from Settings.
Repositories call `currentClient()` on every request — never cache the
client reference.

---

## 10. No hardcoded colors in TextStyle / Typography

Do not set `color` inside `Typography` definitions. Hardcoded text colors
break dark-mode support. Always let individual composables set color via
`MaterialTheme.colorScheme.*` at the call site.

---

## 11. `role = Role.Button` inside `.semantics {}` needs its own import

`import androidx.compose.ui.semantics.Role` and
`import androidx.compose.ui.semantics.semantics` are not enough — `role`
is an extension property on `SemanticsPropertyReceiver` and needs its own
import:

```kotlin
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
```

Missing it compiles fine in some IDE setups (auto-import resolves it) but
fails `compileDebugKotlin` in CI with `Unresolved reference 'role'`.

---

## 12. Debug builds need a stable signing key, or sideloads look "new" every time

If `signingConfigs` doesn't pin a debug keystore, AGP auto-generates one
per clean checkout/CI run. Every resulting APK is signed with a different
certificate, so each install looks like a brand-new, unrecognized signer to
Play Protect — even though it's "the same app" to you.

Fix: commit a dedicated `app/debug.keystore` for the project and pin it:

```kotlin
signingConfigs {
    getByName("debug") {
        storeFile = file("debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
    }
}
buildTypes {
    debug {
        signingConfig = signingConfigs.getByName("debug")
    }
}
```

Do **not** reuse the same debug keystore across multiple unrelated apps —
sharing one signing cert across different `applicationId`s is itself a
pattern that looks suspicious to malware scanners, and it doesn't transfer
any "reputation" between apps anyway. Each app should have its own
dedicated debug keystore.

---

## 13. AlarmManager PendingIntent request codes must be namespaced per entity type

`PendingIntent.getBroadcast` request codes are a single shared integer
space for the whole app. `AlarmScheduler.scheduleTask`/`cancelTask` use
`taskId.hashCode()` as the request code. When adding a second alarm-backed
entity (e.g. standalone reminders), do not reuse the raw id's hash code —
two different tables can produce UUIDs whose hash codes collide, silently
cancelling or overwriting the wrong alarm.

Prefix the id before hashing so each entity type gets its own namespace:

```kotlin
private fun reminderRequestCode(reminderId: String): Int = ("reminder_$reminderId").hashCode()
```

The same prefix must be used for both the notification id
(`NotificationManagerCompat.notify`) and the `PendingIntent` request code,
and `AlarmReceiver` must branch on which `EXTRA_*_ID` is present in the
intent to know which repository to update.

---

## 14. CodeQL `java/android/implicit-pendingintents` always fires on Kotlin, even with FLAG_IMMUTABLE

The "Use of implicit PendingIntents" query (CWE-927) only treats a
`PendingIntent` as immutable if `FLAG_IMMUTABLE` reaches the flags argument
through a `BinaryExpr`/`BitwiseExpr` (Java's `|` operator). Kotlin's `or`
infix function compiles to a `MethodCall`, not a `BinaryExpr`, so CodeQL can
never see `FLAG_IMMUTABLE` in `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE` and
flags every `PendingIntent.getActivity`/`getBroadcast` call in Kotlin
regardless of the flags actually passed.

Adding `setPackage()` or making the `Intent` more explicit does not fix
this — the alert is about the flags argument, not the intent. The fix is to
exclude the query in `.github/codeql/codeql-config.yml`:

```yaml
query-filters:
  - exclude:
      id: java/android/implicit-pendingintents
```

and point `codeql.yml`'s `init` step at `config-file:
./.github/codeql/codeql-config.yml` instead of `queries: security-extended`
(the config file's `queries:` block re-adds `security-extended`).

The query's display name ("Use of implicit PendingIntents") and its short ID
(`pending-intents`, as it appears in some GitHub UI/alert URLs) don't match
the `@id` declared in the `.ql` file's metadata, which is what
`query-filters` actually matches against. Get the real ID from the CodeQL
job log line `Interpreted pathproblem query "..." (<id>) at path ...`.

---

## 15. Denied "exact alarms" / notifications permissions need a Settings link, not a retry

`AlarmManager.canScheduleExactAlarms()` (API 31+) and the `POST_NOTIFICATIONS`
runtime permission can't be re-prompted with the normal permission dialog once
denied, calling the same request again is a no-op. If an app schedules
reminders, a denied permission silently means "nothing fires", with no
indication to the user why.

Implementation suggestion: add a small `permission/PermissionHelper.kt` with
intent builders that deep-link into the right system screen
(`Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` with a `package:` URI, and
`Settings.ACTION_APP_NOTIFICATION_SETTINGS`), then surface the current
permission state in the settings screen with a button or link using those
intents. Re-check the permission state on `ON_RESUME` (via a
`LifecycleEventObserver`) since the user returns from the Settings app
without the Activity restarting. `permission/PermissionHelper.kt` and the
"Reminders & alerts" section of `SettingsScreen` in this repo are a working
example of the pattern.

---

## 16. `a11y_check.py` flags Glance widget `.clickable(Action)` as a missing Role

`androidx.glance.action.clickable(Action)` (used in `GlanceModifier` chains
for home screen widgets) is an unrelated API to Compose's
`Modifier.clickable{}` / `.combinedClickable{}`. Glance has no
`Modifier.semantics { role = Role.* }` equivalent. TalkBack reads the
`Text`/`Button`/`CheckBox` content inside the clickable element instead, so
there's nothing to add.

`a11y_check.py` now skips any file that imports
`androidx.glance.action.clickable` rather than trying to distinguish the two
APIs line by line. Don't "fix" Glance widget code by adding a `.semantics {
role = Role.X }` call: it doesn't exist for `GlanceModifier` and won't
compile.

---

## 17. `NotificationChannel` settings are immutable after creation, even via `createNotificationChannel()` again

`importance`, `enableVibration`, `setSound`, and `setBypassDnd` are only read
the *first* time a channel id is created on a device. Calling
`createNotificationChannel()` again with the same id and different settings
is silently ignored, so shipping a change like "make this channel bypass Do
Not Disturb" does nothing for users who already have the app installed: the
channel already exists from a previous version with the old settings.

Fix: give the channel a new id (e.g. append `_v2`) and delete the old one in
`createChannels()`:

```kotlin
const val CHANNEL_TASK_REMINDERS = "dash_task_reminders_v2"
private const val CHANNEL_TASK_REMINDERS_LEGACY = "dash_task_reminders"

fun createChannels(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.deleteNotificationChannel(CHANNEL_TASK_REMINDERS_LEGACY)
    nm.createNotificationChannel(NotificationChannel(CHANNEL_TASK_REMINDERS, ...).apply { ... })
}
```

Any future change to a channel's importance/sound/vibration/DND-bypass needs
the same treatment: new id, delete the old one.

**Corollary, and the non-obvious part: DND bypass depends on access at creation
time.** `setBypassDnd(true)` is honoured only if the app already holds Do Not
Disturb access (`isNotificationPolicyAccessGranted`) *at the moment the channel
is first created*. Combined with the immutability above, a channel created
before the user grants access will never bypass DND afterwards, even though
`createChannels()` re-runs and calls `setBypassDnd(true)` again on the same id.
The alarm channels work around this by switching to a distinct id (`..._dnd`)
while bypass is active, so granting access creates a fresh, bypassing channel
instead of trying to mutate the existing one, and `createChannels()` is re-run
the instant access is granted (from the Settings "Do Not Disturb access" row's
`ON_RESUME` check) so the bypassing channel exists before the next alarm fires.

---

## 18. Migrating composables to a shared file: re-check imports on both ends

When splitting reusable composables out of a screen file into a shared
`SettingsComponents.kt` (per `docs/SETTINGS_PATTERN.md`), check imports in
*both* files:

- The original screen file may still use APIs (e.g. `Modifier.clickable {}`
  for a row that wasn't extracted, like a local `PermissionRow`) whose import
  only existed because the extracted code also used it. Removing the
  extracted code can silently delete a still-needed import.
- The new shared file often accumulates imports added out of order during
  iterative edits (e.g. duplicate-looking but distinct `PaddingValues`
  re-exports from `foundation.layout` vs `material3`, or imports appended at
  the bottom of the block instead of sorted). Neither breaks the build, but
  sort them for readability before committing.

Also, when removing a full-screen modal (e.g. the old `ChangelogScreen.kt`)
in favour of an in-sheet dialog, grep the nav graph for the route string
*and* the now-unused screen-level callback parameter (e.g.
`onNavigateToChangelog`) — Compose Navigation routes and composable function
signatures drift independently, so removing one doesn't surface a compile
error for the other reviewer to catch by inspection.

---

## 19. "Add to calendar" / "share as .ics" needs no new permissions, but needs a FileProvider

`Intent.ACTION_INSERT` on `CalendarContract.Events.CONTENT_URI` (pre-filled via
`EXTRA_EVENT_BEGIN_TIME` / `EXTRA_EVENT_END_TIME` / `EXTRA_EVENT_ALL_DAY` etc.)
just launches the system calendar app's "create event" UI for the user to
review and save. It requires **no** `READ_CALENDAR`/`WRITE_CALENDAR`
permission.

Sharing a generated `.ics` file via `Intent.ACTION_SEND` (`text/calendar`)
does need a `FileProvider`:
- `<provider android:name="androidx.core.content.FileProvider" ...
  android:exported="false" android:grantUriPermissions="true">` plus a
  `res/xml/file_paths.xml` with a `<cache-path>` entry, both new but neither
  requiring a `<uses-permission>`.
- `androidx.core:core-ktx` (already a dependency in most apps) is sufficient;
  FileProvider lives in that artifact.

Factor date/time derivation (all-day vs timed event, "no date at all" cases)
into one shared utility (`util/CalendarShareUtils.kt` in this repo) so the
ACTION_INSERT intent, the `.ics` body, and the plain-text share summary all
agree on the same begin/end/all-day logic.

Before adding "add to calendar" / "share" actions to every entity type, check
whether each type actually has an edit/detail view to put the icons in. In
this repo, reminders (`ReminderCard.kt`) only render as a checklist row with
no edit sheet, unlike chores (`EditChoreSheet.kt`) and tasks
(`EditTaskSheet.kt`) which have one. Don't build a new detail sheet just to
host these two icons, scope to the entity types that already have a natural
home for them.

---

## 21. "Remove latest log" must re-fetch the scan ID from the DB, not use cached state

`chore.lastScanId` is populated by `load()`. If a log is added after `load()` returns
(e.g. via swipe-to-log while the overview sheet is already in memory), the cached
`lastScanId` still points to the *previous* scan. Calling `deleteScan(chore.lastScanId)`
then deletes the correct older log and leaves the accidental newer one.

Fix: always call `scanHistory(chore.tagId, limit = 1)` right before the delete to get
the actual latest scan ID from the server:

```kotlin
val latestScanId = choreRepository.scanHistory(chore.tagId, limit = 1)
    .firstOrNull()?.id ?: return@runCatching
choreRepository.deleteScan(latestScanId)
```

The same rule applies to any destructive operation that targets "the most recent X" —
never trust a cached ID when freshness matters.

---

## 22. Card date display: make the status date prominent, the history date subtle

When a card shows two dates, one representing current status (e.g. due date)
and one representing history (e.g. last done), give them clearly different
visual weights:

- **Status date** (due date, next action): `bodyLarge` + `FontWeight.SemiBold`
  in the colour-coded `dateColor` so it reads immediately at a glance.
- **History date** (last done, last scanned): `labelSmall` in
  `onSurfaceVariant` directly below, visually subordinate.

If there is no status date, show the history date alone at `bodySmall` in
`dateColor` — it becomes the only signal, so it earns the colour. If there is
no history at all (`lastScanned == null`), show "Never" in italic at
`onSurfaceVariant`.

Layout: a single `Row` with `verticalAlignment = Alignment.CenterVertically`
keeps the title vertically centred against the dates column even when the
dates column is taller than a single line. Putting title and dates in the same
`Row` (rather than stacking them in separate rows inside a `Column`) is what
produces the centred alignment without extra padding arithmetic.

---

## 23. Prefer bundled font files over downloadable fonts for app typefaces

If a font is part of the app's visual identity (used on every screen), bundle the `.ttf`
files in `res/font/` rather than using the GMS downloadable font provider. Downloadable fonts
introduce a fragile dependency on GMS availability and a race between provider response time
and the first Compose frame. Bundled fonts are always available, load synchronously without
blocking the main thread (Compose reads them from the APK), and work on non-GMS devices.

```kotlin
// Bundled variable font — one file, all weights via FontVariation
val Nunito = FontFamily(
    Font(R.font.nunito, FontWeight.Normal,   variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito, FontWeight.Medium,   variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.nunito, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito, FontWeight.Bold,     variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

// Bundled static TTFs — one file per weight
val Lora = FontFamily(
    Font(R.font.lora_medium,   FontWeight.Medium),
    Font(R.font.lora_semibold, FontWeight.SemiBold),
)
```

No `loadingStrategy`, no `preloaded_fonts` manifest entry, no `font_certs.xml`.

**If you must use downloadable fonts** (e.g., to avoid APK size increase for a large family):
use the `app:` namespace in the XML descriptor (not `android:` — the native resolver returns
null on some API levels), and set `loadingStrategy = FontLoadingStrategy.Async` in Compose
(not `Blocking`, which crashes on a null Typeface; not `OptionalLocal`, which permanently
degrades to the system font when the GMS cache is cold on first launch).

**Previous failure chain in this project:**
- Switched from programmatic `GoogleFont.Provider` (safe) to XML descriptors with `android:`
  namespace: null Typeface on API 26-28, NPE crash on launch.
- Fixed namespace to `app:`: load path correct but `Blocking` default still crashes when GMS
  unavailable.
- Changed to `OptionalLocal`: no crash, but fonts permanently missing on cold start / non-GMS.
- Changed to `Async`: correct for downloadable fonts, but still GMS-dependent.
- Bundled the TTF files: no dependency, always loads, all devices.

---

## 24. Lazy singleton with async init creates a cold-start race for all consumers

`SupabaseClientProvider` is a Hilt singleton whose `init {}` block launches a
background IO coroutine to collect the first settings emission and build the
Supabase client. Because it is instantiated lazily (only when first needed by a
ViewModel), and because ViewModels call `load()` immediately in their own
`init {}` blocks, there is a real window where `currentClient()` returns null.

The fix is a `CompletableDeferred<Unit>` that is completed after the first
settings emission is processed. Repositories call a `suspend fun awaitClient()`
that awaits this deferred before returning the client (or throwing if credentials
are blank). `CompletableDeferred.complete()` is a no-op after the first call, so
subsequent credential changes work normally.

```kotlin
private val ready = CompletableDeferred<Unit>()

init {
    scope.launch {
        settingsRepository.settings.collect { settings ->
            _client.value?.close()
            _client.value = buildClient(settings)
            ready.complete(Unit)   // no-op on subsequent emissions
        }
    }
}

suspend fun awaitClient(): SupabaseClient {
    ready.await()
    return _client.value
        ?: error("Supabase client not configured — enter credentials in Settings")
}
```

Repositories replace `private fun requireClient()` (non-suspend, reads
`currentClient()`) with `private suspend fun requireClient() =
clientProvider.awaitClient()`. All repository methods are already `suspend`, so
no call-site changes are needed.

---

## 25. AppWidget receivers must be `android:exported="true"`

AppWidget broadcast receivers need `android:exported="true"` even though they look like
internal components. The system (OS) sends `APPWIDGET_UPDATE` from a different process/UID,
and on Android 12+ (API 31+, targetSdk >= 31) `exported=false` blocks that delivery entirely.

```xml
<!-- Correct -->
<receiver android:name=".widget.MyWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data android:name="android.appwidget.provider"
        android:resource="@xml/my_widget_info" />
</receiver>
```

With `exported=false`, the widget can be added to the home screen but never renders or
updates — the system silently drops the update broadcast. The bug is invisible at install
time and easy to miss in testing if you don't explicitly verify widget refresh.

---

## 26. Resolve notification delivery mode at fire time, not schedule time

The delivery-mode setting (ALARM/NOTIFICATION/SILENT) was baked into every alarm
intent at schedule time and then round-tripped through notification action intents.
That produced two bugs and one inconsistency:

1. `NotificationHelper` stuffed the notification *channel id* into the snooze
   intent's `EXTRA_DELIVERY_MODE`. The receiver compared it against `"ALARM"`/
   `"SILENT"`, never matched, and every snoozed alarm-mode or silent-mode reminder
   came back on the default notification channel.
2. The reminder snooze path dropped the task id, so a snoozed task-linked reminder
   stopped marking its task as reminded in Supabase.
3. UI ViewModels scheduled with the default mode while BootWorker used the settings
   value, so the same reminder fired differently before vs after a reboot.

The fix that removes the whole bug class: treat delivery mode as presentation and
resolve it from `SettingsRepository` in `AlarmReceiver` at fire time (mode -> channel
mapping lives in exactly one place, `NotificationHelper.channelForDeliveryMode`).
Intents carry only identity (reminder id, task id, title). Anything an action intent
does still need must be the scheduler's own vocabulary (entity ids), never derived
presentation values like channel ids.

---

## 27. Warn before discarding unsaved changes in a ModalBottomSheet, without re-triggering the stuck-overlay bug (#2)

Back press, scrim tap, and swipe-down all funnel through the same
`onDismissRequest` (see #1/#2). To warn on unsaved changes instead of silently
discarding them, intercept `onDismissRequest` (and any explicit Cancel button)
with a `requestDismiss()` that checks a locally-computed `isDirty` flag:

```kotlin
fun requestDismiss() {
    if (isDirty) {
        sheetScope.launch { sheetState.show() }  // bounce back — see below
        showDiscardConfirm = true
    } else {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
}
```

The `sheetState.show()` call matters: by the time `onDismissRequest` fires for a
scrim tap or swipe-down, Material3 has already animated the sheet to `Hidden`.
If you only set `showDiscardConfirm = true` without re-showing the sheet, you
recreate the exact stuck-invisible-overlay bug from #2 — the sheet is
transparent but its composable (and the `AlertDialog` on top of it) is still in
the tree.

Compute `isDirty` by snapshotting each field's initial value with its own
`remember { ... }` right next to the `mutableStateOf` that seeds from it, then
comparing current to initial — not by re-deriving the "original" from the
nullable input model at comparison time. A create-mode field seeded with a
non-blank default (e.g. `category = DEFAULT_CATEGORY` when there's no existing
entity) will never equal `null`/`""`, so diffing directly against the input
model produces a false-positive dirty flag the instant the sheet opens:

```kotlin
// Wrong: category is DEFAULT_CATEGORY but task is null, so this is "dirty" immediately
val isDirty = category != task?.category

// Right: snapshot what the field actually started as
val initialCategory = remember { if (task != null) task.category ?: "" else DEFAULT_CATEGORY }
var category by remember { mutableStateOf(initialCategory) }
val isDirty = category != initialCategory
```

## 28. Theme-builder parameters must come from user state, not clamps or constants

The custom colour scheme builder used to clamp the picked saturation and
hard-code the lightness in dark mode (`primaryS.coerceAtMost(0.55f)`, `L =
0.75f`). It compiled, looked "tastefully muted" in review, and shipped — but it
meant the user's picks had almost no effect: every dark custom theme converged
on the same pastel wash, and the picker swatches (drawn from the raw HSL)
never matched the applied theme.

Two rules fall out of this:

1. Apply the picked colour **verbatim** to its role and solve legibility on the
   *derived* colours instead — compute `onX` from relative luminance
   (near-black on bright picks, white on dark picks) rather than constraining
   the pick itself.
2. Previews must resolve through the same code path as the scheme builder. If
   a swatch has its own S/L constants, it will drift from what the theme
   actually renders.

## 29. Derive Material 3 surface ramps from luminance, never from a background's raw HSL

When building a custom `ColorScheme` around a user-chosen background colour,
the "is this a light or dark surface?" decision has to be made from perceived
**luminance** (`Color.luminance()`), but the neutral tones themselves must not
reuse the background's own HSL saturation or lightness. Those two measures
diverge badly for vivid mid-lightness hues: pure yellow `#F3FF00` has luminance
~0.93 (clearly "light") yet HSL lightness only 0.5, so a `surfaceVariant`
computed as `hsl(bgHue, bgSat.coerceIn(...), bgL - 0.10)` collapses to
`hsl(63, 0.20, 0.40)` — a dark olive that painted every card muddy while the
page background stayed bright yellow.

The fix, and the general rule:

1. Derive all neutral/surface roles from the background **hue only**, at a
   near-zero fixed saturation (~0.04), so a saturated background can never tint
   surfaces muddy.
2. Anchor surface lightness to **absolute per-mode values** (a monotonic ramp
   like 0.99 down to 0.87 in light mode, 0.06 up to 0.24 in dark mode), not to
   the background's own lightness.
3. Pick `onBackground` / `onSurface` by contrast (reuse `contrastingOn`) so text
   stays legible on any pick.
4. Set the entire `surfaceContainer*` family plus `surfaceDim` / `surfaceBright`
   explicitly. If you leave them out of `lightColorScheme(...)` /
   `darkColorScheme(...)`, Material 3 fills them with a fixed default neutral
   (lavender in light, charcoal in dark) that clashes with the custom
   background in menus, bottom sheets, and the navigation bar.

## 30. Make "fixed" accent colours theme-aware through a CompositionLocal, not top-level vals

The content-type accents (Tasks/Chores/Reminders tones on the bottom nav and
add menu) were plain top-level `val`s in `Color.kt`, imported directly by the
nav graph and the FAB. That kept them stable across the built-in palettes, but
it also made them impossible to adapt: under a vivid custom theme they stayed
fixed lavender/mint/peach and read as three random colours unrelated to the
user's picks.

The fix is a `staticCompositionLocalOf` (`LocalTypeAccents`) that defaults to
the fixed identity tones, with `DashTheme` overriding it for the custom theme by
mapping each content type onto the user's primary/secondary/tertiary container
roles. Consumers read `LocalTypeAccents.current` instead of importing the
constants. General rule: when a colour needs to be a stable default in some
themes but derive from the scheme in others, resolve it through a
CompositionLocal provided next to `MaterialTheme` (which is the one place that
knows both the active `AppTheme` and the built `ColorScheme`), not through a
top-level constant that call sites import directly.

The Glance home-screen widgets keep importing the `Type*` constants directly:
they render outside the Compose `MaterialTheme` tree and can't read this
CompositionLocal, so their type colours stay fixed by design.

---

## 31. PendingIntent identity needs a data URI, not just a namespaced request code

Lesson 13 namespaced request codes per entity type, but that is only half the
identity story: `PendingIntent` identity is (request code + `Intent.filterEquals`),
and `filterEquals` ignores extras. All alarm intents here wrapped the same bare
`Intent(context, AlarmReceiver::class.java)`, so two ids whose `String.hashCode()`
values collide would silently cancel or clobber each other's alarms, and
`FLAG_UPDATE_CURRENT` could swap extras between unrelated alarms.

Stamp a unique `data` URI onto every alarm intent
(`choredash://alarm/task/$taskId`, `choredash://alarm/reminder/$reminderId`):
`filterEquals` compares data URIs, so every entity is a distinct intent regardless
of request code. When changing PendingIntent identity, alarms registered by the
*old* app version keep the old identity — sweep-cancel the legacy (no-URI) form once
from BootWorker (which runs on `MY_PACKAGE_REPLACED`), or they stay armed alongside
the new ones and double-fire. Same defence GaMeD adopted after hitting the collision
in production; see its LESSONS entry "PendingIntent identity ignores extras".

---

## 32. A follow-up PR needs a NEW changelog fragment, never an edit to an unreleased one

`check_changelog_fragment.py` counts only fragments with git status `A`
(added) in `git diff base...HEAD`. Extending a fragment that a previous
PR already merged into `changelog/unreleased/` shows as `M`, so the check
fails with "no fragment added" even though the file carries new entries.

For follow-up work on an already-merged PR, always add a fresh
`changelog/unreleased/<new-slug>.json` and leave the merged fragment
untouched; the release consolidation merges all fragments anyway, so
splitting them costs nothing.

---

## 33. Partial-update DTOs with all-null defaults cannot clear a column via Supabase

The Supabase client serializes with `encodeDefaults = false`, so any DTO
property equal to its default is omitted from the request body. A
partial-update DTO where every field defaults to null (like `TaskUpdate`)
therefore cannot express "set this column to null": `TaskUpdate(completedAt
= null)` encodes as `{}`, an empty PATCH that matches nothing, and
`decodeSingle()` on the empty response surfaces as a "List is empty" error.
Clearing a single field alongside set fields fails more quietly: the null is
just dropped and the old value stays.

Build such PATCH bodies as `Map<String, String?>` (or a `JsonObject`)
instead: map entries have no defaults, so null values are sent as explicit
JSON nulls. `ChoreRepository.archiveTag` and `TaskRepository`'s payload
builders are the pattern; `TaskPayloadTest` pins the behaviour.

---

## 34. "Could not find or load main class" with a BLANK name means an empty argv word

`gradlew` failed with `Error: Could not find or load main class ` (empty
class name). The launch line evaluated `'"$JAVA_OPTS"'` and
`'"$GRADLE_OPTS"'` as quoted expansions, so when those variables are unset
they become empty-string arguments; java takes the first non-option
argument as the main class, and that argument was `""`. Expand optional
option variables unquoted in the wrapper (`$JAVA_OPTS`), so unset means
zero words, or filter empties the way the upstream Gradle script does with
xargs. The blank class name in the error is the tell: some argv word
before the real main class evaluated to an empty string.
