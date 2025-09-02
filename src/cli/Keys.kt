package cli

enum class KeyCmd { Down, Up, PageDown, PageUp, Top, Bottom, Quit, SearchForward, SearchBackward, SearchNext, SearchPrev, None }

object KeyMap {
    fun fromChar(c: Char): KeyCmd = when (c) {
        'j' -> KeyCmd.Down
        'k' -> KeyCmd.Up
        ' ' -> KeyCmd.PageDown
        'b' -> KeyCmd.PageUp
        'g' -> KeyCmd.Top
        'G' -> KeyCmd.Bottom
        '/' -> KeyCmd.SearchForward
        '?' -> KeyCmd.SearchBackward
        'n' -> KeyCmd.SearchNext
        'N' -> KeyCmd.SearchPrev
        'q' -> KeyCmd.Quit
        else -> KeyCmd.None
    }
}
