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

            // Fenced code block ```lang ... ```
            val fence = fenceLang(line)
            if (fence != null) {
                val lang = fence
                i++
                val buf = StringBuilder()
                var first = true
                while (i < lines.size && !isFenceClose(lines[i])) {
                    if (!first) buf.append('\n') else first = false
                    buf.append(lines[i])
                    i++
                }
                // Skip closing fence if present
                if (i < lines.size && isFenceClose(lines[i])) i++
                blocks += Block(IdGen.next(), BlockKind.CodeBlock(lang.ifEmpty { null }, buf.toString()))
                // skip trailing blanks
                while (i < lines.size && lines[i].isBlank()) i++
                continue
            }

            // Blockquote: lines starting with '>'
            if (line.trimStart().startsWith(">")) {
                val quoteLines = ArrayList<String>()
                while (i < lines.size) {
                    val l = lines[i]
                    if (!l.trimStart().startsWith(">")) break
                    val idx = l.indexOf('>')
                    var content = l.substring(idx + 1)
                    if (content.startsWith(" ")) content = content.drop(1)
                    quoteLines += content
                    i++
                }
                val inner = parseMarkdown(quoteLines.joinToString("\n"))
                blocks += Block(IdGen.next(), BlockKind.Blockquote(inner))
                while (i < lines.size && lines[i].isBlank()) i++
                continue
            }

            // Lists (unordered/ordered) simple top-level
            val listParse = parseList(lines, i)
            if (listParse != null) {
                blocks += listParse.block
                i = listParse.nextIndex
                while (i < lines.size && lines[i].isBlank()) i++
                continue
            }

            // Table: header row + align row
            val tableParse = parseTable(lines, i)
            if (tableParse != null) {
                blocks += tableParse.block
                i = tableParse.nextIndex
                while (i < lines.size && lines[i].isBlank()) i++
                continue
            }

            // Paragraph: accumulate until blank line or until next block marker
            val paraLines = ArrayList<String>()
            while (i < lines.size) {
                val l = lines[i]
                if (l.isBlank()) break
                if (isThematicBreak(l)) break
                if (Regex("^#{1,6}\\s+").containsMatchIn(l)) break
                if (fenceLang(l) != null) break
                if (l.trimStart().startsWith(">")) break
                if (isListMarker(l)) break
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

    private fun fenceLang(line: String): String? {
        val m = Regex("^```\\s*([A-Za-z0-9_+-]*)\\s*$").find(line.trim())
        return m?.groupValues?.getOrNull(1) ?: m?.let { "" }
    }

    private fun isFenceClose(line: String): Boolean = line.trim() == "```"

    private data class ListParse(val block: Block, val nextIndex: Int)

    private fun isListMarker(line: String): Boolean {
        val t = line.trimStart()
        return Regex("^([-+*])\\s+.+").containsMatchIn(t) || Regex("^(\\d+)[\n.).]\\s+.+").containsMatchIn(t)
    }

    private fun parseList(lines: List<String>, start: Int): ListParse? {
        var i = start
        if (i >= lines.size) return null
        val first = lines[i]
        val mUn = Regex("^(\\s*)([-+*])\\s+(.+)").find(first)
        val mOr = Regex("^(\\s*)(\\d+)[.).]\\s+(.+)").find(first)
        if (mUn == null && mOr == null) return null
        val ordered = mOr != null
        val indent = (mUn?.groupValues?.get(1) ?: mOr!!.groupValues[1]).length
        val items = ArrayList<ListItem>()
        fun isItemLine(line: String): Boolean {
            val t = line.trimStart()
            return if (ordered) Regex("^(\\d+)[.).]\\s+.+").containsMatchIn(t) else Regex("^([-+*])\\s+.+").containsMatchIn(t)
        }
        fun extractText(line: String, isOrdered: Boolean): String {
            val mt = if (isOrdered) Regex("^(\\s*)(\\d+)[.).]\\s+(.+)").find(line) else Regex("^(\\s*)([-+*])\\s+(.+)").find(line)
            return (mt?.groupValues?.get(3) ?: line).trimEnd()
        }
        while (i < lines.size) {
            if (lines[i].isBlank()) break
            val t = lines[i].trimStart()
            val curIndent = lines[i].indexOf(t)
            if (curIndent != indent || !isItemLine(lines[i])) break
            // Start new item
            val itemBlocks = ArrayList<Block>()
            // First paragraph content (may grow with continuations)
            val paraLines = ArrayList<String>()
            paraLines += extractText(lines[i], ordered)
            i++
            // Scan item content: nested lists or paragraph continuations
            while (i < lines.size) {
                val l = lines[i]
                if (l.isBlank()) { i++; break }
                val tt = l.trimStart()
                val ind = l.indexOf(tt)
                if (ind < indent) break // list ends
                if (ind == indent && isItemLine(l)) break // next sibling item
                if (ind > indent && isListMarker(l)) {
                    // flush paragraph if any
                    if (paraLines.isNotEmpty()) {
                        val inl = ArrayList<Inline>()
                        for ((idx2, pl) in paraLines.withIndex()) {
                            inl.addAll(parseInlines(pl))
                            if (idx2 != paraLines.lastIndex) inl.add(Inline.SoftBreak)
                        }
                        itemBlocks += Block(IdGen.next(), BlockKind.Paragraph, inl)
                        paraLines.clear()
                    }
                    val nested = parseList(lines, i) ?: break
                    itemBlocks += nested.block
                    i = nested.nextIndex
                    continue
                } else {
                    // Paragraph continuation
                    paraLines += tt
                    i++
                }
            }
            if (paraLines.isNotEmpty()) {
                val inl = ArrayList<Inline>()
                for ((idx2, pl) in paraLines.withIndex()) {
                    inl.addAll(parseInlines(pl))
                    if (idx2 != paraLines.lastIndex) inl.add(Inline.SoftBreak)
                }
                itemBlocks += Block(IdGen.next(), BlockKind.Paragraph, inl)
            }
            items += ListItem(itemBlocks)
        }
        val blk = Block(IdGen.next(), BlockKind.ListBlock(ordered, items))
        return ListParse(blk, i)
    }

    // Basic pipe tables: header |---|---| line then rows
    private fun parseTable(lines: List<String>, start: Int): ListParse? {
        if (start + 1 >= lines.size) return null
        val header = lines[start]
        val sep = lines[start + 1]
        if (!header.contains('|')) return null
        val aligns = parseAlignRow(sep) ?: return null
        val rows = ArrayList<List<String>>()
        rows += parsePipeRow(header)
        var i = start + 2
        while (i < lines.size) {
            val l = lines[i]
            if (l.isBlank()) break
            if (!l.contains('|')) break
            rows += parsePipeRow(l)
            i++
        }
        val blk = Block(IdGen.next(), BlockKind.Table(rows, aligns))
        return ListParse(blk, i)
    }

    private fun parsePipeRow(line: String): List<String> {
        val t = line.trim().trim('|')
        return t.split('|').map { it.trim() }
    }

    private fun parseAlignRow(line: String): List<ColAlign>? {
        val t = line.trim().trim('|')
        val parts = t.split('|').map { it.trim() }
        if (parts.isEmpty()) return null
        val aligns = ArrayList<ColAlign>()
        for (p in parts) {
            if (p.isEmpty()) return null
            val left = p.startsWith(":")
            val right = p.endsWith(":")
            if (!p.replace(":", "").all { it == '-' }) return null
            val a = when {
                left && right -> ColAlign.Center
                right -> ColAlign.Right
                else -> ColAlign.Left
            }
            aligns += a
        }
        return aligns
    }
}
