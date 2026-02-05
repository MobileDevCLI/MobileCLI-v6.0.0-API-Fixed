# MobileCLI AI Environment - v6.0.0

**READ THIS FIRST.** You are an AI running inside MobileCLI on an Android phone.
This is NOT a sandbox. This is a **complete professional development environment** in your pocket.

---

## TABLE OF CONTENTS

1. [Critical: ARM Android Environment](#critical-arm-android-environment)
2. [What You Can Build](#what-you-can-build)
3. [Available CLIs (22)](#available-clis-22)
4. [Hardware Commands (80+)](#hardware-commands-80)
5. [Development Workflows](#development-workflows)
6. [File System & Paths](#file-system--paths)
7. [Known Issues & Workarounds](#known-issues--workarounds)
8. [Rules & Best Practices](#rules--best-practices)

---

## CRITICAL: ARM ANDROID ENVIRONMENT

**You are running on ARM64 Android, NOT x86 Linux.**

### What This Means
| Aspect | Reality |
|--------|---------|
| **Architecture** | ARM64 (aarch64), NOT x86_64 |
| **OS** | Android with Linux layer |
| **Paths** | Use `$PREFIX` not `/usr`, use `~/` not `/home/user/` |
| **Temp files** | Use `$PREFIX/tmp` or `~/tmp`, **NEVER `/tmp`** (permission denied) |
| **User files** | Save to `/sdcard/Download/` for user access from file manager |

### Environment Variables
```bash
$PREFIX = /data/data/com.termux/files/usr
$HOME   = /data/data/com.termux/files/home
$TMPDIR = $PREFIX/tmp  # Use this, NOT /tmp
```

---

## WHAT YOU CAN BUILD

| Type | Tools | Deploy To |
|------|-------|-----------|
| **Android Apps** | Java 17, Kotlin, Gradle, APK signing | Direct install on device |
| **Games** | Godot 4 engine | Android, iOS, Web, Desktop |
| **Websites** | HTML/CSS/JS, any framework | Vercel, Firebase, GitHub Pages |
| **Backends** | Node.js, Python | Firebase, Vercel serverless |
| **Bots** | Telegram, Discord, Slack | Your servers |
| **AI/ML** | Hugging Face models | Local or cloud |

---

## AVAILABLE CLIs (22)

These are accessible from the MobileCLI app menu:

### AI Assistants (4)

| CLI | Description |
|-----|-------------|
| **Claude CLI** | Anthropic's AI coding assistant (you're using this now) |
| **Gemini CLI** | Google's AI assistant |
| **Codex CLI** | OpenAI's coding assistant |
| **OpenRouter CLI** | Access 100+ AI models (Claude, GPT-4, Llama, Mistral) via unified API |

### AI Collaboration
```bash
# AIs can work together
claude "Write a function" > code.js
gemini "Review this code" < code.js
codex "Add tests" < code.js
```

### Deployment & Platform (8)

| CLI | Description |
|-----|-------------|
| **Vercel CLI** | Deploy websites, Next.js, serverless functions |
| **GitHub CLI** | Repos, PRs, issues, releases, full GitHub workflow |
| **Firebase CLI** | Hosting, database, auth, cloud functions |
| **Netlify CLI** | Deploy sites, serverless functions, edge handlers |
| **Heroku CLI** | Deploy, manage, and scale apps on Heroku |
| **EAS CLI** | Build, submit, and update React Native/Expo apps |
| **Fly.io CLI** | Deploy and run apps globally with auto-scaling |
| **Convex CLI** | Realtime backend with TypeScript and serverless |

### Development Tools (2)

| CLI | Description |
|-----|-------------|
| **Godot CLI** | Godot 4 game engine - build games for any platform |
| **Supabase CLI** | Backend as a service - database, auth, storage |

### Communication & APIs (4)

| CLI | Description |
|-----|-------------|
| **Telegram CLI** | Build and run Telegram bots |
| **Discord CLI** | Build and run Discord bots |
| **Slack CLI** | Build Slack apps and bots |
| **Twilio CLI** | Manage SMS, voice, and communication APIs |

### Headless CMS (3)

| CLI | Description |
|-----|-------------|
| **Sanity CLI** | Manage headless CMS content and schemas |
| **Contentful CLI** | Manage headless CMS content, spaces, environments |
| **Shopify CLI** | Build Shopify apps, themes, and storefronts |

### AI/ML (1)

| CLI | Description |
|-----|-------------|
| **Hugging Face CLI** | Access AI models, datasets, spaces |

---

## HARDWARE COMMANDS (80+)

Full access to phone hardware from command line. All commands return JSON.

### Clipboard
| Command | Description |
|---------|-------------|
| `clipboard-get` | Read clipboard contents |
| `clipboard-set "text"` | Copy text to clipboard |

### Notifications
| Command | Description |
|---------|-------------|
| `toast "message"` | Show quick toast notification |
| `notification -t "Title" -c "Content"` | System notification |

### Device Control
| Command | Description |
|---------|-------------|
| `vibrate` | Vibrate device |
| `torch on/off` | Flashlight control |
| `brightness 128` | Set brightness (0-255) |
| `volume music 10` | Set volume (0-15) |
| `wallpaper -f image.jpg` | Set wallpaper |
| `wake-lock` | Keep CPU awake |

### Battery & Power
| Command | Description |
|---------|-------------|
| `battery-status` | Battery info (status, percentage, temp) |

### Network
| Command | Description |
|---------|-------------|
| `wifi-connectioninfo` | Current WiFi details |
| `wifi-scaninfo` | Scan available networks |
| `download URL` | Download file |

### Location
| Command | Description |
|---------|-------------|
| `location` | Get GPS coordinates |

### Camera
| Command | Description |
|---------|-------------|
| `camera-info` | List cameras and capabilities |
| `camera-photo output.jpg` | Take photo |

### Audio & Media
| Command | Description |
|---------|-------------|
| `media-player play file.mp3` | Play audio |
| `microphone-record -f out.m4a -l 10` | Record audio |
| `tts-speak "Hello world"` | Text to speech |
| `speech-to-text` | Voice to text |

### Telephony & SMS
| Command | Description |
|---------|-------------|
| `telephony-deviceinfo` | Device info |
| `telephony-call "+1234567890"` | Make phone call |
| `call-log` | Get call history |
| `sms-list` | List SMS messages |
| `sms-send -n "+1234567890" "Hello!"` | Send SMS |

### Contacts
| Command | Description |
|---------|-------------|
| `contact-list` | List all contacts |

### Sensors
| Command | Description |
|---------|-------------|
| `sensor -l` | List available sensors |
| `sensor -s accelerometer` | Read accelerometer |
| `sensor -s gyroscope` | Read gyroscope |

### Biometrics
| Command | Description |
|---------|-------------|
| `fingerprint` | Authenticate with fingerprint |

### Bluetooth
| Command | Description |
|---------|-------------|
| `bluetooth-paired` | List paired devices |
| `bluetooth-scaninfo` | Scan for devices |

### IR Blaster
| Command | Description |
|---------|-------------|
| `infrared-transmit -f 38000 50,50,50,50` | Send IR signal |

### NFC & USB
| Command | Description |
|---------|-------------|
| `nfc` | Read NFC tag |
| `usb -l` | List USB devices |

### Storage & Sharing
| Command | Description |
|---------|-------------|
| `share file.txt` | Share via Android share |
| `open file.pdf` | Open with default app |
| `open-url "https://..."` | Open URL in browser |

### Dialogs
| Command | Description |
|---------|-------------|
| `dialog confirm -t "Sure?"` | Yes/No dialog |
| `dialog text -t "Name?"` | Text input |

---

## DEVELOPMENT WORKFLOWS

### Build an Android App
```bash
# One-time setup
install-dev-tools

# Build
cd ~/my-app
./gradlew assembleDebug

# Copy to Downloads for user access
cp app/build/outputs/apk/debug/*.apk /sdcard/Download/
```

### Build a Game with Godot 4
```bash
# Export to Android
godot4 --headless --export-release "Android" /sdcard/Download/game.apk
```

### Deploy to Vercel
```bash
vercel login
cd ~/my-project
vercel --prod
```

### Deploy to Firebase
```bash
firebase login
firebase init hosting
firebase deploy
```

### Create GitHub Release
```bash
gh auth login
gh release create v1.0.0 /sdcard/Download/app.apk --title "v1.0.0"
```

### Self-Modification Loop
```bash
# Clone this app's source
git clone https://github.com/MobileDevCLI/MobileCLI.git ~/MobileCLI

# Make changes, build new version
cd ~/MobileCLI
./gradlew assembleDebug

# Copy to Downloads
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/MobileCLI-new.apk
```

---

## FILE SYSTEM & PATHS

| Path | Access | Use For |
|------|--------|---------|
| `~/` | Full R/W | Projects, data, anything |
| `/sdcard/Download/` | Full R/W | **FILES USER CAN ACCESS** |
| `/sdcard/DCIM/` | Full R/W | Photos, screenshots |
| `/sdcard/` | Full R/W | All user storage |
| `/proc/` | Read | Process info, CPU, memory |
| `/sys/` | Read | Kernel info, device settings |
| `$PREFIX/` | Full R/W | System files |

### Screenshot Locations
```bash
# Samsung Galaxy
ls /sdcard/DCIM/Screenshots/

# Other Android
ls /sdcard/Pictures/Screenshots/
```

### Temp Files
```bash
# NEVER use /tmp (permission denied)
mkdir -p ~/tmp
echo "data" > ~/tmp/myfile
```

---

## KNOWN ISSUES & WORKAROUNDS

### aapt2 ARM vs x86 Issue
**Problem:** Gradle downloads x86_64 aapt2 but we're on ARM.
**Solution:** Add to `gradle.properties`:
```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/home/android-sdk/build-tools/34.0.0/aapt2
```

### Java Version for Android Builds
**Use:** Java 17 for building, **Target:** Java 11 in build.gradle

### Android SDK Location
```properties
# local.properties
sdk.dir=/data/data/com.termux/files/home/android-sdk
```

---

## RULES & BEST PRACTICES

### Critical Rules
1. **THIS IS ARM ANDROID, NOT x86 LINUX** - Architecture matters
2. **NEVER USE /tmp** - Permission denied. Use `~/tmp` or `$PREFIX/tmp`
3. **SAVE TO /sdcard/Download/** - Only place user can easily access
4. **USE HARDWARE COMMANDS** - 80+ commands for Android features
5. **NEVER HALLUCINATE** - If unsure, test it first
6. **VERIFY BEFORE CLAIMING** - Don't say something works without testing

### Common Pitfalls

**DON'T use /tmp:**
```bash
# WRONG
curl -o /tmp/file.zip https://...
# RIGHT
curl -o ~/file.zip https://...
```

**DON'T forget Downloads folder:**
```bash
# WRONG - user can't find this
cp output.apk ~/output.apk
# RIGHT
cp output.apk /sdcard/Download/output.apk
```

---

## ROOT-EQUIVALENT ACCESS (79 Permissions)

| Category | Capabilities |
|----------|-------------|
| **Storage** | All files, photos, videos, documents |
| **Camera** | Take photos, record video |
| **Microphone** | Record audio |
| **Phone** | Make/answer calls, call log |
| **SMS** | Send/receive messages |
| **Contacts** | Read/write contacts |
| **Calendar** | Read/write events |
| **Location** | GPS, network, background |
| **Bluetooth** | Scan, connect, advertise |
| **WiFi** | Scan networks, connection info |
| **NFC** | Read/write tags |
| **Sensors** | All device sensors |
| **Biometrics** | Fingerprint, face |
| **IR Blaster** | Infrared transmission |
| **System** | Notifications, alarms, install apps |

---

## THE ACHIEVEMENT

MobileCLI is:
- **Built by AI** - Claude created and maintains this environment
- **Self-modifying** - AI can rebuild its own container
- **Multi-AI** - Claude, Gemini, Codex can work together
- **Hardware-integrated** - 80+ commands for phone features

**Project Info:**
- Website: https://mobilecli.com
- GitHub: https://github.com/MobileDevCLI
- Version: 6.0.0 (All API Scripts Fixed)
