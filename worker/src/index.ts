import { Hono } from "hono";
import { cors } from "hono/cors";
import { logger } from "hono/logger";
import { secureHeaders } from "hono/secure-headers";
import { verifyFirebaseIdToken } from "./auth";
import { Bindings, Variables } from "./types";

// Route imports
import meRoutes from "./routes/me";
import formsRoutes from "./routes/forms";
import aiRoutes from "./routes/ai";
import publicRoutes from "./routes/public";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// 1. Basic Hardening & Secure Headers
app.use("*", secureHeaders());

// 2. Structured Observability
app.use("*", logger()); // Built-in logger for dev/cli visibility
app.use("*", async (c, next) => {
  const start = Date.now();
  const reqId = crypto.randomUUID();
  c.set("reqId", reqId);

  await next();

  const duration = Date.now() - start;
  // Structured JSON logging for production
  console.log(JSON.stringify({
    timestamp: new Date().toISOString(),
    reqId,
    method: c.req.method,
    path: c.req.path,
    status: c.res.status,
    durationMs: duration,
    ip: c.req.header("CF-Connecting-IP") || "unknown",
    uid: c.get("user")?.uid || "unauthenticated",
  }));
});

// 3. Strict CORS
const ALLOWED_ORIGINS = [
  "https://suvforms.suvojeetsengupta.in",
  "https://suvojeetsengupta.in",
  "http://localhost:3000",
];

app.use("*", cors({
  origin: (origin) => {
    // If no origin (e.g. mobile app), allow it. Authentication protects these calls.
    if (!origin) return "*";
    if (ALLOWED_ORIGINS.includes(origin)) return origin;
    // Fallback for unauthorized origins
    return "https://suvforms.suvojeetsengupta.in";
  },
  allowMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  allowHeaders: ["Content-Type", "Authorization"],
  exposeHeaders: ["Content-Length"],
  maxAge: 86400,
}));

// 4. Global Error Handling
app.onError((err, c) => {
  console.error(`[UnhandledError] ${c.req.method} ${c.req.path} (ReqID: ${c.get("reqId")}):`, err);
  return c.json({
    error: "internal_server_error",
    message: "An unexpected error occurred. Please try again later.",
    reqId: c.get("reqId"),
  }, 500);
});

// 5. Auth Middleware
app.use("/v1/*", async (c, next) => {
  // Skip auth for public endpoints
  if (c.req.path.startsWith("/v1/public/")) return next();
  
  const authHeader = c.req.header("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return c.json({ error: "missing_token" }, 401);
  }
  const token = authHeader.slice(7);
  try {
    const user = await verifyFirebaseIdToken(token, c.env.FIREBASE_PROJECT_ID);
    c.set("user", user);
    return next();
  } catch (e) {
    return c.json({ error: "invalid_token", detail: (e as Error).message }, 401);
  }
});

// 6. Route Mounting
app.get("/", (c) => c.json({ ok: true, service: "suvform-api", version: 1 }));

app.route("/v1/me", meRoutes);
app.route("/v1/forms", formsRoutes);
app.route("/v1/ai", aiRoutes);
app.route("/", publicRoutes); // Handles /f/:slug and /v1/public/*

export default app;
