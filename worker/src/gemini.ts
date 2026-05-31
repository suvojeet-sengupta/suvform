/**
 * Gemini integration for AI form generation and response summarization.
 */

// Model chain: try the newest first, fall back to older/lighter models if a
// model is unavailable (404/not found/503) or rate-limited. These three are the
// models available on the project's API key (matches the Advisor-Desk setup).
const GEMINI_MODELS = ["gemini-3-flash-preview", "gemini-2.5-flash", "gemini-2.5-flash-lite"];
const GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models";

// True for errors that mean "this model won't work" — so we should try the next
// model in the chain rather than failing outright.
function isModelUnavailable(status: number, body: string): boolean {
  if (status === 404) return true;
  const b = body.toLowerCase();
  return (
    b.includes("not found") ||
    b.includes("not supported") ||
    b.includes("unavailable") ||
    b.includes("is not found for api version")
  );
}

/**
 * Calls Gemini generateContent, walking the model chain.
 * - On model-unavailable errors (404/not found): immediately try the next model.
 * - On 429/5xx: retry the same model once with backoff, then move to the next.
 * - On other 4xx (e.g. bad key): throw immediately.
 */
async function callGemini(apiKey: string, payload: unknown, timeoutMs: number): Promise<string> {
  let lastErr = "";

  for (const model of GEMINI_MODELS) {
    const url = `${GEMINI_BASE}/${model}:generateContent?key=${apiKey}`;
    let attempt = 0;
    const maxAttempts = 2;

    while (attempt < maxAttempts) {
      try {
        const res = await fetchWithTimeout(url, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        }, timeoutMs);

        if (!res.ok) {
          const errBody = await res.text().catch(() => "");
          lastErr = `gemini_api_error_${res.status}: ${errBody.slice(0, 300)}`;

          // Model won't work at all — skip to the next model in the chain.
          if (isModelUnavailable(res.status, errBody)) break;

          // Transient — retry same model, then fall through to next model.
          if (res.status === 429 || res.status >= 500) {
            attempt++;
            if (attempt >= maxAttempts) break;
            await new Promise((r) => setTimeout(r, 1000 * attempt));
            continue;
          }

          // Other 4xx (bad key, bad request): no point trying other models.
          throw new Error(lastErr);
        }

        const data = (await res.json()) as any;
        const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
        if (!text) {
          lastErr = "gemini_empty_response";
          break; // try next model
        }
        return text as string;
      } catch (e) {
        if ((e as Error).name === "AbortError") throw new Error("gemini_timeout");
        throw e;
      }
    }
  }

  throw new Error(lastErr || "gemini_all_models_failed");
}

export async function generateFormWithGemini(apiKey: string, prompt: string, locale: string) {
  const systemInstructions =
    locale === "hi"
      ? "Tum ek expert form builder ho. User ke prompt ke basis pe ek JSON form structure generate karo. Hindi language use karo labels aur options ke liye. Output sirf valid JSON hona chahiye."
      : "You are an expert form builder. Generate a professional JSON form structure based on the user's prompt. Use English for labels and options. Output MUST be only valid JSON.";

  const payload = {
    contents: [
      {
        parts: [
          {
            text: `${systemInstructions}\n\nUser prompt: ${prompt}\n\nReturn a JSON object with:
            - title: string
            - description: string
            - fields: Array of objects { id: string, label: string, type: "short_text" | "long_text" | "number" | "date" | "single_choice" | "multi_choice" | "rating", required: boolean, placeholder?: string, options?: string[] }
            - calculations: Array of objects { id: string, label: string, expression: string, format: "number" | "currency" | "percent" }
            
            Expression rules: Use field IDs, numbers, and + - * / % (). Example: "(field_1 + field_2) * 0.1".`,
          },
        ],
      },
    ],
    generationConfig: {
      temperature: 0.2,
      topP: 0.8,
      topK: 40,
      responseMimeType: "application/json",
    },
  };

  const text = await callGemini(apiKey, payload, 25000); // 25s timeout for AI generation

  // Gemini sometimes wraps JSON in markdown blocks even with responseMimeType
  const cleaned = text.replace(/```json/g, "").replace(/```/g, "").trim();
  return JSON.parse(cleaned);
}

export async function generateThemeWithGemini(apiKey: string, prompt: string) {
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
  - coverImageKeyword: string - a keyword to find a matching cover image (e.g. "wedding", "office", "nature")
  
  Ensure colors have good contrast and accessibility.`;

  const payload = {
    contents: [
      {
        parts: [
          {
            text: `${systemInstructions}\n\nUser prompt: ${prompt}`,
          },
        ],
      },
    ],
    generationConfig: {
      temperature: 0.7, // Higher temperature for creativity in design
      topP: 0.9,
      responseMimeType: "application/json",
    },
  };

  const text = await callGemini(apiKey, payload, 15000);
  const cleaned = text.replace(/```json/g, "").replace(/```/g, "").trim();
  return JSON.parse(cleaned);
}

export async function summarizeResponsesWithGemini(
  apiKey: string,
  formTitle: string,
  fields: Array<{ id: string; label: string; type: string }>,
  responses: any[],
) {
  const context = `Form: ${formTitle}\nFields: ${JSON.stringify(fields)}\nResponses: ${JSON.stringify(responses)}`;

  const payload = {
    contents: [
      {
        parts: [
          {
            text: `Analyze these form responses and provide a concise, professional summary in 3-4 bullet points. Focus on trends, common answers, and interesting insights.\n\n${context}`,
          },
        ],
      },
    ],
    generationConfig: {
      temperature: 0.3,
      topP: 0.9,
    },
  };

  const text = await callGemini(apiKey, payload, 15000); // 15s for summarization
  return text.trim();
}

/**
 * Internal helper: fetch with abort timeout.
 */
async function fetchWithTimeout(url: string, options: RequestInit, timeoutMs: number): Promise<Response> {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(id);
  }
}
