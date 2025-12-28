# mdless

A console Markdown viewer with `less`-like navigation. Renders Markdown files with
syntax highlighting and formatting using ANSI escape codes.

## Features

- **Markdown Rendering**: Headers, bold, italic, code blocks, lists, links, blockquotes
- **Image Placeholders**: Non-displayable content shown as `[IMAGE: alt → url]`
- **less-like Navigation**: Familiar keyboard shortcuts for scrolling and searching
- **No Dependencies**: Pure C++17, works on Linux and macOS

## Build

```bash
mkdir build && cd build
cmake ..
make
```

## Usage

```bash
./mdless README.md
```

### Key Bindings

| Key | Action |
|-----|--------|
| `j` / `↓` | Scroll down |
| `k` / `↑` | Scroll up |
| `Space` | Page down |
| `b` | Page up |
| `g` | Go to top |
| `G` | Go to bottom |
| `/pattern` | Search |
| `n` / `N` | Next/prev match |
| `q` | Quit |
| `h` | Help |

## License

MIT License - see [LICENSE](LICENSE)
