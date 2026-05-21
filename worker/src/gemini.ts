/**
 * Gemini integration for AI form generation and response summarization.
 */

// Current GA model. gemini-1.5-* was retired by Google, so the old name 404'd.
const GEMINI_MODEL = "gemini-2.0-flash";
const GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models";

export async function generateFormWithGemini(apiKey: string, prompt: string, locale: string) {
  const url = `${GEMINI_BASE}/${GEMINI_MODEL}:generateContent?key=${apiKey}`;

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

  let attempt = 0;
  const maxAttempts = 2;

  while (attempt < maxAttempts) {
    try {
      const res = await fetchWithTimeout(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }, 25000); // 25s timeout for AI generation

      if (!res.ok) {
        const errBody = await res.text().catch(() => "");
        if (res.status === 429 || res.status >= 500) {
          attempt++;
          if (attempt >= maxAttempts) throw new Error(`gemini_api_error_${res.status}: ${errBody.slice(0, 300)}`);
          await new Promise((r) => setTimeout(r, 1000 * attempt)); // simple backoff
          continue;
        }
        throw new Error(`gemini_api_error_${res.status}: ${errBody.slice(0, 300)}`);
      }

      const data = await res.json() as any;
      const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!text) throw new Error("gemini_empty_response");

      // Gemini sometimes wraps JSON in markdown blocks even with responseMimeType
      const cleaned = text.replace(/```json/g, "").replace(/```/g, "").trim();
      return JSON.parse(cleaned);
    } catch (e) {
      if ((e as Error).name === "AbortError") {
        throw new Error("gemini_timeout");
      }
      throw e;
    }
  }
}

export async function summarizeResponsesWithGemini(
  apiKey: string,
  formTitle: string,
  fields: Array<{ id: string; label: string; type: string }>,
  responses: any[],
) {
  const url = `${GEMINI_BASE}/${GEMINI_MODEL}:generateContent?key=${apiKey}`;

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

  try {
    const res = await fetchWithTimeout(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    }, 15000); // 15s for summarization

    if (!res.ok) {
      const errBody = await res.text().catch(() => "");
      throw new Error(`gemini_api_error_${res.status}: ${errBody.slice(0, 300)}`);
    }

    const data = await res.json() as any;
    const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) throw new Error("gemini_empty_response");

    return text.trim();
  } catch (e) {
    if ((e as Error).name === "AbortError") {
      throw new Error("gemini_timeout");
    }
    throw e;
  }
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
