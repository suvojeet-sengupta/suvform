# SuvForm — Advanced AI Form Builder

> Google Forms but smarter. Describe a form in plain words → Gemini builds it → share a link → get responses with auto-calculations and AI insights, all private to you.

Native Android (Kotlin + Jetpack Compose) + Cloudflare Workers + D1 + Pages, with Firebase for Auth and FCM only.

---

## Project layout

```
F:\SuvForm\
├── app\                         # Android app (Kotlin + Compose + Hilt + Room)
├── worker\                      # Cloudflare Worker (Hono + D1 + Gemini proxy)
├── web\                         # Cloudflare Pages (Next.js public form filler)
└── .github\workflows\           # CI/CD — signed APK + AAB on every push to main
```

## Tech stack

| Layer | Choice |
|---|---|
| Android | Kotlin 2.3.21, Jetpack Compose BOM 2026.04.01, Material 3, Hilt, Navigation Compose, Room, Retrofit + OkHttp + kotlinx-serialization, Coil 3, Vico charts |
| Auth | Firebase Auth (Google Sign-In) |
| Push | Firebase Cloud Messaging |
| API & DB | Cloudflare Workers (Hono, TypeScript) + Cloudflare D1 (SQLite) |
| AI | Gemini 2.0 Flash via secure Worker proxy (free tier ~1500 req/day) |
| Public forms | Cloudflare Pages (Next.js 15, Tailwind) |
| Build | Gradle 9.5.1, AGP 9.2.0, Java 17, KSP |

## Local setup

### Prerequisites

- JDK 17 (Temurin recommended)
- Android Studio 2026.1+ (Iguana or later) — for an IDE; not required for command-line builds
- Node.js 20+ (for `worker/` and `web/`)
- `wrangler` CLI: `npm i -g wrangler`

### First-time build

```powershell
cd F:\SuvForm
./gradlew assembleDebug
```

The debug build does NOT require any keystore — it falls back to the auto-generated debug signing key.

### Adding Firebase

1. Create a Firebase project at <https://console.firebase.google.com>.
2. Add an Android app with package `com.suvojeetsengupta.suvform`.
3. Drop the downloaded `google-services.json` at `app/google-services.json` (gitignored).
4. Uncomment `alias(libs.plugins.google.services)` in `app/build.gradle.kts`.
5. Enable Google Sign-In under Auth providers + add your SHA-1 (`./gradlew signingReport`).

---

## CI/CD — every push to `main`

`.github/workflows/android-release.yml` runs on every push to `main`:

1. Sets up JDK 17 + Gradle cache
2. Decodes the keystore + `google-services.json` from GitHub Secrets
3. Builds a signed `app-release.apk` and `app-release.aab`
4. Creates a GitHub Release tagged `v1.0.<run_number>` with both artifacts attached

### Required GitHub Secrets

Create these in your repo's **Settings → Secrets and variables → Actions**:

| Secret | How to produce |
|---|---|
| `KEYSTORE_BASE64` | `keytool -genkey -v -keystore suvform.jks -keyalg RSA -keysize 2048 -validity 10000 -alias suvform`, then `[Convert]::ToBase64String([IO.File]::ReadAllBytes("suvform.jks"))` in PowerShell (Linux/Mac: `base64 -w0 suvform.jks`). |
| `KEYSTORE_PASSWORD` | Password you set when generating the keystore |
| `KEY_ALIAS` | The alias above (e.g. `suvform`) |
| `KEY_PASSWORD` | Key password (usually same as keystore password) |
| `GOOGLE_SERVICES_JSON` | Base64 of `app/google-services.json` |
| `CLOUDFLARE_API_TOKEN` | From Cloudflare dashboard → My Profile → API Tokens → "Edit Cloudflare Workers" template |
| `CLOUDFLARE_ACCOUNT_ID` | Cloudflare dashboard → right sidebar |

### Manual one-off release locally

```powershell
$env:KEYSTORE_FILE = "F:\secrets\suvform.jks"
$env:KEYSTORE_PASSWORD = "..."
$env:KEY_ALIAS = "suvform"
$env:KEY_PASSWORD = "..."
./gradlew bundleRelease assembleRelease "-PversionCode=2" "-PversionName=1.0.2"
```

---

## Bootstrapping git + GitHub

```powershell
cd F:\SuvForm
git init -b main
git add .
git commit -m "Initial SuvForm scaffold"
gh repo create suvform --private --source=. --remote=origin --push
```

Then push your secrets in one go:

```powershell
gh secret set KEYSTORE_BASE64 < keystore.b64
gh secret set KEYSTORE_PASSWORD --body "..."
gh secret set KEY_ALIAS --body "suvform"
gh secret set KEY_PASSWORD --body "..."
gh secret set GOOGLE_SERVICES_JSON < google-services.b64
gh secret set CLOUDFLARE_API_TOKEN --body "..."
gh secret set CLOUDFLARE_ACCOUNT_ID --body "..."
```

The next push to `main` will produce a green build and a v1.0.1 release.

---

## License & ownership

© 2026 Suvojeet Sengupta. All rights reserved.
