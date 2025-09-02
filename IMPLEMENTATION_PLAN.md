# mdless — Stepwise Implementation Plan (Kotlin/Native)

This plan sequences development by modules. Each step delivers:
- source code for the module
- a focused test runner (native executable) that returns non‑zero on failure
- minimal integration glue where helpful

We prioritize correctness and tight feedback loops. Early stages use ASCII‑only width, then switch to POSIX `wcwidth()` via Kotlin/Native interop.

## 0) Bootstrap

- Create repo structure under `src/` with empty packages and a minimal `main()`.
- Add a `build.sh` and `test.sh` invoking Kotlin/Native from `/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin`.
- Output binaries under `build/`.

Deliverables:
- `src/cli/Main.kt` with `--help` stub.
- `build.sh` builds `build/mdless`.

Tests:
- `test/smoke/MainSmoke.kt` prints OK; runner exits 0.

## 1) Core Types (IR)

Goal: Define immutable IR types for blocks/inlines and shared enums.

- Files: `src/ir/Ir.kt` with `Block`, `BlockKind`, `Inline`, `StyledSpan`, `LayoutLine` data classes.
- Provide stable `id` generation for blocks.

Tests:
- `test/ir/IrTest.kt`: construct several IR instances and check invariants (ids unique, paragraph has inlines, etc.).

Integration:
- None; compiled by smoke build.

## 2) Unicode Width (Phase A: ASCII)

Goal: Implement basic width calc to unblock layout; ASCII only for now.

- Files: `src/layout/Width.kt` with `fun charWidth(c: Int): Int` and `fun stringWidth(s: String): Int`.
- Treat ASCII printable 32..126 as width 1; tabs handled later; others default 1 for now.

Tests:
- `test/layout/WidthAsciiTest.kt` covering ASCII, combining ignored for now.

Integration:
- Used by next module.

## 3) Layout Engine (Paragraphs/Headings)

Goal: Wrap text into `LayoutLine` for Paragraph and Heading blocks.

- Files: `src/layout/Layout.kt` with `layoutBlock(...)`, `LayoutCache` (no caching yet), and tab expansion placeholder.
- Supports soft/hard breaks; preserves indentation prefix.

Tests:
- `test/layout/LayoutParaTest.kt`: snapshot like checks for known inputs at widths 20/40.

Integration:
- Small driver `test/run/LayoutDriver.kt` renders a paragraph to stdout for visual diff.

## 4) Styler & Theme

Goal: Map IR kinds to styles (colors, bold, underline) with light/dark/no‑color modes.

- Files: `src/style/Theme.kt`, `src/style/Styler.kt`.
- `Theme(mode=Light|Dark|NoColor)`; `applyStyles(block,inlines,theme)`.

Tests:
- `test/style/ThemeTest.kt` ensures style mapping; for NoColor, style collapses.

Integration:
- Layout consumes styled spans when wrapping.

## 5) Minimal Renderer

Goal: Render a slice of `LayoutLine` to ANSI text.

- Files: `src/render/Renderer.kt` with `render(lines, status, out)`; status has percent + section string.

Tests:
- `test/render/RendererTest.kt` compares ANSI snapshots for simple layouts and NoColor mode.

Integration:
- Add `src/cli/Preview.kt` to preview a file: parse→IR (stub)→style→layout→render.

## 6) Parser (Phase A: Headings, Paragraphs, HR, Inline code/text)

Goal: Minimal Markdown parse sufficient to demo the pipeline.

- Files: `src/parser/Parser.kt` with a line‑oriented parser: headings (#), hr (---/***), paragraphs, inline code `` and emphasis markers parsed naively.

Tests:
- `test/parser/ParserBasicTest.kt` golden tests from Markdown → IR model (string form).

Integration:
- Wire into preview path for a basic end‑to‑end run.

## 7) Pager State (Vertical only)

Goal: Track viewport and scrolling over virtualized lines.

- Files: `src/pager/Pager.kt` with `PagerState`, prefix sums, and mapping logical line ↔ (blockId,row).

Tests:
- `test/pager/PagerTest.kt` covers scroll math, top/bottom clamp, percentages.

Integration:
- Extend preview to accept height/width and select the visible slice.

## 8) TTY IO (raw mode, resize)

Goal: Basic interactive loop for j/k, SPACE/b, g/G, q and SIGWINCH.

- Files: `src/tty/Tty.kt` (termios raw mode, read keys), `src/cli/App.kt` driving the loop.

Tests:
- `test/tty/TtyFakeTest.kt` using a fake input source; separate small native test for raw/restore that runs only in CI that supports TTY (can be skipped locally).

Integration:
- `mdless` becomes interactive when stdout is a TTY, otherwise prints once.

## 9) Search (/, ?, n/N) — basic

Goal: Plain text search over normalized block text; highlight matches.

- Files: `src/pager/Search.kt` with `SearchIndex` mapping blockId → text; simple forward/backward search.

Tests:
- `test/pager/SearchTest.kt` covering wrapping across lines, multiple matches, case sensitivity toggle.

Integration:
- Wire to renderer to draw highlights over spans.

## 10) Lists, Blockquotes, Code Blocks

Goal: Expand parser + styler + layout coverage.

- Parser: unordered/ordered lists, blockquotes '>' nesting, fenced code ``` with language tag.
- Layout: preserve indentation for lists/code; monospaced code spans; quote bar styling.

Tests:
- Golden parser tests; layout snapshots for list wrapping and code indentation.

Integration:
- Add `--no-syntax` to skip syntax highlight for code blocks (placeholder).

## 11) Tables (basic)

Goal: Parse pipe tables and align columns; degrade on narrow widths.

- Layout: compute per‑column widths, split cells, wrap rows.

Tests:
- Layout snapshot for varying widths; alignment correctness.

Integration:
- Renderer draws table borders minimally (spaces and paddings only, no heavy glyphs in MVP).

## 12) Links/Images modes and TOC

Goal: Implement `--links inline|footnote|hide`, and build a TOC from headings.

- Search/jump to heading via `:toc` or `--toc` flag.

Tests:
- Verify link rendering modes; TOC jump correctness.

Integration:
- Status line shows current section based on nearest heading above viewport.

## 13) Caching & Performance polish

Goal: Add `LayoutCache` keyed by `(blockId,width,themeMode,tabWidth)` and small prefetch margin.

Tests:
- Resize invalidation tests; ensure cache hits on scroll.

Integration:
- SIGWINCH remaps top line to (block,row) to preserve visual position.

## 14) Non‑interactive mode, flags, help

Goal: Complete CLI flags, `--help`, `--paging`, `--wrap`, `--width`, `--tab-width`, `--theme`, `--no-syntax`.

Tests:
- Arg parsing tests; verify pipe mode outputs once and exits 0.

---

## Test Running Strategy

- Each test runner is a small native binary under `build/tests/<name>` returning non‑zero on failure.
- `test.sh` builds and runs all test runners, printing a concise summary.
- For snapshot tests, keep expected outputs in `testdata/` and compare byte‑wise.

## Build Commands

- Build app:
```
/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin/kotlinc-native \
  -opt -o build/mdless \
  src/**/*.kt
```

- Build a test runner (example):
```
/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin/kotlinc-native \
  -opt -o build/tests/IrTest \
  test/ir/IrTest.kt src/ir/Ir.kt
```

Note: If glob expansion is limited, use a generated file list in `build.sh`.

## Open Decisions

- Unicode width: switch from ASCII fallback (Step 2) to POSIX `wcwidth()` in Step 13 or earlier once stable.
- Syntax highlighting: out of MVP; consider later via lightweight library or custom.
- Formal test framework: we can keep custom runners or adopt kotlin.test with Gradle later if desired.

## Acceptance per Milestone

- After Step 6: non‑interactive demo showing headings/paragraphs rendered with styling and wrapping.
- After Step 8: interactive scrolling in a TTY with resize handling.
- After Step 9: interactive search with highlight.
- After Step 12: TOC jumps and link display modes.
- After Step 14: CLI feature complete for MVP.

