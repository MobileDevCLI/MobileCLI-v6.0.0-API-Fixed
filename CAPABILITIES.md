# MobileCLI Complete Capabilities Guide

**Version 6.0.0** | The Most Powerful Mobile Development Environment

---

## Table of Contents

1. [Overview](#overview)
2. [AI Assistants](#ai-assistants)
3. [Hardware Commands (80+)](#hardware-commands)
4. [Development & Deployment CLIs](#development--deployment-clis)
5. [Build Capabilities](#build-capabilities)
6. [System Access](#system-access)
7. [Command Reference](#command-reference)

---

## Overview

MobileCLI transforms your Android phone into a complete development workstation. It combines:

- **4 AI Coding Assistants** - Claude, Gemini, Codex, and OpenRouter
- **80+ Hardware Commands** - Full access to phone sensors and features
- **18 Platform CLIs** - Deploy to Vercel, Firebase, GitHub, and more
- **Complete Build Tools** - Java 17, Gradle, Android SDK on ARM

### What Makes MobileCLI Unique

| Feature | MobileCLI | Traditional Terminal Apps |
|---------|-----------|--------------------------|
| AI Integration | 4 built-in AI assistants | None |
| Hardware Access | 80+ commands, all sensors | Limited or none |
| App Building | Full Android SDK, can build APKs | Cannot build apps |
| Self-Modification | Can rebuild itself | Static |
| Permissions | 79 Android permissions | ~10 permissions |

---

## AI Assistants

MobileCLI includes four AI coding assistants that can work independently or together.

### Claude CLI (Anthropic)
```bash
claude "Write a Python web scraper"
claude "Review this code for security issues" < app.py
claude "Help me debug this error"
```
- Best for: Complex reasoning, code review, architecture decisions
- Context: Can read files, run commands, access the web

### Gemini CLI (Google)
```bash
gemini "Explain this codebase"
gemini "Generate unit tests for this function"
```
- Best for: Large context analysis, documentation

### Codex CLI (OpenAI)
```bash
codex "Convert this Python to JavaScript"
codex "Optimize this algorithm"
```
- Best for: Code generation, translations

### OpenRouter CLI
```bash
openrouter "Use Claude to analyze this"
openrouter --model gpt-4 "Explain quantum computing"
```
- Access to 100+ models via unified API
- Switch between Claude, GPT-4, Llama, Mistral, and more

### AI Collaboration
AIs can work together on complex tasks:
```bash
# Claude writes, Gemini reviews
claude "Write a REST API" > api.py
gemini "Review this code" < api.py > review.md

# Chain multiple AIs
claude "Design database schema" | codex "Generate SQL migrations"
```

---

## Hardware Commands

MobileCLI provides 80+ commands to access every phone feature. All commands return JSON for easy parsing.

### Clipboard & Notifications

| Command | Description | Example |
|---------|-------------|---------|
| `termux-clipboard-get` | Read clipboard | `termux-clipboard-get` |
| `termux-clipboard-set` | Write to clipboard | `termux-clipboard-set "Hello"` |
| `termux-toast` | Show toast message | `termux-toast "Done!"` |
| `termux-notification` | System notification | `termux-notification -t "Title" -c "Content"` |
| `termux-notification-remove` | Remove notification | `termux-notification-remove mynotif` |

### Device Information & Control

| Command | Description | Example Output |
|---------|-------------|----------------|
| `termux-battery-status` | Battery info | `{"percentage":85,"status":"CHARGING"}` |
| `termux-brightness` | Get/set brightness | `termux-brightness 128` |
| `termux-torch` | Flashlight control | `termux-torch on` |
| `termux-vibrate` | Vibrate device | `termux-vibrate -d 500` |
| `termux-volume` | Audio volume levels | `{"music":{"volume":10,"max":15}}` |
| `termux-audio-info` | Audio device info | `{"ringer_mode":"NORMAL"}` |

### Network & Connectivity

| Command | Description | Example Output |
|---------|-------------|----------------|
| `termux-wifi-connectioninfo` | Current WiFi | `{"ssid":"MyNetwork","ip":"192.168.1.5"}` |
| `termux-wifi-scaninfo` | Scan networks | `[{"ssid":"Network1","rssi":-65}]` |
| `termux-wifi-enable` | Enable/disable WiFi | `termux-wifi-enable true` |
| `termux-bluetooth-info` | Bluetooth status | `{"enabled":true}` |
| `termux-bluetooth-paired` | Paired devices | `[{"name":"AirPods","address":"AA:BB:CC"}]` |
| `termux-bluetooth-scaninfo` | Scan for devices | `[{"name":"Speaker","rssi":-70}]` |
| `termux-bluetooth-connect` | Connect to device | `termux-bluetooth-connect AA:BB:CC:DD:EE:FF` |

### Location & Sensors

| Command | Description | Example Output |
|---------|-------------|----------------|
| `termux-location` | GPS coordinates | `{"latitude":37.7749,"longitude":-122.4194}` |
| `termux-sensor -l` | List sensors | `[{"name":"Accelerometer","type":1}]` |
| `termux-sensor -s accelerometer` | Read sensor | `{"values":[0.1,9.8,0.3]}` |
| `termux-sensor -s gyroscope` | Gyroscope data | `{"values":[0.01,-0.02,0.00]}` |

### Camera & Media

| Command | Description | Example |
|---------|-------------|---------|
| `termux-camera-info` | List cameras | `[{"id":"0","facing":"back"}]` |
| `termux-camera-photo` | Take photo | `termux-camera-photo -o photo.jpg` |
| `termux-media-player` | Play audio | `termux-media-player play song.mp3` |
| `termux-media-scan` | Scan media file | `termux-media-scan photo.jpg` |
| `termux-microphone-record` | Record audio | `termux-microphone-record -f rec.m4a -l 10` |

### Text-to-Speech & Speech Recognition

| Command | Description | Example |
|---------|-------------|---------|
| `termux-tts-engines` | List TTS engines | `[{"name":"com.google.android.tts"}]` |
| `termux-tts-speak` | Speak text | `termux-tts-speak "Hello world"` |
| `termux-speech-to-text` | Voice to text | Returns transcribed speech |

### Telephony & SMS

| Command | Description | Example |
|---------|-------------|---------|
| `termux-telephony-deviceinfo` | Phone info | `{"network_operator":"T-Mobile"}` |
| `termux-telephony-cellinfo` | Cell tower info | `[{"type":"LTE","registered":true}]` |
| `termux-telephony-call` | Make phone call | `termux-telephony-call +1234567890` |
| `termux-sms-list` | List SMS | `termux-sms-list -l 10` |
| `termux-sms-send` | Send SMS | `termux-sms-send -n +1234567890 "Hello"` |
| `termux-call-log` | Call history | `termux-call-log -l 10` |
| `termux-contact-list` | All contacts | Returns JSON array |

### Biometrics & Security

| Command | Description | Example |
|---------|-------------|---------|
| `termux-fingerprint` | Fingerprint auth | Returns success/failure |
| `termux-keystore` | Hardware keystore | `termux-keystore list` |
| `termux-keystore generate` | Generate key | `termux-keystore generate -a mykey` |

### System Utilities

| Command | Description | Example |
|---------|-------------|---------|
| `termux-wake-lock` | Keep CPU awake | Prevents sleep during tasks |
| `termux-wake-unlock` | Release wake lock | Allow normal sleep |
| `termux-wallpaper` | Set wallpaper | `termux-wallpaper -f image.jpg` |
| `termux-download` | Download file | `termux-download https://example.com/file.zip` |
| `termux-share` | Share content | `echo "text" \| termux-share` |
| `termux-storage-get` | Pick file | Opens file picker |
| `termux-open` | Open file/URL | `termux-open document.pdf` |
| `termux-open-url` | Open URL | `termux-open-url https://google.com` |

### Dialogs & UI

| Command | Description | Example |
|---------|-------------|---------|
| `termux-dialog` | Show input dialog | `termux-dialog -t "Name?"` |
| `termux-dialog confirm` | Yes/No dialog | Returns user choice |
| `termux-dialog checkbox` | Checkbox list | Multiple selection |

### Hardware (Device-Specific)

| Command | Description | Notes |
|---------|-------------|-------|
| `termux-infrared-frequencies` | IR frequency range | Requires IR blaster |
| `termux-infrared-transmit` | Send IR signal | Control TVs, etc. |
| `termux-nfc` | Read NFC tag | Requires NFC hardware |
| `termux-usb` | List USB devices | OTG support required |

### Storage Access Framework (SAF)

| Command | Description | Example |
|---------|-------------|---------|
| `termux-saf-managedir` | Select directory | Opens system picker |
| `termux-saf-ls` | List contents | `termux-saf-ls <uri>` |
| `termux-saf-read` | Read file | `termux-saf-read <uri>` |
| `termux-saf-write` | Write file | `termux-saf-write <uri> "content"` |
| `termux-saf-mkdir` | Create directory | `termux-saf-mkdir <uri> "folder"` |
| `termux-saf-create` | Create file | `termux-saf-create <uri> "file.txt"` |
| `termux-saf-rm` | Delete file | `termux-saf-rm <uri>` |
| `termux-saf-stat` | File info | `termux-saf-stat <uri>` |

---

## Development & Deployment CLIs

MobileCLI includes 18 platform CLIs for building and deploying.

### Deployment Platforms

| CLI | Purpose | Example |
|-----|---------|---------|
| **Vercel** | Deploy websites | `vercel --prod` |
| **Firebase** | Hosting, DB, Auth | `firebase deploy` |
| **Netlify** | JAMstack hosting | `netlify deploy` |
| **Heroku** | App hosting | `heroku create` |
| **Fly.io** | Global deployment | `fly launch` |
| **GitHub** | Repos, PRs, CI/CD | `gh repo create` |
| **EAS** | React Native builds | `eas build` |

### Backend & Database

| CLI | Purpose | Example |
|-----|---------|---------|
| **Supabase** | Postgres + Auth | `supabase start` |
| **Convex** | Realtime backend | `convex dev` |

### Communication & Bots

| CLI | Purpose | Example |
|-----|---------|---------|
| **Telegram** | Build bots | Bot API integration |
| **Discord** | Build bots | Discord.js support |
| **Slack** | Build apps | Slack API integration |
| **Twilio** | SMS/Voice APIs | `twilio api` |

### Content Management

| CLI | Purpose | Example |
|-----|---------|---------|
| **Sanity** | Headless CMS | `sanity start` |
| **Contentful** | Headless CMS | `contentful space` |
| **Shopify** | E-commerce | `shopify theme dev` |

### AI & Machine Learning

| CLI | Purpose | Example |
|-----|---------|---------|
| **Hugging Face** | ML models | `huggingface-cli download` |

### Game Development

| CLI | Purpose | Example |
|-----|---------|---------|
| **Godot 4** | Game engine | `godot4 --export "Android"` |

---

## Build Capabilities

### What You Can Build

| Type | Tools | Output |
|------|-------|--------|
| **Android Apps** | Java 17, Kotlin, Gradle | .apk files |
| **Games** | Godot 4 | Android, iOS, Web, Desktop |
| **Websites** | Any framework | Deploy to Vercel/Firebase |
| **APIs** | Node.js, Python | Serverless functions |
| **Bots** | Telegram, Discord, Slack | Running bots |
| **ML Apps** | Hugging Face | AI-powered apps |

### Android Development

```bash
# Install development tools (one-time)
install-dev-tools

# Create and build an Android app
mkdir ~/my-app && cd ~/my-app
# ... create your app ...
./gradlew assembleDebug

# Copy APK to Downloads (accessible from file manager)
cp app/build/outputs/apk/debug/*.apk /sdcard/Download/
```

### Game Development with Godot

```bash
# Export game to Android
godot4 --headless --export-release "Android" /sdcard/Download/game.apk

# Export to multiple platforms
godot4 --headless --export-release "Web" /sdcard/Download/game.html
```

### Web Development

```bash
# Deploy to Vercel
cd ~/my-website
vercel --prod

# Deploy to Firebase
firebase init hosting
firebase deploy

# Create GitHub release
gh release create v1.0.0 ./dist/* --title "v1.0.0"
```

### Self-Modification

MobileCLI can rebuild itself:

```bash
# Clone source
git clone https://github.com/MobileDevCLI/MobileCLI-v6.0.0-API-Fixed.git ~/MobileCLI

# Make changes, then build
cd ~/MobileCLI
./gradlew assembleDebug

# Install new version
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/MobileCLI-new.apk
```

---

## System Access

### Permissions (79 Total)

MobileCLI has near-complete access to Android features:

| Category | Capabilities |
|----------|-------------|
| **Storage** | All files, photos, videos, documents, downloads |
| **Camera** | Take photos, record video (front/back) |
| **Microphone** | Record audio, speech recognition |
| **Phone** | Make/answer calls, read call log, voicemail |
| **SMS** | Send/receive SMS and MMS |
| **Contacts** | Read/write all contacts |
| **Calendar** | Read/write calendar events |
| **Location** | GPS, network location, background location |
| **Bluetooth** | Scan, connect, advertise, transfer |
| **WiFi** | Scan networks, connection info, enable/disable |
| **NFC** | Read/write NFC tags |
| **Sensors** | Accelerometer, gyroscope, proximity, light, all |
| **Biometrics** | Fingerprint, face authentication |
| **IR Blaster** | Control TVs and devices via infrared |
| **System** | Notifications, alarms, install apps, battery |

### File System Access

| Path | Access | Use For |
|------|--------|---------|
| `~/` | Full R/W | Projects, code, data |
| `/sdcard/Download/` | Full R/W | User-accessible files |
| `/sdcard/DCIM/` | Full R/W | Photos, screenshots |
| `/sdcard/` | Full R/W | All user storage |
| `/proc/` | Read | Process info, CPU stats |
| `/sys/` | Read | Kernel info, device settings |
| `$PREFIX/` | Full R/W | System binaries and libs |

### Environment Variables

```bash
$HOME    = /data/data/com.termux/files/home
$PREFIX  = /data/data/com.termux/files/usr
$TMPDIR  = $PREFIX/tmp
$PATH    = $PREFIX/bin:...
```

---

## Command Reference

### Quick Reference Card

```bash
# Device Info
termux-battery-status          # Battery level and status
termux-volume                  # Volume levels
termux-wifi-connectioninfo     # Current WiFi network

# Communication
termux-toast "Hello!"          # Show toast
termux-notification -t "Hi"    # System notification
termux-tts-speak "Hello"       # Text to speech
termux-vibrate                 # Vibrate phone

# Media
termux-camera-photo -o pic.jpg # Take photo
termux-microphone-record -f a.m4a -l 10  # Record 10s
termux-media-player play x.mp3 # Play audio

# Location
termux-location                # GPS coordinates
termux-sensor -l               # List sensors

# Phone
termux-contact-list            # All contacts
termux-sms-list -l 10          # Recent SMS
termux-call-log -l 10          # Recent calls

# System
termux-torch on                # Flashlight on
termux-wake-lock               # Keep awake
termux-clipboard-set "text"    # Copy to clipboard
termux-share file.txt          # Share file
```

### Scripting Examples

**Morning briefing script:**
```bash
#!/data/data/com.termux/files/usr/bin/bash
BATTERY=$(termux-battery-status | jq -r '.percentage')
WIFI=$(termux-wifi-connectioninfo | jq -r '.ssid')
termux-tts-speak "Good morning. Battery at $BATTERY percent. Connected to $WIFI."
```

**Location-based automation:**
```bash
#!/data/data/com.termux/files/usr/bin/bash
LOC=$(termux-location)
LAT=$(echo $LOC | jq -r '.latitude')
termux-notification -t "Location" -c "Latitude: $LAT"
```

**Backup contacts:**
```bash
termux-contact-list > /sdcard/Download/contacts_backup.json
termux-toast "Contacts backed up!"
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 6.0.0 | 2026-02-05 | All 32 broken API scripts fixed |
| 5.7.0 | 2026-02-04 | Security & stability fixes |
| 5.6.0 | 2026-02-04 | Java 17 fix for Android builds |
| 5.0.0 | 2026-01 | File-based IPC for some APIs |

---

## Support

- **Website:** https://mobilecli.com
- **GitHub:** https://github.com/MobileDevCLI
- **Issues:** https://github.com/MobileDevCLI/MobileCLI-v6.0.0-API-Fixed/issues

---

*MobileCLI - Build Anything From Your Phone*
