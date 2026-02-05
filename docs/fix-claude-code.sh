#!/data/data/com.termux/files/usr/bin/bash
#
# fix-claude-code.sh - Apply environment fixes for Claude Code on MobileCLI
#
# This script fixes known issues with Claude Code running on Android/Termux:
# 1. Creates symlink for ripgrep (arm64-android -> arm64-linux)
# 2. Ensures working directories exist
# 3. Creates required temp directories
#
# Run this script once after installing Claude Code, or whenever tools break.
#

set -e

echo "=========================================="
echo "  MobileCLI Claude Code Environment Fix"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

success() { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; }
info() { echo "     $1"; }

# ====================
# FIX 1: Ripgrep Symlink
# ====================
echo "Fix 1: Ripgrep binary symlink (Glob/Grep tools)"
echo "------------------------------------------------"

RIPGREP_DIR="$PREFIX/lib/node_modules/@anthropic-ai/claude-code/vendor/ripgrep"

if [ -d "$RIPGREP_DIR" ]; then
    if [ -L "$RIPGREP_DIR/arm64-android" ]; then
        success "Symlink already exists"
    elif [ -d "$RIPGREP_DIR/arm64-linux" ]; then
        cd "$RIPGREP_DIR"
        ln -sf arm64-linux arm64-android
        success "Created arm64-android -> arm64-linux symlink"
    else
        fail "arm64-linux directory not found in $RIPGREP_DIR"
        info "You may need to reinstall Claude Code"
    fi
else
    warn "Claude Code ripgrep directory not found"
    info "Expected: $RIPGREP_DIR"
    info "This fix will be needed after you install Claude Code"
fi
echo ""

# ====================
# FIX 2: Working Directories
# ====================
echo "Fix 2: Working directories"
echo "--------------------------"

# Create ~/tmp if it doesn't exist
if [ ! -d "$HOME/tmp" ]; then
    mkdir -p "$HOME/tmp"
    success "Created ~/tmp"
else
    success "~/tmp already exists"
fi

# Create $PREFIX/tmp if it doesn't exist
if [ ! -d "$PREFIX/tmp" ]; then
    mkdir -p "$PREFIX/tmp"
    success "Created \$PREFIX/tmp"
else
    success "\$PREFIX/tmp already exists"
fi

# Ensure .mobilecli directories exist
if [ ! -d "$HOME/.mobilecli/memory" ]; then
    mkdir -p "$HOME/.mobilecli/memory"
    success "Created ~/.mobilecli/memory"
else
    success "~/.mobilecli/memory already exists"
fi

if [ ! -d "$HOME/.mobilecli/config" ]; then
    mkdir -p "$HOME/.mobilecli/config"
    success "Created ~/.mobilecli/config"
else
    success "~/.mobilecli/config already exists"
fi
echo ""

# ====================
# FIX 3: Environment Check
# ====================
echo "Fix 3: Environment verification"
echo "--------------------------------"

# Check if termux-api is installed
if command -v termux-battery-status &> /dev/null; then
    success "termux-api commands available"
else
    warn "termux-api not found"
    info "Install with: pkg install termux-api"
fi

# Check if rg (ripgrep) is available via symlink
if [ -L "$RIPGREP_DIR/arm64-android" ] && [ -f "$RIPGREP_DIR/arm64-android/rg" ]; then
    success "ripgrep binary accessible"
else
    warn "ripgrep binary may not work"
fi

# Check HOME is correct
if [ "$HOME" = "/data/data/com.termux/files/home" ] || [ "$HOME" = "/data/user/0/com.termux/files/home" ]; then
    success "HOME directory is correct: $HOME"
else
    warn "HOME might be incorrect: $HOME"
    info "Expected: /data/data/com.termux/files/home"
fi

echo ""
echo "=========================================="
echo "  Fix script completed!"
echo "=========================================="
echo ""
echo "Tool Compatibility After Fix:"
echo "  Read    - Always works"
echo "  Write   - Always works"
echo "  Edit    - Always works"
echo "  Bash    - Works (avoid deleted CWD)"
echo "  Glob    - Works (after ripgrep fix)"
echo "  Grep    - Works (after ripgrep fix)"
echo "  Task    - BROKEN (uses /tmp, unfixable)"
echo ""
echo "If Bash commands fail with 'exit code 1':"
echo "  The working directory may have been deleted."
echo "  Use 'cd ~' or restart Claude Code session."
echo ""
echo "For more details, see ~/MOBILECLI_AI_BRIEFING.md"
