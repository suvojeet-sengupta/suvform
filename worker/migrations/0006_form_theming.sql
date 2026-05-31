-- Add theme_json column to forms and form_versions
ALTER TABLE forms ADD COLUMN theme_json TEXT;
ALTER TABLE form_versions ADD COLUMN theme_json TEXT;
