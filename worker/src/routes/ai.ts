import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { generateFormWithGemini } from "../gemini";
import { generateFormWithGroq } from "../groq";   // ← Naya import
import { CONFIG } from "../config";
import { getLocalDay } from "../utils/time";
import { isAdmin } from "../db";                   // ← Admin check ke liye

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// POST /v1/ai/generate-form
app.post("/generate-form", async (c) => {
  const u = c.get("user");
  const tz = c.get("timezone");
  const body = await c.req.json<{ prompt?: string; locale?: "en" | "hi" }>().catch(() => ({}));
  const prompt = ("prompt" in body ? body.prompt ?? "" : "").trim();

  if (prompt.length < CONFIG.PROMPT_MIN_LEN) return c.json({ error: "prompt_too_short" }, 400);
  if (prompt.length > CONFIG.PROMPT_MAX_LEN) return c.json({ error: "prompt_too_long" }, 400);

  // === TIERED QUOTA (Admin vs Regular) ===
  const day = getLocalDay(tz);
  const quotaKey = `quota:${u.uid}:${day}`;
  const used = parseInt((await c.env.RATE_LIMIT.get(quotaKey)) ?? "0", 10);

  const userIsAdmin = await isAdmin(c.env.DB, u.uid);
  const dailyLimit = userIsAdmin ? CONFIG.AI_DAILY_QUOTA_ADMIN : CONFIG.AI_DAILY_QUOTA;

  if (used >= dailyLimit) {
    return c.json({ error: "quota_exceeded", limit: dailyLimit, is_admin: userIsAdmin }, 429);
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

    // Increment quota
    await c.env.RATE_LIMIT.put(quotaKey, String(used + 1), { expirationTtl: CONFIG.AI_QUOTA_TTL_SECONDS });

    return c.json({ ...form, provider: finalGroqKey ? "groq" : "gemini", is_admin: userIsAdmin });
  } catch (e) {
    return c.json({ error: "generation_failed", detail: (e as Error).message }, 502);
  }
});

export default app;
