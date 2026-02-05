// MobileCLI Pro - Server-Side PayPal Subscription Creation
// This function creates a PayPal subscription with custom_id properly embedded
// so the webhook can always match the user, regardless of email differences.
//
// FIX v2.1.2: Added duplicate subscription prevention
// - Checks for existing active subscription before creating new one
// - Fixed idempotency key to not include timestamp

import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "https://www.mobilecli.com",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Content-Type": "application/json"
}

// PayPal API endpoints
const PAYPAL_API_BASE = "https://api-m.paypal.com" // Production
// const PAYPAL_API_BASE = "https://api-m.sandbox.paypal.com" // Sandbox

// MobileCLI Pro subscription plan
const PLAN_ID = "P-3RH33892X5467024SNFZON2Y"

interface CreateSubscriptionRequest {
  return_url?: string
  cancel_url?: string
}

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
 * Check if user already has an active subscription
 * Returns existing subscription info if found
 */
async function checkExistingSubscription(userId: string): Promise<{
  exists: boolean
  subscriptionId?: string
  status?: string
}> {
  const supabaseUrl = Deno.env.get("SUPABASE_URL")
  const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")

  if (!supabaseUrl || !supabaseKey) {
    console.log("Warning: Supabase credentials not configured, skipping duplicate check")
    return { exists: false }
  }

  const supabase = createClient(supabaseUrl, supabaseKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false
    }
  })

  try {
    const { data, error } = await supabase
      .from("subscriptions")
      .select("paypal_subscription_id, status")
      .eq("user_id", userId)
      .in("status", ["active", "trialing"])
      .maybeSingle()

    if (error) {
      console.error("Error checking existing subscription:", error.message)
      return { exists: false }
    }

    if (data?.paypal_subscription_id) {
      console.log("Found existing subscription:", data.paypal_subscription_id, "status:", data.status)
      return {
        exists: true,
        subscriptionId: data.paypal_subscription_id,
        status: data.status
      }
    }

    return { exists: false }
  } catch (e) {
    console.error("Exception checking existing subscription:", e)
    return { exists: false }
  }
}

/**
 * Get PayPal access token using client credentials
 */
async function getPayPalAccessToken(): Promise<string> {
  const clientId = Deno.env.get("PAYPAL_CLIENT_ID")
  const clientSecret = Deno.env.get("PAYPAL_CLIENT_SECRET")

  if (!clientId || !clientSecret) {
    throw new Error("PayPal credentials not configured")
  }

  const auth = btoa(`${clientId}:${clientSecret}`)

  const response = await fetch(`${PAYPAL_API_BASE}/v1/oauth2/token`, {
    method: "POST",
    headers: {
      "Authorization": `Basic ${auth}`,
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: "grant_type=client_credentials"
  })

  if (!response.ok) {
    const error = await response.text()
    console.error("PayPal auth failed:", error)
    throw new Error(`PayPal authentication failed: ${response.status}`)
  }

  const data = await response.json()
  return data.access_token
}

/**
 * Create a PayPal subscription with custom_id embedded
 */
async function createPayPalSubscription(
  accessToken: string,
  userId: string,
  returnUrl: string,
  cancelUrl: string
): Promise<{ approvalUrl: string; subscriptionId: string }> {

  const subscriptionData = {
    plan_id: PLAN_ID,
    custom_id: userId, // THIS IS THE KEY - embeds user_id in subscription
    application_context: {
      brand_name: "MobileCLI Pro",
      locale: "en-US",
      shipping_preference: "NO_SHIPPING",
      user_action: "SUBSCRIBE_NOW",
      return_url: returnUrl,
      cancel_url: cancelUrl
    }
  }

  console.log("Creating PayPal subscription with custom_id:", userId)

  const response = await fetch(`${PAYPAL_API_BASE}/v1/billing/subscriptions`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      "PayPal-Request-Id": `mobilecli-sub-${userId}` // Idempotency key - same user = same key (no timestamp)
    },
    body: JSON.stringify(subscriptionData)
  })

  if (!response.ok) {
    const error = await response.text()
    console.error("PayPal subscription creation failed:", error)
    throw new Error(`Failed to create subscription: ${response.status} - ${error}`)
  }

  const subscription = await response.json()
  console.log("PayPal subscription created:", subscription.id)

  // Find the approval URL in the links array
  const approvalLink = subscription.links?.find(
    (link: any) => link.rel === "approve"
  )

  if (!approvalLink) {
    throw new Error("No approval URL in PayPal response")
  }

  return {
    approvalUrl: approvalLink.href,
    subscriptionId: subscription.id
  }
}

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    // Only allow POST
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

    // Parse request body for optional fields
    const body: CreateSubscriptionRequest = await req.json()
    const { return_url, cancel_url } = body

    // Default URLs if not provided
    const finalReturnUrl = return_url || "https://www.mobilecli.com/success.html"
    const finalCancelUrl = cancel_url || "https://www.mobilecli.com/pricing.html"

    console.log("=== Creating Subscription ===")
    console.log("User ID:", user_id)
    console.log("Return URL:", finalReturnUrl)

    // FIX: Check for existing active subscription BEFORE creating new one
    const existing = await checkExistingSubscription(user_id)
    if (existing.exists) {
      console.log("=== DUPLICATE PREVENTED ===")
      console.log("User already has active subscription:", existing.subscriptionId)
      return new Response(
        JSON.stringify({
          error: "duplicate_subscription",
          message: "You already have an active subscription",
          existing_subscription_id: existing.subscriptionId,
          status: existing.status
        }),
        { status: 409, headers: corsHeaders }  // 409 Conflict
      )
    }

    // Get PayPal access token
    const accessToken = await getPayPalAccessToken()

    // Create the subscription
    const { approvalUrl, subscriptionId } = await createPayPalSubscription(
      accessToken,
      user_id,
      finalReturnUrl,
      finalCancelUrl
    )

    console.log("=== Subscription Created ===")
    console.log("Subscription ID:", subscriptionId)
    console.log("Approval URL:", approvalUrl)

    // Return the approval URL for the client to redirect to
    return new Response(
      JSON.stringify({
        success: true,
        approval_url: approvalUrl,
        subscription_id: subscriptionId,
        message: "Redirect user to approval_url to complete payment"
      }),
      { status: 200, headers: corsHeaders }
    )

  } catch (err: any) {
    console.error("Error creating subscription:", err.message || err)
    return new Response(
      JSON.stringify({
        error: "Failed to create subscription"
      }),
      { status: 500, headers: corsHeaders }
    )
  }
})

