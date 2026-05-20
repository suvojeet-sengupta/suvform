// Gemini structured-output proxy. Keeps the API key on the server (Worker)
// so it never ships in the Android APK.

// Cascade — same approach Advisor-Desk uses: try primary, fall back if quota exhausted.
// 1.5/2.0 currently return quota=0 on this Google account; 2.5 series works.
const MODELS = ["gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-1.5-flash"] as const;
const endpointFor = (model: string) =>
  `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`;

/**
 * The JSON schema Gemini must produce. Mirrors what the Android editor /
 * web filler expect. `response_schema` constrains the model so we get
 * valid JSON without parsing hallucinations.
 */
const FORM_SCHEMA = {
  type: "OBJECT",
  properties: {
    title: { type: "STRING", description: "Short form title" },
    description: { type: "STRING", description: "1-2 line subtitle shown under the title" },
    fields: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          id: { type: "STRING", description: "snake_case stable id" },
          type: {
            type: "STRING",
            enum: [
              "short_text",
              "long_text",
              "number",
              "email",
              "phone",
              "single_choice",
              "multi_choice",
              "date",
              "rating",
            ],
          },
          label: { type: "STRING" },
          required: { type: "BOOLEAN" },
          options: { type: "ARRAY", items: { type: "STRING" } },
          placeholder: { type: "STRING" },
        },
        required: ["id", "type", "label"],
      },
    },
    calculations: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          id: { type: "STRING" },
          label: { type: "STRING" },
          expression: {
            type: "STRING",
            description: "Math expression using field ids, e.g. `quantity * price`",
          },
          format: { type: "STRING", enum: ["number", "currency", "percent"] },
        },
        required: ["id", "label", "expression"],
      },
    },
  },
  required: ["title", "fields"],
} as const;

const SYSTEM_PROMPT = `You design clean, mobile-friendly forms. Given a short user description,
produce a JSON form definition matching the provided schema. Rules:
- Choose the smallest set of fields that captures what the user asked for.
- Use snake_case for field ids.
- Mark obvious required fields (name, email, primary number) as required.
- For pricing/quantity-style fields, also add a calculation (e.g. total = quantity * price).
- For rating fields use type "rating" (1-5).
- Localize labels to the requested locale (en or hi).
- Return ONLY the JSON object, no commentary.`;

export type GeneratedForm = {
  title: string;
  description?: string;
  fields: Array<{
    id: string;
    type: string;
    label: string;
    required?: boolean;
    options?: string[];
    placeholder?: string;
  }>;
  calculations?: Array<{
    id: string;
    label: string;
    expression: string;
    format?: string;
  }>;
};

export async function generateFormWithGemini(
  apiKey: string,
  prompt: string,
  locale: "en" | "hi" = "en",
): Promise<GeneratedForm> {
  const userText =
    `Locale: ${locale}\n` +
    `User description:\n${prompt}\n\n` +
    `Respond with JSON matching the schema.`;

  const body = {
    contents: [{ role: "user", parts: [{ text: userText }] }],
    systemInstruction: { parts: [{ text: SYSTEM_PROMPT }] },
    generationConfig: {
      temperature: 0.4,
      response_mime_type: "application/json",
      response_schema: FORM_SCHEMA,
    },
  };

  // Try each model in order; fall through on 429 (quota) / 503 (overloaded).
  let res: Response | null = null;
  let lastErr = "";
  for (const model of MODELS) {
    res = await fetch(`${endpointFor(model)}?key=${apiKey}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (res.ok) break;
    lastErr = `${model}_${res.status}: ${(await res.text()).slice(0, 200)}`;
    if (res.status !== 429 && res.status !== 503) break; // hard error — don't cascade
  }
  if (!res || !res.ok) throw new Error(`gemini_failed: ${lastErr}`);

  const data = (await res.json()) as {
    candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
  };
  const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) throw new Error("gemini_empty_response");

  let parsed: GeneratedForm;
  try {
    parsed = JSON.parse(text) as GeneratedForm;
  } catch (e) {
    throw new Error(`gemini_parse_failed: ${(e as Error).message}`);
  }
  if (!parsed.title || !Array.isArray(parsed.fields)) {
    throw new Error("gemini_bad_shape");
  }
  return parsed;
}
