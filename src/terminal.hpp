#ifndef MDLESS_TERMINAL_HPP
#define MDLESS_TERMINAL_HPP

#include <string>
#include <utility>

// ANSI Color codes
namespace Color {
    const std::string Reset      = "\033[0m";
    const std::string Bold       = "\033[1m";
    const std::string Dim        = "\033[2m";
    const std::string Italic     = "\033[3m";
    const std::string Underline  = "\033[4m";
    const std::string Strike     = "\033[9m";
    
    const std::string Black      = "\033[30m";
    const std::string Red        = "\033[31m";
    const std::string Green      = "\033[32m";
    const std::string Yellow     = "\033[33m";
    const std::string Blue       = "\033[34m";
    const std::string Magenta    = "\033[35m";
    const std::string Cyan       = "\033[36m";
    const std::string White      = "\033[37m";
    
    const std::string BgBlack    = "\033[40m";
    const std::string BgRed      = "\033[41m";
    const std::string BgGreen    = "\033[42m";
    const std::string BgYellow   = "\033[43m";
    const std::string BgBlue     = "\033[44m";
    const std::string BgMagenta  = "\033[45m";
    const std::string BgCyan     = "\033[46m";
    const std::string BgWhite    = "\033[47m";
    
    const std::string BrightBlack   = "\033[90m";
    const std::string BrightRed     = "\033[91m";
    const std::string BrightGreen   = "\033[92m";
    const std::string BrightYellow  = "\033[93m";
    const std::string BrightBlue    = "\033[94m";
    const std::string BrightMagenta = "\033[95m";
    const std::string BrightCyan    = "\033[96m";
    const std::string BrightWhite   = "\033[97m";
}

class Terminal {
public:
    Terminal();
    ~Terminal();
    
    // Enable/disable raw mode for immediate key input
    void enableRawMode();
    void disableRawMode();
    
    // Get terminal dimensions
    std::pair<int, int> getSize() const;  // returns (rows, cols)
    int getRows() const;
    int getCols() const;
    
    // Input handling
    int readKey();
    
    // Screen control
    void clearScreen();
    void moveCursor(int row, int col);
    void hideCursor();
    void showCursor();
    
    // Key codes
    static const int KEY_ESCAPE = 27;
    static const int KEY_ENTER  = 13;
    static const int KEY_UP     = 1000;
    static const int KEY_DOWN   = 1001;
    static const int KEY_LEFT   = 1002;
    static const int KEY_RIGHT  = 1003;
    static const int KEY_PAGEUP = 1004;
    static const int KEY_PAGEDOWN = 1005;
    static const int KEY_HOME   = 1006;
    static const int KEY_END    = 1007;
    
private:
    bool rawModeEnabled;
    struct termios* origTermios;
};

#endif // MDLESS_TERMINAL_HPP
