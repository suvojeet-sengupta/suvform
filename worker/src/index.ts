import { Hono } from "hono";
import { cors } from "hono/cors";
import { verifyFirebaseIdToken, type FirebaseUser } from "./auth";

type Bindings = {
  DB: D1Database;
  RATE_LIMIT: KVNamespace;
  FIREBASE_PROJECT_ID: string;
  GEMINI_API_KEY: string;
};

type Variables = {
  user: FirebaseUser;
};

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

app.use("*", cors({ origin: "*", allowMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"] }));

// Health check (unauthenticated)
app.get("/", (c) => c.json({ ok: true, service: "suvform-api", version: 1 }));

// Auth middleware — runs on every /v1/* route except /v1/public/*
app.use("/v1/*", async (c, next) => {
  if (c.req.path.startsWith("/v1/public/")) return next();
  const authHeader = c.req.header("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return c.json({ error: "missing_token" }, 401);
  }
  const token = authHeader.slice(7);
  try {
    const user = await verifyFirebaseIdToken(token, c.env.FIREBASE_PROJECT_ID);
    c.set("user", user);
  } catch (e) {
    return c.json({ error: "invalid_token", detail: (e as Error).message }, 401);
  }
  return next();
});

// POST /v1/me — upsert user profile after sign-in
app.post("/v1/me", async (c) => {
  const u = c.get("user");
  const now = Date.now();
  await c.env.DB.prepare(
    `INSERT INTO users (uid, email, display_name, photo_url, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?)
     ON CONFLICT(uid) DO UPDATE SET
       email = excluded.email,
       display_name = excluded.display_name,
       photo_url = excluded.photo_url,
       updated_at = excluded.updated_at`,
  )
    .bind(u.uid, u.email ?? null, u.name ?? null, u.picture ?? null, now, now)
    .run();
  return c.json({ uid: u.uid, email: u.email, display_name: u.name, photo_url: u.picture });
});

// GET /v1/forms — list current user's forms
app.get("/v1/forms", async (c) => {
  const u = c.get("user");
  const { results } = await c.env.DB.prepare(
    `SELECT id, title, description, published, public_slug, created_at, updated_at
       FROM forms WHERE owner_uid = ? ORDER BY updated_at DESC LIMIT 100`,
  )
    .bind(u.uid)
    .all();
  return c.json({ forms: results });
});

// POST /v1/forms — create a new (empty) form
app.post("/v1/forms", async (c) => {
  const u = c.get("user");
  const body: { title?: string; description?: string } = await c.req
    .json<{ title?: string; description?: string }>()
    .catch(() => ({}));
  const id = crypto.randomUUID();
  const now = Date.now();
  const title = body.title ?? "Untitled form";
  const description = body.description ?? "";
  await c.env.DB.prepare(
    `INSERT INTO forms (id, owner_uid, title, description, schema_json, calculations_json, created_at, updated_at)
     VALUES (?, ?, ?, ?, '[]', '[]', ?, ?)`,
  )
    .bind(id, u.uid, title, description, now, now)
    .run();
  return c.json({ id, title, description, published: 0, public_slug: null }, 201);
});

// Public read for the web filler — published forms only
app.get("/v1/public/forms/:slug", async (c) => {
  const slug = c.req.param("slug");
  const row = await c.env.DB.prepare(
    `SELECT title, description, schema_json, calculations_json
       FROM forms WHERE public_slug = ? AND published = 1`,
  )
    .bind(slug)
    .first<{ title: string; description: string; schema_json: string; calculations_json: string }>();
  if (!row) return c.json({ error: "not_found" }, 404);
  return c.json({
    title: row.title,
    description: row.description,
    fields: JSON.parse(row.schema_json),
    calculations: JSON.parse(row.calculations_json || "[]"),
  });
});

export default app;
