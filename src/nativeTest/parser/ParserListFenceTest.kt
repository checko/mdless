import parser.Parser
import ir.*
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) { println("ASSERT FAIL: $msg"); exitProcess(1) }
}

fun main() {
    val md = """
        1) Step

           ```bash
           echo hi
           echo bye
           ```

        2) Done
    """.trimIndent()

    val blocks = Parser.parseMarkdown(md).filter { it.kind !is BlockKind.Blank }
    assertTrue(blocks.size == 1, "expected one top-level list block")
    val list = blocks[0].kind as? BlockKind.ListBlock ?: run {
        println("ASSERT FAIL: first block not list")
        exitProcess(1)
    }
    assertTrue(list.ordered && list.items.size == 2, "ordered list with two items")

    val firstItem = list.items[0]
    assertTrue(firstItem.blocks.size == 2, "first item should have paragraph + code block")

    val para = firstItem.blocks[0]
    val text = para.inlines.joinToString("") {
        when (it) {
            is Inline.Text -> it.text
            Inline.SoftBreak -> " "
            else -> ""
        }
    }
    assertTrue(text == "Step", "paragraph text preserved")

    val code = firstItem.blocks[1].kind as? BlockKind.CodeBlock ?: run {
        println("ASSERT FAIL: second block not code block")
        exitProcess(1)
    }
    assertTrue(code.language == "bash", "code language parsed")
    assertTrue(code.text == "echo hi\necho bye", "code content stripped of list indent")

    println("PARSER LIST FENCE TEST OK")
}
