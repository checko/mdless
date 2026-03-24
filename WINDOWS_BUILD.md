# Building mdless on Windows

This guide explains how to build the mdless Markdown viewer on Windows systems using free tools.

## Prerequisites

### Option 1: Using winlibs MinGW-w64 (Recommended)
Download and install from https://winlibs.com/:
- **winlibs personal build** with GCC + LLVM/Clang
- Includes: GCC 14.2.0, LLVM/Clang 18.1.8, CMake 3.30.2, GNU Make 4.4.1

Example installation path: `D:\mingw64\`

### Option 2: Using MSYS2
1. Download MSYS2 from https://www.msys2.org/
2. Open MSYS2 MinGW64 terminal
3. Install packages:
   ```bash
   pacman -Syu
   pacman -S mingw-w64-x86_64-gcc
   pacman -S mingw-w64-x86_64-cmake
   pacman -S mingw-w64-x86_64-make
   ```

### Option 3: Using Visual Studio Community + CMake
1. Install Visual Studio Community 2022 (free)
2. Install CMake from https://cmake.org/download/

## Building

### Using winlibs MinGW-w64 (D:\mingw64 example)

```bash
cd mdless
mkdir build
cd build

# Configure with MinGW toolchain
cmake .. -G "MinGW Makefiles" ^
  -DCMAKE_CXX_COMPILER=D:/mingw64/bin/clang++.exe ^
  -DCMAKE_MAKE_PROGRAM=D:/mingw64/bin/mingw32-make.exe

# Build
mingw32-make.exe
```

### Using MSYS2

```bash
cd mdless
mkdir build
cd build

# Configure
cmake .. -G "MinGW Makefiles"

# Build
make
```

### Using Visual Studio

```bash
cd mdless
mkdir build
cd build

# Configure for Visual Studio 17 2022
cmake .. -G "Visual Studio 17 2022"

# Build
cmake --build . --config Release
```

## Dependencies

The project requires:
- C++17 compatible compiler (Clang, GCC, or MSVC)
- CMake 3.14+
- No external dependencies (pure C++)

## Running

After building, copy **all files** from the build directory to your target location:
- `mdless.exe` (executable)
- `libgcc_s_seh-1.dll` (MinGW runtime)
- `libstdc++-6.dll` (MinGW C++ runtime)

```bash
# Build first, then:
./mdless.exe README.md
```

### Navigation Controls
| Key | Action |
|-----|--------|
| `j` / `↓` | Scroll down |
| `k` / `↑` | Scroll up |
| `Space` | Page down |
| `b` | Page up |
| `g` | Go to top |
| `G` | Go to bottom |
| `/pattern` | Search |
| `n` / `N` | Next/previous match |
| `q` | Quit |
| `h` | Help |

## Windows Requirements

- **Windows 10+** (for ANSI escape sequence support)
- Terminal must support VT100/ANSI escape codes:
  - Windows Terminal (recommended)
  - cmd.exe with VirtualTerminalLevel enabled
  - PowerShell

## Troubleshooting

### "DLL not found" error
- Copy all files from build directory: `mdless.exe`, `libgcc_s_seh-1.dll`, `libstdc++-6.dll`
- The DLLs must be in the same folder as `mdless.exe` when you run it

### "Cannot open file" error
- Ensure you're in the correct directory
- Use full path if needed: `./mdless.exe C:/path/to/file.md`

### ANSI codes not working
- Ensure your terminal supports ANSI escape sequences
- Windows 10 users: Enable VirtualTerminalLevel if using cmd.exe

### Build errors
- Ensure CMake 3.14+ is installed
- Check compiler path is in `%PATH%` or use full paths
- For winlibs: use `mingw32-make.exe`, not `make`
