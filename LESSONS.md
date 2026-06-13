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
    shouldDismissOnBackPress = false
)
```

`shouldDismissOnBackPress = false` is intentional — we manage dismissal
explicitly to prevent the invisible-overlay bug (see #2).

---

## 2. ModalBottomSheet: bounce `onDismissRequest` back to prevent stuck overlay

When the user swipes down or taps the scrim, `onDismissRequest` fires.
If you call `onDismiss()` directly from there (removing the composable
from the tree while the sheet is still animating), Material3 1.2.x leaves
an invisible transparent overlay that blocks all touch input.

Fix — call `sheetState.show()` inside `onDismissRequest` to cancel the
dismiss animation, then hide it properly from within the sheet's own
Cancel / Save button handlers:

```kotlin
ModalBottomSheet(
    onDismissRequest = { sheetScope.launch { sheetState.show() } },
    ...
) {
    // In the Cancel button:
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
