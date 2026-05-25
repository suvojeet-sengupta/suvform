-- Migration 0005: Add response_limit column to forms
-- NULL or 0 (or negative) means unlimited responses.
-- Positive integer = hard cap on total submissions.

ALTER TABLE forms ADD COLUMN response_limit INTEGER;

-- Optional: create an index if we ever query forms by remaining capacity (future optimization)
-- CREATE INDEX IF NOT EXISTS idx_forms_response_limit ON forms(response_limit);
