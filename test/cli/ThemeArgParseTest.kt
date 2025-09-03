import style.*

// Access parseOptions and Options from src/cli/Options.kt

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) {
        println("ASSERT FAIL: $msg")
        kotlin.system.exitProcess(1)
    }
}

fun main() {
    val (o1, rest1) = parseOptions(arrayOf("--theme", "light", "FILE.md"))
    assertTrue(o1.themeMode == ThemeMode.Light, "theme light parsed")
    assertTrue(rest1 == listOf("FILE.md"), "positionals preserved")

    val (o2, rest2) = parseOptions(arrayOf("--theme", "no-color"))
    assertTrue(o2.themeMode == ThemeMode.NoColor, "theme no-color parsed")
    assertTrue(rest2.isEmpty(), "no positionals")

    val (o3, _) = parseOptions(arrayOf("--theme", "unknown"))
    // Unknown falls back to Dark
    assertTrue(o3.themeMode == ThemeMode.Dark, "unknown theme falls back to dark")

    println("THEME ARG PARSE TEST OK")
}

