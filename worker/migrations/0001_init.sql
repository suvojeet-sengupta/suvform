-- SuvForm initial schema
-- Users: created/updated when a Firebase-authenticated user calls /v1/me
CREATE TABLE IF NOT EXISTS users (
  uid          TEXT PRIMARY KEY,
  email        TEXT,
  display_name TEXT,
  photo_url    TEXT,
  fcm_token    TEXT,
  created_at   INTEGER NOT NULL,
  updated_at   INTEGER NOT NULL
);

-- Forms: owned by a user, may be published (with a public_slug for the web filler)
CREATE TABLE IF NOT EXISTS forms (
  id                TEXT PRIMARY KEY,
  owner_uid         TEXT NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
  title             TEXT NOT NULL,
  description       TEXT,
  schema_json       TEXT NOT NULL,          -- fields array
  calculations_json TEXT,                   -- calculations array
  published         INTEGER NOT NULL DEFAULT 0,
  public_slug       TEXT UNIQUE,
  created_at        INTEGER NOT NULL,
  updated_at        INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_forms_owner ON forms(owner_uid);
CREATE INDEX IF NOT EXISTS idx_forms_slug ON forms(public_slug);

-- Responses: anonymous public submissions to a published form
CREATE TABLE IF NOT EXISTS responses (
  id                TEXT PRIMARY KEY,
  form_id           TEXT NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
  answers_json      TEXT NOT NULL,
  calculated_json   TEXT,
  submitter_ip_hash TEXT,
  submitted_at      INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_responses_form ON responses(form_id);
