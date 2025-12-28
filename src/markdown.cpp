#include "markdown.hpp"
#include "terminal.hpp"
#include <regex>
#include <sstream>

MarkdownRenderer::MarkdownRenderer() 
    : inCodeBlock(false), inTable(false), terminalWidth(80) {
}

std::vector<std::string> MarkdownRenderer::render(const std::string& content, int termWidth) {
    terminalWidth = termWidth;
    std::vector<std::string> lines;
    std::istringstream stream(content);
    std::string line;
    
    while (std::getline(stream, line)) {
        // Handle code blocks
        if (line.find("```") == 0) {
            if (inCodeBlock) {
                // End of code block - render accumulated content
                std::istringstream codeStream(codeBlockContent);
                std::string codeLine;
                while (std::getline(codeStream, codeLine)) {
                    lines.push_back(Color::BrightBlack + "  │ " + Color::Green + codeLine + Color::Reset);
                }
                codeBlockContent.clear();
                inCodeBlock = false;
            } else {
                inCodeBlock = true;
            }
            continue;
        }
        
        if (inCodeBlock) {
            codeBlockContent += line + "\n";
            continue;
        }
        
        // Horizontal rule
        if (std::regex_match(line, std::regex("^(---+|\\*\\*\\*+|___+)\\s*$"))) {
            std::string rule(termWidth - 2, '-');
            lines.push_back(Color::BrightBlack + rule + Color::Reset);
            continue;
        }
        
        // Headers
        std::smatch headerMatch;
        if (std::regex_match(line, headerMatch, std::regex("^(#{1,6})\\s+(.+)$"))) {
            int level = headerMatch[1].str().size();
            std::string headerText = headerMatch[2].str();
            lines.push_back(renderHeader(headerText, level));
            continue;
        }
        
        // Blockquotes
        if (line.find("> ") == 0) {
            std::string quoteText = line.substr(2);
            lines.push_back(renderBlockquote(quoteText));
            continue;
        }
        
        // Unordered lists
        std::smatch ulMatch;
        if (std::regex_match(line, ulMatch, std::regex("^(\\s*)[-*+]\\s+(.+)$"))) {
            std::string indent = ulMatch[1].str();
            std::string itemText = ulMatch[2].str();
            lines.push_back(indent + renderListItem(itemText, false, 0));
            continue;
        }
        
        // Ordered lists
        std::smatch olMatch;
        if (std::regex_match(line, olMatch, std::regex("^(\\s*)(\\d+)\\.\\s+(.+)$"))) {
            std::string indent = olMatch[1].str();
            int number = std::stoi(olMatch[2].str());
            std::string itemText = olMatch[3].str();
            lines.push_back(indent + renderListItem(itemText, true, number));
            continue;
        }
        
        // Table handling
        if (isTableRow(line)) {
            if (!inTable) {
                inTable = true;
                tableRows.clear();
            }
            if (!isTableSeparator(line)) {
                tableRows.push_back(parseTableRow(line));
            }
            continue;
        } else if (inTable) {
            // End of table, render it
            auto tableLines = renderTable(tableRows);
            for (const auto& tableLine : tableLines) {
                lines.push_back(tableLine);
            }
            tableRows.clear();
            inTable = false;
        }
        
        // Empty lines
        if (line.empty()) {
            lines.push_back("");
            continue;
        }
        
        // Regular paragraph with inline formatting
        std::string processed = processInlineFormatting(line);
        auto wrapped = wrapText(processed, termWidth);
        for (const auto& wrappedLine : wrapped) {
            lines.push_back(wrappedLine);
        }
    }
    
    return lines;
}

std::string MarkdownRenderer::renderHeader(const std::string& text, int level) {
    std::string color;
    std::string prefix;
    
    switch (level) {
        case 1:
            color = Color::Bold + Color::BrightMagenta;
            prefix = "═══ ";
            break;
        case 2:
            color = Color::Bold + Color::BrightCyan;
            prefix = "── ";
            break;
        case 3:
            color = Color::Bold + Color::BrightYellow;
            prefix = "─ ";
            break;
        case 4:
            color = Color::Bold + Color::BrightGreen;
            break;
        case 5:
            color = Color::Bold + Color::BrightBlue;
            break;
        default:
            color = Color::Bold + Color::White;
            break;
    }
    
    return color + prefix + text + Color::Reset;
}

std::string MarkdownRenderer::renderCodeBlock(const std::string& code) {
    return Color::Green + code + Color::Reset;
}

std::string MarkdownRenderer::renderBlockquote(const std::string& line) {
    return Color::BrightBlack + "┃ " + Color::Italic + Color::White + 
           processInlineFormatting(line) + Color::Reset;
}

std::string MarkdownRenderer::renderListItem(const std::string& line, bool ordered, int number) {
    std::string bullet;
    if (ordered) {
        bullet = Color::BrightCyan + std::to_string(number) + ". " + Color::Reset;
    } else {
        bullet = Color::BrightCyan + "• " + Color::Reset;
    }
    return bullet + processInlineFormatting(line);
}

std::string MarkdownRenderer::renderHorizontalRule(int width) {
    std::string rule(width, '-');
    return Color::BrightBlack + rule + Color::Reset;
}

std::string MarkdownRenderer::renderImage(const std::string& alt, const std::string& url) {
    return Color::Cyan + "[IMAGE: " + alt + " → " + url + "]" + Color::Reset;
}

std::string MarkdownRenderer::renderLink(const std::string& text, const std::string& url) {
    return Color::Underline + Color::BrightBlue + text + Color::Reset + 
           Color::BrightBlack + " (" + url + ")" + Color::Reset;
}

std::string MarkdownRenderer::processInlineFormatting(const std::string& text) {
    std::string result = text;
    result = processImages(result);
    result = processLinks(result);
    result = processInlineCode(result);
    result = processBold(result);
    result = processItalic(result);
    result = processStrikethrough(result);
    return result;
}

std::string MarkdownRenderer::processBold(const std::string& text) {
    std::string result = text;
    std::regex boldRegex("\\*\\*(.+?)\\*\\*|__(.+?)__");
    
    std::smatch match;
    std::string::const_iterator searchStart(result.cbegin());
    std::string output;
    
    while (std::regex_search(searchStart, result.cend(), match, boldRegex)) {
        output += match.prefix().str();
        std::string content = match[1].matched ? match[1].str() : match[2].str();
        output += Color::Bold + content + Color::Reset;
        searchStart = match.suffix().first;
    }
    output += std::string(searchStart, result.cend());
    
    return output.empty() ? result : output;
}

std::string MarkdownRenderer::processItalic(const std::string& text) {
    std::string result = text;
    std::regex italicRegex("\\*(.+?)\\*|_(.+?)_");
    
    std::smatch match;
    std::string::const_iterator searchStart(result.cbegin());
    std::string output;
    
    while (std::regex_search(searchStart, result.cend(), match, italicRegex)) {
        output += match.prefix().str();
        std::string content = match[1].matched ? match[1].str() : match[2].str();
        output += Color::Italic + content + Color::Reset;
        searchStart = match.suffix().first;
    }
    output += std::string(searchStart, result.cend());
    
    return output.empty() ? result : output;
}

std::string MarkdownRenderer::processStrikethrough(const std::string& text) {
    std::string result = text;
    std::regex strikeRegex("~~(.+?)~~");
    
    std::smatch match;
    std::string::const_iterator searchStart(result.cbegin());
    std::string output;
    
    while (std::regex_search(searchStart, result.cend(), match, strikeRegex)) {
        output += match.prefix().str();
        output += Color::Strike + match[1].str() + Color::Reset;
        searchStart = match.suffix().first;
    }
    output += std::string(searchStart, result.cend());
    
    return output.empty() ? result : output;
}

std::string MarkdownRenderer::processInlineCode(const std::string& text) {
    std::string result = text;
    std::regex codeRegex("`([^`]+)`");
    
    std::smatch match;
    std::string::const_iterator searchStart(result.cbegin());
    std::string output;
    
    while (std::regex_search(searchStart, result.cend(), match, codeRegex)) {
        output += match.prefix().str();
        output += Color::BgBlack + Color::BrightGreen + " " + match[1].str() + " " + Color::Reset;
        searchStart = match.suffix().first;
    }
    output += std::string(searchStart, result.cend());
    
    return output.empty() ? result : output;
}

std::string MarkdownRenderer::processLinks(const std::string& text) {
    std::string result = text;
    // Match [text](url) - images are already processed first
    std::regex linkRegex("\\[([^\\]]+)\\]\\(([^)]+)\\)");
    
    std::smatch match;
    std::string::const_iterator searchStart(result.cbegin());
    std::string output;
    
    while (std::regex_search(searchStart, result.cend(), match, linkRegex)) {
        output += match.prefix().str();
        output += renderLink(match[1].str(), match[2].str());
        searchStart = match.suffix().first;
    }
    output += std::string(searchStart, result.cend());
    
    return output.empty() ? result : output;
}

std::string MarkdownRenderer::processImages(const std::string& text) {
    std::string result = text;
    std::regex imgRegex("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
    
    std::smatch match;
    std::string::const_iterator searchStart(result.cbegin());
    std::string output;
    
    while (std::regex_search(searchStart, result.cend(), match, imgRegex)) {
        output += match.prefix().str();
        output += renderImage(match[1].str(), match[2].str());
        searchStart = match.suffix().first;
    }
    output += std::string(searchStart, result.cend());
    
    return output.empty() ? result : output;
}

std::vector<std::string> MarkdownRenderer::wrapText(const std::string& text, int width, const std::string& prefix) {
    std::vector<std::string> lines;
    
    if (text.length() <= static_cast<size_t>(width)) {
        lines.push_back(prefix + text);
        return lines;
    }
    
    // Simple word wrap - doesn't account for ANSI codes in width calculation
    // This is a known limitation
    lines.push_back(prefix + text);
    return lines;
}

bool MarkdownRenderer::isTableRow(const std::string& line) {
    // A table row starts with optional whitespace, then a pipe
    // Or contains pipes separating cells
    if (line.empty()) return false;
    
    // Check if line contains at least one pipe and looks like a table
    size_t pipeCount = 0;
    for (char c : line) {
        if (c == '|') pipeCount++;
    }
    
    return pipeCount >= 1;
}

bool MarkdownRenderer::isTableSeparator(const std::string& line) {
    // Table separator looks like: |---|---|---| or |----|:---:|----:|
    std::regex sepRegex("^\\|?[\\s:]*-+[\\s:|-]*$");
    return std::regex_match(line, sepRegex);
}

std::vector<std::string> MarkdownRenderer::parseTableRow(const std::string& line) {
    std::vector<std::string> cells;
    std::string trimmedLine = line;
    
    // Remove leading/trailing pipes
    if (!trimmedLine.empty() && trimmedLine.front() == '|') {
        trimmedLine = trimmedLine.substr(1);
    }
    if (!trimmedLine.empty() && trimmedLine.back() == '|') {
        trimmedLine = trimmedLine.substr(0, trimmedLine.length() - 1);
    }
    
    // Split by pipe
    std::istringstream stream(trimmedLine);
    std::string cell;
    while (std::getline(stream, cell, '|')) {
        // Trim whitespace
        size_t start = cell.find_first_not_of(" \t");
        size_t end = cell.find_last_not_of(" \t");
        if (start != std::string::npos && end != std::string::npos) {
            cells.push_back(cell.substr(start, end - start + 1));
        } else {
            cells.push_back("");
        }
    }
    
    return cells;
}

std::vector<std::string> MarkdownRenderer::renderTable(const std::vector<std::vector<std::string>>& rows) {
    std::vector<std::string> output;
    
    if (rows.empty()) return output;
    
    // Calculate column widths
    size_t numCols = 0;
    for (const auto& row : rows) {
        numCols = std::max(numCols, row.size());
    }
    
    std::vector<size_t> colWidths(numCols, 0);
    for (const auto& row : rows) {
        for (size_t i = 0; i < row.size(); ++i) {
            // Calculate visible width of processed cell content
            std::string processed = getProcessedCell(row[i]);
            size_t cellWidth = visibleWidth(processed);
            colWidths[i] = std::max(colWidths[i], cellWidth);
        }
    }
    
    // Ensure minimum width of 3 for each column
    for (auto& w : colWidths) {
        if (w < 3) w = 3;
    }
    
    // Build top border: +-------+-------+
    std::string topBorder = Color::BrightBlack + "+";
    for (size_t i = 0; i < numCols; ++i) {
        topBorder += std::string(colWidths[i] + 2, '-') + "+";
    }
    topBorder += Color::Reset;
    output.push_back(topBorder);
    
    // Render each row
    for (size_t rowIdx = 0; rowIdx < rows.size(); ++rowIdx) {
        const auto& row = rows[rowIdx];
        
        std::string line = Color::BrightBlack + "|" + Color::Reset;
        for (size_t i = 0; i < numCols; ++i) {
            std::string cellContent = (i < row.size()) ? row[i] : "";
            std::string processedCell = getProcessedCell(cellContent);
            size_t cellVisibleWidth = visibleWidth(processedCell);
            
            // Calculate padding based on visible width
            size_t padding = (colWidths[i] > cellVisibleWidth) ? (colWidths[i] - cellVisibleWidth) : 0;
            
            // Apply formatting: header row (first row) gets bold cyan
            if (rowIdx == 0) {
                line += " " + Color::Bold + Color::BrightCyan + cellContent + Color::Reset;
            } else {
                line += " " + processedCell;
            }
            
            line += std::string(padding + 1, ' ') + Color::BrightBlack + "|" + Color::Reset;
        }
        output.push_back(line);
        
        // Add separator after header row
        if (rowIdx == 0) {
            std::string sep = Color::BrightBlack + "+";
            for (size_t i = 0; i < numCols; ++i) {
                sep += std::string(colWidths[i] + 2, '=') + "+";
            }
            sep += Color::Reset;
            output.push_back(sep);
        }
    }
    
    // Bottom border
    std::string bottomBorder = Color::BrightBlack + "+";
    for (size_t i = 0; i < numCols; ++i) {
        bottomBorder += std::string(colWidths[i] + 2, '-') + "+";
    }
    bottomBorder += Color::Reset;
    output.push_back(bottomBorder);
    
    return output;
}

// Calculate visible width by stripping ANSI escape codes
size_t MarkdownRenderer::visibleWidth(const std::string& str) {
    size_t width = 0;
    bool inEscape = false;
    
    for (size_t i = 0; i < str.length(); ++i) {
        if (str[i] == '\033') {
            inEscape = true;
        } else if (inEscape) {
            if (str[i] == 'm') {
                inEscape = false;
            }
        } else {
            ++width;
        }
    }
    
    return width;
}

// Pre-process a cell to get its rendered form (for width calculation)
std::string MarkdownRenderer::getProcessedCell(const std::string& cell) {
    return processInlineFormatting(cell);
}
