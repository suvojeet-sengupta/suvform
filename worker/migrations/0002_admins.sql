-- Admin users table
-- Only users present in this table can access /v1/admin/* routes and see Admin panel in app.
-- The first admin must be inserted manually via wrangler (one-time setup).

CREATE TABLE IF NOT EXISTS admins (
  uid       TEXT PRIMARY KEY,
  added_at  INTEGER NOT NULL,
  added_by  TEXT NOT NULL,
  role      TEXT NOT NULL DEFAULT 'admin'
);

CREATE INDEX IF NOT EXISTS idx_admins_added_by ON admins(added_by);
