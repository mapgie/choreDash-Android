#!/usr/bin/env python3
"""Consolidates changelog/unreleased/*.json fragments into CHANGELOG.md and
bumps the app version in app/build.gradle.kts.

Run via the "Prepare release" GitHub Actions workflow (workflow_dispatch).
"""

import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

from check_changelog_fragment import validate

ROOT = Path(__file__).resolve().parent
FRAGMENTS_DIR = ROOT / "changelog" / "unreleased"
CHANGELOG = ROOT / "CHANGELOG.md"
BUILD_GRADLE = ROOT / "app" / "build.gradle.kts"

SECTIONS = ["added", "changed", "fixed"]
BUMP_ORDER = {"patch": 0, "minor": 1, "major": 2}

VERSION_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?$")


def load_fragments():
    return [(path, json.loads(path.read_text())) for path in sorted(FRAGMENTS_DIR.glob("*.json"))]


def highest_bump(fragments):
    return max((data["bump"] for _, data in fragments), key=lambda bump: BUMP_ORDER[bump])


def bump_version(version_name, bump):
    match = VERSION_RE.match(version_name)
    if not match:
        raise ValueError(f"Unrecognised versionName format: {version_name!r}")

    major, minor, patch, beta = match.groups()
    major, minor, patch = int(major), int(minor), int(patch)
    is_beta = beta is not None

    if bump == "major":
        major, minor, patch = major + 1, 0, 0
    elif bump == "minor":
        minor, patch = minor + 1, 0
    else:
        patch += 1
    beta = 1 if is_beta else None

    version = f"{major}.{minor}.{patch}"
    if beta is not None:
        version += f"-beta.{beta}"
    return version


def read_current_version():
    text = BUILD_GRADLE.read_text()
    code = int(re.search(r"versionCode\s*=\s*(\d+)", text).group(1))
    name = re.search(r'versionName\s*=\s*"([^"]+)"', text).group(1)
    return text, code, name


def write_new_version(text, new_code, new_name):
    text = re.sub(r"versionCode\s*=\s*\d+", f"versionCode = {new_code}", text, count=1)
    text = re.sub(r'versionName\s*=\s*"[^"]+"', f'versionName = "{new_name}"', text, count=1)
    BUILD_GRADLE.write_text(text)


def build_entry(version, fragments):
    grouped = {section: [] for section in SECTIONS}
    for _, data in fragments:
        for section in SECTIONS:
            grouped[section].extend(data.get(section, []))

    today = datetime.now(timezone.utc).date().isoformat()
    lines = ["---", f"## [{version}] - {today}", ""]
    for section in SECTIONS:
        items = grouped[section]
        if not items:
            continue
        lines.append(f"### {section.capitalize()}")
        for item in items:
            lines.append(f"- {item}")
        lines.append("")
    return "\n".join(lines).rstrip("\n") + "\n"


def insert_entry(entry):
    text = CHANGELOG.read_text()
    marker = "\n---\n## ["
    index = text.find(marker)
    if index == -1:
        raise ValueError("Could not find an existing changelog entry to insert before")
    insertion_point = index + 1  # keep the leading newline before the marker's "---"
    CHANGELOG.write_text(text[:insertion_point] + entry + "\n" + text[insertion_point:])


def main():
    fragments = load_fragments()
    if not fragments:
        print("status=no_fragments")
        return 0

    errors = [error for path, _ in fragments for error in [validate(path)] if error]
    if errors:
        for error in errors:
            print(f"::error::{error}")
        print("Fix or remove the invalid fragment(s) above before re-running this workflow.")
        return 1

    bump = highest_bump(fragments)
    text, current_code, current_name = read_current_version()
    new_code = current_code + 1
    new_name = bump_version(current_name, bump)

    insert_entry(build_entry(new_name, fragments))
    write_new_version(text, new_code, new_name)

    for path, _ in fragments:
        path.unlink()

    print("status=ok")
    print(f"version={new_name}")
    print(
        f"Bumped {current_name} (code {current_code}) -> {new_name} (code {new_code}); "
        f"consolidated {len(fragments)} fragment(s)."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
