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

}
