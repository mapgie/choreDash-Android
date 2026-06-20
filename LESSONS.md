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

## 20. Card date display: make the status date prominent, the history date subtle

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
