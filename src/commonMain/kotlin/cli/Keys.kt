package cli

import tty.Tty

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

    // Parse keys including arrows and page keys.
    // If initial >= 0, use it; otherwise, read.
    fun readCmd(initial: Int? = null): KeyCmd {
        val first = initial ?: Tty.readByte()
        if (first < 0) return KeyCmd.None
        if (first == 27) { // ESC
            val b1 = Tty.readByte()
            if (b1.toChar() == '[') {
                val b2 = Tty.readByte()
                return when (b2.toChar()) {
                    'A' -> KeyCmd.Up      // Up arrow
                    'B' -> KeyCmd.Down    // Down arrow
                    'C' -> KeyCmd.PageDown // Right as page down (optional)
                    'D' -> KeyCmd.PageUp   // Left as page up (optional)
                    '5' -> { // PageUp: ESC [ 5 ~
                        // read until '~'
                        while (true) { val b = Tty.readByte(); if (b.toChar() == '~') break }
                        KeyCmd.PageUp
                    }
                    '6' -> {
                        while (true) { val b = Tty.readByte(); if (b.toChar() == '~') break }
                        KeyCmd.PageDown
                    }
                    else -> KeyCmd.None
                }
            }
            return KeyCmd.None
        }
        return fromChar(first.toChar())
    }
}
