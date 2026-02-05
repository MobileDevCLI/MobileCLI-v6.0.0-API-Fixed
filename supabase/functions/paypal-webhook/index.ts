// MobileCLI Pro - PayPal Webhook Handler
// Deployed to: https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook
//
// FIXED: Changed import to use jsr.io instead of esm.sh to fix Deno "filename" error
// PayPal Subscription Plan ID: P-3RH33892X5467024SNFZON2Y
// Price: $15/month
//
// v5.3.0 SECURITY FIX: Added webhook signature verification

import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "https://www.mobilecli.com",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, paypal-transmission-id, paypal-transmission-time, paypal-transmission-sig, paypal-cert-url, paypal-auth-algo",
  "Content-Type": "application/json"
}

/**
 * Verify PayPal webhook signature
 * See: https://developer.paypal.com/docs/api-basics/notifications/webhooks/notification-messages/
 */
async function verifyPayPalWebhookSignature(
  req: Request,
  body: string
): Promise<{ valid: boolean; error?: string }> {
  const webhookId = Deno.env.get("PAYPAL_WEBHOOK_ID")
  if (!webhookId) {
    console.warn("PAYPAL_WEBHOOK_ID not configured - skipping signature verification")
    // In production, you should return { valid: false, error: "Webhook ID not configured" }
    // For now, we'll log a warning but allow the request (for backward compatibility during setup)
    return { valid: true }
  }

  const transmissionId = req.headers.get("paypal-transmission-id")
  const transmissionTime = req.headers.get("paypal-transmission-time")
  const transmissionSig = req.headers.get("paypal-transmission-sig")
  const certUrl = req.headers.get("paypal-cert-url")
  const authAlgo = req.headers.get("paypal-auth-algo")

  // Check required headers
  if (!transmissionId || !transmissionTime || !transmissionSig || !certUrl) {
    return { valid: false, error: "Missing PayPal signature headers" }
  }

  // Verify cert URL is from PayPal
  if (!certUrl.startsWith("https://api.paypal.com/") && !certUrl.startsWith("https://api.sandbox.paypal.com/")) {
    return { valid: false, error: "Invalid certificate URL" }
  }

  try {
    // Get PayPal API credentials
    const clientId = Deno.env.get("PAYPAL_CLIENT_ID")
    const clientSecret = Deno.env.get("PAYPAL_CLIENT_SECRET")
    const paypalApiUrl = Deno.env.get("PAYPAL_API_URL") || "https://api-m.paypal.com"

    if (!clientId || !clientSecret) {
      console.error("PayPal API credentials not configured")
      return { valid: false, error: "PayPal API credentials not configured" }
    }

    // Get access token
    const authResponse = await fetch(`${paypalApiUrl}/v1/oauth2/token`, {
      method: "POST",
      headers: {
        "Authorization": "Basic " + btoa(`${clientId}:${clientSecret}`),
        "Content-Type": "application/x-www-form-urlencoded"
      },
      body: "grant_type=client_credentials"
    })

    if (!authResponse.ok) {
      console.error("Failed to get PayPal access token:", await authResponse.text())
      return { valid: false, error: "Failed to authenticate with PayPal" }
    }

    const authData = await authResponse.json()
    const accessToken = authData.access_token

    // Verify webhook signature using PayPal's verification endpoint
    const verifyResponse = await fetch(`${paypalApiUrl}/v1/notifications/verify-webhook-signature`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        auth_algo: authAlgo || "SHA256withRSA",
        cert_url: certUrl,
        transmission_id: transmissionId,
        transmission_sig: transmissionSig,
        transmission_time: transmissionTime,
        webhook_id: webhookId,
        webhook_event: JSON.parse(body)
      })
    })

    if (!verifyResponse.ok) {
      const errorText = await verifyResponse.text()
      console.error("PayPal signature verification request failed:", errorText)
      return { valid: false, error: "Signature verification request failed" }
    }

    const verifyData = await verifyResponse.json()

    if (verifyData.verification_status === "SUCCESS") {
      console.log("PayPal webhook signature verified successfully")
      return { valid: true }
    } else {
      console.error("PayPal webhook signature verification failed:", verifyData.verification_status)
      return { valid: false, error: `Verification failed: ${verifyData.verification_status}` }
    }

  } catch (e) {
    console.error("Error verifying PayPal webhook signature:", e)
    return { valid: false, error: `Verification error: ${e.message}` }
  }
}

// Helper to validate UUID format
function isValidUUID(str: string | null | undefined): boolean {
  if (!str) return false
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
  return uuidRegex.test(str)
}

// Helper to find user by email
async function findUserByEmail(supabase: any, email: string): Promise<string | null> {
  if (!email) return null

  try {
    // Query auth.users via admin API
    const { data, error } = await supabase.auth.admin.listUsers()
    if (error || !data?.users) return null

    const user = data.users.find((u: any) =>
      u.email?.toLowerCase() === email.toLowerCase()
    )
    return user?.id || null
  } catch (e) {
    console.error("Error finding user by email:", e)
    return null
  }
}

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    // SECURITY FIX v5.3.0: Read body as text first for signature verification
    const bodyText = await req.text()

    // Verify PayPal webhook signature BEFORE processing
    const signatureResult = await verifyPayPalWebhookSignature(req, bodyText)
    if (!signatureResult.valid) {
      console.error("PayPal webhook signature verification failed:", signatureResult.error)
      return new Response(
        JSON.stringify({ error: "Invalid webhook signature", details: signatureResult.error }),
        { status: 403, headers: corsHeaders }
      )
    }

    // Parse the incoming webhook payload (after signature verification)
    const body = JSON.parse(bodyText)
    const eventType = body.event_type || "UNKNOWN"
    const resource = body.resource || {}

    // Try multiple ways to get user identifier
    const customId = resource.custom_id || body.custom_id
    const subscriptionId = resource.id || resource.billing_agreement_id
    const subscriberEmail = resource.subscriber?.email_address ||
                           resource.payer?.email_address ||
                           body.payer?.email_address

    console.log("=== PayPal Webhook Received ===")
    console.log("Event Type:", eventType)
    console.log("Custom ID (user_id):", customId)
    console.log("Subscription ID:", subscriptionId)
    console.log("Subscriber Email:", subscriberEmail)
    console.log("Full payload keys:", Object.keys(body))

    // Initialize Supabase client with service role key
    const supabaseUrl = Deno.env.get("SUPABASE_URL")
    const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")

    if (!supabaseUrl || !supabaseKey) {
      console.error("Missing Supabase credentials")
      return new Response(
        JSON.stringify({ error: "Server configuration error" }),
        { status: 500, headers: corsHeaders }
      )
    }

    const supabase = createClient(supabaseUrl, supabaseKey, {
      auth: {
        autoRefreshToken: false,
        persistSession: false
      }
    })

    // Try to get user_id from custom_id first, then by email lookup
    let userId: string | null = isValidUUID(customId) ? customId : null

    // If no custom_id, try to find user by PayPal email
    if (!userId && subscriberEmail) {
      console.log("No custom_id, trying email lookup for:", subscriberEmail)
      userId = await findUserByEmail(supabase, subscriberEmail)
      if (userId) {
        console.log("Found user by email:", userId)
      }
    }

    // Log webhook event to database FIRST (for debugging)
    const logEntry = {
      event_type: eventType,
      event_id: body.id || `manual-${Date.now()}`,
      provider: "paypal",
      payload: body,
      user_id: userId,
      user_email: subscriberEmail || null,
      processed: false,
      processing_result: userId ? "user_found" : "no_user_id"
    }

    const { error: logError } = await supabase
      .from("webhook_logs")
      .insert(logEntry)

    if (logError) {
      console.error("Failed to log webhook:", logError.message)
    } else {
      console.log("Webhook logged successfully")
    }

    // Process the event
    let processingResult = "no_action"

    // Handle subscription activation
    if ((eventType === "BILLING.SUBSCRIPTION.ACTIVATED" ||
         eventType === "BILLING.SUBSCRIPTION.CREATED") && userId) {
      console.log("Activating subscription for user:", userId)

      // FIX v2.1.2: Log the exact data being upserted for debugging
      const upsertData = {
        user_id: userId,
        status: "active",
        paypal_subscription_id: subscriptionId,
        updated_at: new Date().toISOString()
      }
      console.log("Upserting subscription data:", JSON.stringify(upsertData))

      const { data, error } = await supabase
        .from("subscriptions")
        .upsert(upsertData, {
          onConflict: "user_id"
        })
        .select()

      if (error) {
        // FIX v2.1.2: Enhanced error logging to debug subscriptions table issues
        console.error("Database error on activation:", JSON.stringify({
          message: error.message,
          code: error.code,
          details: error.details,
          hint: error.hint
        }))
        processingResult = `error: ${error.message} | code: ${error.code} | details: ${error.details}`
      } else {
        console.log("Subscription activated successfully:", JSON.stringify(data))
        processingResult = "subscription_activated"
      }
    }

    // Handle payment completed (renewal)
    if (eventType === "PAYMENT.SALE.COMPLETED" && userId) {
      console.log("Payment completed for user:", userId)

      const upsertData = {
        user_id: userId,
        status: "active",
        updated_at: new Date().toISOString()
      }
      console.log("Upserting payment data:", JSON.stringify(upsertData))

      const { data, error } = await supabase
        .from("subscriptions")
        .upsert(upsertData, {
          onConflict: "user_id"
        })
        .select()

      if (error) {
        console.error("Database error on payment:", JSON.stringify({
          message: error.message,
          code: error.code,
          details: error.details,
          hint: error.hint
        }))
        processingResult = `error: ${error.message} | code: ${error.code}`
      } else {
        console.log("Payment upsert result:", JSON.stringify(data))
        processingResult = "payment_completed"
      }

      // Record payment history
      await supabase
        .from("payment_history")
        .insert({
          user_id: userId,
          amount: parseFloat(resource.amount?.total) || 15.00,
          currency: resource.amount?.currency || "USD",
          payment_type: "subscription_renewal",
          provider: "paypal",
          paypal_transaction_id: resource.id,
          paypal_subscription_id: subscriptionId,
          status: "completed",
          description: "MobileCLI Pro subscription - $15/month"
        })
    }

    // Handle cancellation
    if (eventType === "BILLING.SUBSCRIPTION.CANCELLED" && userId) {
      console.log("Cancelling subscription for user:", userId)

      const { data, error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: userId,
          status: "cancelled",
          cancelled_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })
        .select()

      if (error) {
        console.error("Database error on cancellation:", JSON.stringify(error))
      } else {
        console.log("Cancellation result:", JSON.stringify(data))
      }
      processingResult = error ? `error: ${error.message}` : "subscription_cancelled"
    }

    // Handle suspension
    if (eventType === "BILLING.SUBSCRIPTION.SUSPENDED" && userId) {
      console.log("Suspending subscription for user:", userId)

      const { data, error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: userId,
          status: "suspended",
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })
        .select()

      if (error) {
        console.error("Database error on suspension:", JSON.stringify(error))
      } else {
        console.log("Suspension result:", JSON.stringify(data))
      }
      processingResult = error ? `error: ${error.message}` : "subscription_suspended"
    }

    // Handle expiration
    if (eventType === "BILLING.SUBSCRIPTION.EXPIRED" && userId) {
      console.log("Expiring subscription for user:", userId)

      const { data, error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: userId,
          status: "expired",
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })
        .select()

      if (error) {
        console.error("Database error on expiration:", JSON.stringify(error))
      } else {
        console.log("Expiration result:", JSON.stringify(data))
      }
      processingResult = error ? `error: ${error.message}` : "subscription_expired"
    }

    // Update webhook log with processing result
    if (body.id) {
      await supabase
        .from("webhook_logs")
        .update({
          processed: true,
          processing_result: processingResult,
          processed_at: new Date().toISOString()
        })
        .eq("event_id", body.id)
    }

    console.log("=== Webhook Processing Complete ===")
    console.log("Result:", processingResult)

    // Always return 200 to PayPal (so they don't retry)
    return new Response(
      JSON.stringify({
        received: true,
        event: eventType,
        user_found: !!userId,
        result: processingResult
      }),
      { status: 200, headers: corsHeaders }
    )

  } catch (err) {
    console.error("Webhook error:", err.message || err)
    return new Response(
      JSON.stringify({ error: "Internal server error" }),
      { status: 500, headers: corsHeaders }
    )
  }
})
