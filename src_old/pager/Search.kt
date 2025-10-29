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

    // Compute all match ranges per line (case-insensitive).
    // Returns a list aligned to `lines`, where each entry is a list of ranges [start..endInclusive].
    fun computeMatches(lines: List<LayoutLine>, query: String): List<List<IntRange>> {
        if (query.isEmpty()) return List(lines.size) { emptyList() }
        val q = query.lowercase()
        val out = ArrayList<List<IntRange>>(lines.size)
        for (line in lines) {
            val text = line.spans.joinToString("") { it.text }
            val lower = text.lowercase()
            val ranges = ArrayList<IntRange>()
            var idx = 0
            while (idx <= lower.length - q.length && q.isNotEmpty()) {
                val pos = lower.indexOf(q, startIndex = idx)
                if (pos < 0) break
                val endExcl = pos + q.length
                ranges.add(IntRange(pos, endExcl - 1))
                idx = pos + q.length
            }
            out += ranges
        }
        return out
    }
}
