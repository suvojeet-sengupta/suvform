import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { ensureUserExists } from "../db";
import { safeParse, makeSlug } from "../utils/helpers";
import { summarizeResponsesWithGemini } from "../gemini";
import { CONFIG } from "../config";
import { formatLocalized } from "../utils/time";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// GET /v1/forms/dashboard — High-performance consolidated home view
app.get("/dashboard", async (c) => {
  const u = c.get("user");
  const tz = c.get("timezone");
  const db = c.env.DB;

  // Use D1 Batching to get everything in ONE database roundtrip.
  // 1. Get recent forms
  // 2. Get total forms count
  // 3. Get total responses count across all forms
  // 4. Get total published forms count
  const results = await db.batch([
    db.prepare(
      `SELECT f.id, f.title, f.description, f.published, f.public_slug, f.created_at, f.updated_at, f.response_limit,
              (SELECT COUNT(*) FROM responses WHERE form_id = f.id) as response_count
         FROM forms f WHERE f.owner_uid = ? ORDER BY f.updated_at DESC LIMIT ?`,
    ).bind(u.uid, CONFIG.FORMS_LIST_LIMIT),
    db.prepare(`SELECT COUNT(*) as c FROM forms WHERE owner_uid = ?`).bind(u.uid),
    db.prepare(
      `SELECT COUNT(*) as c FROM responses r JOIN forms f ON r.form_id = f.id WHERE f.owner_uid = ?`
    ).bind(u.uid),
    db.prepare(`SELECT COUNT(*) as c FROM forms WHERE owner_uid = ? AND published = 1`).bind(u.uid)
  ]);

  const forms = (results[0].results as any[]).map(f => ({
    ...f,
    updated_at_str: formatLocalized(f.updated_at, tz)
  }));

  return c.json({
    stats: {
      total_forms: (results[1].results?.[0] as any)?.c ?? 0,
      total_responses: (results[2].results?.[0] as any)?.c ?? 0,
      published_forms: (results[3].results?.[0] as any)?.c ?? 0,
    },
    forms,
  });
});

type FormBody = {
  title?: string;
  description?: string;
  fields?: unknown[];
  calculations?: unknown[];
  theme?: unknown;
  responseLimit?: number | null;
  response_limit?: number | null;
};

function validateFormBody(b: FormBody): { ok: true; data: { title: string; description: string; fields: unknown[]; calculations: unknown[]; theme: unknown; responseLimit: number | null } } | { ok: false; err: string } {
  const title = (b.title ?? "").toString().trim() || "Untitled form";
  if (title.length > CONFIG.TITLE_MAX_LEN) return { ok: false, err: "title_too_long" };
  const description = (b.description ?? "").toString();
  if (description.length > CONFIG.DESCRIPTION_MAX_LEN) return { ok: false, err: "description_too_long" };
  const fields = Array.isArray(b.fields) ? b.fields : [];
  if (fields.length > CONFIG.MAX_FIELDS) return { ok: false, err: "too_many_fields" };
  const calculations = Array.isArray(b.calculations) ? b.calculations : [];
  if (calculations.length > CONFIG.MAX_CALCULATIONS) return { ok: false, err: "too_many_calculations" };
  const theme = b.theme ?? null;

  let responseLimit: number | null = null;
  const rawLimit = b.responseLimit !== undefined ? b.responseLimit : b.response_limit;
  if (rawLimit !== undefined && rawLimit !== null) {
    const n = Number(rawLimit);
    if (!Number.isInteger(n) || n < 0) {
      return { ok: false, err: "invalid_response_limit" };
    }
    responseLimit = n > 0 ? n : null;
  }
  return { ok: true, data: { title, description, fields, calculations, theme, responseLimit } };
}

// POST /v1/forms — create a new form (full payload)
app.post("/", async (c) => {
  const u = c.get("user");
  await ensureUserExists(c.env.DB, u);

  const body = await c.req.json<FormBody>().catch(() => ({} as FormBody));
  const v = validateFormBody(body);
  if (!v.ok) return c.json({ error: v.err }, 400);
  const id = crypto.randomUUID();
  const versionId = crypto.randomUUID();
  const now = Date.now();
  
  const schemaStr = JSON.stringify(v.data.fields);
  const calcStr = JSON.stringify(v.data.calculations);
  const themeStr = v.data.theme ? JSON.stringify(v.data.theme) : null;
  const limit = v.data.responseLimit ?? null;

  await c.env.DB.batch([
    // 1. Create the form row first (current_version_id remains NULL)
    c.env.DB.prepare(
      `INSERT INTO forms (id, owner_uid, title, description, schema_json, calculations_json, theme_json, response_limit, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      id,
      u.uid,
      v.data.title,
      v.data.description,
      schemaStr,
      calcStr,
      themeStr,
      limit,
      now,
      now,
    ),
    // 2. Create the initial version row (references the form ID)
    c.env.DB.prepare(
      `INSERT INTO form_versions (id, form_id, schema_json, calculations_json, theme_json, created_at)
       VALUES (?, ?, ?, ?, ?, ?)`
    ).bind(versionId, id, schemaStr, calcStr, themeStr, now),
    // 3. Link the form back to its initial version
    c.env.DB.prepare(
      `UPDATE forms SET current_version_id = ? WHERE id = ?`
    ).bind(versionId, id)
  ]);

  const tz = c.get("timezone");
  return c.json(
    {
      id,
      title: v.data.title,
      description: v.data.description,
      published: 0,
      public_slug: null,
      current_version_id: versionId,
      response_limit: limit,
      created_at: now,
      updated_at: now,
      updated_at_str: formatLocalized(now, tz),
    },
    201,
  );
});

// GET /v1/forms/:id
app.get("/:id", async (c) => {
  const u = c.get("user");
  const row = await c.env.DB.prepare(
    `SELECT id, title, description, schema_json, calculations_json, theme_json,
            published, public_slug, response_limit, created_at, updated_at, owner_uid
       FROM forms WHERE id = ?`,
  )
    .bind(c.req.param("id"))
    .first<{
      id: string;
      title: string;
      description: string;
      schema_json: string;
      calculations_json: string;
      theme_json: string | null;
      published: number;
      public_slug: string | null;
      response_limit: number | null;
      created_at: number;
      updated_at: number;
      owner_uid: string;
    }>();
  if (!row) return c.json({ error: "not_found" }, 404);
  if (row.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);

  const tz = c.get("timezone");

  return c.json({
    id: row.id,
    title: row.title,
    description: row.description,
    fields: JSON.parse(row.schema_json || "[]"),
    calculations: JSON.parse(row.calculations_json || "[]"),
    theme: safeParse(row.theme_json || "null", null),
    published: row.published,
    public_slug: row.public_slug,
    response_limit: row.response_limit,
    created_at: row.created_at,
    updated_at: row.updated_at,
    updated_at_str: formatLocalized(row.updated_at, tz),
  });
});

// PUT /v1/forms/:id
app.put("/:id", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const existing = await c.env.DB.prepare(`SELECT owner_uid, schema_json, calculations_json, theme_json, current_version_id, response_limit FROM forms WHERE id = ?`)
    .bind(id)
    .first<{ owner_uid: string; schema_json: string; calculations_json: string; theme_json: string | null; current_version_id: string; response_limit: number | null }>();
  if (!existing) return c.json({ error: "not_found" }, 404);
  if (existing.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);

  const body = await c.req.json<FormBody>().catch(() => ({} as FormBody));
  const v = validateFormBody(body);
  if (!v.ok) return c.json({ error: v.err }, 400);

  const schemaStr = JSON.stringify(v.data.fields);
  const calcStr = JSON.stringify(v.data.calculations);
  const themeStr = v.data.theme ? JSON.stringify(v.data.theme) : null;
  const limit = v.data.responseLimit ?? null;
  const now = Date.now();

  const isSchemaChanged = schemaStr !== existing.schema_json || calcStr !== existing.calculations_json || themeStr !== existing.theme_json;
  let newVersionId = existing.current_version_id;

  const queries = [];
  if (isSchemaChanged) {
    newVersionId = crypto.randomUUID();
    queries.push(
      c.env.DB.prepare(
        `INSERT INTO form_versions (id, form_id, schema_json, calculations_json, theme_json, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`
      ).bind(newVersionId, id, schemaStr, calcStr, themeStr, now)
    );
  }

  queries.push(
    c.env.DB.prepare(
      `UPDATE forms SET title = ?, description = ?, schema_json = ?, calculations_json = ?, theme_json = ?, response_limit = ?, current_version_id = ?, updated_at = ?
         WHERE id = ?`
    ).bind(
      v.data.title,
      v.data.description,
      schemaStr,
      calcStr,
      themeStr,
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

// DELETE /v1/forms/:id
app.delete("/:id", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const owner = await c.env.DB.prepare(`SELECT owner_uid FROM forms WHERE id = ?`)
    .bind(id)
    .first<{ owner_uid: string }>();
  if (!owner) return c.json({ error: "not_found" }, 404);
  if (owner.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);
  await c.env.DB.prepare(`DELETE FROM forms WHERE id = ?`).bind(id).run();
  return c.json({ ok: true });
});

// POST /v1/forms/:id/publish
app.post("/:id/publish", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const row = await c.env.DB.prepare(
    `SELECT owner_uid, public_slug FROM forms WHERE id = ?`,
  )
    .bind(id)
    .first<{ owner_uid: string; public_slug: string | null }>();
  if (!row) return c.json({ error: "not_found" }, 404);
  if (row.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);

  let slug = row.public_slug;
  if (!slug) {
    for (let attempt = 0; attempt < CONFIG.SLUG_MAX_ATTEMPTS; attempt++) {
      const candidate = makeSlug();
      const exists = await c.env.DB.prepare(`SELECT 1 FROM forms WHERE public_slug = ? LIMIT 1`).bind(candidate).first();
      if (!exists) { slug = candidate; break; }
    }
    if (!slug) return c.json({ error: "slug_collision" }, 500);
  }
  const now = Date.now();
  await c.env.DB.prepare(`UPDATE forms SET published = 1, public_slug = ?, updated_at = ? WHERE id = ?`)
    .bind(slug, now, id)
    .run();
  
  const publicBaseUrl = "https://suvforms.suvojeetsengupta.in";
  const shareUrl = `${publicBaseUrl}/f/${slug}`;
  return c.json({ slug, url: shareUrl, published: 1 });
});

// POST /v1/forms/:id/unpublish
app.post("/:id/unpublish", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const row = await c.env.DB.prepare(`SELECT owner_uid FROM forms WHERE id = ?`)
    .bind(id)
    .first<{ owner_uid: string }>();
  if (!row) return c.json({ error: "not_found" }, 404);
  if (row.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);
  await c.env.DB.prepare(`UPDATE forms SET published = 0, updated_at = ? WHERE id = ?`)
    .bind(Date.now(), id)
    .run();
  return c.json({ published: 0 });
});

// GET /v1/forms/:id/responses
app.get("/:id/responses", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const owner = await c.env.DB.prepare(`SELECT owner_uid FROM forms WHERE id = ?`)
    .bind(id)
    .first<{ owner_uid: string }>();
  if (!owner) return c.json({ error: "not_found" }, 404);
  if (owner.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);

  const limit = Math.min(
    Math.max(parseInt(c.req.query("limit") || String(CONFIG.RESPONSES_PAGE_DEFAULT)) || CONFIG.RESPONSES_PAGE_DEFAULT, 1),
    CONFIG.RESPONSES_PAGE_MAX,
  );
  const offset = Math.max(parseInt(c.req.query("offset") || "0") || 0, 0);

  const { results } = await c.env.DB.prepare(
    `SELECT id, answers_json, calculated_json, version_id, submitted_at
       FROM responses WHERE form_id = ? ORDER BY submitted_at DESC LIMIT ? OFFSET ?`,
  )
    .bind(id, limit, offset)
    .all();
    
  const countRow = await c.env.DB.prepare(
    `SELECT COUNT(*) as count FROM responses WHERE form_id = ?`
  ).bind(id).first<{ count: number }>();

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
    has_more: (offset + results.length) < (countRow?.count ?? 0)
  });
});

// POST /v1/forms/:id/insights
app.post("/:id/insights", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const form = await c.env.DB.prepare(
    `SELECT owner_uid, title, schema_json FROM forms WHERE id = ?`,
  )
    .bind(id)
    .first<{ owner_uid: string; title: string; schema_json: string }>();
  if (!form) return c.json({ error: "not_found" }, 404);
  if (form.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);

  const { results } = await c.env.DB.prepare(
    `SELECT answers_json, calculated_json FROM responses
       WHERE form_id = ? ORDER BY submitted_at DESC LIMIT ?`,
  )
    .bind(id, CONFIG.INSIGHTS_SAMPLE_SIZE)
    .all();
  const responses = (results as any[]).map((r) => ({
    ...safeParse(r.answers_json, {}),
    _calc: safeParse(r.calculated_json ?? "{}", {}),
  }));
  if (responses.length === 0) {
    return c.json({ summary: "No responses yet — share your form to start collecting!" });
  }

  const fields = safeParse(form.schema_json, []) as any[];
  // Prefer the user's own key (sent from the app) and fall back to the server secret.
  const apiKey = (c.req.header("X-Gemini-Key") ?? "").trim() || c.env.GEMINI_API_KEY;
  if (!apiKey) return c.json({ error: "no_gemini_key" }, 400);
  try {
    const summary = await summarizeResponsesWithGemini(
      apiKey,
      form.title,
      fields,
      responses,
    );
    return c.json({ summary, response_count: responses.length });
  } catch (e) {
    return c.json({ error: "insights_failed", detail: (e as Error).message }, 502);
  }
});

// DELETE /v1/forms/:id/responses — delete all or selected responses
app.delete("/:id/responses", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const owner = await c.env.DB.prepare(`SELECT owner_uid FROM forms WHERE id = ?`)
    .bind(id)
    .first<{ owner_uid: string }>();
  if (!owner) return c.json({ error: "not_found" }, 404);
  if (owner.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);

  const body = await c.req.json<{ ids?: string[]; all?: boolean }>().catch(() => ({}));
  const b = body as { ids?: string[]; all?: boolean };
  if (b.all === true) {
    await c.env.DB.prepare(`DELETE FROM responses WHERE form_id = ?`).bind(id).run();
    return c.json({ ok: true, deleted: "all" });
  }

  if (Array.isArray(b.ids) && b.ids.length > 0) {
    // Minimize DB writes by using a single query with IN clause for bulk delete.
    // D1 supports up to 100 parameters usually, but we can also just use multiple ? if needed.
    // For simplicity and safety with many IDs, we can use batching or a structured IN clause.
    const placeholders = b.ids.map(() => "?").join(",");
    await c.env.DB.prepare(`DELETE FROM responses WHERE form_id = ? AND id IN (${placeholders})`)
      .bind(id, ...b.ids)
      .run();
    return c.json({ ok: true, deleted_count: b.ids.length });
  }

  return c.json({ error: "missing_ids_or_all_flag" }, 400);
});

// DELETE /v1/forms/:id/responses/:responseId — delete a single response
app.delete("/:id/responses/:responseId", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const responseId = c.req.param("responseId");
  const owner = await c.env.DB.prepare(`SELECT owner_uid FROM forms WHERE id = ?`)
    .bind(id)
    .first<{ owner_uid: string }>();
  if (!owner) return c.json({ error: "not_found" }, 404);
  if (owner.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);

  const res = await c.env.DB.prepare(`DELETE FROM responses WHERE id = ? AND form_id = ?`)
    .bind(responseId, id)
    .run();
  
  if (res.meta.changes === 0) return c.json({ error: "not_found" }, 404);
  return c.json({ ok: true });
});

export default app;
