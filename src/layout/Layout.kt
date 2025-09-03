package layout

import ir.Block
import ir.BlockKind
import ir.Inline
import ir.LayoutLine
import ir.Style
import ir.ColAlign
import ir.StyledSpan

object Layout {
    fun layoutBlock(block: Block, width: Int, tabWidth: Int = 4): List<LayoutLine> {
        return when (val k = block.kind) {
            is BlockKind.Paragraph -> layoutParagraphLike(block, width, tabWidth)
            BlockKind.Blank -> listOf(LayoutLine(listOf(StyledSpan("", Style())), block.id, 0))
            is BlockKind.Heading -> layoutParagraphLike(block, width, tabWidth)
            is BlockKind.CodeBlock -> layoutCodeBlock(block, k, width, tabWidth)
            is BlockKind.ListBlock -> layoutList(block, k, width, tabWidth)
            is BlockKind.Blockquote -> layoutBlockquote(block, k, width, tabWidth)
            BlockKind.ThematicBreak -> layoutHr(block, width)
            is BlockKind.Table -> layoutTable(block, k, width, tabWidth)
            else -> emptyList() // to be implemented later for other kinds
        }
    }

    private fun layoutParagraphLike(block: Block, width: Int, tabWidth: Int): List<LayoutLine> {
        val text = Tabs.expandTabs(flattenText(block.inlines), tabWidth)
        val wrapped = wrapText(text, width)
        val lines = ArrayList<LayoutLine>(wrapped.size)
        var row = 0
        for (ln in wrapped) {
            lines += LayoutLine(listOf(StyledSpan(ln, Style())), block.id, row++)
        }
        return lines
    }

    private fun layoutCodeBlock(block: Block, code: BlockKind.CodeBlock, width: Int, tabWidth: Int): List<LayoutLine> {
        val out = ArrayList<LayoutLine>()
        var row = 0
        for (ln in code.text.split("\n")) {
            var rest = Tabs.expandTabs(ln, tabWidth)
            while (rest.isNotEmpty()) {
                val take = takeByWidthRaw(rest, width)
                out += LayoutLine(listOf(StyledSpan(take, Style())), block.id, row++)
                rest = rest.drop(take.length)
                if (take.isEmpty()) break else if (rest.isNotEmpty()) {
                    // continue wrapping
                }
            }
            if (ln.isEmpty()) {
                out += LayoutLine(listOf(StyledSpan("", Style())), block.id, row++)
            }
        }
        return out
    }

    private fun takeByWidthRaw(s: String, maxWidth: Int): String {
        if (s.isEmpty() || maxWidth <= 0) return ""
        var w = 0
        var i = 0
        while (i < s.length) {
            val cw = Width.charWidth(s[i].code)
            if (w + cw > maxWidth) break
            w += cw
            i++
        }
        return s.substring(0, i)
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

        fun takeByWidth(s: String, maxWidth: Int): Pair<String, String> {
            if (s.isEmpty() || maxWidth <= 0) return "" to s
            var w = 0
            var i = 0
            while (i < s.length) {
                val cw = Width.charWidth(s[i].code)
                if (w + cw > maxWidth) break
                w += cw
                i++
            }
            return s.substring(0, i) to s.substring(i)
        }

        for (t in tokens) {
            if (t == "\n") {
                flush()
                continue
            }
            val tw = Width.stringWidth(t)
            if (tw <= width) {
                if (currentW == 0) {
                    current.append(t)
                    currentW += tw
                } else if (currentW + 1 + tw <= width) {
                    if (current.lastOrNull() != ' ') current.append(' ')
                    current.append(t)
                    currentW = Width.stringWidth(current.toString())
                } else {
                    flush()
                    current.append(t)
                    currentW = tw
                }
            } else {
                var rest = t
                // Place a space before breaking if needed
                if (currentW > 0 && current.lastOrNull() != ' ') {
                    if (currentW + 1 <= width) {
                        current.append(' '); currentW += 1
                    }
                }
                while (rest.isNotEmpty()) {
                    val remaining = width - currentW
                    if (remaining <= 0) {
                        flush()
                    }
                    val (chunk, tail) = takeByWidth(rest, width - currentW)
                    if (chunk.isEmpty()) {
                        flush()
                        continue
                    }
                    current.append(chunk)
                    currentW += Width.stringWidth(chunk)
                    rest = tail
                    if (rest.isNotEmpty()) {
                        flush()
                    }
                }
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

    private fun layoutList(block: Block, list: BlockKind.ListBlock, width: Int, tabWidth: Int, depth: Int = 0): List<LayoutLine> {
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
                        val nested = layoutList(child, k, width, tabWidth, depth + 1)
                        for (cl in nested) {
                            out += LayoutLine(cl.spans, block.id, row++)
                        }
                        firstInItem = false
                    }
                    else -> {
                        val innerWidth = (width - levelIndent.length - bullet.length).coerceAtLeast(1)
                        val childLines = layoutBlock(child, innerWidth, tabWidth)
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

    private fun layoutBlockquote(block: Block, quote: BlockKind.Blockquote, width: Int, tabWidth: Int): List<LayoutLine> {
        val out = ArrayList<LayoutLine>()
        var row = 0
        val prefix = "> "
        val innerWidth = (width - prefix.length).coerceAtLeast(1)
        for (child in quote.children) {
            val childLines = layoutBlock(child, innerWidth, tabWidth)
            for (cl in childLines) {
                val text = prefix + cl.spans.joinToString("") { it.text }
                out += LayoutLine(listOf(StyledSpan(text, Style())), block.id, row++)
            }
        }
        if (out.isEmpty()) out += LayoutLine(listOf(StyledSpan("", Style())), block.id, 0)
        return out
    }

    private fun layoutHr(block: Block, width: Int): List<LayoutLine> {
        val line = "-".repeat(width.coerceAtLeast(1))
        return listOf(LayoutLine(listOf(StyledSpan(line, Style())), block.id, 0))
    }

    private fun layoutTable(block: Block, table: BlockKind.Table, width: Int, tabWidth: Int): List<LayoutLine> {
        val rows = table.rows
        if (rows.isEmpty()) return listOf(LayoutLine(listOf(StyledSpan("", Style())), block.id, 0))
        val cols = rows.maxOf { it.size }
        if (cols == 0) return listOf(LayoutLine(listOf(StyledSpan("", Style())), block.id, 0))
        val aligns = Array(cols) { idx -> table.aligns.getOrNull(idx) ?: ColAlign.Left }
        val colSep = " | "
        val padBetween = colSep.length
        val minCol = 1

        val desired = IntArray(cols) { 1 }
        for (r in rows) {
            for (j in 0 until cols) {
                val raw = r.getOrNull(j) ?: ""
                val text = Tabs.expandTabs(raw, tabWidth)
                val w = Width.stringWidth(text)
                if (w > desired[j]) desired[j] = w
            }
        }

        fun sumWithPads(arr: IntArray): Int = arr.sum() + padBetween * (cols - 1)

        val colWidths = desired.copyOf()
        var total = sumWithPads(colWidths)
        val maxTotal = width
        if (total > maxTotal) {
            val contentTotal = colWidths.sum()
            val targetContent = (maxTotal - padBetween * (cols - 1)).coerceAtLeast(cols * minCol)
            if (contentTotal > 0) {
                for (i in 0 until cols) {
                    val prop = (colWidths[i].toDouble() / contentTotal.toDouble())
                    colWidths[i] = kotlin.math.max(minCol, kotlin.math.floor(prop * targetContent).toInt())
                }
            } else {
                for (i in 0 until cols) colWidths[i] = minCol
            }
            fun widestIndex(): Int = (0 until cols).maxByOrNull { colWidths[it] } ?: 0
            total = sumWithPads(colWidths)
            while (total > maxTotal) {
                val idx = widestIndex()
                if (colWidths[idx] > minCol) colWidths[idx]-- else break
                total = sumWithPads(colWidths)
            }
        }

        val out = ArrayList<LayoutLine>()
        var outRow = 0
        for (r in rows) {
            val wrappedCells: List<List<String>> = (0 until cols).map { j ->
                val raw = r.getOrNull(j) ?: ""
                val text = Tabs.expandTabs(raw, tabWidth)
                wrapText(text, colWidths[j])
            }
            val rowHeight = wrappedCells.maxOf { it.size }
            for (k in 0 until rowHeight) {
                val sb = StringBuilder()
                for (j in 0 until cols) {
                    if (j > 0) sb.append(colSep)
                    val colW = colWidths[j]
                    val cellLine = wrappedCells[j].getOrNull(k) ?: ""
                    val cellW = Width.stringWidth(cellLine)
                    val padding = (colW - cellW).coerceAtLeast(0)
                    when (aligns[j]) {
                        ColAlign.Left -> {
                            sb.append(cellLine)
                            repeat(padding) { sb.append(' ') }
                        }
                        ColAlign.Right -> {
                            repeat(padding) { sb.append(' ') }
                            sb.append(cellLine)
                        }
                        ColAlign.Center -> {
                            val left = padding / 2
                            val right = padding - left
                            repeat(left) { sb.append(' ') }
                            sb.append(cellLine)
                            repeat(right) { sb.append(' ') }
                        }
                    }
                }
                out += LayoutLine(listOf(StyledSpan(sb.toString(), Style())), block.id, outRow++)
            }
            // Draw a simple header separator after first logical row
            if (outRow == rowHeight) {
                val sepWidth = colWidths.sum() + padBetween * (cols - 1)
                val sepLine = "-".repeat(sepWidth.coerceAtLeast(1))
                out += LayoutLine(listOf(StyledSpan(sepLine, Style())), block.id, outRow++)
            }
        }
        if (out.isEmpty()) out += LayoutLine(listOf(StyledSpan("", Style())), block.id, 0)
        return out
    }

}
