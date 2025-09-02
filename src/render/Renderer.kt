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
