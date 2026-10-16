-- choreDash + taskDash Android — Supabase schema
--
-- The whole tables + app schema, and the single source of truth for it. Every
-- statement is idempotent (safe to run any number of times against a database
-- that already has some or all of it): tables and indexes use IF NOT EXISTS,
-- policies are created only when missing, a CHECK is replaced only when it is
-- missing or out of date, grants re-grant. No statement drops a table, a
-- column, a row or a policy. Any section can also be run on its own. So you
-- can apply it two ways, and both are safe:
--   • by hand: paste the whole file into the SQL Editor (Project → SQL Editor →
--     New query) and Run;
--   • automatically: the "Deploy Supabase schema" GitHub Action
--     (.github/workflows/supabase-deploy.yml) runs it against the database on
--     every merge to main, in one transaction, when the SUPABASE_DB_URL secret
--     is set. See supabase/README.md.
--
-- If you're sharing this project with the taskDash web app, the `owners` and
-- `todos` tables are compatible with it. Applying this file does not drop or
-- edit any row, with two exceptions: the run that adds `tags.nfc_id` fills it
-- in once from `tag_id`, and the run that creates `nfc_tags` fills it once
-- from `tags.nfc_id` (see there). Otherwise it only creates/adjusts tables,
-- policies, constraints and grants.
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

-- Created only when missing, so a re-run drops nothing (LESSONS #69).
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'owners' AND policyname = 'anon read owners') THEN
    CREATE POLICY "anon read owners" ON owners FOR SELECT TO anon USING (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'owners' AND policyname = 'anon insert owners') THEN
    CREATE POLICY "anon insert owners" ON owners FOR INSERT TO anon WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'owners' AND policyname = 'anon update owners') THEN
    CREATE POLICY "anon update owners" ON owners FOR UPDATE TO anon USING (true) WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'owners' AND policyname = 'anon delete owners') THEN
    CREATE POLICY "anon delete owners" ON owners FOR DELETE TO anon USING (true);
  END IF;
END $$;

-- Add one row per household member, e.g.:
-- INSERT INTO owners (handle) VALUES ('alex'), ('sam');

-- ─────────────────────────────────────────────────────────────────────────
-- tags — chores, one row per chore tracked by choreDash.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tags (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  -- The chore's key: scans point at it, and it never changes. Older chores used
  -- the id on their NFC sticker here; new ones get a random one.
  tag_id        text NOT NULL UNIQUE,
  -- Legacy: the one NFC tag a chore had before nfc_tags (below) held them.
  -- Still read by older app versions; this one reads nfc_tags instead.
  nfc_id        text UNIQUE,
  label         text NOT NULL,
  category      text,
  owner         text REFERENCES owners(handle),
  interval_days double precision,
  -- Optional fixed due date. The chore falls due on it and every repeat after;
  -- the app works out the next one from the latest scan, so a scan never edits it.
  due_date      date,
  -- What interval_days counts in: NULL means days. interval_days keeps the
  -- approximate length (a year is 365) so readers that ignore this column still work.
  repeat_unit   text CHECK (repeat_unit IN ('week', 'month', 'year')),
  archived_at   timestamptz,
  created_at    timestamptz DEFAULT now()
);

-- CREATE TABLE IF NOT EXISTS never alters an existing table, so add the
-- due-date columns to a database created before they existed. The CHECK is
-- re-asserted in "Constraint sync" below.
ALTER TABLE tags ADD COLUMN IF NOT EXISTS due_date    date;
ALTER TABLE tags ADD COLUMN IF NOT EXISTS repeat_unit text;

-- nfc_id arrived after tag_id had been doing both jobs. It is added, and filled
-- in, only on the run that creates it: every chore whose tag_id someone typed
-- (a sticker's id) keeps that id as its tag, while an app-made random id (a
-- UUID, minted when no tag was given) means the chore has no tag. Later runs
-- leave the column alone, so a tag the app has unlinked stays unlinked.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'tags' AND column_name = 'nfc_id'
  ) THEN
    ALTER TABLE tags ADD COLUMN nfc_id text UNIQUE;
    UPDATE tags SET nfc_id = tag_id
    WHERE tag_id !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS tags_owner_idx    ON tags(owner);
CREATE INDEX IF NOT EXISTS tags_archived_idx ON tags(archived_at);

ALTER TABLE tags ENABLE ROW LEVEL SECURITY;

-- Created only when missing, so a re-run drops nothing (LESSONS #69).
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'tags' AND policyname = 'anon read tags') THEN
    CREATE POLICY "anon read tags" ON tags FOR SELECT TO anon USING (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'tags' AND policyname = 'anon insert tags') THEN
    CREATE POLICY "anon insert tags" ON tags FOR INSERT TO anon WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'tags' AND policyname = 'anon update tags') THEN
    CREATE POLICY "anon update tags" ON tags FOR UPDATE TO anon USING (true) WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'tags' AND policyname = 'anon delete tags') THEN
    CREATE POLICY "anon delete tags" ON tags FOR DELETE TO anon USING (true);
  END IF;
END $$;

-- ─────────────────────────────────────────────────────────────────────────
-- nfc_tags — every NFC tag the app has saved, one row per physical tag.
--
-- A tag is saved under a name and attached to a chore later, or never; a chore
-- can have any number of tags (one by the front door, one by the back). An
-- unattached tag has chore_tag_id NULL. Deleting a chore frees its tags rather
-- than forgetting them. Tags on private chores live on the phone, never here.
--
-- This replaces tags.nfc_id, which held one tag per chore. That column stays,
-- untouched, for app versions that still read it. The table is created, and
-- filled from tags.nfc_id (or tag_id, on a database that never got nfc_id),
-- only on the run that creates it, so a tag the app detaches later is never
-- re-attached by a re-run.
-- ─────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'nfc_tags'
  ) THEN
    CREATE TABLE IF NOT EXISTS nfc_tags (
      -- The id the tag answers with: its written id or its hardware UID.
      nfc_id       text PRIMARY KEY,
      name         text NOT NULL,
      -- The key of the chore a tap logs, or NULL while the tag is only saved.
      chore_tag_id text REFERENCES tags(tag_id) ON DELETE SET NULL,
      created_at   timestamptz DEFAULT now()
    );
    -- Run on its own, this section can meet a tags table that never got
    -- nfc_id (the block above adds it). Then tag_id is still the sticker's
    -- id, and an app-made UUID means the chore has no tag.
    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'tags' AND column_name = 'nfc_id'
    ) THEN
      INSERT INTO nfc_tags (nfc_id, name, chore_tag_id)
        SELECT nfc_id, label, tag_id FROM tags WHERE nfc_id IS NOT NULL
        ON CONFLICT (nfc_id) DO NOTHING;
    ELSE
      INSERT INTO nfc_tags (nfc_id, name, chore_tag_id)
        SELECT tag_id, label, tag_id FROM tags
        WHERE tag_id !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        ON CONFLICT (nfc_id) DO NOTHING;
    END IF;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS nfc_tags_chore_idx ON nfc_tags(chore_tag_id);

ALTER TABLE nfc_tags ENABLE ROW LEVEL SECURITY;

-- Created only when missing, so a re-run drops nothing (LESSONS #69).
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'nfc_tags' AND policyname = 'anon read nfc_tags') THEN
    CREATE POLICY "anon read nfc_tags" ON nfc_tags FOR SELECT TO anon USING (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'nfc_tags' AND policyname = 'anon insert nfc_tags') THEN
    CREATE POLICY "anon insert nfc_tags" ON nfc_tags FOR INSERT TO anon WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'nfc_tags' AND policyname = 'anon update nfc_tags') THEN
    CREATE POLICY "anon update nfc_tags" ON nfc_tags FOR UPDATE TO anon USING (true) WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'nfc_tags' AND policyname = 'anon delete nfc_tags') THEN
    CREATE POLICY "anon delete nfc_tags" ON nfc_tags FOR DELETE TO anon USING (true);
  END IF;
END $$;

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

-- Created only when missing, so a re-run drops nothing (LESSONS #69).
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'scans' AND policyname = 'anon read scans') THEN
    CREATE POLICY "anon read scans" ON scans FOR SELECT TO anon USING (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'scans' AND policyname = 'anon insert scans') THEN
    CREATE POLICY "anon insert scans" ON scans FOR INSERT TO anon WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'scans' AND policyname = 'anon update scans') THEN
    CREATE POLICY "anon update scans" ON scans FOR UPDATE TO anon USING (true) WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'scans' AND policyname = 'anon delete scans') THEN
    CREATE POLICY "anon delete scans" ON scans FOR DELETE TO anon USING (true);
  END IF;
END $$;

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

-- Created only when missing, so a re-run drops nothing (LESSONS #69).
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'todos' AND policyname = 'anon read todos') THEN
    CREATE POLICY "anon read todos" ON todos FOR SELECT TO anon USING (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'todos' AND policyname = 'anon insert todos') THEN
    CREATE POLICY "anon insert todos" ON todos FOR INSERT TO anon WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'todos' AND policyname = 'anon update todos') THEN
    CREATE POLICY "anon update todos" ON todos FOR UPDATE TO anon USING (true) WITH CHECK (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'todos' AND policyname = 'anon delete todos') THEN
    CREATE POLICY "anon delete todos" ON todos FOR DELETE TO anon USING (true);
  END IF;
END $$;

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
GRANT SELECT, INSERT, UPDATE, DELETE ON nfc_tags TO anon, authenticated, service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON todos  TO anon, authenticated, service_role;

-- ─────────────────────────────────────────────────────────────────────────
-- Constraint sync
--
-- CREATE TABLE IF NOT EXISTS never alters a table that already exists, so the
-- inline CHECKs above only land on a brand-new database. Re-assert them here so
-- an older database (created before 'eventually' was allowed, or before
-- repeat_unit existed) is brought in line; a no-op once it already matches.
-- ─────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
  -- Replaced only when missing or lacking the newest value the app writes, so
  -- a database already in line is left alone. When a value is added to a CHECK
  -- below, change the LIKE to look for it.
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'todos_due_period_check' AND pg_get_constraintdef(oid) LIKE '%''eventually''%'
  ) THEN
    ALTER TABLE todos DROP CONSTRAINT IF EXISTS todos_due_period_check;
    ALTER TABLE todos ADD CONSTRAINT todos_due_period_check
      CHECK (due_period IN ('today', 'this_week', 'this_month', 'eventually'));
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'tags_repeat_unit_check' AND pg_get_constraintdef(oid) LIKE '%''year''%'
  ) THEN
    ALTER TABLE tags DROP CONSTRAINT IF EXISTS tags_repeat_unit_check;
    ALTER TABLE tags ADD CONSTRAINT tags_repeat_unit_check
      CHECK (repeat_unit IN ('week', 'month', 'year'));
  END IF;
END $$;
