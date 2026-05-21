import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { publicFormHtml } from "../publicForm";
import { safeParse, sha256Short } from "../utils/helpers";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// GET /v1/public/forms/:slug
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

// POST /v1/public/forms/:slug/responses
app.post("/v1/public/forms/:slug/responses", async (c) => {
  const slug = c.req.param("slug");
  const form = await c.env.DB.prepare(`SELECT id, schema_json FROM forms WHERE public_slug = ? AND published = 1`)
    .bind(slug)
    .first<{ id: string; schema_json: string }>();
  if (!form) return c.json({ error: "not_found" }, 404);

  const ip = c.req.header("CF-Connecting-IP") ?? "0.0.0.0";
  const ipHash = await sha256Short(ip);
  const rlKey = `rl:${slug}:${ipHash}:${Math.floor(Date.now() / 3_600_000)}`;
  const used = parseInt((await c.env.RATE_LIMIT.get(rlKey)) ?? "0", 10);
  if (used >= 10) return c.json({ error: "rate_limited" }, 429);

  const body = await c.req.json<{ answers?: Record<string, unknown>; calculated?: Record<string, number> }>().catch(() => ({}));
  const answers = body.answers ?? {};
  const calculated = body.calculated ?? {};

  const fields = safeParse(form.schema_json, []) as any[];
  const missing: string[] = [];
  for (const f of fields) {
    if (!f.required) continue;
    const v = answers[f.id];
    if (v === undefined || v === null || v === "" || (Array.isArray(v) && v.length === 0)) missing.push(f.label);
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

// GET /f/:slug — Serve HTML filler
app.get("/f/:slug", async (c) => {
  const slug = c.req.param("slug");
  const row = await c.env.DB.prepare(
    `SELECT title, description, schema_json, calculations_json
       FROM forms WHERE public_slug = ? AND published = 1`,
  )
    .bind(slug)
    .first<{ title: string; description: string; schema_json: string; calculations_json: string }>();
  if (!row) return c.text("Form not found", 404);
  
  const url = new URL(c.req.url);
  const html = publicFormHtml({
    slug,
    title: row.title,
    description: row.description ?? "",
    fields: safeParse(row.schema_json, []),
    calculations: safeParse(row.calculations_json || "[]", []),
    submitUrl: `${url.origin}/v1/public/forms/${slug}/responses`,
  });
  return c.html(html, { headers: { "Cache-Control": "public, max-age=60" } });
});

export default app;
