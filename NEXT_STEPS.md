# mdless — Next Options

- Highlight matches: add search match ranges mapped to LayoutLine spans and render with reverse/underline; toggle highlight on/off.
- Colored TTY rendering: integrate Styler + Renderer in interactive mode; support theme switch `--theme dark|light|no-color`.
- Parser coverage: add lists (ordered/unordered), blockquotes with nesting, fenced code blocks with language, and basic tables.
- Links/Images modes: implement `--links inline|footnote|hide`; footnotes with index and URL section.
- TOC and jumps: build heading index, `--toc` flag, jump-to-section and breadcrumb in status.
- Caching/performance: LayoutCache keyed by `(blockId,width,themeMode,tabWidth)` with prefetch margin; preserve visual position on resize.
- Unicode width: replace ASCII fallback with POSIX `wcwidth()/wcswidth()` via Kotlin/Native `platform.posix`.
- Horizontal scroll: implement wrap off + `h/l` keys and `--wrap on|off` flag; track x-offset in PagerState.
- Status line polish: show current section, line/percent/width, link hint mode; colorize status.
- CLI flags: complete parsing for `--paging`, `--width`, `--tab-width`, `--no-syntax`, `--toc`, `--links`.
- Tests: add golden fixtures for parser and layout, renderer ANSI snapshots, and headless key-driven integration tests.
- Packaging: small install script, `--version`, and release build flags (strip, -opt).

