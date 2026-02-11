#include "viewer.hpp"
#include <fstream>
#include <sstream>
#include <iostream>
#include <algorithm>
#include <cstdint>

Viewer::Viewer(Terminal& terminal) 
    : term(terminal), scrollOffset(0), viewHeight(24), viewWidth(80),
      lastWrapWidth(0), currentMatch(-1) {
}

bool Viewer::loadFile(const std::string& fname) {
    filename = fname;
    
    std::ifstream file(filename);
    if (!file.is_open()) {
        return false;
    }
    
    std::stringstream buffer;
    buffer << file.rdbuf();
    rawContent = buffer.str();
    file.close();
    
    auto [rows, cols] = term.getSize();
    viewHeight = rows - 1;  // Leave room for status bar
    viewWidth = cols;
    
    rerenderAndWrap();
    
    return true;
}

void Viewer::rerenderAndWrap() {
    auto rawLines = renderer.render(rawContent, viewWidth);
    
    // Wrap each rendered line to fit within terminal width
    renderedLines.clear();
    for (const auto& line : rawLines) {
        auto wrapped = wrapLine(line, viewWidth);
        for (const auto& wl : wrapped) {
            renderedLines.push_back(wl);
        }
    }
    lastWrapWidth = viewWidth;
}

void Viewer::run() {
    term.enableRawMode();
    term.hideCursor();
    
    refresh();
    
    bool running = true;
    while (running) {
        int key = term.readKey();
        
        switch (key) {
            // Quit
            case 'q':
            case 'Q':
                running = false;
                break;
            
            // Scroll down
            case 'j':
            case Terminal::KEY_DOWN:
            case '\r':
            case '\n':
                scrollDown();
                break;
            
            // Scroll up
            case 'k':
            case Terminal::KEY_UP:
                scrollUp();
                break;
            
            // Page down
            case ' ':
            case Terminal::KEY_PAGEDOWN:
                pageDown();
                break;
            
            // Page up
            case 'b':
            case Terminal::KEY_PAGEUP:
                pageUp();
                break;
            
            // Go to top
            case 'g':
            case Terminal::KEY_HOME:
                scrollToTop();
                break;
            
            // Go to bottom
            case 'G':
            case Terminal::KEY_END:
                scrollToBottom();
                break;
            
            // Search
            case '/':
                promptSearch();
                break;
            
            // Next match
            case 'n':
                findNext();
                break;
            
            // Previous match
            case 'N':
                findPrevious();
                break;
            
            // Help
            case 'h':
            case '?':
                drawHelpScreen();
                break;
            
            default:
                break;
        }
        
        if (running) {
            refresh();
        }
    }
    
    term.clearScreen();
    term.showCursor();
    term.disableRawMode();
}

// Determine the display width of a Unicode code point in a terminal.
// CJK ideographs, fullwidth forms, and certain other characters occupy 2 columns.
static int charDisplayWidth(uint32_t codepoint) {
    // CJK Unified Ideographs and extensions
    if ((codepoint >= 0x4E00 && codepoint <= 0x9FFF) ||   // CJK Unified Ideographs
        (codepoint >= 0x3400 && codepoint <= 0x4DBF) ||   // CJK Extension A
        (codepoint >= 0x20000 && codepoint <= 0x2A6DF) || // CJK Extension B
        (codepoint >= 0x2A700 && codepoint <= 0x2B73F) || // CJK Extension C
        (codepoint >= 0x2B740 && codepoint <= 0x2B81F) || // CJK Extension D
        (codepoint >= 0xF900 && codepoint <= 0xFAFF) ||   // CJK Compatibility Ideographs
        // Fullwidth Forms
        (codepoint >= 0xFF01 && codepoint <= 0xFF60) ||
        (codepoint >= 0xFFE0 && codepoint <= 0xFFE6) ||
        // CJK Symbols and Punctuation, Hiragana, Katakana
        (codepoint >= 0x3000 && codepoint <= 0x303F) ||
        (codepoint >= 0x3040 && codepoint <= 0x309F) ||
        (codepoint >= 0x30A0 && codepoint <= 0x30FF) ||
        // Hangul
        (codepoint >= 0xAC00 && codepoint <= 0xD7AF) ||
        // Enclosed CJK
        (codepoint >= 0x3200 && codepoint <= 0x32FF) ||
        (codepoint >= 0xFE30 && codepoint <= 0xFE4F) ||   // CJK Compatibility Forms
        // Bopomofo
        (codepoint >= 0x3100 && codepoint <= 0x312F) ||
        // Box Drawing and Block Elements (used in this app for table borders)
        (codepoint >= 0x2500 && codepoint <= 0x257F) ||   // Box Drawing - actually single width
        (codepoint >= 0x2580 && codepoint <= 0x259F)) {   // Block Elements - actually single width
        // Box drawing and block elements are single width in most terminals
        if (codepoint >= 0x2500 && codepoint <= 0x259F) return 1;
        return 2;
    }
    return 1;
}

// Wrap a single rendered line (with ANSI codes) into multiple lines that fit within maxWidth columns.
// Properly handles multi-byte UTF-8 and CJK double-width characters.
// Carries active ANSI styles forward to continuation lines.
std::vector<std::string> Viewer::wrapLine(const std::string& str, int maxWidth) {
    std::vector<std::string> result;
    if (maxWidth <= 0) maxWidth = 1;
    
    std::string currentLine;
    int visibleWidth = 0;
    // Track active ANSI escape codes so continuation lines inherit styles
    std::string activeAnsi;
    
    for (size_t i = 0; i < str.length(); ) {
        unsigned char ch = static_cast<unsigned char>(str[i]);
        
        if (ch == '\033') {
            // Start of ANSI escape sequence - accumulate it
            std::string escSeq;
            escSeq += str[i];
            ++i;
            while (i < str.length()) {
                escSeq += str[i];
                if (str[i] == 'm') {
                    ++i;
                    break;
                }
                ++i;
            }
            currentLine += escSeq;
            // Track the active style: reset clears it, else append
            if (escSeq == "\033[0m") {
                activeAnsi.clear();
            } else {
                activeAnsi += escSeq;
            }
        } else {
            // Decode UTF-8 to get the codepoint and byte length
            uint32_t codepoint = 0;
            int byteLen = 1;
            
            if (ch < 0x80) {
                codepoint = ch;
                byteLen = 1;
            } else if ((ch & 0xE0) == 0xC0) {
                codepoint = ch & 0x1F;
                byteLen = 2;
            } else if ((ch & 0xF0) == 0xE0) {
                codepoint = ch & 0x0F;
                byteLen = 3;
            } else if ((ch & 0xF8) == 0xF0) {
                codepoint = ch & 0x07;
                byteLen = 4;
            }
            
            // Read continuation bytes
            for (int j = 1; j < byteLen && (i + j) < str.length(); ++j) {
                codepoint = (codepoint << 6) | (static_cast<unsigned char>(str[i + j]) & 0x3F);
            }
            
            int charWidth = charDisplayWidth(codepoint);
            
            // If adding this character would exceed width, start a new line
            if (visibleWidth + charWidth > maxWidth) {
                // Close styles on this line
                if (!activeAnsi.empty()) {
                    currentLine += "\033[0m";
                }
                result.push_back(currentLine);
                // Start new line with the carried-over style
                currentLine = activeAnsi;
                visibleWidth = 0;
            }
            
            // Copy all bytes of this character
            for (int j = 0; j < byteLen && (i + j) < str.length(); ++j) {
                currentLine += str[i + j];
            }
            visibleWidth += charWidth;
            i += byteLen;
        }
    }
    
    // Push the last line (even if empty, to preserve blank lines)
    result.push_back(currentLine);
    
    return result;
}

void Viewer::refresh() {
    auto [rows, cols] = term.getSize();
    viewHeight = rows - 1;
    viewWidth = cols;
    
    // Re-wrap if terminal width changed
    if (viewWidth != lastWrapWidth) {
        rerenderAndWrap();
    }
    
    // Move to top and clear screen
    std::cout << "\033[H";  // Move to home position
    
    // Draw visible lines (already wrapped to fit terminal width)
    for (int i = 0; i < viewHeight; ++i) {
        int lineIdx = scrollOffset + i;
        
        // Clear line first
        std::cout << "\033[2K";  // Clear entire line
        
        if (lineIdx < static_cast<int>(renderedLines.size())) {
            std::cout << renderedLines[lineIdx];
        }
        
        std::cout << "\r\n";
    }
    
    drawStatusBar();
    std::cout.flush();
}

void Viewer::drawStatusBar() {
    auto [rows, cols] = term.getSize();
    term.moveCursor(rows, 1);
    
    // Clear line and set inverse colors for status bar
    std::cout << "\033[2K\033[7m";
    
    // Left side: filename
    std::string left = " " + filename;
    
    // Right side: position info
    int totalLines = renderedLines.size();
    int currentLine = scrollOffset + 1;
    int percent = totalLines > 0 ? (currentLine * 100 / totalLines) : 100;
    
    std::string right;
    if (scrollOffset == 0 && totalLines <= viewHeight) {
        right = "All ";
    } else if (scrollOffset == 0) {
        right = "Top ";
    } else if (scrollOffset + viewHeight >= totalLines) {
        right = "End ";
    } else {
        right = std::to_string(percent) + "% ";
    }
    
    // Add search info if searching
    if (!searchPattern.empty()) {
        right = "/" + searchPattern + " " + right;
    }
    
    // Calculate padding - fill the entire line
    int padding = cols - static_cast<int>(left.length()) - static_cast<int>(right.length());
    if (padding < 0) padding = 0;
    
    std::cout << left << std::string(padding, ' ') << right;
    std::cout << "\033[0m";
}

void Viewer::drawHelpScreen() {
    term.clearScreen();
    term.moveCursor(1, 1);
    
    std::cout << Color::Bold << Color::BrightCyan << "mdless - Markdown Viewer Help" << Color::Reset << "\n\n";
    
    std::cout << Color::Bold << "Navigation:" << Color::Reset << "\n";
    std::cout << "  j/↓/Enter    Scroll down one line\n";
    std::cout << "  k/↑          Scroll up one line\n";
    std::cout << "  Space/PgDn   Scroll down one page\n";
    std::cout << "  b/PgUp       Scroll up one page\n";
    std::cout << "  g/Home       Go to beginning\n";
    std::cout << "  G/End        Go to end\n\n";
    
    std::cout << Color::Bold << "Search:" << Color::Reset << "\n";
    std::cout << "  /pattern     Search forward\n";
    std::cout << "  n            Next match\n";
    std::cout << "  N            Previous match\n\n";
    
    std::cout << Color::Bold << "Other:" << Color::Reset << "\n";
    std::cout << "  h/?          Show this help\n";
    std::cout << "  q            Quit\n\n";
    
    std::cout << Color::BrightBlack << "Press any key to continue..." << Color::Reset;
    std::cout.flush();
    
    term.readKey();
}

void Viewer::scrollDown(int lines) {
    int maxOffset = std::max(0, static_cast<int>(renderedLines.size()) - viewHeight);
    scrollOffset = std::min(scrollOffset + lines, maxOffset);
}

void Viewer::scrollUp(int lines) {
    scrollOffset = std::max(0, scrollOffset - lines);
}

void Viewer::scrollToTop() {
    scrollOffset = 0;
}

void Viewer::scrollToBottom() {
    scrollOffset = std::max(0, static_cast<int>(renderedLines.size()) - viewHeight);
}

void Viewer::pageDown() {
    scrollDown(viewHeight - 1);
}

void Viewer::pageUp() {
    scrollUp(viewHeight - 1);
}

void Viewer::promptSearch() {
    auto [rows, cols] = term.getSize();
    term.moveCursor(rows, 1);
    term.showCursor();
    
    std::cout << "\033[K/";
    std::cout.flush();
    
    // Read search pattern character by character
    std::string pattern;
    while (true) {
        int ch = term.readKey();
        
        if (ch == '\r' || ch == '\n') {
            break;
        } else if (ch == 27) {  // Escape
            pattern.clear();
            break;
        } else if (ch == 127 || ch == 8) {  // Backspace
            if (!pattern.empty()) {
                pattern.pop_back();
                std::cout << "\b \b";
                std::cout.flush();
            }
        } else if (ch >= 32 && ch < 127) {
            pattern += static_cast<char>(ch);
            std::cout << static_cast<char>(ch);
            std::cout.flush();
        }
    }
    
    term.hideCursor();
    
    if (!pattern.empty()) {
        searchPattern = pattern;
        performSearch();
        if (!searchMatches.empty()) {
            scrollOffset = searchMatches[0];
            currentMatch = 0;
        }
    }
}

void Viewer::performSearch() {
    searchMatches.clear();
    currentMatch = -1;
    
    for (size_t i = 0; i < renderedLines.size(); ++i) {
        // Simple substring search (case-insensitive would be better)
        if (renderedLines[i].find(searchPattern) != std::string::npos) {
            searchMatches.push_back(i);
        }
    }
}

void Viewer::findNext() {
    if (searchMatches.empty()) return;
    
    if (currentMatch < 0) {
        currentMatch = 0;
    } else {
        currentMatch = (currentMatch + 1) % searchMatches.size();
    }
    
    scrollOffset = searchMatches[currentMatch];
}

void Viewer::findPrevious() {
    if (searchMatches.empty()) return;
    
    if (currentMatch < 0) {
        currentMatch = searchMatches.size() - 1;
    } else {
        currentMatch = (currentMatch - 1 + searchMatches.size()) % searchMatches.size();
    }
    
    scrollOffset = searchMatches[currentMatch];
}
