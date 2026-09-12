# Release signing runbook

Why this exists: Android ties special-access grants (full-screen alarms, exact
alarms, Do Not Disturb access, notifications) to the app's signing certificate.
An update keeps them only if it is signed with the same key as the installed
build. choreDash releases have been debug builds signed with the committed
`app/debug.keystore`, which is `debuggable`, unminified, and signed with a key
anyone with the repo can use. The end state is a release build signed with a
dedicated key that lives only in CI secrets.

## What is already in the repo

`app/build.gradle.kts` has a `release` signing config that reads four
environment variables and applies to the `release` build type:

| Variable | Meaning |
|---|---|
| `RELEASE_STORE_FILE` | Path to the `.jks` keystore on the build machine |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias inside the keystore |
| `RELEASE_KEY_PASSWORD` | Key password |

When `RELEASE_STORE_FILE` is unset the release build type falls back to the
debug key, so `assembleRelease` works locally and for contributors. Only a
build with the real key produces an APK that updates in place.

`android-actions-sign-release.patch` in this folder is the matching change for
the shared `mapgie/android-Actions` workflows. It has not been applied yet.

## Steps, in order

### 1. Generate the keystore (once, on your machine, never committed)

```
keytool -genkeypair -v -keystore choredash-release.jks \
  -alias choredash -keyalg RSA -keysize 2048 -validity 10000
```

Use strong store and key passwords. Back up the `.jks` and both passwords
somewhere offline. If they are lost, no future build can update an existing
install in place, ever, and every user is back to reinstalling.

### 2. Add the CI secrets on `mapgie/choreDash-Android`

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | output of `base64 -w0 choredash-release.jks` |
| `RELEASE_STORE_PASSWORD` | the keystore password |
| `RELEASE_KEY_ALIAS` | `choredash` |
| `RELEASE_KEY_PASSWORD` | the key password |

### 3. Apply the android-Actions patch

In a clone of `mapgie/android-Actions`, on a branch:

```
git am path/to/android-actions-sign-release.patch
```

Push and open a PR. The patch adds a `sign-release` input (default `false`)
and four optional `RELEASE_*` secrets to `android-build-release.yml`, forwards
them from `release.yml`, and documents them in the README. Dash, GoFlo and the
other callers are unaffected until they opt in.

Then, before merging, add a second commit that re-pins the nested reference
inside `release.yml`:

```yaml
    uses: mapgie/android-Actions/.github/workflows/android-build-release.yml@<sha of the first commit>
```

The nested call has to point at a commit that already defines `sign-release`,
which is why this is two commits. Merge with a merge commit or rebase rather
than a squash, so the first commit's SHA stays reachable. Note the SHA of the
final commit on `main`.

### 4. Wire choreDash's callers (a follow-up PR in this repo)

In `.github/workflows/build.yml`, replace the pinned SHA with the new
android-Actions commit and add the input and secrets:

```yaml
  build:
    uses: mapgie/android-Actions/.github/workflows/android-build-release.yml@<new sha>
    with:
      app-name: Dash
      create-release: ${{ github.event_name == 'workflow_dispatch' }}
      sign-release: true
    secrets: inherit
```

In `.github/workflows/release.yml`:

```yaml
  release:
    uses: mapgie/android-Actions/.github/workflows/release.yml@<new sha>
    with:
      app-name: Dash
      sign-release: true
    secrets: inherit
```

That follow-up PR touches only workflow files, so CI does not demand a
changelog fragment, but it must carry one anyway: it is the change whose next
release forces the one-time reinstall described below. Suggested fragment,
with the bump left as a decision rather than a default:

```json
{
  "bump": "minor",
  "changed": [
    "App updates now keep your granted permissions (full-screen alarms, exact alarms, Do Not Disturb access) instead of resetting them, because releases are signed with a permanent release key.",
    "One-time cost: this version cannot install over the previous one. Uninstall the old version first. That wipes on-device data (memos, settings, snoozes, custom themes, saved NFC records) once; every update after this one keeps everything."
  ]
}
```

`minor` matches the versioning table in `.claude/CLAUDE.md` (a backward-compatible
addition; the Supabase schema and export format are untouched). `major` is
defensible on the grounds that users lose local data once. Pick one; do not
let it default.

### 5. The one-time reinstall

Moving from the debug key to the release key is itself a signature change.
The first release built with the new key cannot install over an existing
install. Users uninstall once, losing on-device data (memos, settings, snoozes,
custom themes, saved NFC records), and are then on the permanent key.

Consider warning users before shipping that release, not only in its notes.

### 6. Verify

1. On the first signed release, the "Verify release signature" step in CI
   prints the certificate; it must not be `CN=Android Debug`. The step fails
   the run if a debug-signed APK is about to be published.
2. Install the release APK on a device and grant full-screen alarm access.
3. Install the next release APK over it. It must update in place with no
   uninstall prompt, and the grant must survive.
4. If step 3 still forces a reinstall, the keys differ. Check that the run
   decoded the keystore (the step logs "Release keystore decoded.") and that
   the secrets match the keystore you generated.

## Things to know before the first signed release

- `assembleRelease` has never run in CI for this app. The release build type
  minifies with R8 using `app/proguard-rules.pro`. Once the follow-up PR from
  step 4 is open, every PR build will exercise the release build type, but
  runtime breakage (a serializer stripped, a reflective lookup gone) only shows
  on a device. Install the CI-built release APK and walk the main flows before
  cutting the release: sync against Supabase, an NFC tap, a memo ringing, a
  widget update.
- Secrets are not available to pull requests from forks. Such a PR still
  builds a release APK, debug-signed, and CI notes that instead of failing.
- Never commit any keystore other than the existing `app/debug.keystore`.
