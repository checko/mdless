import ir.*
import layout.Layout
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) { println("ASSERT FAIL: $msg"); exitProcess(1) }
}

private fun linesText(lines: List<LayoutLine>): List<String> =
    lines.map { it.spans.joinToString("") { s -> s.text } }

fun main() {
    // Unordered list with two items and a nested paragraph in second item
    val item1 = ListItem(blocks = listOf(Block(IdGen.next(), BlockKind.Paragraph, listOf(Inline.Text("one")))))
    val item2 = ListItem(blocks = listOf(Block(IdGen.next(), BlockKind.Paragraph, listOf(Inline.Text("two")))))
    val listBlock = Block(IdGen.next(), BlockKind.ListBlock(ordered = false, items = listOf(item1, item2)))

    val lns = Layout.layoutBlock(listBlock, width = 20)
    val txt = linesText(lns)
    assertTrue(txt[0].startsWith("- one"), "first bullet text")
    assertTrue(txt[1].startsWith("- two"), "second bullet text")

    // Blockquote wrapping
    val quoteChild = Block(IdGen.next(), BlockKind.Paragraph, listOf(Inline.Text("quoted text here")))
    val quote = Block(IdGen.next(), BlockKind.Blockquote(children = listOf(quoteChild)))
    val qLines = Layout.layoutBlock(quote, width = 10)
    val qText = linesText(qLines)
    assertTrue(qText.all { it.startsWith("> ") }, "quote prefix per line")

    println("LAYOUT LIST/QUOTE TEST OK")
}

