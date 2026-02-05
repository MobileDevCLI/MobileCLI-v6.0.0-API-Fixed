# MobileCLI Pro - Version Information

## Current Version

**Version:** 5.7.0
**Version Code:** 570
**Release Date:** February 4, 2026
**Status:** Stable Release (Critical Security & Stability)

---

## Version Naming Convention

```
MAJOR.MINOR.PATCH-STAGE.BUILD
  │     │     │     │     │
  │     │     │     │     └── Build number (auto-incremented by CI)
  │     │     │     └── Development stage (alpha, beta, rc, release)
  │     │     └── Patch version (bug fixes)
  │     └── Minor version (new features)
  └── Major version (breaking changes)
```

### Examples
- `2.0.0-beta.34-build13` = Version 2.0.0, Beta 34, Build 13
- `2.1.0-release` = Version 2.1.0, Production Release

---

## Release Channels

| Channel | Description | Stability |
|---------|-------------|-----------|
| **alpha** | Early development, may be broken | Unstable |
| **beta** | Feature complete, testing phase | Mostly stable |
| **rc** | Release candidate, final testing | Stable |
| **release** | Production ready | Stable |

---

## Current Features (v5.5.0)

### Authentication
- [x] Email + Password login
- [x] Email + Password signup
- [x] Google OAuth login (PKCE flow)
- [x] GitHub OAuth login
- [x] Apple OAuth login
- [x] Microsoft OAuth login (Azure)
- [x] Discord OAuth login
- [x] Session persistence
- [x] Logout functionality

### Payments
- [x] PayPal subscription ($15/month)
- [x] PayPal Business account integration
- [x] Stripe payment integration (Supabase Edge Functions)
- [x] 7-day free trial
- [x] Automatic license activation via webhook
- [x] Webhook logging for debugging
- [x] Payment history recording

### Terminal & AI
- [x] Multi-session terminal with persistence across activity recreation
- [x] Claude Code, Gemini CLI, Codex CLI integration
- [x] cc wrapper (proot-based /tmp fix for Claude Code on Android)
- [x] Power Mode (autonomous AI with --dangerously-skip-permissions)
- [x] Direct CLI launch buttons in drawer (Claude, Gemini, Codex)
- [x] Vercel CLI, GitHub CLI, Supabase CLI, Hugging Face CLI integration
- [x] Godot Engine integration (proot-distro Ubuntu wrapper)

### Developer Tools
- [x] Expanded package installation (rust, clang, cmake, ripgrep, fzf, bat, etc.)
- [x] Build tools for on-phone APK compilation (Java 17, Gradle, aapt2, d8, apksigner)
- [x] Self-modification loop (rebuild own APK from within)
- [x] Developer Mode (7-tap activation)

### Crash Stability (v3.1.0)
- [x] Thread-safe sessions list (CopyOnWriteArrayList)
- [x] Touch event crash protection (try-catch in dispatchTouchEvent)
- [x] Paste race condition fix (lifecycle guards + null safety)
- [x] Post-destroy callback prevention (handler cleanup in onDestroy)
- [x] Lifecycle guards on all TerminalSessionClient callbacks

### Infrastructure
- [x] Supabase authentication backend
- [x] JWT auth, CORS, RLS security hardening
- [x] GitHub Actions CI/CD
- [x] Automatic APK builds
- [x] 79 Android permissions (root-equivalent access)

---

## Download Links

### Latest Stable (Build 15)
https://github.com/MobileDevCLI/MobileCLI-Pro/releases/tag/v2.0.0-rc.3

### All Releases
https://github.com/MobileDevCLI/MobileCLI-Pro/releases

---

## Build Information

### Build Environment
- **CI Platform:** GitHub Actions
- **Build Runner:** Ubuntu Latest
- **Java Version:** 17 (Temurin)
- **Gradle:** Wrapper (project version)
- **Android SDK:** 34

### Build Triggers
- Push to `master` branch
- Manual workflow dispatch

### Build Outputs
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Artifact retention: 30 days
- Automatic GitHub Release creation

---

## Compatibility

### Android Requirements
| Requirement | Value |
|-------------|-------|
| **Minimum SDK** | 24 (Android 7.0) |
| **Target SDK** | 28 (Android 9.0)* |
| **Compile SDK** | 34 |

*Target SDK 28 required for Termux binary execution compatibility

### Tested Devices
- Samsung Galaxy S24 Ultra
- Android tablets (various)

---

## File Checksums

Build 15 APK checksums (verify download integrity):
```
SHA256: (generate after download with: sha256sum app-debug.apk)
MD5: (generate after download with: md5sum app-debug.apk)
```

---

## Upgrade Path

### From Previous Versions
1. Download latest APK from GitHub Releases
2. Install over existing app (data preserved)
3. Re-login if prompted

### Breaking Changes
- None in current beta series

---

## Support

- **Issues:** https://github.com/MobileDevCLI/MobileCLI-Pro/issues
- **Releases:** https://github.com/MobileDevCLI/MobileCLI-Pro/releases
