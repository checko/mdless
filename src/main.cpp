#include <iostream>
#include <string>
#include <cstring>
#include "terminal.hpp"
#include "viewer.hpp"

#ifdef _WIN32
#include <windows.h>
#endif

const char* VERSION = "1.0.0";

#ifdef _WIN32
void initWindowsConsole() {
    // Set console output code page to UTF-8
    SetConsoleOutputCP(CP_UTF8);
    
    // Enable virtual terminal processing for ANSI escape sequences
    HANDLE hOut = GetStdHandle(STD_OUTPUT_HANDLE);
    if (hOut != INVALID_HANDLE_VALUE) {
        DWORD dwMode = 0;
        if (GetConsoleMode(hOut, &dwMode)) {
            dwMode |= ENABLE_VIRTUAL_TERMINAL_PROCESSING;
            SetConsoleMode(hOut, dwMode);
        }
    }
}
#endif

void printUsage(const char* progName) {
    std::cout << "Usage: " << progName << " [OPTIONS] <file.md>\n\n";
    std::cout << "A console Markdown viewer with less-like navigation.\n\n";
    std::cout << "Options:\n";
    std::cout << "  -h, --help      Show this help message\n";
    std::cout << "  -v, --version   Show version information\n\n";
    std::cout << "Navigation:\n";
    std::cout << "  j/↓/Enter    Scroll down\n";
    std::cout << "  k/↑          Scroll up\n";
    std::cout << "  Space/b      Page down/up\n";
    std::cout << "  g/G          Go to top/bottom\n";
    std::cout << "  /pattern     Search\n";
    std::cout << "  n/N          Next/prev match\n";
    std::cout << "  q            Quit\n";
    std::cout << "  h            Help\n";
}

void printVersion() {
    std::cout << "mdless version " << VERSION << "\n";
    std::cout << "A console Markdown viewer with ANSI formatting.\n";
}

int main(int argc, char* argv[]) {
#ifdef _WIN32
    initWindowsConsole();
#endif

    if (argc < 2) {
        printUsage(argv[0]);
        return 3;
    }
    
    std::string filename;
    
    for (int i = 1; i < argc; ++i) {
        if (strcmp(argv[i], "-h") == 0 || strcmp(argv[i], "--help") == 0) {
            printUsage(argv[0]);
            return 0;
        } else if (strcmp(argv[i], "-v") == 0 || strcmp(argv[i], "--version") == 0) {
            printVersion();
            return 0;
        } else if (argv[i][0] == '-') {
            std::cerr << "Unknown option: " << argv[i] << "\n";
            return 3;
        } else {
            filename = argv[i];
        }
    }
    
    if (filename.empty()) {
        std::cerr << "Error: No file specified\n";
        printUsage(argv[0]);
        return 3;
    }
    
    Terminal terminal;
    Viewer viewer(terminal);
    
    if (!viewer.loadFile(filename)) {
        std::cerr << "Error: Cannot open file '" << filename << "'\n";
        return 1;
    }
    
    viewer.run();
    
    return 0;
}
