import style.ThemeMode

data class Options(
    val themeMode: ThemeMode = ThemeMode.Dark,
    val width: Int? = null,
)

internal fun parseOptions(argv: Array<String>): Pair<Options, List<String>> {
    var mode: ThemeMode = ThemeMode.Dark
    var width: Int? = null
    val rest = ArrayList<String>()
    var i = 0
    while (i < argv.size) {
        val a = argv[i]
        when (a) {
            "--theme" -> {
                val v = argv.getOrNull(i + 1)
                if (v != null) {
                    when (v.lowercase()) {
                        "dark" -> mode = ThemeMode.Dark
                        "light" -> mode = ThemeMode.Light
                        "no-color", "nocolor", "none" -> mode = ThemeMode.NoColor
                        else -> {
                            println("Unknown theme '$v', using dark")
                            mode = ThemeMode.Dark
                        }
                    }
                    i += 2
                    continue
                } else {
                    println("--theme requires a value: dark|light|no-color")
                    i++
                    continue
                }
            }
            "--width" -> {
                val v = argv.getOrNull(i + 1)
                width = v?.toIntOrNull()
                if (width == null) println("--width requires an integer value")
                i += if (v != null) 2 else 1
                continue
            }
            "-h", "--help" -> {
                // Help is handled in main; we pass it through as positional to allow tests
                rest += a
                i++
            }
            else -> {
                rest += a
                i++
            }
        }
    }
    return Options(themeMode = mode, width = width) to rest
}
