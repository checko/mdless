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
    val line = LayoutLine(
        spans = listOf(
            StyledSpan("Hello", Style(fg = AnsiColor.CYAN, bold = true)),
            StyledSpan(" world", Style())
        ),
        blockId = 1,
        rowInBlock = 0
    )

    val outColor = Renderer.render(listOf(line), enableColor = true)
    val expectedColor = "\u001B[1;36mHello\u001B[0m world\u001B[0m\n"
    assertEq(outColor, expectedColor, "ANSI color render")

    val outNo = Renderer.render(listOf(line), enableColor = false)
    val expectedNo = "Hello world\n"
    assertEq(outNo, expectedNo, "No-color render")

    println("RENDERER TEST OK")
}
