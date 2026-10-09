# Scrolly — Audit, Checklist & Verification Record

Baseline: `main` @ `ea17740` (a clean revert of `9fa5ac6`). Work branch: `scrolly/ws1-data-correctness` (local, uncommitted).
Patch: `scrolly-ws1-data-correctness.patch` — verified to `git apply` cleanly to a fresh clone of `main`.

## 0. Verification status — read this first

| Check | Result |
|---|---|
| `./gradlew build` / `testDebugUnitTest` / `lint` | **NOT RUN.** Sandbox cannot reach Google Maven, Maven Central, Gradle plugin portal, `services.gradle.org` or `dl.google.com` (`x-deny-reason: host_not_allowed`). No Android SDK present. |
| Repo has no `gradlew`, `gradlew.bat` or `gradle-wrapper.jar` | Pre-existing (removed by the revert). Only `gradle-wrapper.properties` exists. |
| Pure-logic unit tests (`DateKeysTest`, `DailyCounterTest`) | **23/23 passed** — compiled with the real Kotlin 2.2.10 compiler, run with a *minimal stand-in for JUnit* (not real JUnit). |
| Mutation check | Re-introducing each original bug into a temp copy made the matching tests fail (4/4 mutants killed). |
| `TrackingRepository`, `ScrollyApp`, `StatsViewModel`, `HomeScreen`, `HomeViewModel`, `ScrollyDatabase` edits | **NOT COMPILED, NOT RUN.** Reviewed by hand only. A duplicate method and an invalid `@OptIn` target were caught that way; others may remain. |
| Emulator / device run | **NOT DONE.** |

## 1. Findings (each confirmed in `main` source, not inferred from commit messages)

### P0 — Correctness / data integrity
- [x] **Date keys depend on device locale.** `SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())` in `ScrollyDatabase` (4 helpers) and `StatsViewModel` (5 sites) + locale-less `String.format("%s-%02d")` (6 sites). Under `bn`/`ar`/`fa`/`ne`/`mr` the JVM emits non-ASCII digits (reproduced on JVM; Android device not tested) → history orphaned on language change; month/year `LIKE`/range queries return nothing. **Fixed:** `DateKeys` (Locale.US).
- [x] **Live counter stale after midnight** (`today` captured once in `init`). **Fixed:** day key advances at midnight (capped 15-min sleep); counter and Home flows re-subscribe via `flatMapLatest`.
- [x] **Live counter permanently inflated by one failed write** (incremented before write; only accepted increases from DB). **Fixed:** `DailyCounter` (`stored + pending`, rollback on failure).
- [x] **4 non-transactional DAO writes per scroll.** **Fixed:** single `withTransaction` (via `RoomTransactionRunner`; no schema change).
- [x] **`isBlocked` never cleared** after a new day / raised limit. **Fixed:** cleared when count < limit. *Note: nothing reads `isBlocked` yet — see P0 below.*
- [x] **"Live Scroll Tester" writes fake scrolls into real stats in all builds.** **Fixed:** gated by `BuildConfig.DEBUG`. Also fixed a double-count in `simulateScroll`.
- [ ] **Blocking has no enforcement.** `limitReachedEvents` and `isBlocked` have zero consumers; the "intervention" is an in-app preview dialog only (hard-coded "limit of 100", inert "Emergency Override" `Text`). Needs a product decision + device testing (accessibility overlay and/or `GLOBAL_ACTION_HOME`).
- [ ] **"Claim" is free and unlimited, always credits Instagram, and persists into `dailyLimit`** (`unblockAppTemporarily`) → repeated taps permanently raise the user's own cap; a >200 limit also exceeds the slider range (10..200). Needs a session/daily-scoped bonus (schema change + migration).
- [ ] **`fallbackToDestructiveMigration()` + `exportSchema=false`** → any future schema bump silently wipes all history. Needs explicit migrations + exported schema before *any* schema change.
- [ ] **`app_stats` uses a surrogate key**, so duplicate (date, package) rows are possible. Needs a natural key + migration that sums duplicates.
- [ ] **First-run seeding** runs via `INSTANCE?.scrollyDao()` and silently no-ops if `INSTANCE` is null.

### P0 — Fabricated production data (violates "no fake data")
- [ ] `UserProfile` defaults: streak 7, best streak 11, **14,832** total scrolls, 23 W / 17 L, level 8, XP 3450/4000 — only `username` is real. `ProfileScreen` also hard-codes `"14,832"`.
- [ ] Seeded fake friends ("Alex Rivera" 42, "Sarah Chen" 35) written to the user's DB on first run.
- [ ] `addFriendChallenge`: `userScrolls = 36` hard-coded, random opponent score, `date = "Today"` (literal), status always "WINNING". `recordBattleResult` is an empty stub. `BattlesScreen` hard-codes 36 / 93 and "Alex has scrolled 57 more…".
- [ ] Mascot states hard-coded (`scrollCount = 75`, `250`, `15`) on Block/Profile.
- [ ] Achievements are seeded but never progressed; `xpEarned` is never written; streaks/levels are never computed. Real derivations are straightforward from `daily_stats` — design needed for XP/level rules (none exist in code).

### P1 — Functional / robustness
- [ ] `Daily goal`: `setDailyGoal` has no callers; goal is effectively always 100 and a new day's row is created with `goal = 100` (would reset a user-set goal each midnight if a goal UI is added).
- [ ] `scroll_events` grows unbounded (1 row per scroll, never pruned).
- [ ] `getRecentDailyStatsFlow` returns last 7 *rows*, not last 7 *days*.
- [ ] Stats month view: only day-keys 1..daysInMonth, labels (`Jan`…`Dec`) hard-coded English.
- [ ] `ScrollyApp` uses `GlobalScope.launch` for Firebase sign-in; `updateUsername` writes prefs then calls Firebase without error handling.
- [ ] Permission/overlay state in `HomeViewModel` is read in `init`/`refreshPermissions()`; verify it is refreshed on `ON_RESUME` after returning from system settings.
- [ ] Release build: `isMinifyEnabled = false`; release signing config reads env vars and a keystore path that may not exist (build fails without them).
- [ ] CI absent; no Gradle wrapper scripts.

### P2 — Design (not started)
- [ ] Theme tokens exist (`Sleek*`), but corner radii (12/14/16/20/24/28), card borders and elevation are re-declared inline on every screen. Extract `ScrollyCard`, `MetricCard`, `SectionHeader`, `EmptyState`, `ErrorState` and radius/spacing tokens.
- [ ] Verify border/text contrast ≥ 3:1 / 4.5:1 in both themes; verify font-scale (200%) and inset behaviour on every screen.
- [ ] Add loading/empty/error states (Stats, Battles, Block currently only show `emptyList()` initial values).

## 2. Why the previous large commit was not simply re-applied
`9fa5ac6` changed ~5,000 lines across the whole app in one unverified commit and was reverted by the repo owner. I treated the revert as a deliberate decision and instead re-verified each claimed defect against current `main`, then re-implemented a small, independently tested subset.

## 3. What changed (this patch)
New: `data/local/DateKeys.kt`, `data/repository/DailyCounter.kt`, `DateKeysTest.kt`, `DailyCounterTest.kt`.
Modified: `TrackingRepository.kt`, `ScrollyDatabase.kt`, `StatsViewModel.kt`, `ScrollyApp.kt`, `HomeViewModel.kt`, `HomeScreen.kt` (debug-gate only; 5 real lines).
**No schema change, no migration, no new dependency, no new permission.**

## 4. Manual verification required (needs a machine with Gradle + Android SDK)
1. `gradle wrapper --gradle-version 9.3.1` (or open in Android Studio) to restore `gradlew`; then `./gradlew :app:assembleDebug :app:testDebugUnitTest`.
2. If compile errors appear, start with `TrackingRepository.kt` (new `flatMapLatest`/`@OptIn`/`withTransaction` usage).
3. Install a debug build; set device language to **Bengali (Bangladesh)**; scroll in Instagram Reels; confirm Stats shows the count. Switch language back to English and confirm history is still present.
4. Leave the app open across midnight (or change device date forward one day with the app in foreground): confirm Home counter and island show 0/1, not yesterday's total.
5. Confirm the Live Scroll Tester card is present in debug and **absent** in a release build.
6. Existing rows written under a non-English locale before this patch (if any) keep their non-ASCII keys; they are not migrated by this patch. Check with `adb shell run-as com.scrolly.health sqlite3 databases/scrolly_database "select distinct date from daily_stats"`.
