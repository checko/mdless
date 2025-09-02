# mdless — Memo for Next Session

Context
- Language/Toolchain: Kotlin/Native (no JVM). Compiler at `/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin/kotlinc-native`.
- Target: Linux x86_64 native CLI.
- Constraints: ASCII-only width for now; HTML out-of-scope; images show alt text only.

Repo Layout
- `src/cli` — Main, App (key handling), Keys
- `src/parser` — Parser (Phase A: headings, paragraphs, HR, inline code/emph/strong)
- `src/ir` — IR types (Block/Inline/StyledSpan/LayoutLine)
- `src/style` — Theme presets and Styler
- `src/layout` — Width (ASCII) and Layout (paragraphs/headings/code block)
- `src/render` — ANSI renderer
- `src/pager` — PagerState (viewport/scroll/mapping), Search (basic line scan)
- `src/tty` — Raw mode, term size, TTY checks
- `test/` — Native test runners; see `test.sh`

What Works Now
- Build: `./build.sh` produces `build/mdless` (or `.kexe` depending on platform).
- Tests: `./test.sh` builds and runs all native test runners; all passing.
- CLI: Non-interactive prints full render (no color). Interactive when stdout is TTY:
  - Keys: j/k (line), SPACE/b (page), g/G (top/bottom), q (quit)
  - Search: `/` and `?` prompt; `n`/`N` repeat; jumps to first matching line
  - Resize: loop polls terminal size and reflows; preserves scroll clamped to new bounds
  - Rendering: text only (no color) with simple status line

Decisions (to remember)
- Width: ASCII-only for now; plan to switch to POSIX `wcwidth` later.
- HTML blocks are out-of-scope for MVP.
- Images: render alt text only in MVP.
- Tests: simple native runners, no Gradle.

Immediate Next Options
- See `NEXT_STEPS.md` for prioritized items (highlight matches, colored TTY rendering, parser coverage for lists/quotes/code fences/tables, TOC, caching, unicode width via wcwidth, wrap off/horizontal scroll, CLI flags, more tests, packaging).

Handy Commands
- Build app: `/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin/kotlinc-native -opt -o build/mdless src/**/*.kt`
- Run tests: `./test.sh`
- Interactive run: `./build.sh && ./build/mdless README.md`
- Pipe mode: `cat README.md | ./build/mdless`

Notes
- Renderer supports ANSI but interactive path currently calls it with `enableColor=false`.
- Layout currently wraps paragraphs and headings; code blocks are line-based without wrapping.
- Parser is minimal; no lists/quotes/fences/tables yet.

