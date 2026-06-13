# Unreleased changelog fragments

Every PR that changes app code must add at least one new file here, named
`changelog/unreleased/<short-slug>.json`, instead of editing `CHANGELOG.md` or
bumping the version in `app/build.gradle.kts` directly. New files never
conflict with each other, so this avoids merge conflicts between PRs.

## Format

```json
{
  "bump": "patch",
  "added": ["..."],
  "changed": ["..."],
  "fixed": ["..."]
}
```

- `bump` is required: `"patch"`, `"minor"`, or `"major"` (see the versioning
  policy in `CHANGELOG.md`).
- `added` / `changed` / `fixed` are optional lists of one-line, user-facing
  descriptions. Include only the sections that apply, but at least one must
  be a non-empty list.

CI validates every fragment in this directory on each PR (not just newly
added ones), so an edit that breaks an existing fragment will fail that PR's
checks rather than surfacing later at release time.

## Releasing

Run the "Prepare release" workflow (`workflow_dispatch`). It consolidates all
fragments in this directory into a single new `CHANGELOG.md` entry, bumps
`versionCode` and `versionName` in `app/build.gradle.kts` accordingly, removes
the consumed fragments, and opens a PR for review.
