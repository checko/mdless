#include "terminal.hpp"
#include <cstdio>
#include <cstdlib>
#include <cstring>

#ifdef _WIN32
#include <windows.h>

// File descriptor constants for write()
#define STDOUT_FILENO 1
#define STDIN_FILENO 0

// Write function wrapper for Windows
static ssize_t write(int fd, const void* buf, size_t count) {
    HANDLE hOut = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD written;
    if (!WriteFile(hOut, buf, (DWORD)count, &written, NULL)) {
        return -1;
    }
    return (ssize_t)written;
}

Terminal::Terminal() 
    : rawModeEnabled(false), vtModeEnabled(false),
      hStdin(GetStdHandle(STD_INPUT_HANDLE)),
      hStdout(GetStdHandle(STD_OUTPUT_HANDLE)) {
}

Terminal::~Terminal() {
    if (rawModeEnabled) {
        disableRawMode();
    }
}

void Terminal::enableRawMode() {
    if (rawModeEnabled) return;
    
    // Get current input mode
    if (!GetConsoleMode(hStdin, &origInputMode)) {
        return;
    }
    
    // Disable echo and line buffering for raw-like behavior. We read input
    // via ReadConsoleInput and dispatch on virtual-key codes directly, so
    // ENABLE_VIRTUAL_TERMINAL_INPUT must stay OFF — otherwise arrow / PgUp /
    // PgDn keys arrive as VT escape sequences instead of KEY_EVENT records.
    DWORD newInputMode = origInputMode;
    newInputMode &= ~ENABLE_ECHO_INPUT;
    newInputMode &= ~ENABLE_LINE_INPUT;
    newInputMode &= ~ENABLE_MOUSE_INPUT;
    newInputMode &= ~ENABLE_VIRTUAL_TERMINAL_INPUT;
    
    if (!SetConsoleMode(hStdin, newInputMode)) {
        return;
    }
    
    // Get current output mode
    if (!GetConsoleMode(hStdout, &origOutputMode)) {
        return;
    }
    
    // Enable virtual terminal processing for ANSI escape sequences
    if (origOutputMode & ENABLE_VIRTUAL_TERMINAL_PROCESSING) {
        vtModeEnabled = true;
    } else {
        // Try to enable it
        DWORD newOutputMode = origOutputMode | ENABLE_VIRTUAL_TERMINAL_PROCESSING;
        if (SetConsoleMode(hStdout, newOutputMode)) {
            vtModeEnabled = true;
        }
    }
    
    rawModeEnabled = true;
}

void Terminal::disableRawMode() {
    if (!rawModeEnabled) return;
    
    // Restore original input mode
    SetConsoleMode(hStdin, origInputMode);
    
    // Restore original output mode
    SetConsoleMode(hStdout, origOutputMode);
    
    rawModeEnabled = false;
    vtModeEnabled = false;
}

std::pair<int, int> Terminal::getSize() const {
    CONSOLE_SCREEN_BUFFER_INFO info;
    
    if (!GetConsoleScreenBufferInfo(hStdout, &info)) {
        return {24, 80};  // Default fallback
    }
    
    // Calculate rows from window size
    int rows = info.srWindow.Bottom - info.srWindow.Top + 1;
    int cols = info.srWindow.Right - info.srWindow.Left + 1;
    
    return {rows, cols};
}

int Terminal::getRows() const {
    return getSize().first;
}

int Terminal::getCols() const {
    return getSize().second;
}

int Terminal::readKey() {
    // Block until we get a usable key-down event. ReadConsoleInput blocks
    // until at least one input record is available, so this loop only
    // spins on uninteresting events (key-up, resize, focus, mouse, etc.).
    while (true) {
        INPUT_RECORD record;
        DWORD eventsRead = 0;
        if (!ReadConsoleInput(hStdin, &record, 1, &eventsRead) || eventsRead == 0) {
            return -1;
        }

        if (record.EventType != KEY_EVENT || !record.Event.KeyEvent.bKeyDown) {
            continue;
        }

        DWORD keyCode = record.Event.KeyEvent.wVirtualKeyCode;
        char charCode = record.Event.KeyEvent.uChar.AsciiChar;

        switch (keyCode) {
            case VK_ESCAPE: return KEY_ESCAPE;
            case VK_RETURN: return KEY_ENTER;
            case VK_UP:     return KEY_UP;
            case VK_DOWN:   return KEY_DOWN;
            case VK_LEFT:   return KEY_LEFT;
            case VK_RIGHT:  return KEY_RIGHT;
            case VK_PRIOR:  return KEY_PAGEUP;
            case VK_NEXT:   return KEY_PAGEDOWN;
            case VK_HOME:   return KEY_HOME;
            case VK_END:    return KEY_END;
            case VK_DELETE: return 127;
        }

        if (charCode != 0) {
            return static_cast<unsigned char>(charCode);
        }
    }
}

void Terminal::clearScreen() {
    // ANSI escape sequence for clear screen
    const char* clear = "\033[2J";
    write(STDOUT_FILENO, clear, 4);
    
    // Move cursor to home position
    const char* home = "\033[H";
    write(STDOUT_FILENO, home, 3);
}

void Terminal::moveCursor(int row, int col) {
    char buf[32];
    snprintf(buf, sizeof(buf), "\033[%d;%dH", row, col);
    write(STDOUT_FILENO, buf, strlen(buf));
}

void Terminal::hideCursor() {
    const char* hide = "\033[?25l";
    write(STDOUT_FILENO, hide, 6);
}

void Terminal::showCursor() {
    const char* show = "\033[?25h";
    write(STDOUT_FILENO, show, 6);
}

#else

// POSIX/Linux implementation
#include <unistd.h>
#include <termios.h>
#include <sys/ioctl.h>

Terminal::Terminal() : rawModeEnabled(false), origTermios(new struct termios) {
}

Terminal::~Terminal() {
    if (rawModeEnabled) {
        disableRawMode();
    }
    delete origTermios;
}

void Terminal::enableRawMode() {
    if (rawModeEnabled) return;
    
    if (tcgetattr(STDIN_FILENO, origTermios) == -1) {
        return;
    }
    
    struct termios raw = *origTermios;
    
    // Input flags: no break, no CR to NL, no parity check, no strip, no XON/XOFF
    raw.c_iflag &= ~(BRKINT | ICRNL | INPCK | ISTRIP | IXON);
    
    // Output flags: disable post processing
    raw.c_oflag &= ~(OPOST);
    
    // Control flags: set 8 bit chars
    raw.c_cflag |= (CS8);
    
    // Local flags: no echo, no canonical mode, no extended, no signal chars
    raw.c_lflag &= ~(ECHO | ICANON | IEXTEN | ISIG);
    
    // Control chars: set return conditions
    raw.c_cc[VMIN] = 0;   // Return as soon as any input is available
    raw.c_cc[VTIME] = 1;  // 100ms timeout
    
    if (tcsetattr(STDIN_FILENO, TCSAFLUSH, &raw) == -1) {
        return;
    }
    
    rawModeEnabled = true;
}

void Terminal::disableRawMode() {
    if (!rawModeEnabled) return;
    
    tcsetattr(STDIN_FILENO, TCSAFLUSH, origTermios);
    rawModeEnabled = false;
}

std::pair<int, int> Terminal::getSize() const {
    struct winsize ws;
    
    if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws) == -1 || ws.ws_col == 0) {
        return {24, 80};  // Default fallback
    }
    
    return {ws.ws_row, ws.ws_col};
}

int Terminal::getRows() const {
    return getSize().first;
}

int Terminal::getCols() const {
    return getSize().second;
}

int Terminal::readKey() {
    int nread;
    char c;
    
    while ((nread = read(STDIN_FILENO, &c, 1)) != 1) {
        if (nread == -1) return -1;
    }
    
    // Handle escape sequences
    if (c == '\033') {
        char seq[3];
        
        if (read(STDIN_FILENO, &seq[0], 1) != 1) return KEY_ESCAPE;
        if (read(STDIN_FILENO, &seq[1], 1) != 1) return KEY_ESCAPE;
        
        if (seq[0] == '[') {
            if (seq[1] >= '0' && seq[1] <= '9') {
                if (read(STDIN_FILENO, &seq[2], 1) != 1) return KEY_ESCAPE;
                if (seq[2] == '~') {
                    switch (seq[1]) {
                        case '1': return KEY_HOME;
                        case '3': return KEY_END;  // Delete key, but we'll use as END
                        case '4': return KEY_END;
                        case '5': return KEY_PAGEUP;
                        case '6': return KEY_PAGEDOWN;
                        case '7': return KEY_HOME;
                        case '8': return KEY_END;
                    }
                }
            } else {
                switch (seq[1]) {
                    case 'A': return KEY_UP;
                    case 'B': return KEY_DOWN;
                    case 'C': return KEY_RIGHT;
                    case 'D': return KEY_LEFT;
                    case 'H': return KEY_HOME;
                    case 'F': return KEY_END;
                }
            }
        } else if (seq[0] == 'O') {
            switch (seq[1]) {
                case 'H': return KEY_HOME;
                case 'F': return KEY_END;
            }
        }
        
        return KEY_ESCAPE;
    }
    
    return c;
}

void Terminal::clearScreen() {
    write(STDOUT_FILENO, "\033[2J", 4);
    write(STDOUT_FILENO, "\033[H", 3);
}

void Terminal::moveCursor(int row, int col) {
    char buf[32];
    snprintf(buf, sizeof(buf), "\033[%d;%dH", row, col);
    write(STDOUT_FILENO, buf, strlen(buf));
}

void Terminal::hideCursor() {
    write(STDOUT_FILENO, "\033[?25l", 6);
}

void Terminal::showCursor() {
    write(STDOUT_FILENO, "\033[?25h", 6);
}

#endif // _WIN32
