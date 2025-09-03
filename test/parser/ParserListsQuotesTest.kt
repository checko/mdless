import parser.Parser
import ir.*
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) { println("ASSERT FAIL: $msg"); exitProcess(1) }
}

fun main() {
    val md = """
        - one
        - two

        1. first
        2. second

        > quoted line
        > second line
    """.trimIndent()

    val blocks = Parser.parseMarkdown(md)
    assertTrue(blocks.size == 3, "expected 3 blocks")

    val b0 = blocks[0].kind as BlockKind.ListBlock
    assertTrue(!b0.ordered && b0.items.size == 2, "unordered list 2 items")
    val t0 = (b0.items[0].blocks[0].inlines[0] as Inline.Text).text
    assertTrue(t0 == "one", "first bullet text")

    val b1 = blocks[1].kind as BlockKind.ListBlock
    assertTrue(b1.ordered && b1.items.size == 2, "ordered list 2 items")
    val t1 = (b1.items[1].blocks[0].inlines[0] as Inline.Text).text
    assertTrue(t1 == "second", "second ordered text")

    val b2 = blocks[2].kind as BlockKind.Blockquote
    assertTrue(b2.children.size == 1, "blockquote single paragraph child")
    val para = b2.children[0]
    val concat = para.inlines.joinToString("") { when (it) {
        is Inline.Text -> it.text
        Inline.SoftBreak -> " "
        else -> ""
    }}
    assertTrue(concat.contains("quoted line") && concat.contains("second line"), "blockquote content present")

    println("PARSER LISTS/QUOTES TEST OK")
}

