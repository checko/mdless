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
    // non-ASCII fallback currently 1 per code unit
    assertEq(Width.stringWidth("你"), 1, "fallback width")
    println("WIDTH ASCII TEST OK")
}

