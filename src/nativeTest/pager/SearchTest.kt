import pager.Search
import ir.*
import kotlin.system.exitProcess

private fun assertEq(a: Any?, b: Any?, msg: String) {
    if (a != b) {
        println("ASSERT FAIL: $msg (got=$a expected=$b)")
        exitProcess(1)
    }
}

fun main() {
    val lines = listOf(
        LayoutLine(listOf(StyledSpan("Hello world", Style())), 1, 0),
        LayoutLine(listOf(StyledSpan("The quick brown fox", Style())), 2, 0),
        LayoutLine(listOf(StyledSpan("jumps over the lazy dog", Style())), 2, 1),
    )

    val n1 = Search.findNext(lines, "quick", -1)
    assertEq(n1, 1, "findNext from start")

    val n2 = Search.findNext(lines, "dog", 1)
    assertEq(n2, 2, "findNext after line 1")

    val p1 = Search.findPrev(lines, "hello", 2)
    assertEq(p1, 0, "findPrev back to line 0")

    val miss = Search.findNext(lines, "xyz", -1)
    assertEq(miss, null, "findNext miss")

    println("SEARCH TEST OK")
}

