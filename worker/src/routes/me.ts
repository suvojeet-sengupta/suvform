import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { syncProfileAndCheckAdmin } from "../db";
import { safeParse } from "../utils/helpers";
import { formatLocalized } from "../utils/time";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// POST /v1/me — sync user profile after sign-in
app.post("/", async (c) => {
  const u = c.get("user");
  console.log(`[AUTH DEBUG - SYNC] uid=${u.uid}`);

  // Sync profile and check admin status in a single efficient DB batch
  const admin = await syncProfileAndCheckAdmin(c.env.DB, u);

  console.log(`[AUTH DEBUG - RESULT] uid=${u.uid} isAdmin=${admin}`);
  return c.json({
    uid: u.uid,
    email: u.email,
    display_name: u.name,
    photo_url: u.picture,
    is_admin: admin,
  });
});

// POST /v1/me/revoke-sessions — "sign out everywhere".
// Stores a revocation cutoff (now, in seconds). The auth middleware rejects any
// token whose auth_time predates this, so all existing sessions are invalidated.
app.post("/revoke-sessions", async (c) => {
  const u = c.get("user");
  const nowSec = Math.floor(Date.now() / 1000);
  await c.env.RATE_LIMIT.put(`revoke:${u.uid}`, String(nowSec));
  return c.json({ ok: true, revoked_at: nowSec });
});

// GET /v1/me/export — export all of the user's data (GDPR "data portability").
// Returns the profile, every form (with full schema), and every response.
app.get("/export", async (c) => {
  const u = c.get("user");
  const tz = c.get("timezone");
  const now = Date.now();

  const profile = await c.env.DB.prepare(
    `SELECT uid, email, display_name, photo_url, created_at, updated_at FROM users WHERE uid = ?`,
  )
    .bind(u.uid)
    .first();

  const { results: formRows } = await c.env.DB.prepare(
    `SELECT id, title, description, schema_json, calculations_json, published, public_slug, created_at, updated_at
       FROM forms WHERE owner_uid = ? ORDER BY created_at ASC`,
  )
    .bind(u.uid)
    .all();

  const forms = [] as any[];
  for (const f of formRows as any[]) {
    const { results: respRows } = await c.env.DB.prepare(
      `SELECT id, answers_json, calculated_json, submitted_at
         FROM responses WHERE form_id = ? ORDER BY submitted_at ASC`,
    )
      .bind(f.id)
      .all();
    forms.push({
      id: f.id,
      title: f.title,
      description: f.description,
      fields: safeParse(f.schema_json, []),
      calculations: safeParse(f.calculations_json ?? "[]", []),
      published: f.published,
      public_slug: f.public_slug,
      created_at: f.created_at,
      updated_at: f.updated_at,
      responses: (respRows as any[]).map((r) => ({
        id: r.id,
        answers: safeParse(r.answers_json, {}),
        calculated: safeParse(r.calculated_json ?? "{}", {}),
        submitted_at: r.submitted_at,
        submitted_at_str: formatLocalized(r.submitted_at, tz),
      })),
    });
  }

  return c.json(
    { 
      exported_at: now, 
      exported_at_str: formatLocalized(now, tz),
      timezone: tz,
      profile, 
      forms 
    },
    200,
    { "Content-Disposition": `attachment; filename="suvform-export-${u.uid}.json"` },
  );
});

// DELETE /v1/me — delete the account and all associated data.
// forms (ON DELETE CASCADE → responses) are removed by deleting the user row.
// A revocation cutoff is also set so any outstanding tokens stop working.
app.delete("/", async (c) => {
  const u = c.get("user");
  await c.env.DB.prepare(`DELETE FROM users WHERE uid = ?`).bind(u.uid).run();
  const nowSec = Math.floor(Date.now() / 1000);
  await c.env.RATE_LIMIT.put(`revoke:${u.uid}`, String(nowSec));
  return c.json({ ok: true });
});

export default app;
