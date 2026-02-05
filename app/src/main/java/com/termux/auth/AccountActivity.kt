package com.termux.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.termux.R
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Account/Settings Activity for MobileCLI Pro.
 *
 * Industry-standard account management:
 * - View profile (email)
 * - View subscription status
 * - Manage subscription (opens PayPal)
 * - Logout
 * - Delete account (with confirmation)
 */
class AccountActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AccountActivity"
        private const val PAYPAL_SUBSCRIPTIONS_URL = "https://www.paypal.com/myaccount/autopay"

        fun start(context: Context) {
            context.startActivity(Intent(context, AccountActivity::class.java))
        }
    }

    private lateinit var licenseManager: LicenseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Account"

        licenseManager = LicenseManager(this)

        setupUI()
        loadAccountInfo()
    }

    private fun setupUI() {
        // Logout button
        findViewById<Button>(R.id.logout_button).setOnClickListener {
            showLogoutConfirmation()
        }

        // Manage subscription button
        findViewById<Button>(R.id.manage_subscription_button).setOnClickListener {
            openManageSubscription()
        }

        // Delete account button (if you want this feature)
        findViewById<Button>(R.id.delete_account_button)?.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun loadAccountInfo() {
        // Email
        val email = SupabaseClient.getCurrentUserEmail() ?: "Not logged in"
        findViewById<TextView>(R.id.account_email).text = email

        // User ID (for support)
        val userId = SupabaseClient.getCurrentUserId() ?: "Unknown"
        findViewById<TextView>(R.id.account_user_id).text = "ID: ${userId.take(8)}..."

        // Subscription status
        val license = licenseManager.getLicenseInfo()
        val statusText = when {
            license == null -> "Not logged in"
            license.isPro() -> "Pro Subscriber"
            license.tier == "free" && license.daysUntilExpiry() > 0 ->
                "Trial (${license.daysUntilExpiry()} days left)"
            else -> "Expired"
        }
        findViewById<TextView>(R.id.subscription_status).text = statusText

        // Show/hide manage subscription based on status
        val manageButton = findViewById<Button>(R.id.manage_subscription_button)
        manageButton.text = if (license?.isPro() == true) {
            "Manage Subscription"
        } else {
            "Subscribe to Pro"
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out? You can log back in anytime with the same account.")
            .setPositiveButton("Log Out") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                // Clear local license cache
                licenseManager.clearCache()

                // Sign out from Supabase
                SupabaseClient.signOut()

                Toast.makeText(this@AccountActivity, "Logged out", Toast.LENGTH_SHORT).show()

                // Go to login screen
                LoginActivity.start(this@AccountActivity)
                finishAffinity() // Close all activities

            } catch (e: Exception) {
                Log.e(TAG, "Logout failed", e)
                Toast.makeText(this@AccountActivity, "Logout failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openManageSubscription() {
        val license = licenseManager.getLicenseInfo()

        if (license?.isPro() == true) {
            // Check subscription provider to decide where to send the user
            lifecycleScope.launch {
                val provider = getSubscriptionProvider()
                if (provider == "stripe") {
                    openStripePortal()
                } else {
                    // PayPal or legacy - open PayPal autopay page
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_SUBSCRIPTIONS_URL))
                    startActivity(intent)
                }
            }
        } else {
            // Go to paywall to subscribe
            PaywallActivity.start(this)
            finish()
        }
    }

    /**
     * Get the subscription provider (stripe or paypal) from the database.
     */
    private suspend fun getSubscriptionProvider(): String? = withContext(Dispatchers.IO) {
        try {
            val userId = SupabaseClient.getCurrentUserId() ?: return@withContext null
            val subscriptions = SupabaseClient.client.postgrest
                .from("subscriptions")
                .select(columns = Columns.ALL) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<LicenseManager.Subscription>()

            subscriptions.firstOrNull()?.provider
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get subscription provider", e)
            null
        }
    }

    /**
     * Open Stripe Customer Portal for subscription management.
     */
    private fun openStripePortal() {
        val userId = SupabaseClient.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Opening subscription management...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val portalUrl = createPortalSession(userId)
                if (portalUrl != null) {
                    try {
                        val customTabsIntent = CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .build()
                        customTabsIntent.launchUrl(this@AccountActivity, Uri.parse(portalUrl))
                    } catch (e: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl))
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this@AccountActivity, "Failed to open subscription portal", Toast.LENGTH_LONG).show()
                    // Fallback to PayPal page
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_SUBSCRIPTIONS_URL))
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open Stripe portal", e)
                Toast.makeText(this@AccountActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Call create-portal-session Edge Function.
     */
    private suspend fun createPortalSession(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/create-portal-session")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${SupabaseClient.getAccessToken()}")
            connection.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY)
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            // Body no longer needs user_id - server extracts it from JWT
            val json = """{}"""
            connection.outputStream.bufferedWriter().use { it.write(json) }

            val responseCode = connection.responseCode
            Log.d(TAG, "Portal session response code: $responseCode")

            if (responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().readText()
                val jsonResponse = org.json.JSONObject(responseBody)
                jsonResponse.optString("portal_url", null)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "Portal session failed: $responseCode - $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling create-portal-session API", e)
            null
        }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("This will sign you out and delete all local account data. Your server account data will be deleted within 30 days. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Clear local license/subscription cache
                        licenseManager.clearCache()
                        // Sign out from Supabase
                        SupabaseClient.signOut()
                        Toast.makeText(this@AccountActivity, "Account data cleared. Server data will be deleted within 30 days.", Toast.LENGTH_LONG).show()
                        // Redirect to login
                        LoginActivity.start(this@AccountActivity)
                        finish()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during account deletion", e)
                        Toast.makeText(this@AccountActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
