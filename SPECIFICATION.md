# mdless - Software Specification

## 1. Overview

**mdless** is a C++ console application that renders Markdown files in the terminal with formatting preserved, providing a `less`-like interactive viewing experience.

## 2. Goals

- Render Markdown files with visual formatting using ANSI escape codes
- Provide intuitive navigation similar to the `less` command
- Handle non-displayable content (images, videos) with placeholder markers
- Support common Markdown syntax elements
- Cross-platform compatibility (Linux, macOS, Windows 10+)

## 3. Functional Requirements

### 3.1 Markdown Parsing

The application shall support the following Markdown elements:

| Element | Rendering |
|---------|-----------|
| Headers (h1-h6) | Bold + color hierarchy (h1 = magenta, h2 = cyan, etc.) |
| **Bold** | Bold text (ANSI code 1) |
| *Italic* | Italic/dimmed text (ANSI code 3) |
| ~~Strikethrough~~ | Strikethrough (ANSI code 9) |
| `inline code` | Highlighted background |
| Code blocks | Indented with syntax highlight color |
| Lists (ordered/unordered) | Bullet/number with proper indentation |
| [Links](url) | Underlined blue text with URL shown |
| ![Images](url) | Placeholder: `[IMAGE: alt-text → url]` |
| > Blockquotes | Indented with vertical bar |
| Horizontal rules | Line of dashes across terminal width |
| Tables | ASCII-formatted tables |

### 3.2 Navigation Controls

| Key | Action |
|-----|--------|
| `j` / `↓` / `Enter` | Scroll down one line |
| `k` / `↑` | Scroll up one line |
| `Space` / `Page Down` | Scroll down one page |
| `b` / `Page Up` | Scroll up one page |
| `g` | Go to beginning |
| `G` | Go to end |
| `/pattern` | Search forward |
| `n` | Next search result |
| `N` | Previous search result |
| `q` | Quit |
| `h` | Show help |

### 3.3 Terminal Handling

- Detect terminal dimensions (rows × columns)
- Handle terminal resize events (SIGWINCH)
- Raw mode input for immediate key response
- Proper cleanup on exit (restore terminal state)

### 3.4 Non-Text Content Handling

Content that cannot be displayed in a terminal shall be shown as:
- **Images**: `[IMAGE: alt-text → url]` in cyan
- **Videos**: `[VIDEO: description → url]` in cyan
- **Other embeds**: `[EMBED: type → url]` in cyan

## 4. Non-Functional Requirements

### 4.1 Performance
- Load files up to 10MB efficiently
- Smooth scrolling with no perceptible lag
- Memory-efficient line buffering

### 4.2 Compatibility
- C++17 standard
- Linux: Full support  
- macOS: Full support
- Windows 10+: Support via Virtual Terminal Processing

### 4.3 Dependencies
- Standard C++ library only (no external dependencies)
- POSIX APIs for terminal control (termios.h)
- Windows Console API for Windows builds

## 5. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        main.cpp                              │
│                    (Entry point, CLI)                       │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  markdown.hpp   │  │  terminal.hpp   │  │   viewer.hpp    │
│  markdown.cpp   │  │  terminal.cpp   │  │   viewer.cpp    │
│                 │  │                 │  │                 │
│  - Parse MD     │  │  - Raw mode     │  │  - Navigation   │
│  - Render ANSI  │  │  - Key input    │  │  - Display      │
│                 │  │  - Screen size  │  │  - Search       │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

## 6. File Structure

```
mdless/
├── CMakeLists.txt
├── README.md
├── LICENSE
├── SPECIFICATION.md
├── .gitignore
└── src/
    ├── main.cpp
    ├── markdown.hpp
    ├── markdown.cpp
    ├── terminal.hpp
    ├── terminal.cpp
    ├── viewer.hpp
    └── viewer.cpp
```

## 7. Command Line Interface

```
Usage: mdless [OPTIONS] <file.md>

Options:
  -h, --help      Show help message
  -v, --version   Show version information
  -n, --no-color  Disable colored output
```

## 8. Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | File not found |
| 2 | Unable to read file |
| 3 | Invalid arguments |

## 9. Future Enhancements (Out of Scope v1.0)

- Syntax highlighting for code blocks (language-aware)
- Mouse scroll support
- Bookmark support
- Multiple file viewing
- Configuration file (.mdlessrc)
