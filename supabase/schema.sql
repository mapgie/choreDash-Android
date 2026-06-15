-- choreDash + taskDash Android — Supabase schema
--
-- Run this entire file once in your Supabase project's SQL Editor
-- (Project → SQL Editor → New query) to create the tables the Android
-- app expects: owners, tags, scans, todos.
--
-- If you're sharing this project with the taskDash web app, the `owners`
-- and `todos` tables below are compatible with it — do not create them
-- twice.
--
-- This schema grants the `anon` role full read/write access (no auth),
-- matching how the app connects with the Supabase anon key. Only share
-- your Project URL and anon key with people you trust with this data.

-- ─────────────────────────────────────────────────────────────────────────
-- owners — household members. The "I am" picker in Settings reads this.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS owners (
  handle text PRIMARY KEY
);

ALTER TABLE owners ENABLE ROW LEVEL SECURITY;

CREATE POLICY "anon read owners"   ON owners FOR SELECT TO anon USING (true);
CREATE POLICY "anon insert owners" ON owners FOR INSERT TO anon WITH CHECK (true);
CREATE POLICY "anon update owners" ON owners FOR UPDATE TO anon USING (true) WITH CHECK (true);
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

CREATE POLICY "anon read tags"   ON tags FOR SELECT TO anon USING (true);
CREATE POLICY "anon insert tags" ON tags FOR INSERT TO anon WITH CHECK (true);
CREATE POLICY "anon update tags" ON tags FOR UPDATE TO anon USING (true) WITH CHECK (true);
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

CREATE POLICY "anon read scans"   ON scans FOR SELECT TO anon USING (true);
CREATE POLICY "anon insert scans" ON scans FOR INSERT TO anon WITH CHECK (true);
CREATE POLICY "anon update scans" ON scans FOR UPDATE TO anon USING (true) WITH CHECK (true);
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
  due_period   text CHECK (due_period IN ('today', 'this_week', 'this_month')),
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

CREATE POLICY "anon read todos"   ON todos FOR SELECT TO anon USING (true);
CREATE POLICY "anon insert todos" ON todos FOR INSERT TO anon WITH CHECK (true);
CREATE POLICY "anon update todos" ON todos FOR UPDATE TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon delete todos" ON todos FOR DELETE TO anon USING (true);
