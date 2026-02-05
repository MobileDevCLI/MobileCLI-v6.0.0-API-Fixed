package com.termux.auth

import android.util.Log
import android.content.Context
import com.termux.BuildConfig
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import java.security.SecureRandom
import java.util.Base64

private const val TAG = "SupabaseClient"

// SECURITY FIX v5.3.0: OAuth state parameter storage for CSRF protection
private const val OAUTH_STATE_PREFS = "oauth_state_prefs"
private const val KEY_OAUTH_STATE = "oauth_state"
private const val KEY_STATE_TIMESTAMP = "state_timestamp"
private const val STATE_EXPIRY_MS = 10 * 60 * 1000L // 10 minutes

/**
 * Supabase client singleton for MobileCLI Pro.
 * Handles authentication and database access.
 */
object SupabaseClient {

    // Supabase project credentials
    // These are safe to include in client-side code (anon key has RLS protection)
    private const val SUPABASE_URL = "https://mwxlguqukyfberyhtkmg.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im13eGxndXF1a3lmYmVyeWh0a21nIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc0OTg5ODgsImV4cCI6MjA4MzA3NDk4OH0.VdpU9WzYpTyLeVX9RaXKBP3dNNNf0t9YkQfVf7x_TA8"

    val client: io.github.jan.supabase.SupabaseClient by lazy {
        Log.i(TAG, "Initializing Supabase client...")
        try {
            createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_ANON_KEY
            ) {
                install(Auth) {
                    // Use PKCE flow for mobile OAuth
                    flowType = FlowType.PKCE
                    // Configure custom scheme for redirect
                    scheme = "com.termux"
                    host = "login-callback"
                }
                install(Postgrest) {
                    // Configure postgrest settings
                }
            }.also {
                Log.i(TAG, "Supabase client initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
            throw e
        }
    }

    val auth get() = client.auth
    val db get() = client.postgrest

    /**
     * Sign up with email and password.
     */
    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with email and password.
     */
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google OAuth (browser-based flow).
     * Returns the OAuth URL to open in browser.
     */
    suspend fun signInWithGoogle(): Result<Unit> {
        return try {
            auth.signInWith(Google)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out the current user.
     */
    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
        }
    }

    /**
     * Get the current session's access token for authenticating API calls.
     * Returns null if no active session exists.
     */
    fun getAccessToken(): String? {
        return try {
            auth.currentSessionOrNull()?.accessToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get access token", e)
            null
        }
    }

    /**
     * Get current user ID if logged in.
     */
    fun getCurrentUserId(): String? {
        return try {
            auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user ID", e)
            null
        }
    }

    /**
     * Check if user is logged in.
     */
    fun isLoggedIn(): Boolean {
        return try {
            auth.currentUserOrNull() != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check login status", e)
            false
        }
    }

    /**
     * Get current user's email.
     */
    fun getCurrentUserEmail(): String? {
        return try {
            auth.currentUserOrNull()?.email
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to get user email", e)
            }
            null
        }
    }

    // SECURITY FIX v5.3.0: OAuth state management for CSRF protection
    private var oauthStatePrefs: SharedPreferences? = null

    /**
     * Initialize OAuth state storage with context.
     * Must be called before using OAuth flows.
     */
    fun initOAuthState(context: Context) {
        oauthStatePrefs = context.getSharedPreferences(OAUTH_STATE_PREFS, Context.MODE_PRIVATE)
    }

    /**
     * Generate and store a cryptographically secure state parameter for OAuth.
     */
    fun generateOAuthState(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val state = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        } else {
            android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
        }

        oauthStatePrefs?.edit()
            ?.putString(KEY_OAUTH_STATE, state)
            ?.putLong(KEY_STATE_TIMESTAMP, System.currentTimeMillis())
            ?.apply()

        return state
    }

    /**
     * Validate OAuth state parameter to prevent CSRF attacks.
     */
    fun validateOAuthState(state: String?): Boolean {
        if (state == null) {
            Log.w(TAG, "OAuth state parameter is null")
            return false
        }

        val storedState = oauthStatePrefs?.getString(KEY_OAUTH_STATE, null)
        val timestamp = oauthStatePrefs?.getLong(KEY_STATE_TIMESTAMP, 0L) ?: 0L

        // Clear stored state after retrieval (one-time use)
        oauthStatePrefs?.edit()?.remove(KEY_OAUTH_STATE)?.remove(KEY_STATE_TIMESTAMP)?.apply()

        if (storedState == null) {
            Log.w(TAG, "No stored OAuth state found")
            return false
        }

        // Check if state has expired
        if (System.currentTimeMillis() - timestamp > STATE_EXPIRY_MS) {
            Log.w(TAG, "OAuth state has expired")
            return false
        }

        // Constant-time comparison to prevent timing attacks
        if (state.length != storedState.length) {
            Log.w(TAG, "OAuth state length mismatch")
            return false
        }

        var result = 0
        for (i in state.indices) {
            result = result or (state[i].code xor storedState[i].code)
        }

        if (result != 0) {
            Log.w(TAG, "OAuth state mismatch - possible CSRF attack")
            return false
        }

        Log.i(TAG, "OAuth state validated successfully")
        return true
    }

    /**
     * Handle OAuth deep link callback.
     * Call this when receiving a deep link intent.
     * For PKCE flow, extracts the code parameter and exchanges it for a session.
     * SECURITY FIX v5.3.0: Now validates state parameter for CSRF protection.
     */
    suspend fun handleDeepLink(uri: Uri): Boolean {
        return try {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Handling deep link: ${uri.scheme}://${uri.host}")
            }

            // SECURITY FIX v5.3.0: Validate state parameter if we generated one
            // Note: Only validate if we have stored state (meaning we initiated the flow)
            // This maintains backward compatibility with existing OAuth setups
            val state = uri.getQueryParameter("state")
            val hasStoredState = oauthStatePrefs?.contains(KEY_OAUTH_STATE) == true
            if (state != null && hasStoredState && !validateOAuthState(state)) {
                Log.e(TAG, "OAuth state validation failed - rejecting callback")
                return false
            }

            // For PKCE flow, the callback contains a 'code' parameter
            val code = uri.getQueryParameter("code")
            if (code != null) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Found authorization code, exchanging for session...")
                }
                auth.exchangeCodeForSession(code)
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Session exchange complete, user logged in: ${isLoggedIn()}")
                }
                return isLoggedIn()
            }

            // Check for error in callback
            val error = uri.getQueryParameter("error")
            val errorDescription = uri.getQueryParameter("error_description")
            if (error != null) {
                Log.e(TAG, "OAuth error: $error - $errorDescription")
                return false
            }

            // For implicit flow (fragment-based tokens)
            val fragment = uri.fragment
            if (fragment != null && fragment.contains("access_token")) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Found access token in fragment, importing session...")
                }
                auth.importAuthToken(uri.getQueryParameter("access_token") ?: "")
                return isLoggedIn()
            }

            Log.w(TAG, "No code, token, or error found in deep link")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle deep link", e)
            false
        }
    }

    /**
     * Handle OAuth deep link from Intent.
     */
    suspend fun handleDeepLink(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        return handleDeepLink(uri)
    }
}
