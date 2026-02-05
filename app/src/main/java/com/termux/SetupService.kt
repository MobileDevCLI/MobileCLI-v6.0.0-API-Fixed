package com.termux

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Foreground service that owns the setup download lifecycle.
 *
 * Survives activity destruction — downloads continue even when user
 * navigates away from SetupWizard. Shows a persistent notification
 * with progress updates.
 */
class SetupService : Service() {

    companion object {
        private const val TAG = "SetupService"
        private const val CHANNEL_ID = "setup_channel"
        private const val CHANNEL_NAME = "Setup Progress"
        private const val NOTIFICATION_ID = 1338 // TermuxService uses 1337
    }

    private val binder = LocalBinder()
    private val setupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    var onProgress: ((Int, String) -> Unit)? = null
    var isRunning = false
        private set
    var isComplete = false
        private set
    var hasFailed = false
        private set
    var failureMessage: String? = null
        private set

    private lateinit var bootstrapInstaller: BootstrapInstaller

    inner class LocalBinder : Binder() {
        fun getService(): SetupService = this@SetupService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        bootstrapInstaller = BootstrapInstaller(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Preparing..."))
        acquireWakeLock()
        acquireWifiLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun startSetup() {
        if (isRunning) return
        isRunning = true
        hasFailed = false
        failureMessage = null
        isComplete = false

        setupScope.launch {
            try {
                // Set up progress callback for bootstrap download
                bootstrapInstaller.onProgress = { progress, message ->
                    // Scale bootstrap progress to 0-50%
                    val scaledProgress = (progress * 0.5).toInt()
                    reportProgress(scaledProgress, message)
                }

                // Phase 1: Bootstrap (0-50%)
                reportProgress(0, "Downloading terminal environment (~50MB)...")
                val bootstrapSuccess = bootstrapInstaller.install()

                if (!bootstrapSuccess) {
                    reportFailure("Bootstrap download failed. Please check your internet connection and try again.")
                    return@launch
                }

                // Run login shell to trigger bootstrap second stage
                reportProgress(51, "Configuring system packages...")
                runLoginShell()

                // Phase 2: Install packages individually (50-100%)
                // Each package gets its own install call so one failure doesn't block others
                val failedPackages = mutableListOf<String>()

                // Run pkg update separately before any installs
                reportProgress(52, "Updating package index...")
                if (!runBashCommand("pkg update -y")) {
                    Log.w(TAG, "pkg update failed, retrying...")
                    reportProgress(52, "Retrying package index update...")
                    if (!runBashCommand("pkg update -y")) {
                        Log.e(TAG, "pkg update failed after retry, continuing anyway")
                    }
                }

                // --- CRITICAL packages (abort setup if fails after retry) ---
                var currentProgress = 53
                val criticalPackages = listOf("nodejs-lts", "python")
                for (pkg in criticalPackages) {
                    reportProgress(currentProgress, "Installing $pkg...")
                    if (!installPackage(pkg, critical = true)) {
                        failedPackages.add(pkg)
                        reportFailure("Critical package '$pkg' failed to install. Please check your internet connection and try again.")
                        return@launch
                    }
                    currentProgress += 2
                }

                // --- IMPORTANT packages (retry, warn if fails) ---
                val importantPackages = listOf("proot", "proot-distro", "clang", "cmake")
                for (pkg in importantPackages) {
                    reportProgress(currentProgress, "Installing $pkg...")
                    if (!installPackage(pkg, critical = true)) {
                        failedPackages.add(pkg)
                        Log.w(TAG, "Important package failed after retry: $pkg")
                    }
                    currentProgress++
                }

                // --- OPTIONAL packages (try once, log if fails) ---
                reportProgress(62, "Installing optional packages...")
                val optionalPackages = listOf(
                    "rust", "rclone", "scons", "ripgrep", "fzf", "bat", "eza",
                    "fd", "ncdu", "tree", "unzip", "ffmpeg", "imagemagick",
                    "openjdk-17", "gradle", "aapt", "aapt2", "apksigner", "d8"
                )
                val optionalProgressStep = 23.0 / optionalPackages.size
                var optionalProgress = 62.0
                for (pkg in optionalPackages) {
                    reportProgress(optionalProgress.toInt(), "Installing $pkg...")
                    if (!installPackage(pkg, critical = false)) {
                        failedPackages.add(pkg)
                    }
                    optionalProgress += optionalProgressStep
                }

                // Configure native module builds
                reportProgress(85, "Configuring native module builds...")
                runBashCommand("mkdir -p ~/.gyp && printf \"{\\n  'variables': {\\n    'android_ndk_path': ''\\n  }\\n}\\n\" > ~/.gyp/include.gypi")

                // --- NPM CRITICAL (retry) ---
                reportProgress(87, "Installing Claude Code...")
                if (!installNpmPackage("@anthropic-ai/claude-code", critical = true)) {
                    failedPackages.add("npm:@anthropic-ai/claude-code")
                    Log.e(TAG, "Claude Code npm install failed after retry")
                }

                // --- NPM OPTIONAL (try once) ---
                reportProgress(92, "Installing Gemini CLI...")
                if (!installNpmPackage("@google/gemini-cli", critical = false)) {
                    failedPackages.add("npm:@google/gemini-cli")
                }

                reportProgress(96, "Installing Codex CLI...")
                if (!installNpmPackage("@openai/codex", critical = false)) {
                    failedPackages.add("npm:@openai/codex")
                }

                // Log summary of failed packages
                if (failedPackages.isNotEmpty()) {
                    Log.w(TAG, "Setup completed with ${failedPackages.size} failed package(s): ${failedPackages.joinToString()}")
                }

                // Complete
                reportProgress(100, "Setup complete!")
                isComplete = true
                delay(1000)

            } catch (e: Exception) {
                Log.e(TAG, "Setup failed", e)
                reportFailure("Error: ${e.message}")
            } finally {
                isRunning = false
                releaseWakeLock()
                releaseWifiLock()
                if (isComplete || hasFailed) {
                    stopSelf()
                }
            }
        }
    }

    private fun reportProgress(progress: Int, message: String) {
        updateNotification("$message ($progress%)")
        onProgress?.invoke(progress, message)
    }

    private fun reportFailure(message: String) {
        hasFailed = true
        failureMessage = message
        isRunning = false
        updateNotification("Setup failed")
        onProgress?.invoke(0, "Setup failed")
    }

    private suspend fun runBashCommand(command: String): Boolean {
        val maxRetries = 2
        for (attempt in 0..maxRetries) {
            try {
                val bashPath = File(bootstrapInstaller.binDir, "bash").absolutePath
                val process = Runtime.getRuntime().exec(
                    arrayOf(bashPath, "-c", command),
                    bootstrapInstaller.getEnvironment(),
                    bootstrapInstaller.homeDir
                )
                val finished = process.waitFor(10, TimeUnit.MINUTES)
                if (!finished) {
                    Log.w(TAG, "Command timed out (10min), destroying process: $command")
                    process.destroyForcibly()
                    if (attempt < maxRetries) {
                        Log.i(TAG, "Retrying command (attempt ${attempt + 2}/${maxRetries + 1}): $command")
                        delay(5000)
                        continue
                    }
                    return false
                }
                val exitCode = process.exitValue()
                if (exitCode == 0) return true
                Log.w(TAG, "Command failed (exit=$exitCode, attempt ${attempt + 1}/${maxRetries + 1}): $command")
                if (attempt < maxRetries) {
                    delay(5000)
                    continue
                }
                return false
            } catch (e: Exception) {
                Log.e(TAG, "Command exception (attempt ${attempt + 1}/${maxRetries + 1}): $command", e)
                if (attempt < maxRetries) {
                    delay(5000)
                    continue
                }
                return false
            }
        }
        return false
    }

    /**
     * Install a single pkg package with optional retry.
     * @param name Package name for pkg install
     * @param critical If true, retries once on failure
     * @return true if install succeeded
     */
    private suspend fun installPackage(name: String, critical: Boolean = false): Boolean {
        val success = runBashCommand("pkg install -y $name")
        if (!success && critical) {
            Log.w(TAG, "Critical package '$name' failed, retrying...")
            return runBashCommand("pkg install -y $name")
        }
        if (!success) Log.w(TAG, "Optional package failed: $name")
        return success
    }

    /**
     * Install a single npm global package with optional retry.
     * @param name Package name for npm install -g
     * @param critical If true, retries once on failure
     * @return true if install succeeded
     */
    private suspend fun installNpmPackage(name: String, critical: Boolean = false): Boolean {
        val success = runBashCommand("npm install -g $name")
        if (!success && critical) {
            Log.w(TAG, "Critical npm package '$name' failed, retrying...")
            return runBashCommand("npm install -g $name")
        }
        if (!success) Log.w(TAG, "Optional npm package failed: $name")
        return success
    }

    private suspend fun runLoginShell(): Boolean {
        return try {
            val loginPath = File(bootstrapInstaller.binDir, "login").absolutePath
            val process = Runtime.getRuntime().exec(
                arrayOf(loginPath, "-c", "exit"),
                bootstrapInstaller.getEnvironment(),
                bootstrapInstaller.homeDir
            )
            val finished = process.waitFor(5, TimeUnit.MINUTES)
            if (!finished) {
                Log.w(TAG, "Login shell timed out (5min), destroying process")
                process.destroy()
                return true // Non-fatal
            }
            process.exitValue() == 0
        } catch (e: Exception) {
            Log.w(TAG, "Login shell exception (non-fatal)", e)
            true
        }
    }

    // ═══════════════════════════════════════════════════════
    // Notification
    // ═══════════════════════════════════════════════════════

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows setup download progress"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MobileCLI Setup")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    // ═══════════════════════════════════════════════════════
    // Wake Lock / WiFi Lock
    // ═══════════════════════════════════════════════════════

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MobileCLI:SetupServiceWakeLock"
        )
        wakeLock?.acquire(120 * 60 * 1000L) // 2 hours max — setup can run 30+ min on slow connections
    }

    private fun acquireWifiLock() {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wm.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "MobileCLI:SetupServiceWifiLock"
        )
        wifiLock?.acquire()
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun releaseWifiLock() {
        wifiLock?.let {
            if (it.isHeld) it.release()
        }
        wifiLock = null
    }

    override fun onDestroy() {
        setupScope.cancel()
        releaseWakeLock()
        releaseWifiLock()
        super.onDestroy()
    }
}
