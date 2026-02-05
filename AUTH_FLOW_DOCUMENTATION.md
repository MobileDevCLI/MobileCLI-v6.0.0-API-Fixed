# MobileCLI Authentication Flow Documentation

**Version:** 5.3.0
**Last Updated:** February 3, 2026

---

## Overview

MobileCLI uses Supabase Auth for user authentication, supporting multiple OAuth providers and email/password authentication.

---

## Authentication Methods

### 1. OAuth Providers (Primary)

Supported providers:
- **Google** - via `io.github.jan.supabase.gotrue.providers.Google`
- **GitHub** - via `io.github.jan.supabase.gotrue.providers.Github`
- **Apple** - via `io.github.jan.supabase.gotrue.providers.Apple`
- **Microsoft (Azure)** - via `io.github.jan.supabase.gotrue.providers.Azure`
- **Discord** - via `io.github.jan.supabase.gotrue.providers.Discord`

### 2. Email/Password

Traditional email and password authentication is available but OAuth is preferred for better security.

---

## Authentication Flow Diagram

```
┌─────────────────┐
│  SplashActivity │
│  (Entry Point)  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│ Check Existing Session      │
│ SupabaseClient.isLoggedIn() │
└────────────┬────────────────┘
             │
     ┌───────┴───────┐
     │               │
     ▼               ▼
 [Logged In]    [Not Logged In]
     │               │
     ▼               ▼
┌─────────────┐ ┌──────────────┐
│ Check       │ │ LoginActivity │
│ Subscription│ │              │
└──────┬──────┘ └──────┬───────┘
       │               │
       ▼               ▼
  [Has Pro?]    [User Authenticates]
   │    │              │
   │    │      ┌───────┴───────┐
   │    │      │               │
   ▼    ▼      ▼               ▼
[Yes] [No]  [OAuth]       [Email/Pass]
  │    │      │               │
  ▼    ▼      ▼               ▼
┌────┐┌────────┐ ┌─────────────────────┐
│Main││Paywall │ │ OAuth Browser Flow  │
│Act.││Activity│ │ with PKCE           │
└────┘└────────┘ └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Deep Link Callback  │
                 │ com.termux://       │
                 │ login-callback      │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Validate State      │◄── NEW in v5.3.0
                 │ (CSRF Protection)   │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Exchange Code       │
                 │ for Session (PKCE)  │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Check Subscription  │
                 │ from Supabase       │
                 └──────────┬──────────┘
                            │
                    ┌───────┴───────┐
                    │               │
                    ▼               ▼
               [Has Pro]      [No Pro]
                    │               │
                    ▼               ▼
              ┌──────────┐   ┌──────────┐
              │ Main     │   │ Paywall  │
              │ Activity │   │ Activity │
              └──────────┘   └──────────┘
```

---

## PKCE Flow Details

MobileCLI uses PKCE (Proof Key for Code Exchange) for OAuth:

1. **Generate Code Verifier**: Random 43-128 character string
2. **Generate Code Challenge**: SHA256 hash of verifier, base64url encoded
3. **Authorization Request**: Includes challenge + method (S256)
4. **Callback**: Receive authorization code
5. **Token Exchange**: Code + verifier exchanged for tokens

### State Parameter (v5.3.0 Security Fix)

```kotlin
// Generate state
fun generateOAuthState(): String {
    val random = SecureRandom()
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return Base64.getUrlEncoder().encodeToString(bytes)
}

// Validate state (constant-time comparison)
fun validateOAuthState(state: String?): Boolean {
    // Prevents CSRF attacks
    // Uses constant-time comparison to prevent timing attacks
}
```

---

## Deep Link Configuration

### Scheme: `com.termux`

| Host | Purpose |
|------|---------|
| `login-callback` | OAuth callback |
| `payment-success` | PayPal return |

### AndroidManifest.xml

```xml
<activity android:name=".auth.LoginActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="com.termux" android:host="login-callback" />
    </intent-filter>
</activity>
```

---

## Session Management

### Session Storage

Sessions are managed by Supabase SDK with encrypted local storage.

### Session Lifecycle

| Event | Action |
|-------|--------|
| Login | Session stored |
| Token Refresh | Automatic by SDK |
| App Background | Session persisted |
| App Kill | Session survives |
| Logout | Session cleared |

### Token Refresh

The Supabase SDK automatically refreshes access tokens before expiry.

---

## Security Considerations

### v5.3.0 Improvements

1. **CSRF Protection**: State parameter validated on OAuth callback
2. **PII Protection**: User emails not logged in release builds
3. **Password Policy**: Minimum 8 characters required
4. **Session Security**: HTTPS-only, certificate pinning

### Best Practices

- Never log access tokens
- Use secure storage for credentials
- Validate all OAuth parameters
- Clear sensitive data on logout

---

## Logout Flow

```kotlin
suspend fun logout() {
    // 1. Clear Supabase session
    SupabaseClient.auth.signOut()

    // 2. Clear license cache
    licenseManager.clearCache()

    // 3. Navigate to login
    LoginActivity.start(context)
}
```

---

## Error Handling

| Error | User Message |
|-------|--------------|
| Invalid credentials | "Invalid email or password" |
| Email not confirmed | "Please verify your email first" |
| User exists | "An account with this email already exists" |
| Network error | "Network error. Please check your connection." |
| OAuth cancelled | "Sign-in cancelled. Please try again." |

---

## Related Documentation

- [PAYMENT_FLOW_DOCUMENTATION.md](PAYMENT_FLOW_DOCUMENTATION.md)
- [SECURITY_BEST_PRACTICES.md](SECURITY_BEST_PRACTICES.md)
- [SECURITY_AUDIT_v5.3.0.md](SECURITY_AUDIT_v5.3.0.md)
