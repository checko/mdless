import style.ThemeMode

data class Options(
    val themeMode: ThemeMode = ThemeMode.Dark,
)

internal fun parseOptions(argv: Array<String>): Pair<Options, List<String>> {
    var mode: ThemeMode = ThemeMode.Dark
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
    return Options(themeMode = mode) to rest
}

