# MobileCLI Payment Flow Documentation

**Version:** 5.3.0
**Last Updated:** February 3, 2026

---

## Overview

MobileCLI Pro uses PayPal subscriptions for monetization. The subscription is $15/month with a 7-day free trial.

---

## Payment Provider

**PayPal Subscriptions**
- Plan ID: `P-3RH33892X5467024SNFZON2Y`
- Price: $15.00 USD/month
- Trial: 7 days
- Billing: Automatic recurring

---

## Payment Flow Diagram

```
┌──────────────┐
│ PaywallAct.  │
│ (User clicks │
│  Subscribe)  │
└──────┬───────┘
       │
       ▼
┌────────────────────────────┐
│ Open PayPal in Browser     │
│ with subscription link     │
│ + custom_id = user_id      │
└──────────────┬─────────────┘
               │
               ▼
┌────────────────────────────┐
│ User Completes Payment     │
│ on PayPal                  │
└──────────────┬─────────────┘
               │
       ┌───────┴───────┐
       │               │
       ▼               ▼
[Returns to App]  [PayPal Webhook]
       │               │
       ▼               ▼
┌─────────────┐  ┌─────────────────────┐
│ Deep Link   │  │ Webhook Handler     │
│ payment-    │  │ paypal-webhook      │
│ success     │  │ (Supabase Function) │
└──────┬──────┘  └──────────┬──────────┘
       │                    │
       │         ┌──────────┴──────────┐
       │         │ Verify Signature    │◄── NEW in v5.3.0
       │         │ (CRITICAL)          │
       │         └──────────┬──────────┘
       │                    │
       ▼                    ▼
┌─────────────┐  ┌─────────────────────┐
│ Force Server│  │ Update Database     │
│ Check       │  │ subscriptions table │
└──────┬──────┘  └─────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│ Subscription Verified       │
│ → Proceed to MainActivity   │
└─────────────────────────────┘
```

---

## Webhook Security (v5.3.0)

### Signature Verification

All PayPal webhooks MUST be verified before processing:

```typescript
// Required headers from PayPal
const transmissionId = req.headers.get("paypal-transmission-id")
const transmissionTime = req.headers.get("paypal-transmission-time")
const transmissionSig = req.headers.get("paypal-transmission-sig")
const certUrl = req.headers.get("paypal-cert-url")

// Verify via PayPal API
const verifyResponse = await fetch(
    `${paypalApiUrl}/v1/notifications/verify-webhook-signature`,
    { /* ... */ }
)

if (verifyData.verification_status !== "SUCCESS") {
    return new Response("Invalid signature", { status: 403 })
}
```

### Why This Matters

Without signature verification, anyone could:
- Craft fake webhook payloads
- Activate subscriptions without paying
- Cause financial loss

---

## Webhook Events Handled

| Event | Action |
|-------|--------|
| `BILLING.SUBSCRIPTION.ACTIVATED` | Set status = "active" |
| `BILLING.SUBSCRIPTION.CREATED` | Set status = "active" |
| `PAYMENT.SALE.COMPLETED` | Confirm status = "active", record payment |
| `BILLING.SUBSCRIPTION.CANCELLED` | Set status = "cancelled" |
| `BILLING.SUBSCRIPTION.SUSPENDED` | Set status = "suspended" |
| `BILLING.SUBSCRIPTION.EXPIRED` | Set status = "expired" |

---

## Database Schema

### `subscriptions` Table

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| user_id | UUID | Foreign key to auth.users |
| status | TEXT | active, trial, cancelled, suspended, expired |
| paypal_subscription_id | TEXT | PayPal subscription reference |
| paypal_payer_id | TEXT | PayPal payer reference |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update |
| expires_at | TIMESTAMP | Expiration time |
| cancelled_at | TIMESTAMP | Cancellation time |

### `payment_history` Table

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| user_id | UUID | Foreign key |
| amount | DECIMAL | Payment amount |
| currency | TEXT | USD |
| payment_type | TEXT | subscription_renewal |
| provider | TEXT | paypal |
| status | TEXT | completed |
| paypal_transaction_id | TEXT | Transaction reference |

---

## Subscription Status Flow

```
┌─────────┐
│ (none)  │ User creates account
└────┬────┘
     │
     ▼
┌─────────┐
│  trial  │ 7-day free trial
└────┬────┘
     │
     ├─────────────────┐
     │                 │
     ▼                 ▼
┌─────────┐     ┌───────────┐
│ active  │     │ expired   │
│ ($15/mo)│     │ (no pay)  │
└────┬────┘     └───────────┘
     │
     ├─────────────────┐
     │                 │
     ▼                 ▼
┌───────────┐   ┌───────────┐
│ cancelled │   │ suspended │
│ (user)    │   │ (payment) │
└───────────┘   └───────────┘
```

---

## Client-Side Verification

### License Cache (v5.3.0 Change)

**Before:** 30-day cache interval
**After:** 7-day cache interval

This ensures cancelled subscriptions are detected within 1 week.

```kotlin
// LicenseManager.kt
private const val VERIFICATION_INTERVAL = 7L * 24 * 60 * 60 * 1000 // 7 days
```

### Force Server Check

Used after payment or "Restore Purchase":

```kotlin
suspend fun forceServerCheck(): Result<SubscriptionStatus> {
    clearCache()  // Clear all cached data
    return checkSubscription()  // Fresh server query
}
```

---

## PayPal Configuration

### Environment Variables (Supabase)

| Variable | Description |
|----------|-------------|
| `PAYPAL_CLIENT_ID` | PayPal API client ID |
| `PAYPAL_CLIENT_SECRET` | PayPal API secret |
| `PAYPAL_WEBHOOK_ID` | Webhook ID for verification |
| `PAYPAL_API_URL` | `https://api-m.paypal.com` (prod) |

### Sandbox Testing

Use `https://api-m.sandbox.paypal.com` for testing.

---

## Refund Handling

Refunds are processed manually through PayPal dashboard. When refunded:
1. PayPal sends `BILLING.SUBSCRIPTION.CANCELLED` webhook
2. Subscription status set to "cancelled"
3. User loses access within 7 days (cache expiry)

---

## Security Checklist

- [x] Webhook signature verification
- [x] HTTPS-only communication
- [x] Certificate pinning for PayPal
- [x] No sensitive data in logs
- [x] User ID validated as UUID
- [x] Database RLS policies enforced

---

## Related Documentation

- [AUTH_FLOW_DOCUMENTATION.md](AUTH_FLOW_DOCUMENTATION.md)
- [SECURITY_BEST_PRACTICES.md](SECURITY_BEST_PRACTICES.md)
- [SECURITY_AUDIT_v5.3.0.md](SECURITY_AUDIT_v5.3.0.md)
