/**
 * Groq integration for ultra-fast AI form generation.
 * Groq uses LPU → bahut fast + accha free tier limits.
 * OpenAI compatible endpoint use kar rahe hain.
 */

const GROQ_BASE = "https://api.groq.com/openai/v1/chat/completions";

// Recommended models (May 2026):
// - "llama-3.3-70b-versatile" → Best quality for JSON forms (Primary)
// - "llama-3.1-8b-instant"    → Ultra fast + higher limits (Fallback)
const PRIMARY_GROQ_MODEL = "llama-3.3-70b-versatile";
const FALLBACK_GROQ_MODEL = "llama-3.1-8b-instant";

async function fetchWithTimeout(url: string, options: RequestInit, timeoutMs: number): Promise<Response> {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(id);
  }
}

async function callGroq(
  apiKey: string,
  messages: Array<{ role: string; content: string }>,
  model: string,
  timeoutMs: number
): Promise<string> {
  const url = GROQ_BASE;

  const res = await fetchWithTimeout(
    url,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model,
        messages,
        temperature: 0.2,
        top_p: 0.9,
        response_format: { type: "json_object" }, // Force JSON output
        max_tokens: 4096,
      }),
    },
    timeoutMs
  );

  if (!res.ok) {
    const errBody = await res.text().catch(() => "");
    throw new Error(`groq_api_error_${res.status}: ${errBody.slice(0, 400)}`);
  }

  const data = (await res.json()) as any;
  const text = data.choices?.[0]?.message?.content;
  if (!text) throw new Error("groq_empty_response");

  return text.trim();
}

export async function generateFormWithGroq(
  apiKey: string,
  prompt: string,
  locale: string
) {
  const systemPrompt =
    locale === "hi"
      ? "Tum ek expert form builder ho. User ke prompt ke basis pe ek JSON form structure generate karo. Hindi language use karo labels aur options ke liye. Output sirf valid JSON hona chahiye."
      : "You are an expert form builder. Generate a professional JSON form structure based on the user's prompt. Use English for labels and options. Output MUST be only valid JSON.";

  const userContent = `${systemPrompt}

User prompt: ${prompt}

Return a JSON object with:
- title: string
- description: string
- fields: Array of objects { id: string, label: string, type: "short_text" | "long_text" | "number" | "date" | "single_choice" | "multi_choice" | "rating", required: boolean, placeholder?: string, options?: string[] }
- calculations: Array of objects { id: string, label: string, expression: string, format: "number" | "currency" | "percent" }

Expression rules: Use field IDs, numbers, and + - * / % (). Example: "(field_1 + field_2) * 0.1".`;

  const messages = [
    { role: "system", content: systemPrompt },
    { role: "user", content: userContent },
  ];

  let text: string;
  try {
    // Attempt with high-quality versatile model first
    text = await callGroq(apiKey, messages, PRIMARY_GROQ_MODEL, 20000);
  } catch (e) {
    console.log("[GROQ PRIMARY FAILED] Falling back to instant model:", (e as Error).message);
    // If versatile fails or times out, try the ultra-fast instant model
    text = await callGroq(apiKey, messages, FALLBACK_GROQ_MODEL, 10000);
  }

  // Clean possible markdown wrapping
  const cleaned = text.replace(/```json/g, "").replace(/```/g, "").trim();
  return JSON.parse(cleaned);
}

export async function generateThemeWithGroq(apiKey: string, prompt: string) {
  const systemInstructions = `You are a professional UI/UX designer. Based on the user's prompt, generate a cohesive and beautiful color theme and typography for a web form.
  
  Prompt example: "wedding invite" or "corporate blue" or "sunset vibes".
  
  Return a JSON object with:
  - backgroundColor: string (6-digit hex)
  - primaryColor: string (6-digit hex) - for main buttons and highlights
  - accentColor: string (6-digit hex) - for secondary highlights
  - textColor: string (6-digit hex) - for main text
  - mutedTextColor: string (6-digit hex) - for labels and hints
  - cardBackgroundColor: string (6-digit hex) - for the form container
  - fontFamily: "serif" | "sans" | "mono"
  - borderRadius: "none" | "small" | "medium" | "large" | "full"
  
  Ensure colors have good contrast and accessibility.`;

  const messages = [
    { role: "system", content: systemInstructions },
    { role: "user", content: `User prompt: ${prompt}` },
  ];

  let text: string;
  try {
    text = await callGroq(apiKey, messages, PRIMARY_GROQ_MODEL, 15000);
  } catch (e) {
    console.log("[GROQ THEME PRIMARY FAILED] Falling back to instant model:", (e as Error).message);
    text = await callGroq(apiKey, messages, FALLBACK_GROQ_MODEL, 10000);
  }

  const cleaned = text.replace(/```json/g, "").replace(/```/g, "").trim();
  return JSON.parse(cleaned);
}
