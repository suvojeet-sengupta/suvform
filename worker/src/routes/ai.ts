import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { generateFormWithGemini } from "../gemini";
import { generateFormWithGroq } from "../groq";   // ← Naya import
import { CONFIG } from "../config";
import { getLocalDay } from "../utils/time";
import { isAdmin } from "../db";                   // ← Admin gets unlimited AI

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// POST /v1/ai/generate-form
app.post("/generate-form", async (c) => {
  const u = c.get("user");
  const tz = c.get("timezone");
  const body = await c.req.json<{ prompt?: string; locale?: "en" | "hi" }>().catch(() => ({}));
  const prompt = ("prompt" in body ? body.prompt ?? "" : "").trim();

  if (prompt.length < CONFIG.PROMPT_MIN_LEN) return c.json({ error: "prompt_too_short" }, 400);
  if (prompt.length > CONFIG.PROMPT_MAX_LEN) return c.json({ error: "prompt_too_long" }, 400);

  // === QUOTA: admin is unlimited, everyone else gets AI_DAILY_QUOTA/day ===
  const day = getLocalDay(tz);
  const quotaKey = `quota:${u.uid}:${day}`;
  const used = parseInt((await c.env.RATE_LIMIT.get(quotaKey)) ?? "0", 10);

  const userIsAdmin = await isAdmin(c.env.DB, u.uid);

  if (!userIsAdmin && used >= CONFIG.AI_DAILY_QUOTA) {
    return c.json({ error: "quota_exceeded", limit: CONFIG.AI_DAILY_QUOTA }, 429);
  }

  // === KEY PRIORITY: Groq > Gemini ===
  const groqKey = (c.req.header("X-Groq-Key") ?? "").trim();
  const geminiKey = (c.req.header("X-Gemini-Key") ?? "").trim();

  const finalGroqKey = groqKey || c.env.GROQ_API_KEY;
  const finalGeminiKey = geminiKey || c.env.GEMINI_API_KEY;

  const locale = "locale" in body ? body.locale ?? "en" : "en";

  try {
    let form: any;

    // Groq first (faster + better limits)
    if (finalGroqKey) {
      try {
        form = await generateFormWithGroq(finalGroqKey, prompt, locale);
      } catch (groqErr) {
        console.log("[GROQ FAILED] Falling back to Gemini:", (groqErr as Error).message);
        if (!finalGeminiKey) throw groqErr;
        form = await generateFormWithGemini(finalGeminiKey, prompt, locale);
      }
    } else if (finalGeminiKey) {
      form = await generateFormWithGemini(finalGeminiKey, prompt, locale);
    } else {
      return c.json({ error: "no_ai_key" }, 400);
    }

    // Increment quota only for non-admins (admin is unlimited).
    if (!userIsAdmin) {
      await c.env.RATE_LIMIT.put(quotaKey, String(used + 1), { expirationTtl: CONFIG.AI_QUOTA_TTL_SECONDS });
    }

    return c.json({ ...form, provider: finalGroqKey ? "groq" : "gemini", is_admin: userIsAdmin });
  } catch (e) {
    console.error(`[AIError] ${c.get("reqId")}:`, (e as Error).message);
    return c.json({ error: "generation_failed" }, 502);
  }
});

// POST /v1/ai/generate-theme
app.post("/generate-theme", async (c) => {
  const u = c.get("user");
  const body = await c.req.json<{ prompt?: string }>().catch(() => ({}));
  const prompt = (body.prompt ?? "").trim();

  if (prompt.length < 3) return c.json({ error: "prompt_too_short" }, 400);

  const geminiKey = (c.req.header("X-Gemini-Key") ?? "").trim() || c.env.GEMINI_API_KEY;
  if (!geminiKey) return c.json({ error: "no_ai_key" }, 400);

  try {
    const theme = await generateThemeWithGemini(geminiKey, prompt);
    return c.json(theme);
  } catch (e) {
    console.error(`[ThemeAIError] ${c.get("reqId")}:`, (e as Error).message);
    return c.json({ error: "theme_generation_failed" }, 502);
  }
});

export default app;
