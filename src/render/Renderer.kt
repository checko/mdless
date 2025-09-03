package render

import ir.AnsiColor
import ir.LayoutLine
import ir.Style
import ir.StyledSpan
import layout.Width

object Renderer {
    private const val ESC = "\u001B["

    fun render(lines: List<LayoutLine>, enableColor: Boolean = true, clearEol: Boolean = false, maxColumns: Int? = null, crlf: Boolean = false): String {
        val sb = StringBuilder()
        for (line in lines) {
            renderLine(line, sb, enableColor, maxColumns)
            if (clearEol) sb.append(eraseToEol())
            if (crlf) sb.append('\r')
            sb.append('\n')
        }
        return sb.toString()
    }

    fun renderLine(line: LayoutLine, out: Appendable, enableColor: Boolean = true, maxColumns: Int? = null) {
        var cols = 0
        fun canFit(cw: Int): Boolean = maxColumns == null || cols + cw <= maxColumns
        for (span in line.spans) {
            if (enableColor) {
                val code = styleToSgr(span.style)
                if (code.isNotEmpty()) out.append(code)
            }
            val s = span.text
            if (maxColumns == null) {
                out.append(s)
            } else {
                var i = 0
                while (i < s.length) {
                    val cp = nextCodePoint(s, i)
                    val cw = Width.charWidth(cp.first)
                    if (!canFit(cw)) break
                    // append code point
                    if (cp.second == i + 1) {
                        out.append(s[i])
                    } else {
                        out.append(s.substring(i, cp.second))
                    }
                    cols += cw
                    i = cp.second
                }
            }
            if (enableColor) out.append(reset())
            if (maxColumns != null && cols >= maxColumns) break
        }
    }

    private fun nextCodePoint(s: String, index: Int): Pair<Int, Int> {
        val ch = s[index]
        if (ch >= '\uD800' && ch <= '\uDBFF' && index + 1 < s.length) {
            val low = s[index + 1]
            if (low >= '\uDC00' && low <= '\uDFFF') {
                val hiVal = ch.code - 0xD800
                val loVal = low.code - 0xDC00
                val cp = 0x10000 + ((hiVal shl 10) or loVal)
                return cp to (index + 2)
            }
        }
        return ch.code to (index + 1)
    }

    // Render with highlight ranges per line. Ranges are [start..endInclusive] in the concatenated line text.
    fun renderWithHighlights(lines: List<LayoutLine>, highlights: List<List<IntRange>>, enableColor: Boolean = true, clearEol: Boolean = false, maxColumns: Int? = null, crlf: Boolean = false): String {
        val sb = StringBuilder()
        for (i in lines.indices) {
            val line = lines[i]
            val hls = highlights.getOrNull(i) ?: emptyList()
            renderLineWithHighlights(line, hls, sb, enableColor, maxColumns)
            if (clearEol) sb.append(eraseToEol())
            if (crlf) sb.append('\r')
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun renderLineWithHighlights(line: LayoutLine, ranges: List<IntRange>, out: Appendable, enableColor: Boolean, maxColumns: Int? = null) {
        val text = line.spans.joinToString("") { it.text }
        val base = line.spans.firstOrNull()?.style ?: Style()
        if (ranges.isEmpty()) {
            // Fallback to normal rendering of a single span with base style
            val tmp = LayoutLine(listOf(StyledSpan(text, base)), line.blockId, line.rowInBlock)
            renderLine(tmp, out, enableColor, maxColumns)
            return
        }
        // Merge and clamp ranges
        val clamped = ranges
            .map { IntRange(it.first.coerceAtLeast(0), it.last.coerceAtMost((text.length - 1).coerceAtLeast(0))) }
            .filter { it.first <= it.last }
            .sortedBy { it.first }
        var cursor = 0
        var cols = 0
        fun canFit(len: Int): Boolean = maxColumns == null || cols + len <= maxColumns
        for ((idx, r) in clamped.withIndex()) {
            if (cursor < r.first && (maxColumns == null || cols < maxColumns)) {
                // non-highlight segment
                val seg = text.substring(cursor, r.first)
                val take = if (maxColumns == null) seg.length else (maxColumns - cols).coerceAtLeast(0)
                val chunk = if (maxColumns == null) seg else seg.take(take)
                emitSegment(chunk, base, out, enableColor)
                cols += chunk.length
            }
            if (maxColumns != null && cols >= maxColumns) break
            // highlight segment
            val hlStyle = Style(fg = base.fg, bold = base.bold, underline = true)
            val segHL = text.substring(r.first, r.last + 1)
            val takeHL = if (maxColumns == null) segHL.length else (maxColumns - cols).coerceAtLeast(0)
            val chunkHL = if (maxColumns == null) segHL else segHL.take(takeHL)
            emitSegment(chunkHL, hlStyle, out, enableColor)
            cols += chunkHL.length
            cursor = r.last + 1
            if (idx == clamped.lastIndex && cursor < text.length && (maxColumns == null || cols < maxColumns)) {
                val segRest = text.substring(cursor)
                val takeRest = if (maxColumns == null) segRest.length else (maxColumns - cols).coerceAtLeast(0)
                val chunkRest = if (maxColumns == null) segRest else segRest.take(takeRest)
                emitSegment(chunkRest, base, out, enableColor)
                cols += chunkRest.length
            }
        }
    }

    private fun emitSegment(s: String, style: Style, out: Appendable, enableColor: Boolean) {
        if (s.isEmpty()) return
        if (enableColor) {
            val code = styleToSgr(style)
            if (code.isNotEmpty()) out.append(code)
        }
        out.append(s)
        if (enableColor) out.append(reset())
    }

    private fun styleToSgr(style: Style): String {
        val codes = mutableListOf<String>()
        if (style.bold) codes.add("1")
        if (style.underline) codes.add("4")
        style.fg?.let { fg ->
            codes.add(fgCode(fg))
        }
        return if (codes.isEmpty()) "" else ESC + codes.joinToString(";") + "m"
    }

    private fun fgCode(c: AnsiColor): String = when (c) {
        AnsiColor.DEFAULT -> "39"
        AnsiColor.BLACK -> "30"
        AnsiColor.RED -> "31"
        AnsiColor.GREEN -> "32"
        AnsiColor.YELLOW -> "33"
        AnsiColor.BLUE -> "34"
        AnsiColor.MAGENTA -> "35"
        AnsiColor.CYAN -> "36"
        AnsiColor.WHITE -> "37"
    }

    private fun reset(): String = ESC + "0m"

    private fun eraseToEol(): String = ESC + "K"
}
