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

## 13. Denied "exact alarms" / notifications permissions need a Settings link, not a retry

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
