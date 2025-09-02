package parser

import ir.*

object Parser {
    fun parseMarkdown(src: String): List<Block> {
        val lines = src.replace("\r\n", "\n").replace('\r', '\n').split("\n")
        val blocks = ArrayList<Block>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            // Skip leading empty lines
            if (line.isBlank()) { i++; continue }

            // Heading: #{1,6} + space
            val headingMatch = Regex("^(#{1,6})\\s+(.*)").find(line)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val text = headingMatch.groupValues[2]
                val inlines = parseInlines(text)
                blocks += Block(IdGen.next(), BlockKind.Heading(level), inlines)
                i++
                continue
            }

            // Thematic break: --- or *** (>=3), possibly with spaces
            if (isThematicBreak(line)) {
                blocks += Block(IdGen.next(), BlockKind.ThematicBreak)
                i++
                continue
            }

            // Paragraph: accumulate until blank line or until next block marker
            val paraLines = ArrayList<String>()
            while (i < lines.size) {
                val l = lines[i]
                if (l.isBlank()) break
                if (isThematicBreak(l)) break
                if (Regex("^#{1,6}\\s+").containsMatchIn(l)) break
                paraLines += l
                i++
            }
            val paraInlines = ArrayList<Inline>()
            for ((idx, pl) in paraLines.withIndex()) {
                val parts = parseInlines(pl)
                paraInlines.addAll(parts)
                if (idx != paraLines.lastIndex) paraInlines.add(Inline.SoftBreak)
            }
            blocks += Block(IdGen.next(), BlockKind.Paragraph, paraInlines)
            // Skip trailing blank lines after paragraph
            while (i < lines.size && lines[i].isBlank()) i++
        }
        return blocks
    }

    private fun isThematicBreak(line: String): Boolean {
        val t = line.trim()
        if (t.length < 3) return false
        val allDash = t.all { it == '-' }
        val allStar = t.all { it == '*' }
        return (allDash || allStar) && t.length >= 3
    }

    // Naive inline parser: handles code ``, **strong**, *emph*, leaves others as text.
    private fun parseInlines(text: String): List<Inline> {
        val out = ArrayList<Inline>()
        var i = 0
        fun flushText(buf: StringBuilder) {
            if (buf.isNotEmpty()) {
                out += Inline.Text(buf.toString())
                buf.clear()
            }
        }
        val buf = StringBuilder()
        while (i < text.length) {
            val ch = text[i]
            // Inline code
            if (ch == '`') {
                flushText(buf)
                val end = text.indexOf('`', startIndex = i + 1)
                if (end > i + 1) {
                    out += Inline.Code(text.substring(i + 1, end))
                    i = end + 1
                    continue
                } else {
                    buf.append(ch); i++; continue
                }
            }
            // Strong **text**
            if (ch == '*' && i + 1 < text.length && text[i + 1] == '*') {
                flushText(buf)
                val end = text.indexOf("**", startIndex = i + 2)
                if (end >= 0) {
                    val inner = parseInlines(text.substring(i + 2, end))
                    out += Inline.Strong(inner)
                    i = end + 2
                    continue
                } else {
                    buf.append("**"); i += 2; continue
                }
            }
            // Emph *text*
            if (ch == '*') {
                flushText(buf)
                val end = text.indexOf('*', startIndex = i + 1)
                if (end > i + 1) {
                    val inner = parseInlines(text.substring(i + 1, end))
                    out += Inline.Emph(inner)
                    i = end + 1
                    continue
                } else {
                    buf.append('*'); i++; continue
                }
            }
            // default
            buf.append(ch)
            i++
        }
        flushText(buf)
        return out
    }
}

