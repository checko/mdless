import ir.*
import render.Renderer
import kotlin.system.exitProcess

private fun assertEq(a: String, b: String, msg: String) {
    if (a != b) {
        println("ASSERT FAIL: $msg\nGot:    ${escapeVis(a)}\nExpect: ${escapeVis(b)}")
        exitProcess(1)
    }
}

private fun escapeVis(s: String): String = s
    .replace("\u001B", "<ESC>")
    .replace("\n", "\\n")

fun main() {
    // Base line with no color style
    val line = LayoutLine(
        spans = listOf(StyledSpan("abc abc", Style())),
        blockId = 1,
        rowInBlock = 0
    )
    val ranges = listOf(IntRange(0, 2), IntRange(4, 6))
    val out = Renderer.renderWithHighlights(listOf(line), listOf(ranges), enableColor = true)
    val expected = "\u001B[4mabc\u001B[0m \u001B[0m\u001B[4mabc\u001B[0m\n"
    assertEq(out, expected, "Underline highlight rendering")
    println("RENDERER HIGHLIGHT TEST OK")
}

