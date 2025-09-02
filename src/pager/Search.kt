package pager

import ir.LayoutLine

object Search {
    fun findNext(lines: List<LayoutLine>, query: String, fromLineExclusive: Int): Int? {
        if (query.isEmpty()) return null
        val q = query.lowercase()
        var i = (fromLineExclusive + 1).coerceAtLeast(0)
        while (i < lines.size) {
            val txt = lines[i].spans.joinToString("") { it.text }.lowercase()
            if (txt.contains(q)) return i
            i++
        }
        return null
    }

    fun findPrev(lines: List<LayoutLine>, query: String, fromLineExclusive: Int): Int? {
        if (query.isEmpty()) return null
        val q = query.lowercase()
        var i = (fromLineExclusive - 1).coerceAtMost(lines.lastIndex)
        while (i >= 0) {
            val txt = lines[i].spans.joinToString("") { it.text }.lowercase()
            if (txt.contains(q)) return i
            i--
        }
        return null
    }
}

