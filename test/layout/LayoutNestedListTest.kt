import ir.*
import layout.Layout
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) { println("ASSERT FAIL: $msg"); exitProcess(1) }
}

private fun linesText(lines: List<LayoutLine>): List<String> =
    lines.map { it.spans.joinToString("") { s -> s.text } }

fun main() {
    // parent
    val child1 = ListItem(blocks = listOf(Block(IdGen.next(), BlockKind.Paragraph, listOf(Inline.Text("parent")))))
    // nested list as second item block
    val nestedItem = ListItem(blocks = listOf(Block(IdGen.next(), BlockKind.Paragraph, listOf(Inline.Text("child")))))
    val nestedListBlock = Block(IdGen.next(), BlockKind.ListBlock(ordered = false, items = listOf(nestedItem)))
    val child2 = ListItem(blocks = listOf(nestedListBlock))
    val listBlock = Block(IdGen.next(), BlockKind.ListBlock(ordered = false, items = listOf(child1, child2)))

    val lns = Layout.layoutBlock(listBlock, width = 20)
    val txt = linesText(lns)
    // First line: top-level bullet
    assertTrue(txt[0].startsWith("- parent"), "top-level bullet")
    // Second line: nested bullet with two-space indent
    assertTrue(txt[1].startsWith("  - child"), "nested bullet indentation")

    println("LAYOUT NESTED LIST TEST OK")
}

