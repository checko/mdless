# Markdown Viewer

This is a simple markdown viewer application for the terminal.

## Features

- Scroll through markdown files using keyboard navigation
- Similar to the `less` command
- Supports common markdown elements:
  - Headers
  - Lists
  - Code blocks
  - Emphasis

## Usage

To use the viewer, simply run:

```
./markdown-viewer.kexe your-file.md
```

## Keyboard Controls

- `j` or `↓` - Move down one line
- `k` or `↑` - Move up one line
- `d` - Move down half a page
- `u` - Move up half a page
- `f` or `Space` - Move down a full page
- `b` - Move up a full page
- `g` - Go to the beginning of the file
- `G` - Go to the end of the file
- `q` - Quit the viewer

## Example Code Block

Here's an example of a code block:

```kotlin
fun main() {
    println("Hello, World!")
}
```

## Lists

1. First item
2. Second item
3. Third item

- Unordered item 1
- Unordered item 2
- Unordered item 3

## Emphasis

*This text is italic*

**This text is bold**

***This text is both italic and bold***