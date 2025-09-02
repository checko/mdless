import parser.Parser
import ir.*
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) {
        println("ASSERT FAIL: $msg")
        exitProcess(1)
    }
}

private fun kindName(b: Block): String = when (val k = b.kind) {
    is BlockKind.Heading -> "Heading(${k.level})"
    is BlockKind.Paragraph -> "Paragraph"
    is BlockKind.ThematicBreak -> "HR"
    is BlockKind.CodeBlock -> "CodeBlock"
    is BlockKind.Blockquote -> "Blockquote"
    is BlockKind.ListBlock -> "List"
    is BlockKind.Table -> "Table"
    is BlockKind.Image -> "Image"
}

private fun inlineSig(inline: Inline): String = when (inline) {
    is Inline.Text -> "T(${inline.text})"
    is Inline.Code -> "C(${inline.code})"
    is Inline.Emph -> "E(${inline.children.joinToString("") { inlineSig(it) }})"
    is Inline.Strong -> "S(${inline.children.joinToString("") { inlineSig(it) }})"
    is Inline.Link -> "L(${inline.children.joinToString("") { inlineSig(it) }})" // not used yet
    Inline.SoftBreak -> "SB"
    Inline.HardBreak -> "HB"
}

fun main() {
    val md = """
        ## Title

        Paragraph with `code` and *emph* and **strong**.

        ---

        Another paragraph.
    """.trimIndent()

    val blocks = Parser.parseMarkdown(md)
    assertTrue(blocks.size == 4, "expected 4 blocks, got ${blocks.size}")

    // 1) Heading H2 with text "Title"
    val b0 = blocks[0]
    assertTrue(kindName(b0) == "Heading(2)", "first block should be Heading(2)")
    val hText = (b0.inlines.singleOrNull() as? Inline.Text)?.text
    assertTrue(hText == "Title", "heading text should be 'Title' got '$hText'")

    // 2) Paragraph with inline code/emph/strong
    val b1 = blocks[1]
    assertTrue(kindName(b1) == "Paragraph", "second block should be Paragraph")
    val sig = b1.inlines.joinToString("|") { inlineSig(it) }
    // Ensure presence/order of key parts
    val flatText = buildString {
        fun walk(list: List<Inline>) {
            for (n in list) when (n) {
                is Inline.Text -> append(n.text)
                is Inline.Code -> append(n.code)
                is Inline.Emph -> walk(n.children)
                is Inline.Strong -> walk(n.children)
                is Inline.Link -> walk(n.children)
                Inline.SoftBreak -> append(' ')
                Inline.HardBreak -> append('\n')
            }
        }
        walk(b1.inlines)
    }
    assertTrue(flatText.contains("Paragraph with"), "paragraph text chunk missing")
    assertTrue(sig.contains("C(code)"), "inline code missing")
    assertTrue(sig.contains("E(T(emph))"), "emphasis missing")
    assertTrue(sig.contains("S(T(strong))"), "strong missing")

    // 3) HR
    val b2 = blocks[2]
    assertTrue(kindName(b2) == "HR", "third block should be HR")

    // 4) Paragraph
    val b3 = blocks[3]
    assertTrue(kindName(b3) == "Paragraph", "fourth block should be Paragraph")
    val p2 = (b3.inlines.singleOrNull() as? Inline.Text)?.text
    assertTrue(p2 == "Another paragraph.", "paragraph text mismatch: '$p2'")

    println("PARSER BASIC TEST OK")
}
