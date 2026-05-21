import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { generateFormWithGemini, summarizeResponsesWithGemini } from "../gemini";
import { safeParse } from "../utils/helpers";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// POST /v1/ai/generate-form
app.post("/generate-form", async (c) => {
  const u = c.get("user");
  const body = await c.req.json<{ prompt?: string; locale?: "en" | "hi" }>().catch(() => ({}));
  const prompt = ("prompt" in body ? body.prompt ?? "" : "").trim();
  
  if (prompt.length < 10) return c.json({ error: "prompt_too_short" }, 400);
  if (prompt.length > 3000) return c.json({ error: "prompt_too_long" }, 400);

  const day = new Date().toISOString().slice(0, 10);
  const quotaKey = `quota:${u.uid}:${day}`;
  const used = parseInt((await c.env.RATE_LIMIT.get(quotaKey)) ?? "0", 10);
  if (used >= 50) return c.json({ error: "quota_exceeded", limit: 50 }, 429);

  try {
    const locale = "locale" in body ? body.locale ?? "en" : "en";
    const form = await generateFormWithGemini(c.env.GEMINI_API_KEY, prompt, locale);
    await c.env.RATE_LIMIT.put(quotaKey, String(used + 1), { expirationTtl: 86400 });
    return c.json(form);
  } catch (e) {
    return c.json({ error: "generation_failed", detail: (e as Error).message }, 502);
  }
});

// POST /v1/forms/:id/insights
app.post("/insights/:id", async (c) => {
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
  const responses = (results as any[]).map((r) => ({
    ...safeParse(r.answers_json, {}),
    _calc: safeParse(r.calculated_json ?? "{}", {}),
  }));
  if (responses.length === 0) {
    return c.json({ summary: "No responses yet — share your form to start collecting!" });
  }

  const fields = safeParse(form.schema_json, []) as any[];
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

export default app;
