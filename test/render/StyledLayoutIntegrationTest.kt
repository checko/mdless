import ir.*
import layout.LayoutStyled
import render.Renderer
import style.*
import kotlin.system.exitProcess

private fun assertTrue(cond: Boolean, msg: String) {
    if (!cond) {
        println("ASSERT FAIL: $msg")
        exitProcess(1)
    }
}

fun main() {
    val heading = Block(
        id = IdGen.next(),
        kind = BlockKind.Heading(2),
        inlines = listOf(
            Inline.Text("This is a long heading that wraps")
        )
    )

    val para = Block(
        id = IdGen.next(),
        kind = BlockKind.Paragraph,
        inlines = listOf(
            Inline.Text("Visit "),
            Inline.Link(children = listOf(Inline.Text("example")), url = "https://example.com"),
            Inline.Text(" and use "),
            Inline.Code("code"),
            Inline.Text(" now."),
        )
    )

    val theme = Themes.DARK
    val hLines = LayoutStyled.layoutBlock(heading, width = 12, theme = theme)
    val pLines = LayoutStyled.layoutBlock(para, width = 40, theme = theme)
    val out = Renderer.render(hLines + pLines, enableColor = true)
    // Debug prints can be enabled when diagnosing failures

    // Expect heading style (bold + cyan) applied on each wrapped heading line
    val headingSgr = "\u001B[1;36m"
    val headingOccurrences = out.windowed(headingSgr.length, 1).count { it == headingSgr }
    assertTrue(headingOccurrences >= hLines.size, "heading SGR should appear on each heading line")

    // Link underline + blue present for the word 'example'
    assertTrue(out.contains("\u001B[4;34mexample "), "link styling underline+blue should render")

    // Code colored (yellow) for the word 'code'
    assertTrue(out.contains("\u001B[33mcode "), "code styling yellow should render")

    println("STYLED LAYOUT INTEGRATION TEST OK")
}
