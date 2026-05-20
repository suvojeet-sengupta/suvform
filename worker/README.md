# SuvForm Worker (API)

Cloudflare Worker exposing the SuvForm REST API. Verifies Firebase ID tokens, stores data in D1, proxies Gemini for AI form generation.

## First-time setup

```powershell
cd F:\SuvForm\worker
npm install

# 1. Create the D1 database (one-time, locally creates an empty SQLite db)
npx wrangler d1 create suvform-db
```

Copy the `database_id` from the output into `wrangler.toml` (replace `REPLACE_WITH_D1_DATABASE_ID`).

```powershell
# 2. Create the KV namespace for rate limiting
npx wrangler kv namespace create RATE_LIMIT
```

Copy the `id` into `wrangler.toml` (replace `REPLACE_WITH_KV_NAMESPACE_ID`).

```powershell
# 3. Apply the migration to your remote D1
npx wrangler d1 migrations apply suvform-db --remote

# 4. Set required secrets (interactive, paste when prompted)
npx wrangler secret put FIREBASE_PROJECT_ID
npx wrangler secret put GEMINI_API_KEY
```

`FIREBASE_PROJECT_ID` = the project ID from your Firebase console (e.g. `suvform-12ab3`).
`GEMINI_API_KEY` = from <https://aistudio.google.com/app/apikey> (free tier ~1500 req/day).

```powershell
# 5. Deploy
npx wrangler deploy
```

Output gives you a URL like `https://suvform-api.<your-subdomain>.workers.dev`. That's your `API_BASE_URL` for the Android app.

## Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/` | none | health check |
| POST | `/v1/me` | Firebase ID token | upsert user profile |
| GET | `/v1/forms` | Firebase ID token | list user's forms |
| POST | `/v1/forms` | Firebase ID token | create blank form |
| GET | `/v1/public/forms/:slug` | none | public read of published form |

Auth: send `Authorization: Bearer <firebase_id_token>` on `/v1/*` (except `/v1/public/*`).

## Local dev

```powershell
npx wrangler d1 migrations apply suvform-db --local
npm run dev
```

Worker runs at `http://127.0.0.1:8787`. The local D1 lives in `.wrangler/state/v3/d1/`.

## CI deploy

`.github/workflows/worker-deploy.yml` deploys automatically on push to `main` when files under `worker/**` change. Uses `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` secrets.
