import layout.Width
import kotlin.system.exitProcess

private fun assertEq(a: Any, b: Any, msg: String) {
    if (a != b) {
        println("ASSERT FAIL: $msg (got=$a expected=$b)")
        exitProcess(1)
    }
}

fun main() {
    assertEq(Width.charWidth('A'.code), 1, "ASCII A width")
    assertEq(Width.charWidth(' '.code), 1, "space width")
    assertEq(Width.stringWidth("abc"), 3, "abc width")
    assertEq(Width.stringWidth(""), 0, "empty width")
    // non-ASCII width via wcwidth (typically 2 for CJK)
    val w = Width.stringWidth("你")
    if (w < 1) {
        println("ASSERT FAIL: non-ASCII width should be >=1 (got=$w)")
        exitProcess(1)
    }
    println("WIDTH ASCII TEST OK")
}
