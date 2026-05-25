import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { isAdmin, isOwner, listAdmins, addAdmin, removeAdmin, findUserByEmail } from "../db";
import { CONFIG } from "../config";
import { safeParse } from "../utils/helpers";
import { formatLocalized } from "../utils/time";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

type FormBody = {
  title?: string;
  description?: string;
  fields?: unknown[];
  calculations?: unknown[];
  responseLimit?: number | null;
  response_limit?: number | null;
};

function validateFormBody(b: FormBody): { ok: true; data: { title: string; description: string; fields: unknown[]; calculations: unknown[]; responseLimit: number | null } } | { ok: false; err: string } {
  const title = (b.title ?? "").toString().trim() || "Untitled form";
  if (title.length > CONFIG.TITLE_MAX_LEN) return { ok: false, err: "title_too_long" };
  const description = (b.description ?? "").toString();
  if (description.length > CONFIG.DESCRIPTION_MAX_LEN) return { ok: false, err: "description_too_long" };
  const fields = Array.isArray(b.fields) ? b.fields : [];
  if (fields.length > CONFIG.MAX_FIELDS) return { ok: false, err: "too_many_fields" };
  const calculations = Array.isArray(b.calculations) ? b.calculations : [];
  if (calculations.length > CONFIG.MAX_CALCULATIONS) return { ok: false, err: "too_many_calculations" };

  let responseLimit: number | null = null;
  const rawLimit = b.responseLimit !== undefined ? b.responseLimit : b.response_limit;
  if (rawLimit !== undefined && rawLimit !== null) {
    const n = Number(rawLimit);
    if (!Number.isInteger(n) || n < 0) {
      return { ok: false, err: "invalid_response_limit" };
    }
    responseLimit = n > 0 ? n : null;
  }
  return { ok: true, data: { title, description, fields, calculations, responseLimit } };
}

// Middleware: only allow admins
app.use("*", async (c, next) => {
  const u = c.get("user");
  const admin = await isAdmin(c.env.DB, u.uid);
  if (!admin) {
    return c.json({
      error: "admin_revoked",
      message: "Your admin access has been revoked by the owner.",
    }, 403);
  }
  return next();
});

// GET /v1/admin/me — quick check if current user is admin
app.get("/me", async (c) => {
  const u = c.get("user");
  return c.json({ uid: u.uid, is_admin: true, is_owner: await isOwner(c.env.DB, u.uid) });
});

// GET /v1/admin/dashboard — consolidated overview to reduce roundtrips
app.get("/dashboard", async (c) => {
  const db = c.env.DB;
  
  // Use batch to get everything in ONE roundtrip
  const results = await db.batch([
    db.prepare(`SELECT COUNT(*) as c FROM users`),
    db.prepare(`SELECT COUNT(*) as c FROM forms`),
    db.prepare(`SELECT COUNT(*) as c FROM responses`),
    db.prepare(`SELECT COUNT(*) as c FROM admins`),
    db.prepare(`SELECT COUNT(*) as c FROM forms WHERE published = 1`),
    // Also get the latest 5 users and forms for the "Recent activity" section
    db.prepare(`SELECT uid, email, display_name, created_at FROM users ORDER BY created_at DESC LIMIT 5`),
    db.prepare(`SELECT f.id, f.title, f.owner_uid, u.email as owner_email FROM forms f JOIN users u ON f.owner_uid = u.uid ORDER BY f.created_at DESC LIMIT 5`),
  ]);

  return c.json({
    stats: {
      total_users: (results[0].results?.[0] as any)?.c ?? 0,
      total_forms: (results[1].results?.[0] as any)?.c ?? 0,
      total_responses: (results[2].results?.[0] as any)?.c ?? 0,
      total_admins: (results[3].results?.[0] as any)?.c ?? 0,
      published_forms: (results[4].results?.[0] as any)?.c ?? 0,
    },
    recent_users: results[5].results,
    recent_forms: results[6].results,
  });
});

// GET /v1/admin/stats — high level dashboard numbers
app.get("/stats", async (c) => {
  const db = c.env.DB;

  const [users, forms, responses, admins] = await Promise.all([
    db.prepare(`SELECT COUNT(*) as c FROM users`).first<{ c: number }>(),
    db.prepare(`SELECT COUNT(*) as c FROM forms`).first<{ c: number }>(),
    db.prepare(`SELECT COUNT(*) as c FROM responses`).first<{ c: number }>(),
    db.prepare(`SELECT COUNT(*) as c FROM admins`).first<{ c: number }>(),
  ]);

  const publishedForms = await db
    .prepare(`SELECT COUNT(*) as c FROM forms WHERE published = 1`)
    .first<{ c: number }>();

  return c.json({
    total_users: users?.c ?? 0,
    total_forms: forms?.c ?? 0,
    published_forms: publishedForms?.c ?? 0,
    total_responses: responses?.c ?? 0,
    total_admins: admins?.c ?? 0,
  });
});

// GET /v1/admin/users — list all registered users (paginated)
app.get("/users", async (c) => {
  const limit = Math.min(parseInt(c.req.query("limit") || "50", 10), 200);
  const offset = parseInt(c.req.query("offset") || "0", 10);

  const { results } = await c.env.DB
    .prepare(
      `SELECT uid, email, display_name, photo_url, created_at, updated_at
       FROM users ORDER BY created_at DESC LIMIT ? OFFSET ?`
    )
    .bind(limit, offset)
    .all();

  const total = await c.env.DB.prepare(`SELECT COUNT(*) as c FROM users`).first<{ c: number }>();

  return c.json({
    users: results,
    total: total?.c ?? 0,
    limit,
    offset,
    has_more: (offset + results.length) < (total?.c ?? 0),
  });
});

// GET /v1/admin/forms — list ALL forms from every user (with owner info)
app.get("/forms", async (c) => {
  const limit = Math.min(parseInt(c.req.query("limit") || "50", 10), 200);
  const offset = parseInt(c.req.query("offset") || "0", 10);

  const { results } = await c.env.DB
    .prepare(
      `SELECT f.id, f.title, f.description, f.published, f.public_slug,
              f.created_at, f.updated_at, f.owner_uid,
              u.email as owner_email, u.display_name as owner_name,
              (SELECT COUNT(*) FROM responses WHERE form_id = f.id) as response_count
       FROM forms f
       LEFT JOIN users u ON u.uid = f.owner_uid
       ORDER BY f.created_at DESC LIMIT ? OFFSET ?`
    )
    .bind(limit, offset)
    .all();

  const total = await c.env.DB.prepare(`SELECT COUNT(*) as c FROM forms`).first<{ c: number }>();

  return c.json({
    forms: results,
    total: total?.c ?? 0,
    limit,
    offset,
    has_more: (offset + results.length) < (total?.c ?? 0),
  });
});

// GET /v1/admin/responses — all responses across all forms (heavy, use filters)
app.get("/responses", async (c) => {
  const limit = Math.min(parseInt(c.req.query("limit") || "30", 10), 100);
  const offset = parseInt(c.req.query("offset") || "0", 10);
  const formId = c.req.query("form_id");

  let query = `
    SELECT r.id, r.form_id, r.answers_json, r.calculated_json, r.submitted_at,
           f.title as form_title, f.owner_uid
    FROM responses r
    JOIN forms f ON f.id = r.form_id
  `;
  const params: any[] = [];

  if (formId) {
    query += ` WHERE r.form_id = ?`;
    params.push(formId);
  }

  query += ` ORDER BY r.submitted_at DESC LIMIT ? OFFSET ?`;
  params.push(limit, offset);

  const { results } = await c.env.DB.prepare(query).bind(...params).all();

  // Count is expensive without form filter, so approximate
  const totalRow = formId
    ? await c.env.DB.prepare(`SELECT COUNT(*) as c FROM responses WHERE form_id = ?`).bind(formId).first<{ c: number }>()
    : await c.env.DB.prepare(`SELECT COUNT(*) as c FROM responses`).first<{ c: number }>();

  return c.json({
    responses: results,
    total: totalRow?.c ?? 0,
    limit,
    offset,
    has_more: (offset + results.length) < (totalRow?.c ?? 0),
  });
});

// DELETE /v1/admin/forms/:id — admin delete of any user's form.
// Explicitly removes responses first so it works regardless of FK cascade.
// The client gates this behind a warning + type-to-confirm step.
app.delete("/forms/:id", async (c) => {
  const id = c.req.param("id");
  const exists = await c.env.DB.prepare(`SELECT 1 FROM forms WHERE id = ? LIMIT 1`).bind(id).first();
  if (!exists) return c.json({ error: "not_found" }, 404);

  await c.env.DB.batch([
    c.env.DB.prepare(`DELETE FROM responses WHERE form_id = ?`).bind(id),
    c.env.DB.prepare(`DELETE FROM forms WHERE id = ?`).bind(id),
  ]);

  return c.json({ ok: true, deleted: id });
});

// DELETE /v1/admin/users/:uid — admin delete of an entire user account and all
// their data (forms + responses). Protected: cannot delete the owner or yourself.
// The client gates this behind a warning + type-to-confirm (email) step.
app.delete("/users/:uid", async (c) => {
  const uid = c.req.param("uid");
  const caller = c.get("user").uid;

  if (uid === caller) {
    return c.json({ error: "cannot_delete_self", message: "You can't delete your own account from the admin panel." }, 403);
  }
  if (await isOwner(c.env.DB, uid)) {
    return c.json({ error: "cannot_delete_owner", message: "The owner account cannot be deleted." }, 403);
  }

  const user = await c.env.DB.prepare(`SELECT uid FROM users WHERE uid = ? LIMIT 1`).bind(uid).first();
  if (!user) return c.json({ error: "not_found" }, 404);

  // Remove responses → forms → admin row → user, in dependency order.
  await c.env.DB.batch([
    c.env.DB.prepare(`DELETE FROM responses WHERE form_id IN (SELECT id FROM forms WHERE owner_uid = ?)`).bind(uid),
    c.env.DB.prepare(`DELETE FROM forms WHERE owner_uid = ?`).bind(uid),
    c.env.DB.prepare(`DELETE FROM admins WHERE uid = ?`).bind(uid),
    c.env.DB.prepare(`DELETE FROM users WHERE uid = ?`).bind(uid),
  ]);

  // Invalidate any outstanding tokens for the deleted user.
  const nowSec = Math.floor(Date.now() / 1000);
  await c.env.RATE_LIMIT.put(`revoke:${uid}`, String(nowSec));

  return c.json({ ok: true, deleted: uid });
});

// === ADMIN MANAGEMENT ===

// GET /v1/admin/admins — list current admins
app.get("/admins", async (c) => {
  const admins = await listAdmins(c.env.DB);
  return c.json({ admins });
});

// POST /v1/admin/admins — add new admin by email (preferred) or uid.
// Email must belong to a user who has already signed in.
app.post("/admins", async (c) => {
  const body = await c.req.json<{ uid?: string; email?: string }>().catch(() => ({} as { uid?: string; email?: string }));
  const email = (body.email || "").trim();
  let target = (body.uid || "").trim();

  if (!target && email) {
    const user = await findUserByEmail(c.env.DB, email);
    if (!user) {
      return c.json({
        error: "user_not_found",
        message: "No registered user with that email. They must sign in to the app at least once before they can be made an admin.",
      }, 404);
    }
    target = user.uid;
  }

  if (!target) return c.json({ error: "uid_or_email_required" }, 400);

  const caller = c.get("user").uid;
  if (await isOwner(c.env.DB, target)) {
    return c.json({ error: "cannot_modify_owner" }, 403);
  }
  await addAdmin(c.env.DB, target, caller);

  return c.json({ ok: true, added: target });
});

// GET /v1/admin/users/:uid — single user profile with form/response counts
app.get("/users/:uid", async (c) => {
  const uid = c.req.param("uid");
  const tz = c.get("timezone");

  const user = await c.env.DB
    .prepare(`SELECT uid, email, display_name, photo_url, created_at, updated_at FROM users WHERE uid = ?`)
    .bind(uid)
    .first<{ uid: string; email: string | null; display_name: string | null; photo_url: string | null; created_at: number; updated_at: number }>();
  if (!user) return c.json({ error: "not_found" }, 404);

  const [formCount, publishedCount, responseCount, adminRow] = await Promise.all([
    c.env.DB.prepare(`SELECT COUNT(*) as c FROM forms WHERE owner_uid = ?`).bind(uid).first<{ c: number }>(),
    c.env.DB.prepare(`SELECT COUNT(*) as c FROM forms WHERE owner_uid = ? AND published = 1`).bind(uid).first<{ c: number }>(),
    c.env.DB.prepare(`SELECT COUNT(*) as c FROM responses r JOIN forms f ON f.id = r.form_id WHERE f.owner_uid = ?`).bind(uid).first<{ c: number }>(),
    c.env.DB.prepare(`SELECT role FROM admins WHERE uid = ? LIMIT 1`).bind(uid).first<{ role: string }>(),
  ]);

  return c.json({
    ...user,
    created_at_str: formatLocalized(user.created_at, tz),
    is_admin: !!adminRow,
    role: adminRow?.role ?? null,
    total_forms: formCount?.c ?? 0,
    published_forms: publishedCount?.c ?? 0,
    total_responses: responseCount?.c ?? 0,
  });
});

// GET /v1/admin/users/:uid/forms — forms owned by a specific user (paginated)
app.get("/users/:uid/forms", async (c) => {
  const uid = c.req.param("uid");
  const limit = Math.min(parseInt(c.req.query("limit") || "50", 10), 200);
  const offset = Math.max(parseInt(c.req.query("offset") || "0", 10), 0);
  const tz = c.get("timezone");

  const { results } = await c.env.DB
    .prepare(
      `SELECT f.id, f.title, f.description, f.published, f.public_slug, f.created_at, f.updated_at, f.owner_uid,
              (SELECT COUNT(*) FROM responses WHERE form_id = f.id) as response_count
       FROM forms f WHERE f.owner_uid = ? ORDER BY f.updated_at DESC LIMIT ? OFFSET ?`,
    )
    .bind(uid, limit, offset)
    .all();

  const total = await c.env.DB.prepare(`SELECT COUNT(*) as c FROM forms WHERE owner_uid = ?`).bind(uid).first<{ c: number }>();

  return c.json({
    forms: (results as any[]).map((f) => ({ ...f, updated_at_str: formatLocalized(f.updated_at, tz) })),
    total: total?.c ?? 0,
    limit,
    offset,
    has_more: (offset + results.length) < (total?.c ?? 0),
  });
});

// GET /v1/admin/forms/:id — full detail of any form (fields + calculations)
app.get("/forms/:id", async (c) => {
  const id = c.req.param("id");
  const tz = c.get("timezone");

  const row = await c.env.DB
    .prepare(
      `SELECT f.id, f.title, f.description, f.schema_json, f.calculations_json,
              f.published, f.public_slug, f.response_limit, f.created_at, f.updated_at, f.owner_uid,
              u.email as owner_email, u.display_name as owner_name
       FROM forms f LEFT JOIN users u ON u.uid = f.owner_uid
       WHERE f.id = ?`,
    )
    .bind(id)
    .first<any>();
  if (!row) return c.json({ error: "not_found" }, 404);

  const responseCount = await c.env.DB
    .prepare(`SELECT COUNT(*) as c FROM responses WHERE form_id = ?`)
    .bind(id)
    .first<{ c: number }>();

  return c.json({
    id: row.id,
    title: row.title,
    description: row.description,
    fields: safeParse(row.schema_json || "[]", []),
    calculations: safeParse(row.calculations_json || "[]", []),
    published: row.published,
    public_slug: row.public_slug,
    response_limit: row.response_limit,
    created_at: row.created_at,
    updated_at: row.updated_at,
    updated_at_str: formatLocalized(row.updated_at, tz),
    owner_uid: row.owner_uid,
    owner_email: row.owner_email,
    owner_name: row.owner_name,
    total_responses: responseCount?.c ?? 0,
  });
});

// GET /v1/admin/forms/:id/responses — responses for any form (paginated).
// Mirrors the owner-facing /v1/forms/:id/responses shape so the client can
// reuse the same response models and rendering.
app.get("/forms/:id/responses", async (c) => {
  const id = c.req.param("id");
  const exists = await c.env.DB.prepare(`SELECT 1 FROM forms WHERE id = ? LIMIT 1`).bind(id).first();
  if (!exists) return c.json({ error: "not_found" }, 404);

  const limit = Math.min(
    Math.max(parseInt(c.req.query("limit") || String(CONFIG.RESPONSES_PAGE_DEFAULT)) || CONFIG.RESPONSES_PAGE_DEFAULT, 1),
    CONFIG.RESPONSES_PAGE_MAX,
  );
  const offset = Math.max(parseInt(c.req.query("offset") || "0") || 0, 0);

  const { results } = await c.env.DB
    .prepare(
      `SELECT id, answers_json, calculated_json, version_id, submitted_at
       FROM responses WHERE form_id = ? ORDER BY submitted_at DESC LIMIT ? OFFSET ?`,
    )
    .bind(id, limit, offset)
    .all();

  const countRow = await c.env.DB
    .prepare(`SELECT COUNT(*) as count FROM responses WHERE form_id = ?`)
    .bind(id)
    .first<{ count: number }>();

  const tz = c.get("timezone");

  return c.json({
    responses: (results as any[]).map((r) => ({
      id: r.id,
      submitted_at: r.submitted_at,
      submitted_at_str: formatLocalized(r.submitted_at, tz),
      version_id: r.version_id,
      answers: safeParse(r.answers_json, {}),
      calculated: safeParse(r.calculated_json ?? "{}", {}),
    })),
    total_count: countRow?.count ?? 0,
    has_more: (offset + results.length) < (countRow?.count ?? 0),
  });
});

// PUT /v1/admin/forms/:id — admin edit of any user's form.
// The client surfaces a warning + explicit confirm before calling this.
app.put("/forms/:id", async (c) => {
  const id = c.req.param("id");
  const existing = await c.env.DB.prepare(`SELECT schema_json, calculations_json, current_version_id FROM forms WHERE id = ?`)
    .bind(id)
    .first<{ schema_json: string; calculations_json: string; current_version_id: string }>();
  if (!existing) return c.json({ error: "not_found" }, 404);

  const body = await c.req.json<FormBody>().catch(() => ({} as FormBody));
  const v = validateFormBody(body);
  if (!v.ok) return c.json({ error: v.err }, 400);

  const schemaStr = JSON.stringify(v.data.fields);
  const calcStr = JSON.stringify(v.data.calculations);
  const limit = v.data.responseLimit ?? null;
  const now = Date.now();

  const isSchemaChanged = schemaStr !== existing.schema_json || calcStr !== existing.calculations_json;
  let newVersionId = existing.current_version_id;

  const queries = [];
  if (isSchemaChanged) {
    newVersionId = crypto.randomUUID();
    queries.push(
      c.env.DB.prepare(
        `INSERT INTO form_versions (id, form_id, schema_json, calculations_json, created_at)
         VALUES (?, ?, ?, ?, ?)`
      ).bind(newVersionId, id, schemaStr, calcStr, now)
    );
  }

  queries.push(
    c.env.DB.prepare(
      `UPDATE forms SET title = ?, description = ?, schema_json = ?, calculations_json = ?, response_limit = ?, current_version_id = ?, updated_at = ?
         WHERE id = ?`
    ).bind(
      v.data.title,
      v.data.description,
      schemaStr,
      calcStr,
      limit,
      newVersionId,
      now,
      id,
    )
  );

  await c.env.DB.batch(queries);

  const tz = c.get("timezone");
  return c.json({ id, updated_at: now, updated_at_str: formatLocalized(now, tz), version_id: newVersionId, response_limit: limit });
});

// DELETE /v1/admin/admins/:uid — remove an admin (cannot remove last admin)
app.delete("/admins/:uid", async (c) => {
  const target = c.req.param("uid");
  if (!target) return c.json({ error: "uid_required" }, 400);

  if (await isOwner(c.env.DB, target)) {
    return c.json({ error: "cannot_remove_owner" }, 403);
  }

  const result = await removeAdmin(c.env.DB, target);
  if (!result.removed) {
    return c.json({ error: result.reason || "cannot_remove" }, 409);
  }
  return c.json({ ok: true, removed: target });
});

// DELETE /v1/admin/forms/:id/responses — delete all or selected responses for ANY form
app.delete("/forms/:id/responses", async (c) => {
  const id = c.req.param("id");
  const exists = await c.env.DB.prepare(`SELECT 1 FROM forms WHERE id = ? LIMIT 1`).bind(id).first();
  if (!exists) return c.json({ error: "not_found" }, 404);

  const body = await c.req.json<{ ids?: string[]; all?: boolean }>().catch(() => ({}));
  const b = body as { ids?: string[]; all?: boolean };
  if (b.all === true) {
    await c.env.DB.prepare(`DELETE FROM responses WHERE form_id = ?`).bind(id).run();
    return c.json({ ok: true, deleted: "all" });
  }

  if (Array.isArray(b.ids) && b.ids.length > 0) {
    const placeholders = b.ids.map(() => "?").join(",");
    await c.env.DB.prepare(`DELETE FROM responses WHERE form_id = ? AND id IN (${placeholders})`)
      .bind(id, ...b.ids)
      .run();
    return c.json({ ok: true, deleted_count: b.ids.length });
  }

  return c.json({ error: "missing_ids_or_all_flag" }, 400);
});

// DELETE /v1/admin/forms/:id/responses/:responseId — delete a single response for ANY form
app.delete("/forms/:id/responses/:responseId", async (c) => {
  const id = c.req.param("id");
  const responseId = c.req.param("responseId");
  
  const res = await c.env.DB.prepare(`DELETE FROM responses WHERE id = ? AND form_id = ?`)
    .bind(responseId, id)
    .run();
  
  if (res.meta.changes === 0) return c.json({ error: "not_found" }, 404);
  return c.json({ ok: true });
});

export default app;
