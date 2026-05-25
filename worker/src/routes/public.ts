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
    `SELECT title, description, schema_json, calculations_json, current_version_id, response_limit
       FROM forms WHERE public_slug = ? AND published = 1`,
  )
    .bind(slug)
    .first<{ title: string; description: string; schema_json: string; calculations_json: string; current_version_id: string; response_limit: number | null }>();
  if (!row) return c.json({ error: "not_found" }, 404);
  return c.json({
    title: row.title,
    description: row.description,
    fields: safeParse(row.schema_json, []),
    calculations: safeParse(row.calculations_json || "[]", []),
    version_id: row.current_version_id,
    response_limit: row.response_limit,
  });
});

// POST /v1/public/forms/:slug/responses
app.post("/v1/public/forms/:slug/responses", async (c) => {
  const slug = c.req.param("slug");
  const form = await c.env.DB.prepare(`SELECT id, current_version_id, schema_json, response_limit FROM forms WHERE public_slug = ? AND published = 1`)
    .bind(slug)
    .first<{ id: string; current_version_id: string; schema_json: string; response_limit: number | null }>();
  if (!form) return c.json({ error: "not_found" }, 404);

  // Enforce response limit (if set)
  if (form.response_limit && form.response_limit > 0) {
    const countRow = await c.env.DB.prepare(
      `SELECT COUNT(*) as c FROM responses WHERE form_id = ?`
    ).bind(form.id).first<{ c: number }>();
    const currentCount = countRow?.c ?? 0;
    if (currentCount >= form.response_limit) {
      return c.json({ error: "response_limit_reached", limit: form.response_limit }, 403);
    }
  }

  const ip = c.req.header("CF-Connecting-IP") ?? "0.0.0.0";
  const ipHash = await sha256Short(ip);
  const rlKey = `rl:${slug}:${ipHash}:${Math.floor(Date.now() / 3_600_000)}`;
  const used = parseInt((await c.env.RATE_LIMIT.get(rlKey)) ?? "0", 10);
  if (used >= CONFIG.PUBLIC_SUBMIT_PER_HOUR) return c.json({ error: "rate_limited" }, 429);

  const rawBody = await c.req.text();
  if (rawBody.length > CONFIG.MAX_BODY_BYTES) return c.json({ error: "payload_too_large" }, 413);

  const body = safeParse<{ answers?: Record<string, unknown>; calculated?: Record<string, number>; versionId?: string }>(rawBody, {});
  
  // Use the submitted versionId if valid, otherwise fallback to current.
  let targetVersionId = body?.versionId || form.current_version_id;
  let validationSchemaJson = form.schema_json;

  // If the submitted version isn't the current one, fetch its specific schema for validation.
  if (body?.versionId && body.versionId !== form.current_version_id) {
    const verRow = await c.env.DB.prepare(`SELECT schema_json FROM form_versions WHERE id = ? AND form_id = ?`)
      .bind(body.versionId, form.id)
      .first<{ schema_json: string }>();
    if (verRow) {
      validationSchemaJson = verRow.schema_json;
    } else {
      // Version not found for this form, fallback to current
      targetVersionId = form.current_version_id;
    }
  }

  const rawAnswers = (body && typeof body === "object" && body.answers && typeof body.answers === "object")
    ? (body.answers as Record<string, unknown>)
    : {};
  const calculated = (body && typeof body === "object" && body.calculated && typeof body.calculated === "object")
    ? (body.calculated as Record<string, number>)
    : {};

  const fields = safeParse(validationSchemaJson, []) as any[];
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
    `INSERT INTO responses (id, form_id, version_id, answers_json, calculated_json, submitter_ip_hash, submitted_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
  )
    .bind(id, form.id, targetVersionId, JSON.stringify(answers), JSON.stringify(calculated), ipHash, now)
    .run();

  await c.env.RATE_LIMIT.put(rlKey, String(used + 1), { expirationTtl: CONFIG.RATE_LIMIT_TTL_SECONDS });
  return c.json({ ok: true, id });
});

// GET /f/:slug — Serve HTML filler
app.get("/f/:slug", async (c) => {
  const slug = c.req.param("slug");
  const row = await c.env.DB.prepare(
    `SELECT title, description, schema_json, calculations_json, current_version_id, response_limit
       FROM forms WHERE public_slug = ? AND published = 1`,
  )
    .bind(slug)
    .first<{ title: string; description: string; schema_json: string; calculations_json: string; current_version_id: string; response_limit: number | null }>();
  if (!row) return c.text("Form not found", 404);

  let isClosed = false;
  let currentResponseCount = 0;
  if (row.response_limit && row.response_limit > 0) {
    const countRow = await c.env.DB.prepare(
      `SELECT COUNT(*) as c FROM responses WHERE form_id = (SELECT id FROM forms WHERE public_slug = ? LIMIT 1)`
    ).bind(slug).first<{ c: number }>();
    currentResponseCount = countRow?.c ?? 0;
    if (currentResponseCount >= row.response_limit) isClosed = true;
  }

  const url = new URL(c.req.url);
  const html = publicFormHtml({
    slug,
    title: row.title,
    description: row.description ?? "",
    fields: safeParse(row.schema_json, []),
    calculations: safeParse(row.calculations_json || "[]", []),
    versionId: row.current_version_id,
    responseLimit: row.response_limit,
    currentResponseCount,
    isClosed,
    submitUrl: `${url.origin}/v1/public/forms/${slug}/responses`,
  });
  return c.html(html, { headers: { "Cache-Control": `public, max-age=${CONFIG.PUBLIC_FORM_CACHE_SECONDS}` } });
});

export default app;
