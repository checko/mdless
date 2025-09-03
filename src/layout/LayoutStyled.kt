package layout

import ir.Block
import ir.BlockKind
import ir.LayoutLine
import ir.Style
import ir.StyledSpan
import style.Styler
import style.Theme

object LayoutStyled {
    fun layoutBlock(block: Block, width: Int, theme: Theme, tabWidth: Int = 4): List<LayoutLine> {
        return when (val k = block.kind) {
            is BlockKind.Paragraph, is BlockKind.Heading -> layoutStyled(block, width, theme)
            is BlockKind.CodeBlock -> layoutCodeBlockStyled(block, k, width, theme)
            is BlockKind.ListBlock -> layoutList(block, k, width, theme)
            is BlockKind.Blockquote -> layoutQuote(block, k, width, theme)
            else -> emptyList()
        }
    }

    private fun layoutCodeBlockStyled(block: Block, code: BlockKind.CodeBlock, width: Int, theme: Theme): List<LayoutLine> {
        val style = Styler.styleBlock(block, theme).firstOrNull()?.style ?: Style()
        val lines = code.text.split('\n')
        val out = ArrayList<LayoutLine>(lines.size)
        var row = 0
        for (ln in lines) {
            out += LayoutLine(listOf(StyledSpan(ln, style)), block.id, row++)
        }
        return out
    }

    private data class Tok(val text: String, val style: Style, val isNewline: Boolean)

    private fun layoutStyled(block: Block, width: Int, theme: Theme): List<LayoutLine> {
        val styled = Styler.styleBlock(block, theme)
        val toks = toTokens(styled)
        val lines = ArrayList<LayoutLine>()
        var current = ArrayList<StyledSpan>()
        var currentW = 0

        fun appendSpan(text: String, style: Style) {
            if (text.isEmpty()) return
            if (current.isNotEmpty()) {
                val last = current.last()
                if (last.style == style) {
                    current[current.lastIndex] = StyledSpan(last.text + text, style)
                    return
                }
            }
            current.add(StyledSpan(text, style))
        }

        fun flush(row: Int) {
            lines += LayoutLine(current.toList(), block.id, row)
            current = ArrayList()
            currentW = 0
        }

        var row = 0
        for (t in toks) {
            if (t.isNewline) {
                flush(row++)
                continue
            }
            val tw = Width.stringWidth(t.text)
            if (currentW == 0) {
                appendSpan(t.text, t.style)
                currentW += tw
            } else if (currentW + 1 + tw <= width) {
                // add a single space between tokens if last char isn't space
                val prevStyle = current.lastOrNull()?.style ?: t.style
                val lastText = current.lastOrNull()?.text
                if (lastText != null && lastText.lastOrNull() != ' ') appendSpan(" ", prevStyle)
                appendSpan(t.text, t.style)
                currentW = Width.stringWidth(current.joinToString("") { it.text })
            } else {
                flush(row++)
                appendSpan(t.text, t.style)
                currentW = tw
            }
        }
        if (current.isNotEmpty()) flush(row++)
        if (lines.isEmpty()) lines += LayoutLine(listOf(StyledSpan("", Style())), block.id, 0)
        return lines
    }

    private fun toTokens(spans: List<StyledSpan>): List<Tok> {
        val out = ArrayList<Tok>()
        for (sp in spans) {
            var i = 0
            while (i < sp.text.length) {
                val ch = sp.text[i]
                when (ch) {
                    '\n' -> {
                        out += Tok("\n", sp.style, isNewline = true)
                        i++
                    }
                    ' ' -> {
                        // collapse spaces as separators: skip emitting explicit spaces here
                        i++
                    }
                    else -> {
                        // read a word until space or newline
                        val start = i
                        var j = i
                        while (j < sp.text.length) {
                            val c = sp.text[j]
                            if (c == ' ' || c == '\n') break
                            j++
                        }
                        val word = sp.text.substring(start, j)
                        out += Tok(word, sp.style, isNewline = false)
                        i = j
                    }
                }
            }
        }
        return out
    }

    private fun layoutList(block: Block, list: BlockKind.ListBlock, width: Int, theme: Theme): List<LayoutLine> {
        val out = ArrayList<LayoutLine>()
        var row = 0
        val baseStyle = when (theme.mode) { style.ThemeMode.NoColor -> Style() else -> Style(fg = theme.text) }
        for ((idx, item) in list.items.withIndex()) {
            val bulletText = if (list.ordered) "${idx + 1}. " else "- "
            val bulletStyle = baseStyle
            val indentText = " ".repeat(bulletText.length)
            var firstInItem = true
            for (child in item.blocks) {
                val childLines = layoutBlock(child, (width - bulletText.length).coerceAtLeast(1), theme)
                for ((j, cl) in childLines.withIndex()) {
                    val prefText = if (firstInItem && j == 0) bulletText else indentText
                    val spans = ArrayList<StyledSpan>()
                    if (prefText.isNotEmpty()) spans += StyledSpan(prefText, bulletStyle)
                    spans.addAll(cl.spans)
                    out += LayoutLine(spans, block.id, row++)
                }
                firstInItem = false
            }
        }
        if (out.isEmpty()) out += LayoutLine(listOf(StyledSpan("", baseStyle)), block.id, 0)
        return out
    }

    private fun layoutQuote(block: Block, quote: BlockKind.Blockquote, width: Int, theme: Theme): List<LayoutLine> {
        val out = ArrayList<LayoutLine>()
        var row = 0
        val prefixText = "> "
        val prefixStyle = when (theme.mode) { style.ThemeMode.NoColor -> Style() else -> Style(fg = theme.quote) }
        val innerW = (width - prefixText.length).coerceAtLeast(1)
        for (child in quote.children) {
            val childLines = layoutBlock(child, innerW, theme)
            for (cl in childLines) {
                val spans = ArrayList<StyledSpan>()
                spans += StyledSpan(prefixText, prefixStyle)
                spans.addAll(cl.spans)
                out += LayoutLine(spans, block.id, row++)
            }
        }
        if (out.isEmpty()) out += LayoutLine(listOf(StyledSpan("", prefixStyle)), block.id, 0)
        return out
    }
}
