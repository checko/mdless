#!/usr/bin/env bash
set -euo pipefail

KOTLINC="/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin/kotlinc-native"
OUT_DIR="build"
APP_OUT="$OUT_DIR/mdless"

mkdir -p "$OUT_DIR"

echo "[build] Compiling application to $APP_OUT"
"$KOTLINC" -opt -o "$APP_OUT" \
  src/**/*.kt

echo "[build] Done: $APP_OUT (or $APP_OUT.kexe depending on platform)"

