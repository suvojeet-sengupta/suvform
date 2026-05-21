import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { generateFormWithGemini } from "../gemini";

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

export default app;
