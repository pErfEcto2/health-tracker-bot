# TrackHub — TODO

## Website

### Features
- [ ] Edit existing records (food entries, workout sessions, measurements) — currently only create+delete
- [ ] Password change from profile (when remembered) — plain `new password` form that keeps the recovery wrap unchanged
- [ ] Regenerate recovery key from profile (invalidates old one)
- [ ] Stats / charts — weight over time, calories/macros trend, workout volume (Chart.js already in deps earlier, can re-add)
- [ ] Weekly / monthly aggregates on home page
- [ ] Notes field on food entries
- [ ] Copy yesterday's meals to today
- [ ] Favorites / recent foods — persisted per-user as encrypted record
- [ ] Workout templates (pre-filled session with exercises)
- [ ] Edit/delete individual sets within workout session
- [ ] Custom meal types or renaming

### Polish / UX
- [ ] PWA manifest + service worker → "install to home screen" on mobile
- [ ] Explicit light/dark toggle (today follows OS only)
- [ ] Russian date formatting (currently browser-default)
- [ ] Undo toast after delete (soft-delete for 5s)
- [ ] Loading skeletons during carousel pre-render
- [ ] Empty-state illustrations / hints

### Tech
- [ ] Edit/update record API route protection — confirm PUT checks user_id (already done, regression test coverage)
- [ ] Rate-limit records endpoints (currently only auth endpoints)
- [ ] Audit log on server for suspicious activity (failed logins, account deletes)
- [ ] Alembic migration tooling in Makefile
- [ ] CI: GitHub Action — pytest + tsc --noEmit on push

---

## Android

### Phase 2 (auth flow)
- [ ] Compose screens: login, change-password + recovery-key reveal, recovery, setup
- [ ] DekManager wired: bootstrap on first login, biometric unlock on cold start
- [ ] Navigation (Navigation Compose)
- [ ] Error states + toast equivalents

### Phase 3 (main UI, view + add)
- [ ] Home: today's calories/macros/water/weight cards, quick-action row
- [ ] Food: day picker with `‹ ›` arrows, meal sections, add via `/food-search` or manual
- [ ] Workout: day picker, session list, new session, add sets (exercise picker from `/exercises`)
- [ ] Journal: day picker, measurements + water list + add
- [ ] Profile: view profile + edit, logout, delete account, export JSON
- [ ] Bottom navigation with 5 tabs (питание / тренировки / главная / журнал / профиль), home centered
- [ ] Horizontal pager for swipe between tabs (Compose `HorizontalPager`)
- [ ] Pull-to-refresh

### Phase 4 (distribution)
- [ ] Signed release APK (keystore outside repo)
- [ ] Serve at `https://health-tracker-bot.ru/download/trackhub.apk`
- [ ] `/download` landing page with side-load instructions + versioning
- [ ] Auto-update prompt (app checks version on startup, offers new APK download)

### Later
- [ ] Edit/delete records (parity with web after it's added there)
- [ ] Charts (Compose Canvas or `vico`)
- [ ] Notifications via Telegram bot (`notify.py` already exists)
- [ ] Reminders (local scheduling via WorkManager; notifications never include user data)
- [ ] Offline cache (Room, encrypted) — resync on reconnect
- [ ] Share-sheet integration (share food name from another app → add-food flow)

---

## Android home-screen widgets

All widgets respect the zero-knowledge model: they only run when the app has an
unlocked DEK in memory, otherwise they show "Unlock in TrackHub" and tap opens
the biometric prompt.

| Widget | Size | Behavior |
|---|---|---|
| **Water +250 ml** | 1×1 | Tap → log 250 ml for today, haptic feedback, number in corner shows today's total. Double-tap to enter custom ml. |
| **Meal quick-add** | 4×1 | Four tappable zones: 🍳 завтрак / 🥗 обед / 🍽 ужин / 🍎 перекус. Tap → launches app directly into the `/food-search` modal for that meal. |
| **Daily summary** | 2×2 | Read-only. Three rows: ккал сегодня (with TDEE %), макро split (Б/Ж/У), вода. Auto-refreshes every 15 min. |
| **Weight log** | 2×1 | Shows latest weight + delta vs 7 days ago. Tap opens small input dialog (numeric pad) to log new weight for today. |
| **Workout quick-start** | 2×1 | Shows last workout summary. Tap → create new workout session + open exercise picker. |
| **Calorie ring** | 2×2 | Circular progress toward TDEE. Empty center shows remaining ккал. Tap → food page. |
| **Combo** | 4×2 | Summary (left 2/4) + four action tiles (right 2/4: 🍳 / 🥗 / 🍽 / 💧). Most useful single widget. |

### Implementation notes
- Built with Glance (Jetpack Compose for widgets) — same Kotlin, not legacy RemoteViews.
- Widget updates triggered by app writes (WorkManager broadcast) + periodic (15 min default, 5 min when interactive).
- Taps that create records call the same `records` repository as the main app — DEK must be in memory; otherwise the widget shows a re-auth CTA.
- Daily-rollover: widgets read local-date at render time (no timezone bug, same fix as web).

### Icons / theming
- Match Material You dynamic colors (API 31+) via `GlanceTheme`.
- Monochrome variant for themed icons (API 33+).

---

## Bot (future)
- [ ] Notification scheduler (daily reminder if no food logged by 20:00 etc) — bot sends plaintext "не забудь записать ужин"
- [ ] `/link` command to attach `telegram_chat_id` to an account so notifications can find the right user
- [ ] `/stats` command → generic info only (uptime, user count), never user data
