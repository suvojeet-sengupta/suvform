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
import adminRoutes from "./routes/admin";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// 1. Basic Hardening & Secure Headers
app.use("*", secureHeaders());

// 2. Structured Observability
app.use("*", logger()); // Built-in logger for dev/cli visibility
app.use("*", async (c, next) => {
  const start = Date.now();
  const reqId = crypto.randomUUID();
  c.set("reqId", reqId);

  // Detect timezone from Cloudflare or X-Timezone header (useful for dev/native apps).
  // Default to Asia/Kolkata since the primary user base is in India.
  const tz = c.req.header("cf-timezone") || c.req.header("X-Timezone") || "Asia/Kolkata";
  c.set("timezone", tz);

  await next();

  const duration = Date.now() - start;
  // Structured JSON logging for production
  console.log(JSON.stringify({
    timestamp: new Date().toISOString(),
    timezone: tz,
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
  "https://suvform.suvojeetsengupta.in",
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
  allowHeaders: ["Content-Type", "Authorization", "X-Gemini-Key"],
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

    // Honor "sign out everywhere" / account deletion: a revocation cutoff is
    // stored in KV when the user invalidates sessions. Any token whose
    // auth_time predates the cutoff is rejected even though it hasn't expired.
    const cutoffRaw = await c.env.RATE_LIMIT.get(`revoke:${user.uid}`);
    if (cutoffRaw) {
      const cutoff = parseInt(cutoffRaw, 10);
      if (Number.isFinite(cutoff) && (user.authTime ?? 0) < cutoff) {
        return c.json({ error: "token_revoked" }, 401);
      }
    }

    c.set("user", user);
    return next();
  } catch (e) {
    console.error(`[AuthError] ${c.get("reqId")}:`, (e as Error).message);
    return c.json({ error: "invalid_token" }, 401);
  }
});

// 6. Route Mounting
app.get("/", (c) => c.json({ ok: true, service: "suvform-api", version: 1 }));

app.route("/v1/me", meRoutes);
app.route("/v1/forms", formsRoutes);
app.route("/v1/ai", aiRoutes);
app.route("/v1/admin", adminRoutes);
app.route("/", publicRoutes); // Handles /f/:slug and /v1/public/*

export default app;
