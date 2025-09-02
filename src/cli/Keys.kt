package cli

enum class KeyCmd { Down, Up, PageDown, PageUp, Top, Bottom, Quit, None }

object KeyMap {
    fun fromChar(c: Char): KeyCmd = when (c) {
        'j' -> KeyCmd.Down
        'k' -> KeyCmd.Up
        ' ' -> KeyCmd.PageDown
        'b' -> KeyCmd.PageUp
        'g' -> KeyCmd.Top
        'G' -> KeyCmd.Bottom
        'q' -> KeyCmd.Quit
        else -> KeyCmd.None
    }
}

