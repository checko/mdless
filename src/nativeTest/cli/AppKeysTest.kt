import cli.App
import cli.KeyCmd
import pager.BlockLines
import pager.PagerState
import kotlin.system.exitProcess

private fun assertEq(a: Any, b: Any, msg: String) {
    if (a != b) {
        println("ASSERT FAIL: $msg (got=$a expected=$b)")
        exitProcess(1)
    }
}

fun main() {
    val blocks = listOf(BlockLines(1, 50))
    val pager = PagerState(blocks, height = 10)

    // j j SPACE -> 1 + 1 + 10 = 12
    App.handleKey(pager, KeyCmd.Down)
    App.handleKey(pager, KeyCmd.Down)
    App.handleKey(pager, KeyCmd.PageDown)
    assertEq(pager.viewportRange().first, 12, "after j j SPACE")

    // b -> page up to 2
    App.handleKey(pager, KeyCmd.PageUp)
    assertEq(pager.viewportRange().first, 2, "after b")

    // g -> top 0
    App.handleKey(pager, KeyCmd.Top)
    assertEq(pager.viewportRange().first, 0, "after g")

    // G -> bottom total-height = 40
    App.handleKey(pager, KeyCmd.Bottom)
    assertEq(pager.viewportRange().first, 40, "after G")

    println("APP KEYS TEST OK")
}

