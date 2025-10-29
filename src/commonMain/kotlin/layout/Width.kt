package layout

expect object Width {
    var usePosix: Boolean
    fun charWidth(cp: Int): Int
    fun stringWidth(s: String): Int
    fun takePrefixByColumns(s: String, maxCols: Int): Int
}

object Tabs {
    fun expandTabs(s: String, tabWidth: Int): String {
        if (tabWidth <= 0) return s
        val out = StringBuilder(s.length)
        var col = 0
        for (ch in s) {
            if (ch == '\t') {
                val spaces = tabWidth - (col % tabWidth)
                repeat(spaces) { out.append(' ') }
                col += spaces
            } else {
                out.append(ch)
                if (ch == '\n') col = 0 else col += Width.charWidth(ch.code)
            }
        }
        return out.toString()
    }
}