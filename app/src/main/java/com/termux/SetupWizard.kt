package com.termux

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * MobileCLI Setup Wizard - 3-Stage Flow
 *
 * Stage 1: Permissions (all 15 + dev mode guide)
 * Stage 2: Full Environment Download (bootstrap + ALL tools)
 * Stage 3: Choose Your AI (modern cards)
 *
 * ALL AI tools are installed regardless of choice.
 * User just picks which one to LAUNCH first.
 */
class SetupWizard : AppCompatActivity() {

    private var setupService: SetupService? = null
    private var serviceBound = false
    private var isSetupInProgress = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            setupService = (binder as SetupService.LocalBinder).getService()
            serviceBound = true
            setupService?.onProgress = { progress, message ->
                runOnUiThread {
                    updateProgress(progress, message)
                    if (progress >= 100) {
                        isSetupInProgress = false
                        showStage(3)
                    }
                }
            }
            // If service already completed while we were unbound, advance to stage 3
            if (setupService?.isComplete == true) {
                runOnUiThread {
                    isSetupInProgress = false
                    showStage(3)
                }
                return
            }
            // If service failed while we were unbound, show retry
            if (setupService?.hasFailed == true) {
                runOnUiThread {
                    isSetupInProgress = false
                    showRetryDialog("Setup failed", setupService?.failureMessage ?: "Unknown error")
                }
                return
            }
            // Start setup if service just connected and not already running
            if (setupService?.isRunning != true && setupService?.isComplete != true) {
                setupService?.startSetup()
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            setupService = null
            serviceBound = false
        }
    }

    companion object {
        const val PREFS_NAME = "mobilecli_setup"
        const val KEY_SETUP_COMPLETE = "setup_complete"
        const val KEY_SELECTED_AI = "selected_ai"
        const val KEY_LEGAL_ACCEPTED = "legal_accepted"

        const val AI_CLAUDE = "claude"
        const val AI_GEMINI = "gemini"
        const val AI_CODEX = "codex"
        const val AI_NONE = "none"

        private const val PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002

        // All DANGEROUS permissions that require runtime request
        // (Normal permissions like INTERNET, VIBRATE, WAKE_LOCK are granted automatically)
        val REQUIRED_PERMISSIONS = arrayOf(
            // Storage
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            // Camera & Microphone
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            // Location
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            // Contacts & Calendar
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            // SMS
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
            // Phone/Telephony
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.ADD_VOICEMAIL,
            Manifest.permission.USE_SIP,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_PHONE_NUMBERS,
            // Sensors
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION
        )

        // Android 12+ Bluetooth permissions (separate request)
        val BLUETOOTH_PERMISSIONS = arrayOf(
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_ADVERTISE"
        )

        // Android 13+ permissions (separate request)
        val ANDROID_13_PERMISSIONS = arrayOf(
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
            "android.permission.BODY_SENSORS_BACKGROUND"
        )

        fun isSetupComplete(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        }

        fun getSelectedAI(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_SELECTED_AI, AI_CLAUDE) ?: AI_CLAUDE
        }
    }

    // UI Elements
    private lateinit var stageContainer: ViewGroup
    private lateinit var stage0View: View  // Legal agreement
    private lateinit var stage1View: View
    private lateinit var stage2View: View
    private lateinit var stage3View: View

    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null
    private var statusText: TextView? = null

    private var currentStage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if already completed
        if (isSetupComplete(this)) {
            launchMainActivity()
            return
        }

        // Create UI programmatically for flexibility
        createUI()

        // Check if legal already accepted
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_LEGAL_ACCEPTED, false)) {
            showStage(1)  // Skip to permissions
        } else {
            showStage(0)  // Show legal agreement first
        }
    }

    private fun createUI() {
        stageContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        setContentView(stageContainer)

        // Create all stage views
        stage0View = createStage0()  // Legal agreement
        stage1View = createStage1()
        stage2View = createStage2()
        stage3View = createStage3()
    }

    // ═══════════════════════════════════════════════════════════════
    // STAGE 0: Legal Agreement (Terms of Service & Privacy Policy)
    // ═══════════════════════════════════════════════════════════════

    private fun createStage0(): View {
        return android.widget.ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setPadding(32, 48, 32, 48)

                // Title
                addView(TextView(context).apply {
                    text = "MobileCLI"
                    textSize = 32f
                    setTextColor(0xFF6750A4.toInt())
                    gravity = android.view.Gravity.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })

                addView(TextView(context).apply {
                    text = "Terms of Service"
                    textSize = 18f
                    setTextColor(0xFF49454F.toInt())
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 8, 0, 24)
                })

                // Terms summary
                addView(TextView(context).apply {
                    text = """
PLEASE READ CAREFULLY BEFORE USING

By using MobileCLI, you agree to the following:

DISCLAIMER OF WARRANTIES
MobileCLI is provided "AS IS" without warranty of any kind. We make no guarantees about reliability, accuracy, or fitness for any purpose.

LIMITATION OF LIABILITY
The MobileCLI Team shall NOT be liable for ANY damages arising from your use of this app, including but not limited to:
• Data loss or corruption
• Device damage or financial losses
• Any consequences of AI actions
• SMS/calls made through the app
• Any other direct or indirect damages

ASSUMPTION OF RISK
MobileCLI provides powerful access including:
• 79 Android permissions
• AI assistants that execute commands
• Power Mode for autonomous AI
• Access to SMS, calls, contacts, files

YOU ASSUME ALL RISK for actions taken by the app or AI assistants.

INDEMNIFICATION
You agree to hold the MobileCLI Team harmless from any claims arising from your use.

DATA & PRIVACY
Your account email and subscription status are stored on Supabase (our authentication provider). Payment data is processed by Stripe or PayPal. Device data (files, contacts, SMS, etc.) stays on your device and is only accessed when you run specific commands. When you use AI assistants (Claude, Gemini, Codex), your queries are sent to their providers (Anthropic, Google, OpenAI).

Full Privacy Policy: https://mobilecli.com/privacy
Full Terms of Service: https://mobilecli.com/terms

OPEN SOURCE ATTRIBUTION
Terminal rendering uses open source libraries (Apache 2.0). Runtime packages are downloaded from official repositories. See Open Source Licenses for details.
                    """.trimIndent()
                    textSize = 13f
                    setTextColor(0xFF1C1B1F.toInt())
                    setPadding(0, 0, 0, 24)
                })

                // Checkbox for agreement
                val checkBox = android.widget.CheckBox(context).apply {
                    text = "I have read and agree to the Terms of Service and Privacy Policy"
                    textSize = 14f
                    setTextColor(0xFF1C1B1F.toInt())
                    setPadding(0, 0, 0, 24)
                }
                addView(checkBox)

                // Accept button
                val acceptButton = android.widget.Button(context).apply {
                    text = "Accept & Continue"
                    textSize = 16f
                    isEnabled = false
                    setPadding(48, 24, 48, 24)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = android.view.Gravity.CENTER
                    }
                    setOnClickListener {
                        acceptLegalAgreement()
                    }
                }
                addView(acceptButton)

                // Enable button when checkbox is checked
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    acceptButton.isEnabled = isChecked
                }

                // Decline button
                addView(TextView(context).apply {
                    text = "Decline"
                    textSize = 14f
                    setTextColor(0xFF79747E.toInt())
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 24, 0, 0)
                    setOnClickListener {
                        // Exit the app if user declines
                        Toast.makeText(context, "You must accept the terms to use MobileCLI", Toast.LENGTH_LONG).show()
                        finish()
                    }
                })
            })
        }
    }

    private fun acceptLegalAgreement() {
        // Save that user accepted terms
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LEGAL_ACCEPTED, true)
            .apply()

        // Proceed to permissions
        showStage(1)
    }

    // ═══════════════════════════════════════════════════════════════
    // STAGE 1: Permissions
    // ═══════════════════════════════════════════════════════════════

    private fun createStage1(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 48, 48, 48)

            // Title
            addView(TextView(context).apply {
                text = "MobileCLI"
                textSize = 32f
                setTextColor(0xFF6750A4.toInt())
                gravity = android.view.Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })

            addView(TextView(context).apply {
                text = "AI-Powered Terminal"
                textSize = 16f
                setTextColor(0xFF49454F.toInt())
                gravity = android.view.Gravity.CENTER
                setPadding(0, 8, 0, 48)
            })

            // Permissions info
            addView(TextView(context).apply {
                text = "To provide full functionality, MobileCLI needs access to:"
                textSize = 14f
                setTextColor(0xFF1C1B1F.toInt())
                setPadding(0, 0, 0, 16)
            })

            // Permission list
            val permissionItems = listOf(
                "Camera & Microphone - Voice commands, photos",
                "Storage - Save files, export APKs",
                "Location - GPS for location APIs",
                "Contacts & SMS - Termux API access",
                "Network - Download packages, AI communication"
            )

            permissionItems.forEach { item ->
                addView(TextView(context).apply {
                    text = "• $item"
                    textSize = 13f
                    setTextColor(0xFF49454F.toInt())
                    setPadding(16, 4, 0, 4)
                })
            }

            // Continue button
            addView(android.widget.Button(context).apply {
                text = "Grant Permissions"
                textSize = 16f
                setPadding(48, 24, 48, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 48
                    gravity = android.view.Gravity.CENTER
                }
                setOnClickListener { requestPermissions() }
            })
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toMutableList()

        // Android 12+ Bluetooth permissions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS.forEach { perm ->
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(perm)
                }
            }
        }

        // Android 13+ permissions (notifications, granular media, background sensors)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ANDROID_13_PERMISSIONS.forEach { perm ->
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(perm)
                }
            }
        }

        if (permissionsToRequest.isEmpty()) {
            // All permissions already granted
            proceedToStage2()
        } else {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Continue regardless - some permissions may be denied
            proceedToStage2()
        }
    }

    private fun proceedToStage2() {
        // Check for "Display over other apps" permission - required for opening URLs from terminal
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(this)) {
            // Show explanation and open Settings
            android.app.AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("MobileCLI needs the \"Display over other apps\" permission to open browser links from terminal (required for AI authentication).\n\nPlease enable it on the next screen, then return to the app.")
                .setPositiveButton("Open Settings") { _, _ ->
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                }
                .setCancelable(false)
                .show()
        } else {
            showStage(2)
            startFullDownload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            // Continue regardless - user may have granted or denied
            showStage(2)
            startFullDownload()
        }
    }

    /**
     * Handle configuration changes (rotation, etc.) without restarting activity.
     * This prevents the setup wizard from jumping back to permissions during download.
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Just let the layout adjust - don't recreate views or change state
        android.util.Log.i("SetupWizard", "Configuration changed during setup, continuing...")
    }

    // ═══════════════════════════════════════════════════════════════
    // STAGE 2: Full Environment Download
    // ═══════════════════════════════════════════════════════════════

    private fun createStage2(): View {
        return android.widget.ScrollView(this).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setPadding(48, 48, 48, 48)

                // Title
                addView(TextView(context).apply {
                    text = "Setting Up MobileCLI"
                    textSize = 24f
                    setTextColor(0xFF6750A4.toInt())
                    gravity = android.view.Gravity.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, 32)
                })

                // Status text
                addView(TextView(context).apply {
                    text = "Preparing environment..."
                    textSize = 16f
                    setTextColor(0xFF1C1B1F.toInt())
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 24)
                }.also { statusText = it })

                // Progress bar
                addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        24
                    )
                    max = 100
                    progress = 0
                }.also { progressBar = it })

                // Percentage text
                addView(TextView(context).apply {
                    text = "0%"
                    textSize = 18f
                    setTextColor(0xFF6750A4.toInt())
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 16, 0, 32)
                }.also { progressText = it })

                // Info text
                addView(TextView(context).apply {
                    text = "Installing: Terminal environment, AI tools, developer tools\nThis may take a few minutes..."
                    textSize = 12f
                    setTextColor(0xFF79747E.toInt())
                    gravity = android.view.Gravity.CENTER
                })

                // Stay on screen warning
                addView(TextView(context).apply {
                    text = "Please stay on this screen during installation.\nSwitching apps may interrupt the download."
                    textSize = 14f
                    setTextColor(0xFFB3261E.toInt())
                    gravity = android.view.Gravity.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(0, 48, 0, 40)
                })

                // Tools list header
                addView(TextView(context).apply {
                    text = "What's being installed:"
                    textSize = 13f
                    setTextColor(0xFF49454F.toInt())
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 12)
                })

                // Tools list
                addView(TextView(context).apply {
                    text = "Terminal environment (Bash, core utilities)\n" +
                        "Python 3 (scripting, automation)\n" +
                        "Node.js (JavaScript runtime)\n" +
                        "Java 17 & Gradle (Android builds)\n" +
                        "Git & GitHub CLI (version control)\n" +
                        "Claude Code, Gemini CLI, Codex CLI\n" +
                        "Rust, Go, Ruby (additional languages)\n" +
                        "SQLite, jq, curl, ffmpeg (utilities)"
                    textSize = 12f
                    setTextColor(0xFF79747E.toInt())
                    gravity = android.view.Gravity.CENTER
                    lineHeight = 44
                })
            })
        }
    }

    private fun updateProgress(progress: Int, status: String) {
        runOnUiThread {
            progressBar?.progress = progress
            progressText?.text = "$progress%"
            statusText?.text = status
        }
    }

    /**
     * Show a retry dialog when setup fails.
     * Gives user the option to retry or exit.
     */
    private fun showRetryDialog(title: String, message: String) {
        updateProgress(0, "Setup failed")
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ ->
                // Stop old service, unbind, then restart
                if (serviceBound) {
                    setupService?.onProgress = null
                    unbindService(serviceConnection)
                    serviceBound = false
                }
                stopService(Intent(this, SetupService::class.java))
                startFullDownload()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun startFullDownload() {
        // Keep screen on during setup
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        isSetupInProgress = true

        // Start and bind to SetupService — it owns the download lifecycle
        // Progress callback is set in onServiceConnected
        val intent = Intent(this, SetupService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // ═══════════════════════════════════════════════════════════════
    // STAGE 3: Choose Your AI (Modern Cards)
    // ═══════════════════════════════════════════════════════════════

    private fun createStage3(): View {
        return android.widget.ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 48, 24, 48)
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                )

                // Title
                addView(TextView(context).apply {
                    text = "Choose Your AI"
                    textSize = 28f
                    setTextColor(0xFF1C1B1F.toInt())
                    gravity = android.view.Gravity.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })

                addView(TextView(context).apply {
                    text = "All AI tools are installed. Pick which to launch."
                    textSize = 14f
                    setTextColor(0xFF49454F.toInt())
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 8, 0, 32)
                })

                // Claude Card
                addView(createAICard(
                    "Claude",
                    "by Anthropic",
                    "The most capable AI for coding. Builds apps, debugs issues, writes production code.",
                    0xFF6750A4.toInt(),
                    true // recommended
                ) { selectAI(AI_CLAUDE) })

                // Gemini Card
                addView(createAICard(
                    "Gemini",
                    "by Google",
                    "Multimodal AI for research, documentation, and development.",
                    0xFF4285F4.toInt(),
                    false
                ) { selectAI(AI_GEMINI) })

                // Codex Card
                addView(createAICard(
                    "Codex",
                    "by OpenAI",
                    "Code-focused model for completion and generation.",
                    0xFF10A37F.toInt(),
                    false
                ) { selectAI(AI_CODEX) })

                // Basic Terminal Card
                addView(createAICard(
                    "Basic Terminal",
                    "Skip AI launch",
                    "Full Linux terminal. You can call any AI tool anytime.",
                    0xFF79747E.toInt(),
                    false
                ) { selectAI(AI_NONE) })
            })
        }
    }

    private fun createAICard(
        title: String,
        subtitle: String,
        description: String,
        color: Int,
        recommended: Boolean,
        onClick: () -> Unit
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(0xFFF5F5F5.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }

            // Title row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL

                addView(TextView(context).apply {
                    text = title
                    textSize = 22f
                    setTextColor(color)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                if (recommended) {
                    addView(TextView(context).apply {
                        text = "RECOMMENDED"
                        textSize = 10f
                        setTextColor(0xFFFFFFFF.toInt())
                        setBackgroundColor(color)
                        setPadding(12, 6, 12, 6)
                    })
                }
            })

            // Subtitle
            addView(TextView(context).apply {
                text = subtitle
                textSize = 12f
                setTextColor(0xFF49454F.toInt())
                setPadding(0, 0, 0, 8)
            })

            // Description
            addView(TextView(context).apply {
                text = description
                textSize = 14f
                setTextColor(0xFF49454F.toInt())
            })
        }
    }

    private fun selectAI(ai: String) {
        // Save selection
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SETUP_COMPLETE, true)
            .putString(KEY_SELECTED_AI, ai)
            .apply()

        // Launch main activity with selected AI
        launchMainActivity(ai)
    }

    // ═══════════════════════════════════════════════════════════════
    // Navigation
    // ═══════════════════════════════════════════════════════════════

    private fun showStage(stage: Int) {
        currentStage = stage
        stageContainer.removeAllViews()

        val view = when (stage) {
            0 -> stage0View  // Legal agreement
            1 -> stage1View
            2 -> stage2View
            3 -> stage3View
            else -> stage0View
        }

        stageContainer.addView(view)
    }

    private fun launchMainActivity(selectedAI: String = AI_NONE) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("selected_ai", selectedAI)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        // Unbind from service but do NOT stop it — download continues in background
        if (serviceBound) {
            setupService?.onProgress = null
            try {
                unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                android.util.Log.w("SetupWizard", "Service already unbound", e)
            }
            serviceBound = false
        }
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        android.util.Log.i("SetupWizard", "onDestroy - unbound from SetupService (download continues)")
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (isSetupInProgress) {
            Toast.makeText(this, "Setup continues in background via notification", Toast.LENGTH_SHORT).show()
            // Let user leave — service keeps running
            finish()
        } else if (currentStage > 1) {
            Toast.makeText(this, "Please complete setup", Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }
}
