import pager.BlockLines
import pager.PagerState
import kotlin.system.exitProcess

private fun assertEq(a: Any, b: Any, msg: String) {
    if (a != b) {
        println("ASSERT FAIL: $msg (got=$a expected=$b)")
        exitProcess(1)
    }
}

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) {
        println("ASSERT FAIL: $msg")
        exitProcess(1)
    }
}

fun main() {
    // Doc with 3 blocks: 5,3,10 lines => total 18
    val blocks = listOf(BlockLines(1,5), BlockLines(2,3), BlockLines(3,10))
    val pager = PagerState(blocks, height = 5)

    assertEq(pager.totalLines, 18, "total lines")
    // At top, percent should be 0 when there is scrollable range
    assertEq(pager.percent(), 0, "percent at top")

    // Viewport range initially 0..5
    val r0 = pager.viewportRange()
    assertEq(r0.first, 0, "range start")
    assertEq(r0.last, 4, "range end inclusive")
    val p0 = pager.logicalToPos(0)
    assertEq(p0.blockId, 1, "line 0 block id")
    assertEq(p0.rowInBlock, 0, "line 0 row")

    // Scroll 3 lines
    pager.scrollLines(3)
    assertEq(pager.viewportRange().first, 3, "after scroll 3")
    var pct = pager.percent()
    assertTrue(pct in 1..50, "percent should increase from top ($pct)")

    // Page down 1 page (5)
    pager.scrollPages(1)
    assertEq(pager.viewportRange().first, 8, "after page down")
    val p8 = pager.logicalToPos(8)
    // line 8 is after block0(5) + block1(3) => first line of block3
    assertEq(p8.blockId, 3, "line 8 block id")
    assertEq(p8.rowInBlock, 0, "line 8 row")

    // Jump bottom
    pager.jumpToBottom()
    val rb = pager.viewportRange()
    assertEq(rb.first, 13, "bottom topLine should be total-height=13")
    assertEq(pager.percent(), 100, "percent at bottom")

    // posToLogical roundtrip
    val back = pager.posToLogical(3, 2)
    assertEq(back, 5 + 3 + 2, "posToLogical")

    // Clamp when scrolling beyond bottom
    pager.scrollLines(100)
    assertEq(pager.viewportRange().first, 13, "clamp at bottom after overscroll")

    println("PAGER TEST OK")
}
