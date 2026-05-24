-- Create form_versions table
CREATE TABLE IF NOT EXISTS form_versions (
  id                TEXT PRIMARY KEY,
  form_id           TEXT NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
  schema_json       TEXT NOT NULL,
  calculations_json TEXT,
  created_at        INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_form_versions_form ON form_versions(form_id);

-- Add versioning columns to existing tables
ALTER TABLE forms ADD COLUMN current_version_id TEXT REFERENCES form_versions(id);
ALTER TABLE responses ADD COLUMN version_id TEXT REFERENCES form_versions(id);

-- Initial Migration: Create a version for every existing form
-- SQLite's random hex generation for IDs
INSERT INTO form_versions (id, form_id, schema_json, calculations_json, created_at)
SELECT lower(hex(randomblob(16))), id, schema_json, calculations_json, created_at FROM forms;

-- Link existing forms to their initial version
UPDATE forms SET current_version_id = (
  SELECT id FROM form_versions WHERE form_versions.form_id = forms.id LIMIT 1
);

-- Link existing responses to the form's current version
UPDATE responses SET version_id = (
  SELECT current_version_id FROM forms WHERE forms.id = responses.form_id
);
