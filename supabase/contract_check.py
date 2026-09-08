#!/usr/bin/env python3
"""Live app-to-database contract check for the shared Supabase project.

Reads the values allowed by supabase/schema.sql (the source of truth) and
confirms a Supabase project actually accepts each one through its REST API, by
inserting a throwaway `todos` row per value and deleting it again. It catches
the other half of schema drift that SchemaSyncTest can't see: a schema.sql that
has never been applied (or was applied to a different project), so the live
CHECK constraint still rejects a value the app now sends.

Point it at a THROWAWAY project (never production): it writes and deletes rows.
Set two environment variables (in CI, as repository secrets):

    SUPABASE_TEST_URL       e.g. https://abcd1234.supabase.co
    SUPABASE_TEST_ANON_KEY  that project's anon/publishable key

With either unset the check prints a notice and exits 0, so it stays dormant
until someone opts in.
"""

import json
import os
import re
import sys
import urllib.error
import urllib.request
import uuid

HERE = os.path.dirname(os.path.abspath(__file__))
SCHEMA = os.path.join(HERE, "schema.sql")
MARKER = f"ci-contract-{uuid.uuid4().hex[:8]}"


def allowed_values(column: str, schema: str) -> list[str]:
    m = re.search(column + r"\s+IN\s*\(([^)]*)\)", schema, re.IGNORECASE)
    if not m:
        sys.exit(f"schema.sql has no CHECK ({column} IN (...)) to check against")
    return re.findall(r"'([^']*)'", m.group(1))


def request(method: str, url: str, key: str, body: dict | None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("apikey", key)
    req.add_header("Authorization", f"Bearer {key}")
    req.add_header("Content-Type", "application/json")
    req.add_header("Prefer", "return=representation" if method == "POST" else "return=minimal")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def main() -> int:
    url = os.environ.get("SUPABASE_TEST_URL", "").rstrip("/")
    key = os.environ.get("SUPABASE_TEST_ANON_KEY", "")
    if not url or not key:
        print("SUPABASE_TEST_URL / SUPABASE_TEST_ANON_KEY not set; skipping live contract check.")
        return 0

    schema = open(SCHEMA).read()
    endpoint = f"{url}/rest/v1/todos"
    cases: list[tuple[str, str]] = []
    for v in allowed_values("due_period", schema):
        cases.append(("due_period", v))
    for v in allowed_values("priority", schema):
        cases.append(("priority", v))

    failures = []
    for column, value in cases:
        status, payload = request("POST", endpoint, key, {"title": MARKER, column: value})
        if status not in (200, 201):
            failures.append(f"{column}={value!r} rejected (HTTP {status}): {payload.strip()[:200]}")
            continue
        try:
            rows = json.loads(payload)
            for row in rows:
                request("DELETE", f"{endpoint}?id=eq.{row['id']}", key, None)
        except (ValueError, KeyError):
            pass
        print(f"ok: {column}={value}")

    # Defensive sweep for anything a crash left behind.
    request("DELETE", f"{endpoint}?title=eq.{MARKER}", key, None)

    if failures:
        print("\nContract check FAILED. The live database rejects values the app can send:")
        for f in failures:
            print(f"  - {f}")
        print("\nApply supabase/schema.sql (including its migration block) to this project.")
        return 1
    print(f"\nAll {len(cases)} values accepted by {url}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
