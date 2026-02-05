// MobileCLI Pro - Stripe Webhook Handler
// Handles Stripe webhook events with HMAC-SHA256 signature verification.
// Deployed to: https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/stripe-webhook

import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "https://www.mobilecli.com",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, stripe-signature",
  "Content-Type": "application/json"
}

/**
 * Verify Stripe webhook signature using Web Crypto API.
 * Implements HMAC-SHA256 verification per Stripe docs.
 */
async function verifyStripeSignature(
  rawBody: string,
  signatureHeader: string,
  webhookSecret: string
): Promise<boolean> {
  // Parse the Stripe-Signature header
  const elements = signatureHeader.split(",")
  const sigMap: Record<string, string> = {}
  for (const element of elements) {
    const [key, value] = element.split("=", 2)
    sigMap[key.trim()] = value
  }

  const timestamp = sigMap["t"]
  const expectedSig = sigMap["v1"]

  if (!timestamp || !expectedSig) {
    console.error("Missing t or v1 in Stripe-Signature header")
    return false
  }

  // Check timestamp is within 5 minutes (replay attack prevention)
  const timestampAge = Math.floor(Date.now() / 1000) - parseInt(timestamp)
  if (timestampAge > 300) {
    console.error("Webhook timestamp too old:", timestampAge, "seconds")
    return false
  }

  // Build the signed payload: {timestamp}.{rawBody}
  const signedPayload = `${timestamp}.${rawBody}`

  // Compute HMAC-SHA256
  const encoder = new TextEncoder()
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(webhookSecret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  )

  const signatureBytes = await crypto.subtle.sign(
    "HMAC",
    key,
    encoder.encode(signedPayload)
  )

  // Convert to hex string
  const computedSig = Array.from(new Uint8Array(signatureBytes))
    .map(b => b.toString(16).padStart(2, "0"))
    .join("")

  // Constant-time comparison
  if (computedSig.length !== expectedSig.length) {
    return false
  }
  let mismatch = 0
  for (let i = 0; i < computedSig.length; i++) {
    mismatch |= computedSig.charCodeAt(i) ^ expectedSig.charCodeAt(i)
  }
  return mismatch === 0
}

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  // Read the raw body for signature verification
  const rawBody = await req.text()

  try {
    // Verify webhook signature
    const signatureHeader = req.headers.get("stripe-signature")
    const webhookSecret = Deno.env.get("STRIPE_WEBHOOK_SECRET")

    if (!webhookSecret) {
      console.error("STRIPE_WEBHOOK_SECRET not configured")
      // Still return 200 so Stripe doesn't retry during misconfiguration
      return new Response(
        JSON.stringify({ error: "Webhook secret not configured" }),
        { status: 200, headers: corsHeaders }
      )
    }

    if (!signatureHeader) {
      console.error("Missing Stripe-Signature header")
      return new Response(
        JSON.stringify({ error: "Missing signature" }),
        { status: 400, headers: corsHeaders }
      )
    }

    const isValid = await verifyStripeSignature(rawBody, signatureHeader, webhookSecret)
    if (!isValid) {
      console.error("Invalid webhook signature")
      return new Response(
        JSON.stringify({ error: "Invalid signature" }),
        { status: 400, headers: corsHeaders }
      )
    }

    console.log("Stripe signature verified successfully")

    // Parse the event
    const event = JSON.parse(rawBody)
    const eventType = event.type || "unknown"
    const eventId = event.id || `unknown-${Date.now()}`

    console.log("=== Stripe Webhook Received ===")
    console.log("Event Type:", eventType)
    console.log("Event ID:", eventId)

    // Initialize Supabase client
    const supabaseUrl = Deno.env.get("SUPABASE_URL")
    const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")

    if (!supabaseUrl || !supabaseKey) {
      console.error("Missing Supabase credentials")
      return new Response(
        JSON.stringify({ received: true, error: "Server configuration error" }),
        { status: 200, headers: corsHeaders }
      )
    }

    const supabase = createClient(supabaseUrl, supabaseKey, {
      auth: { autoRefreshToken: false, persistSession: false }
    })

    // Idempotency check - skip if we already processed this event
    const { data: existingLog } = await supabase
      .from("webhook_logs")
      .select("id")
      .eq("event_id", eventId)
      .eq("processed", true)
      .maybeSingle()

    if (existingLog) {
      console.log("Event already processed, skipping:", eventId)
      return new Response(
        JSON.stringify({ received: true, duplicate: true }),
        { status: 200, headers: corsHeaders }
      )
    }

    // Log webhook event
    const logEntry = {
      event_type: eventType,
      event_id: eventId,
      provider: "stripe",
      payload: event,
      processed: false,
      processing_result: "pending"
    }

    await supabase.from("webhook_logs").insert(logEntry)

    // Process the event
    let processingResult = "no_action"
    const eventData = event.data?.object

    switch (eventType) {
      case "checkout.session.completed": {
        // User completed checkout - activate subscription
        const userId = eventData?.client_reference_id || eventData?.metadata?.user_id
        const stripeCustomerId = eventData?.customer
        const stripeSubscriptionId = eventData?.subscription

        if (!userId) {
          console.error("No user_id found in checkout session")
          processingResult = "error: no_user_id"
          break
        }

        console.log("Activating subscription for user:", userId)
        console.log("Stripe Customer:", stripeCustomerId)
        console.log("Stripe Subscription:", stripeSubscriptionId)

        const { error } = await supabase
          .from("subscriptions")
          .upsert({
            user_id: userId,
            status: "active",
            stripe_customer_id: stripeCustomerId,
            stripe_subscription_id: stripeSubscriptionId,
            provider: "stripe",
            updated_at: new Date().toISOString(),
            last_payment_at: new Date().toISOString(),
          }, { onConflict: "user_id" })

        if (error) {
          console.error("Database error on activation:", JSON.stringify(error))
          processingResult = `error: ${error.message}`
        } else {
          console.log("Subscription activated successfully")
          processingResult = "subscription_activated"
        }
        break
      }

      case "invoice.paid": {
        // Successful payment (initial or renewal)
        const stripeCustomerId = eventData?.customer
        const amountPaid = (eventData?.amount_paid || 0) / 100 // Stripe uses cents
        const currency = eventData?.currency?.toUpperCase() || "USD"
        const stripeSubscriptionId = eventData?.subscription

        // Find user by stripe_customer_id
        const { data: sub } = await supabase
          .from("subscriptions")
          .select("user_id")
          .eq("stripe_customer_id", stripeCustomerId)
          .maybeSingle()

        if (!sub?.user_id) {
          console.log("No subscription found for customer:", stripeCustomerId)
          processingResult = "no_matching_user"
          break
        }

        console.log("Invoice paid for user:", sub.user_id, "amount:", amountPaid)

        // Update subscription
        const { error: updateError } = await supabase
          .from("subscriptions")
          .update({
            status: "active",
            last_payment_at: new Date().toISOString(),
            updated_at: new Date().toISOString(),
            payment_failed_at: null, // Clear any previous failure
          })
          .eq("user_id", sub.user_id)

        if (updateError) {
          console.error("Database error on invoice.paid:", JSON.stringify(updateError))
        }

        // Record payment in history
        await supabase.from("payment_history").insert({
          user_id: sub.user_id,
          amount: amountPaid,
          currency: currency,
          payment_type: "subscription_renewal",
          provider: "stripe",
          stripe_invoice_id: eventData?.id,
          stripe_subscription_id: stripeSubscriptionId,
          status: "completed",
          description: `MobileCLI Pro subscription - $${amountPaid}/month`
        })

        processingResult = updateError ? `error: ${updateError.message}` : "invoice_paid"
        break
      }

      case "invoice.payment_failed": {
        // Payment failed
        const stripeCustomerId = eventData?.customer

        const { data: sub } = await supabase
          .from("subscriptions")
          .select("user_id")
          .eq("stripe_customer_id", stripeCustomerId)
          .maybeSingle()

        if (!sub?.user_id) {
          processingResult = "no_matching_user"
          break
        }

        console.log("Payment failed for user:", sub.user_id)

        const { error } = await supabase
          .from("subscriptions")
          .update({
            payment_failed_at: new Date().toISOString(),
            status: "past_due",
            updated_at: new Date().toISOString(),
          })
          .eq("user_id", sub.user_id)

        processingResult = error ? `error: ${error.message}` : "payment_failed_recorded"
        break
      }

      case "customer.subscription.updated": {
        // Subscription status changed (e.g., past_due, active, canceled)
        const stripeCustomerId = eventData?.customer
        const stripeStatus = eventData?.status // active, past_due, canceled, unpaid, etc.

        const { data: sub } = await supabase
          .from("subscriptions")
          .select("user_id")
          .eq("stripe_customer_id", stripeCustomerId)
          .maybeSingle()

        if (!sub?.user_id) {
          processingResult = "no_matching_user"
          break
        }

        // Map Stripe status to our status
        let mappedStatus = stripeStatus
        if (stripeStatus === "canceled") mappedStatus = "cancelled"
        if (stripeStatus === "unpaid") mappedStatus = "expired"
        if (stripeStatus === "trialing") mappedStatus = "trial"

        console.log("Subscription updated for user:", sub.user_id, "status:", stripeStatus, "->", mappedStatus)

        const updateData: Record<string, any> = {
          status: mappedStatus,
          updated_at: new Date().toISOString(),
        }

        if (stripeStatus === "canceled") {
          updateData.cancelled_at = new Date().toISOString()
        }

        // Update expires_at from current_period_end if available
        if (eventData?.current_period_end) {
          updateData.expires_at = new Date(eventData.current_period_end * 1000).toISOString()
        }

        const { error } = await supabase
          .from("subscriptions")
          .update(updateData)
          .eq("user_id", sub.user_id)

        processingResult = error ? `error: ${error.message}` : `subscription_updated_to_${mappedStatus}`
        break
      }

      case "customer.subscription.deleted": {
        // Subscription fully cancelled
        const stripeCustomerId = eventData?.customer

        const { data: sub } = await supabase
          .from("subscriptions")
          .select("user_id")
          .eq("stripe_customer_id", stripeCustomerId)
          .maybeSingle()

        if (!sub?.user_id) {
          processingResult = "no_matching_user"
          break
        }

        console.log("Subscription deleted for user:", sub.user_id)

        const { error } = await supabase
          .from("subscriptions")
          .update({
            status: "cancelled",
            cancelled_at: new Date().toISOString(),
            updated_at: new Date().toISOString(),
          })
          .eq("user_id", sub.user_id)

        processingResult = error ? `error: ${error.message}` : "subscription_cancelled"
        break
      }

      default:
        console.log("Unhandled event type:", eventType)
        processingResult = `unhandled_event: ${eventType}`
    }

    // Update webhook log with processing result
    await supabase
      .from("webhook_logs")
      .update({
        processed: true,
        processing_result: processingResult,
        processed_at: new Date().toISOString()
      })
      .eq("event_id", eventId)

    console.log("=== Webhook Processing Complete ===")
    console.log("Result:", processingResult)

    // Always return 200 so Stripe doesn't retry
    return new Response(
      JSON.stringify({
        received: true,
        event: eventType,
        result: processingResult
      }),
      { status: 200, headers: corsHeaders }
    )

  } catch (err: any) {
    console.error("Webhook error:", err.message || err)
    // Return 200 even on errors to prevent Stripe retries during bugs
    return new Response(
      JSON.stringify({ received: true, error: "Internal server error" }),
      { status: 200, headers: corsHeaders }
    )
  }
})
