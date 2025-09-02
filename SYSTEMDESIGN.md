# mdless — System Design

## Overview
mdless is a terminal Markdown viewer. It renders Markdown to ANSI-styled text and provides less-like navigation, search, and resize-aware wrapping. The design prioritizes responsiveness, correct text layout for Unicode, and a clean separation between parsing, layout, state, and terminal I/O.

## Core Flow Options

1) Pre-render to temporary file (/tmp) then page it
- Description: Parse Markdown → render complete styled text to a temporary file under `/tmp` → feed that file to an internal pager-like UI.
- Pros: Simple mental model; easy to snapshot-test by diffing the temp file; decouples writer from UI; low memory footprint for huge inputs (OS page cache handles it).
- Cons: Loses semantic structure needed for dynamic features (TOC jumps, per-element focus, link hints); costly to support window-resize reflow (must re-render entire file and re-seek); search match highlighting requires a second pass or re-writing the file; harder to implement theme toggles without re-render; disk I/O on every reflow; difficult to map key actions back to source blocks.
- When to choose: Minimal interactive needs, fixed width, or when implementation time is extremely constrained.

2) In-memory IR with on-demand layout (Recommended)
- Description: Parse Markdown into a compact intermediate representation (IR) of blocks/inlines. Lazily layout blocks into terminal-width lines on demand. Cache layout and invalidate on window resize or theme change. Render directly to the terminal from the layout slice that fits the viewport.
- Pros: Clean separation of concerns; instant reflow on resize; precise search highlighting; easy TOC jumps; theming toggles without re-parsing; enables virtualization (only layout visible regions) → great performance on large files; straightforward unit and snapshot tests at each layer.
- Cons: More moving parts than pre-render; requires careful Unicode width handling and efficient caching; implementation complexity is moderate.
- When to choose: You want responsiveness, correctness, and room for advanced features.

The remainder of this document details Option 2 and its module boundaries.

## Architecture (Option 2)

- Parser: Converts Markdown text into a simplified IR (Blocks and Inlines), normalizing whitespace, lists, code blocks, blockquotes, tables, and links/images (alt only).
- IR: A compact, immutable representation with stable IDs for blocks, plus inline spans carrying semantic roles and base styles.
- Styler: Maps IR nodes to themed styles (colors, bold, underline, dim) based on light/dark/no-color themes.
- Layout: Wraps styled inlines into lines for a given width; computes visual columns using Unicode-aware width; caches per-block layout keyed by (blockId, width, tabWidth, theme-mode minimal key).
- Pager State: Tracks viewport (top line index, height, width), search state, and current section; exposes commands for scrolling and jumping.
- Renderer: Emits ANSI sequences to paint the current viewport; minimizes updates by diffing previously rendered lines when feasible.
- TTY/IO: Handles raw mode, resize signals, and key input; abstracts terminal so renderer is platform-agnostic.

## Data Model (IR)

- Block(id, kind, meta, inlines?)
  - kind: Heading(level), Paragraph, List(ordered|unordered, items), Blockquote(children), CodeBlock(language, text), ThematicBreak, Table(rows, aligns), RawHtml (ignored or downgraded), Image(alt)
  - meta: source range, numbering, etc.
- Inline(kind, text?, children?, target?)
  - kinds: Text, Emph, Strong, Code, Link(text, url), SoftBreak, HardBreak
- StyledSpan(text, style)
  - style: color, bold, underline; computed by Styler from IR + theme

IR is immutable after creation to simplify caching and testing.

## Layout

- Input: [Block] + theme + width + tabWidth
- Output: [LayoutLine]
  - LayoutLine(spans: [StyledSpan], blockId, rowInBlock)
- Wrapping: Compute printable width using Unicode width rules; respect code block monospacing and preserved indentation; support table alignment by precomputing column widths per table block.
- Caching: Per block, memoize layout results by (width, theme key, tabWidth). Invalidate only impacted blocks on resize/theme change.
- Virtualization: Compute only the lines needed for [topLine, topLine+height+margin]. Prefetch a small margin for smooth scroll.

## Pager & Input

- Commands: j/k, arrows, d/u, SPACE/b, g/G, / ? n/N, h/l (wrap off), q.
- Search: Maintain a forward index of normalized text per block; match ranges are mapped back to LayoutLine spans for highlight.
- Status line: Percent through document and current section (nearest heading).
- Resize: On SIGWINCH, recompute viewport width and reflow via layout cache; preserve logical top line by mapping block/row pairs.

## Rendering

- Stateless renderer consumes a slice of LayoutLine and writes ANSI sequences.
- Minimize flicker: Write complete lines; clear rest-of-line; avoid full-screen clears when possible.
- No-color mode: styles collapse to plain text with minimal escape codes.

## Testing Strategy

- Parser: Golden tests mapping Markdown → IR.
- Layout: Snapshot tests given IR + width → LayoutLines; edge cases for tabs, East Asian widths, combining marks.
- Pager: Unit tests for scroll math and jump targets.
- Renderer: Snapshot ANSI outputs with a virtual terminal sink.
- End-to-end: Sample Markdown fixtures exercised via a headless driver using scripted key events.

## Trade-off: Temp File Pre-render vs IR

- Simplicity vs capability: temp-file is simpler but limits interactivity and resize-aware layout; IR enables dynamic features with modest extra complexity.
- Testability: both are testable; IR allows targeted unit tests for parser/layout/pager and fast golden tests, while temp-file favors only end-to-end snapshots.
- Performance: IR with virtualization avoids touching the whole document on each frame; temp-file forces whole-document writes for many operations.

