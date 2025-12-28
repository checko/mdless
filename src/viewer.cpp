#include "viewer.hpp"
#include <fstream>
#include <sstream>
#include <iostream>
#include <algorithm>

Viewer::Viewer(Terminal& terminal) 
    : term(terminal), scrollOffset(0), viewHeight(24), viewWidth(80),
      currentMatch(-1) {
}

bool Viewer::loadFile(const std::string& fname) {
    filename = fname;
    
    std::ifstream file(filename);
    if (!file.is_open()) {
        return false;
    }
    
    std::stringstream buffer;
    buffer << file.rdbuf();
    std::string content = buffer.str();
    file.close();
    
    auto [rows, cols] = term.getSize();
    viewHeight = rows - 1;  // Leave room for status bar
    viewWidth = cols;
    
    renderedLines = renderer.render(content, viewWidth);
    
    return true;
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

void Viewer::refresh() {
    auto [rows, cols] = term.getSize();
    viewHeight = rows - 1;
    viewWidth = cols;
    
    term.moveCursor(1, 1);
    
    // Draw visible lines
    for (int i = 0; i < viewHeight; ++i) {
        int lineIdx = scrollOffset + i;
        
        // Clear line
        std::cout << "\033[K";
        
        if (lineIdx < static_cast<int>(renderedLines.size())) {
            std::cout << renderedLines[lineIdx];
        }
        
        if (i < viewHeight - 1) {
            std::cout << "\r\n";
        }
    }
    
    drawStatusBar();
    std::cout.flush();
}

void Viewer::drawStatusBar() {
    auto [rows, cols] = term.getSize();
    term.moveCursor(rows, 1);
    
    // Inverse colors for status bar
    std::cout << "\033[7m";
    
    // Left side: filename
    std::string left = " " + filename;
    
    // Right side: position info
    int totalLines = renderedLines.size();
    int currentLine = scrollOffset + 1;
    int percent = totalLines > 0 ? (currentLine * 100 / totalLines) : 100;
    
    std::string right;
    if (scrollOffset == 0 && totalLines <= viewHeight) {
        right = " All ";
    } else if (scrollOffset == 0) {
        right = " Top ";
    } else if (scrollOffset + viewHeight >= totalLines) {
        right = " End ";
    } else {
        right = " " + std::to_string(percent) + "% ";
    }
    
    // Add search info if searching
    if (!searchPattern.empty()) {
        right = " /" + searchPattern + " " + right;
    }
    
    // Fill the middle with spaces
    int padding = cols - left.length() - right.length();
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
