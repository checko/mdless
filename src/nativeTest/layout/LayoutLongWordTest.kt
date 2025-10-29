import ir.*
import layout.Layout
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) { println("ASSERT FAIL: $msg"); exitProcess(1) }
}

fun main() {
    val long = "abcdefghijklmnopqrstuvwxyz"
    val block = Block(IdGen.next(), BlockKind.Paragraph, listOf(Inline.Text(long)))
    val lines = Layout.layoutBlock(block, width = 10)
    assertTrue(lines.all { it.spans.joinToString("") { s -> s.text }.length <= 10 }, "hard wrap long token to width")
    println("LAYOUT LONG WORD TEST OK")
}

