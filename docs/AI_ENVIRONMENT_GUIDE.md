# MobileCLI AI Briefing - Complete Technical Guide

**Version:** 1.0
**Last Updated:** 2026-01-26
**Purpose:** Help AI models (Claude Code, etc.) work correctly in the MobileCLI environment

---

## 1. CRITICAL: READ THIS FIRST

### This is NOT Standard Linux

You are running on **ARM64 Android via Termux**, not x86 Linux. Many assumptions that work on Linux/macOS will break here.

| Assumption | Reality |
|------------|---------|
| `/tmp` is writable | **NO** - Permission denied on Android |
| x86_64 binaries work | **NO** - This is ARM64 (aarch64) |
| Standard Linux paths | **NO** - Use `$PREFIX` and `$HOME` |
| System package manager | **NO** - Use `pkg install` instead of apt |

### Key Paths

```bash
$HOME = /data/user/0/com.termux/files/home  # Your home directory
$PREFIX = /data/data/com.termux/files/usr   # Equivalent to /usr
$TMPDIR = $PREFIX/tmp                       # Use this, NOT /tmp

# User-accessible locations (they can see from file manager):
/sdcard/Download/          # Put files here for user
/sdcard/DCIM/Screenshots/  # Screenshots (Samsung)
/sdcard/Pictures/Screenshots/  # Screenshots (other Android)
```

### Claude Code Tool Compatibility

| Tool | Status | Notes |
|------|--------|-------|
| **Read** | WORKS | Always works |
| **Write** | WORKS | Always works |
| **Edit** | WORKS | Always works |
| **WebFetch** | WORKS | Always works |
| **Bash** | WORKS* | *If CWD exists (see Fix #1 below) |
| **Grep** | WORKS* | *After ripgrep symlink fix (see Fix #2) |
| **Glob** | WORKS* | *After ripgrep symlink fix (see Fix #2) |
| **Task** | BROKEN | Uses hardcoded /tmp - cannot be fixed |

---

## 2. KNOWN ISSUES AND FIXES

### Fix #1: Bash "Exit Code 1" Error

**Problem:** All Bash commands fail with exit code 1

**Root Cause:** Claude Code tracks the current working directory between commands. If the CWD is deleted (e.g., a temp directory), ALL subsequent Bash commands will fail.

**Solution:** Recreate the directory or change to a valid directory:
```bash
# If CWD was ~/some/deleted/path, either:
# 1. Recreate it:
mkdir -p ~/some/deleted/path

# 2. Or change to home:
cd ~
```

**Prevention:** Avoid setting CWD to temporary directories that might be cleaned up.

---

### Fix #2: Glob/Grep "spawn ripgrep ENOENT" Error

**Problem:** Glob and Grep tools fail with:
```
spawn ripgrep ENOENT
```

**Root Cause:** Claude Code looks for `arm64-android/rg` but the bundled ripgrep only includes `arm64-linux/rg`. Android reports as `arm64-android` platform.

**Solution:** Create a symlink:
```bash
cd $PREFIX/lib/node_modules/@anthropic-ai/claude-code/vendor/ripgrep/
ln -sf arm64-linux arm64-android
```

**Auto-fix:** Run `~/fix-claude-code.sh` which applies this automatically.

---

### Fix #3: Task Tool (UNFIXABLE)

**Problem:** Task agents fail with:
```
mkdir /tmp: permission denied
```

**Root Cause:** Claude Code's Task agent system hardcodes `/tmp` for agent communication. On Android, `/tmp` is not writable and cannot be made writable without root.

**Status:** Cannot be fixed without modifying Claude Code source code.

**Workaround:** Don't use the Task tool. Instead:
- Use Bash directly for shell operations
- Use Read/Write/Edit for file operations
- Use WebFetch for HTTP requests
- Break complex tasks into steps you execute directly

---

## 3. TERMUX API REFERENCE

MobileCLI has 79 Android permissions. Access them via `termux-*` commands:

### Essential Commands

| Command | Description | Example |
|---------|-------------|---------|
| `termux-toast "msg"` | Show toast notification | `termux-toast "Done!"` |
| `termux-notification -t "T" -c "C"` | System notification | `termux-notification -t "Alert" -c "Task complete"` |
| `termux-clipboard-get` | Read clipboard | `text=$(termux-clipboard-get)` |
| `termux-clipboard-set` | Write clipboard | `echo "text" \| termux-clipboard-set` |
| `termux-open-url URL` | Open in browser | `termux-open-url "https://google.com"` |
| `termux-vibrate` | Vibrate phone | `termux-vibrate -d 500` |
| `termux-torch on/off` | Toggle flashlight | `termux-torch on` |

### Device Info (JSON output)

| Command | Returns |
|---------|---------|
| `termux-battery-status` | Battery level, charging status, temperature |
| `termux-wifi-connectioninfo` | SSID, IP, signal strength, frequency |
| `termux-wifi-scaninfo` | List of nearby WiFi networks |
| `termux-telephony-deviceinfo` | Device ID, phone type, SIM info |
| `termux-telephony-cellinfo` | Cell tower info (MCC, MNC, LAC, CID) |
| `termux-audio-info` | Audio devices, volume levels |
| `termux-volume` | Get/set volume (music, ring, notification) |

### Location & Sensors

| Command | Description |
|---------|-------------|
| `termux-location` | Get GPS coordinates (JSON) |
| `termux-location -p gps` | Force GPS provider |
| `termux-location -p network` | Use network provider |
| `termux-sensor -l` | List available sensors |
| `termux-sensor -s <name>` | Read specific sensor |
| `termux-sensor -s accelerometer -n 1` | Single accelerometer reading |

### Camera & Media

| Command | Description |
|---------|-------------|
| `termux-camera-info` | List cameras and capabilities |
| `termux-camera-photo -c 0 file.jpg` | Take photo with back camera |
| `termux-camera-photo -c 1 file.jpg` | Take photo with front camera |
| `termux-media-player play file.mp3` | Play audio file |
| `termux-media-player pause` | Pause playback |
| `termux-media-scan file.mp3` | Add file to media library |
| `termux-tts-speak "text"` | Text to speech |

### Communication

| Command | Description |
|---------|-------------|
| `termux-sms-send -n NUMBER "message"` | Send SMS |
| `termux-sms-list -l 10` | List last 10 SMS |
| `termux-sms-list -t inbox` | List inbox messages |
| `termux-contact-list` | List all contacts (JSON) |
| `termux-call-log -l 10` | Last 10 call records |
| `termux-telephony-call NUMBER` | Initiate phone call |
| `termux-share -a send file.txt` | Share file via Android share menu |

### Hardware Control

| Command | Description |
|---------|-------------|
| `termux-infrared-transmit -f 38000 ...` | Send IR signal (TV remote) |
| `termux-brightness 128` | Set brightness (0-255) |
| `termux-brightness auto` | Auto brightness |
| `termux-fingerprint` | Authenticate with fingerprint |
| `termux-nfc` | Read NFC tag |
| `termux-usb -l` | List USB devices |

### System Control

| Command | Description |
|---------|-------------|
| `termux-wake-lock` | Prevent CPU sleep |
| `termux-wake-unlock` | Allow CPU sleep |
| `termux-wallpaper -f image.jpg` | Set wallpaper |
| `termux-wallpaper -u URL` | Set wallpaper from URL |
| `termux-download URL` | Download file (shows in notification) |

### User Interaction

| Command | Description |
|---------|-------------|
| `termux-dialog confirm -t "Title" -i "Message"` | Yes/No dialog |
| `termux-dialog text -t "Title" -i "Prompt"` | Text input dialog |
| `termux-dialog sheet -v "A,B,C"` | Selection sheet |
| `termux-storage-get` | File picker dialog |

---

## 4. FILE ACCESS PATTERNS

### Where to Save Files

| Path | Who Can Access | Use For |
|------|----------------|---------|
| `~/` | Only Termux/AI | Working files, projects |
| `/sdcard/Download/` | User + Termux | **Files user needs to access** |
| `/sdcard/DCIM/Camera/` | User + Termux | Camera photos |
| `$PREFIX/tmp/` | Termux only | Temporary files |

### Reading Screenshots

```bash
# Samsung Galaxy - screenshots go here:
ls -lt /sdcard/DCIM/Screenshots/ | head -5

# Read most recent screenshot (Claude Code can view images):
# Use Read tool on: /sdcard/DCIM/Screenshots/Screenshot_YYYYMMDD_HHMMSS.jpg

# Other Android devices may use:
ls -lt /sdcard/Pictures/Screenshots/ | head -5
```

### Clipboard Operations

```bash
# Copy text to clipboard (user can paste anywhere)
echo "Hello World" | termux-clipboard-set

# Copy file contents
cat ~/results.txt | termux-clipboard-set

# Read from clipboard
termux-clipboard-get
```

---

## 5. ANDROID APP DEVELOPMENT

### Setup (One-Time)

```bash
# Install build tools
install-dev-tools

# This installs:
# - Java 17 (openjdk-17)
# - Gradle
# - Android SDK (in ~/android-sdk/)
# - Build tools (aapt2, d8, apksigner)
```

### Build Process

```bash
cd ~/my-android-project

# Build debug APK
./gradlew assembleDebug

# APK location
ls -la app/build/outputs/apk/debug/

# Copy to Downloads for user to install
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
```

### Known Build Issues

#### aapt2 ARM vs x86 Issue

**Problem:** Gradle downloads x86 aapt2 but this is ARM device.

**Solution:** Add to `gradle.properties`:
```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/home/android-sdk/build-tools/34.0.0/aapt2
```

#### Java Version

**Use:** Java 17 for building
**Target:** Java 11 in build.gradle

```gradle
compileOptions {
    sourceCompatibility JavaVersion.VERSION_11
    targetCompatibility JavaVersion.VERSION_11
}
kotlinOptions {
    jvmTarget = '11'
}
```

#### SDK Location

`local.properties`:
```properties
sdk.dir=/data/data/com.termux/files/home/android-sdk
```

---

## 6. SELF-MODIFICATION

MobileCLI can rebuild itself from within:

```bash
# Clone source
git clone https://github.com/MobileDevCLI/MobileCLI-v2.git ~/MobileCLI-v2

# Make modifications...
# Edit source files in ~/MobileCLI-v2/

# Build new APK
cd ~/MobileCLI-v2
./gradlew assembleDebug

# Copy to Downloads
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/MobileCLI-new.apk

# User installs the new APK manually
```

This creates a recursive loop: AI running inside MobileCLI can modify its own container app.

---

## 7. MEMORY SYSTEM

AI learnings persist across sessions in:

```
~/.mobilecli/
├── memory/
│   ├── evolution_history.json  # Version history
│   ├── problems_solved.json    # Solutions discovered
│   ├── capabilities.json       # Learned capabilities
│   └── goals.json              # Current objectives
└── config/
    └── preferences.json        # User preferences
```

### Viewing Memory

```bash
mobilecli-memory status
```

### Updating Memory

Write JSON to the appropriate file:
```bash
# Example: Add a solved problem
# Use Edit tool to modify ~/.mobilecli/memory/problems_solved.json
```

---

## 8. TROUBLESHOOTING

### "Exit code 1" on all Bash commands

**Cause:** CWD was deleted
**Fix:** `cd ~` or recreate the directory

### "spawn ripgrep ENOENT"

**Cause:** Missing arm64-android symlink
**Fix:** `~/fix-claude-code.sh` or manually:
```bash
cd $PREFIX/lib/node_modules/@anthropic-ai/claude-code/vendor/ripgrep/
ln -sf arm64-linux arm64-android
```

### Task tool "mkdir /tmp: permission denied"

**Cause:** Android doesn't allow /tmp
**Fix:** None available - don't use Task tool

### "permission denied" errors

**Cause:** Trying to use Linux paths
**Fix:** Use Termux paths:
- NOT `/tmp` -> Use `$PREFIX/tmp` or `~/tmp`
- NOT `/usr` -> Use `$PREFIX`
- NOT `/home` -> Use `$HOME`

### Binary won't execute

**Cause:** Wrong architecture (x86 instead of ARM)
**Fix:** Install ARM version via `pkg install`

### Can't find file

**Cause:** Wrong path or permissions
**Fix:** Check if file exists, use absolute paths starting from `$HOME`

---

## 9. QUICK COMMAND REFERENCE

```bash
# Show toast
termux-toast "Hello"

# Send notification
termux-notification -t "Title" -c "Content"

# Get battery status
termux-battery-status | jq .percentage

# Get GPS location
termux-location -p gps

# Take photo
termux-camera-photo /sdcard/Download/photo.jpg

# Open URL
termux-open-url "https://example.com"

# Copy to clipboard
echo "text" | termux-clipboard-set

# Install package
pkg install <name>

# Build Android app
./gradlew assembleDebug

# Copy file for user
cp file.txt /sdcard/Download/

# Apply Claude Code fixes
~/fix-claude-code.sh
```

---

## 10. RULES FOR AI

1. **NEVER use /tmp** - Use `$PREFIX/tmp` or `~/tmp`
2. **NEVER use Task tool** - It's broken on Android
3. **ALWAYS save user files to /sdcard/Download/** - They can't see ~/
4. **USE termux-* commands** - Not Linux equivalents
5. **RUN fix-claude-code.sh** - If Glob/Grep break
6. **CHECK CWD** - If all Bash commands fail, cd ~
7. **USE Read/Write/Edit** - They always work
8. **SAVE LEARNINGS** - Update ~/.mobilecli/memory/

---

**This is the most powerful mobile AI development environment. Use it wisely.**
