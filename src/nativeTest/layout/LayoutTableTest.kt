import ir.*
import layout.Layout
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) { println("ASSERT FAIL: $msg"); exitProcess(1) }
}

fun main() {
    val rows = listOf(
        listOf("Header A", "Header B", "Header C"),
        listOf("alpha", "beta gamma", "delta"),
        listOf("longlonglongword", "x", "y"),
    )
    val aligns = listOf(ColAlign.Left, ColAlign.Center, ColAlign.Right)
    val table = Block(
        id = IdGen.next(),
        kind = BlockKind.Table(rows = rows, aligns = aligns)
    )
    val width = 24
    val lines = Layout.layoutBlock(table, width)
    for (ln in lines) {
        val s = ln.spans.joinToString("") { it.text }
        assertTrue(s.length <= width, "table line exceeds width: '${s}'")
    }
    println("LAYOUT TABLE TEST OK")
}

