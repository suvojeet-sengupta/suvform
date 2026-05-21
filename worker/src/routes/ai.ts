import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { generateFormWithGemini } from "../gemini";
import { CONFIG } from "../config";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// POST /v1/ai/generate-form
app.post("/generate-form", async (c) => {
  const u = c.get("user");
  const body = await c.req.json<{ prompt?: string; locale?: "en" | "hi" }>().catch(() => ({}));
  const prompt = ("prompt" in body ? body.prompt ?? "" : "").trim();
  
  if (prompt.length < CONFIG.PROMPT_MIN_LEN) return c.json({ error: "prompt_too_short" }, 400);
  if (prompt.length > CONFIG.PROMPT_MAX_LEN) return c.json({ error: "prompt_too_long" }, 400);

  const day = new Date().toISOString().slice(0, 10);
  const quotaKey = `quota:${u.uid}:${day}`;
  const used = parseInt((await c.env.RATE_LIMIT.get(quotaKey)) ?? "0", 10);
  if (used >= CONFIG.AI_DAILY_QUOTA) return c.json({ error: "quota_exceeded", limit: CONFIG.AI_DAILY_QUOTA }, 429);

  // Prefer the user's own key (sent from the app) and fall back to the server secret.
  const apiKey = (c.req.header("X-Gemini-Key") ?? "").trim() || c.env.GEMINI_API_KEY;
  if (!apiKey) return c.json({ error: "no_gemini_key" }, 400);

  try {
    const locale = "locale" in body ? body.locale ?? "en" : "en";
    const form = await generateFormWithGemini(apiKey, prompt, locale);
    await c.env.RATE_LIMIT.put(quotaKey, String(used + 1), { expirationTtl: CONFIG.AI_QUOTA_TTL_SECONDS });
    return c.json(form);
  } catch (e) {
    return c.json({ error: "generation_failed", detail: (e as Error).message }, 502);
  }
});

export default app;
