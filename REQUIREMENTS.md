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

## Technical Approach
- Language: Rust preferred (pulldown-cmark for parsing; ratatui/crossterm for TUI; unicode-width/segmentation for correct widths; optional syntect for highlighting). Alternatives: Go (goldmark + tcell) or Python (rich/textual) if speed is secondary.
- Architecture: parse to styled blocks → layout to lines/spans → pager state (viewport, scroll, search) → terminal renderer. Keep renderer separate from pager for testability.
- Testing: golden/snapshot tests for rendering; key-handling unit tests; width and wrapping edge cases.

## Questions
- Language/stack: Rust, Go, or Python?
- OS targets: Linux/macOS only, or include Windows?
- Key subset: Is the proposed less-like set sufficient for v1?
- Syntax highlight: include in MVP or defer?
- Dependencies: any restrictions on third-party libraries?
- Theming: need both light/dark and a no-color mode in MVP?

