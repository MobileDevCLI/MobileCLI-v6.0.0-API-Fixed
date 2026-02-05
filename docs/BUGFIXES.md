# MobileCLI Bug Fixes Documentation

This document tracks major bugs that were discovered and fixed, to help prevent regressions and aid future debugging.

---

## BUG-001: Welcome Message Blocking Terminal Output

**Date Fixed:** January 26, 2026
**Version:** v2.1.1
**Severity:** High
**Affected File:** `app/src/main/java/com/termux/BootstrapInstaller.kt`

### Problem Description

A welcome message was being printed to the terminal on every new bash session (new tab, bash call, subshell). This caused:

1. **Screen clearing** - The `clear` command wiped Claude Code's output
2. **Overlay effect** - The welcome banner covered terminal content
3. **Claude Code interference** - AI couldn't read code because the banner was on top

### Symptoms

- When opening a new tab: "Welcome to MobileCLI" banner appears
- When bash commands run: Banner appears and clears previous output
- Claude Code reports it can't see the code/terminal content

### Root Cause

In `BootstrapInstaller.kt`, the `.bashrc` file was being created with a welcome message block (lines 423-436 in the original):

```bash
# Welcome message on new session
clear
echo ""
echo "  ╔═══════════════════════════════════════╗"
echo "  ║       Welcome to MobileCLI            ║"
echo "  ╚═══════════════════════════════════════╝"
echo ""
echo "  Start an AI assistant:"
echo "    claude  - Claude Code"
echo "    gemini  - Gemini CLI"
echo "    codex   - Codex CLI"
echo ""
echo "  Or use the terminal normally."
echo ""
```

This was originally added as an IP protection measure / branding, but it interfered with normal terminal operation.

### The Fix

**Removed the entire welcome message block** from the bashrc content in `BootstrapInstaller.kt`.

The PS1 prompt remains (so users still see `user@mobilecli:~$`), but no banner is printed.

### Files Changed

- `app/src/main/java/com/termux/BootstrapInstaller.kt` - Removed 15 lines (welcome message block)

### How to Verify the Fix

1. Install the fixed APK
2. Open a new tab - should NOT show welcome banner
3. Run `bash` command - should NOT clear screen or show banner
4. Claude Code should be able to read terminal output normally

### Regression Prevention

**DO NOT add any `echo` or `clear` commands to `.bashrc` that run on every session.**

If branding is needed, consider:
- A one-time welcome on first launch only (check a flag file)
- A motd-style message that doesn't clear the screen
- Showing info in the app UI instead of terminal

### Related Commits

- `f15897d` - fix: Remove welcome message that blocks terminal output

---

## Version History

| Version | Date | Bug Fixed |
|---------|------|-----------|
| v2.1.1 | 2026-01-26 | BUG-001: Welcome message blocking terminal |
| v2.1.0 | 2026-01-26 | Initial release with PayPal integration |

---

*Add new bug fixes above this line, following the same format.*
