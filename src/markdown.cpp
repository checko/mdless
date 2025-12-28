#include "markdown.hpp"
#include "terminal.hpp"
#include <regex>
#include <sstream>

MarkdownRenderer::MarkdownRenderer() 
    : inCodeBlock(false), terminalWidth(80) {
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
