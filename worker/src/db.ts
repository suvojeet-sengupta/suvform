import { FirebaseUser } from "./auth";

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
 * Returns true if the given uid exists in the admins table.
 * This is the single source of truth for admin privileges.
 */
export async function isAdmin(db: D1Database, uid: string): Promise<boolean> {
  const row = await db
    .prepare(`SELECT 1 FROM admins WHERE uid = ? LIMIT 1`)
    .bind(uid)
    .first<{ 1: number }>();
  return !!row;
}

/**
 * Looks up a user by email (case-insensitive). Returns null if no such user
 * has signed in yet — admin rights can only attach to a real account.
 */
export async function findUserByEmail(db: D1Database, email: string) {
  return await db
    .prepare(
      `SELECT uid, email, display_name, photo_url, created_at
       FROM users WHERE lower(email) = lower(?) LIMIT 1`,
    )
    .bind(email)
    .first<{ uid: string; email: string | null; display_name: string | null; photo_url: string | null; created_at: number }>();
}

/**
 * Returns list of all admins with basic info.
 */
export async function listAdmins(db: D1Database) {
  const { results } = await db
    .prepare(
      `SELECT a.uid, a.added_at, a.added_by, a.role, u.email, u.display_name
       FROM admins a
       LEFT JOIN users u ON u.uid = a.uid
       ORDER BY a.added_at ASC`
    )
    .all();
  return results;
}

/**
 * Adds a new admin (only callable by existing admin).
 * Prevents duplicate inserts.
 */
export async function addAdmin(db: D1Database, targetUid: string, addedBy: string) {
  const now = Date.now();
  await db
    .prepare(
      `INSERT INTO admins (uid, added_at, added_by)
       VALUES (?, ?, ?)
       ON CONFLICT(uid) DO NOTHING`
    )
    .bind(targetUid, now, addedBy)
    .run();
}

/**
 * Adds or upgrades the owner account. Only one owner should exist.
 */
export async function upsertOwner(db: D1Database, ownerUid: string) {
  const now = Date.now();
  await db
    .prepare(
      `INSERT INTO admins (uid, added_at, added_by, role)
       VALUES (?, ?, ?, 'owner')
       ON CONFLICT(uid) DO UPDATE SET role = 'owner'`
    )
    .bind(ownerUid, now, ownerUid)
    .run();
}

export async function getAdminRecord(db: D1Database, uid: string): Promise<{ uid: string; role: string } | null> {
  return await db
    .prepare(`SELECT uid, role FROM admins WHERE uid = ? LIMIT 1`)
    .bind(uid)
    .first<{ uid: string; role: string }>();
}

export async function isOwner(db: D1Database, uid: string): Promise<boolean> {
  const row = await getAdminRecord(db, uid);
  return row?.role === 'owner';
}

/**
 * Removes an admin.
 * Safety: cannot remove the last remaining admin.
 */
/**
 * Full profile sync and admin check in a single batch.
 * Usually called once after sign-in to ensure the user exists and to check their privileges.
 */
export async function syncProfileAndCheckAdmin(db: D1Database, u: FirebaseUser): Promise<boolean> {
  const now = Date.now();
  const results = await db.batch([
    db.prepare(
      `INSERT INTO users (uid, email, display_name, photo_url, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?)
       ON CONFLICT(uid) DO UPDATE SET
         email = excluded.email,
         display_name = excluded.display_name,
         photo_url = excluded.photo_url,
         updated_at = excluded.updated_at`,
    ).bind(u.uid, u.email ?? null, u.name ?? null, u.picture ?? null, now, now),
    db.prepare(`SELECT 1 FROM admins WHERE uid = ? LIMIT 1`).bind(u.uid)
  ]);

  const adminRow = results[1].results?.[0];
  return !!adminRow;
}

export async function removeAdmin(db: D1Database, targetUid: string): Promise<{ removed: boolean; reason?: string }> {
  const target = await getAdminRecord(db, targetUid);
  if (target?.role === 'owner') {
    return { removed: false, reason: 'cannot_remove_owner' };
  }
  const count = await db.prepare(`SELECT COUNT(*) as c FROM admins`).first<{ c: number }>();
  if ((count?.c ?? 0) <= 1) {
    return { removed: false, reason: "cannot_remove_last_admin" };
  }
  const res = await db.prepare(`DELETE FROM admins WHERE uid = ?`).bind(targetUid).run();
  return { removed: (res.meta.changes ?? 0) > 0 };
}
