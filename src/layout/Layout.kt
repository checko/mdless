package layout

import ir.Block
import ir.BlockKind
import ir.Inline
import ir.LayoutLine
import ir.Style
import ir.StyledSpan

object Layout {
    fun layoutBlock(block: Block, width: Int, tabWidth: Int = 4): List<LayoutLine> {
        return when (val k = block.kind) {
            is BlockKind.Paragraph -> layoutParagraphLike(block, width)
            is BlockKind.Heading -> layoutParagraphLike(block, width)
            is BlockKind.CodeBlock -> layoutCodeBlock(block, k, width)
            is BlockKind.ListBlock -> layoutList(block, k, width)
            is BlockKind.Blockquote -> layoutBlockquote(block, k, width)
            else -> emptyList() // to be implemented later for other kinds
        }
    }


    private fun layoutParagraphLike(block: Block, width: Int): List<LayoutLine> {
        val text = flattenText(block.inlines)
        val wrapped = wrapText(text, width)
        val lines = ArrayList<LayoutLine>(wrapped.size)
        var row = 0
        for (ln in wrapped) {
            lines += LayoutLine(listOf(StyledSpan(ln, Style())), block.id, row++)
        }
        return lines
    }

    private fun layoutCodeBlock(block: Block, code: BlockKind.CodeBlock, width: Int): List<LayoutLine> {
        val lines = code.text.split("\n")
        val out = ArrayList<LayoutLine>(lines.size)
        var row = 0
        for (ln in lines) {
            // No wrapping for code in this minimal version; truncate visually if needed
            out += LayoutLine(listOf(StyledSpan(ln, Style())), block.id, row++)
        }
        return out
    }


    private fun flattenText(inlines: List<Inline>): String {
        val sb = StringBuilder()
        for (i in inlines) {
            when (i) {
                is Inline.Text -> sb.append(i.text)
                is Inline.Code -> sb.append(i.code)
                is Inline.Emph -> sb.append(flattenText(i.children))
                is Inline.Strong -> sb.append(flattenText(i.children))
                is Inline.Link -> sb.append(flattenText(i.children))
                Inline.SoftBreak -> sb.append(' ')
                Inline.HardBreak -> sb.append('\n')
            }
        }
        return sb.toString()
    }

    private fun wrapText(text: String, width: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        val tokens = tokenize(text)
        val lines = ArrayList<String>()
        val current = StringBuilder()
        var currentW = 0

        fun flush() {
            lines += current.toString().trimEnd()
            current.clear()
            currentW = 0
        }

        for (t in tokens) {
            if (t == "\n") {
                flush()
                continue
            }
            val tw = Width.stringWidth(t)
            if (currentW == 0) {
                current.append(t)
                currentW += tw
            } else if (currentW + 1 + tw <= width) {
                // add with a space between tokens if previous didn't end in space
                if (current.lastOrNull() != ' ') current.append(' ')
                current.append(t)
                currentW = Width.stringWidth(current.toString())
            } else {
                flush()
                current.append(t)
                currentW = tw
            }
        }
        if (current.isNotEmpty()) flush()
        return lines
    }

    private fun tokenize(text: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        fun pushToken() {
            if (sb.isNotEmpty()) {
                out += sb.toString()
                sb.clear()
            }
        }
        for (ch in text) {
            when (ch) {
                ' ' -> {
                    // treat spaces as separators; do not keep multiple spaces
                    pushToken()
                }
                '\n' -> {
                    pushToken()
                    out += "\n"
                }
                else -> sb.append(ch)
            }
        }
        pushToken()
        return out
    }

    private fun layoutList(block: Block, list: BlockKind.ListBlock, width: Int, depth: Int = 0): List<LayoutLine> {
        val out = ArrayList<LayoutLine>()
        var row = 0
        val levelIndent = "  ".repeat(depth)
        for ((idx, item) in list.items.withIndex()) {
            val bullet = if (list.ordered) "${idx + 1}. " else "- "
            val contIndent = " ".repeat(bullet.length)
            var firstInItem = true
            for (child in item.blocks) {
                when (val k = child.kind) {
                    is BlockKind.ListBlock -> {
                        val nested = layoutList(child, k, width, depth + 1)
                        for (cl in nested) {
                            out += LayoutLine(cl.spans, block.id, row++)
                        }
                        firstInItem = false
                    }
                    else -> {
                        val innerWidth = (width - levelIndent.length - bullet.length).coerceAtLeast(1)
                        val childLines = layoutBlock(child, innerWidth)
                        for ((j, cl) in childLines.withIndex()) {
                            val pref = if (firstInItem && j == 0) levelIndent + bullet else levelIndent + contIndent
                            val text = pref + cl.spans.joinToString("") { it.text }
                            out += LayoutLine(listOf(StyledSpan(text, Style())), block.id, row++)
                        }
                        firstInItem = false
                    }
                }
            }
        }
        if (out.isEmpty()) out += LayoutLine(listOf(StyledSpan("", Style())), block.id, 0)
        return out
    }

    private fun layoutBlockquote(block: Block, quote: BlockKind.Blockquote, width: Int): List<LayoutLine> {
        val out = ArrayList<LayoutLine>()
        var row = 0
        val prefix = "> "
        val innerWidth = (width - prefix.length).coerceAtLeast(1)
        for (child in quote.children) {
            val childLines = layoutBlock(child, innerWidth)
            for (cl in childLines) {
                val text = prefix + cl.spans.joinToString("") { it.text }
                out += LayoutLine(listOf(StyledSpan(text, Style())), block.id, row++)
            }
        }
        if (out.isEmpty()) out += LayoutLine(listOf(StyledSpan("", Style())), block.id, 0)
        return out
    }

}
