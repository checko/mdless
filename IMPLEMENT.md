# mdless — Implementation Notes

## Build & Toolchain

- Language: Kotlin using Kotlin/Native (no JVM runtime).
- Compiler: `/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin/kotlinc-native`.
- Output: native executable `mdless` (no JVM).

Example build command (single-module):

```
/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin/kotlinc-native \
  -opt \
  -o build/mdless \
  src/**/*.kt
```

Notes:
- Produces `build/mdless.kexe` or a platform executable named `build/mdless` depending on version. Rename/symlink to `mdless` for convenience.
- For release builds, include `-opt` and strip symbols if desired via `-linker-options "-s"`.

## Project Layout

- `src/` top-level Kotlin sources
  - `parser/` — Markdown → IR
  - `ir/` — Block, Inline, StyledSpan models
  - `style/` — theme mapping
  - `layout/` — wrapping, tables, cache
  - `pager/` — viewport state, search index
  - `tty/` — raw mode, input, SIGWINCH
  - `render/` — ANSI renderer
  - `cli/` — args, flags, main()

## Key Modules & APIs

- Parser
  - `fun parseMarkdown(src: String): List<Block>`
  - Minimal Markdown 80/20: headings (#..######), lists (-,*,+ and 1.), blockquotes (>), fenced code (```), inline code (`), emphasis (*, _), links [text](url), images ![alt](url), hr (---, ***), tables (| col | col |).

- IR
  - `data class Block(id: Int, kind: BlockKind, meta: Meta, inlines: List<Inline> = emptyList())`
  - `sealed class BlockKind { Heading(level:Int); Paragraph; List(...); Blockquote(...); CodeBlock(...); ThematicBreak; Table(...); Image(alt:String) }`
  - `sealed class Inline { Text; Emph; Strong; Code; Link; SoftBreak; HardBreak }`

- Styler
  - `data class Theme(val mode: Mode, ...colors...)`
  - `fun style(block: Block, theme: Theme): List<StyledSpan>` for paragraphs/headings; or style during layout per inline.

- Layout
  - `fun layoutBlock(block: Block, width: Int, theme: Theme, tabWidth: Int): List<LayoutLine>`
  - `class LayoutCache { get(blockId, key): List<LayoutLine>; put(...); invalidateOnWidthOrThemeChange(...) }`
  - Unicode width via POSIX `wcwidth()`/`wcswidth()` interop or a small embedded table for BMP; tabs expanded by `tabWidth`.

- Pager
  - `data class Viewport(topLine: Int, height: Int, width: Int)`
  - `class PagerState { fun scrollLines(n:Int); scrollPages(n:Int); jumpToTop(); jumpToBottom(); findNext(); findPrev(); }`
  - Search index: `class SearchIndex(blocks: List<Block>) { fun query(q:String): Sequence<Match> }`

- Renderer
  - `fun render(view: List<LayoutLine>, status: StatusLine, noColor: Boolean, out: Appendable)`

- TTY
  - `fun withRawMode(body: () -> Unit)` using `termios` via Kotlin/Native POSIX interop; SIGWINCH handler updates viewport width.

## Implementation Steps (Option 2)

1) Skeleton & CLI
- Implement `main()` parsing flags: theme, wrap, paging, width, toc, no-syntax, links mode, tab width.
- Detect TTY vs pipe (isatty) and choose interactive vs straight render.

2) Minimal Parser → IR
- Start with headings, paragraphs, lists, code blocks, blockquotes, hr, inline code/emph/strong, links, images (alt only), and basic tables.
- Normalize to IR; assign stable incremental block IDs.

3) Theme & Styling
- Define Theme colors for light/dark/no-color.
- Map headings, code, links, quotes to styles; collapse to plain text in no-color mode.

4) Layout Engine
- Implement `wcwidth` adapter (POSIX interop) or embed a compact width function.
- Word-wrap by width; preserve indentation for code blocks; support soft/hard breaks; tab expansion.
- Cache results per block keyed by `(width, themeMode, tabWidth)`.

5) Pager State & Virtualization
- Build `PagerState` over a logical `List<LayoutLine>` without materializing everything: keep per-block prefix sums of line counts and map logical line index ↔ (blockId,rowInBlock).
- Implement scrolling, page jumps, and status percent.

6) Search
- Build a normalized text index per block on load.
- Searching returns (blockId, inlineRange) hits; on demand, map hits to visible LayoutLines for highlight.

7) Renderer & TTY
- Write a minimal ANSI renderer; add a status line with percent and current section.
- Add raw mode input handling; parse keys for navigation and search.
- Handle SIGWINCH: recompute width, invalidate cache keys, remap top line logically.

8) Integration & Flags
- Implement `--wrap on|off` (wrap off enables horizontal scroll with h/l), `--paging auto|always|never`.
- Implement `--links inline|footnote|hide` by changing styler/layout rendering.

9) Tests & Fixtures
- Parser/IR golden tests; layout snapshots with fixed width; search mapping tests; end-to-end headless driver.

## Unicode Width Strategy

- Preferred: POSIX `wcwidth()`/`wcswidth()` via Kotlin/Native `platform.posix`. Cache per code point for speed.
- Fallback: embed a minimal width table for ASCII + common BMP; treat combining marks as width 0; East Asian Ambiguous as 1 (configurable later).

## TTY & Signals

- Use `tcgetattr/tcsetattr` to toggle raw mode. Restore raw mode on exit or exceptions with a shutdown hook. Use non-blocking read or poll/select for input.
- Capture `SIGWINCH` to trigger layout invalidation and viewport recompute.

## Notes on a /tmp Pre-render Variant

- If ever needed, wire an alternate code path: IR → full render string → write `/tmp/mdless-XXXX` → reuse Pager with a read-only file-backed line source. Keep IR around for TOC/search mapping if desired.

