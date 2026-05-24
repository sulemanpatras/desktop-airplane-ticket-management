#!/bin/bash
# ═══════════════════════════════════════════════════════
#  SkyBook – Build & Run Script
#  Requires: JDK 17+, JavaFX SDK
# ═══════════════════════════════════════════════════════

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
OUT_DIR="$PROJECT_DIR/out"
JAVAFX_LIB="C:/Program Files/Java/openjfx-26.0.1_windows-x64_bin-sdk/javafx-sdk-26.0.1/lib"
PDF_LIB="C:/Program Files/Java/pdf"

ITEXT_CP="${PDF_LIB}/kernel-7.2.5.jar;${PDF_LIB}/layout-7.2.5.jar;${PDF_LIB}/io-7.2.5.jar;${PDF_LIB}/commons-7.2.5.jar;${PDF_LIB}/slf4j-api-1.7.36.jar;${PDF_LIB}/slf4j-simple-1.7.36.jar"

echo "══════════════════════════════════════"
echo "  SkyBook Build & Run"
echo "══════════════════════════════════════"

# ── Verify JARs ───────────────────────────────────────────────────────────────
echo "► Checking iText7 JARs..."
for jar in \
  "${PDF_LIB}/kernel-7.2.5.jar" \
  "${PDF_LIB}/layout-7.2.5.jar" \
  "${PDF_LIB}/io-7.2.5.jar" \
  "${PDF_LIB}/commons-7.2.5.jar" \
  "${PDF_LIB}/slf4j-api-1.7.36.jar" \
  "${PDF_LIB}/slf4j-simple-1.7.36.jar"
do
  if [ -f "$jar" ]; then
    echo "  ✓ $jar"
  else
    echo "  ✗ MISSING: $jar"
    exit 1
  fi
done

# ── Compile ───────────────────────────────────────────────────────────────────
mkdir -p "$OUT_DIR"
SOURCES=$(find "$SRC_DIR" -name "*.java" | tr '\n' ' ')

echo "► Compiling sources..."
javac \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "$ITEXT_CP" \
  -d "$OUT_DIR" \
  $SOURCES

echo "► Compilation successful!"

# ── Convert OUT_DIR to Windows-style path ─────────────────────────────────────
# e.g. /c/Users/Suleman/... → C:/Users/Suleman/...
WIN_OUT=$(echo "$OUT_DIR" | sed 's|^/\([a-zA-Z]\)/|\1:/|')
echo "► OUT_DIR (Windows): $WIN_OUT"
echo "► Launching SkyBook..."

# ── Run ───────────────────────────────────────────────────────────────────────
java \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "${WIN_OUT};${ITEXT_CP}" \
  skybook.ui.MainApp

echo "══════════════════════════════════════"
echo "  Done."
echo "══════════════════════════════════════"