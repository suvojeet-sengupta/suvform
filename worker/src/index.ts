import { Hono } from "hono";
import { cors } from "hono/cors";
import { verifyFirebaseIdToken, type FirebaseUser } from "./auth";
import { generateFormWithGemini, summarizeResponsesWithGemini } from "./gemini";
import { publicFormHtml } from "./publicForm";

type Bindings = {
  DB: D1Database;
  RATE_LIMIT: KVNamespace;
  FIREBASE_PROJECT_ID: string;
  GEMINI_API_KEY: string;
};

type Variables = {
  user: FirebaseUser;
};

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

app.use("*", cors({ origin: "*", allowMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"] }));

// Health check (unauthenticated)
app.get("/", (c) => c.json({ ok: true, service: "suvform-api", version: 1 }));

// Auth middleware — runs on every /v1/* route except /v1/public/*
app.use("/v1/*", async (c, next) => {
  if (c.req.path.startsWith("/v1/public/")) return next();
  const authHeader = c.req.header("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return c.json({ error: "missing_token" }, 401);
  }
  const token = authHeader.slice(7);
  try {
    const user = await verifyFirebaseIdToken(token, c.env.FIREBASE_PROJECT_ID);
    c.set("user", user);
  } catch (e) {
    return c.json({ error: "invalid_token", detail: (e as Error).message }, 401);
  }
  return next();
});

// POST /v1/me — upsert user profile after sign-in
app.post("/v1/me", async (c) => {
  const u = c.get("user");
  const now = Date.now();
  await c.env.DB.prepare(
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
  return c.json({ uid: u.uid, email: u.email, display_name: u.name, photo_url: u.picture });
});

// GET /v1/forms — list current user's forms
app.get("/v1/forms", async (c) => {
  const u = c.get("user");
  const { results } = await c.env.DB.prepare(
    `SELECT id, title, description, published, public_slug, created_at, updated_at
       FROM forms WHERE owner_uid = ? ORDER BY updated_at DESC LIMIT 100`,
  )
    .bind(u.uid)
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
  if (title.length > 200) return { ok: false, err: "title_too_long" };
  const description = (b.description ?? "").toString();
  if (description.length > 2000) return { ok: false, err: "description_too_long" };
  const fields = Array.isArray(b.fields) ? b.fields : [];
  if (fields.length > 200) return { ok: false, err: "too_many_fields" };
  const calculations = Array.isArray(b.calculations) ? b.calculations : [];
  if (calculations.length > 50) return { ok: false, err: "too_many_calculations" };
  return { ok: true, data: { title, description, fields, calculations } };
}

// POST /v1/forms — create a new form (full payload)
app.post("/v1/forms", async (c) => {
  const u = c.get("user");
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

// GET /v1/forms/:id — get one form (owner-only)
app.get("/v1/forms/:id", async (c) => {
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

// PUT /v1/forms/:id — update an existing form (owner-only)
app.put("/v1/forms/:id", async (c) => {
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

// DELETE /v1/forms/:id — delete (owner-only). Cascades to responses.
app.delete("/v1/forms/:id", async (c) => {
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

// POST /v1/ai/generate-form — Gemini proxy with simple per-user daily quota.
app.post("/v1/ai/generate-form", async (c) => {
  const u = c.get("user");
  const body = await c.req
    .json<{ prompt?: string; locale?: "en" | "hi" }>()
    .catch(() => ({} as { prompt?: string; locale?: "en" | "hi" }));
  const prompt = (body.prompt ?? "").trim();
  if (prompt.length < 3) return c.json({ error: "prompt_too_short" }, 400);
  if (prompt.length > 2000) return c.json({ error: "prompt_too_long" }, 400);

  // Per-user soft quota: 50 generations / 24h via KV. Cheap and approximate.
  const day = new Date().toISOString().slice(0, 10); // YYYY-MM-DD UTC
  const quotaKey = `quota:${u.uid}:${day}`;
  const used = parseInt((await c.env.RATE_LIMIT.get(quotaKey)) ?? "0", 10);
  if (used >= 50) return c.json({ error: "quota_exceeded", limit: 50 }, 429);

  try {
    const form = await generateFormWithGemini(c.env.GEMINI_API_KEY, prompt, body.locale ?? "en");
    // Increment quota (24h TTL).
    await c.env.RATE_LIMIT.put(quotaKey, String(used + 1), { expirationTtl: 24 * 60 * 60 });
    return c.json(form);
  } catch (e) {
    return c.json({ error: "generation_failed", detail: (e as Error).message }, 502);
  }
});

// ---------- Publish / unpublish ----------

function makeSlug(len = 8): string {
  const alphabet = "23456789abcdefghjkmnpqrstuvwxyz"; // omit confusing chars (0/o, 1/l, i)
  let s = "";
  const bytes = new Uint8Array(len);
  crypto.getRandomValues(bytes);
  for (let i = 0; i < len; i++) s += alphabet[bytes[i] % alphabet.length];
  return s;
}

// POST /v1/forms/:id/publish — generate a public slug and mark published
app.post("/v1/forms/:id/publish", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const row = await c.env.DB.prepare(
    `SELECT owner_uid, public_slug FROM forms WHERE id = ?`,
  )
    .bind(id)
    .first<{ owner_uid: string; public_slug: string | null }>();
  if (!row) return c.json({ error: "not_found" }, 404);
  if (row.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);

  // Re-use existing slug if already published.
  let slug = row.public_slug;
  if (!slug) {
    // Try a few times in the unlikely event of a collision.
    for (let attempt = 0; attempt < 5; attempt++) {
      const candidate = makeSlug();
      const exists = await c.env.DB.prepare(
        `SELECT 1 FROM forms WHERE public_slug = ? LIMIT 1`,
      )
        .bind(candidate)
        .first();
      if (!exists) { slug = candidate; break; }
    }
    if (!slug) return c.json({ error: "slug_collision" }, 500);
  }
  const now = Date.now();
  await c.env.DB.prepare(
    `UPDATE forms SET published = 1, public_slug = ?, updated_at = ? WHERE id = ?`,
  )
    .bind(slug, now, id)
    .run();
  const url = new URL(c.req.url);
  const shareUrl = `${url.origin}/f/${slug}`;
  return c.json({ slug, url: shareUrl, published: 1 });
});

// POST /v1/forms/:id/unpublish
app.post("/v1/forms/:id/unpublish", async (c) => {
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

// ---------- Responses (owner-only) ----------

// GET /v1/forms/:id/responses — list responses for owner's form
app.get("/v1/forms/:id/responses", async (c) => {
  const u = c.get("user");
  const id = c.req.param("id");
  const owner = await c.env.DB.prepare(`SELECT owner_uid FROM forms WHERE id = ?`)
    .bind(id)
    .first<{ owner_uid: string }>();
  if (!owner) return c.json({ error: "not_found" }, 404);
  if (owner.owner_uid !== u.uid) return c.json({ error: "forbidden" }, 403);

  const { results } = await c.env.DB.prepare(
    `SELECT id, answers_json, calculated_json, submitted_at
       FROM responses WHERE form_id = ? ORDER BY submitted_at DESC LIMIT 500`,
  )
    .bind(id)
    .all();
  return c.json({
    responses: (results as Array<{
      id: string; answers_json: string; calculated_json: string | null; submitted_at: number;
    }>).map((r) => ({
      id: r.id,
      submitted_at: r.submitted_at,
      answers: safeParse(r.answers_json, {}),
      calculated: safeParse(r.calculated_json ?? "{}", {}),
    })),
  });
});

// POST /v1/forms/:id/insights — Gemini summary of responses
app.post("/v1/forms/:id/insights", async (c) => {
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
       WHERE form_id = ? ORDER BY submitted_at DESC LIMIT 50`,
  )
    .bind(id)
    .all();
  const responses = (results as Array<{ answers_json: string; calculated_json: string | null }>).map((r) => ({
    ...safeParse(r.answers_json, {}),
    _calc: safeParse(r.calculated_json ?? "{}", {}),
  }));
  if (responses.length === 0) {
    return c.json({ summary: "No responses yet — share your form to start collecting!" });
  }

  const fields = (safeParse(form.schema_json, []) as Array<{ id: string; label: string; type: string }>);
  try {
    const summary = await summarizeResponsesWithGemini(
      c.env.GEMINI_API_KEY,
      form.title,
      fields,
      responses,
    );
    return c.json({ summary, response_count: responses.length });
  } catch (e) {
    return c.json({ error: "insights_failed", detail: (e as Error).message }, 502);
  }
});

// ---------- Public read + submit ----------

// JSON read for the web filler / Android preview
app.get("/v1/public/forms/:slug", async (c) => {
  const slug = c.req.param("slug");
  const row = await c.env.DB.prepare(
    `SELECT title, description, schema_json, calculations_json
       FROM forms WHERE public_slug = ? AND published = 1`,
  )
    .bind(slug)
    .first<{ title: string; description: string; schema_json: string; calculations_json: string }>();
  if (!row) return c.json({ error: "not_found" }, 404);
  return c.json({
    title: row.title,
    description: row.description,
    fields: safeParse(row.schema_json, []),
    calculations: safeParse(row.calculations_json || "[]", []),
  });
});

// POST /v1/public/forms/:slug/responses — rate-limited public submit
app.post("/v1/public/forms/:slug/responses", async (c) => {
  const slug = c.req.param("slug");
  const form = await c.env.DB.prepare(
    `SELECT id, schema_json FROM forms WHERE public_slug = ? AND published = 1`,
  )
    .bind(slug)
    .first<{ id: string; schema_json: string }>();
  if (!form) return c.json({ error: "not_found" }, 404);

  // Rate limit: 10 submissions / hour / form / IP
  const ip = c.req.header("CF-Connecting-IP") ?? "0.0.0.0";
  const ipHash = await sha256Short(ip);
  const rlKey = `rl:${slug}:${ipHash}:${Math.floor(Date.now() / 3_600_000)}`;
  const used = parseInt((await c.env.RATE_LIMIT.get(rlKey)) ?? "0", 10);
  if (used >= 10) return c.json({ error: "rate_limited" }, 429);

  const body = await c.req.json<{ answers?: Record<string, unknown>; calculated?: Record<string, number> }>()
    .catch(() => ({} as { answers?: Record<string, unknown>; calculated?: Record<string, number> }));
  const answers = body.answers ?? {};
  const calculated = body.calculated ?? {};

  // Validate required fields
  const fields = safeParse(form.schema_json, []) as Array<{
    id: string; label: string; type: string; required?: boolean;
  }>;
  const missing: string[] = [];
  for (const f of fields) {
    if (!f.required) continue;
    const v = (answers as Record<string, unknown>)[f.id];
    const empty =
      v === undefined || v === null || v === "" ||
      (Array.isArray(v) && v.length === 0);
    if (empty) missing.push(f.label);
  }
  if (missing.length) return c.json({ error: "missing_required", fields: missing }, 400);

  const id = crypto.randomUUID();
  const now = Date.now();
  await c.env.DB.prepare(
    `INSERT INTO responses (id, form_id, answers_json, calculated_json, submitter_ip_hash, submitted_at)
     VALUES (?, ?, ?, ?, ?, ?)`,
  )
    .bind(id, form.id, JSON.stringify(answers), JSON.stringify(calculated), ipHash, now)
    .run();

  await c.env.RATE_LIMIT.put(rlKey, String(used + 1), { expirationTtl: 3600 });

  return c.json({ ok: true, id });
});

// ---------- Public HTML form filler ----------

// GET /f/:slug — the form filler page
app.get("/f/:slug", async (c) => {
  const slug = c.req.param("slug");
  const row = await c.env.DB.prepare(
    `SELECT title, description, schema_json, calculations_json
       FROM forms WHERE public_slug = ? AND published = 1`,
  )
    .bind(slug)
    .first<{ title: string; description: string; schema_json: string; calculations_json: string }>();
  if (!row) {
    return new Response(notFoundHtml(), {
      status: 404,
      headers: { "Content-Type": "text/html; charset=utf-8" },
    });
  }
  const url = new URL(c.req.url);
  const html = publicFormHtml({
    slug,
    title: row.title,
    description: row.description ?? "",
    fields: safeParse(row.schema_json, []),
    calculations: safeParse(row.calculations_json || "[]", []),
    submitUrl: `${url.origin}/v1/public/forms/${slug}/responses`,
  });
  return new Response(html, {
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "Cache-Control": "public, max-age=60",
    },
  });
});

// ---------- helpers ----------

function safeParse<T>(s: string, fallback: T): T {
  try { return JSON.parse(s) as T; } catch { return fallback; }
}

async function sha256Short(input: string): Promise<string> {
  const buf = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(input));
  const arr = Array.from(new Uint8Array(buf)).slice(0, 8);
  return arr.map((b) => b.toString(16).padStart(2, "0")).join("");
}

function notFoundHtml(): string {
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<meta name="color-scheme" content="light dark" />
<title>Form not found – SuvForm</title>
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet" />
<script src="https://cdn.tailwindcss.com"></script>
<style>body { font-family: 'Inter', system-ui, sans-serif; -webkit-font-smoothing: antialiased; }</style>
</head>
<body class="bg-white dark:bg-slate-950 text-slate-900 dark:text-slate-100 min-h-screen flex items-center justify-center px-6">
<div class="text-center max-w-sm">
  <div class="inline-flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 dark:bg-slate-900 mb-5">
    <svg class="h-6 w-6 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
    </svg>
  </div>
  <h1 class="text-xl font-semibold mb-2 tracking-tight">Form not found</h1>
  <p class="text-sm text-slate-500 dark:text-slate-400 leading-relaxed">This form may have been unpublished or the link is incorrect. Ask the form owner for a new link.</p>
</div>
</body></html>`;
}

export default app;
