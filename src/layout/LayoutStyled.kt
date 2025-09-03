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
            is BlockKind.Paragraph, is BlockKind.Heading -> layoutStyled(block, width, theme, tabWidth)
            is BlockKind.CodeBlock -> layoutCodeBlockStyled(block, k, width, theme, tabWidth)
            is BlockKind.ListBlock -> layoutList(block, k, width, theme, tabWidth)
            is BlockKind.Blockquote -> layoutQuote(block, k, width, theme, tabWidth)
            BlockKind.ThematicBreak -> layoutHr(block, width, theme)
            is BlockKind.Table -> layoutTable(block, k, width, theme, tabWidth)
            else -> emptyList()
        }
    }

    private fun layoutCodeBlockStyled(block: Block, code: BlockKind.CodeBlock, width: Int, theme: Theme, tabWidth: Int): List<LayoutLine> {
        val style = Styler.styleBlock(block, theme).firstOrNull()?.style ?: Style()
        val out = ArrayList<LayoutLine>()
        var row = 0
        for (ln in code.text.split('\n')) {
            var rest = Tabs.expandTabs(ln, tabWidth)
            while (rest.isNotEmpty()) {
                val take = takeByWidth(rest, width)
                out += LayoutLine(listOf(StyledSpan(take, style)), block.id, row++)
                rest = rest.drop(take.length)
                if (take.isEmpty()) break
            }
            if (ln.isEmpty()) out += LayoutLine(listOf(StyledSpan("", style)), block.id, row++)
        }
        return out
    }

    private data class Tok(val text: String, val style: Style, val isNewline: Boolean)

    private fun layoutStyled(block: Block, width: Int, theme: Theme, tabWidth: Int): List<LayoutLine> {
        // Expand tabs in each styled span before tokenization
        val styled = Styler.styleBlock(block, theme).map { sp ->
            val t = Tabs.expandTabs(sp.text, tabWidth)
            StyledSpan(t, sp.style)
        }
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
            if (tw <= width) {
                if (currentW == 0) {
                    appendSpan(t.text, t.style)
                    currentW += tw
                } else if (currentW + 1 + tw <= width) {
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
            } else {
                var rest = t.text
                // place a space before breaking if needed
                val prevStyle = current.lastOrNull()?.style ?: t.style
                val lastText = current.lastOrNull()?.text
                if (currentW > 0 && lastText != null && lastText.lastOrNull() != ' ') {
                    if (currentW + 1 <= width) {
                        appendSpan(" ", prevStyle)
                        currentW += 1
                    }
                }
                while (rest.isNotEmpty()) {
                    val remaining = width - currentW
                    if (remaining <= 0) {
                        flush(row++)
                    }
                    val chunk = takeByWidth(rest, width - currentW)
                    if (chunk.isEmpty()) {
                        flush(row++)
                        continue
                    }
                    appendSpan(chunk, t.style)
                    currentW += Width.stringWidth(chunk)
                    rest = rest.drop(chunk.length)
                    if (rest.isNotEmpty()) flush(row++)
                }
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

    private fun takeByWidth(s: String, maxWidth: Int): String {
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

    private fun layoutList(block: Block, list: BlockKind.ListBlock, width: Int, theme: Theme, tabWidth: Int, depth: Int = 0): List<LayoutLine> {
        val out = ArrayList<LayoutLine>()
        var row = 0
        val baseStyle = when (theme.mode) { style.ThemeMode.NoColor -> Style() else -> Style(fg = theme.text) }
        val levelIndent = "  ".repeat(depth)
        for ((idx, item) in list.items.withIndex()) {
            val bulletText = if (list.ordered) "${idx + 1}. " else "- "
            val bulletStyle = baseStyle
            val indentText = " ".repeat(bulletText.length)
            var firstInItem = true
            for (child in item.blocks) {
                when (val k = child.kind) {
                    is BlockKind.ListBlock -> {
                        val nested = layoutList(child, k, width, theme, tabWidth, depth + 1)
                        for (cl in nested) out += LayoutLine(cl.spans, block.id, row++)
                        firstInItem = false
                    }
                    else -> {
                        val innerWidth = (width - levelIndent.length - bulletText.length).coerceAtLeast(1)
                        val childLines = layoutBlock(child, innerWidth, theme, tabWidth)
                        for ((j, cl) in childLines.withIndex()) {
                            val prefText = if (firstInItem && j == 0) levelIndent + bulletText else levelIndent + indentText
                            val spans = ArrayList<StyledSpan>()
                            if (prefText.isNotEmpty()) spans += StyledSpan(prefText, bulletStyle)
                            spans.addAll(cl.spans)
                            out += LayoutLine(spans, block.id, row++)
                        }
                        firstInItem = false
                    }
                }
            }
        }
        if (out.isEmpty()) out += LayoutLine(listOf(StyledSpan("", baseStyle)), block.id, 0)
        return out
    }

    private fun layoutQuote(block: Block, quote: BlockKind.Blockquote, width: Int, theme: Theme, tabWidth: Int): List<LayoutLine> {
        val out = ArrayList<LayoutLine>()
        var row = 0
        val prefixText = "> "
        val prefixStyle = when (theme.mode) { style.ThemeMode.NoColor -> Style() else -> Style(fg = theme.quote) }
        val innerW = (width - prefixText.length).coerceAtLeast(1)
        for (child in quote.children) {
            val childLines = layoutBlock(child, innerW, theme, tabWidth)
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

    private fun layoutHr(block: Block, width: Int, theme: Theme): List<LayoutLine> {
        val style = when (theme.mode) { style.ThemeMode.NoColor -> Style() else -> Style(fg = theme.heading) }
        val line = "-".repeat(width.coerceAtLeast(1))
        return listOf(LayoutLine(listOf(StyledSpan(line, style)), block.id, 0))
    }

    private fun layoutTable(block: Block, table: BlockKind.Table, width: Int, theme: Theme, tabWidth: Int): List<LayoutLine> {
        val rows = table.rows
        if (rows.isEmpty()) return listOf(LayoutLine(listOf(StyledSpan("", Style())), block.id, 0))
        val cols = rows.maxOf { it.size }
        if (cols == 0) return listOf(LayoutLine(listOf(StyledSpan("", Style())), block.id, 0))
        val aligns = Array(cols) { idx -> table.aligns.getOrNull(idx) ?: ir.ColAlign.Left }
        val colSep = " | "
        val padBetween = colSep.length
        val minCol = 1

        val base = when (theme.mode) { style.ThemeMode.NoColor -> Style() else -> Style(fg = theme.text) }

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

        fun wrapCell(text: String, w: Int): List<String> {
            if (w <= 0) return listOf("")
            val out = ArrayList<String>()
            var current = StringBuilder()
            var currentW = 0
            fun flush() { out += current.toString().trimEnd(); current = StringBuilder(); currentW = 0 }
            fun takeByWidth(s: String, maxW: Int): Pair<String, String> {
                var i = 0; var ww = 0
                while (i < s.length) {
                    val cw = Width.charWidth(s[i].code)
                    if (ww + cw > maxW) break
                    ww += cw; i++
                }
                return s.substring(0, i) to s.substring(i)
            }
            val tokens = run {
                val list = ArrayList<String>()
                val sb = StringBuilder()
                fun push() { if (sb.isNotEmpty()) { list += sb.toString(); sb.clear() } }
                for (ch in text) {
                    when (ch) {
                        ' ' -> push()
                        '\n' -> { push(); list += "\n" }
                        else -> sb.append(ch)
                    }
                }
                push(); list
            }
            for (t in tokens) {
                if (t == "\n") { flush(); continue }
                val tw = Width.stringWidth(t)
                if (tw <= w) {
                    if (currentW == 0) { current.append(t); currentW += tw }
                    else if (currentW + 1 + tw <= w) { if (current.lastOrNull() != ' ') current.append(' '); current.append(t); currentW = Width.stringWidth(current.toString()) }
                    else { flush(); current.append(t); currentW = tw }
                } else {
                    var rest = t
                    if (currentW > 0 && current.lastOrNull() != ' ') { if (currentW + 1 <= w) { current.append(' '); currentW += 1 } }
                    while (rest.isNotEmpty()) {
                        val rem = w - currentW
                        if (rem <= 0) flush()
                        val (chunk, tail) = takeByWidth(rest, w - currentW)
                        if (chunk.isEmpty()) { flush(); continue }
                        current.append(chunk); currentW += Width.stringWidth(chunk)
                        rest = tail
                        if (rest.isNotEmpty()) flush()
                    }
                }
            }
            if (current.isNotEmpty()) flush()
            return out
        }

        val out = ArrayList<LayoutLine>()
        var outRow = 0
        for (r in rows) {
            val wrapped: List<List<String>> = (0 until cols).map { j ->
                wrapCell(Tabs.expandTabs(r.getOrNull(j) ?: "", tabWidth), colWidths[j])
            }
            val rowHeight = wrapped.maxOf { it.size }
            for (k in 0 until rowHeight) {
                val spans = ArrayList<StyledSpan>()
                for (j in 0 until cols) {
                    if (j > 0) spans += StyledSpan(colSep, base)
                    val colW = colWidths[j]
                    val cellLine = wrapped[j].getOrNull(k) ?: ""
                    val cellW = Width.stringWidth(cellLine)
                    val padding = (colW - cellW).coerceAtLeast(0)
                    when (aligns[j]) {
                        ir.ColAlign.Left -> {
                            spans += StyledSpan(cellLine, base)
                            if (padding > 0) spans += StyledSpan(" ".repeat(padding), base)
                        }
                        ir.ColAlign.Right -> {
                            if (padding > 0) spans += StyledSpan(" ".repeat(padding), base)
                            spans += StyledSpan(cellLine, base)
                        }
                        ir.ColAlign.Center -> {
                            val left = padding / 2
                            val right = padding - left
                            if (left > 0) spans += StyledSpan(" ".repeat(left), base)
                            spans += StyledSpan(cellLine, base)
                            if (right > 0) spans += StyledSpan(" ".repeat(right), base)
                        }
                    }
                }
                out += LayoutLine(spans, block.id, outRow++)
            }
            // Add a header separator after first logical row
            if (outRow == rowHeight) {
                val sepWidth = colWidths.sum() + padBetween * (cols - 1)
                val sep = "-".repeat(sepWidth.coerceAtLeast(1))
                out += LayoutLine(listOf(StyledSpan(sep, base)), block.id, outRow++)
            }
        }
        if (out.isEmpty()) out += LayoutLine(listOf(StyledSpan("", base)), block.id, 0)
        return out
    }
}
