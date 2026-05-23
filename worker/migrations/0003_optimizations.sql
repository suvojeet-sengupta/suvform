-- Migration: 0003_optimizations.sql
-- Description: Add missing indexes for performance and optimize common queries.

-- Index for global response sorting and insights
CREATE INDEX IF NOT EXISTS idx_responses_submitted ON responses(submitted_at DESC);

-- Composite index for per-form response listing (very common in app)
CREATE INDEX IF NOT EXISTS idx_responses_form_submitted ON responses(form_id, submitted_at DESC);

-- Index for listing user's forms by recency
CREATE INDEX IF NOT EXISTS idx_forms_updated ON forms(updated_at DESC);
