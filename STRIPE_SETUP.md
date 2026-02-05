# Stripe Integration — Setup Status & Next Steps

**Last Updated:** January 30, 2026
**Version:** v3.0.0 (versionCode 138)
**Repo:** MobileCLI-Stripe-Integration

---

## Current Status: TEST MODE (Working)

Stripe payments are fully functional in **test mode**. Real payments require completing Stripe account onboarding and swapping keys (see Steps below).

---

## What Has Been Done

### 1. Security Hardening (v3.0.0)
- **JWT Authentication**: All edge functions authenticate users via `Authorization: Bearer <jwt>` header. Server extracts `user_id` from verified token — clients cannot spoof identity.
- **CORS Hardening**: `Access-Control-Allow-Origin` changed from `*` to `https://www.mobilecli.com`
- **Row Level Security (RLS)**: Migration `004_security_hardening_rls.sql` enables RLS on `payment_history`, `webhook_logs`, `admin_users`, `email_logs`, `user_profiles`
- **Error Sanitization**: Removed `details: err.message` from error responses
- **API Key Header**: Android app sends `apikey` header alongside JWT for Supabase gateway compatibility
- **Gateway JWT Bypass**: All edge functions deployed with `--no-verify-jwt` (functions handle their own auth; Supabase gateway rejects valid user JWTs)

### 2. Edge Functions Deployed
All 5 functions deployed to Supabase project `mwxlguqukyfberyhtkmg`:

| Function | Purpose | Auth |
|----------|---------|------|
| `create-stripe-checkout` | Creates Stripe Checkout Session for $15/month subscription | JWT + apikey |
| `stripe-webhook` | Handles Stripe webhook events (signature verified via HMAC-SHA256) | Stripe signature |
| `create-subscription` | Creates PayPal subscription with proper `custom_id` | JWT + apikey |
| `paypal-webhook` | Handles PayPal webhook events | PayPal verification |
| `create-portal-session` | Creates Stripe Customer Portal session for managing subscriptions | JWT + apikey |

### 3. Android App Integration
- `PaywallActivity.kt`: Stripe checkout button calls `create-stripe-checkout`, opens checkout URL in Chrome Custom Tab
- `PaywallActivity.kt`: PayPal button calls `create-subscription`, opens PayPal approval URL
- `AccountActivity.kt`: "Manage Subscription" button calls `create-portal-session`
- `SupabaseClient.kt`: Exposes `getAccessToken()` and `SUPABASE_ANON_KEY` for API calls
- Post-payment polling: App polls `forceVerifyLicense()` every 3 seconds for up to 30 seconds after returning from checkout

### 4. Webhook Event Handling
The `stripe-webhook` function handles these events:
- `checkout.session.completed` → Activates subscription in `subscriptions` table
- `invoice.paid` → Records payment in `payment_history`, updates `last_payment_at`
- `invoice.payment_failed` → Sets subscription to `past_due`
- `customer.subscription.updated` → Syncs status changes (active, canceled, past_due, etc.)
- `customer.subscription.deleted` → Marks subscription as `cancelled`

Features: Idempotency check (prevents duplicate processing), webhook logging, HMAC-SHA256 signature verification.

### 5. GitHub Actions
`.github/workflows/deploy-functions.yml` auto-deploys all edge functions on push to `master`/`main` when files in `supabase/functions/**` change. Can also be triggered manually via `workflow_dispatch`.

### 6. Commits Made
1. `2bc8734` — feat: v3.0.0 security hardening - JWT auth, CORS, RLS (12 files)
2. `54bacc4` — fix: Add apikey header to edge function calls
3. `add92b7` — fix: Re-add --no-verify-jwt to edge function deploys

### 7. GitHub Release
- **v3.0.0** release created with debug APK attached

---

## Test Mode Verification Results (January 30, 2026)

Tested via Stripe API with `sk_test_` key:

| Check | Result |
|-------|--------|
| API key valid | Yes — Account `acct_1Sn2a22OJZarbVAR` (MobileCLI) |
| Product exists | `prod_TsY1QAeuyoOnad` — "MobileCLI Pro" (active, livemode: false) |
| Price exists | `price_1Sumva2OJZarbVARAwiCm2qB` — $15.00/month recurring (active, livemode: false) |
| Webhook exists | `we_1Sun9R2OJZarbVARx84co96A` — pointed at Supabase function (enabled, livemode: false) |
| Webhook events | checkout.session.completed, invoice.paid, customer.subscription.updated, customer.subscription.deleted |
| Test payment | Successful — test card 4242 4242 4242 4242 accepted, subscription activated |
| Charges enabled | **NO** — account onboarding incomplete |
| Payouts enabled | **NO** — no bank account linked |
| Details submitted | **NO** — identity/business verification not done |

---

## Current Supabase Secrets (Test Mode)

These are the secrets currently set in Supabase edge function environment:

| Secret | Value | Mode |
|--------|-------|------|
| `STRIPE_SECRET_KEY` | `sk_test_51Sn2a22OJZarbVAR...` | TEST |
| `STRIPE_PRICE_ID` | `price_1Sumva2OJZarbVARAwiCm2qB` | TEST |
| `STRIPE_WEBHOOK_SECRET` | `whsec_6sEbInM9aWAzVlkJ4QKk7UAGe30Iz5Fh` | TEST |

---

## What Needs To Be Done To Go Live

### Step 1: Complete Stripe Account Onboarding
**URL:** `https://dashboard.stripe.com/account/onboarding`

Required:
- [ ] Verify identity (government ID)
- [ ] Add business details (individual/sole proprietor is fine)
- [ ] Link bank account (Bank of America — user plans to open a new account for this)
- [ ] Wait for Stripe to activate the account (`charges_enabled: true`)

### Step 2: Create Live Mode Product & Price
Once account is active, switch Stripe dashboard to **live mode** (not sandbox):

1. Go to `https://dashboard.stripe.com/products`
2. Click **"+ Add product"**
3. Name: `MobileCLI Pro`
4. Description: `AI-powered mobile terminal with root-equivalent access. Includes Claude, Gemini, and Codex AI assistants.`
5. Pricing: **$15.00 / month**, recurring
6. Save — copy the new **Price ID** (starts with `price_`, this will be a LIVE price)

### Step 3: Create Live Mode Webhook
1. Go to `https://dashboard.stripe.com/webhooks`
2. Click **"+ Add endpoint"**
3. Endpoint URL: `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/stripe-webhook`
4. Select events:
   - `checkout.session.completed`
   - `invoice.paid`
   - `invoice.payment_failed`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
5. Save — click **"Reveal"** on the signing secret and copy it (starts with `whsec_`)

### Step 4: Get Live Secret Key
1. Go to `https://dashboard.stripe.com/apikeys`
2. Make sure you're in live mode (NOT sandbox)
3. Under **Standard keys**, reveal or create a new **Secret key** (starts with `sk_live_`)
4. Copy the full key

### Step 5: Update Supabase Secrets
Update these 3 secrets in Supabase (dashboard or CLI):

**Via Dashboard:**
1. Go to Supabase Dashboard → Project `mwxlguqukyfberyhtkmg`
2. Go to **Edge Functions** → **Secrets** (or Project Settings → Edge Functions)
3. Update:
   - `STRIPE_SECRET_KEY` → `sk_live_...` (from Step 4)
   - `STRIPE_PRICE_ID` → `price_...` (from Step 2)
   - `STRIPE_WEBHOOK_SECRET` → `whsec_...` (from Step 3)

**Via CLI (alternative):**
```bash
npx supabase secrets set STRIPE_SECRET_KEY=sk_live_YOUR_KEY --project-ref mwxlguqukyfberyhtkmg
npx supabase secrets set STRIPE_PRICE_ID=price_YOUR_LIVE_PRICE --project-ref mwxlguqukyfberyhtkmg
npx supabase secrets set STRIPE_WEBHOOK_SECRET=whsec_YOUR_LIVE_SECRET --project-ref mwxlguqukyfberyhtkmg
```
(Requires `SUPABASE_ACCESS_TOKEN` environment variable or `npx supabase login` first)

### Step 6: Verify Live Mode
After updating secrets, test with a real card (a small charge will occur):
1. Open MobileCLI on your phone
2. Tap Subscribe → Card Payment
3. Use a real card
4. Verify the charge appears in Stripe Dashboard → Payments
5. Verify subscription activates in the app

---

## Key Reference

| Resource | Value |
|----------|-------|
| Supabase Project | `mwxlguqukyfberyhtkmg` |
| Supabase Edge Functions URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/` |
| Stripe Account ID | `acct_1Sn2a22OJZarbVAR` |
| Stripe Dashboard | `https://dashboard.stripe.com` |
| GitHub Repo | MobileCLI-Stripe-Integration |
| Deploy Workflow | `.github/workflows/deploy-functions.yml` |
| Android versionCode | 138 |
| Android versionName | 3.0.0 |

---

## Files Modified in v3.0.0

| File | Changes |
|------|---------|
| `app/build.gradle.kts` | Version bump to 3.0.0 (138) |
| `app/src/main/java/com/termux/auth/SupabaseClient.kt` | Added `getAccessToken()`, made `SUPABASE_ANON_KEY` public |
| `app/src/main/java/com/termux/auth/PaywallActivity.kt` | Added Authorization + apikey headers to Stripe and PayPal calls |
| `app/src/main/java/com/termux/auth/AccountActivity.kt` | Added Authorization + apikey headers to portal session call |
| `.github/workflows/deploy-functions.yml` | Restored `--no-verify-jwt` on all 5 functions |
| `supabase/functions/create-stripe-checkout/index.ts` | JWT auth, CORS, error sanitization |
| `supabase/functions/create-portal-session/index.ts` | JWT auth, CORS, error sanitization |
| `supabase/functions/create-subscription/index.ts` | JWT auth, CORS, error sanitization |
| `supabase/functions/paypal-webhook/index.ts` | CORS hardening |
| `supabase/functions/stripe-webhook/index.ts` | CORS hardening |
| `supabase/migrations/004_security_hardening_rls.sql` | New — RLS policies |
| `README.md` | Added v3.0.0 Security section |

---

## Pending Features (Not Yet Implemented)

### Backup/Restore (Researched, Not Built)
- Backup conversation history (Claude/Gemini/Codex) to `/sdcard/Download/MobileCLI-backup.tar.gz`
- Restore on reinstall if backup file exists
- Data locations identified: `~/.claude/`, `~/.gemini/`, `~/.codex/`, `~/.mobilecli/`, `~/.ssh/`, `~/.gitconfig`, `~/.bashrc`, `~/.config/`, SharedPreferences
- User said: "Don't make any changes yet"

### Free Trial
- User mentioned "I'm not sure if my free trial is working" and "I don't even want there to be a free trial"
- Current implementation: 7-day free trial on sign-up
- Not yet addressed

### Google Play Store
- Developer account created (Account ID: 4824773357272454397, email: Slostuff1@gmail.com)
- $25 registration fee paid
- Identity verification successful
- Still needs: verify contact phone number, finish app setup, create store listing, run closed test with 12 testers for 14 days, then apply for production
