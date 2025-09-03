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

# Layout list/blockquote
run_test LayoutListQuoteTest test/layout/LayoutListQuoteTest.kt \
  src/layout/Layout.kt src/layout/Width.kt src/ir/Ir.kt

# Layout long word hard wrap
run_test LayoutLongWordTest test/layout/LayoutLongWordTest.kt \
  src/layout/Layout.kt src/layout/Width.kt src/ir/Ir.kt

# Layout nested list
run_test LayoutNestedListTest test/layout/LayoutNestedListTest.kt \
  src/layout/Layout.kt src/layout/Width.kt src/ir/Ir.kt

# Theme & Styler
run_test ThemeTest test/style/ThemeTest.kt \
  src/style/Theme.kt src/style/Styler.kt src/ir/Ir.kt

# Renderer
run_test RendererTest test/render/RendererTest.kt \
  src/render/Renderer.kt src/ir/Ir.kt

# Renderer highlight
run_test RendererHighlightTest test/render/RendererHighlightTest.kt \
  src/render/Renderer.kt src/ir/Ir.kt

# Styled layout integration
run_test StyledLayoutIntegrationTest test/render/StyledLayoutIntegrationTest.kt \
  src/layout/LayoutStyled.kt src/layout/Width.kt src/style/Styler.kt src/style/Theme.kt src/render/Renderer.kt src/ir/Ir.kt

# Parser basic
run_test ParserBasicTest test/parser/ParserBasicTest.kt \
  src/parser/Parser.kt src/ir/Ir.kt

# Parser lists and blockquotes
run_test ParserListsQuotesTest test/parser/ParserListsQuotesTest.kt \
  src/parser/Parser.kt src/ir/Ir.kt

# Parser fenced code and tables
run_test ParserFencesTablesTest test/parser/ParserFencesTablesTest.kt \
  src/parser/Parser.kt src/ir/Ir.kt

# Parser nested lists
run_test ParserNestedListTest test/parser/ParserNestedListTest.kt \
  src/parser/Parser.kt src/ir/Ir.kt

# Pager
run_test PagerTest test/pager/PagerTest.kt \
  src/pager/Pager.kt

# App key handling (no TTY)
run_test AppKeysTest test/cli/AppKeysTest.kt \
  src/cli/App.kt src/cli/Keys.kt src/pager/Pager.kt

# Search
run_test SearchTest test/pager/SearchTest.kt \
  src/pager/Search.kt src/ir/Ir.kt

# CLI theme arg parsing
run_test ThemeArgParseTest test/cli/ThemeArgParseTest.kt \
  src/cli/Options.kt src/style/Theme.kt src/ir/Ir.kt

echo "[test] All tests passed."
