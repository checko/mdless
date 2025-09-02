import ir.*
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) {
        System.err.println("ASSERT FAIL: $msg")
        exitProcess(1)
    }
}

fun main() {
    // Create a paragraph block with text and code inline
    val b1 = Block(
        id = IdGen.next(),
        kind = BlockKind.Paragraph,
        inlines = listOf(
            Inline.Text("Hello "),
            Inline.Code("world"),
            Inline.Text("!"),
        )
    )

    // Heading level 2
    val b2 = Block(
        id = IdGen.next(),
        kind = BlockKind.Heading(2),
        inlines = listOf(Inline.Text("Title"))
    )

    // Simple table
    val table = Block(
        id = IdGen.next(),
        kind = BlockKind.Table(
            rows = listOf(listOf("a", "b"), listOf("c", "d")),
            aligns = listOf(ColAlign.Left, ColAlign.Right)
        )
    )

    val ids = setOf(b1.id, b2.id, table.id)
    assertTrue(ids.size == 3, "block ids should be unique")

    assertTrue(b1.inlines.isNotEmpty(), "paragraph should have inlines")
    assertTrue((b2.kind as BlockKind.Heading).level == 2, "heading level should be 2")

    println("IR TEST OK")
    exitProcess(0)
}

