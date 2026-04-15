# TrackHub

Zero-knowledge health tracker — nutrition, workouts, body metrics. Server stores
only ciphertext; encryption/decryption happens on the client with a key derived
from the user's password. Accounts are admin-created (invite-only); no
self-signup.

Live at https://health-tracker-bot.ru.

## Stack

| Layer | Tech |
|---|---|
| Backend | Python 3.12, FastAPI (async), SQLAlchemy 2.0, Alembic |
| Datastore | PostgreSQL 16, Redis 7 |
| Auth (server-side) | Argon2id over client-derived auth key, opaque session cookie (httpOnly, SameSite=Strict, 7-day sliding), CSRF double-submit |
| Web frontend | Vite + TypeScript, Compose-free SPA, 5-page carousel with swipe |
| Android client | Kotlin, Jetpack Compose, Retrofit, Koin, Android Keystore + biometric |
| Crypto (client) | PBKDF2-SHA256 600k, HKDF-SHA256 subkeys, AES-GCM (256-bit, 12-byte nonce) |
| Telegram bot | aiogram 3 — reduced to a greeter + notification channel |
| Infra | Docker Compose, nginx + Let's Encrypt, UFW |

## Layout

```
.
├── src/
│   ├── app/                 # FastAPI
│   │   ├── main.py          # app, middlewares (CSP, rate limit, CSRF), SPA fallback
│   │   ├── config.py        # pydantic-settings
│   │   ├── database.py      # async SQLAlchemy engine
│   │   ├── redis.py
│   │   ├── crypto.py        # argon2 + PBKDF2/HKDF helpers (admin CLI side)
│   │   ├── dependencies.py  # session+CSRF FastAPI deps
│   │   ├── admin.py         # CLI: create/list/delete/reset users
│   │   ├── models/          # User, Session, Record, Exercise (SQLAlchemy)
│   │   ├── schemas/         # Pydantic request/response models
│   │   ├── api/             # /api/v1/auth, /records, /exercises, /food-search, /debug
│   │   ├── services/        # food-db helpers
│   │   └── static/          # Vite+TS project (served built from dist/)
│   ├── bot/                 # aiogram 3 entrypoint + notify helper
│   └── alembic/             # single-revision schema
├── android/                 # Kotlin Android app (Compose, Material3)
├── docker/                  # Dockerfile.web_server, Dockerfile.tg_bot, docker-compose.yml
├── TODO.md                  # feature roadmap
└── README.md
```

## Auth & crypto model

- Admin creates an account via CLI with a random temp password
  (`python -m app.admin create-user <username>`).
- User logs in; server returns `must_change_password=true` with no wrapped DEK.
- User sets their own password. The client generates a 256-bit random DEK and a
  256-bit random recovery key. Both are wrapped (AES-GCM) with keys derived from
  the password and recovery key respectively, then uploaded to the server.
- Server stores: salt, argon2id(auth key), wrapped DEKs. Never sees plaintext.
- Recovery: user supplies recovery key + new password → server returns the
  recovery-wrapped DEK → client unwraps with recovery KEK → re-wraps with new
  password. Original encrypted records remain intact.
- Android: DEK is wrapped a second time by a hardware-backed Android Keystore
  key that requires `BIOMETRIC_STRONG` for every use. Unlocking the app
  ≈ one biometric prompt per cold start.

Crypto parameters are shared byte-for-byte between the web client
(`src/app/static/src/crypto.ts`), the admin CLI (`src/app/crypto.py`) and the
Android client (`android/app/src/main/kotlin/com/trackhub/crypto/Crypto.kt`).
Regression fixtures are generated from the web client and exercised by the
Android unit tests (`CryptoInteropTest`).

## Running locally

```bash
cp .env.example .env            # fill: POSTGRES_PASSWORD, REDIS_PASSWORD,
                                #       SECRET_KEY, TELEGRAM_BOT_TOKEN, MINIAPP_URL
make up                         # docker compose up -d --build
```

Migrations and admin commands run in the web container:

```bash
docker compose -f docker/docker-compose.yml --env-file .env run --rm web \
    /app/.venv/bin/alembic upgrade head

docker compose -f docker/docker-compose.yml --env-file .env exec web \
    /app/.venv/bin/python -m app.admin create-user alice
```

Frontend dev (hot reload):

```bash
cd src/app/static
npm ci
npm run dev          # Vite on http://localhost:5173, proxies /api to :3000
```

## Android

Open `android/` in Android Studio (Hedgehog+) or use the CLI:

```bash
cd android
./gradlew testDebugUnitTest     # crypto interop vs web fixtures
./gradlew assembleDebug         # APK in app/build/outputs/apk/debug/
```

`minSdk=30` (Android 11+), `targetSdk=35`, ABI filter `arm64-v8a` only.

## API

All routes live under `/api/v1/`. Auth is via opaque session cookie + CSRF
header (`X-CSRF-Token` on POST/PUT/DELETE). Swagger is disabled in production.

| Group | Endpoints |
|---|---|
| Auth | `POST /auth/salt`, `/login`, `/logout`, `/change-password`, `/recover-start`, `/recover-complete`; `GET /auth/me`; `DELETE /auth/account` |
| Records (encrypted) | `GET/POST /records`, `GET/PUT/DELETE /records/{id}` |
| Plaintext catalogs | `GET /exercises`, `GET /food-search?q=` |
| Debug | `GET /debug/health` |

Record payloads are stored as ciphertext blobs with a plaintext `type` and
`record_date` for filtering.

## Security posture

- Strict CSP (no `unsafe-inline`), HSTS preload, `frame-ancestors 'none'`,
  `referrer-policy`, `permissions-policy`, `X-Frame-Options: DENY`.
- CSRF double-submit on all state-changing endpoints (including `/auth/logout`).
- Per-endpoint rate limits: 5/min for `/auth/login`, 3/hour for recovery, 100/min global.
- Argon2id over client-derived auth keys (server-side replay protection even if
  DB leaks).
- Android keystore + biometric-bound DEK wrap; `android:allowBackup="false"`;
  backup-rules exclude all data; network-security-config forbids cleartext.
- UFW on server, only 22/80/443 open.

## Threat model

- **Server operator / DB breach**: can read only ciphertext + metadata
  (usernames, record types, dates, sizes, timestamps).
- **Stolen device (locked)**: Android DEK is bound to biometric in hardware
  keystore; cannot be used without biometric auth.
- **Forgotten password**: recoverable only with recovery key shown once at first
  login. Lost both → data permanently unreadable (by design).
- **Compromised server delivering malicious JS/APK**: fundamentally not
  defendable without out-of-band signing. Users must trust the maintainer.

## Infra

One server behind nginx + Let's Encrypt at `health-tracker-bot.ru`. Docker
Compose brings up `web` (FastAPI + built frontend), `bot` (aiogram), `db`
(Postgres), `redis`. See `docker/docker-compose.yml`.

See `TODO.md` for roadmap (web polish, Android phase 3 UI, home-screen widgets,
reminders via the bot).
