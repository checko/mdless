package layout

actual object Width {
    actual var usePosix: Boolean = true

    actual fun charWidth(cp: Int): Int {
        // cp is a Unicode code point, not UTF-16 unit.
        if (cp == 9) return 1 // tabs expanded earlier
        if (cp < 32) return 0 // control
        if (!usePosix) return when (cp) {
            in 32..126 -> 1
            else -> 1
        }
        val w = platform.posix.wcwidth(cp)
        return if (w < 0) 1 else w
    }

    actual fun stringWidth(s: String): Int {
        var w = 0
        var i = 0
        val n = s.length
        while (i < n) {
            val cp = nextCodePoint(s, i)
            w += charWidth(cp.first)
            i = cp.second
        }
        return w
    }

    // Return (codePoint, nextIndex) from a UTF-16 string, not splitting surrogate pairs.
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

    // Returns the end index (exclusive) of the longest prefix of s whose display width <= maxCols.
    actual fun takePrefixByColumns(s: String, maxCols: Int): Int {
        if (maxCols <= 0) return 0
        var cols = 0
        var i = 0
        val n = s.length
        while (i < n) {
            val (cp, next) = nextCodePoint(s, i)
            val cw = charWidth(cp)
            if (cols + cw > maxCols) break
            cols += cw
            i = next
        }
        return i
    }
}