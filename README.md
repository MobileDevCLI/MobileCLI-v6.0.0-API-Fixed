# MobileCLI v5.0.0

**The Complete Professional Development Environment for Android**

Build Android apps. Create games. Deploy websites. All from your phone.

[![Version](https://img.shields.io/badge/version-5.0.0-blue.svg)](https://github.com/MobileDevCLI/MobileCLI)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)](https://play.google.com/store)

---

## What is MobileCLI?

MobileCLI transforms your Android phone into a complete development workstation. Not a toy. Not a demo. A real, professional environment where AI builds real software.

**This is the app where Claude fixed its own bugs and rebuilt itself.**

### What You Can Build

| Type | Tools | Deploy To |
|------|-------|-----------|
| **Android Apps** | Java 17, Kotlin, Gradle | Direct install |
| **Games** | Godot 4 engine | Android, iOS, Web |
| **Websites** | Any framework | Vercel, Netlify, Heroku |
| **Mobile Apps** | React Native, Expo | App Store, Play Store |
| **Shopify** | Themes, Apps | Shopify stores |
| **Backends** | Node.js, Python | Any platform |

---

## Three AI Assistants Built In

| AI | Package | Capabilities |
|----|---------|--------------|
| **Claude Code** | @anthropic-ai/claude-code | Full autonomous coding |
| **Gemini CLI** | @google/gemini-cli | Google's AI assistant |
| **Codex** | @openai/codex | OpenAI's coding AI |

### Multi-AI Development

```bash
# AIs can collaborate without human intervention
claude "Write a function" > code.js
gemini "Review this code" < code.js
codex "Add tests" < code.js
```

---

## 18 Professional CLIs Pre-Installed

### Deployment Platforms
| CLI | Version | Purpose |
|-----|---------|---------|
| `vercel` | 50.10.0 | Static sites, Next.js, serverless |
| `netlify` | 23.14.0 | Static sites, forms, functions |
| `heroku` | 8.7.1 | Full applications, databases |
| `gh` | (system) | GitHub CLI - repos, PRs, releases |

### Mobile Development
| CLI | Version | Purpose |
|-----|---------|---------|
| `expo` | 6.3.12 | React Native development |
| `eas` | 16.32.0 | App Store / Play Store builds |

### CMS & Services
| CLI | Version | Purpose |
|-----|---------|---------|
| `shopify` | 3.90.0 | Shopify themes, apps, Hydrogen |
| `sanity` | 5.7.0 | Headless CMS |
| `contentful` | 3.10.2 | Content management |
| `twilio` | 6.2.3 | SMS, voice, WhatsApp |
| `doppler` | 1.0.0 | Secrets management |

### Development Tools
| CLI | Version | Purpose |
|-----|---------|---------|
| `godot4` | (system) | 2D/3D game engine |
| `turso` | 0.1.0 | Edge SQLite database |
| `lin` | 0.0.5 | Linear issue tracking |

---

## 80+ Hardware Commands

Full command-line access to your phone's hardware:

### What You Can Control
| Category | Examples |
|----------|----------|
| **Camera** | Take photos, switch cameras |
| **Audio** | Record, play, text-to-speech |
| **SMS** | Send/receive messages |
| **Phone** | Make calls, access call log |
| **Location** | GPS coordinates |
| **Sensors** | Accelerometer, gyroscope, light |
| **Bluetooth** | Scan, pair, connect |
| **WiFi** | Scan, connect, get info |
| **NFC** | Read/write tags |
| **IR Blaster** | Control TVs and devices |
| **Fingerprint** | Biometric authentication |
| **Notifications** | Toast, system notifications |
| **Device** | Vibrate, flashlight, brightness |

---

## 79 Android Permissions

Root-equivalent access without root:

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

## Quick Start

### Deploy a Website
```bash
# Create and deploy in minutes
mkdir my-site && cd my-site
echo "<h1>Hello World</h1>" > index.html
vercel login
vercel --prod
# Site is live!
```

### Build an Android App
```bash
cd ~/my-app
./gradlew assembleDebug
cp app/build/outputs/apk/debug/*.apk /sdcard/Download/
# Install from Downloads
```

### Create a Game
```bash
godot4 --headless --export-release "Android" /sdcard/Download/game.apk
```

### Send an SMS
```bash
sms-send -n "+1234567890" "Hello from MobileCLI!"
```

### Take a Photo
```bash
camera-photo /sdcard/Download/photo.jpg
```

---

## The Achievement

This app was built by AI running on an Android phone.

**The Self-Modification Loop:**
```
AI runs inside MobileCLI
     ↓
AI reads its own source code
     ↓
AI fixes bugs and adds features
     ↓
AI builds new APK with Gradle
     ↓
User installs new version
     ↓
AI runs inside new MobileCLI
     ↓
∞
```

In v5.0.0, Claude:
- Found 6 bugs causing 70+ commands to silently fail
- Fixed permission issues in AndroidManifest.xml
- Fixed race conditions in the API receiver
- Fixed path normalization bugs
- Rebuilt the APK
- Created the GitHub release

All from inside the app, on a phone.

---

## Building from Source

### Requirements
- Android SDK (API 34)
- Java 17
- Gradle 8.2+

### Build on Phone
```bash
# Install tools
pkg install openjdk-17 gradle aapt2

# Clone
git clone https://github.com/MobileDevCLI/MobileCLI.git
cd MobileCLI

# Configure
echo "sdk.dir=$HOME/android-sdk" > local.properties

# Enable ARM aapt2 (in gradle.properties)
# android.aapt2FromMavenOverride=...

# Build
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Build on Desktop
```bash
git clone https://github.com/MobileDevCLI/MobileCLI.git
cd MobileCLI
# Configure local.properties with your SDK path
./gradlew assembleDebug
```

---

## v5.0.0 Release Notes

**All 70+ hardware commands now work correctly.**

### Fixes
| # | Issue | Fix |
|---|-------|-----|
| 1 | Commands silently failed | Removed permission restriction from API receiver |
| 2 | Results not written | Fixed path normalization |
| 3 | Toast race condition | Added UI thread synchronization |
| 4 | Vibrate permission | Added runtime check |
| 5 | Torch crash | Fixed camera array bounds |
| 6 | API timing | Increased result wait time |

---

## Documentation

| Document | Description |
|----------|-------------|
| [CLAUDE.md](CLAUDE.md) | AI environment briefing |
| [docs/V5_API_FIX.md](docs/V5_API_FIX.md) | Technical bug analysis |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System architecture |
| [CHANGELOG.md](CHANGELOG.md) | Version history |

---

## Links

- **Website**: https://mobilecli.com
- **Download**: https://mobilecli.com/download.html
- **GitHub**: https://github.com/MobileDevCLI

---

## License

Apache 2.0 License. See [LICENSE](LICENSE) for details.

---

**MobileCLI v5.0.0** - Build anything. Deploy anywhere. From your phone.
