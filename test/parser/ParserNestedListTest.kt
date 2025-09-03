import parser.Parser
import ir.*
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) { println("ASSERT FAIL: $msg"); exitProcess(1) }
}

fun main() {
    val md = """
        - parent
          - child1
          - child2
        - parent2
    """.trimIndent()

    val blocks = Parser.parseMarkdown(md)
    assertTrue(blocks.size == 1, "single top-level list")
    val list = blocks[0].kind as BlockKind.ListBlock
    assertTrue(list.items.size == 2, "two top-level items")

    val item1 = list.items[0]
    assertTrue(item1.blocks.size == 2, "first item has para + nested list")
    val para1 = item1.blocks[0]
    val text1 = (para1.inlines[0] as Inline.Text).text
    assertTrue(text1 == "parent", "parent text")
    val nested = item1.blocks[1].kind as BlockKind.ListBlock
    assertTrue(nested.items.size == 2, "two nested items")
    val child1 = (nested.items[0].blocks[0].inlines[0] as Inline.Text).text
    assertTrue(child1 == "child1", "child1 text")

    println("PARSER NESTED LIST TEST OK")
}

