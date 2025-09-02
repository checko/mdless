package style

import ir.AnsiColor

enum class ThemeMode { Light, Dark, NoColor }

data class Theme(
    val mode: ThemeMode,
    val text: AnsiColor,
    val heading: AnsiColor,
    val link: AnsiColor,
    val code: AnsiColor,
    val quote: AnsiColor,
)

object Themes {
    val DARK = Theme(
        mode = ThemeMode.Dark,
        text = AnsiColor.WHITE,
        heading = AnsiColor.CYAN,
        link = AnsiColor.BLUE,
        code = AnsiColor.YELLOW,
        quote = AnsiColor.GREEN,
    )
    val LIGHT = Theme(
        mode = ThemeMode.Light,
        text = AnsiColor.BLACK,
        heading = AnsiColor.BLUE,
        link = AnsiColor.MAGENTA,
        code = AnsiColor.RED,
        quote = AnsiColor.GREEN,
    )
    val NOCOLOR = Theme(
        mode = ThemeMode.NoColor,
        text = AnsiColor.DEFAULT,
        heading = AnsiColor.DEFAULT,
        link = AnsiColor.DEFAULT,
        code = AnsiColor.DEFAULT,
        quote = AnsiColor.DEFAULT,
    )
}

