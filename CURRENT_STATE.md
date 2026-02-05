# MobileCLI Pro - Current State (For AI Context Recovery)

**Date:** February 2, 2026
**Status:** v4.0.0 Stable Release
**Version:** 4.0.0 (versionCode 200)

---

## LATEST WORK: v4.0.0 — February 2026 Stable Release

**Completed February 2, 2026**

### What Changed (v4.0.0)

**Version Bump & Documentation Cleanup:**
- versionCode 200, versionName 4.0.0
- Fixed hardcoded "Version 3.1.21" in drawer to "Version 4.0.0"
- Updated all documentation files (CLAUDE.md, VERSION.md, CURRENT_STATE.md, CHANGELOG.md, README.md)
- No Kotlin source code changes — code is battle-tested from v3.1.25

**This release includes everything from v3.1.22 through v3.1.25:**
- v3.1.22: Setup reliability hardening (SetupService foreground service)
- v3.1.23: Firebase CLI integration (drawer button + installer)
- v3.1.24: Session safety (nullable createSession, MAX_SESSIONS 30, shell init delay 1200ms)
- v3.1.25: Crash recovery (BASHRC V10, session cleanup, orphaned process kill, /tmp fix, lock cleanup)

---

## PREVIOUS: v3.1.21 — Legal & Licensing Overhaul

**Completed February 1, 2026**

### What Changed (v3.1.21)

**Legal Text Overhaul:**
- Updated SetupWizard Stage 0 legal text — full disclosure of Supabase, Stripe, PayPal, AI providers, and 79 permissions
- Updated Privacy Policy dialog in MainActivity — account data, device data, AI assistants sections
- Updated Open Source Licenses dialog — proper attribution with copyright holders, Termux trademark disclaimer

**Documentation Rewrite:**
- `docs/PRIVACY_POLICY.md` — Full rewrite: 79 permissions disclosure, Stripe added, AI providers listed, CCPA/GDPR expanded, data breach clause fixed
- `docs/TERMS_OF_SERVICE.md` — Stripe added, free trial documented, service refusal narrowed, California governing law, open source section
- `docs/LEGAL_DISCLAIMERS.md` — Major overhaul: removed unenforceable clauses ($50K damages, criminal threats, blanket sue waiver, $100 cap), added arbitration opt-out, small claims exception, force majeure, California law
- `LICENSE` — Rewritten: clear proprietary license, no "NO LICENSE GRANTED" contradiction, allows APK use and source viewing
- `LICENSE.md` — Rewritten: removed criminal penalty threats, $50K liquidated damages, DMCA/criminal fraud claims; replaced with standard civil enforcement
- `THIRD_PARTY_LICENSES.md` — Added detailed runtime downloads section with GPL compliance note
- `README.md` — Fixed dead links, updated privacy claim, added Termux compatibility section

**New Files:**
- `NOTICE` — Apache 2.0 compliance file with attribution for all open source components

**Version Bump:**
- versionCode 161, versionName 3.1.21 across 9 locations

---

## PREVIOUS: v3.1.20 — Security Hardening + Download Reliability

**Completed February 1, 2026**

### What Changed (v3.1.20)

**Security Fixes:**
- **C-5:** Payment deeplink log redaction (no more token logging)
- **H-1:** Remove EncryptedSharedPreferences plaintext fallback (delete+retry, crash if still fails)
- **H-2:** Active subscriptions require server re-verification every 30 days
- **H-3:** Trial clock manipulation detection (stored start time + forward-only check)
- **H-4:** BootReceiver locked with RECEIVE_BOOT_COMPLETED permission
- **H-5:** GDPR account deletion (clears local data, signs out, redirects to login)
- **M-1:** Thread-safe Supabase client (Kotlin `by lazy`)
- **L-1:** Removed dead `licenseKey` field from LicenseInfo
- **L-4:** Removed dead `getGoogleOAuthUrl()` method

**Download Reliability:**
- **NEW:** SetupService foreground service — downloads survive app backgrounding
- **FIX:** Setup no longer dies when user navigates away from SetupWizard
- Persistent notification shows download progress
- Wake lock + WiFi lock held by service, not activity

**Files changed:**
- `PaywallActivity.kt` — C-5 log redaction
- `LicenseManager.kt` — H-1 no plaintext fallback, H-2 active sub expiry, H-3 clock guard, L-1 remove licenseKey
- `AccountActivity.kt` — H-5 GDPR deletion
- `AndroidManifest.xml` — H-4 BootReceiver permission, SetupService registration, FOREGROUND_SERVICE_DATA_SYNC permission
- `SupabaseClient.kt` — M-1 thread-safe lazy, L-4 remove dead method
- `SetupService.kt` — NEW foreground service for downloads
- `SetupWizard.kt` — Refactored to bind to SetupService
- `build.gradle.kts` — version bump 159→160, 3.1.19→3.1.20
- `activity_main.xml` — version string bump

---

## PREVIOUS: v3.1.19 — Security Patch (C-2, C-3, C-4)

**Completed February 1, 2026**

### What Changed (v3.1.19)

**C-2: Stop logging OAuth authorization codes:**
- `LoginActivity.kt` line 262: OAuth callback now logs only `scheme://host` instead of full URI with auth code
- `SupabaseClient.kt` line 195: Deep link handler now logs only `scheme://host` instead of full URI

**C-3: Validate URLs before opening:**
- `MainActivity.kt` URL watcher: now only opens `http://` and `https://` URLs
- Blocks dangerous schemes (`intent://`, `file://`, `content://`) with warning log

**C-4: Lock down TermuxApiReceiver:**
- `AndroidManifest.xml`: Added `android:permission="com.termux.permission.RUN_COMMAND"` to TermuxApiReceiver
- External apps must now hold `RUN_COMMAND` permission to send API calls

**Files changed:**
- `LoginActivity.kt` — redacted OAuth callback log
- `SupabaseClient.kt` — redacted deep link log
- `MainActivity.kt` — added HTTP/HTTPS scheme validation to URL watcher
- `AndroidManifest.xml` — added permission requirement to TermuxApiReceiver
- `build.gradle.kts` — version bump 158→159, 3.1.18→3.1.19
- `activity_main.xml` — version string bump

---

## PREVIOUS: v3.1.18 — Slack CLI Integration

**Completed February 1, 2026**

### What Changed (v3.1.18)

**Added Slack CLI drawer button:**
- New "Slack CLI" item in navigation drawer (after Hugging Face CLI, before divider)
- `installSlackCLI()` function follows exact same pattern as all other CLI installers
- Opens new tab, runs: `pip install slack-cli slackclient --break-system-packages`
- Creates `~/.local/bin/slack-setup` interactive script for Bot Token configuration
- Runs `slack-setup` automatically after install

**Files changed:**
- `activity_main.xml` — added `nav_slack` TextView in drawer
- `MainActivity.kt` — added `installSlackCLI()` function + click listener in `setupDrawer()`

---

## PREVIOUS: v3.1.13 — Session Race Conditions + Hardcoded Versions

**Completed January 31, 2026**

### What Changed (v3.1.13)

**Session race conditions in 8 functions:**
- `session?.write()` after coroutine delay was unsafe — `session` is a computed property that re-evaluates `currentSessionIndex`
- If user switched tabs during delay, command wrote to wrong session
- Fix: capture `val targetSession = session` immediately after `createSession()`
- Fixed in: `launchAI`, `togglePowerMode`, `installVercelCLI`, `installGitHubCLI`, `installSupabaseCLI`, `installHuggingFaceCLI`, `reinstallAITools`, `installDeveloperTools`

**Power Mode drawer not closing:**
- Added `drawerLayout.closeDrawers()` to `togglePowerMode()`

**Hardcoded "Version 2.0.0":**
- About dialog and SettingsActivity now read version from `packageManager.getPackageInfo()` dynamically

---

## PREVIOUS: v3.1.12 — Double-Tab Fix + Tool Installer Session Reliability

**Completed January 31, 2026**

### What Changed (v3.1.12)

**Double-tab on first launch (Setup Wizard → Claude):**
- `onServiceConnected()` created a blank session, THEN `launchAI()` created a second one
- Fix: skip blank session creation when `pendingAILaunch` is set — `launchAI()` creates its own
- Result: single tab with Claude, no empty tab

**Tool installer session reliability:**
- Vercel, GitHub, Supabase, Godot, Hugging Face installers now use `bypassDebounce = true`
- Previously used default debounce (500ms), which could silently swallow session creation
- If called within 500ms of another session, command would be written to wrong (existing) session

---

## PREVIOUS: v3.1.11 — Power Mode + Claude CLI Button Clean Separation

**Completed January 31, 2026**

### What Changed (v3.1.11)

**Power Mode + Claude CLI — clean separation, no shared state:**
- Removed `power_mode` file-based approach entirely — the `~/.termux/power_mode` file was poisoning ALL claude calls with `--dangerously-skip-permissions` silently
- Claude CLI button: creates new session, sends plain `claude` command
- Power Mode button: creates new session, explicitly sends `claude --dangerously-skip-permissions`
- Bashrc `claude()` function simplified to just `command claude "$@"` — no file checks
- Power Mode button deletes legacy `power_mode` file if it exists (cleanup)
- Bashrc version marker bumped V6→V7 to force regeneration on all existing installs

### What Changed (v3.1.10)

**Power Mode button fix:**
- togglePowerMode() rewritten to create new session and explicitly send `claude --dangerously-skip-permissions`
- Matches launchAI() pattern: createSession → delay 800ms → write command
- Previously was not opening new tab and was not passing --dangerously-skip-permissions

### What Changed (v3.1.9)

**Critical Fix — Claude CLI broken by proot wrapper:**
- v3.1.4 wrapped `claude()` with proot to fix `/tmp` access — this broke Claude CLI
- proot cannot execute the claude Node.js script (execve fails: "Function not implemented")
- Reverted `claude()` to original working form: `command claude` (no proot)
- `TMPDIR=$PREFIX/tmp` export already handles /tmp for tools that respect it
- `cc` wrapper still available with proot for anyone who needs it
- Bashrc version marker bumped V5→V6 to force regeneration on all existing installs

### What Changed (v3.1.1-v3.1.7)
- **v3.1.7**: Attempted proot fix with `$(which claude)` — still broken
- **v3.1.6**: Fixed memory leak, payment connection leaks, session race conditions, drawer version display
- **v3.1.5**: Update-safe bashrc with version markers, Power Mode /tmp fix
- **v3.1.4**: Environment fixes (TMPDIR, SHELL, BROWSER, COLORTERM), zoom crash fix
- **v3.1.3**: Auto backup/restore conversation history to /sdcard/MobileCLI/
- **v3.1.2**: Added CODEBASE_REFERENCE.md documentation
- **v3.1.1**: Fixed drawer buttons, download retry/resume, command timeouts

### What Changed (v3.1.0 on top of v3.0.0 Stripe Integration)

**Crash Stability Fixes:**
- Thread-safe sessions list (CopyOnWriteArrayList in TermuxService)
- Touch event crash protection (try-catch in dispatchTouchEvent)
- Paste race condition fix (lifecycle guards + null safety)
- Handler cleanup in onDestroy (prevents post-destroy crashes)
- Lifecycle guards on all TerminalSessionClient callbacks

**New Features:**
- cc wrapper script (proot-based /tmp fix for Claude Code on Android)
- Godot Engine integration (proot-distro Ubuntu ARM64 wrapper)
- Expanded packages: rust, clang, cmake, ripgrep, fzf, bat, eza, fd, ffmpeg, imagemagick, proot, proot-distro
- Direct CLI launch buttons in drawer: Claude, Gemini, Codex, Hugging Face
- Hugging Face CLI integration (pip install huggingface_hub)

**Navigation drawer now shows:**
Install AI Tools → Claude CLI → Gemini CLI → Codex CLI → Vercel CLI → GitHub CLI → Supabase CLI → Godot Engine → Hugging Face CLI

---

## PREVIOUS WORK: Stripe Payment Integration (v3.0.0)

**Completed January 28, 2026**

### What Changed
Added Stripe as a second payment provider alongside PayPal. Users can now subscribe via:
- **Card payment** (Stripe Checkout) - new
- **PayPal** - existing, unchanged

### New Edge Functions Created
| Function | File | Purpose |
|----------|------|---------|
| `create-stripe-checkout` | `supabase/functions/create-stripe-checkout/index.ts` | Creates Stripe Checkout Session (subscription mode) |
| `stripe-webhook` | `supabase/functions/stripe-webhook/index.ts` | Handles Stripe webhooks with HMAC-SHA256 signature verification |
| `create-portal-session` | `supabase/functions/create-portal-session/index.ts` | Creates Stripe Customer Portal session for subscription management |

### Files Modified
| File | What Changed |
|------|-------------|
| `PaywallActivity.kt` | Added "Subscribe with Card" button, `openStripeCheckout()` method, `createStripeCheckoutSession()` API call |
| `LicenseManager.kt` | Added `stripe_subscription_id`, `stripe_customer_id`, `provider` to `Subscription` data class |
| `AccountActivity.kt` | Smart subscription management: Stripe Portal for Stripe users, PayPal autopay for PayPal users. Added `getSubscriptionProvider()`, `openStripePortal()`, `createPortalSession()` methods |
| `activity_paywall.xml` | Added Stripe button (purple #635BFF), "or" divider, updated PayPal button (blue #0070BA), auto-renewal disclosure text |
| `deploy-functions.yml` | Added deploy steps for 3 new edge functions |

### Files NOT Modified (Protected)
- BootstrapInstaller.kt, SetupWizard.kt, MainActivity.kt
- SupabaseClient.kt, LoginActivity.kt, SplashActivity.kt
- build.gradle.kts (no new dependencies - Stripe uses REST API)
- create-subscription/index.ts, paypal-webhook/index.ts (PayPal code untouched)

### Database Migration Required
Run in Supabase SQL Editor:
```sql
ALTER TABLE subscriptions
  ADD COLUMN IF NOT EXISTS stripe_subscription_id TEXT,
  ADD COLUMN IF NOT EXISTS stripe_customer_id TEXT,
  ADD COLUMN IF NOT EXISTS provider TEXT DEFAULT 'paypal';
```

### Supabase Secrets Required
Set via Supabase Dashboard > Edge Functions > Secrets:
- `STRIPE_SECRET_KEY` = `sk_test_...` (test mode first, then `sk_live_...`)
- `STRIPE_WEBHOOK_SECRET` = `whsec_...` (from Stripe webhook endpoint registration)
- `STRIPE_PRICE_ID` = `price_...` (from Stripe Products page)

### Stripe Webhook Registration (Manual)
1. Go to Stripe Dashboard > Developers > Webhooks > Add endpoint
2. URL: `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/stripe-webhook`
3. Events: `checkout.session.completed`, `invoice.paid`, `invoice.payment_failed`, `customer.subscription.updated`, `customer.subscription.deleted`
4. Copy signing secret (`whsec_...`) to Supabase secrets

---

## PREVIOUS WORK: Auth/Payment Flow Fixes

**Completed January 25, 2026**

### Changes Made
1. **Fixed support email** - Changed `support@mobilecli.com` to `mobiledevcli@gmail.com`
2. **Added webhook logging** - All PayPal events logged to `webhook_logs` table
3. **Added payment history** - All payments recorded to `payment_history` table
4. **Processing tracking** - Webhook marks events as processed with result status

### New Database Tables Required
Run in Supabase SQL Editor:
```sql
CREATE TABLE IF NOT EXISTS webhook_logs (...);
CREATE TABLE IF NOT EXISTS payment_history (...);
```
See full SQL in docs or commit message.

---

## PREVIOUS WORK: PayPal Documentation Archive

**Completed January 25, 2026**

Created complete PayPal integration documentation in `docs/paypal/`:

| File | Purpose |
|------|---------|
| `README.md` | Overview and quick start |
| `STORY.md` | Full development history |
| `SETUP_GUIDE.md` | Step-by-step setup from scratch |
| `WEBHOOK_CODE.md` | Working webhook with explanations |
| `DATABASE_SCHEMA.md` | All SQL needed |
| `TROUBLESHOOTING.md` | Common problems and solutions |
| `TEST_PAYLOADS.md` | How to test webhooks |

**Key Fix Documented:** Changed `.update()` to `.upsert()` in webhook to fix silent failures.

---

## LATEST APK

**File:** `/sdcard/Download/MobileCLI-v3.1.12.apk`

This APK includes everything from v2.0.0 through v3.1.12:
- Power Mode + Claude CLI button clean separation (no shared state)
- Claude CLI fix (removed proot wrapper, reverted to `command claude`)
- All auth/payment fixes (PayPal + Stripe)
- Crash stability fixes (thread safety, lifecycle guards)
- Memory leak fixes, payment connection leak fixes
- Conversation history backup/restore
- Update-safe bashrc with version markers (V7)
- cc wrapper, expanded packages, Godot integration
- Direct CLI launch buttons (Claude, Gemini, Codex, Hugging Face)
- Full drawer reorganization

---

## APK VERSION HISTORY (For Revert)

| Version | File | Changes |
|---------|------|---------|
| **v2.0.0-rc.3** | `MobileCLI-Pro-v2.0.0-rc.3.apk` | All email fixes complete |
| v2.0.0-rc.2 | `MobileCLI-Pro-v2.0.0-rc.2.apk` | Support email fix, webhook logging |
| v2.0.8-BACKFIX | `MobileCLI-Pro-v2.0.8-BACKFIX.apk` | Back button navigation fix |
| v2.0.7-BROWSER-OAUTH | `MobileCLI-Pro-v2.0.7-BROWSER-OAUTH.apk` | Browser-based Google OAuth with PKCE |
| v2.0.6-STABLE | `MobileCLI-Pro-v2.0.6-STABLE.apk` | Crash loop fix, stable |
| v2.0.5-FIXED | `MobileCLI-Pro-v2.0.5-FIXED.apk` | LoginActivity onResume fix |
| v2.0.4-GOOGLE-RESTORED | `MobileCLI-Pro-v2.0.4-GOOGLE-RESTORED.apk` | Restored SDK Google OAuth |
| v2.0.3-OAUTH-FIX | `MobileCLI-Pro-v2.0.3-OAUTH-FIX.apk` | Browser-based OAuth attempt |
| v2.0.2-RESTORE-FIX | `MobileCLI-Pro-v2.0.2-RESTORE-FIX.apk` | Restore Purchase button clickability |
| v2.0.1-PAYMENT-FIX | `MobileCLI-Pro-v2.0.1-PAYMENT-FIX.apk` | PayPal deep link handler |
| v2.0.0-FINAL | `MobileCLI-Pro-v2.0.0-FINAL.apk` | Original release |

All APKs stored in `/sdcard/Download/` for easy revert.

---

## KEY IDS

| Item | Value |
|------|-------|
| PayPal Plan ID | `P-3RH33892X5467024SNFZON2Y` |
| PayPal Button ID | `DHCKPWE3PJ684` |
| Supabase Project | `mwxlguqukyfberyhtkmg` |
| PayPal Webhook URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook` |
| Stripe Webhook URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/stripe-webhook` |
| Stripe Checkout URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/create-stripe-checkout` |
| Stripe Portal URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/create-portal-session` |
| Website | `https://www.mobilecli.com` |
| Success Page | `https://www.mobilecli.com/success` |

---

## PAYMENT FLOWS

### Stripe Flow (New)
```
User clicks "Subscribe with Card"
    |
    v
App calls create-stripe-checkout Edge Function
    |
    v
Stripe Checkout page opens (hosted by Stripe)
    |
    v
User enters card details
    |
    v
Stripe processes payment, sends webhook (HMAC-SHA256 verified)
    |
    v
stripe-webhook Edge Function: UPSERT subscription (status='active', provider='stripe')
    |
    v
User returns to app -> polling detects active subscription -> Pro access
```

### PayPal Flow (Existing, Unchanged)
```
User clicks "Subscribe with PayPal"
    |
    v
PayPal checkout (with custom_id = user_id)
    |
    v
User completes payment
    |
    v
PayPal webhook -> UPSERT (creates row if needed)
    |
    v
status = 'active' in database
    |
    v
User returns to app -> "Restore Purchase" -> Pro access
```

### Subscription Management
```
User opens Account -> "Manage Subscription"
    |
    v
Check provider field in subscriptions table
    |
    ├── provider = 'stripe' -> Open Stripe Customer Portal
    |                           (cancel, update card, view invoices)
    |
    └── provider = 'paypal' or null -> Open PayPal autopay page
```

---

## PAYPAL STATUS

**Status:** WORKING and DOCUMENTED

**The Fix (January 25, 2026):**
- Problem: Webhook using `.update()` returned empty array when no row matched
- Solution: Changed to `.upsert()` with `onConflict: "user_id"`
- Now creates subscription row if missing, updates if exists

**Full Documentation:** See `docs/paypal/` directory

---

## STRIPE STATUS

**Status:** CODE COMPLETE - Pending deployment and configuration

**What's Done:**
- All 3 edge functions written (checkout, webhook, portal)
- Webhook has HMAC-SHA256 signature verification
- Event deduplication via webhook_logs table
- Kotlin UI updated (Stripe button, provider-aware account management)
- GitHub Actions updated to deploy new functions

**What User Needs To Do:**
1. Run database migration (ALTER TABLE)
2. Set Supabase secrets (STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET, STRIPE_PRICE_ID)
3. Register webhook endpoint in Stripe Dashboard
4. Configure Customer Portal in Stripe Dashboard
5. Push code to trigger GitHub Actions deployment
6. Test with Stripe test card `4242 4242 4242 4242`
7. Build new APK with Stripe UI

---

## KNOWN ISSUES (To Fix)

### PayPal custom_id Reliability
- **Problem:** PayPal subscription URLs don't reliably pass `custom_id` as URL parameter
- **Impact:** Webhook can't find user unless PayPal email matches Google login email
- **Solution Needed:** Use PayPal JavaScript SDK to pass custom_id properly
- **Workaround:** User must use same email for Google login and PayPal
- **Note:** This problem does NOT affect Stripe (client_reference_id is reliable)

### Subscription Verification
- User must click "Restore Purchase" manually after payment
- Auto-verification removed to prevent crash loops
- Webhook logs should be checked in Supabase dashboard

---

## FEATURES COMPLETED

### Authentication & Payments
- Google OAuth + Email/Password login
- Stripe subscription ($15/month via card) - NEW
- PayPal subscription ($15/month via PayPal)
- Stripe webhook with HMAC-SHA256 signature verification - NEW
- PayPal webhook handles all subscription events (with UPSERT fix)
- Stripe Customer Portal for subscription management - NEW
- Multi-device login support
- Payment success deep link handler

### Account Management (Industry Standard)
- Account screen with profile display
- Logout button with confirmation
- Manage Subscription (Stripe Portal or PayPal, based on provider) - UPDATED
- Restore Purchase functionality
- Delete Account option (updated text for Stripe)

### Bug Fixes Applied
- Fixed: Account screen transparent background
- Fixed: Deprecated onBackPressed (Android 13+)
- Fixed: Webhook field mismatch
- Fixed: PayPal 404 on return
- Fixed: Restore Purchase button not responding
- Fixed: Google OAuth error handling
- Fixed: Crash loop
- Fixed: "Immediately kicks away" issue
- Fixed: Google OAuth not working (browser-based OAuth)
- **Fixed: Webhook silent failure (UPSERT)**

---

## GIT TAGS

| Tag | Description |
|-----|-------------|
| `paypal-working-jan25` | Complete working PayPal integration |

To recover PayPal-only:
```bash
git checkout paypal-working-jan25
# Read docs/paypal/SETUP_GUIDE.md
```

---

## IMPORTANT FILES

| File | Purpose |
|------|---------|
| `CURRENT_STATE.md` | Quick AI context recovery |
| `docs/paypal/` | **Complete PayPal archive** |
| `CLAUDE.md` | AI environment guide |
| `app/src/main/java/com/termux/auth/LoginActivity.kt` | Login + Google OAuth |
| `app/src/main/java/com/termux/auth/PaywallActivity.kt` | Stripe + PayPal subscription |
| `app/src/main/java/com/termux/auth/LicenseManager.kt` | Subscription verification |
| `app/src/main/java/com/termux/auth/AccountActivity.kt` | Account management (Stripe/PayPal aware) |
| `supabase/functions/create-stripe-checkout/index.ts` | Stripe Checkout Session creation |
| `supabase/functions/stripe-webhook/index.ts` | Stripe webhook handler (signature verified) |
| `supabase/functions/create-portal-session/index.ts` | Stripe Customer Portal session |
| `supabase/functions/paypal-webhook/index.ts` | PayPal webhook (UPSERT version) |
| `supabase/functions/create-subscription/index.ts` | PayPal subscription creation |
| `supabase/setup_subscriptions.sql` | Database setup SQL |

---

## COMPLETE SOURCE FILE MAP (21 Kotlin files, 11,606 lines)

| File | Lines | Purpose |
|------|-------|---------|
| `MainActivity.kt` | 1,672 | Terminal UI, session management, drawer navigation, AI launcher |
| `BootstrapInstaller.kt` | 3,258 | Bootstrap download, 60+ shell script generation (cc, godot4, install-godot) |
| `SetupWizard.kt` | 908 | 3-stage setup: permissions → packages → AI selection |
| `TermuxApiReceiver.kt` | 1,767 | 39+ Termux API commands (cameras, SMS, GPS, Bluetooth, NFC, IR, sensors) |
| `app/TermuxService.kt` | 639 | Background service, session persistence, wake/WiFi locks |
| `auth/PaywallActivity.kt` | 657 | Stripe + PayPal subscription flow |
| `am/AmSocketServer.kt` | 443 | Socket server for activity manager commands |
| `auth/LicenseManager.kt` | 366 | License verification, offline caching, expiration |
| `auth/LoginActivity.kt` | 356 | Email/password + Google OAuth (PKCE) |
| `auth/AccountActivity.kt` | 275 | User profile, subscription management |
| `auth/SupabaseClient.kt` | 237 | Supabase SDK integration (gotrue + postgrest) |
| `app/TermuxOpenReceiver.kt` | 186 | URL/file opening handler |
| `activities/SettingsActivity.kt` | 169 | Settings UI |
| `auth/SplashActivity.kt` | 152 | Entry point, license/auth routing |
| `filepicker/TermuxDocumentsProvider.kt` | 141 | SAF file access provider |
| `filepicker/TermuxFileReceiverActivity.kt` | 107 | File sharing receiver |
| `app/RunCommandService.kt` | 79 | External command execution (Tasker) |
| `TermuxAmDispatcherActivity.kt` | 65 | Am command dispatch |
| `boot/BootReceiver.kt` | 57 | Boot-time script execution |
| `TermuxUrlHandlerActivity.kt` | 46 | URL opening with Activity context |
| `TermuxApplication.kt` | 26 | App singleton initialization |

## KEY ARCHITECTURAL NOTES

- Sessions persist via TermuxService (survives activity destruction)
- CopyOnWriteArrayList for thread-safe session management
- 79 Android permissions (root-equivalent)
- Supabase backend for auth + payments
- Bootstrap installs ARM64 Termux environment on first run
- Self-modification: app can rebuild its own APK from within

---

## VERIFICATION PLAN (Test Mode)

1. Deploy edge functions via GitHub Actions push
2. Test `create-stripe-checkout` with curl:
   ```bash
   curl -X POST https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/create-stripe-checkout \
     -H "Content-Type: application/json" \
     -d '{"user_id": "test-uuid-here"}'
   ```
3. Open checkout URL, use Stripe test card `4242 4242 4242 4242`
4. Verify webhook fires (check `webhook_logs` table)
5. Verify `subscriptions` table updated: `status='active'`, `provider='stripe'`
6. Test Customer Portal via `create-portal-session`
7. Test `invoice.payment_failed` with test card `4000 0000 0000 0341`
8. Once all tests pass, switch to live keys

## GO-LIVE CHECKLIST

1. Replace `sk_test_` with `sk_live_` in Supabase secrets
2. Create live webhook endpoint (same URL, Stripe live mode)
3. Replace `whsec_` test secret with live secret
4. Update Price ID from test to live product
5. Verify one real test transaction
6. Commit final config changes

---

*Last updated: February 2, 2026 - v4.0.0 Stable Release*
