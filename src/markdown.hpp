#ifndef MDLESS_MARKDOWN_HPP
#define MDLESS_MARKDOWN_HPP

#include <string>
#include <vector>

class MarkdownRenderer {
public:
    MarkdownRenderer();
    
    // Render markdown content to ANSI-formatted lines
    std::vector<std::string> render(const std::string& content, int termWidth);
    
private:
    // Render individual elements
    std::string renderHeader(const std::string& line, int level);
    std::string renderCodeBlock(const std::string& code);
    std::string renderBlockquote(const std::string& line);
    std::string renderListItem(const std::string& line, bool ordered, int number);
    std::string renderHorizontalRule(int width);
    std::string renderImage(const std::string& alt, const std::string& url);
    std::string renderLink(const std::string& text, const std::string& url);
    
    // Table rendering
    std::vector<std::string> renderTable(const std::vector<std::vector<std::string>>& rows);
    bool isTableRow(const std::string& line);
    bool isTableSeparator(const std::string& line);
    std::vector<std::string> parseTableRow(const std::string& line);
    
    // Inline formatting
    std::string processInlineFormatting(const std::string& text);
    std::string processBold(const std::string& text);
    std::string processItalic(const std::string& text);
    std::string processStrikethrough(const std::string& text);
    std::string processInlineCode(const std::string& text);
    std::string processLinks(const std::string& text);
    std::string processImages(const std::string& text);
    
    // State tracking for multi-line elements
    bool inCodeBlock;
    bool inTable;
    std::string codeBlockContent;
    std::vector<std::vector<std::string>> tableRows;
    int terminalWidth;
    
    // Word wrapping
    std::vector<std::string> wrapText(const std::string& text, int width, const std::string& prefix = "");
};

#endif // MDLESS_MARKDOWN_HPP
