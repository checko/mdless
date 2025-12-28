#include "terminal.hpp"
#include <unistd.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>

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
