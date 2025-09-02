package style

import ir.*

object Styler {
    fun styleBlock(block: Block, theme: Theme): List<StyledSpan> {
        return when (val k = block.kind) {
            is BlockKind.Heading -> styleInlines(block.inlines, theme, inHeading = true)
            is BlockKind.Paragraph -> styleInlines(block.inlines, theme, inHeading = false)
            is BlockKind.CodeBlock -> listOf(StyledSpan(k.text, styleForCode(theme)))
            is BlockKind.Image -> listOf(StyledSpan(k.alt, baseStyle(theme)))
            else -> styleInlines(block.inlines, theme, inHeading = false)
        }
    }

    private fun baseStyle(theme: Theme): Style {
        return when (theme.mode) {
            ThemeMode.NoColor -> Style()
            else -> Style(fg = theme.text)
        }
    }

    private fun styleForHeading(theme: Theme): Style {
        return when (theme.mode) {
            ThemeMode.NoColor -> Style()
            else -> Style(fg = theme.heading, bold = true)
        }
    }

    private fun styleForLink(theme: Theme): Style {
        return when (theme.mode) {
            ThemeMode.NoColor -> Style()
            else -> Style(fg = theme.link, underline = true)
        }
    }

    private fun styleForCode(theme: Theme): Style {
        return when (theme.mode) {
            ThemeMode.NoColor -> Style()
            else -> Style(fg = theme.code)
        }
    }

    private fun styleForEmph(theme: Theme): Style {
        return when (theme.mode) {
            ThemeMode.NoColor -> Style()
            else -> Style(fg = theme.text, underline = true)
        }
    }

    private fun styleForStrong(theme: Theme): Style {
        return when (theme.mode) {
            ThemeMode.NoColor -> Style()
            else -> Style(fg = theme.text, bold = true)
        }
    }

    fun styleInlines(inlines: List<Inline>, theme: Theme, inHeading: Boolean): List<StyledSpan> {
        val spans = ArrayList<StyledSpan>()
        val headStyle = if (inHeading) styleForHeading(theme) else baseStyle(theme)

        fun addText(text: String, style: Style) {
            if (text.isEmpty()) return
            if (theme.mode == ThemeMode.NoColor) {
                spans.add(StyledSpan(text, Style()))
            } else {
                spans.add(StyledSpan(text, style))
            }
        }

        fun walk(list: List<Inline>, inherited: Style) {
            for (node in list) {
                when (node) {
                    is Inline.Text -> addText(node.text, inherited)
                    is Inline.Code -> addText(node.code, styleForCode(theme))
                    is Inline.Emph -> walk(node.children, mergeStyles(inherited, styleForEmph(theme)))
                    is Inline.Strong -> walk(node.children, mergeStyles(inherited, styleForStrong(theme)))
                    is Inline.Link -> walk(node.children, mergeStyles(inherited, styleForLink(theme)))
                    Inline.SoftBreak -> addText(" ", inherited)
                    Inline.HardBreak -> addText("\n", inherited)
                }
            }
        }

        walk(inlines, headStyle)
        return spans
    }

    private fun mergeStyles(a: Style, b: Style): Style {
        return Style(
            fg = b.fg ?: a.fg,
            bold = a.bold || b.bold,
            underline = a.underline || b.underline,
        )
    }
}

