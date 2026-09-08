# Supabase schema

`schema.sql` is the single source of truth for the shared Supabase project the
Android app and the taskDash web app talk to. It creates the tables (`owners`,
`tags`, `scans`, `todos`) and, at the bottom, holds a **Migrations** section:
`ALTER` statements existing projects run once, since `CREATE TABLE IF NOT EXISTS`
never changes a table that already exists.

## Applying it

Paste the file into the project's SQL Editor (Project → SQL Editor → New query)
and run it. Every statement is safe to run more than once. After editing
`schema.sql`, run the new statements against every project that uses it (there is
only one shared project today).

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
database. The one gap is applying a merged `schema.sql` change to the production
project, which is still a manual SQL Editor step (see "Applying it").
