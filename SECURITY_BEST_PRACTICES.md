# MobileCLI Security Best Practices

**Version:** 5.3.0
**Last Updated:** February 3, 2026

---

## For Users

### Account Security

1. **Use Strong Passwords**
   - Minimum 8 characters (enforced)
   - Mix uppercase, lowercase, numbers, symbols
   - Don't reuse passwords from other services

2. **Enable OAuth When Possible**
   - Google, GitHub, Apple, Microsoft, Discord
   - Inherits security from trusted providers
   - Supports 2FA if enabled on provider

3. **Keep Your Device Secure**
   - Use device lock (PIN, fingerprint, face)
   - Keep Android OS updated
   - Don't root unless you understand the risks

### Data Privacy

- MobileCLI stores minimal data locally
- Subscription status cached in encrypted storage
- No payment details stored on device
- Logs don't contain PII in release builds

---

## For Developers

### Authentication

1. **Always Use PKCE for OAuth**
   ```kotlin
   flowType = FlowType.PKCE
   ```

2. **Validate State Parameter**
   - Prevents CSRF attacks
   - Use cryptographically secure random
   - One-time use, short expiry

3. **Don't Log Sensitive Data**
   ```kotlin
   if (BuildConfig.DEBUG) {
       Log.d(TAG, "User: $userId")  // Only in debug
   }
   ```

### Network Security

1. **Enforce HTTPS**
   ```xml
   <base-config cleartextTrafficPermitted="false">
   ```

2. **Certificate Pinning**
   - Pin intermediate certificates
   - Set reasonable expiration
   - Have backup pins

3. **Validate SSL Certificates**
   - Don't disable verification in production
   - Use system trust store

### File Operations

1. **Validate All Paths**
   ```kotlin
   fun isValidPath(path: String): Boolean {
       val normalized = File(path).canonicalPath
       return allowedPrefixes.any { normalized.startsWith(it) }
   }
   ```

2. **Don't Trust User Input**
   - Sanitize file names
   - Check for path traversal (../)
   - Restrict to allowed directories

### Webhook Security

1. **Always Verify Signatures**
   - Use provider's verification API
   - Reject invalid signatures immediately
   - Log verification failures

2. **Validate Payload Contents**
   - Check required fields exist
   - Validate data types
   - Don't trust webhook data blindly

### Build Security

1. **Enable Minification**
   ```kotlin
   isMinifyEnabled = true
   isShrinkResources = true
   ```

2. **Protect Signing Keys**
   - Never commit keystores
   - Use environment variables
   - Rotate keys if compromised

3. **Use .gitignore**
   - Exclude credentials
   - Exclude build outputs
   - Exclude IDE files

---

## Security Checklist for Releases

### Pre-Release

- [ ] All debug logging protected with BuildConfig.DEBUG
- [ ] No hardcoded credentials
- [ ] Minification enabled
- [ ] ProGuard rules updated
- [ ] Network security config present
- [ ] Webhook signature verification working
- [ ] Path validation in place

### Post-Release

- [ ] Monitor crash reports for security issues
- [ ] Review Supabase logs for anomalies
- [ ] Check PayPal webhook delivery status
- [ ] Verify certificate pins still valid

---

## Incident Response

### If Credentials Leaked

1. **Immediately revoke** affected credentials
2. **Rotate** all related secrets
3. **Audit** logs for unauthorized access
4. **Notify** affected users if data exposed
5. **Document** the incident

### If Vulnerability Found

1. **Assess** severity and impact
2. **Develop** and test fix
3. **Release** patched version
4. **Disclose** responsibly after patch deployed

---

## Resources

- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [Supabase Security](https://supabase.com/docs/guides/platform/security)
- [PayPal Webhook Security](https://developer.paypal.com/docs/api-basics/notifications/webhooks/)

---

## Reporting Vulnerabilities

See [VULNERABILITY_REPORT.md](VULNERABILITY_REPORT.md) for responsible disclosure process.
