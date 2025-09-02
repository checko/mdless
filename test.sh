#!/usr/bin/env bash
set -euo pipefail

KOTLINC="/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin/kotlinc-native"
OUT_DIR="build/tests"

mkdir -p "$OUT_DIR"

run_test() {
  local name="$1"; shift
  local out="$OUT_DIR/$name"
  echo "[test] Building $name"
  "$KOTLINC" -opt -o "$out" "$@"
  echo "[test] Running $name"
  if [[ -x "$out" ]]; then
    "$out" || { echo "[test] FAIL: $name"; exit 1; }
  elif [[ -x "$out.kexe" ]]; then
    "$out.kexe" || { echo "[test] FAIL: $name"; exit 1; }
  else
    echo "[test] FAIL: $name (no runnable output at $out or $out.kexe)"
    exit 1
  fi
  echo "[test] PASS: $name"
}

# Smoke
run_test Smoke test/smoke/MainSmoke.kt

# IR
run_test IrTest test/ir/IrTest.kt src/ir/Ir.kt

# Width (ASCII)
run_test WidthAsciiTest test/layout/WidthAsciiTest.kt src/layout/Width.kt

# Layout paragraphs/headings
run_test LayoutParaTest test/layout/LayoutParaTest.kt \
  src/layout/Layout.kt src/layout/Width.kt src/ir/Ir.kt

# Theme & Styler
run_test ThemeTest test/style/ThemeTest.kt \
  src/style/Theme.kt src/style/Styler.kt src/ir/Ir.kt

echo "[test] All tests passed."
