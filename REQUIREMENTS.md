# mdless v1 — Requirements

## Goal
- Render Markdown to ANSI-styled text and browse with less-like keys in the terminal, supporting stdin and files, with sensible defaults and good performance.

## MVP Requirements
- Markdown coverage: headings, paragraphs, lists, blockquotes, code (inline/fenced), horizontal rules, links, images (alt text), tables (basic).
- Styling: ANSI colors/bold/underline; light/dark themes; no-color fallback.
- Pager: viewport with word wrap, page/line scroll, search, window resize handling.
- TTY behavior: interactive in TTY; non-interactive pipe outputs rendered text.
- Performance: smooth on files up to several MB; lazy reflow on resize.

## Navigation Keys
- Move: `j/k` or arrows, `SPACE`/`b`, `d/u`, `g`/`G`.
- Search: `/` forward, `?` backward, `n/N` next/prev; highlight matches.
- Horizontal: `h/l` or arrows when wrap is off; `-S` equivalent via flag/key.
- Misc: `q` quit; show status line with percent and current section.

## Rendering
- Headings: levels with size/weight/color difference; optional sticky current heading line.
- Code blocks: monospaced; optional syntax highlighting; preserve indentation.
- Links/Images: render link text; optional footnote-style URL hints; images show alt text only.
- Tables: align columns; degrade gracefully in narrow widths.

## CLI
- Basic: `mdless [FILE]` or `cat README.md | mdless`.
- Flags: `--theme dark|light|no-color`, `--wrap on|off`, `--paging auto|always|never`, `--width N`, `--toc`, `--no-syntax`, `--links inline|footnote|hide`, `--tab-width N`.

## Nice-To-Have (Post-MVP)
- TOC: toggle/jump to sections; breadcrumbs of heading path.
- Open links: `o` to open in browser; copy links to clipboard.
- Marks: `m<char>` set, `' <char>` jump.
- Reload: `R` to reload file on change.
- Search: regex and incremental search with live highlight.

## Platform & Constraints
- Language: Kotlin using Kotlin/Native (no JVM runtime).
- Target OS/Arch: Linux x86_64 on this machine.
- Toolchain: build with Kotlin/Native compiler at `/home/charles-chang/kotlinrun/kotlin-native-prebuilt-linux-x86_64-2.2.10/bin`.
- Output: single native CLI binary named `mdless` runnable from the terminal.
- Runtime deps: standard Linux terminal (ANSI-capable); avoid non-standard daemons or services.
- Terminal compatibility: behave correctly in typical `xterm`/`vt100` environments; provide a no-color mode.
- Performance: responsive on Markdown files up to several megabytes; avoid noticeable input latency.

## Deliverables & Acceptance
- Command `mdless` supports file paths and stdin piping per the CLI section.
- Implements the MVP feature set above, including navigation keys and search.
- Provides `--help` with usage, flags, and examples.
- Includes a simple local build script/instructions using the specified Kotlin/Native toolchain.
