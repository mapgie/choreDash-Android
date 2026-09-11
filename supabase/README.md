# Supabase schema

`schema.sql` is the single source of truth for the shared Supabase project the
Android app and the taskDash web app talk to. It creates the tables (`owners`,
`tags`, `scans`, `todos`), their policies, grants and constraints. Every statement
is **idempotent** (tables/indexes use `IF NOT EXISTS`, policies are dropped then
created, constraints dropped then added, grants re-granted), so applying the whole
file to a database that already has some or all of it is safe and never touches row
data.

## Applying it by hand

Paste the file into the project's SQL Editor (Project → SQL Editor → New query) and
Run. Safe to re-run any time. This is the recommended path for a Free-plan project.

## Applying it automatically (GitHub Action)

`.github/workflows/supabase-deploy.yml` applies `schema.sql` to the database on
every merge to `main`, in a single transaction, with `psql`. No Supabase CLI, no
Branching, no paid plan. It is **dormant** until one repository secret is set:

- `SUPABASE_DB_URL` — the project's **Session pooler** connection URI (Dashboard →
  Connect → Session pooler; it is IPv4, which GitHub runners need, unlike the direct
  connection). It contains the database password, so it lives only as an Actions
  secret (Settings → Secrets and variables → Actions → New repository secret).

With the secret unset the job prints a notice and passes, so merging it changes
nothing until you opt in. The workflow runs only on push to `main`, so the secret
is never exposed to pull requests. Watch the first run (Actions tab): because the
file is idempotent it is a no-op on objects that already exist, and only applies
what is new.

## Data API exposure (grants)

The app reaches every table through the Data API (PostgREST) with the anon key.
Supabase is removing the automatic grant that exposes `public` tables to that API:
new projects lose it on 2026-05-30, and new tables in existing projects on
2026-10-30 ([discussion](https://github.com/orgs/supabase/discussions/45329)).
Tables that already exist keep their grants, so the live project is safe, but a
project created fresh from `schema.sql`, or any table added later, needs an
explicit `GRANT` or requests return "permission denied". `schema.sql` therefore
grants each table to `anon` / `authenticated` / `service_role` in its "Data API
exposure" block, and `SchemaSyncTest` fails if a `CREATE TABLE` ever lacks a grant
to `anon`. RLS still governs which rows a role sees; the grant governs whether the
table is reachable at all. Both are required.

## Keeping the app and the database in sync

The app writes fixed sets of strings to constrained columns (`todos.due_period`,
`todos.priority`). If the app learns a new value and the column's CHECK constraint
doesn't, Supabase rejects the insert at runtime, e.g.

> new row for relation "todos" violates check constraint "todos_due_period_check"

Two CI guards catch that drift:

1. **`SchemaSyncTest`** (a JVM unit test in the normal "Unit tests" job, no setup).
   Asserts every value the app can write, drawn from the `DuePeriod` and
   `TaskPriority` enums, appears in the matching CHECK in `schema.sql`. Adding an
   enum value without widening the schema fails this test on the PR that adds it.

2. **`schema-contract.yml`** (the `contract_check.py` script). Reads the allowed
   values from `schema.sql` and inserts one throwaway `todos` row per value against
   a real project's REST API, then deletes them, proving the live database has the
   schema applied. It is **dormant** until two repository secrets are set:

   - `SUPABASE_TEST_URL` — a **throwaway** project's URL (never production; the
     check writes and deletes rows).
   - `SUPABASE_TEST_ANON_KEY` — that project's anon/publishable key.

   Create a scratch Supabase project, run `schema.sql` in it, add the two secrets
   (Settings → Secrets and variables → Actions), and the job starts enforcing that
   the schema is actually applied. Until then it prints a notice and passes.

Together: guard 1 ties the app to `schema.sql`; guard 2 ties `schema.sql` to a live
database. Applying a merged `schema.sql` change to production is handled by the
deploy Action above once `SUPABASE_DB_URL` is set, or by hand in the SQL Editor.
