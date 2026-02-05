# MobileCLI v6.0.0 - API Fix Documentation

## The Problem

Android 10+ introduced restrictions on background broadcast execution. When shell scripts call `am broadcast` directly, the command often fails silently because:

1. The shell process runs in a background context
2. Android blocks broadcasts from background processes
3. API commands appear to "hang" or return nothing

This affected **32 out of 62** termux-api commands in MobileCLI.

## The Solution: File-Based IPC

Instead of scripts calling `am broadcast` directly, they now:

1. Write the command to `~/.termux/am_command`
2. `TermuxService` (running in foreground) polls this file
3. Service executes the command with proper app permissions
4. Result is written to `~/.termux/api_result_<pid>`
5. Script reads result and cleans up

### Why This Works

- TermuxService runs with foreground service notification
- It has full app context and permissions
- Android allows broadcasts from foreground app context
- File polling avoids background process restrictions

## Fixed Scripts (32)

### Bluetooth (5)
- `termux-bluetooth-connect`
- `termux-bluetooth-enable`
- `termux-bluetooth-info`
- `termux-bluetooth-paired`
- `termux-bluetooth-scaninfo`

### Camera (1)
- `termux-camera-photo`

### SAF - Storage Access Framework (8)
- `termux-saf-create`
- `termux-saf-dirs`
- `termux-saf-ls`
- `termux-saf-mkdir`
- `termux-saf-read`
- `termux-saf-rm`
- `termux-saf-stat`
- `termux-saf-write`

### Media (3)
- `termux-media-player`
- `termux-media-scan`
- `termux-microphone-record`

### SMS/Phone (2)
- `termux-sms-send`
- `termux-telephony-call`

### Network (1)
- `termux-wifi-enable`

### Power (2)
- `termux-wake-lock`
- `termux-wake-unlock`

### IR (1)
- `termux-infrared-transmit`

### Other (9)
- `termux-dialog`
- `termux-download`
- `termux-job-scheduler`
- `termux-keystore`
- `termux-keystore-list`
- `termux-share`
- `termux-speech-to-text`
- `termux-storage-get`
- `termux-wallpaper`

## Script Template (v6.0.0)

```bash
#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API (v6.0.0 - File-based)
TERMUX_DIR="/data/data/com.termux/files/home/.termux"
CMD_FILE="$TERMUX_DIR/am_command"
AM_RESULT="$TERMUX_DIR/am_result"
API_RESULT="$TERMUX_DIR/api_result_$$"
mkdir -p "$TERMUX_DIR"
rm -f "$API_RESULT" "$AM_RESULT" 2>/dev/null
ARGS="${*:-_}"
[ -z "$ARGS" ] && ARGS="_"
echo "broadcast -n com.termux/com.termux.TermuxApiReceiver -a com.termux.api.API_CALL --es api_method METHOD_NAME --es api_args $ARGS --es result_file $API_RESULT" > "$CMD_FILE"
for i in $(seq 1 30); do
    if [ -f "$API_RESULT" ]; then
        cat "$API_RESULT"
        rm -f "$API_RESULT" "$AM_RESULT" 2>/dev/null
        exit 0
    fi
    [ -f "$AM_RESULT" ] && rm -f "$AM_RESULT"
    sleep 0.1
done
exit 0
```

## How TermuxService Handles Commands

```kotlin
// In TermuxService.kt - startCommandWatcher()
private fun startCommandWatcher() {
    commandWatcherJob = serviceScope.launch {
        val cmdFile = File(homeDir, ".termux/am_command")
        val resultFile = File(homeDir, ".termux/am_result")

        while (isActive) {
            if (cmdFile.exists()) {
                val cmd = cmdFile.readText().trim()
                cmdFile.delete()

                // Execute with app context (has proper permissions)
                val process = Runtime.getRuntime().exec(arrayOf("/system/bin/am") + cmd.split(" "))
                val result = process.inputStream.bufferedReader().readText()
                process.waitFor()

                resultFile.writeText(result)
            }
            delay(100) // Poll every 100ms
        }
    }
}
```

## Testing

```bash
# Test battery status (should return JSON)
termux-battery-status

# Test bluetooth info
termux-bluetooth-info

# Test wake lock
termux-wake-lock
termux-wake-unlock

# Test camera (requires permission)
termux-camera-photo -o /sdcard/Download/test.jpg
```

## Version History

| Version | Changes |
|---------|---------|
| 5.0.0 | Initial file-based IPC for some commands |
| 5.7.0 | 30 scripts using file-based system |
| **6.0.0** | **All 62 API scripts now use file-based system** |

## Files Modified

| File | Change |
|------|--------|
| `app/build.gradle.kts` | Version 5.7.0 → 6.0.0 |
| `BootstrapInstaller.kt` | BOOTSTRAP_VERSION, all 32 scripts |

## Backward Compatibility

- Existing scripts continue to work
- No API changes for users
- BOOTSTRAP_VERSION change ensures fresh install of scripts
