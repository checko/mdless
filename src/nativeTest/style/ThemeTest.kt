import ir.*
import style.*
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) {
        println("ASSERT FAIL: $msg")
        exitProcess(1)
    }
}

private fun hasStyle(spans: List<StyledSpan>, predicate: (Style) -> Boolean): Boolean =
    spans.any { predicate(it.style) }

fun main() {
    val themeDark = Themes.DARK
    val themeNo = Themes.NOCOLOR

    val heading = Block(
        id = IdGen.next(),
        kind = BlockKind.Heading(2),
        inlines = listOf(
            Inline.Text("Doc "),
            Inline.Strong(listOf(Inline.Text("Title")))
        )
    )

    val para = Block(
        id = IdGen.next(),
        kind = BlockKind.Paragraph,
        inlines = listOf(
            Inline.Text("Visit "),
            Inline.Link(children = listOf(Inline.Text("site")), url = "https://example.com"),
            Inline.Text(" and use "),
            Inline.Code("code"),
        )
    )

    val hSpans = Styler.styleBlock(heading, themeDark)
    assertTrue(hasStyle(hSpans) { it.bold }, "heading should include bold style")
    assertTrue(hasStyle(hSpans) { it.fg == themeDark.heading }, "heading color should be applied")

    val pSpans = Styler.styleBlock(para, themeDark)
    assertTrue(hasStyle(pSpans) { it.underline }, "link should be underlined")
    assertTrue(hasStyle(pSpans) { it.fg == themeDark.link }, "link color should be applied")
    assertTrue(hasStyle(pSpans) { it.fg == themeDark.code }, "code color should be applied")

    val pNo = Styler.styleBlock(para, themeNo)
    // In no-color mode, styles collapse
    assertTrue(pNo.all { it.style == Style() }, "no-color mode should have default styles only")

    println("THEME TEST OK")
}

