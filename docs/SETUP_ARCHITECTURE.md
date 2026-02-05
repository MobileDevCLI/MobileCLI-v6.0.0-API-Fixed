# Setup Architecture

Developer reference for MobileCLI's first-run setup flow. Covers the bootstrap installer,
package installation, service lifecycle, and error handling.

---

## Installation Flow Overview

```
App Launch
  |
  v
SplashActivity --> SetupWizard (if not completed)
  |
  v
Stage 0: Legal Agreement (Terms of Service)
  |
  v
Stage 1: Permission Requests (79 Android permissions + overlay)
  |
  v
Stage 2: Full Environment Download (SetupService)
  |    Phase 1: Bootstrap (0-50%)  -- BootstrapInstaller
  |    Phase 2: Packages (50-100%) -- pkg + npm installs
  |
  v
Stage 3: Choose Your AI (Claude / Gemini / Codex / None)
  |
  v
MainActivity (terminal)
```

---

## Bootstrap Phases (Phase 1: 0-50%)

Handled by `BootstrapInstaller.kt`. Downloads and extracts the Termux bootstrap archive.

1. **Download** -- Fetches bootstrap-aarch64.zip (~50MB) from GitHub releases
2. **Extract** -- Unzips into `$PREFIX` (`/data/data/com.termux/files/usr/`)
3. **Permissions** -- `chmod 700` on bin directory, symlink setup
4. **Login shell** -- Runs `login -c exit` to trigger Termux second-stage setup

BootstrapInstaller has its own retry logic with exponential backoff for the download.
The extract/permission steps are local-only and don't fail on network issues.

---

## Package Installation (Phase 2: 50-100%)

Handled by `SetupService.kt`. Packages are installed **individually** (not batched)
so that one package failure doesn't block the rest.

### Package Categories

| Category | Behavior | Packages |
|----------|----------|----------|
| **CRITICAL** | Retry once, abort setup if still fails | `nodejs-lts`, `python` |
| **IMPORTANT** | Retry once, warn and continue if fails | `proot`, `proot-distro`, `clang`, `cmake` |
| **OPTIONAL** | Try once, log and continue if fails | `rust`, `rclone`, `scons`, `ripgrep`, `fzf`, `bat`, `eza`, `fd`, `ncdu`, `tree`, `unzip`, `ffmpeg`, `imagemagick`, `openjdk-17`, `gradle`, `aapt`, `aapt2`, `apksigner`, `d8` |
| **NPM CRITICAL** | Retry once, log if fails | `@anthropic-ai/claude-code` |
| **NPM OPTIONAL** | Try once, log if fails | `@google/gemini-cli`, `@openai/codex` |

### Why These Categories

- **nodejs-lts** and **python** are required for npm packages and native module compilation.
  Without them, nothing else in Phase 2 works. Setup should abort early rather than
  silently produce a broken environment.

- **proot/proot-distro** enable Linux distribution access. **clang/cmake** are needed
  for compiling native Node.js modules. These are important but the terminal still
  functions without them.

- Everything else enhances the experience but isn't required for basic operation.

### Install Order

1. `pkg update -y` (separate, with retry)
2. Critical packages (nodejs-lts, python)
3. Important packages (proot, proot-distro, clang, cmake)
4. Optional packages (rust through d8)
5. Configure gyp for native modules
6. NPM critical (@anthropic-ai/claude-code)
7. NPM optional (@google/gemini-cli, @openai/codex)

### Failed Package Tracking

A `failedPackages` list tracks every package that failed to install. At the end of
Phase 2, if any failures occurred, they're logged as a summary:

```
SetupService: Setup completed with 3 failed package(s): rust, ffmpeg, npm:@openai/codex
```

Only CRITICAL package failures abort setup entirely.

---

## Error Handling and Retry Strategy

### runBashCommand()

Every shell command goes through `runBashCommand()` which has built-in retry:
- Up to 3 attempts (initial + 2 retries)
- 5-second delay between attempts
- 10-minute timeout per attempt
- Returns `true` only on exit code 0

### installPackage() / installNpmPackage()

Wrappers around `runBashCommand()` that add category-aware retry:
- `critical = true`: One additional retry on top of runBashCommand's built-in retries
- `critical = false`: No additional retry, just logs the failure

### Bootstrap (BootstrapInstaller)

Separate retry logic with exponential backoff:
- Download retries with increasing delays
- Resume support for partial downloads
- SHA-256 verification after download

### Wake Lock

The service acquires a partial wake lock for **2 hours** (was 30 minutes in v3.1.21).
Setup on slow connections can exceed 30 minutes, and a wake lock expiring mid-install
causes the CPU to sleep, stalling all downloads silently.

---

## Service Lifecycle

### SetupService (Foreground Service)

```
onCreate()
  - Initialize BootstrapInstaller
  - Create notification channel
  - Start foreground with persistent notification
  - Acquire wake lock (2 hours) and WiFi lock

startSetup()  [called from SetupWizard binding]
  - Launches coroutine on Dispatchers.IO
  - Phase 1: Bootstrap download/extract
  - Phase 2: Package installation
  - Reports progress via onProgress callback

onDestroy()
  - Cancels coroutine scope
  - Releases wake lock and WiFi lock
```

### SetupWizard (Activity) <-> SetupService Binding

```
startFullDownload()
  - startService(intent)     -- ensures service survives activity death
  - bindService(intent)      -- gets binder for progress callbacks

onServiceConnected()
  - Sets onProgress callback
  - Checks if service already completed/failed while unbound
  - Starts setup if not already running

onDestroy()
  - Clears progress callback
  - Unbinds from service (wrapped in try-catch for IllegalArgumentException)
  - Does NOT stop service -- download continues in background
```

The service uses `START_STICKY` so Android will restart it if killed. The activity
can rebind on recreation and pick up where it left off by checking `isComplete`
and `hasFailed` state.

---

## Known Limitations

1. **No synchronization on isRunning** -- `startSetup()` checks `isRunning` without
   synchronization. In practice this is safe because it's only called from
   `onServiceConnected`, but it's not formally thread-safe.

2. **Progress callback during binding window** -- Between service start and bind
   completion, progress events are lost. The UI catches up by checking service state
   in `onServiceConnected`.

3. **Login shell timeout treated as non-fatal** -- If the login shell hangs for 5
   minutes, it's killed and setup continues. This could leave the bootstrap in a
   partially-configured state, but changing this behavior requires understanding
   what the second-stage setup actually does.

4. **No resume for package installs** -- If setup is interrupted during Phase 2,
   the retry starts all package installs from the beginning. Individual packages
   that already installed will return quickly (pkg recognizes them as installed).

---

## File References

| File | Role |
|------|------|
| `SetupService.kt` | Foreground service, package installation, progress reporting |
| `SetupWizard.kt` | 4-stage UI (legal, permissions, download, AI choice) |
| `BootstrapInstaller.kt` | Bootstrap download, extraction, environment setup |
| `MainActivity.kt` | Post-setup terminal activity |
| `SplashActivity.kt` | Entry point, routes to SetupWizard or MainActivity |
