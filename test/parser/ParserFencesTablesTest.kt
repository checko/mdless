import parser.Parser
import ir.*
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) { println("ASSERT FAIL: $msg"); exitProcess(1) }
}

fun main() {
    val md = """
        ```kotlin
        fun main() {}
        ```

        | A | B |
        |---|:--:|
        | x | y |
        | p | q |
    """.trimIndent()

    val all = Parser.parseMarkdown(md)
    val blocks = all.filter { it.kind !is BlockKind.Blank }
    assertTrue(blocks.size == 2, "expected code block + table")

    val code = blocks[0].kind as BlockKind.CodeBlock
    assertTrue(code.language == "kotlin", "code lang kotlin")
    assertTrue(code.text.trim() == "fun main() {}", "code content")

    val table = blocks[1].kind as BlockKind.Table
    assertTrue(table.rows.size == 3, "table rows include header + 2 body")
    assertTrue(table.rows[0] == listOf("A","B"), "header row")
    assertTrue(table.aligns == listOf(ColAlign.Left, ColAlign.Center), "aligns parsed")

    println("PARSER FENCES/TABLES TEST OK")
}
