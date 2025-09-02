package layout

object Width {
    fun charWidth(c: Int): Int {
        return when (c) {
            in 0..31 -> 0 // control; treat as zero width (we don't expect them in content)
            9 -> 1 // tab handled at layout stage later; placeholder width
            in 32..126 -> 1 // printable ASCII
            else -> 1 // fallback for now (non-ASCII)
        }
    }

    fun stringWidth(s: String): Int {
        var w = 0
        for (ch in s) {
            w += charWidth(ch.code)
        }
        return w
    }
}

