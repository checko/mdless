package pager

data class BlockLines(val blockId: Int, val lineCount: Int)

class PagerState(
    blocks: List<BlockLines>,
    height: Int,
) {
    private val ids: IntArray
    private val prefix: IntArray // size = n+1, prefix[0]=0, prefix[i+1]=prefix[i]+count[i]
    val totalLines: Int
    var height: Int = height
        private set
    var topLine: Int = 0
        private set

    init {
        require(height > 0) { "height must be > 0" }
        ids = IntArray(blocks.size)
        prefix = IntArray(blocks.size + 1)
        var acc = 0
        for (i in blocks.indices) {
            val b = blocks[i]
            ids[i] = b.blockId
            prefix[i] = acc
            acc += b.lineCount.coerceAtLeast(0)
        }
        prefix[blocks.size] = acc
        totalLines = acc
        clampTop()
    }

    fun setHeight(newHeight: Int) {
        require(newHeight > 0)
        this.height = newHeight
        clampTop()
    }

    fun jumpToTop() {
        topLine = 0
    }

    fun jumpToBottom() {
        topLine = maxTop()
    }

    fun scrollLines(n: Int) {
        topLine = (topLine + n).coerceIn(0, maxTop())
    }

    fun scrollPages(n: Int) {
        scrollLines(n * height)
    }

    fun viewportRange(): IntRange {
        val start = topLine
        val endExclusive = (topLine + height).coerceAtMost(totalLines)
        return start until endExclusive
    }

    data class Pos(val blockId: Int, val rowInBlock: Int)

    fun logicalToPos(line: Int): Pos {
        require(line in 0 until totalLines) { "line out of bounds" }
        val idx = findBlockIndex(line)
        val base = prefix[idx]
        return Pos(ids[idx], line - base)
    }

    fun posToLogical(blockId: Int, rowInBlock: Int): Int {
        var idx = -1
        for (i in ids.indices) if (ids[i] == blockId) { idx = i; break }
        require(idx >= 0) { "unknown blockId" }
        val base = prefix[idx]
        return base + rowInBlock
    }

    // Percent through document based on scrollable range
    fun percent(): Int {
        if (totalLines == 0) return 100
        val maxTop = maxTop()
        if (maxTop == 0) return 100
        val p = (topLine * 100) / maxTop
        return p.coerceIn(0, 100)
    }

    private fun maxTop(): Int = (totalLines - height).coerceAtLeast(0)

    private fun clampTop() {
        topLine = topLine.coerceIn(0, maxTop())
    }

    private fun findBlockIndex(line: Int): Int {
        var lo = 0
        var hi = ids.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val start = prefix[mid]
            val end = prefix[mid + 1]
            if (line < start) hi = mid - 1
            else if (line >= end) lo = mid + 1
            else return mid
        }
        return ids.size - 1
    }
}

