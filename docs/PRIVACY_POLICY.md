# MobileCLI Pro - Privacy Policy

**Effective Date:** January 22, 2026
**Last Updated:** February 1, 2026

---

## 1. Introduction

This Privacy Policy explains how the MobileCLI Team ("we", "us", "our") collects, uses, and protects your information when you use MobileCLI Pro ("the App"). We are committed to transparency about what data we handle and how.

---

## 2. Information We Collect

### 2.1 Account Information
When you create an account, we collect:
- Email address
- Password (encrypted via Supabase — we cannot see it)
- Authentication provider used (email, Google, GitHub, Apple, Microsoft, Discord)

### 2.2 Payment Information
- **Stripe** processes card payments — we never see or store your card number
- **PayPal** processes subscription payments — we receive only transaction confirmation and subscription status
- We store: transaction IDs, subscription status, payment timestamps, and plan type

### 2.3 Subscription & License Data
- Subscription start date and status (active, trial, cancelled, expired)
- Trial start timestamp (used for 7-day free trial calculation)
- License verification timestamps

---

## 3. Device Permissions & On-Device Data

MobileCLI requests **79 Android permissions** to provide root-equivalent terminal functionality. This is a large number of permissions — here is exactly what they cover and how data is handled:

### Storage & Files
- `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`
- **Used for:** File management, project files, APK building
- **Data stays on device.** No files are uploaded to MobileCLI servers.

### Camera & Microphone
- `CAMERA`, `RECORD_AUDIO`
- **Used for:** `termux-camera-photo`, `termux-microphone-record` commands
- **Data stays on device.** Photos/recordings are saved locally only.

### Phone & SMS
- `READ_PHONE_STATE`, `CALL_PHONE`, `READ_CALL_LOG`, `SEND_SMS`, `READ_SMS`, `RECEIVE_SMS`
- **Used for:** `termux-telephony-*`, `termux-sms-*`, `termux-call-log` commands
- **Data stays on device.** No SMS content or call logs are transmitted.

### Contacts & Calendar
- `READ_CONTACTS`, `WRITE_CONTACTS`, `READ_CALENDAR`, `WRITE_CALENDAR`
- **Used for:** `termux-contact-list`, `termux-calendar-*` commands
- **Data stays on device.**

### Location
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
- **Used for:** `termux-location` command
- **Data stays on device.** No location data is sent to MobileCLI servers.

### Bluetooth, NFC, WiFi, IR, Sensors
- Various hardware access permissions
- **Used for:** `termux-bluetooth-*`, `termux-nfc`, `termux-wifi-*`, `termux-infrared-*`, `termux-sensor` commands
- **Data stays on device.**

### System
- `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`, `REQUEST_INSTALL_PACKAGES`
- **Used for:** Background terminal sessions, setup downloads, notifications, APK installation
- **Data stays on device.**

**Important:** All 79 permissions are used exclusively for on-device terminal commands. No device data (files, contacts, SMS, location, sensor readings, etc.) is collected, transmitted, or stored by MobileCLI on any external server.

---

## 4. How We Use Your Information

We use your information to:
- Authenticate your account and manage sessions
- Process subscription payments via Stripe or PayPal
- Verify your license status
- Provide the terminal application and its features
- Send important service updates (e.g., subscription expiration)
- Respond to support requests

We do **NOT** use your information for:
- Advertising or marketing profiling
- Selling to third parties
- Tracking your terminal commands or activity

---

## 5. Third-Party Service Providers

We share limited data with the following services:

| Service | Data Shared | Purpose | Their Privacy Policy |
|---------|-------------|---------|---------------------|
| **Supabase** | Email, user ID, subscription status | Authentication & database | supabase.com/privacy |
| **Stripe** | Email (for payment link) | Card payment processing | stripe.com/privacy |
| **PayPal** | Email | Subscription payment processing | paypal.com/privacy |
| **Anthropic** | Your AI queries* | Claude AI assistant | anthropic.com/privacy |
| **Google** | Your AI queries* | Gemini AI assistant | policies.google.com/privacy |
| **OpenAI** | Your AI queries* | Codex AI assistant | openai.com/privacy |

*AI queries are sent directly from your device to the AI provider when you use AI assistants. We do not proxy, store, or have access to your AI conversations.

---

## 6. Information Sharing

### 6.1 We Do NOT Sell Your Data
We will never sell, rent, or trade your personal information to third parties for any purpose.

### 6.2 Legal Requirements
We may disclose your information if required by law, court order, or government request. We will attempt to notify you before doing so, unless prohibited by law.

---

## 7. Data Storage & Security

### 7.1 Where Data is Stored
- **Account data:** Supabase cloud servers
- **Payment data:** Stripe and PayPal servers
- **Local app data:** Your device only (in `/data/data/com.termux/`)

### 7.2 Security Measures
We implement:
- HTTPS encryption for all data transmission
- Encrypted password storage (bcrypt via Supabase)
- Row Level Security (RLS) on all database tables
- Secure OAuth protocols (PKCE flow)
- EncryptedSharedPreferences for local license data
- JWT-authenticated API calls

### 7.3 Security Limitations
No system is 100% secure. While we maintain reasonable security measures appropriate for the data we handle, we cannot guarantee absolute security of internet transmissions or stored data.

---

## 8. Data Breach Notification

In the event of a data breach that affects your personal information, we will:
- Notify affected users within 72 hours (when feasible)
- Describe the nature of the breach and data affected
- Provide steps you can take to protect yourself
- Report to relevant authorities as required by law

We maintain reasonable security measures to protect your data and take breach prevention seriously.

---

## 9. Your Rights

### 9.1 Access Your Data
You can view your account information in the App's Account screen.

### 9.2 Delete Your Data
You can delete your account from the Account settings screen. Upon deletion:
- Your Supabase account and associated data are removed
- Local app data is cleared
- Active subscriptions should be cancelled separately through Stripe or PayPal

### 9.3 Cancel Subscription
- **PayPal:** https://www.paypal.com/myaccount/autopay/
- **Stripe:** Contact mobiledevcli@gmail.com or use the Account screen

---

## 10. Data Retention

- **Active accounts:** Data retained as long as your account exists
- **Cancelled accounts:** Account data removed within 30 days of deletion request
- **Payment records:** Retained as required by tax and financial regulations
- **Anonymized analytics:** May be retained indefinitely (non-personally-identifiable)

---

## 11. Children's Privacy

MobileCLI Pro is not intended for children under 13. We do not knowingly collect personal information from children under 13. If we discover such data has been collected, we will delete it promptly. If you believe a child under 13 has provided us data, contact us at mobiledevcli@gmail.com.

---

## 12. California Privacy Rights (CCPA)

If you are a California resident, you have the following rights under the California Consumer Privacy Act:

### Right to Know
You may request that we disclose what personal information we collect, use, and share. We collect: email, subscription status, and payment transaction IDs. We share limited data with Supabase, Stripe, and PayPal as described in Section 5.

### Right to Delete
You may request deletion of your personal information. Use the Account settings screen or email mobiledevcli@gmail.com. We will respond within 45 days.

### Right to Opt-Out of Sale
**We do not sell your personal information.** No opt-out is necessary.

### Right to Non-Discrimination
We will not discriminate against you for exercising your CCPA rights.

### How to Exercise Your Rights
- **In-App:** Account settings > Delete Account
- **Email:** mobiledevcli@gmail.com
- **Response time:** Within 45 days of verifiable request

---

## 13. GDPR Compliance (EU Users)

### Data Controller
The MobileCLI Team
Contact: mobiledevcli@gmail.com

### Legal Basis for Processing
- **Contract performance:** Account data and payment processing (necessary to provide the service)
- **Legitimate interest:** Service security and fraud prevention
- **Consent:** Optional data processing (you can withdraw consent at any time)

### Your Rights Under GDPR
If you are in the European Economic Area, you have the right to:
- **Access** your personal data
- **Rectify** inaccurate data
- **Erase** your data ("right to be forgotten")
- **Restrict** processing
- **Data portability** (receive your data in a structured format)
- **Object** to processing
- **Withdraw consent** at any time

### How to Exercise Your Rights
Email mobiledevcli@gmail.com with your request. We will respond within 30 days. If you are unsatisfied with our response, you may lodge a complaint with your local data protection authority.

### Deletion Process
When you request deletion:
1. Your Supabase account is deactivated
2. Personal data is removed from our database within 30 days
3. Payment processor records are retained per their own policies and legal requirements
4. Local app data is cleared when you uninstall

---

## 14. International Users

If you access the App from outside the United States, your account data may be transferred to and processed in the United States (via Supabase cloud infrastructure). By using the App, you consent to this transfer. We rely on Supabase's data processing agreements for international data transfer compliance.

---

## 15. Changes to This Policy

We may update this Privacy Policy from time to time. Changes will be reflected in the "Last Updated" date at the top of this document. For significant changes, we will provide notice through the App. Continued use after changes constitutes acceptance.

---

## 16. Contact Us

For privacy questions, data requests, or concerns:
- **Email:** mobiledevcli@gmail.com
- **Website:** https://mobilecli.com
- **GitHub:** https://github.com/MobileDevCLI

---

**By using MobileCLI Pro, you acknowledge that you have read and understood this Privacy Policy.**
