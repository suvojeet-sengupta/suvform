import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { isAdmin, isOwner, listAdmins, addAdmin, removeAdmin } from "../db";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// Middleware: only allow admins
app.use("*", async (c, next) => {
  const u = c.get("user");
  const admin = await isAdmin(c.env.DB, u.uid);
  if (!admin) {
    return c.json({ error: "forbidden", message: "Admin access required" }, 403);
  }
  return next();
});

// GET /v1/admin/me — quick check if current user is admin
app.get("/me", async (c) => {
  const u = c.get("user");
  return c.json({ uid: u.uid, is_admin: true, is_owner: await isOwner(c.env.DB, u.uid) });
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
              u.email as owner_email, u.display_name as owner_name
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

// === ADMIN MANAGEMENT ===

// GET /v1/admin/admins — list current admins
app.get("/admins", async (c) => {
  const admins = await listAdmins(c.env.DB);
  return c.json({ admins });
});

// POST /v1/admin/admins — add new admin (body: { uid: "..." })
app.post("/admins", async (c) => {
  const body = await c.req.json<{ uid?: string }>().catch(() => ({} as { uid?: string }));
  const target = (body.uid || "").trim();
  if (!target) return c.json({ error: "uid_required" }, 400);

  const caller = c.get("user").uid;
  if (await isOwner(c.env.DB, target)) {
    return c.json({ error: "cannot_modify_owner" }, 403);
  }
  await addAdmin(c.env.DB, target, caller);

  return c.json({ ok: true, added: target });
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

export default app;
