# Changelog

All notable changes to MobileCLI Pro are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [6.0.0] - 2026-02-05 (All API Scripts Fixed)

### Fixed (Critical)

- **All 32 Broken API Scripts** — Converted remaining scripts from direct `am broadcast` calls to file-based IPC system. Android 10+ blocks broadcasts from background shell processes; the file-based system uses TermuxService (foreground) to execute commands with proper app permissions.

### Fixed Scripts

| Category | Scripts |
|----------|---------|
| Bluetooth (5) | bluetooth-connect, bluetooth-enable, bluetooth-info, bluetooth-paired, bluetooth-scaninfo |
| Camera (1) | camera-photo |
| SAF (8) | saf-create, saf-dirs, saf-ls, saf-mkdir, saf-read, saf-rm, saf-stat, saf-write |
| Media (3) | media-player, media-scan, microphone-record |
| SMS/Phone (2) | sms-send, telephony-call |
| Network (1) | wifi-enable |
| Power (2) | wake-lock, wake-unlock |
| IR (1) | infrared-transmit |
| Other (9) | dialog, download, job-scheduler, keystore, keystore-list, share, speech-to-text, storage-get, wallpaper |

### Changed

- **Version:** 6.0.0 (versionCode 600)
- **BOOTSTRAP_VERSION:** mobilecli-v6.0.0 (forces script regeneration)
- **CLAUDE.md:** Updated to v6.0.0

### Technical Details

All 62 termux-api scripts now use the file-based IPC pattern:
1. Script writes command to `~/.termux/am_command`
2. TermuxService (foreground) polls and executes
3. Result written to `~/.termux/api_result_<pid>`
4. Script reads result and exits

See `API_FIX_DOCUMENTATION.md` for full technical details.

---

## [5.7.0] - 2026-02-04 (Critical Security & Stability)

### Fixed (Critical)

- **E-C1: Godot /tmp Path Fix** — Changed `/tmp/godot.zip` to `/var/tmp/godot.zip` in Godot install scripts. The `/tmp` directory doesn't exist inside proot-distro Ubuntu; this was causing Godot installation to fail silently.

- **Q-C1: switchToSession Race Condition** — Captured snapshot of sessions list before bounds check and array access. Previously, sessions could be modified between the check and access, causing IndexOutOfBoundsException crashes.

- **Q-C2: CopyOnWriteArrayList Iteration+Clear Bug** — Changed `sessions.forEach { ... }; sessions.clear()` to a `while (sessions.isNotEmpty()) { sessions.removeAt(0)... }` pattern. The previous pattern could leak sessions added during iteration.

- **Q-C3/Q-C4: Thread Safety Annotations** — Added `@Volatile` to `currentSessionIndex` and `isWakeLockHeld` in TermuxService. These fields are accessed from multiple threads (UI thread, service callbacks, coroutines) and required visibility guarantees.

- **Q-C5: Process.destroyForcibly()** — Replaced `process.destroy()` with `process.destroyForcibly()` in SetupService timeout handling. The `destroy()` method doesn't guarantee process termination; `destroyForcibly()` (API 26+) sends SIGKILL.

### Changed

- **Version:** 5.7.0 (versionCode 570)
- **CLAUDE.md:** Updated to v5.7.0

### Technical Details

| File | Change |
|------|--------|
| `BootstrapInstaller.kt:3397,3433` | `/tmp/godot.zip` → `/var/tmp/godot.zip` |
| `MainActivity.kt:715-720` | Snapshot sessions before iteration |
| `TermuxService.kt:76` | `@Volatile` on isWakeLockHeld |
| `TermuxService.kt:482` | `@Volatile` on currentSessionIndex |
| `TermuxService.kt:383-391` | While loop instead of forEach+clear |
| `SetupService.kt:234` | destroyForcibly() instead of destroy() |

### Notes

- All fixes are conservative hardening — they add safety guards without changing behavior
- Q-C6 (null check in launchAI) was already implemented in v5.6.0
- No changes to authentication, payments, or permissions
- bashrc version unchanged (V13) — no environment changes

### What's NOT Changed (Documented Only)

These audit findings were reviewed but intentionally NOT changed:

- **OAuth State Validation** — Working fine, changing risks breaking login
- **Deep Link Scheme** — Needs assetlinks.json on domain first
- **Supabase Anon Key** — Designed for client-side use, protected by RLS
- **CSRF Tokens** — PayPal handles server-side
- **Clipboard Cap** — 8KB is appropriate for developers

---

## [5.6.0] - 2026-02-04 (Java 17 Fix for Android Builds)

### Fixed

- **Java 21 Conflict Resolution** — The `gradle` and `d8` packages pull openjdk-21 as a dependency, causing Android build failures. This release:
  - Sets JAVA_HOME to Java 17 in .bashrc generation
  - Updates install-dev-tools script to export JAVA_HOME after package installation
  - Updates getEnvironment() to include JAVA_HOME in process environment
  - Updates selfmod.sh to prefer Java 17 over auto-detected Java

### Changed

- **bashrc version:** V12 → V13 (forces regeneration on existing installs)
- **Version:** 5.6.0 (versionCode 560)

### Technical Details

| Component | Before | After |
|-----------|--------|-------|
| JAVA_HOME in .bashrc | Not set | `$PREFIX/lib/jvm/java-17-openjdk` |
| ANDROID_HOME in .bashrc | Not set | `$HOME/android-sdk` |
| install-dev-tools | No JAVA_HOME export | Exports JAVA_HOME after pkg install |
| selfmod.sh | Dynamic detection | Prefers Java 17 |

### Notes

- Both Java 17 and Java 21 remain installed (gradle dependency)
- Java 17 is forced for Android builds via JAVA_HOME
- All v5.5.0 TTS fixes and v5.4.0 features included
- No changes to authentication, payments, or permissions

---

## [5.5.0] - 2026-02-04 (TTS Fix & API Stability)

### Fixed

- **TTS Function Fix** — Replaced `Thread.sleep(500)` blocking call with proper callback-based initialization using `CountDownLatch`. TTS now waits for engine initialization before speaking.
- **TTS Resource Leak Fix** — Added `tts.shutdown()` call after speaking to properly release TTS resources.
- **TTS Completion Callback** — Added `OnUtteranceProgressListener` to know when speech completes before releasing resources.

### Changed

- **Version:** 5.5.0 (versionCode 550)
- **VERSION.md:** Updated to reflect v5.5.0

### Notes

- All v5.4.0 features and v5.3.0 security fixes are included
- TTS fix improves reliability of `tts-speak` hardware command
- New repository: MobileCLI-Pre-Release-5.5

---

## [5.4.0] - 2026-02-03 (OpenRouter CLI Integration)

### Added
- **OpenRouter CLI** — Access 100+ AI models (Claude, GPT-4, Llama, Mistral, Gemma, and more) via unified API
- New navigation drawer button for OpenRouter CLI installation
- One-click install via npm with automatic setup

### Changed
- **Total CLIs:** 22 (up from 21)
- **AI Assistants:** Now 4 CLIs (Claude, Gemini, Codex, OpenRouter)
- **CLAUDE.md:** Updated to v5.4.0 with all 22 CLIs
- **Version:** 5.4.0 (versionCode 540)

### Notes
- OpenRouter requires an API key from https://openrouter.ai
- Provides access to models from OpenAI, Anthropic, Meta, Mistral, Google, and more
- All v5.3.0 security fixes are included
- New repository: MobileCLI-v5.4.0-OpenRouter

---

## [5.3.0] - 2026-02-03 (Security Hardening Release)

### Security Fixes (CRITICAL)

- **PayPal Webhook Signature Verification** — Webhooks now verify PayPal signatures before processing. Invalid signatures return HTTP 403. Prevents fraudulent subscription activation.
- **TermuxApiReceiver Path Traversal Fix** — Added path validation to prevent arbitrary file writes via `resultFile` parameter. Only allows writes to app-owned directories and `/sdcard/`.

### Security Fixes (HIGH)

- **OAuth State Parameter** — Added CSRF protection via cryptographically secure state parameter with constant-time validation.
- **Password Minimum Increased** — Changed from 6 to 8 characters minimum.
- **Release Minification Enabled** — APK now minified and obfuscated with ProGuard.

### Security Fixes (MEDIUM)

- **License Cache Reduced** — Changed from 30 days to 7 days. Cancelled subscriptions detected faster.
- **PII Removed from Logs** — Email addresses and user IDs only logged in debug builds.
- **Network Security Config** — Added HTTPS-only enforcement and certificate pinning for Supabase.
- **.gitignore Added** — Prevents committing secrets (keystores, credentials, env files).

### Added

- `SECURITY_AUDIT_v5.3.0.md` — Full security audit results
- `AUTH_FLOW_DOCUMENTATION.md` — Authentication flow details
- `PAYMENT_FLOW_DOCUMENTATION.md` — Payment system documentation
- `SECURITY_BEST_PRACTICES.md` — Security guidelines
- `VULNERABILITY_REPORT.md` — How to report security issues
- `network_security_config.xml` — HTTPS/certificate pinning

### Changed

- **Version:** 5.3.0 (versionCode 530)
- **CLAUDE.md:** Updated to v5.3.0

### Notes

- **No breaking changes** — All fixes are backward compatible
- **Supabase function redeployment required** — Update paypal-webhook with new signature verification
- New repository: MobileCLI-v5.3.0-Security-Hardening

---

## [5.2.0] - 2026-02-03 (More Deployment CLIs)

### Added
- **Fly.io CLI** — Deploy and run apps globally with automatic scaling, browser-based auth
- **Convex CLI** — Realtime backend with TypeScript, database, serverless functions, browser-based auth

### Changed
- **Total CLIs:** 21 (up from 19)
- **CLAUDE.md:** Updated to reflect all 21 available CLIs
- **Version:** 5.2.0 (versionCode 520)

### Notes
- Both new CLIs support one-click browser-based authentication
- Tested and verified working on ARM64 Android
- New repository: MobileCLI-v5.2.0-Extended-CLIs

---

## [5.1.0] - 2026-02-03 (Extended CLIs Release)

### Added
- **7 New CLI Integrations** — Netlify, EAS (Expo), Shopify, Heroku, Twilio, Sanity, and Contentful
- **Netlify CLI** — Deploy sites, serverless functions, edge handlers
- **EAS CLI** — Build, submit, and update React Native/Expo apps with SSO login
- **Shopify CLI** — Build Shopify apps, themes, and storefronts
- **Heroku CLI** — Deploy, manage, and scale apps on Heroku platform
- **Twilio CLI** — Manage SMS, voice, and communication APIs
- **Sanity CLI** — Manage headless CMS content and schemas
- **Contentful CLI** — Manage headless CMS content, spaces, and environments

### Changed
- **Total CLIs:** 19 (up from 12)
- **CLAUDE.md:** Updated to reflect all 19 available CLIs
- **Version:** 5.1.0 (versionCode 510)

### Notes
- All new CLIs follow the same installation pattern: AlertDialog → createSession → safeSessionWrite
- Each CLI button opens a new terminal tab and runs install + login commands
- New repository: MobileCLI-v5.1.0-Extended-CLIs (version-controlled separately)

---

## [4.0.0] - 2026-02-02 (February 2026 Stable Release)

### Changed
- **Version bump** to 4.0.0 (versionCode 200) — first major stable release
- Fixed hardcoded "Version 3.1.21" in navigation drawer to "Version 4.0.0"
- Updated all documentation files (CLAUDE.md, VERSION.md, CURRENT_STATE.md, CHANGELOG.md, README.md)

### Notes
- No Kotlin source code changes — code is battle-tested from v3.1.25
- Package name remains `com.termux` for OAuth/payment/API compatibility
- All 13 CLI tools present in drawer (Claude, Gemini, Codex, Vercel, GitHub, Supabase, Godot, HuggingFace, Slack, Telegram, Discord, Firebase)
- Feature freeze — stability is the goal

---

## [3.1.25] - 2026-02-02 (Crash Recovery)

### Fixed
- **BASHRC V10** — session cleanup, orphaned process kill, /tmp fix, lock cleanup
- Crash recovery improvements for terminal session lifecycle
- Orphaned background processes properly cleaned up on session exit

### Changed
- versionCode 164, versionName 3.1.25

---

## [3.1.24] - 2026-02-02 (Session Safety)

### Changed
- **Nullable `createSession()`** — returns null instead of crashing when session creation fails, with null-check at every call site
- **MAX_SESSIONS = 30** — prevents unbounded session creation
- **Shell init delay 1200ms** — ensures shell is fully initialized before writing commands

### Changed
- versionCode 163, versionName 3.1.24

---

## [3.1.23] - 2026-02-02 (Firebase CLI Integration)

### Added
- **Firebase CLI** drawer button — installs Firebase tools via npm, opens new terminal tab

### Changed
- versionCode 162, versionName 3.1.23

---

## [3.1.22] - 2026-02-01 (Setup Reliability Hardening)

### Fixed
- **Setup reliability** — hardened SetupService foreground service for more reliable bootstrap downloads

### Changed
- versionCode 161, versionName 3.1.22

---

## [3.1.21] - 2026-02-01 (Legal & Licensing Overhaul)

### Changed
- **SetupWizard Stage 0:** Complete rewrite of legal text — now discloses Supabase, Stripe, PayPal, AI providers (Anthropic, Google, OpenAI), 79 permissions, links to full privacy policy and terms
- **Privacy Policy dialog:** Rewritten to accurately describe account data (Supabase), payment data (Stripe/PayPal), device data (stays on device), AI assistants, user rights (CCPA)
- **Open Source Licenses dialog:** Added copyright holders (Jack Palevich, Fredrik Fornwall), runtime packages note, Termux trademark disclaimer

### Legal Documentation
- **PRIVACY_POLICY.md:** Full rewrite — 79 permissions disclosure by category, Stripe as payment processor, AI providers (Anthropic, Google, OpenAI), expanded CCPA/GDPR sections, fixed data breach clause (removed "not liable for breaches")
- **TERMS_OF_SERVICE.md:** Added Stripe to payment processors, documented 7-day free trial, narrowed service refusal to specific causes, added AI providers to third-party services, California governing law with venue clause, open source acknowledgment section
- **LEGAL_DISCLAIMERS.md:** Major overhaul — removed unenforceable clauses (blanket waiver of right to sue, $100 liability cap, $50K liquidated damages, criminal prosecution threats, waiver of fair use defense, waiver of unconscionability claims, blanket data breach liability waiver), added 30-day arbitration opt-out, small claims exception, California governing law, force majeure
- **LICENSE:** Rewritten — removed "NO LICENSE GRANTED" contradiction, clear proprietary license allowing APK use and source viewing
- **LICENSE.md:** Rewritten — removed criminal penalty threats ($250K fines, imprisonment), $50K liquidated damages, DMCA/criminal fraud claims; replaced with standard civil enforcement (17 U.S.C. § 501)
- **THIRD_PARTY_LICENSES.md:** Added detailed runtime downloads table (bash, coreutils, Python, Node.js, Rust) with GPL compliance note
- **README.md:** Fixed dead links (removed LEGAL_SUMMARY.md and IP.md), added LEGAL_DISCLAIMERS.md/PRIVACY_POLICY.md/TERMS_OF_SERVICE.md links, updated privacy claim, added Termux compatibility section

### Added
- **NOTICE** file (Apache 2.0 requirement) — attribution for terminal-view, terminal-emulator, AndroidX, Kotlin, Supabase SDK

### Version
- versionCode 161, versionName 3.1.21

### Notes
- Package name `com.termux` unchanged — required for OAuth deep links, payment callbacks, API broadcasts, and existing user data
- Package name migration planned for future release
- No changes to any functional code — all changes are display text and documentation only

---

## [3.1.20] - 2026-02-01 (Security Hardening + Download Reliability)

### Security
- **C-5:** Payment deeplink log redaction — no more token/parameter logging in PaywallActivity
- **H-1:** Remove EncryptedSharedPreferences plaintext fallback — delete corrupted prefs and retry, crash if still fails (never store secrets in plaintext)
- **H-2:** Active subscriptions require server re-verification every 30 days — cancelled subs no longer valid forever locally
- **H-3:** Trial clock manipulation detection — stored absolute start time, forward-only time check
- **H-4:** BootReceiver locked with `RECEIVE_BOOT_COMPLETED` permission — external apps can no longer trigger it
- **H-5:** GDPR account deletion — clears local license data, signs out from Supabase, redirects to login (replaces "email us" toast)
- **M-1:** Thread-safe Supabase client — replaced manual singleton with Kotlin `by lazy` (synchronized)
- **L-1:** Removed dead `licenseKey` field from LicenseInfo data class
- **L-4:** Removed dead `getGoogleOAuthUrl()` method from SupabaseClient

### Added
- **SetupService** foreground service — downloads survive app backgrounding/destruction
- Persistent notification shows download progress
- Wake lock + WiFi lock held by service, not activity

### Fixed
- Setup download no longer dies when user navigates away from SetupWizard
- `onBackPressed` toast now accurately reflects that setup continues in background

### Changed
- versionCode 160, versionName 3.1.20

### Notes
- H-6 (security-crypto alpha06): Kept — no stable release exists, widely used in production
- H-8 decision documented

---

## [3.1.19] - 2026-02-01 (Security Patch: C-2, C-3, C-4)

### Security
- **C-2: Stop logging OAuth authorization codes** — `LoginActivity.kt` and `SupabaseClient.kt` now log only `scheme://host` instead of full URI containing secret auth codes
- **C-3: Validate URLs before opening** — URL watcher in `MainActivity.kt` now only opens `http://` and `https://` schemes, blocking dangerous `intent://`, `file://`, `content://` schemes
- **C-4: Lock down TermuxApiReceiver** — Added `android:permission="com.termux.permission.RUN_COMMAND"` so external apps can no longer exploit 79 permissions via broadcast

### Changed
- versionCode 159, versionName 3.1.19

---

## [3.1.18] - 2026-02-01 (Slack CLI Integration)

### Added
- **Slack CLI** drawer button — installs `slack-cli` and `slackclient` via pip, creates interactive `slack-setup` script that guides user through Slack Bot Token setup
- Follows identical pattern to all other CLI installers (new tab, bypassDebounce, captured session)

### Changed
- versionCode 158, versionName 3.1.18

---

## [3.1.17] - 2026-01-31 (Hide Unconfigured OAuth Providers)

### Changed
- **Hidden Apple, Microsoft, Discord buttons** on login screen — providers not yet configured in Supabase
- Login screen now shows: Google + GitHub (both working and configured)
- All hidden provider code remains intact for future re-enablement
- versionCode 157, versionName 3.1.17

---

## [3.1.16] - 2026-01-31 (Multi-Provider OAuth Login)

### Added
- **GitHub OAuth** login button — opens browser for GitHub authentication
- **Apple OAuth** login button — opens browser for Apple authentication
- **Microsoft OAuth** login button (Azure) — opens browser for Microsoft authentication
- **Discord OAuth** login button — opens browser for Discord authentication
- "Sign in to continue" label above provider buttons
- Generic `loginWithOAuth()` method handles all providers uniformly via Supabase SDK

### Changed
- Login screen now shows 5 OAuth provider buttons (Google, GitHub, Apple, Microsoft, Discord)
- Email/password fields remain hidden (available for future re-enablement)
- versionCode 156, versionName 3.1.16

### Note
- Each provider must be enabled in Supabase Dashboard → Authentication → Providers
- Each provider requires an OAuth app created in their respective developer console
- All providers use the same deep link callback: `com.termux://login-callback`

---

## [3.1.15] - 2026-01-31 (Google OAuth Only Login)

### Changed
- **Login screen simplified to Google OAuth only** — hidden email input, password input, Log In button, and Create Account button
- Google "Continue with Google" button is the sole login method visible to users
- Email/password auth code remains intact in `LoginActivity.kt` for future re-enablement (just set `android:visibility="visible"` on the hidden elements)
- Hidden "or continue with" divider — no longer needed with single auth method
- versionCode 155, versionName 3.1.15

### Reverted
- v3.1.14 incorrectly hid Google button instead of email/password — this version corrects it

---

## [3.1.13] - 2026-01-31 (Session Race Conditions + Hardcoded Versions)

### Fixed
- **Session race conditions in 8 functions** — `session?.write()` after coroutine delay was unsafe because `session` is a computed property that re-evaluates `currentSessionIndex`. If user switched tabs during the delay, command would write to wrong session. Now captures `val targetSession = session` immediately after `createSession()` in: `launchAI`, `togglePowerMode`, `installVercelCLI`, `installGitHubCLI`, `installSupabaseCLI`, `installHuggingFaceCLI`, `reinstallAITools`, `installDeveloperTools`
- **Power Mode drawer not closing** — drawer stayed open after launching Power Mode. Added `drawerLayout.closeDrawers()`
- **Hardcoded "Version 2.0.0"** in About dialog (`showAbout()`) and SettingsActivity — now reads from `packageManager.getPackageInfo()` dynamically
- versionCode 153, versionName 3.1.13

---

## [3.1.12] - 2026-01-31 (Double-Tab Fix + Tool Installer Session Reliability)

### Fixed
- **Double-tab on first launch** — When user selected Claude from Setup Wizard, `onServiceConnected()` created a blank session AND THEN `launchAI()` created a second session with Claude. Now skips blank session when `pendingAILaunch` is set
- **Tool installer session reliability** — Vercel, GitHub, Supabase, Godot, and Hugging Face installers now use `bypassDebounce = true`. Previously could silently lose their session creation if called within 500ms debounce window, causing commands to write to wrong session

### Changed
- versionCode 152, versionName 3.1.12

---

## [3.1.11] - 2026-01-31 (Power Mode + Claude CLI Clean Separation)

### Fixed
- **Power Mode + Claude CLI shared state bug** — `~/.termux/power_mode` file was written by Power Mode button but never deleted, causing ALL `claude` calls (from bashrc `claude()` function) to silently add `--dangerously-skip-permissions`
- Removed file-based `power_mode` approach entirely — no more hidden persistent state
- Power Mode button now explicitly sends `claude --dangerously-skip-permissions` in a new session (no file needed)
- Claude CLI button sends plain `claude` in a new session
- Legacy `power_mode` file is cleaned up if it exists
- Bashrc `claude()` simplified to just `command claude "$@"` — no file checks

### Changed
- Bashrc version marker bumped V6→V7 to force regeneration on existing installs
- versionCode 151, versionName 3.1.11

---

## [3.1.10] - 2026-01-31 (Power Mode Button Fix)

### Fixed
- **Power Mode not opening new tab** — `togglePowerMode()` was putting the command into the current session instead of creating a new one
- **Power Mode not using --dangerously-skip-permissions** — was sending plain `claude` instead of `claude --dangerously-skip-permissions`
- Rewrote `togglePowerMode()` to match `launchAI()` pattern: createSession → delay 800ms → write command

### Changed
- versionCode 150, versionName 3.1.10

---

## [3.1.9] - 2026-01-31 (Claude CLI Fix)

### Fixed
- **CRITICAL: Claude CLI broken by proot wrapper** — `claude()` bash function used proot which caused `execve: Function not implemented` on the claude Node.js script. Reverted to original `command claude` form that worked for 25+ days
- Bumped bashrc version marker V5→V6 to force regeneration on existing installs
- versionCode bumped to 149 to install over on-device builds (146-148)

### Changed
- `claude()` function no longer uses proot — relies on `TMPDIR=$PREFIX/tmp` for temp file handling
- `cc` wrapper still available with proot for users who specifically need `/tmp` bind mount

---

## [3.1.7] - 2026-01-30

### Fixed
- Attempted fix: `$(which claude)` to resolve binary path for proot (still broken — proot itself can't execute Node.js scripts)
- `createSession()` debounce blocking `launchAI()` — added `bypassDebounce` parameter

---

## [3.1.6] - 2026-01-30

### Fixed
- Drawer version display: was hardcoded "1.8.1", now reads from BuildConfig
- Crash: `onServiceConnected()` on destroyed activity — added `isFinishing`/`isDestroyed` guard
- Memory leak: ViewTreeObserver keyboard listener now removed in `onDestroy()`
- PayPal/Stripe HTTP connection leaks — added `connection.disconnect()` and `use {}` blocks
- Payment polling thread safety — `isPolling` now `@Volatile`
- Payment polling timeout — increased from 30s to 90s
- `launchAI()` race condition — session captured after delay
- Rapid session creation crash — added 500ms debounce to `createSession()`
- Wake lock toast message was inverted

---

## [3.1.5] - 2026-01-30

### Fixed
- Bashrc now regenerates on app update via `MOBILECLI_BASHRC_V4` version marker
- Power Mode now uses bashrc `claude()` function for proot /tmp fix

---

## [3.1.4] - 2026-01-30

### Added
- `TMPDIR=$PREFIX/tmp` export in bashrc
- `SHELL`, `BROWSER`, `COLORTERM`, `ANDROID_DATA`, `ANDROID_ROOT`, `EXTERNAL_STORAGE` env vars in bashrc
- proot /tmp bind mount in `claude()` function (later reverted in v3.1.9)

### Fixed
- Zoom crash: divide-by-zero guard in `updateTerminalSize()`
- Zoom crash: 50ms throttle on `onScale()` preventing rapid-fire reflection calls

---

## [3.1.3] - 2026-01-30

### Added
- Automatic conversation history backup to `/sdcard/MobileCLI/` (survives app uninstall)
- Automatic restore on fresh install from sdcard backup
- Covers `~/.claude/`, `~/.gemini/`, `~/.codex/`

---

## [3.1.2] - 2026-01-30

### Added
- Comprehensive `CODEBASE_REFERENCE.md` documentation (1,200+ lines)

---

## [3.1.1] - 2026-01-30

### Fixed
- Claude/Gemini/Codex drawer buttons now open new terminal tabs
- Claude drawer runs plain `claude` (Power Mode is separate toggle)
- Godot auto-installs proot-distro before launching

### Added
- WiFi lock (`WIFI_MODE_FULL_HIGH_PERF`) for download reliability
- Retry + resume for `downloadBootstrap()` (3 retries, exponential backoff, HTTP Range)
- 10-minute timeout + 3 retries for `runBashCommand()`
- 5-minute timeout for `runLoginShell()`
- Cleanup on install failure (delete partial zip + version marker)

---

## [3.1.0] - 2026-01-30 (Pre-Release)

### Added
- **cc wrapper script** — proot-based wrapper that maps `$PREFIX/tmp` to `/tmp`, fixing Claude Code "permission denied" errors on Android
- **Godot Engine integration** — proot-distro Ubuntu wrapper scripts (`godot4`, `install-godot`) for running Godot 4.4-stable ARM64
- **Expanded package installation** in SetupWizard:
  - Build tools: rust, clang, cmake, make, pkg-config, binutils
  - CLI tools: ripgrep, fzf, bat, eza, fd, ncdu, tree, unzip
  - Media tools: ffmpeg, imagemagick
  - Utilities: rclone, sqlite, scons, proot, proot-distro
- **Direct CLI launch buttons** in navigation drawer:
  - Claude CLI, Gemini CLI, Codex CLI (direct launch in current session)
  - Hugging Face CLI (`pip install huggingface_hub` + login in new tab)
- **Hugging Face CLI** integration button in drawer

### Fixed
- **Thread safety**: `sessions` list in TermuxService changed from `mutableListOf` to `CopyOnWriteArrayList` — prevents ConcurrentModificationException (root cause of most crashes)
- **Touch event crash**: `dispatchTouchEvent()` wrapped in try-catch — accidental touches during terminal output no longer crash the app
- **Paste race condition**: `onPasteTextFromClipboard()` now has lifecycle guards, null safety, and try-catch — pasting while Claude Code is responding no longer crashes
- **Post-destroy callbacks**: `onDestroy()` now calls `uiHandler.removeCallbacksAndMessages(null)` and `super.onDestroy()` last — prevents handler callbacks firing after activity is destroyed
- **Lifecycle guards**: Added `isFinishing`/`isDestroyed` checks to `onTextChanged`, `onSessionFinished`, `onColorsChanged`, `onCopyTextToClipboard`, `onPasteTextFromClipboard`
- **Session cleanup**: TermuxService `onDestroy()` wraps each session finish in individual try-catch

### Changed
- Navigation drawer reorganized: removed separate "Game Engines" section, all tools in one list
- Version bumped from 3.0.0 (versionCode 138) to 3.1.0 (versionCode 139)
- Progress percentages in SetupWizard adjusted for new package install steps

---

## [3.0.0] - 2026-01-28 (Stripe Integration)

### Added
- **Stripe payment integration** alongside PayPal via Supabase Edge Functions
- Supabase Edge Function: `create-checkout-session` for Stripe payments
- Supabase Edge Function: `stripe-webhook` for payment confirmation
- JWT authentication for Edge Functions
- CORS configuration for mobile app requests

### Security
- **JWT auth hardening** — Edge Functions validate JWT tokens
- **CORS policy** — Restricted to authorized origins
- **Row Level Security (RLS)** — Supabase database policies for subscriptions
- Added `apikey` header to all Edge Function calls
- Re-added `--no-verify-jwt` to edge function deploys for proper routing

### Fixed
- Edge function checkout failure (missing apikey header)
- Duplicate subscription bug (6x charge prevention)

---

## [2.1.0] - 2026-01-26 (PayPal Release)

### Added
- Initial public release with PayPal subscription integration
- Complete authentication system (email/password, Google OAuth)
- 79 Android permissions for root-equivalent access
- Terminal emulator with multi-session support
- AI integration (Claude Code, Gemini CLI, Codex CLI)
- Developer Mode with 7-tap activation
- Self-modification loop (rebuild own APK from within)
- 75+ Termux API commands for hardware access

---

## [2.0.0-rc.3] - 2026-01-25 (Release Candidate 3)

### Fixed
- AccountActivity delete account email changed from `support@mobilecli.com` to `mobiledevcli@gmail.com`

### Changed
- All support email references now use correct email address

---

## [2.0.0-rc.2] - 2026-01-25 (Release Candidate 2)

### Fixed
- Support email changed from `support@mobilecli.com` to `mobiledevcli@gmail.com`

### Added
- Webhook logging to `webhook_logs` table for debugging payment issues
- Payment history recording to `payment_history` table for audit trail
- Processing result tracking in webhook handler

### Changed
- PayPal webhook now logs all events before processing
- Webhook marks events as processed with result status

---

## [2.0.0-rc.1] - 2026-01-22 (Release Candidate)

### Security
- **CRITICAL FIX**: Removed "Skip Login (Demo Mode)" button that bypassed authentication
- All users must now authenticate to access the app

### Added
- Complete subscription system with Supabase backend
- PayPal IPN webhook for automatic payment activation (`supabase/functions/paypal-webhook/`)
- Webhook setup documentation (`docs/WEBHOOK_SETUP.md`)
- Subscription setup documentation (`docs/SUBSCRIPTION_SETUP.md`)
- Terms of Service (`docs/TERMS_OF_SERVICE.md`)
- Privacy Policy (`docs/PRIVACY_POLICY.md`)
- Legal Disclaimers (`docs/LEGAL_DISCLAIMERS.md`)
- Row Level Security (RLS) for subscriptions table
- Automatic trial subscription (7 days) on signup
- Encrypted local caching for offline access

### Fixed
- java.time.Instant API 26+ requirement - now works on Android 7.0+ (API 24)
- Added registerDevice() backward compatibility method
- PayPal checkout return handling with proper delay

### Changed
- LicenseManager now queries Supabase subscriptions table
- PaywallActivity onResume increased delay for webhook processing
- Version bumped to Release Candidate

---

## [2.0.0-beta.34-build13] - 2026-01-22

### Fixed
- PayPal payment URL format changed to NCP format (`/ncp/payment/`)
- PayPal subscription now fully working with Business account

### Changed
- PayPal Button ID updated to `DHCKPWE3PJ684` (Business account button)

---

## [2.0.0-beta.34-build12] - 2026-01-22

### Changed
- Updated PayPal button to Business account button
- Updated all documentation with new button ID

---

## [2.0.0-beta.34-build11] - 2026-01-22

### Added
- Master configuration documentation (`docs/MASTER_CONFIG.md`)
- Comprehensive security guidelines

---

## [2.0.0-beta.34-build9] - 2026-01-22

### Added
- PayPal subscription integration ($15/month)
- PaywallActivity with subscribe button
- Payment documentation (`docs/PAYMENTS_SETUP.md`)

### Changed
- Checkout flow now uses PayPal instead of website redirect

---

## [2.0.0-beta.34-build7] - 2026-01-22

### Fixed
- Google OAuth PKCE flow - proper code exchange
- Deep link handling with `exchangeCodeForSession()`
- Auth configuration with scheme and host parameters

### Added
- Authentication documentation (`docs/AUTH_SETUP.md`)

---

## [2.0.0-beta.34-build6] - 2026-01-22

### Added
- Deep link intent-filter for OAuth callback
- `onNewIntent()` handler in LoginActivity
- OAuth callback URL: `com.termux://login-callback`

### Changed
- LoginActivity launch mode set to `singleTask`
- LoginActivity exported set to `true`

---

## [2.0.0-beta.34-build5] - 2026-01-22

### Added
- Google OAuth login button (enabled)
- Google provider configuration in Supabase

### Fixed
- Removed "Google login requires configuration" toast

---

## [2.0.0-beta.34-build4] - 2026-01-22

### Added
- GitHub Actions workflow for automated builds
- Automatic GitHub Releases with APK artifacts

### Changed
- CI workflow removes ARM-specific aapt2 override

---

## [2.0.0-beta.34-build3] - 2026-01-22

### Added
- "Skip for now" demo mode button on login screen

---

## [2.0.0-beta.34] - 2026-01-22

### Added
- Complete authentication system
  - LoginActivity with email/password
  - SignUp functionality
  - Google OAuth (initial setup)
- SupabaseClient singleton for auth management
- LicenseManager for subscription verification
- PaywallActivity for subscription prompts
- SplashActivity for initial loading

### Technical
- Supabase SDK integration (v2.0.4)
- Ktor client for Android
- PKCE OAuth flow support
- Encrypted SharedPreferences for license storage

---

## [1.x.x] - Previous Versions

### Original MobileCLI
- Terminal emulator functionality
- Termux integration
- Basic app structure

---

## Version Categories

### Added
New features added to the project.

### Changed
Changes in existing functionality.

### Deprecated
Features that will be removed in upcoming releases.

### Removed
Features removed in this release.

### Fixed
Bug fixes.

### Security
Security vulnerability fixes.

---

## Links

- [GitHub Releases](https://github.com/MobileDevCLI/MobileCLI-Pro/releases)
- [GitHub Repository](https://github.com/MobileDevCLI/MobileCLI-Pro)
# Deploy trigger
# v2.1.2 deploy
