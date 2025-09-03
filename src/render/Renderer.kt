package render

import ir.AnsiColor
import ir.LayoutLine
import ir.Style
import ir.StyledSpan

object Renderer {
    private const val ESC = "\u001B["

    fun render(lines: List<LayoutLine>, enableColor: Boolean = true): String {
        val sb = StringBuilder()
        for (line in lines) {
            renderLine(line, sb, enableColor)
            sb.append('\n')
        }
        return sb.toString()
    }

    fun renderLine(line: LayoutLine, out: Appendable, enableColor: Boolean = true) {
        for (span in line.spans) {
            if (enableColor) {
                val code = styleToSgr(span.style)
                if (code.isNotEmpty()) out.append(code)
            }
            out.append(span.text)
            if (enableColor) out.append(reset())
        }
    }

    // Render with highlight ranges per line. Ranges are [start..endInclusive] in the concatenated line text.
    fun renderWithHighlights(lines: List<LayoutLine>, highlights: List<List<IntRange>>, enableColor: Boolean = true): String {
        val sb = StringBuilder()
        for (i in lines.indices) {
            val line = lines[i]
            val hls = highlights.getOrNull(i) ?: emptyList()
            renderLineWithHighlights(line, hls, sb, enableColor)
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun renderLineWithHighlights(line: LayoutLine, ranges: List<IntRange>, out: Appendable, enableColor: Boolean) {
        val text = line.spans.joinToString("") { it.text }
        val base = line.spans.firstOrNull()?.style ?: Style()
        if (ranges.isEmpty()) {
            // Fallback to normal rendering of a single span with base style
            val tmp = LayoutLine(listOf(StyledSpan(text, base)), line.blockId, line.rowInBlock)
            renderLine(tmp, out, enableColor)
            return
        }
        // Merge and clamp ranges
        val clamped = ranges
            .map { IntRange(it.first.coerceAtLeast(0), it.last.coerceAtMost((text.length - 1).coerceAtLeast(0))) }
            .filter { it.first <= it.last }
            .sortedBy { it.first }
        var cursor = 0
        for ((idx, r) in clamped.withIndex()) {
            if (cursor < r.first) {
                // non-highlight segment
                emitSegment(text.substring(cursor, r.first), base, out, enableColor)
            }
            // highlight segment
            val hlStyle = Style(fg = base.fg, bold = base.bold, underline = true)
            emitSegment(text.substring(r.first, r.last + 1), hlStyle, out, enableColor)
            cursor = r.last + 1
            if (idx == clamped.lastIndex && cursor < text.length) {
                emitSegment(text.substring(cursor), base, out, enableColor)
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
}
