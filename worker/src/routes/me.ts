import { Hono } from "hono";
import { Bindings, Variables } from "../types";
import { upsertUserProfile } from "../db";

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// POST /v1/me — sync user profile after sign-in
app.post("/", async (c) => {
  const u = c.get("user");
  await upsertUserProfile(c.env.DB, u);
  return c.json({ uid: u.uid, email: u.email, display_name: u.name, photo_url: u.picture });
});

export default app;
