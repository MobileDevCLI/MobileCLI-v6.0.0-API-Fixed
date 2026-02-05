# MobileCLI Engineering Chronicle

**26 Days of Building the Most Powerful AI Development Environment on Android**

*This document chronicles the complete engineering journey of MobileCLI Pro - from concept to production release.*

---

## Table of Contents

1. [The Vision](#the-vision)
2. [Core Innovation](#core-innovation)
3. [Architecture Deep Dive](#architecture-deep-dive)
4. [The TermuxApiReceiver: 39+ Android API Bridges](#the-termuxapireceiver)
5. [The PayPal Integration Battle](#the-paypal-integration-battle)
6. [Infrastructure & Backend](#infrastructure--backend)
7. [CLI Tools Built](#cli-tools-built)
8. [Major Bugs & Fixes](#major-bugs--fixes)
9. [The Self-Modification Loop](#the-self-modification-loop)
10. [Lessons Learned](#lessons-learned)
11. [Version History](#version-history)

---

## The Vision

MobileCLI Pro was born from a simple but ambitious question: **What if an AI could have root-equivalent access to an Android phone and rebuild its own container app?**

The answer became MobileCLI - a complete Linux development environment running on Android that:
- Grants AI access to 79 Android permissions (cameras, SMS, GPS, Bluetooth, NFC, sensors, etc.)
- Provides full dev tools (Java 17, Python, Node.js, Gradle, Android SDK)
- Enables self-modification (the AI can rebuild the very app it runs inside)
- Creates a persistent memory system for AI learnings

**The Achievement:** This app was built by AI running on an Android phone. The AI built its own container.

---

## Core Innovation

### 1. Root-Equivalent Access Without Root

MobileCLI requests **79 Android permissions** - nearly every capability available to any app:

| Category | Capabilities |
|----------|-------------|
| **Storage** | All files, photos, videos, documents |
| **Camera** | Take photos, record video |
| **Microphone** | Record audio |
| **Phone** | Make/answer calls, read call log |
| **SMS** | Send/receive SMS, MMS |
| **Contacts** | Read/write contacts |
| **Calendar** | Read/write calendar events |
| **Location** | GPS, network location, background location |
| **Bluetooth** | Scan, connect, advertise |
| **WiFi** | Scan networks, connection info |
| **NFC** | Read/write NFC tags |
| **Sensors** | Accelerometer, gyro, proximity, light |
| **Biometrics** | Fingerprint, face authentication |
| **IR Blaster** | Control TVs/devices via infrared |

Plus access to `/proc/`, `/sys/`, and system files - all without actual root.

### 2. Custom Termux API Bridge

The heart of MobileCLI is `TermuxApiReceiver.kt` - a 2000+ line Kotlin file that bridges 75+ shell commands to Android APIs:

```
termux-camera-photo → Intent → TermuxApiReceiver.onReceive() → Camera2 API → Photo saved
```

Every `termux-*` command goes through this receiver, which handles:
- Parameter parsing
- Permission checking
- Android API calls
- Result formatting (JSON output)
- Error handling

### 3. Self-Modification Capability

The self-modification loop enables AI to improve its own container:

```
AI runs inside MobileCLI
    → modifies source code
    → rebuilds APK via Gradle
    → installs new version
    → repeat ∞
```

This required solving the ARM aapt2 problem (Gradle downloads x86 binaries, but we're on ARM64).

---

## Architecture Deep Dive

### File Structure

```
app/src/main/java/com/termux/
├── BootstrapInstaller.kt     # Termux environment setup (700+ lines)
├── SetupWizard.kt            # 4-stage first-run wizard
├── MainActivity.kt           # Terminal UI with navigation drawer
├── TermuxApiReceiver.kt      # 39+ API method handlers (2000+ lines)
├── TermuxApplication.kt      # App lifecycle management
├── auth/
│   ├── SupabaseClient.kt     # Supabase authentication
│   ├── LoginActivity.kt      # Google OAuth flow
│   ├── SplashActivity.kt     # Launch screen with license check
│   ├── AccountActivity.kt    # Account management UI
│   ├── LicenseManager.kt     # Subscription verification
│   └── PaywallActivity.kt    # Payment UI with PayPal
├── app/
│   ├── TermuxService.kt      # Background terminal service
│   ├── TermuxOpenReceiver.kt # Intent handling
│   └── RunCommandService.kt  # External command execution
└── filepicker/
    └── TermuxDocumentsProvider.kt  # SAF integration
```

### Bootstrap System

`BootstrapInstaller.kt` handles first-run setup:

1. **Extract Bootstrap** - Unpack pre-built Termux environment (arch-specific)
2. **Configure Shell** - Set up bash with proper PS1, PATH, aliases
3. **Install Core Packages** - Essential tools via pkg
4. **Set Permissions** - Configure file permissions for security

Key implementation detail: The bashrc content is embedded in Kotlin as a multi-line string, not a separate file.

### Session Management

MobileCLI supports multiple terminal sessions with persistence:
- Each tab is an independent bash session
- Sessions survive app backgrounding
- Scroll-back buffer preserved
- Working directory maintained

### File-based IPC Workaround

Android restricts direct process communication. MobileCLI uses temp files:

```kotlin
// URL opening via temp file
val tempFile = File(filesDir, "url_to_open.txt")
tempFile.writeText(url)
// Another component reads this file and opens the URL
```

This pattern is used for:
- URL opening
- File sharing
- Clipboard operations (in some cases)

---

## The TermuxApiReceiver

`TermuxApiReceiver.kt` is the bridge between shell commands and Android APIs. Here are the 39+ methods it implements:

### Communication
| Method | Shell Command | Android API |
|--------|--------------|-------------|
| `onReceiveSmsListCommand()` | `termux-sms-list` | SMS ContentProvider |
| `onReceiveSmsSendCommand()` | `termux-sms-send` | SmsManager |
| `onReceiveContactListCommand()` | `termux-contact-list` | Contacts ContentProvider |
| `onReceiveCallLogCommand()` | `termux-call-log` | CallLog ContentProvider |
| `onReceiveTelephonyDeviceInfo()` | `termux-telephony-deviceinfo` | TelephonyManager |
| `onReceiveTelephonyCellInfo()` | `termux-telephony-cellinfo` | TelephonyManager |

### Sensors & Location
| Method | Shell Command | Android API |
|--------|--------------|-------------|
| `onReceiveLocationCommand()` | `termux-location` | LocationManager/FusedLocation |
| `onReceiveSensorCommand()` | `termux-sensor` | SensorManager |
| `onReceiveBatteryStatusCommand()` | `termux-battery-status` | BatteryManager |
| `onReceiveBrightnessCommand()` | `termux-brightness` | Settings.System |

### Media
| Method | Shell Command | Android API |
|--------|--------------|-------------|
| `onReceiveCameraPhotoCommand()` | `termux-camera-photo` | Camera2 API |
| `onReceiveCameraInfoCommand()` | `termux-camera-info` | CameraManager |
| `onReceiveMediaPlayerCommand()` | `termux-media-player` | MediaPlayer |
| `onReceiveAudioInfoCommand()` | `termux-audio-info` | AudioManager |
| `onReceiveVolumeCommand()` | `termux-volume` | AudioManager |
| `onReceiveTtsCommand()` | `termux-tts-speak` | TextToSpeech |

### Connectivity
| Method | Shell Command | Android API |
|--------|--------------|-------------|
| `onReceiveWifiConnectionInfo()` | `termux-wifi-connectioninfo` | WifiManager |
| `onReceiveWifiScanInfo()` | `termux-wifi-scaninfo` | WifiManager |
| `onReceiveBluetoothScanInfo()` | `termux-bluetooth-scaninfo` | BluetoothAdapter |
| `onReceiveNfcCommand()` | `termux-nfc` | NfcAdapter |
| `onReceiveInfraredTransmit()` | `termux-infrared-transmit` | ConsumerIrManager |
| `onReceiveUsbCommand()` | `termux-usb` | UsbManager |

### System
| Method | Shell Command | Android API |
|--------|--------------|-------------|
| `onReceiveNotificationCommand()` | `termux-notification` | NotificationManager |
| `onReceiveToastCommand()` | `termux-toast` | Toast |
| `onReceiveVibrateCommand()` | `termux-vibrate` | Vibrator |
| `onReceiveTorchCommand()` | `termux-torch` | CameraManager (flashlight) |
| `onReceiveWallpaperCommand()` | `termux-wallpaper` | WallpaperManager |
| `onReceiveClipboardGet()` | `termux-clipboard-get` | ClipboardManager |
| `onReceiveClipboardSet()` | `termux-clipboard-set` | ClipboardManager |
| `onReceiveFingerprintCommand()` | `termux-fingerprint` | FingerprintManager |
| `onReceiveDialogCommand()` | `termux-dialog` | AlertDialog |
| `onReceiveDownloadCommand()` | `termux-download` | DownloadManager |
| `onReceiveShareCommand()` | `termux-share` | Intent.ACTION_SEND |
| `onReceiveStorageGetCommand()` | `termux-storage-get` | SAF |
| `onReceiveOpenUrlCommand()` | `termux-open-url` | Intent.ACTION_VIEW |
| `onReceiveWakeLockCommand()` | `termux-wake-lock` | PowerManager |
| `onReceiveWakeUnlockCommand()` | `termux-wake-unlock` | PowerManager |

Each method:
1. Parses intent extras for parameters
2. Checks relevant permissions
3. Calls the appropriate Android API
4. Formats results as JSON
5. Returns via result receiver or stdout

---

## The PayPal Integration Battle

**Duration:** 3 days of debugging
**Problem:** PayPal uses different email than Supabase auth

### The Challenge

Users authenticate with Google OAuth (Supabase), then pay with PayPal. The emails often don't match:
- Supabase: user@gmail.com
- PayPal: user@hotmail.com (or any other email)

PayPal webhooks send the PayPal email, not the Google email, so we couldn't match payments to users.

### Failed Approaches

1. **Email matching** - Doesn't work when emails differ
2. **Client-side subscription creation** - PayPal returns JSON string, not approval URL
3. **Direct API calls** - Still couldn't embed user_id

### The Solution: Server-Side Subscription Creation with custom_id

```typescript
// Edge Function: create-subscription/index.ts
const subscriptionPayload = {
  plan_id: "P-...",
  custom_id: user_id,  // <-- This is the key!
  subscriber: {
    email_address: email
  },
  application_context: {
    return_url: "https://mobilecli.com/payment-success",
    cancel_url: "https://mobilecli.com/payment-cancelled"
  }
};
```

PayPal's `custom_id` field:
- Embedded during subscription creation
- Returned in ALL webhook events
- Max 127 characters
- Survives the entire subscription lifecycle

### Webhook Handler

```typescript
// Edge Function: paypal-webhook/index.ts
const customId = webhookEvent.resource?.custom_id;

// Use custom_id (user_id) instead of email matching
const { data: subscription } = await supabaseAdmin
  .from('subscriptions')
  .upsert({
    user_id: customId,
    status: 'active',
    paypal_subscription_id: subscriptionId
  });
```

### Payment Flow (Final)

```
1. User logs in with Google (Supabase auth)
2. User taps "Subscribe" in PaywallActivity
3. App calls create-subscription Edge Function with user_id
4. Edge Function creates PayPal subscription with custom_id = user_id
5. App opens PayPal approval URL
6. User completes payment in PayPal
7. PayPal sends webhook with custom_id
8. Webhook handler matches by custom_id, not email
9. Subscription activated!
```

---

## Infrastructure & Backend

### Supabase Setup

**Project:** `zuifztlxttaxppxspdgo`

**Tables:**
- `auth.users` - Supabase managed authentication
- `subscriptions` - Payment status, PayPal IDs, dates
- `webhook_logs` - All PayPal events for debugging
- `payment_history` - Transaction records

**Subscription Table Schema:**
```sql
CREATE TABLE subscriptions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES auth.users(id),
  status TEXT DEFAULT 'inactive',
  paypal_subscription_id TEXT,
  paypal_payer_id TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  expires_at TIMESTAMPTZ,
  trial_started_at TIMESTAMPTZ,
  trial_reminder_sent BOOLEAN DEFAULT FALSE,
  payment_failed_at TIMESTAMPTZ,
  last_payment_at TIMESTAMPTZ,
  cancelled_at TIMESTAMPTZ,
  cancel_reason TEXT,
  admin_notes TEXT
);
```

### Edge Functions

**1. create-subscription**
- Creates PayPal subscription with custom_id
- Returns approval URL for redirect
- Handles initial trial setup

**2. paypal-webhook**
- Verifies PayPal webhook signatures
- Handles 8 event types:
  - BILLING.SUBSCRIPTION.ACTIVATED
  - BILLING.SUBSCRIPTION.CANCELLED
  - BILLING.SUBSCRIPTION.SUSPENDED
  - BILLING.SUBSCRIPTION.EXPIRED
  - BILLING.SUBSCRIPTION.UPDATED
  - PAYMENT.SALE.COMPLETED
  - PAYMENT.SALE.DENIED
  - PAYMENT.SALE.REFUNDED
- Logs all events for debugging
- Updates subscription status

### GitHub Repositories

Over the 26 days, we created 17+ repositories in the MobileDevCLI organization:

| Repository | Purpose |
|------------|---------|
| MobileCLI-v2 | Main development repo |
| MobileCLI-Pro | Production source |
| MobileCLI-v2.1-Release | Clean release repo |
| website | mobilecli.com landing page |
| documentation | Standalone docs |
| supabase-functions | Edge functions |

---

## CLI Tools Built

### install-dev-tools

Installs complete Android development environment:
```bash
#!/bin/bash
pkg install -y openjdk-17 gradle aapt2 d8 apksigner
# Configure JAVA_HOME, ANDROID_HOME
# Set up gradle.properties with ARM aapt2 override
```

### mobilecli-rebuild

Rebuilds MobileCLI from source:
```bash
#!/bin/bash
cd ~/MobileCLI-v2
./gradlew assembleDebug
cp app/build/outputs/apk/debug/*.apk /sdcard/Download/
termux-notification -t "Build Complete" -c "APK ready in Downloads"
```

### mobilecli-memory

Manages AI memory system:
```bash
#!/bin/bash
case "$1" in
  status)
    echo "Memory System Status:"
    cat ~/.mobilecli/memory/evolution_history.json | jq .
    ;;
  add)
    # Add new learning to memory
    ;;
  clear)
    # Clear specific memory category
    ;;
esac
```

### mobilecli-caps

Displays all capabilities:
```bash
#!/bin/bash
echo "MobileCLI Capabilities:"
echo "======================"
echo "Permissions: 79"
echo "API Commands: 75+"
echo "Dev Tools: Java 17, Python 3, Node.js, Gradle"
# ... etc
```

### selfmod

Self-modification wizard:
```bash
#!/bin/bash
echo "MobileCLI Self-Modification Wizard"
echo "=================================="
echo "1. Extract bundled source"
echo "2. Open source in editor"
echo "3. Build modified version"
echo "4. Install new APK"
```

### setup-github

Configures GitHub credentials:
```bash
#!/bin/bash
read -p "GitHub username: " username
read -sp "GitHub token: " token
gh auth login --with-token <<< "$token"
git config --global user.name "$username"
```

### extract-source

Extracts bundled source code:
```bash
#!/bin/bash
# Extract source from APK assets to ~/MobileCLI-source/
unzip -o /data/app/.../base.apk -d /tmp/apk
cp -r /tmp/apk/assets/source/* ~/MobileCLI-source/
```

---

## Major Bugs & Fixes

### BUG-001: Welcome Message Blocking Terminal Output

**Severity:** High
**Date Fixed:** January 26, 2026

**Problem:** A welcome message was printed on every new bash session, clearing Claude Code's output.

**Root Cause:** `BootstrapInstaller.kt` wrote a bashrc that included:
```bash
clear
echo "  ╔═══════════════════════════════════════╗"
echo "  ║       Welcome to MobileCLI            ║"
# ... more banner lines
```

**Fix:** Removed the entire welcome message block from bashrc content.

**Prevention:** Never add `echo` or `clear` commands that run on every session.

---

### BUG-002: JSON Subscription Parsing Failure

**Severity:** Critical
**Date Fixed:** January 25, 2026

**Problem:** `LicenseManager.kt` crashed when parsing subscription data.

**Root Cause:** Kotlin data class for `Subscription` was missing fields that existed in the database.

**Fix:** Added ALL 15 fields to the data class:
```kotlin
@Serializable
data class Subscription(
    val id: String,
    val user_id: String,
    val status: String,
    val paypal_subscription_id: String?,
    val paypal_payer_id: String?,
    val created_at: String?,
    val updated_at: String?,
    val expires_at: String?,
    val trial_started_at: String?,
    val trial_reminder_sent: Boolean?,
    val payment_failed_at: String?,
    val last_payment_at: String?,
    val cancelled_at: String?,
    val cancel_reason: String?,
    val admin_notes: String?
)
```

**Prevention:** Always include all database columns in data classes, even if not used.

---

### BUG-003: Email Display Mismatch

**Severity:** Low
**Date Fixed:** January 25, 2026

**Problem:** Account screen showed PayPal email instead of Google email.

**Root Cause:** Using PayPal response email instead of Supabase auth email.

**Fix:** Always use `supabaseClient.auth.currentUser.email` for display.

---

### BUG-004: ARM aapt2 Build Failure

**Severity:** Critical
**Date Fixed:** January 20, 2026

**Problem:** Gradle builds failed because it downloaded x86_64 aapt2.

**Root Cause:** Maven repository provides x86 binaries, but we're on ARM64.

**Fix:** Added to `gradle.properties`:
```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/home/android-sdk/build-tools/34.0.0/aapt2
```

---

## The Self-Modification Loop

The crown jewel of MobileCLI is the ability for AI to modify and rebuild its own container:

```
┌─────────────────────────────────────────────────────────────┐
│                    SELF-MODIFICATION LOOP                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌──────────────┐                                          │
│   │ AI (Claude)  │                                          │
│   │ Running in   │                                          │
│   │ MobileCLI    │                                          │
│   └──────┬───────┘                                          │
│          │                                                   │
│          ▼                                                   │
│   ┌──────────────┐     ┌──────────────┐     ┌────────────┐ │
│   │ Identify     │────▶│ Modify       │────▶│ Build APK  │ │
│   │ Improvement  │     │ Source Code  │     │ via Gradle │ │
│   └──────────────┘     └──────────────┘     └─────┬──────┘ │
│                                                    │        │
│          ┌────────────────────────────────────────┘        │
│          │                                                  │
│          ▼                                                  │
│   ┌──────────────┐     ┌──────────────┐                    │
│   │ Install New  │────▶│ AI Now Runs  │                    │
│   │ APK Version  │     │ in Improved  │                    │
│   │              │     │ Container    │─────────────┐      │
│   └──────────────┘     └──────────────┘             │      │
│                                                      │      │
│   ┌──────────────────────────────────────────────────┘      │
│   │                                                         │
│   └─────────────────────▶ REPEAT ∞                         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Requirements for Self-Modification

1. **Build Tools** - Java 17, Gradle, aapt2, d8, apksigner
2. **ARM aapt2 Fix** - Override in gradle.properties
3. **Android SDK** - Platform 34, build-tools 34.0.0
4. **Source Code** - Bundled in assets or cloned from GitHub
5. **Signing Key** - Debug key auto-generated, release requires keystore

### Example Self-Modification Session

```bash
# AI identifies a bug and decides to fix it
$ cd ~/MobileCLI-v2
$ # Edit the problematic file
$ nano app/src/main/java/com/termux/BootstrapInstaller.kt
$ # Remove the problematic code...

# Build the fixed version
$ JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk \
  ./gradlew assembleDebug

# Copy to Downloads for installation
$ cp app/build/outputs/apk/debug/app-debug.apk \
  /sdcard/Download/MobileCLI-fixed.apk

# Notify user
$ termux-notification -t "Build Complete" \
  -c "Fixed APK ready in Downloads"
```

---

## Lessons Learned

### Technical Lessons

1. **Android != Linux**
   - Never use `/tmp` (permission denied)
   - Use Termux API commands, not Linux equivalents
   - ARM64 binaries only - no x86

2. **PayPal Integration**
   - `custom_id` is the key to user matching
   - Server-side subscription creation required
   - Always log webhook events for debugging

3. **Kotlin Data Classes**
   - Include ALL database fields or JSON parsing fails
   - Use nullable types for optional fields
   - @Serializable annotation required for kotlinx.serialization

4. **Gradle on ARM**
   - Override aapt2 path in gradle.properties
   - Use Java 17 for building, target Java 11
   - SDK path must be absolute

5. **bashrc Gotchas**
   - Never `clear` or print banners on session start
   - It breaks tools that read terminal output
   - Use one-time flags if branding needed

### Process Lessons

1. **Version Control Everything**
   - New versions, don't overwrite
   - Document all changes
   - Keep multiple repo backups

2. **Test on Real Device**
   - Emulators don't have all permissions
   - Real payment flows needed for PayPal
   - Termux API only works on real phones

3. **Document As You Go**
   - Bug fixes need documentation
   - Architecture decisions need rationale
   - Setup steps need exact commands

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| v2.1.1 | 2026-01-26 | Fixed welcome message bug |
| v2.1.0 | 2026-01-26 | PayPal integration complete, clean release |
| v2.0.0 | 2026-01-20 | Authentication system, Supabase integration |
| v1.5.0 | 2026-01-15 | Self-modification capability |
| v1.0.0 | 2026-01-01 | Initial release with 79 permissions |

---

## Credits

MobileCLI Pro was built through a unique collaboration:
- **Human Developer:** Vision, testing, deployment
- **Claude (AI):** Code implementation, debugging, documentation

This document itself was written by AI, about an app built by AI, running on Android.

**The future of development is here - and it fits in your pocket.**

---

*Last Updated: January 26, 2026*
*MobileCLI v2.1.1*
