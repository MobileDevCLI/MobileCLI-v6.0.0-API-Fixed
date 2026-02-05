// MobileCLI Pro - Stripe Checkout Session Creation
// Creates a Stripe Checkout Session (mode: subscription) for $15/month Pro plan.
// Uses Stripe REST API directly (no SDK needed in Deno).

import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "https://www.mobilecli.com",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Content-Type": "application/json"
}

const STRIPE_API_BASE = "https://api.stripe.com"

/**
 * Authenticate user from JWT token in Authorization header.
 * Returns the verified user_id - cannot be faked.
 */
async function authenticateUser(req: Request): Promise<string> {
  const authHeader = req.headers.get("authorization")
  if (!authHeader?.startsWith("Bearer ")) {
    throw new Error("UNAUTHORIZED")
  }
  const token = authHeader.replace("Bearer ", "")
  const supabaseUrl = Deno.env.get("SUPABASE_URL")!
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!
  const supabase = createClient(supabaseUrl, supabaseAnonKey)
  const { data: { user }, error } = await supabase.auth.getUser(token)
  if (error || !user) throw new Error("UNAUTHORIZED")
  return user.id
}

/**
 * Check if user already has an active subscription (any provider)
 */
async function checkExistingSubscription(userId: string): Promise<{
  exists: boolean
  provider?: string
  status?: string
}> {
  const supabaseUrl = Deno.env.get("SUPABASE_URL")
  const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")

  if (!supabaseUrl || !supabaseKey) {
    console.log("Warning: Supabase credentials not configured, skipping duplicate check")
    return { exists: false }
  }

  const supabase = createClient(supabaseUrl, supabaseKey, {
    auth: { autoRefreshToken: false, persistSession: false }
  })

  try {
    const { data, error } = await supabase
      .from("subscriptions")
      .select("status, provider")
      .eq("user_id", userId)
      .in("status", ["active", "trialing"])
      .maybeSingle()

    if (error) {
      console.error("Error checking existing subscription:", error.message)
      return { exists: false }
    }

    if (data) {
      console.log("Found existing subscription:", data.status, "provider:", data.provider)
      return { exists: true, provider: data.provider, status: data.status }
    }

    return { exists: false }
  } catch (e) {
    console.error("Exception checking existing subscription:", e)
    return { exists: false }
  }
}

/**
 * Get or create a Stripe Customer for the given user.
 * Reuses existing stripe_customer_id from subscriptions table if available.
 */
async function getOrCreateStripeCustomer(
  userId: string,
  stripeSecretKey: string
): Promise<string> {
  // Check if user already has a Stripe customer ID in the database
  const supabaseUrl = Deno.env.get("SUPABASE_URL")
  const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")

  if (supabaseUrl && supabaseKey) {
    const supabase = createClient(supabaseUrl, supabaseKey, {
      auth: { autoRefreshToken: false, persistSession: false }
    })

    const { data } = await supabase
      .from("subscriptions")
      .select("stripe_customer_id")
      .eq("user_id", userId)
      .not("stripe_customer_id", "is", null)
      .limit(1)
      .maybeSingle()

    if (data?.stripe_customer_id) {
      console.log("Reusing existing Stripe customer:", data.stripe_customer_id)
      return data.stripe_customer_id
    }
  }

  // Create a new Stripe Customer
  const params = new URLSearchParams()
  params.append("metadata[user_id]", userId)

  const response = await fetch(`${STRIPE_API_BASE}/v1/customers`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${stripeSecretKey}`,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: params.toString(),
  })

  if (!response.ok) {
    const errorBody = await response.text()
    console.error("Stripe Customer creation error:", response.status, errorBody)
    throw new Error(`Failed to create Stripe customer: ${response.status} - ${errorBody}`)
  }

  const customer = await response.json()
  console.log("Created new Stripe customer:", customer.id)
  return customer.id
}

/**
 * Create a Stripe Checkout Session for subscription
 */
async function createStripeCheckoutSession(
  userId: string
): Promise<{ checkoutUrl: string; sessionId: string }> {
  const stripeSecretKey = Deno.env.get("STRIPE_SECRET_KEY")
  if (!stripeSecretKey) {
    throw new Error("STRIPE_SECRET_KEY not configured")
  }

  const priceId = Deno.env.get("STRIPE_PRICE_ID")
  if (!priceId) {
    throw new Error("STRIPE_PRICE_ID not configured")
  }

  // Get or create a Stripe Customer (required for Stripe Accounts V2)
  const customerId = await getOrCreateStripeCustomer(userId, stripeSecretKey)

  // Build form-encoded body (Stripe API uses form encoding, not JSON)
  const params = new URLSearchParams()
  params.append("mode", "subscription")
  params.append("customer", customerId)
  params.append("line_items[0][price]", priceId)
  params.append("line_items[0][quantity]", "1")
  params.append("client_reference_id", userId)
  params.append("metadata[user_id]", userId)
  params.append("success_url", "https://www.mobilecli.com/success?session_id={CHECKOUT_SESSION_ID}")
  params.append("cancel_url", "https://www.mobilecli.com/pricing.html")
  // Note: automatic_tax requires head office address in Stripe Dashboard.
  // Enable once tax settings are configured: params.append("automatic_tax[enabled]", "true")

  console.log("Creating Stripe Checkout Session for user:", userId, "customer:", customerId)

  const response = await fetch(`${STRIPE_API_BASE}/v1/checkout/sessions`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${stripeSecretKey}`,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: params.toString(),
  })

  if (!response.ok) {
    const errorBody = await response.text()
    console.error("Stripe API error:", response.status, errorBody)
    throw new Error(`Stripe API error: ${response.status} - ${errorBody}`)
  }

  const session = await response.json()
  console.log("Stripe Checkout Session created:", session.id)

  if (!session.url) {
    throw new Error("No checkout URL in Stripe response")
  }

  return {
    checkoutUrl: session.url,
    sessionId: session.id,
  }
}

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    if (req.method !== "POST") {
      return new Response(
        JSON.stringify({ error: "Method not allowed" }),
        { status: 405, headers: corsHeaders }
      )
    }

    // Authenticate user from JWT - user_id comes from verified token
    let user_id: string
    try {
      user_id = await authenticateUser(req)
    } catch {
      return new Response(
        JSON.stringify({ error: "Unauthorized" }),
        { status: 401, headers: corsHeaders }
      )
    }

    console.log("=== Creating Stripe Checkout ===")
    console.log("User ID:", user_id)

    // Check for existing active subscription
    const existing = await checkExistingSubscription(user_id)
    if (existing.exists) {
      console.log("=== DUPLICATE PREVENTED ===")
      return new Response(
        JSON.stringify({
          error: "duplicate_subscription",
          message: "You already have an active subscription",
          provider: existing.provider,
          status: existing.status
        }),
        { status: 409, headers: corsHeaders }
      )
    }

    // Create Stripe Checkout Session
    const { checkoutUrl, sessionId } = await createStripeCheckoutSession(user_id)

    console.log("=== Checkout Session Created ===")
    console.log("Session ID:", sessionId)
    console.log("Checkout URL:", checkoutUrl)

    return new Response(
      JSON.stringify({
        success: true,
        checkout_url: checkoutUrl,
        session_id: sessionId,
        message: "Redirect user to checkout_url to complete payment"
      }),
      { status: 200, headers: corsHeaders }
    )

  } catch (err: any) {
    console.error("Error creating checkout session:", err.message || err)
    return new Response(
      JSON.stringify({
        error: "Failed to create checkout session"
      }),
      { status: 500, headers: corsHeaders }
    )
  }
})
