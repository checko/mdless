# mdless

A fast, minimal Markdown viewer for the terminal. It renders Markdown to ANSI-styled text and lets you navigate with less-like keys, including search with highlights and resize-aware wrapping. Built in Kotlin/Native for a single self-contained binary on Linux. [^codex]

## Features
- Markdown coverage: headings, paragraphs, lists (nested), blockquotes, fenced code (with language tag captured), horizontal rules, links/images (alt text only), basic pipe tables with per-column alignment.
- Theming: dark, light, and no-color themes; inline styles for headings, code, links, quotes.
- Pager: smooth scrolling by line/page, jump to top/bottom, case-insensitive search with inline highlights, percentage status, and responsive reflow on terminal resize.
- Unicode-aware layout: uses POSIX `wcwidth` with your locale for correct column widths (CJK, emoji, combining marks). Tabs expand to spaces.
- TTY-aware: interactive pager when stdout is a TTY; in pipelines it renders the whole document once to stdout.

## Quick Start
- View a file interactively:
  - `build/mdless README.md`
- Pipe content:
  - `cat sample.md | build/mdless`
- Choose a theme:
  - `build/mdless --theme light sample.md`
- Fix the render width (useful for testing):
  - `build/mdless --width 80 sample.md`

Run `build/mdless -h` to see usage in the app.

## Key Bindings
- Move: `j`/Down, `k`/Up, `SPACE` (page down), `b` (page up)
- Jumps: `g` (top), `G` (bottom)
- Search: `/` forward, `?` backward; `n` next, `N` previous (case-insensitive, highlights matches)
- Quit: `q`

A one-line status bar shows the current percent through the document and search hint when active.

## CLI Options
The built-in help shows the full intended surface:
- `--theme dark|light|no-color` (implemented)
- `--width N` (implemented)
- `-h`, `--help` (implemented)

The following are planned but currently stubbed/not fully wired in this build:
- `--wrap on|off`, `--paging auto|always|never`, `--toc`, `--no-syntax`, `--links inline|footnote|hide`, `--tab-width N`

Interactive color/styling is active when a TTY is detected; non-interactive output uses plain text by default.

## Build
Requirements:
- Linux x86_64
- Kotlin/Native compiler (tested with 2.2.10)

Steps:
- Ensure the Kotlin/Native compiler path in `build.sh` (`KOTLINC=.../kotlinc-native`) matches your environment.
- Build the binary:
  - `./build.sh`
- Output: `build/mdless` (the script also normalizes `.kexe` to `mdless`).

## Run Tests
A lightweight test runner compiles and executes module-level tests with Kotlin/Native.
- `./test.sh`

Covers IR modeling, paragraph/list/table layout (including long-word wrapping and tabs), Unicode width helpers, renderer output (and highlight rendering), parser coverage (headings/lists/quotes/fences/tables, including nesting), pager math, search, and basic CLI parsing.

## Samples
- `sample.md` and `sample_unicode.md` exercise typical Markdown and tricky Unicode.
- `comprehensive-test.md` combines headings, lists, quotes, code, rules, tables, Unicode, and tabs.

Try:
- `build/mdless sample.md`
- `build/mdless --theme dark sample_unicode.md`
- `cat comprehensive-test.md | build/mdless`

## How It Works (High Level)
- Parser converts Markdown to a compact IR (blocks/inlines).
- Styler maps IR to themed styles.
- Layout wraps to terminal width using Unicode-aware column widths and expands tabs.
- Pager virtualizes a viewport over laid-out lines and supports search.
- Renderer emits ANSI for the current viewport; non-interactive mode renders the entire document once.

## Current Limitations / Roadmap
- Flags listed above as stubbed are not yet functional (wrap off + horizontal scroll, paging modes, TOC, link rendering modes, tab width override, syntax highlighting).
- Links render as their text; images render alt text only.
- Syntax highlighting for code blocks is not implemented in this build.
- Tested primarily on Linux terminals; no Windows support provided.

## Project Layout
- `src/` Kotlin sources: `parser/`, `ir/`, `style/`, `layout/`, `pager/`, `tty/`, `render/`, `cli/`.
- `build.sh` compiles the app to `build/mdless`.
- `test.sh` compiles and runs focused tests under `test/`.

## License
MIT License — see `LICENSE`.

[^codex]: This project was written by codex-cli.
