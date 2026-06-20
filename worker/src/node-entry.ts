import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { serve } from "@hono/node-server";
import Database from "better-sqlite3";
import app from "./index";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 1. Configuration
const PORT = parseInt(process.env.PORT || "8787", 10);
const DB_PATH = process.env.DATABASE_PATH || "./suvform.db";
const MIGRATIONS_DIR = process.env.MIGRATIONS_DIR || path.resolve(__dirname, "../migrations");

console.log(`[Node Server] Starting SuvForm backend...`);
console.log(`[Node Server] Database path: ${DB_PATH}`);
console.log(`[Node Server] Migrations directory: ${MIGRATIONS_DIR}`);

// Ensure parent directory of database exists
const dbDir = path.dirname(DB_PATH);
if (!fs.existsSync(dbDir)) {
  fs.mkdirSync(dbDir, { recursive: true });
}

// 2. Initialize Database
const sqliteDb = new Database(DB_PATH);

// 3. Migration Runner
function runMigrations(db: Database.Database, dir: string) {
  db.exec(`
    CREATE TABLE IF NOT EXISTS _migrations (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT UNIQUE NOT NULL,
      applied_at INTEGER NOT NULL
    )
  `);

  if (!fs.existsSync(dir)) {
    console.warn(`[Migrations] Directory not found: ${dir}. Skipping migrations.`);
    return;
  }

  const files = fs.readdirSync(dir)
    .filter(file => file.endsWith(".sql"))
    .sort();

  console.log(`[Migrations] Found ${files.length} migration file(s).`);

  for (const file of files) {
    const isApplied = db.prepare("SELECT 1 FROM _migrations WHERE name = ?").get(file);
    if (isApplied) continue;

    console.log(`[Migrations] Applying ${file}...`);
    const sql = fs.readFileSync(path.join(dir, file), "utf8");

    const execute = db.transaction(() => {
      db.exec(sql);
      db.prepare("INSERT INTO _migrations (name, applied_at) VALUES (?, ?)").run(file, Date.now());
    });

    try {
      execute();
      console.log(`[Migrations] Successfully applied ${file}.`);
    } catch (err) {
      console.error(`[Migrations] Failed to apply ${file}:`, err);
      throw err;
    }
  }
}

try {
  runMigrations(sqliteDb, MIGRATIONS_DIR);
} catch (err) {
  console.error("[Node Server] Migrations failed, exiting:", err);
  process.exit(1);
}

// 4. Cloudflare D1/KV Emulation Layer
class NodeD1Statement {
  constructor(
    private db: Database.Database,
    public query: string,
    public params: any[] = []
  ) {}

  bind(...params: any[]): NodeD1Statement {
    return new NodeD1Statement(this.db, this.query, params);
  }

  async all<T = any>() {
    const start = Date.now();
    try {
      const stmt = this.db.prepare(this.query);
      const results = stmt.all(...this.params) as T[];
      return {
        results,
        success: true,
        meta: { duration: Date.now() - start, changes: 0 }
      };
    } catch (err: any) {
      console.error(`[NodeD1 Error] Query: "${this.query}"`, err);
      throw err;
    }
  }

  async first<T = any>(colName?: string): Promise<T | null> {
    try {
      const stmt = this.db.prepare(this.query);
      const row = stmt.get(...this.params) as any;
      if (row === undefined) return null;
      if (colName) return row[colName] ?? null;
      return row as T;
    } catch (err) {
      console.error(`[NodeD1 Error] first() Query: "${this.query}"`, err);
      throw err;
    }
  }

  async run() {
    const start = Date.now();
    try {
      const stmt = this.db.prepare(this.query);
      const info = stmt.run(...this.params);
      return {
        results: [],
        success: true,
        meta: {
          duration: Date.now() - start,
          changes: info.changes,
          last_row_id: Number(info.lastInsertRowid)
        }
      };
    } catch (err: any) {
      console.error(`[NodeD1 Error] run() Query: "${this.query}"`, err);
      throw err;
    }
  }
}

class NodeD1Database {
  constructor(private db: Database.Database) {}

  prepare(query: string): NodeD1Statement {
    return new NodeD1Statement(this.db, query);
  }

  async batch(statements: NodeD1Statement[]) {
    const transaction = this.db.transaction((stmts: NodeD1Statement[]) => {
      const results: any[] = [];
      for (const stmt of stmts) {
        const s = this.db.prepare(stmt.query);
        const start = Date.now();
        if (s.reader) {
          const res = s.all(...stmt.params);
          results.push({
            results: res,
            success: true,
            meta: { duration: Date.now() - start, changes: 0 }
          });
        } else {
          const info = s.run(...stmt.params);
          results.push({
            results: [],
            success: true,
            meta: {
              duration: Date.now() - start,
              changes: info.changes,
              last_row_id: Number(info.lastInsertRowid)
            }
          });
        }
      }
      return results;
    });
    return transaction(statements);
  }

  async exec(sql: string): Promise<void> {
    this.db.exec(sql);
  }
}

class NodeKVNamespace {
  constructor(private db: Database.Database) {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS kv_store (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL,
        expires_at INTEGER
      )
    `);
  }

  async get(key: string): Promise<string | null> {
    const row = this.db
      .prepare("SELECT value, expires_at FROM kv_store WHERE key = ?")
      .get(key) as { value: string; expires_at: number | null } | undefined;

    if (!row) return null;
    if (row.expires_at !== null && row.expires_at < Date.now()) {
      this.db.prepare("DELETE FROM kv_store WHERE key = ?").run(key);
      return null;
    }
    return row.value;
  }

  async put(
    key: string,
    value: string,
    options?: { expirationTtl?: number }
  ): Promise<void> {
    const expiresAt = options?.expirationTtl
      ? Date.now() + options.expirationTtl * 1000
      : null;

    this.db
      .prepare(
        `
        INSERT INTO kv_store (key, value, expires_at)
        VALUES (?, ?, ?)
        ON CONFLICT(key) DO UPDATE SET value = excluded.value, expires_at = excluded.expires_at
      `
      )
      .run(key, value, expiresAt);
  }

  async delete(key: string): Promise<void> {
    this.db.prepare("DELETE FROM kv_store WHERE key = ?").run(key);
  }
}

const nodeD1Database = new NodeD1Database(sqliteDb);
const nodeKVNamespace = new NodeKVNamespace(sqliteDb);

// 5. Start Server
serve({
  fetch: (req) => {
    return app.fetch(req, {
      DB: nodeD1Database,
      RATE_LIMIT: nodeKVNamespace,
      FIREBASE_PROJECT_ID: process.env.FIREBASE_PROJECT_ID || "",
      GEMINI_API_KEY: process.env.GEMINI_API_KEY || "",
      GROQ_API_KEY: process.env.GROQ_API_KEY || "",
    } as any);
  },
  port: PORT,
});

console.log(`[Node Server] Running on http://localhost:${PORT}`);
