import ir.*
import layout.Layout
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) {
        println("ASSERT FAIL: $msg")
        exitProcess(1)
    }
}

private fun linesText(lines: List<LayoutLine>): List<String> =
    lines.map { it.spans.joinToString("") { s -> s.text } }

private fun normalizeSpaces(s: String): String = s.trim().replace(Regex("\\s+"), " ")

fun main() {
    val txt = "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs."
    val block = Block(
        id = IdGen.next(),
        kind = BlockKind.Paragraph,
        inlines = listOf(Inline.Text(txt))
    )

    val w20 = Layout.layoutBlock(block, width = 20)
    val t20 = linesText(w20)
    // None exceed width 20
    for (ln in t20) {
        assertTrue(ln.length <= 20, "line > 20: '$ln'")
    }
    // Reconstruct normalized text should match
    val roundtrip20 = normalizeSpaces(t20.joinToString(" "))
    assertTrue(roundtrip20 == normalizeSpaces(txt), "roundtrip 20 failed")

    val w40 = Layout.layoutBlock(block, width = 40)
    val t40 = linesText(w40)
    for (ln in t40) {
        assertTrue(ln.length <= 40, "line > 40: '$ln'")
    }
    val roundtrip40 = normalizeSpaces(t40.joinToString(" "))
    assertTrue(roundtrip40 == normalizeSpaces(txt), "roundtrip 40 failed")

    println("LAYOUT PARA TEST OK")
}

