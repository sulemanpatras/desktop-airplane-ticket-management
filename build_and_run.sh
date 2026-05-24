#!/bin/bash
# ═══════════════════════════════════════════════════════
#  SkyBook – Build & Run Script
#  Requires: JDK 17+, JavaFX SDK
# ═══════════════════════════════════════════════════════

# Download JavaFX SDK if not present (adjust version/OS as needed)
# https://gluonhq.com/products/javafx/

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
OUT_DIR="$PROJECT_DIR/out"
JAVAFX_LIB="C:/Program Files/Java/openjfx-26.0.1_windows-x64_bin-sdk/javafx-sdk-26.0.1/lib"

echo "══════════════════════════════════════"
echo "  SkyBook Build & Run"
echo "══════════════════════════════════════"

# Create output directory
mkdir -p "$OUT_DIR"

# Find all Java files
SOURCES=$(find "$SRC_DIR" -name "*.java" | tr '\n' ' ')

echo "► Compiling sources..."
javac \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml \
  -d "$OUT_DIR" \
  $SOURCES

echo "► Compilation successful!"
echo "► Launching SkyBook..."

java \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "$OUT_DIR" \
  skybook.ui.MainApp

echo "══════════════════════════════════════"
echo "  Done."
echo "══════════════════════════════════════"
