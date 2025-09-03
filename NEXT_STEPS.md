# mdless — Next Options

- Parser coverage: add lists (ordered/unordered), blockquotes (basic), fenced code blocks with language, and basic pipe tables.
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

Completed since last step
- Colored TTY rendering in interactive mode via `--theme`.
- Search match highlighting with underline; basic cache invalidation on reflow.
- Tests for renderer highlights and CLI theme argument parsing.
- Inline styles preserved in interactive layout using `LayoutStyled`.
