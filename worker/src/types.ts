import { FirebaseUser } from "./auth";

export type Bindings = {
  DB: D1Database;
  RATE_LIMIT: KVNamespace;
  FIREBASE_PROJECT_ID: string;
  GEMINI_API_KEY: string;
  GROQ_API_KEY?: string;  // Optional Groq key
};

export type Variables = {
  user: FirebaseUser;
  reqId: string;
  timezone: string;
};
