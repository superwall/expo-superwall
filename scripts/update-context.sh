#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CONTEXT_DIR="$ROOT_DIR/.context"

echo "Cleaning .context directory..."
rm -rf "$CONTEXT_DIR/superwall-android"
rm -rf "$CONTEXT_DIR/superwall-ios"

echo "Cloning Superwall-Android..."
git clone git@github.com:superwall/Superwall-Android.git "$CONTEXT_DIR/superwall-android"

echo "Cloning Superwall-iOS..."
git clone git@github.com:superwall/Superwall-iOS.git "$CONTEXT_DIR/superwall-ios"

echo "Removing .git directories..."
rm -rf "$CONTEXT_DIR/superwall-android/.git"
rm -rf "$CONTEXT_DIR/superwall-ios/.git"

echo "Done."
