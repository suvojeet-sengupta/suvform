import { FirebaseUser } from "../auth";

/**
 * Ensures the user exists in the database for FK constraints.
 * Does NOT overwrite existing profile details.
 */
export async function ensureUserExists(db: D1Database, u: FirebaseUser) {
  const now = Date.now();
  await db
    .prepare(
      `INSERT INTO users (uid, email, created_at, updated_at)
       VALUES (?, ?, ?, ?)
       ON CONFLICT(uid) DO NOTHING`,
    )
    .bind(u.uid, u.email ?? null, now, now)
    .run();
}

/**
 * Full profile sync. Usually called once after sign-in.
 */
export async function upsertUserProfile(db: D1Database, u: FirebaseUser) {
  const now = Date.now();
  await db
    .prepare(
      `INSERT INTO users (uid, email, display_name, photo_url, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?)
       ON CONFLICT(uid) DO UPDATE SET
         email = excluded.email,
         display_name = excluded.display_name,
         photo_url = excluded.photo_url,
         updated_at = excluded.updated_at`,
    )
    .bind(u.uid, u.email ?? null, u.name ?? null, u.picture ?? null, now, now)
    .run();
}
