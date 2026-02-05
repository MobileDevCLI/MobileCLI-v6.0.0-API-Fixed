package com.termux.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.termux.BuildConfig
import com.termux.MainActivity
import com.termux.R
import com.termux.SetupWizard
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Apple
import io.github.jan.supabase.gotrue.providers.Azure
import io.github.jan.supabase.gotrue.providers.Discord
import io.github.jan.supabase.gotrue.providers.Github
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.OAuthProvider
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

/**
 * Login/Signup Activity for MobileCLI Pro.
 *
 * Supports:
 * - Google OAuth
 * - GitHub OAuth
 * - Apple OAuth
 * - Microsoft OAuth (Azure)
 * - Discord OAuth
 * - Email + password authentication (hidden, available for future use)
 *
 * After successful login, registers device and gets license.
 */
class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"

        fun start(context: Context) {
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    private lateinit var licenseManager: LicenseManager

    // UI Elements
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    private lateinit var googleButton: Button
    private lateinit var githubButton: Button
    private lateinit var appleButton: Button
    private lateinit var microsoftButton: Button
    private lateinit var discordButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        licenseManager = LicenseManager(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        signupButton = findViewById(R.id.signup_button)
        googleButton = findViewById(R.id.google_button)
        githubButton = findViewById(R.id.github_button)
        appleButton = findViewById(R.id.apple_button)
        microsoftButton = findViewById(R.id.microsoft_button)
        discordButton = findViewById(R.id.discord_button)
        progressBar = findViewById(R.id.progress_bar)
        errorText = findViewById(R.id.error_text)
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            hideKeyboard()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (validateInput(email, password)) {
                login(email, password)
            }
        }

        signupButton.setOnClickListener {
            hideKeyboard()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (validateInput(email, password)) {
                signup(email, password)
            }
        }

        googleButton.setOnClickListener {
            hideKeyboard()
            loginWithOAuth(Google, "Google")
        }

        githubButton.setOnClickListener {
            hideKeyboard()
            loginWithOAuth(Github, "GitHub")
        }

        appleButton.setOnClickListener {
            hideKeyboard()
            loginWithOAuth(Apple, "Apple")
        }

        microsoftButton.setOnClickListener {
            hideKeyboard()
            loginWithOAuth(Azure, "Microsoft")
        }

        discordButton.setOnClickListener {
            hideKeyboard()
            loginWithOAuth(Discord, "Discord")
        }

        // Skip login REMOVED for production release
        // Users must authenticate to use the app
    }

    private fun validateInput(email: String, password: String): Boolean {
        errorText.visibility = View.GONE

        if (email.isEmpty()) {
            showError("Please enter your email")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email")
            return false
        }

        if (password.isEmpty()) {
            showError("Please enter your password")
            return false
        }

        // SECURITY FIX v5.3.0: Increased minimum password length from 6 to 8 characters
        if (password.length < 8) {
            showError("Password must be at least 8 characters")
            return false
        }

        return true
    }

    private fun login(email: String, password: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                SupabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                // SECURITY FIX v5.3.0: Don't log email addresses in release builds
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Login successful for $email")
                } else {
                    Log.i(TAG, "Login successful")
                }
                onLoginSuccess()

            } catch (e: Exception) {
                Log.e(TAG, "Login failed", e)
                setLoading(false)
                showError(getErrorMessage(e))
            }
        }
    }

    private fun signup(email: String, password: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                SupabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // SECURITY FIX v5.3.0: Don't log email addresses in release builds
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Signup successful for $email")
                } else {
                    Log.i(TAG, "Signup successful")
                }

                // Show confirmation message
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Account created! Please check your email to verify.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // After signup, user needs to verify email
                // For now, we'll let them proceed (Supabase handles verification)
                onLoginSuccess()

            } catch (e: Exception) {
                Log.e(TAG, "Signup failed", e)
                setLoading(false)
                showError(getErrorMessage(e))
            }
        }
    }

    private fun loginWithOAuth(provider: OAuthProvider, providerName: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                Log.i(TAG, "Starting $providerName OAuth via Supabase SDK")
                SupabaseClient.auth.signInWith(provider)
                Log.i(TAG, "$providerName OAuth initiated successfully")
                // Callback will come via deep link to onNewIntent()

            } catch (e: Exception) {
                Log.e(TAG, "$providerName login failed: ${e.javaClass.simpleName}: ${e.message}", e)
                runOnUiThread {
                    setLoading(false)
                    val errorMsg = when {
                        e.message?.contains("Credential", ignoreCase = true) == true ||
                        e.message?.contains("GetCredential", ignoreCase = true) == true ->
                            "$providerName Sign-In unavailable on this device."
                        e.message?.contains("canceled", ignoreCase = true) == true ||
                        e.message?.contains("cancelled", ignoreCase = true) == true ->
                            "Sign-in cancelled. Please try again."
                        e.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your connection."
                        else -> "$providerName sign-in failed: ${e.message}"
                    }
                    showError(errorMsg)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle OAuth callback deep link
        intent?.data?.let { uri ->
            Log.i(TAG, "Received OAuth callback: ${uri.scheme}://${uri.host}")
            setLoading(true)
            lifecycleScope.launch {
                try {
                    val success = SupabaseClient.handleDeepLink(uri)
                    if (success && SupabaseClient.isLoggedIn()) {
                        onLoginSuccess()
                    } else {
                        setLoading(false)
                        showError("Login failed. Please try again.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to handle OAuth callback", e)
                    setLoading(false)
                    showError("Login failed. Please try again.")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset loading state if user returns from OAuth without completing
        // Don't auto-redirect here - SplashActivity handles initial auth check
        // This prevents "immediately kicks away" bug from stale sessions
        if (progressBar.visibility == View.VISIBLE) {
            // Give brief delay for deep link to arrive, then reset if no login
            progressBar.postDelayed({
                if (!isFinishing && !isDestroyed && progressBar.visibility == View.VISIBLE) {
                    setLoading(false)
                }
            }, 2000)
        }
    }

    /**
     * Called after successful authentication.
     * Forces a fresh server check for subscription status.
     */
    private suspend fun onLoginSuccess() {
        try {
            Log.i(TAG, "Login successful, forcing fresh subscription check from server...")

            // Force fresh check from server - bypass any cached data
            // This ensures we get the latest subscription status after login
            val result = licenseManager.forceVerifyLicense()

            runOnUiThread {
                setLoading(false)

                if (result.isSuccess) {
                    val license = result.getOrNull() ?: run {
                        Log.e(TAG, "License check returned Success(null)")
                        showError("License verification failed. Please try again.")
                        return@runOnUiThread
                    }
                    Log.i(TAG, "Fresh server check complete, license tier: ${license.tier}, isPro: ${license.isPro()}")

                    // Proceed based on license
                    if (license.isPro()) {
                        // Pro user - go directly to app
                        Log.i(TAG, "User has Pro access, proceeding to app")
                        proceedToApp()
                    } else {
                        // Free/trial user - go to paywall
                        Log.i(TAG, "User does not have Pro access, going to paywall")
                        goToPaywall()
                    }
                } else {
                    // Server check failed - go to paywall to handle
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w(TAG, "Subscription check failed: $error")
                    goToPaywall()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onLoginSuccess error", e)
            runOnUiThread {
                setLoading(false)
                goToPaywall()
            }
        }
    }

    private fun proceedToApp() {
        // Check if setup wizard needs to run
        if (!SetupWizard.isSetupComplete(this)) {
            startActivity(Intent(this, SetupWizard::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun goToPaywall() {
        startActivity(Intent(this, PaywallActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !loading
        signupButton.isEnabled = !loading
        googleButton.isEnabled = !loading
        githubButton.isEnabled = !loading
        appleButton.isEnabled = !loading
        microsoftButton.isEnabled = !loading
        discordButton.isEnabled = !loading
        emailInput.isEnabled = !loading
        passwordInput.isEnabled = !loading
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun getErrorMessage(e: Exception): String {
        val message = e.message ?: "Unknown error"
        return when {
            message.contains("Invalid login credentials") -> "Invalid email or password"
            message.contains("Email not confirmed") -> "Please verify your email first"
            message.contains("User already registered") -> "An account with this email already exists"
            message.contains("Password should be at least") -> "Password must be at least 8 characters"
            message.contains("Unable to validate email") -> "Please enter a valid email address"
            message.contains("network") || message.contains("timeout") -> "Network error. Please check your connection."
            else -> "Error: $message"
        }
    }
}
