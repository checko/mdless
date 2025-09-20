#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${KONAN_DATA_DIR:-}" ]]; then
  export KONAN_DATA_DIR="$PWD/.konan-cache"
fi
mkdir -p "$KONAN_DATA_DIR"

KOTLINC="/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin/kotlinc-native"
OUT_DIR="build"
APP_OUT="$OUT_DIR/mdless"

mkdir -p "$OUT_DIR"

echo "[build] Compiling application to $APP_OUT"
"$KOTLINC" -opt -o "$APP_OUT" \
  src/**/*.kt

# Kotlin/Native may emit a .kexe on Linux; normalize to 'mdless'
if [[ -f "$APP_OUT.kexe" ]]; then
  mv -f "$APP_OUT.kexe" "$APP_OUT"
fi

chmod +x "$APP_OUT" || true

echo "[build] Done: $APP_OUT"
