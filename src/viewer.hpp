#ifndef MDLESS_VIEWER_HPP
#define MDLESS_VIEWER_HPP

#include <string>
#include <vector>
#include "terminal.hpp"
#include "markdown.hpp"

class Viewer {
public:
    Viewer(Terminal& terminal);
    
    // Load and render a file
    bool loadFile(const std::string& filename);
    
    // Main viewing loop
    void run();
    
private:
    Terminal& term;
    MarkdownRenderer renderer;
    
    std::string rawContent;           // Raw file content for re-rendering on resize
    std::vector<std::string> renderedLines;
    std::string filename;
    
    int scrollOffset;
    int viewHeight;
    int viewWidth;
    int lastWrapWidth;                // Width used for last wrap, to detect resize
    
    // Search state
    std::string searchPattern;
    std::vector<int> searchMatches;
    int currentMatch;
    
    // Display methods
    void refresh();
    void drawStatusBar();
    void drawHelpScreen();
    void rerenderAndWrap();
    std::vector<std::string> wrapLine(const std::string& str, int maxWidth);
    
    // Navigation
    void scrollDown(int lines = 1);
    void scrollUp(int lines = 1);
    void scrollToTop();
    void scrollToBottom();
    void pageDown();
    void pageUp();
    
    // Search
    void promptSearch();
    void findNext();
    void findPrevious();
    void performSearch();
};

#endif // MDLESS_VIEWER_HPP
