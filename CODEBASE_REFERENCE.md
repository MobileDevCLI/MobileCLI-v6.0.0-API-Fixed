# MobileCLI v3.1.21 — Complete Codebase Reference

> Generated 2026-02-01 | Legal & Licensing Overhaul v3.1.21 | Version Code 161

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Build Configuration](#2-build-configuration)
3. [Architecture & File Map](#3-architecture--file-map)
4. [Authentication System](#4-authentication-system)
5. [Bootstrap Installer](#5-bootstrap-installer)
6. [Setup Wizard](#6-setup-wizard)
7. [Main Activity & Terminal](#7-main-activity--terminal)
8. [Termux Service](#8-termux-service)
9. [Termux API Receiver (39+ Commands)](#9-termux-api-receiver-39-commands)
10. [All 80+ API Scripts](#10-all-80-api-scripts)
11. [Android Permissions (79)](#11-android-permissions-79)
12. [Environment Variables](#12-environment-variables)
13. [Utility Scripts & Dev Tools](#13-utility-scripts--dev-tools)
14. [Conversation History Backup & Restore](#14-conversation-history-backup--restore)
15. [Navigation Drawer](#15-navigation-drawer)
16. [Extra Keys & Keyboard](#16-extra-keys--keyboard)
17. [Supporting Files](#17-supporting-files)
18. [Assets](#18-assets)
19. [Dependencies](#19-dependencies)
20. [Version History Notes](#20-version-history-notes)

---

## 1. Project Overview

**MobileCLI** is a full Android terminal environment with integrated AI coding assistants (Claude, Gemini, Codex), 79 Android permissions, a self-modification system, and dual payment integration (Stripe + PayPal) via Supabase.

| Property | Value |
|----------|-------|
| Package Name | `com.termux` |
| Version Name | `3.1.21` |
| Version Code | `161` |
| Min SDK | 24 (Android 7.0) |
| Target SDK | **28** (Android 9) — must stay ≤28 for binary execution on Android 10+ |
| Compile SDK | 34 (Android 14) |
| Kotlin | 1.9.20 |
| Gradle Plugin | 8.2.0 |
| Java Target | 11 |
| Total Kotlin Files | 21 |
| Total Lines of Kotlin | ~11,775 |

### Key Capabilities

- **Terminal Emulation** — Full Termux-compatible terminal with multi-session support (max 10)
- **AI Integration** — Claude Code, Gemini CLI, Codex CLI installed via npm
- **79 Android Permissions** — Camera, SMS, GPS, Bluetooth, NFC, IR, sensors, everything
- **Self-Modification** — AI can rebuild the app from within itself
- **Bootstrap System** — Downloads ~50MB ARM64 bootstrap, extracts full Linux environment
- **Setup Wizard** — 4-stage onboarding: Legal → Permissions → Download → AI Selection
- **Authentication** — Supabase GoTrue with email/password and Google OAuth (PKCE)
- **Payments** — Stripe card payments + PayPal subscriptions with 7-day free trial
- **60+ Termux API Scripts** — Shell access to all Android hardware
- **Persistent Memory** — JSON-based AI memory system for cross-session learning
- **Developer Tools** — Java 17, Gradle, Android SDK, aapt2, apksigner, d8

---

## 2. Build Configuration

### app/build.gradle.kts

```
Namespace:        com.termux
Compile SDK:      34
Min SDK:          24
Target SDK:       28
Version Code:     161
Version Name:     3.1.21
Application ID:   com.termux
Java:             VERSION_11
Kotlin JVM:       11
BuildConfig:      Enabled
```

**Signing:** Release keystore at `../mobilecli-release.keystore`, credentials from `local.properties`.

### gradle.properties

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
# ARM aapt2 override — comment out for Windows builds
android.aapt2FromMavenOverride=/data/user/0/com.termux/files/home/android-sdk/build-tools/34.0.0/aapt2
```

### settings.gradle.kts

```
Root Project: MobileCLI
Modules: :app
Repositories: google(), mavenCentral(), jitpack.io
```

---

## 3. Architecture & File Map

### Kotlin Source (21 files)

```
app/src/main/java/com/termux/
├── MainActivity.kt              (1,704 lines) — Terminal UI, sessions, drawer, AI launch
├── BootstrapInstaller.kt        (3,346 lines) — Environment bootstrap, 60+ API scripts
├── SetupWizard.kt               (957 lines)   — 4-stage onboarding wizard
├── TermuxApiReceiver.kt         (1,767 lines) — BroadcastReceiver for 39+ API commands
├── TermuxApplication.kt         (26 lines)    — Application singleton
├── TermuxUrlHandlerActivity.kt  (46 lines)    — URL opening handler
├── TermuxAmDispatcherActivity.kt(65 lines)    — Activity Manager dispatch
│
├── auth/
│   ├── SplashActivity.kt        (152 lines)   — Entry point, auth state check
│   ├── LoginActivity.kt         (356 lines)   — Email/OAuth login
│   ├── PaywallActivity.kt       (657 lines)   — Subscription paywall
│   ├── AccountActivity.kt       (275 lines)   — Profile & subscription management
│   ├── LicenseManager.kt        (366 lines)   — License validation & caching
│   └── SupabaseClient.kt        (237 lines)   — Supabase SDK integration
│
├── app/
│   ├── TermuxService.kt         (639 lines)   — Foreground service for sessions
│   ├── RunCommandService.kt     (79 lines)    — External command execution
│   └── TermuxOpenReceiver.kt    (186 lines)   — File/URL opening receiver
│
├── am/
│   └── AmSocketServer.kt        (443 lines)   — Unix socket for am commands
│
├── boot/
│   └── BootReceiver.kt          (57 lines)    — Boot completion receiver
│
├── activities/
│   └── SettingsActivity.kt      (169 lines)   — Terminal settings
│
└── filepicker/
    ├── TermuxDocumentsProvider.kt(141 lines)   — SAF document provider
    └── TermuxFileReceiverActivity.kt(107 lines)— File share receiver
```

### XML Files

```
res/layout/
├── activity_main.xml           (908 lines) — Terminal + drawer + tabs + extra keys
├── activity_paywall.xml        (303 lines) — Subscription cards
├── activity_setup_wizard.xml   (325 lines) — Setup stages container
├── activity_login.xml          (185 lines) — Login form
├── activity_account.xml        (145 lines) — Account profile
├── activity_splash.xml         (52 lines)  — Loading screen
├── dialog_progress.xml         (25 lines)  — Progress dialog
└── layout_setup_overlay.xml    (93 lines)  — Bootstrap overlay

res/drawable/
├── ic_launcher.xml             — Vector: terminal > cursor
├── card_background.xml         — Dark card (#1E1E1E)
└── ai_card_background.xml      — Material Design 3 card with pressed state

res/values/
├── strings.xml                 — App name, permission descriptions
├── styles.xml                  — ExtraKeyButton style
└── themes.xml                  — Material Design 3 themes
```

### AndroidManifest.xml (467 lines)

**Activities:** SplashActivity (launcher), LoginActivity, PaywallActivity, AccountActivity, MainActivity, SetupWizard, SettingsActivity, TermuxUrlHandlerActivity, TermuxAmDispatcherActivity, TermuxFileReceiverActivity

**Services:** TermuxService (foreground), RunCommandService

**Receivers:** TermuxApiReceiver, TermuxOpenReceiver, BootReceiver

**Providers:** TermuxDocumentsProvider, TermuxOpenReceiver$ContentProvider

---

## 4. Authentication System

### Flow

```
SplashActivity
    ├── Valid local license? ──Yes──→ MainActivity
    ├── Logged in? ──No──→ LoginActivity
    │                         ├── Email + Password (Supabase GoTrue)
    │                         └── Google OAuth (PKCE, browser-based)
    │                              Deep link: com.termux://login-callback
    └── Has Pro access? ──No──→ PaywallActivity
                                  ├── 7-day Free Trial
                                  ├── Stripe Card Payment
                                  └── PayPal Subscription
```

### Supabase Configuration

| Property | Value |
|----------|-------|
| Project URL | `https://mwxlguqukyfberyhtkmg.supabase.co` |
| Auth | GoTrue with PKCE flow |
| Redirect | `com.termux://login-callback` |
| Email Provider | Yes |
| Google Provider | Yes (browser OAuth) |

### Database: `subscriptions` Table

| Column | Type | Purpose |
|--------|------|---------|
| `id` | UUID | Primary key |
| `user_id` | UUID | FK to auth.users |
| `status` | String | "active", "trial", "cancelled", "expired" |
| `provider` | String | "stripe" or "paypal" |
| `expires_at` | Timestamp | Subscription expiry |
| `trial_started_at` | Timestamp | Trial start |
| `stripe_subscription_id` | String | Stripe sub ID |
| `stripe_customer_id` | String | Stripe customer ID |
| `paypal_subscription_id` | String | PayPal sub ID |
| `paypal_payer_id` | String | PayPal payer ID |
| `last_payment_at` | Timestamp | Last payment |
| `payment_failed_at` | Timestamp | Last payment failure |
| `cancelled_at` | Timestamp | Cancellation time |
| `cancel_reason` | String | Reason for cancel |
| `admin_notes` | String | Admin annotations |

### Edge Functions

| Function | Purpose |
|----------|---------|
| `create-subscription` | Creates PayPal subscription, returns approval URL |
| `create-stripe-checkout` | Creates Stripe checkout session URL |
| `create-portal-session` | Creates Stripe Customer Portal URL |

### PayPal Integration

- **Plan ID:** `P-3RH33892X5467024SNFZON2Y`
- **Subscribe URL:** `https://www.paypal.com/webapps/billing/plans/subscribe?plan_id=P-3RH33892X5467024SNFZON2Y`
- **Management:** `https://www.paypal.com/myaccount/autopay`
- Duplicate prevention: HTTP 409 if subscription exists

### Stripe Integration

- Checkout via hosted Stripe page
- Portal for self-service subscription management
- Provider stored as "stripe" in subscriptions table

### Trial System

- **Duration:** 7 days (hardcoded)
- Supabase trigger creates subscription with `status="trial"` on signup
- `expires_at` set to current_time + 7 days
- Days remaining shown in PaywallActivity
- After expiry: paywall shows upgrade options

### License Caching (LicenseManager)

- **Storage:** EncryptedSharedPreferences (AES256-GCM)
- **Pref name:** `mobilecli_license`
- **Verification interval:** 30 days
- **Offline support:** Returns cached status if server unreachable
- **Keys:** `user_id`, `status`, `expires_at`, `last_verified`

### Deep Links

| Scheme | Purpose |
|--------|---------|
| `com.termux://login-callback` | OAuth code exchange |
| `com.termux://payment-success` | Payment verification trigger |
| `https://www.mobilecli.com/success` | Payment success (web) |

---

## 5. Bootstrap Installer

**File:** `BootstrapInstaller.kt` (3,346 lines) — The largest file in the codebase.

### Download Details

| Property | Value |
|----------|-------|
| URL | `https://github.com/termux/termux-packages/releases/download/bootstrap-2026.01.04-r1%2Bapt.android-7/bootstrap-aarch64.zip` |
| Architecture | ARM64 (aarch64) |
| Size | ~50MB |
| Format | ZIP with SYMLINKS.txt |
| Extract Location | `/data/data/com.termux/files/usr/` (PREFIX) |

### install() Method Flow

```
Step 1  (0%)   — Check if already installed (version marker exists)
Step 2  (0%)   — Create directories: filesDir, prefixDir, homeDir, binDir, libDir, etcDir, tmp, var, share
Step 3  (5%)   — Download bootstrap (retry/resume, progress 5-50%)
Step 4  (50%)  — Extract bootstrap (two-pass: files then symlinks, progress 50-85%)
Step 5  (88%)  — Set permissions (chmod -R 755 bin/ lib/, individual binaries, Os.chmod)
Step 6  (90%)  — Install TermuxAm (am.apk → $PREFIX/libexec/termux-am/, read-only for Android 14+)
Step 7  (92%)  — Install API scripts (60+ termux-* commands, cc wrapper, godot4, APK tools)
Step 8  (94%)  — Configure npm (~/.npmrc: foreground-scripts=true)
Step 9  (95%)  — Setup GitHub (~/.config/gh/, setup-github script)
Step 10 (96%)  — Initialize persistent memory system (~/.mobilecli/memory/*.json)
Step 11 (97%)  — Write version marker ($PREFIX/.mobilecli_version = "mobilecli-v1.8.1")
Step 12 (100%) — Delete bootstrap zip, return success
```

### Download Retry & Resume Logic

| Property | Value |
|----------|-------|
| Max Retries | 3 |
| Backoff Delays | 5s, 15s, 45s (exponential) |
| Connection Timeout | 30s |
| Read Timeout | 60s |
| Resume | HTTP Range header on retry if partial file exists |
| Redirect Handling | Up to 5 redirects (301, 302, 303) |
| Validation | File size vs Content-Length |
| Cleanup on Failure | Delete partial zip + version marker |

### Extraction Process

**Pass 1 — Files:**
- Opens ZIP via ZipInputStream
- Creates directories with mkdirs()
- Writes files via ByteArrayOutputStream
- Saves SYMLINKS.txt content for Pass 2
- Tracks progress every 100 files (50% → 80%)

**Pass 2 — Symlinks:**
- Parses SYMLINKS.txt: format is `target←link_path`
- Removes `./` prefix from link paths
- Creates parent directories if needed
- Uses `android.system.Os.symlink(target, linkPath)`
- Progress 80% → 85%

### Permission Setting

1. `/system/bin/chmod -R 755 $PREFIX/bin` and `$PREFIX/lib`
2. Individual binaries (bash, sh, apt, dpkg, cat, ls, chmod, chown, ln, cp, mv, rm, mkdir): `/system/bin/chmod 755` + `setExecutable(true, false)` + `setReadable(true, false)`
3. All bin files: `android.system.Os.chmod(file, 493)` (octal 0755)
4. All lib files: `setReadable(true, false)` recursively
5. am.apk: `setReadOnly()` + `chmod 0400` (Android 14+ requires non-writable DEX)

### Version Marker

- File: `$PREFIX/.mobilecli_version`
- Content: `"mobilecli-v1.8.1"`
- Checked by `isInstalled()` to skip re-installation

---

## 6. Setup Wizard

**File:** `SetupWizard.kt` (957 lines)

### Stage 0: Legal Agreement

- Scrollable Terms of Service covering:
  - Disclaimer of warranties ("AS IS")
  - Limitation of liability (not liable for data loss, device damage, financial loss, AI consequences)
  - Assumption of risk (79 permissions, AI executing commands, Power Mode)
  - Indemnification clause
  - Privacy (no data collection to own servers, AI queries go to providers)
  - Open source notice (Termux Apache 2.0)
- Checkbox: "I have read and agree to the Terms of Service and Privacy Policy"
- Accept button (disabled until checkbox checked)
- Decline exits app with toast
- Saves `KEY_LEGAL_ACCEPTED = true`

### Stage 1: Permissions

Requests 25+ dangerous/runtime permissions:

| Category | Permissions |
|----------|------------|
| Storage | READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE |
| Camera | CAMERA, RECORD_AUDIO |
| Location | ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION |
| Contacts | READ_CONTACTS, WRITE_CONTACTS, GET_ACCOUNTS |
| Calendar | READ_CALENDAR, WRITE_CALENDAR |
| SMS | SEND_SMS, READ_SMS, RECEIVE_SMS, RECEIVE_MMS, RECEIVE_WAP_PUSH |
| Phone | READ_PHONE_STATE, CALL_PHONE, READ_CALL_LOG, WRITE_CALL_LOG, ADD_VOICEMAIL, USE_SIP, ANSWER_PHONE_CALLS, READ_PHONE_NUMBERS |
| Sensors | BODY_SENSORS, ACTIVITY_RECOGNITION |
| Bluetooth (12+) | BLUETOOTH_CONNECT, BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE |
| Android 13+ | POST_NOTIFICATIONS, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO, BODY_SENSORS_BACKGROUND |

Also requests "Display over other apps" (ACTION_MANAGE_OVERLAY_PERMISSION).

Continues regardless of grant results.

### Stage 2: Full Environment Download

**Wake Lock:** `PARTIAL_WAKE_LOCK` — 30 minutes max, keeps CPU alive.

**WiFi Lock:** `WIFI_MODE_FULL_HIGH_PERF` — keeps WiFi connected when screen off / app backgrounded. This is the critical fix for download reliability.

**Screen On:** `FLAG_KEEP_SCREEN_ON` — prevents screen timeout during setup.

**Coroutine:** `setupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` — survives activity pause/resume.

**Progress Mapping:**
```
0%   — "Downloading terminal environment (~50MB)..."
51%  — "Configuring system packages..."
52%  — "Installing Node.js..."           (pkg install -y nodejs-lts)
54%  — "Installing Python..."            (pkg install -y python)
56%  — "Installing build tools..."       (pkg install -y rust clang cmake make pkg-config binutils)
60%  — "Installing CLI tools..."         (pkg install -y ripgrep fzf bat eza fd ncdu tree unzip)
63%  — "Installing media tools..."       (pkg install -y ffmpeg imagemagick)
66%  — "Installing utilities..."         (pkg install -y rclone sqlite scons proot proot-distro)
68%  — "Configuring native modules..."   (creates ~/.gyp/include.gypi)
70%  — "Installing Claude Code..."       (npm install -g @anthropic-ai/claude-code)
75%  — "Installing Gemini CLI..."        (npm install -g @google/gemini-cli)
80%  — "Installing Codex CLI..."         (npm install -g @openai/codex)
85%  — "Installing Java 17..."           (pkg install -y openjdk-17)
90%  — "Installing Gradle..."            (pkg install -y gradle)
95%  — "Installing Android build tools..."(pkg install -y aapt aapt2 apksigner d8)
100% — "Setup complete!"
```

### Stage 3: Choose Your AI

All three AIs are installed regardless of selection. The choice determines which launches first.

| Card | Subtitle | Color | Description |
|------|----------|-------|-------------|
| **Claude** (RECOMMENDED) | by Anthropic | Purple (#6750A4) | "The most capable AI for coding. Builds apps, debugs issues, writes production code." |
| **Gemini** | by Google | Blue (#4285F4) | "Multimodal AI for research, documentation, and development." |
| **Codex** | by OpenAI | Green (#10A37F) | "Code-focused model for completion and generation." |
| **Basic Terminal** | Skip AI launch | Gray (#79747E) | "Full Linux terminal. You can call any AI tool anytime." |

### runBashCommand() — Timeout & Retry

- **Max Retries:** 3 attempts (0..2)
- **Timeout:** 10 minutes per command via `process.waitFor(10, TimeUnit.MINUTES)`
- **On Timeout:** `process.destroy()`, retry with 5s delay
- **On Non-Zero Exit:** Log warning, retry with 5s delay
- **On Exception:** Log error, retry with 5s delay
- **Logging:** `android.util.Log` (w/warning, i/info, e/error)

### runLoginShell() — Timeout

- **Purpose:** Trigger bootstrap second stage (postinst scripts)
- **Timeout:** 5 minutes via `process.waitFor(5, TimeUnit.MINUTES)`
- **Non-Fatal:** Returns true even on failure (setup continues)
- **No Retry:** Single attempt only

### SharedPreferences Keys

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `setup_complete` | Boolean | false | Entire wizard complete |
| `selected_ai` | String | "claude" | Which AI to launch first |
| `legal_accepted` | Boolean | false | ToS accepted |

Pref name: `mobilecli_setup`

---

## 7. Main Activity & Terminal

**File:** `MainActivity.kt` (1,704 lines)

### Session Management

- Maximum 10 sessions
- Sessions created via TermuxService (persist across activity destruction)
- Shell: `$PREFIX/bin/login` with full environment from `bootstrapInstaller.getEnvironment()`
- Session list: `CopyOnWriteArrayList<TerminalSession>` (thread-safe)
- Switch: `switchToSession(index)` — detach old, attach new, update tabs
- Kill: `killCurrentSession()` — prevents killing last session, auto-switches
- Reconnect: `reconnectToExistingSessions()` — on activity recreation

### AI Launch (launchAI)

```
1. Map AI to command: claude\n, gemini\n, codex\n
2. createSession() — new terminal tab
3. Capture session reference immediately
4. Null-check with toast on failure
5. delay(800ms) — wait for login shell init
6. Write command to session on Main dispatcher
7. Toast: "Launching $aiName CLI in new tab..."
```

Claude drawer always runs plain `claude` (no --dangerously-skip-permissions). Power Mode is a separate toggle.

### Godot Launch (launchGodot)

- Confirmation dialog explaining ~500MB download for proot Ubuntu
- Regenerates godot4 wrapper if missing
- Creates new session
- Command: `command -v proot-distro >/dev/null 2>&1 || pkg install -y proot-distro proot; godot4\n`
- 800ms delay for shell init

### Hugging Face CLI Install

- `pip install huggingface_hub`
- Auto-starts `huggingface-cli login` after install
- New session tab with 500ms delay

### Supabase CLI Install

- `pkg install -y golang`
- `go install github.com/supabase/cli@v1.220.0`
- Renames `~/go/bin/cli` to `~/go/bin/supabase`
- Updates PATH in ~/.bashrc
- Auto-starts `supabase login`
- ~130MB download

### Power Mode

- Shows warning dialog about autonomous AI execution
- Creates new terminal session and explicitly sends `claude --dangerously-skip-permissions`
- No persistent file — each invocation is independent
- Cleans up legacy `~/.termux/power_mode` file if it exists
- Separate from drawer Claude CLI button (which runs plain `claude` in a new tab)

### URL Watcher

- Polls `~/.termux/url_to_open` every 500ms
- If file exists: reads URL, deletes file, opens in browser via `Intent.ACTION_VIEW`
- Critical for OAuth flows (terminal writes URL → app opens browser)

### Lifecycle

| Event | Action |
|-------|--------|
| onCreate | Bind service, check setup, handle pending AI |
| onResume | Re-attach session, set session client |
| onStop | Save session index to prefs |
| onDestroy | Unbind service (don't stop it) |
| onBackPressed | Close drawer → hide keyboard → move to background |

---

## 8. Termux Service

**File:** `TermuxService.kt` (639 lines)

- **Foreground service** with persistent notification — keeps sessions alive days/weeks
- **Wake lock:** `PARTIAL_WAKE_LOCK` (CPU stays on, display can sleep)
- **WiFi lock:** `WIFI_MODE_FULL_HIGH_PERF` (network stays connected)
- **Session persistence:** `CopyOnWriteArrayList<TerminalSession>` survives activity destruction
- **Am Socket Server:** Unix socket at `/data/data/com.termux/files/apps/com.termux/termux-am/am.sock`
- **File-based command polling:** Fallback, polls `~/.termux/am_command` every 200ms

### Service Actions

| Action | Purpose |
|--------|---------|
| `com.termux.service_wake_lock` | Acquire wake lock |
| `com.termux.service_wake_unlock` | Release wake lock |
| `com.termux.service_stop` | Stop service |
| `com.termux.service_execute` | Execute command |
| `com.termux.service_create_session` | Create new session |

---

## 9. Termux API Receiver (39+ Commands)

**File:** `TermuxApiReceiver.kt` (1,767 lines)

BroadcastReceiver processing API calls. Protocol:
- Action: `com.termux.api.API_CALL`
- Extra `api_method`: Method name
- Extra `api_args`: Arguments
- Extra `result_file`: Write result JSON to this file

### All Commands

**Clipboard:** clipboard-get, clipboard-set

**Notifications:** toast, notification (title|content|id), notification-remove, notification-list

**Device Info:** battery-status (JSON), vibrate (duration, amplitude), brightness/brightness-set, torch (on/off), volume/volume-set, audio-info

**Network:** wifi-connectioninfo, wifi-enable, wifi-scaninfo

**Location:** location (GPS/network)

**Camera:** camera-info, camera-photo

**Audio/Media:** media-scan, media-player (play|pause|resume|stop|info), microphone-record (start|stop)

**TTS:** tts-engines, tts-speak

**Telephony:** telephony-call, telephony-cellinfo, telephony-deviceinfo

**SMS:** sms-list (inbox|sent|draft), sms-send (number|message)

**Contacts:** contact-list

**Call Log:** call-log (limit)

**Sensors:** sensor (list|type_id)

**Biometric:** fingerprint (AUTH_AVAILABLE, NO_HARDWARE, NONE_ENROLLED)

**Infrared:** infrared-frequencies, infrared-transmit (frequency, pattern)

**USB:** usb (list devices)

**System:** wallpaper, download (url|title|description), share (text|file), dialog, storage-get, job-scheduler

**Keystore:** keystore-list, keystore-generate (alias|algo|size), keystore-delete, keystore-sign (alias|data), keystore-verify (alias|sig|iv)

**NFC:** nfc (check availability)

**Speech:** speech-to-text (check availability)

**SAF:** saf-ls, saf-stat, saf-read, saf-write, saf-mkdir, saf-rm, saf-create, saf-managedir, saf-dirs

**Wake Lock:** wake-lock (acquire|release, 1-hour max)

**Bluetooth:** bluetooth-info, bluetooth-enable, bluetooth-scaninfo, bluetooth-connect, bluetooth-paired

---

## 10. All 80+ API Scripts

Created by `BootstrapInstaller.installApiScripts()`. Each is a shell script in `$PREFIX/bin/` that broadcasts to TermuxApiReceiver.

### Clipboard (2)
- `termux-clipboard-get` — Read text from clipboard
- `termux-clipboard-set` — Write text to clipboard

### Notifications (3)
- `termux-toast` — Show toast notification
- `termux-notification` — Send system notification (title, content, ID)
- `termux-notification-remove` — Remove notification by ID

### Device Info (6)
- `termux-battery-status` — Battery info (JSON: percentage, status, health)
- `termux-vibrate` — Vibrate phone (default 1000ms)
- `termux-brightness` — Get/set screen brightness
- `termux-torch` — Toggle flashlight (on/off)
- `termux-volume` — Get/set volume levels
- `termux-audio-info` — Get audio device info

### Network & WiFi (3)
- `termux-wifi-connectioninfo` — Connected WiFi info (SSID, BSSID, RSSI, IP)
- `termux-wifi-enable` — Enable/disable WiFi
- `termux-wifi-scaninfo` — Scan available networks

### Location (1)
- `termux-location` — GPS coordinates and location info

### Camera (2)
- `termux-camera-info` — Camera device details
- `termux-camera-photo` — Take photo (-o output, -c camera_id)

### Audio & Media (4)
- `termux-media-scan` — Scan media files
- `termux-media-player` — Play audio (action, file)
- `termux-microphone-record` — Record audio (-f file, -l limit)
- `termux-tts-engines` — List TTS engines

### Text-to-Speech (1)
- `termux-tts-speak` — Speak text

### Telephony (3)
- `termux-telephony-call` — Make phone call
- `termux-telephony-cellinfo` — Cell tower information
- `termux-telephony-deviceinfo` — Phone device info

### SMS (2)
- `termux-sms-list` — List messages (-t type, -l limit)
- `termux-sms-send` — Send SMS (-n number, text)

### Contacts & Call Log (2)
- `termux-contact-list` — List all contacts
- `termux-call-log` — Call history (-l limit)

### Sensors (1)
- `termux-sensor` — Read device sensors (-s type, -l list)

### Biometric (1)
- `termux-fingerprint` — Fingerprint authentication

### Infrared (2)
- `termux-infrared-frequencies` — IR frequency codes
- `termux-infrared-transmit` — Send IR signals (-f frequency, pattern)

### USB (1)
- `termux-usb` — USB device access

### System Utilities (6)
- `termux-wallpaper` — Set device wallpaper
- `termux-download` — Download files (-t title, -d description, URL)
- `termux-share` — Share content via intent
- `termux-dialog` — Show input dialog (-t title, -i hint)
- `termux-storage-get` — Pick file from storage
- `termux-job-scheduler` — Schedule background jobs

### Bluetooth (5)
- `termux-bluetooth-info` — Bluetooth device info
- `termux-bluetooth-enable` — Enable/disable Bluetooth
- `termux-bluetooth-scaninfo` — Scan for devices
- `termux-bluetooth-connect` — Connect to device (MAC address)
- `termux-bluetooth-paired` — List paired devices

### Keystore (5)
- `termux-keystore-list` — List keys in Android KeyStore
- `termux-keystore` — Multi-function: list, generate, delete, sign, verify

### NFC (1)
- `termux-nfc` — Read/write NFC tags

### Notification List (1)
- `termux-notification-list` — List all notifications

### Speech (1)
- `termux-speech-to-text` — Voice recognition

### SAF — Storage Access Framework (9)
- `termux-saf-ls` — List directory
- `termux-saf-stat` — File/directory info
- `termux-saf-read` — Read file
- `termux-saf-write` — Write to file
- `termux-saf-mkdir` — Create directory
- `termux-saf-rm` — Remove file/directory
- `termux-saf-create` — Create new file (-m mime_type)
- `termux-saf-managedir` — Open directory picker
- `termux-saf-dirs` — List available directories

### URL/File Opening (4)
- `termux-open-url` — Open URL in browser (file-based IPC)
- `termux-open` — Open files and URLs with intent
- `xdg-open` — freedesktop.org URL/file opener
- `sensible-browser` — Browser wrapper

### Special Wrappers (2)
- `open` — macOS-style open command
- `am` — Activity Manager with proper app permissions (v54 file-based)

### Wake Lock (2)
- `termux-wake-lock` — Acquire wake lock
- `termux-wake-unlock` — Release wake lock

### Compatibility & Setup (8)
- `termux-setup-storage` — Create ~/storage/ symlinks
- `termux-reload-settings` — No-op for compatibility
- `termux-info` — Display system info
- `termux-change-repo` — Change package mirror
- `termux-fix-shebang` — Fix script shebangs for Termux paths
- `termux-reset` — Reset to clean state (keeps home)
- `termux-backup` — Backup home directory to tar.gz
- `termux-restore` — Restore from backup

### File Handling (3)
- `termux-file-editor` — Open file in system editor
- `termux-url-opener` — Handle URLs from other apps
- `termux-file-opener` — Handle files from other apps

---

## 11. Android Permissions (79)

### Network & Connectivity (5)
INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, CHANGE_NETWORK_STATE

### Location (3)
ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION

### Camera & Media (4)
CAMERA, RECORD_AUDIO, MODIFY_AUDIO_SETTINGS, FLASHLIGHT

### Storage (7)
READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE, DOWNLOAD_WITHOUT_NOTIFICATION, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO

### Telephony & SMS (12)
READ_PHONE_STATE, READ_PHONE_NUMBERS, CALL_PHONE, ANSWER_PHONE_CALLS, READ_CALL_LOG, WRITE_CALL_LOG, ADD_VOICEMAIL, USE_SIP, PROCESS_OUTGOING_CALLS, READ_SMS, SEND_SMS, RECEIVE_SMS, RECEIVE_MMS, RECEIVE_WAP_PUSH

### Contacts & Calendar (7)
READ_CONTACTS, WRITE_CONTACTS, GET_ACCOUNTS, READ_CALENDAR, WRITE_CALENDAR, READ_SYNC_SETTINGS, WRITE_SYNC_SETTINGS

### Bluetooth (5)
BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_CONNECT, BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE

### Sensors & Biometrics (7)
BODY_SENSORS, BODY_SENSORS_BACKGROUND, HIGH_SAMPLING_RATE_SENSORS, ACTIVITY_RECOGNITION, USE_BIOMETRIC, USE_FINGERPRINT, TRANSMIT_IR

### NFC (3)
NFC, NFC_TRANSACTION_EVENT, NFC_PREFERRED_PAYMENT_INFO

### USB (1)
USB_PERMISSION

### System & Notifications (11)
VIBRATE, POST_NOTIFICATIONS, SET_WALLPAPER, SET_WALLPAPER_HINTS, ACCESS_NOTIFICATION_POLICY, EXPAND_STATUS_BAR, WAKE_LOCK, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, RECEIVE_BOOT_COMPLETED

### System Management (7)
SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM, REQUEST_INSTALL_PACKAGES, REQUEST_DELETE_PACKAGES, PACKAGE_USAGE_STATS, REORDER_TASKS, KILL_BACKGROUND_PROCESSES

### System Settings (3)
WRITE_SETTINGS, BROADCAST_STICKY, SET_ALARM

### UI & Package Queries (2)
SYSTEM_ALERT_WINDOW, QUERY_ALL_PACKAGES

### Custom Permission (1)
`com.termux.permission.RUN_COMMAND` (protectionLevel: dangerous)

### Hardware Features (all optional)
Touchscreen, Leanback, Camera, Flash, Autofocus, Microphone, Location, GPS, Telephony, Accelerometer, Gyroscope, Light, Proximity, Fingerprint, Consumer IR, USB Host, NFC

### Package Queries
com.android.chrome, com.sec.android.app.sbrowser, org.mozilla.firefox, com.opera.browser, com.brave.browser

---

## 12. Environment Variables

Set by `BootstrapInstaller.getEnvironment()`:

### Core Unix
```bash
HOME=/data/data/com.termux/files/home
PREFIX=/data/data/com.termux/files/usr
PATH=$PREFIX/bin:/system/bin:/system/xbin
LD_LIBRARY_PATH=$PREFIX/lib
TMPDIR=$PREFIX/tmp
PWD=$HOME
TERM=xterm-256color
COLORTERM=truecolor
LANG=en_US.UTF-8
SHELL=$PREFIX/bin/bash
```

### User Information
```bash
USER=u0_a<uid % 100000>
LOGNAME=u0_a<uid % 100000>
```

### Termux Core
```bash
TERMUX_VERSION=0.118.0
TERMUX_APK_RELEASE=MOBILECLI
TERMUX_IS_DEBUGGABLE_BUILD=0
TERMUX_MAIN_PACKAGE_FORMAT=debian
TERMUX__PREFIX=$PREFIX
TERMUX__HOME=$HOME
TERMUX__ROOTFS_DIR=$FILESDIR
```

### Termux App
```bash
TERMUX_APP_PID=<process_id>
TERMUX_APP__PID=<process_id>
TERMUX_APP__UID=<uid>
TERMUX_APP__PACKAGE_NAME=com.termux
TERMUX_APP__VERSION_NAME=1.0.0
TERMUX_APP__VERSION_CODE=1
TERMUX_APP__TARGET_SDK=28
TERMUX_APP__USER_ID=0
TERMUX_APP__IS_DEBUGGABLE_BUILD=false
TERMUX_APP__APK_RELEASE=MOBILECLI
TERMUX_APP__PACKAGE_MANAGER=apt
TERMUX_APP__PACKAGE_VARIANT=apt-android-7
TERMUX_APP__FILES_DIR=$FILESDIR
TERMUX_APP__DATA_DIR=/data/user/0/com.termux
TERMUX_APP__LEGACY_DATA_DIR=/data/data/com.termux
```

### Android System
```bash
ANDROID_DATA=/data
ANDROID_ROOT=/system
EXTERNAL_STORAGE=/sdcard
ANDROID_STORAGE=/storage
```

### Support
```bash
TMUX_TMPDIR=$PREFIX/var/run
BROWSER=termux-open-url
COREPACK_ENABLE_AUTO_PIN=0
SSL_CERT_FILE=$PREFIX/etc/tls/cert.pem       # if exists
NODE_EXTRA_CA_CERTS=$PREFIX/etc/tls/cert.pem  # if exists
CURL_CA_BUNDLE=$PREFIX/etc/tls/cert.pem       # if exists
LD_PRELOAD=$PREFIX/lib/libtermux-exec-ld-preload.so  # if exists
```

---

## 13. Utility Scripts & Dev Tools

### cc (Claude Code Wrapper)
Wraps `claude` with a proot bind mount to fix `/tmp` access:
```bash
exec proot --bind=$PREFIX/tmp:/tmp claude "$@"
```

### godot4 (Godot Engine Launcher)
Launches Godot 4 via proot-distro Ubuntu environment with X11 libraries.

### install-godot
Installs Godot 4.4 in the Ubuntu proot environment.

### install-dev-tools
Installs Java 17, Gradle, Android SDK, build tools for on-device APK building.

### APK Decompilation Tools
- `install-apk-tools` — Install apktool, jadx, smali, baksmali
- `apktool` — APK decoder/rebuilder
- `jadx` — DEX to Java decompiler
- `smali` — Smali assembler
- `baksmali` — DEX disassembler

### pkg-config
Wrapper that uses `pkgconf` for compatibility.

### mobilecli-memory
AI memory system helper:
- `status` — Show memory system status
- `history` — Show evolution history
- `problems` — Show solved problems
- `caps` — Show capabilities
- `goals` — Show current goals
- `log <msg>` — Add rebuild log entry

### mobilecli-rebuild
Full rebuild-from-source script.

### setup-github
Configure GitHub token for CI/CD workflow.

---

## 14. Conversation History Backup & Restore

**Location:** `.bashrc` generated by `BootstrapInstaller.setPermissions()` (lines 508-514)

**Purpose:** Automatically backs up AI conversation history to `/sdcard/MobileCLI/` so it survives app uninstall. On fresh install, automatically restores from backup if available — enabling `/resume` to work immediately after reinstall.

### How It Works

**Restore (runs first, synchronously on every terminal open):**
```bash
[ ! -d ~/.claude ] && [ -d /sdcard/MobileCLI/.claude ] && cp -r /sdcard/MobileCLI/.claude ~/ 2>/dev/null
[ ! -d ~/.gemini ] && [ -d /sdcard/MobileCLI/.gemini ] && cp -r /sdcard/MobileCLI/.gemini ~/ 2>/dev/null
[ ! -d ~/.codex ] && [ -d /sdcard/MobileCLI/.codex ] && cp -r /sdcard/MobileCLI/.codex ~/ 2>/dev/null
```

- Only triggers if AI directory is **missing** (fresh install) AND backup **exists** on sdcard
- Runs before the user can type any command
- After restore, `claude` → `/resume` shows all previous conversations

**Backup (runs second, in background):**
```bash
(mkdir -p /sdcard/MobileCLI; [ -d ~/.claude ] && cp -ru ~/.claude /sdcard/MobileCLI/; [ -d ~/.gemini ] && cp -ru ~/.gemini /sdcard/MobileCLI/; [ -d ~/.codex ] && cp -ru ~/.codex /sdcard/MobileCLI/) 2>/dev/null &
```

- Runs in background (`&`) — terminal opens instantly, no delay
- Only copies newer files (`-u` flag) — minimal I/O
- Creates `/sdcard/MobileCLI/` directory if it doesn't exist
- Each AI backup is independent (semicolons, not `&&`)
- Completely silent (`2>/dev/null`)

### Backup Location

```
/sdcard/MobileCLI/
├── .claude/          ← Claude Code conversations, settings, projects
├── .gemini/          ← Gemini CLI history
└── .codex/           ← Codex CLI history
```

This directory is **external storage** — it persists when the app is uninstalled.

### Safety Properties

| Property | Detail |
|----------|--------|
| Zero Kotlin code changes | Only modifies bashrc string content |
| No effect on downloads | Runs after terminal is fully initialized |
| No effect on bootstrap | Only in bashrc, not in install flow |
| Idempotent | Running 100 times = running once (`-u` flag) |
| Non-blocking | Backup runs in background subshell |
| Fail-safe | All errors silenced, never prevents terminal from opening |
| No new permissions needed | Already has WRITE_EXTERNAL_STORAGE |

---

## 15. Navigation Drawer

All drawer items in `activity_main.xml` / `MainActivity.kt`:

| ID | Label | Action |
|----|-------|--------|
| nav_account | Account | Opens AccountActivity |
| nav_new_session | New Session | Creates new terminal tab |
| nav_settings | Settings | Opens settings dialog |
| nav_keyboard | Keyboard | Toggles soft keyboard |
| nav_text_size | Text Size | Size selection dialog (20/28/36/44) |
| nav_wake_lock | Wake Lock | Toggle CPU wake lock ON/OFF |
| nav_power_mode | Power Mode | Launch Claude with --dangerously-skip-permissions |
| nav_install_ai | Install AI | AI install/reinstall menu |
| nav_claude | Claude | Launch Claude CLI in new tab |
| nav_gemini | Gemini | Launch Gemini CLI in new tab |
| nav_codex | Codex | Launch Codex CLI in new tab |
| nav_vercel | Vercel CLI | Install Vercel CLI |
| nav_github | GitHub CLI | Install GitHub CLI |
| nav_supabase | Supabase CLI | Install Supabase CLI (Go + build) |
| nav_godot | Godot CLI | Launch Godot via proot Ubuntu |
| nav_huggingface | Hugging Face | Install HF CLI via pip |
| nav_ai_briefing | AI Briefing | Fetch AI environment docs |
| nav_update | Update | Check for app updates |
| nav_help | Help | Show help dialog |
| nav_about | About | Show about info |
| nav_licenses | Licenses | Open source licenses |
| nav_privacy | Privacy Policy | Privacy policy text |
| nav_terms | Terms of Service | ToS text |
| nav_dev_mode | Developer Mode | Toggle dev mode (hidden) |
| nav_install_dev_tools | Install Dev Tools | Install Java, Gradle, SDK |
| nav_version | Version | 7-tap dev mode activator |

---

## 16. Extra Keys & Keyboard

### Button Row

| Button | Key | Sends |
|--------|-----|-------|
| ESC | Escape | `sendKey(27)` |
| CTRL | Ctrl (toggle) | Visual state change, modifier |
| ALT | Alt (toggle) | Visual state change, modifier |
| TAB | Tab | `sendKey(9)` |
| HOME | Home | `\u001b[H` |
| END | End | `\u001b[F` |
| ↑ | Up Arrow | `\u001b[A` |
| ↓ | Down Arrow | `\u001b[B` |
| ← | Left Arrow | `\u001b[D` |
| → | Right Arrow | `\u001b[C` |
| PGUP | Page Up | `\u001b[5~` |
| PGDN | Page Down | `\u001b[6~` |
| - | Dash | `sendChar('-')` |
| / | Slash | `sendChar('/')` |
| \ | Backslash | `sendChar('\\')` |
| \| | Pipe | `sendChar('\|')` |
| ~ | Tilde | `sendChar('~')` |
| _ | Underscore | `sendChar('_')` |
| : | Colon | `sendChar(':')` |
| " | Quote | `sendChar('"')` |
| ... | More | Opens context menu |

**Modifier Behavior:** Ctrl/Alt toggle with visual feedback (green when active). Auto-reset after each key press.

**"More" Menu:** Copy All, Paste, New Session, Kill Session, Reset Terminal

**Pinch Zoom:** 14pt to 56pt range, updates terminal columns/rows.

---

## 17. Supporting Files

### AmSocketServer.kt (443 lines)
Unix socket at `/data/data/com.termux/files/apps/com.termux/termux-am/am.sock` for fast Activity Manager command execution. Validates client UID. Supports: `am start`, `am startservice`, `am broadcast`. Intent parameter parsing: -a (action), -d (data), -t (type), -c (category), -n (component), -e/-es (string extra), -ei (int extra), -ez (boolean extra), -f (flags), -p (package).

### TermuxOpenReceiver.kt (186 lines)
BroadcastReceiver for xdg-open and termux-open. Includes ContentProvider for `content://com.termux.files/...`. Security: only allows files in /home and /usr directories.

### RunCommandService.kt (79 lines)
Allows external apps (e.g., Tasker) to run commands. Protected by `com.termux.permission.RUN_COMMAND`.

### BootReceiver.kt (57 lines)
Listens for BOOT_COMPLETED to optionally restart services.

### TermuxApplication.kt (26 lines)
Simple Application singleton with `getInstance()`.

### TermuxUrlHandlerActivity.kt (46 lines)
Opens URLs from shell with proper Activity context. Solves Android 10+ background activity start restrictions.

### TermuxAmDispatcherActivity.kt (65 lines)
Transparent dispatcher for am commands. Shell passes command via extras, activity dispatches with foreground context.

### SettingsActivity.kt (169 lines)
Terminal settings: font size, color scheme, keyboard behavior.

### TermuxDocumentsProvider.kt (141 lines)
Storage Access Framework provider for file access.

### TermuxFileReceiverActivity.kt (107 lines)
Handles files shared to app from other apps.

---

## 18. Assets

```
assets/
├── mobilecli-source.tar         (530 KB) — Bundled source code
├── scripts/
│   ├── extract-source.sh        (1.6 KB) — Extract bundled source
│   ├── install-selfmod.sh       (1.6 KB) — Self-modification installer
│   ├── selfmod.sh               (6.3 KB) — Self-modification wizard
│   └── setup-github.sh          (1.9 KB) — GitHub credential setup
```

---

## 19. Dependencies

### Terminal Libraries
```kotlin
implementation("com.github.termux.termux-app:terminal-view:v0.118.0")
implementation("com.github.termux.termux-app:terminal-emulator:v0.118.0")
```

### AndroidX
```kotlin
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.10.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
implementation("androidx.browser:browser:1.7.0")
```

### Coroutines
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

### Supabase
```kotlin
implementation(platform("io.github.jan-tennert.supabase:bom:2.0.4"))
implementation("io.github.jan-tennert.supabase:gotrue-kt")
implementation("io.github.jan-tennert.supabase:postgrest-kt")
```

### Ktor HTTP
```kotlin
implementation("io.ktor:ktor-client-android:2.3.6")
implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.6")
```

### Serialization
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
```

### Security
```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

---

## 20. Version History Notes

### v3.1.12 (double-tab fix + tool installer session reliability)
- `onServiceConnected()` was creating a blank session even when `pendingAILaunch` was set, causing two tabs on first launch from Setup Wizard
- Fix: skip `createSession()` when `pendingAILaunch` is set — `launchAI()` creates its own session
- All tool installers (Vercel, GitHub, Supabase, Godot, Hugging Face) now use `bypassDebounce = true`
- Previously the 500ms debounce could silently swallow programmatic session creation, causing commands to write to the wrong tab
- versionCode 152, versionName 3.1.12

### v3.1.11 (Power Mode + Claude CLI — clean separation, no shared state)
- Removed `power_mode` file-based approach entirely — the file was written but never deleted, poisoning all claude calls with `--dangerously-skip-permissions`
- Power Mode button: creates new session, explicitly sends `claude --dangerously-skip-permissions`
- Claude CLI button: creates new session, sends plain `claude`
- Bashrc `claude()` simplified to `command claude "$@"` — no file checks
- Legacy `power_mode` file deleted on Power Mode launch (cleanup)
- Bashrc version marker bumped V6→V7
- versionCode 151, versionName 3.1.11

### v3.1.10 (Power Mode button fix — new tab + --dangerously-skip-permissions)
- `togglePowerMode()` rewritten to create new session and explicitly send `claude --dangerously-skip-permissions`
- Previously was not opening new tab and was sending plain `claude` instead
- Matches `launchAI()` pattern: createSession → delay 800ms → write command
- versionCode 150, versionName 3.1.10

### v3.1.9 (fix Claude CLI — remove proot, bump above on-device builds)
- Same fix as v3.1.8 but with versionCode 149 to install over on-device builds (146-148)
- **Root cause:** v3.1.4-v3.1.7 wrapped `claude()` with proot to fix `/tmp` access, but proot cannot execute the claude Node.js script — `execve()` fails with "Function not implemented" because proot can't resolve the shebang interpreter chain
- **Fix:** Reverted `claude()` to the original working form: `command claude` (no proot). The `TMPDIR=$PREFIX/tmp` export already handles `/tmp` for tools that respect it
- The `cc` wrapper script still uses proot for anyone who specifically needs the `/tmp` bind mount
- Bumped bashrc version marker from `MOBILECLI_BASHRC_V5` to `MOBILECLI_BASHRC_V6` so existing users get the fix on next app launch
- Bumped versionCode to 149, versionName to 3.1.9

### v3.1.7 (fix Claude CLI broken by proot `command` bug)
- **Root cause:** bashrc `claude()` function used `proot ... command claude` — but `command` is a bash built-in, not a binary. proot tried to execute `command` as a program and failed silently
- Fixed: now uses `local _claude_bin="$(which claude)"` then `proot ... "$_claude_bin"` to pass the resolved binary path directly to proot
- Bumped bashrc version marker to `MOBILECLI_BASHRC_V5` so existing users get the fix on next app launch
- Fixed `createSession()` debounce blocking `launchAI()` — added `bypassDebounce` parameter so AI launch always gets a new session
- Bumped versionCode to 144, versionName to 3.1.7

### v3.1.6 (bug fixes — critical stability + payments + memory leaks)
- Fixed drawer version display: was hardcoded "1.8.1", now reads dynamically from BuildConfig
- Fixed crash: onServiceConnected() on destroyed activity — added isFinishing/isDestroyed guard
- Fixed memory leak: ViewTreeObserver keyboard listener now properly removed in onDestroy()
- Fixed SetupWizard: critical package installs (Node.js, Python, proot, Claude Code) now retry on failure
- Fixed PayPal/Stripe HTTP connection leaks — added `connection.disconnect()` and `use {}` blocks
- Fixed payment polling thread safety — `isPolling` now `@Volatile`
- Fixed payment polling timeout — increased from 30s to 90s (webhooks can be slow)
- Fixed launchAI() race condition — session captured after delay instead of immediately
- Fixed rapid session creation crash — added 500ms debounce to createSession()
- Fixed wake lock toast message — was inverted ("screen can turn off" → "screen stays on")
- Bumped versionCode to 143, versionName to 3.1.6

### v3.1.5 (update-safe bashrc + Power Mode fix)
- Fixed bashrc to regenerate on app update — uses `MOBILECLI_BASHRC_V4` version marker
- Previously bashrc was only written on fresh install (`!bashrc.exists()`), so updates kept the old bashrc without TMPDIR, proot /tmp fix, etc.
- Now checks: `!bashrc.exists() || !bashrc.readText().contains(bashrcVersion)` — regenerates if version marker is missing or outdated
- Future bashrc changes only need to bump the version marker (e.g., V5) to auto-deploy to all users on next app update
- Fixed Power Mode to use bashrc `claude()` function instead of raw `claude --dangerously-skip-permissions` — Power Mode now gets the proot /tmp fix
- Bumped versionCode to 142, versionName to 3.1.5

### v3.1.4 (environment fixes + zoom crash fix)
- Fixed bashrc missing critical environment variables: TMPDIR, SHELL, BROWSER, COLORTERM, ANDROID_DATA, ANDROID_ROOT, EXTERNAL_STORAGE
- Fixed `claude` command to use proot /tmp bind mount (same as `cc` wrapper) — /tmp now works in all Claude Code sessions
- `claude()` bashrc function saves/restores LD_PRELOAD around proot call
- `TMPDIR=$PREFIX/tmp` exported in bashrc — tools that respect TMPDIR now work without proot
- `mkdir -p $TMPDIR` ensures tmp directory exists on every terminal open
- `BROWSER=termux-open-url` exported — OAuth URL opening works for Gemini/Codex
- Fixed zoom crash: added divide-by-zero guard in updateTerminalSize() (fontWidthPx/fontHeightPx clamped to minimum 1)
- Fixed zoom crash: added 50ms throttle to onScale() preventing rapid-fire reflection calls
- Fixed zoom crash: wrapped onScale() in try/catch to prevent uncaught exceptions
- Bumped versionCode to 141, versionName to 3.1.4

### v3.1.3 (conversation backup/restore)
- Added automatic conversation history backup to `/sdcard/MobileCLI/` (survives app uninstall)
- Added automatic restore on fresh install — if backup exists on sdcard, conversations are restored before user opens any AI
- Covers all three AIs: `~/.claude/`, `~/.gemini/`, `~/.codex/`
- Backup runs silently in background on every terminal open (only copies newer files)
- Restore runs synchronously on terminal open only when AI directory is missing
- Zero Kotlin code changes outside of bashrc content in BootstrapInstaller.setPermissions()
- Bumped versionCode to 140, versionName to 3.1.3

### v3.1.2 (commit 9caae8e)
- Added comprehensive CODEBASE_REFERENCE.md documentation (1,184 lines)
- Documentation-only release, no functional changes

### v3.1.1 (commit e526656)
- Fixed Claude/Gemini/Codex drawer buttons to open new terminal tabs
- Fixed Claude drawer running plain `claude` instead of `--dangerously-skip-permissions`
- Fixed Godot to auto-install proot-distro before launching
- Renamed "Godot Engine" to "Godot CLI" in drawer
- Added WiFi lock (WIFI_MODE_FULL_HIGH_PERF) for download reliability
- Added retry + resume to downloadBootstrap() (3 retries, exponential backoff, HTTP Range)
- Added 10-minute timeout + 3 retries to runBashCommand()
- Added 5-minute timeout to runLoginShell()
- Added cleanup on install failure (delete partial zip + version marker)

### v3.1.0 (base)
- Pre-release with Stripe integration
- Full auth system (Supabase + PayPal + Stripe)
- Setup wizard with 4 stages
- 60+ Termux API scripts
- Self-modification system
- Persistent AI memory
- Developer tools installation

---

## Architecture Diagram

```
┌──────────────────────────────────────────────────────┐
│                    Android OS                         │
│  ┌─────────────────────────────────────────────────┐ │
│  │              MobileCLI (com.termux)              │ │
│  │                                                  │ │
│  │  ┌──────────┐  ┌───────────┐  ┌──────────────┐ │ │
│  │  │ Splash   │→ │  Login    │→ │   Paywall    │ │ │
│  │  │ Activity │  │ Activity  │  │   Activity   │ │ │
│  │  └──────────┘  └───────────┘  └──────────────┘ │ │
│  │       ↓              ↑ Supabase GoTrue           │ │
│  │  ┌──────────┐  ┌───────────┐  ┌──────────────┐ │ │
│  │  │  Setup   │→ │   Main    │↔ │   Termux     │ │ │
│  │  │  Wizard  │  │ Activity  │  │   Service    │ │ │
│  │  └──────────┘  └───────────┘  └──────────────┘ │ │
│  │       ↓              ↓              ↓           │ │
│  │  ┌──────────────────────────────────────────┐   │ │
│  │  │          BootstrapInstaller              │   │ │
│  │  │  • Download ARM64 bootstrap (~50MB)      │   │ │
│  │  │  • Extract with symlinks                 │   │ │
│  │  │  • Create 60+ API scripts                │   │ │
│  │  │  • Set permissions                       │   │ │
│  │  │  • Configure environment                 │   │ │
│  │  └──────────────────────────────────────────┘   │ │
│  │       ↓                                         │ │
│  │  ┌──────────────────────────────────────────┐   │ │
│  │  │           Termux Environment             │   │ │
│  │  │  $PREFIX = /data/data/com.termux/files/usr│  │ │
│  │  │  $HOME   = /data/data/com.termux/files/home│ │ │
│  │  │                                          │   │ │
│  │  │  ┌─────────┐ ┌────────┐ ┌─────────────┐│   │ │
│  │  │  │ Claude  │ │ Gemini │ │    Codex    ││   │ │
│  │  │  │  Code   │ │  CLI   │ │     CLI     ││   │ │
│  │  │  └─────────┘ └────────┘ └─────────────┘│   │ │
│  │  │  ┌─────────┐ ┌────────┐ ┌─────────────┐│   │ │
│  │  │  │  Node   │ │ Python │ │  Java 17    ││   │ │
│  │  │  │  LTS    │ │   3    │ │  + Gradle   ││   │ │
│  │  │  └─────────┘ └────────┘ └─────────────┘│   │ │
│  │  │  ┌──────────────────────────────────────┐│  │ │
│  │  │  │  60+ termux-* API scripts → Android  ││  │ │
│  │  │  │  (camera, SMS, GPS, BT, NFC, IR...)  ││  │ │
│  │  │  └──────────────────────────────────────┘│  │ │
│  │  └──────────────────────────────────────────┘   │ │
│  │                                                  │ │
│  │  ┌──────────────┐  ┌──────────────────────────┐ │ │
│  │  │ AmSocket     │  │ TermuxApiReceiver        │ │ │
│  │  │ Server       │  │ (BroadcastReceiver)      │ │ │
│  │  │ (Unix sock)  │  │ 39+ API commands         │ │ │
│  │  └──────────────┘  └──────────────────────────┘ │ │
│  └─────────────────────────────────────────────────┘ │
│                                                       │
│  79 Android Permissions → Full hardware access        │
│  Supabase + Stripe + PayPal → Auth & payments         │
│  Self-modification → AI rebuilds its own container    │
└──────────────────────────────────────────────────────┘
```

---

*This document is auto-generated from codebase analysis. For the latest source, see: https://github.com/MobileDevCLI/MobileCLI-Pre-Release-v3.1.0*
