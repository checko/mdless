@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import parser.Parser
import layout.LayoutStyled
import layout.Width
import pager.BlockLines
import pager.PagerState
import render.Renderer
import style.Themes
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.system.exitProcess

private fun readAll(path: String): String {
    val sb = StringBuilder()
    memScoped {
        val f = fopen(path, "rb")
        if (f == null) {
            println("Error: cannot open '$path'")
            return ""
        }
        try {
            val buf = ByteArray(4096)
            while (true) {
                val n = fread(buf.refTo(0), 1.convert(), buf.size.convert(), f)
                if (n.toLong() <= 0L) break
                sb.append(buf.copyOf(n.toInt()).decodeToString())
            }
        } finally { fclose(f) }
    }
    return sb.toString()
}

private data class Slice(val start: Int, val endInclusive: Int)

fun main(args: Array<String>) {
    if (args.size < 3) {
        println("Usage: ViewportCheck <FILE> <WIDTH> <HEIGHT>")
        exitProcess(2)
    }
    val file = args[0]
    val width = args[1].toIntOrNull()?.coerceAtLeast(1) ?: run { println("Invalid width"); exitProcess(2) }
    val height = args[2].toIntOrNull()?.coerceAtLeast(1) ?: run { println("Invalid height"); exitProcess(2) }

    val text = readAll(file)
    val blocks = Parser.parseMarkdown(text)
    // Layout with styles to mirror interactive path
    val theme = Themes.NOCOLOR // keep output clean for measurement
    val lines = ArrayList<ir.LayoutLine>()
    val meta = ArrayList<BlockLines>()
    for (b in blocks) {
        val ll = LayoutStyled.layoutBlock(b, width, theme)
        lines.addAll(ll)
        meta.add(BlockLines(b.id, ll.size))
    }
    val pager = PagerState(meta, height)
    val range = pager.viewportRange()
    val slice = Slice(range.first, range.last)
    val view = lines.subList(slice.start, slice.endInclusive + 1)

    // Verify each line width <= given width using Width.stringWidth
    var ok = true
    for ((idx, ln) in view.withIndex()) {
        val s = ln.spans.joinToString("") { it.text }
        val w = Width.stringWidth(s)
        if (w > width) {
            ok = false
            println("LINE ${idx} OVER: width=$w > $width | ${s}")
        }
    }
    // Render and print for manual inspection (no color)
    val out = Renderer.render(view, enableColor = false)
    print(out)
    if (view.size > height) {
        ok = false
        println("VIEW OVER HEIGHT: ${view.size} > $height")
    }
    if (!ok) exitProcess(1) else println("VIEWPORT CHECK OK")
}

