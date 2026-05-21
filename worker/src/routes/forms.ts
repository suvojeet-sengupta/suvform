import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { ensureUserExists } from "../db";
import { safeParse, makeSlug } from "../utils/helpers";
import { summarizeResponsesWithGemini } from "../gemini";
import { CONFIG } from "../config";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// GET /v1/forms — list current user's forms
app.get("/", async (c) => {
  const u = c.get("user");
  // No ensureUserExists here: a SELECT needs no FK row, and the user is already
  // created on sign-in (POST /v1/me) and on form creation. Saves a write per list.
  const { results } = await c.env.DB.prepare(
    `SELECT id, title, description, published, public_slug, created_at, updated_at
       FROM forms WHERE owner_uid = ? ORDER BY updated_at DESC LIMIT ?`,
  )
    .bind(u.uid, CONFIG.FORMS_LIST_LIMIT)
    .all();
  return c.json({ forms: results });
});

type FormBody = {
  title?: string;
  description?: string;
  fields?: unknown[];
  calculations?: unknown[];
};

function validateFormBody(b: FormBody): { ok: true; data: Required<FormBody> } | { ok: false; err: string } {
  const title = (b.title ?? "").toString().trim() || "Untitled form";
  if (title.length > CONFIG.TITLE_MAX_LEN) return { ok: false, err: "title_too_long" };
  const description = (b.description ?? "").toString();
  if (description.length > CONFIG.DESCRIPTION_MAX_LEN) return { ok: false, err: "description_too_long" };
  const fields = Array.isArray(b.fields) ? b.fields : [];
  if (fields.length > CONFIG.MAX_FIELDS) return { ok: false, err: "too_many_fields" };
  const calculations = Array.isArray(b.calculations) ? b.calculations : [];
  if (calculations.length > CONFIG.MAX_CALCULATIONS) return { ok: false, err: "too_many_calculations" };
  return { ok: true, data: { title, description, fields, calculations } };
}

// POST /v1/forms — create a new form (full payload)
app.post("/", async (c) => {
  const u = c.get("user");
  await ensureUserExists(c.env.DB, u);

  const body = await c.req.json<FormBody>().catch(() => ({} as FormBody));
  const v = validateFormBody(body);
  if (!v.ok) return c.json({ error: v.err }, 400);
  const id = crypto.randomUUID();
  const now = Date.now();
  await c.env.DB.prepare(
    `INSERT INTO forms (id, owner_uid, title, description, schema_json, calculations_json, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
  )
    .bind(
      id,
      u.uid,
      v.data.title,
      v.data.description,
      JSON.stringify(v.data.fields),
      JSON.stringify(v.data.calculations),
      now,
      now,
    )
    .run();
  return c.json(
    {
      id,
      title: v.data.title,
      description: v.data.description,
      published: 0,
      public_slug: null,
      created_at: now,
      updated_at: now,
    },
    201,
  );
});

// GET /v1/forms/:id
app.get("/:id", async (c) => {
  const u = c.get("user");
  const row = await c.env.DB.prepare(
    `SELECT id, title, description, schema_json, calculations_json,
            published, public_slug, created_at, updated_at, owner_uid
       FROM forms WHERE id = ?`,
  )
    .bind(c.req.param("id"))
    .first<{
      id: string;
      title: string;
      description: string;
      schema_json: string;
      calculations_json: string;
      published: number;
      public_slug: string | null;
      created_at: number;
      updated_at: number;
      owner_uid: string;
    }>();
  if (!row) return c.json({ error: "not_found" }, 404);
  if (row.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);
  return c.json({
    id: row.id,
    title: row.title,
    description: row.description,
    fields: JSON.parse(row.schema_json || "[]"),
    calculations: JSON.parse(row.calculations_json || "[]"),
    published: row.published,
    public_slug: row.public_slug,
    created_at: row.created_at,
    updated_at: row.updated_at,
  });
});

// PUT /v1/forms/:id
app.put("/:id", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const owner = await c.env.DB.prepare(`SELECT owner_uid FROM forms WHERE id = ?`)
    .bind(id)
    .first<{ owner_uid: string }>();
  if (!owner) return c.json({ error: "not_found" }, 404);
  if (owner.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);

  const body = await c.req.json<FormBody>().catch(() => ({} as FormBody));
  const v = validateFormBody(body);
  if (!v.ok) return c.json({ error: v.err }, 400);
  const now = Date.now();
  await c.env.DB.prepare(
    `UPDATE forms SET title = ?, description = ?, schema_json = ?, calculations_json = ?, updated_at = ?
       WHERE id = ?`,
  )
    .bind(
      v.data.title,
      v.data.description,
      JSON.stringify(v.data.fields),
      JSON.stringify(v.data.calculations),
      now,
      id,
    )
    .run();
  return c.json({ id, updated_at: now });
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
    `SELECT id, answers_json, calculated_json, submitted_at
       FROM responses WHERE form_id = ? ORDER BY submitted_at DESC LIMIT ? OFFSET ?`,
  )
    .bind(id, limit, offset)
    .all();
    
  const countRow = await c.env.DB.prepare(
    `SELECT COUNT(*) as count FROM responses WHERE form_id = ?`
  ).bind(id).first<{ count: number }>();

  return c.json({
    responses: (results as any[]).map((r) => ({
      id: r.id,
      submitted_at: r.submitted_at,
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

export default app;
