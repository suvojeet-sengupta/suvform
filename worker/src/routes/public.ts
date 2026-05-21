import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { publicFormHtml } from "../publicForm";
import { safeParse, sha256Short } from "../utils/helpers";
import { CONFIG } from "../config";

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
  if (used >= CONFIG.PUBLIC_SUBMIT_PER_HOUR) return c.json({ error: "rate_limited" }, 429);

  // Reject oversized payloads early to prevent storage abuse / DoS.
  const rawBody = await c.req.text();
  if (rawBody.length > CONFIG.MAX_BODY_BYTES) return c.json({ error: "payload_too_large" }, 413);

  const body = safeParse<{ answers?: Record<string, unknown>; calculated?: Record<string, number> }>(rawBody, {});
  const rawAnswers = (body && typeof body === "object" && body.answers && typeof body.answers === "object")
    ? (body.answers as Record<string, unknown>)
    : {};
  const calculated = (body && typeof body === "object" && body.calculated && typeof body.calculated === "object")
    ? (body.calculated as Record<string, number>)
    : {};

  const fields = safeParse(form.schema_json, []) as any[];
  const MAX_TEXT_LEN = CONFIG.MAX_TEXT_LEN;

  // Build answers only from known fields, validating each by type. Unknown keys are dropped.
  const answers: Record<string, unknown> = {};
  const missing: string[] = [];
  const invalid: string[] = [];

  for (const f of fields) {
    const raw = rawAnswers[f.id];
    const empty = raw === undefined || raw === null || raw === "" || (Array.isArray(raw) && raw.length === 0);

    if (empty) {
      if (f.required) missing.push(f.label);
      continue;
    }

    const options: string[] = Array.isArray(f.options) ? f.options : [];
    switch (f.type) {
      case "short_text":
      case "long_text":
      case "date": {
        if (typeof raw !== "string" || raw.length > MAX_TEXT_LEN) { invalid.push(f.label); continue; }
        answers[f.id] = raw;
        break;
      }
      case "number":
      case "rating": {
        const num = typeof raw === "number" ? raw : Number(raw);
        if (!Number.isFinite(num)) { invalid.push(f.label); continue; }
        answers[f.id] = num;
        break;
      }
      case "single_choice": {
        if (typeof raw !== "string" || (options.length > 0 && !options.includes(raw))) { invalid.push(f.label); continue; }
        answers[f.id] = raw;
        break;
      }
      case "multi_choice": {
        if (!Array.isArray(raw) || (options.length > 0 && !raw.every((v) => typeof v === "string" && options.includes(v)))) {
          invalid.push(f.label);
          continue;
        }
        answers[f.id] = raw;
        break;
      }
      default: {
        // Unknown field type: accept strings up to the text cap, reject the rest.
        if (typeof raw === "string" && raw.length <= MAX_TEXT_LEN) answers[f.id] = raw;
        else invalid.push(f.label);
      }
    }
  }

  if (missing.length) return c.json({ error: "missing_required", fields: missing }, 400);
  if (invalid.length) return c.json({ error: "invalid_answers", fields: invalid }, 400);

  const id = crypto.randomUUID();
  const now = Date.now();
  await c.env.DB.prepare(
    `INSERT INTO responses (id, form_id, answers_json, calculated_json, submitter_ip_hash, submitted_at)
     VALUES (?, ?, ?, ?, ?, ?)`,
  )
    .bind(id, form.id, JSON.stringify(answers), JSON.stringify(calculated), ipHash, now)
    .run();

  await c.env.RATE_LIMIT.put(rlKey, String(used + 1), { expirationTtl: CONFIG.RATE_LIMIT_TTL_SECONDS });
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
  return c.html(html, { headers: { "Cache-Control": `public, max-age=${CONFIG.PUBLIC_FORM_CACHE_SECONDS}` } });
});

export default app;
