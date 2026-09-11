-- choreDash + taskDash Android — Supabase schema
--
-- The whole tables + app schema, and the single source of truth for it. Every
-- statement is idempotent (safe to run any number of times against a database
-- that already has some or all of it): tables and indexes use IF NOT EXISTS,
-- policies are dropped-then-created, constraints are dropped-then-added, grants
-- re-grant. So you can apply it two ways, and both are safe:
--   • by hand: paste the whole file into the SQL Editor (Project → SQL Editor →
--     New query) and Run;
--   • automatically: the "Deploy Supabase schema" GitHub Action
--     (.github/workflows/supabase-deploy.yml) runs it against the database on
--     every merge to main, in one transaction, when the SUPABASE_DB_URL secret
--     is set. See supabase/README.md.
--
-- If you're sharing this project with the taskDash web app, the `owners` and
-- `todos` tables are compatible with it. Applying this file does not drop or
-- edit any row; it only creates/adjusts tables, policies, constraints and grants.
--
-- This schema grants the `anon` role full read/write access (no auth), matching
-- how the app connects with the Supabase anon key. Only share your Project URL
-- and anon key with people you trust with this data.

-- ─────────────────────────────────────────────────────────────────────────
-- owners — household members. The "I am" picker in Settings reads this.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS owners (
  handle text PRIMARY KEY
);

ALTER TABLE owners ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "anon read owners"   ON owners;
CREATE POLICY "anon read owners"   ON owners FOR SELECT TO anon USING (true);
DROP POLICY IF EXISTS "anon insert owners" ON owners;
CREATE POLICY "anon insert owners" ON owners FOR INSERT TO anon WITH CHECK (true);
DROP POLICY IF EXISTS "anon update owners" ON owners;
CREATE POLICY "anon update owners" ON owners FOR UPDATE TO anon USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "anon delete owners" ON owners;
CREATE POLICY "anon delete owners" ON owners FOR DELETE TO anon USING (true);

-- Add one row per household member, e.g.:
-- INSERT INTO owners (handle) VALUES ('alex'), ('sam');

-- ─────────────────────────────────────────────────────────────────────────
-- tags — chores, one row per NFC tag/label tracked by choreDash.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tags (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tag_id        text NOT NULL UNIQUE,
  label         text NOT NULL,
  category      text,
  owner         text REFERENCES owners(handle),
  interval_days double precision,
  archived_at   timestamptz,
  created_at    timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS tags_owner_idx    ON tags(owner);
CREATE INDEX IF NOT EXISTS tags_archived_idx ON tags(archived_at);

ALTER TABLE tags ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "anon read tags"   ON tags;
CREATE POLICY "anon read tags"   ON tags FOR SELECT TO anon USING (true);
DROP POLICY IF EXISTS "anon insert tags" ON tags;
CREATE POLICY "anon insert tags" ON tags FOR INSERT TO anon WITH CHECK (true);
DROP POLICY IF EXISTS "anon update tags" ON tags;
CREATE POLICY "anon update tags" ON tags FOR UPDATE TO anon USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "anon delete tags" ON tags;
CREATE POLICY "anon delete tags" ON tags FOR DELETE TO anon USING (true);

-- ─────────────────────────────────────────────────────────────────────────
-- scans — log of NFC taps. Each scan marks the matching tag as "done now".
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS scans (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tag_id     text NOT NULL REFERENCES tags(tag_id) ON DELETE CASCADE,
  scanned_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS scans_tag_id_idx     ON scans(tag_id);
CREATE INDEX IF NOT EXISTS scans_scanned_at_idx ON scans(scanned_at);

ALTER TABLE scans ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "anon read scans"   ON scans;
CREATE POLICY "anon read scans"   ON scans FOR SELECT TO anon USING (true);
DROP POLICY IF EXISTS "anon insert scans" ON scans;
CREATE POLICY "anon insert scans" ON scans FOR INSERT TO anon WITH CHECK (true);
DROP POLICY IF EXISTS "anon update scans" ON scans;
CREATE POLICY "anon update scans" ON scans FOR UPDATE TO anon USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "anon delete scans" ON scans;
CREATE POLICY "anon delete scans" ON scans FOR DELETE TO anon USING (true);

-- ─────────────────────────────────────────────────────────────────────────
-- todos — shared task list, used by both taskDash (web) and the Android app.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS todos (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title        text NOT NULL,
  notes        text,
  category     text,
  owner        text REFERENCES owners(handle),
  priority     text NOT NULL DEFAULT 'normal'
               CHECK (priority IN ('higher', 'normal', 'lower')),
  due_date     date,
  due_period   text CHECK (due_period IN ('today', 'this_week', 'this_month', 'eventually')),
  completed_at timestamptz,
  archived_at  timestamptz,
  reminder_at  timestamptz,
  reminded     boolean DEFAULT false,
  created_at   timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS todos_completed_at_idx ON todos(completed_at);
CREATE INDEX IF NOT EXISTS todos_category_idx     ON todos(category);
CREATE INDEX IF NOT EXISTS todos_owner_idx        ON todos(owner);

ALTER TABLE todos ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "anon read todos"   ON todos;
CREATE POLICY "anon read todos"   ON todos FOR SELECT TO anon USING (true);
DROP POLICY IF EXISTS "anon insert todos" ON todos;
CREATE POLICY "anon insert todos" ON todos FOR INSERT TO anon WITH CHECK (true);
DROP POLICY IF EXISTS "anon update todos" ON todos;
CREATE POLICY "anon update todos" ON todos FOR UPDATE TO anon USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "anon delete todos" ON todos;
CREATE POLICY "anon delete todos" ON todos FOR DELETE TO anon USING (true);

-- ─────────────────────────────────────────────────────────────────────────
-- Data API exposure (table grants)
--
-- Supabase is removing the automatic privilege grant that exposes public
-- tables to the Data API (PostgREST / GraphQL / supabase-js):
--   • 2026-05-30: new projects no longer auto-expose public tables.
--   • 2026-10-30: newly created tables in existing projects no longer auto-expose.
-- Tables that already exist keep their grants past those dates, so the current
-- shared project keeps working — but a project created fresh from this file, or
-- any table added to it afterwards, needs the grants below or every request
-- through the anon key returns "permission denied". See
-- https://github.com/orgs/supabase/discussions/45329
--
-- RLS (above) decides which rows a role may touch; these GRANTs decide whether
-- the role reaches the table through the API at all. Both are required. The app
-- connects with the anon key and does full CRUD, so anon gets all four verbs,
-- matching its RLS policies. authenticated/service_role are granted too so the
-- shared project stays open to those roles as it is today. No sequence grants:
-- every id is a uuid default or a text key, so there are no sequences.
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON owners TO anon, authenticated, service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON tags   TO anon, authenticated, service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON scans  TO anon, authenticated, service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON todos  TO anon, authenticated, service_role;

-- ─────────────────────────────────────────────────────────────────────────
-- Constraint sync
--
-- CREATE TABLE IF NOT EXISTS never alters a table that already exists, so the
-- inline due_period CHECK above only lands on a brand-new database. Re-assert it
-- here so an older database (created before 'eventually' was allowed) is widened
-- to match; a no-op once it already allows all four values.
-- ─────────────────────────────────────────────────────────────────────────
ALTER TABLE todos DROP CONSTRAINT IF EXISTS todos_due_period_check;
ALTER TABLE todos ADD CONSTRAINT todos_due_period_check
  CHECK (due_period IN ('today', 'this_week', 'this_month', 'eventually'));
