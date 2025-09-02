import cli.KeyMap
import cli.KeyCmd
import cli.App
import ir.Block
import ir.BlockKind
import ir.LayoutLine
import layout.Layout
import parser.Parser
import pager.BlockLines
import pager.PagerState
import pager.Search
import render.Renderer
import tty.Tty
import kotlin.system.exitProcess
import kotlinx.cinterop.*
import platform.posix.*

private const val VERSION = "0.1.0-dev"

fun printHelp(): Unit = println(
    """
    mdless $VERSION
    
    Usage:
      mdless [FILE]
      cat README.md | mdless
    
    Options:
      --theme dark|light|no-color    Theme selection (default: dark)
      --wrap on|off                  Line wrap (default: on)
      --paging auto|always|never     Paging mode (default: auto)
      --width N                      Fixed render width (default: terminal)
      --toc                          Show table of contents (stub)
      --no-syntax                    Disable code syntax highlight (stub)
      --links inline|footnote|hide   Link rendering mode (default: inline)
      --tab-width N                  Tab width (default: 4)
      -h, --help                     Show this help
    """.trimIndent()
)

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
        } finally {
            fclose(f)
        }
    }
    return sb.toString()
}

private fun readAllStdin(): String {
    val sb = StringBuilder()
    memScoped {
        val buf = ByteArray(4096)
        while (true) {
            val n = platform.posix.read(platform.posix.STDIN_FILENO, buf.refTo(0), buf.size.convert())
            if (n <= 0) break
            val s = buf.copyOf(n).decodeToString()
            sb.append(s)
        }
    }
    return sb.toString()
}

private fun layoutAll(blocks: List<Block>, width: Int): Pair<List<LayoutLine>, List<BlockLines>> {
    val all = ArrayList<LayoutLine>()
    val meta = ArrayList<BlockLines>()
    for (b in blocks) {
        val lines = Layout.layoutBlock(b, width)
        all.addAll(lines)
        meta.add(BlockLines(b.id, lines.size))
    }
    return all to meta
}

private fun clearScreen() {
    print("\u001B[2J\u001B[H")
}

private fun promptLine(prefix: String): String? {
    // Simple inline prompt; ESC cancels; Enter submits; Backspace supported
    print(prefix)
    val buf = StringBuilder()
    while (true) {
        val b = tty.Tty.readByte()
        if (b < 0) continue
        when (b) {
            27 -> { // ESC
                println()
                return null
            }
            10, 13 -> { // Enter
                println()
                return buf.toString()
            }
            127 -> { // Backspace
                if (buf.isNotEmpty()) {
                    buf.deleteAt(buf.length - 1)
                    // Erase last char visually
                    print("\b \b")
                }
            }
            else -> {
                val ch = b.toChar()
                if (ch.code in 32..126) { // printable ASCII
                    buf.append(ch)
                    print(ch)
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    if (args.contains("-h") || args.contains("--help")) {
        printHelp()
        return
    }

    val input: String
    val interactive: Boolean
    val hasFile = args.isNotEmpty()
    if (hasFile) {
        input = readAll(args[0])
        interactive = Tty.isattyStdout()
    } else {
        val stdinIsTty = Tty.isattyStdin()
        interactive = !stdinIsTty && Tty.isattyStdout()
        input = if (stdinIsTty) {
            printHelp(); return
        } else readAllStdin()
    }

    val blocks = Parser.parseMarkdown(input)
    var size = Tty.getTermSize()
    var width = size.cols.coerceAtLeast(20)
    var height = (size.rows - 1).coerceAtLeast(1)
    var pair = layoutAll(blocks, width)
    var lines = pair.first
    var meta = pair.second

    if (!interactive) {
        // Non-interactive: render all at once, no color for now
        val out = Renderer.render(lines, enableColor = false)
        print(out)
        return
    }

    val pager = PagerState(meta, height)
    var lastQuery: String? = null
    var lastDir: Int = 1 // 1 forward, -1 backward
    Tty.withRawMode {
        var running = true
        while (running) {
            // poll size and reflow if changed
            val ns = Tty.getTermSize()
            if (ns.cols != width || ns.rows != height + 1) {
                size = ns
                width = size.cols.coerceAtLeast(20)
                height = (size.rows - 1).coerceAtLeast(1)
                pair = layoutAll(blocks, width)
                lines = pair.first
                meta = pair.second
                pager.setHeight(height)
            }
            clearScreen()
            val range = pager.viewportRange()
            val slice = lines.subList(range.first, range.last + 1)
            val out = Renderer.render(slice, enableColor = false)
            print(out)
            // status line
            println("-- ${pager.percent()}% (q quit, / ? search) --")

            val b = tty.Tty.readByte()
            if (b >= 0) {
                val c = b.toChar()
                val cmd = KeyMap.fromChar(c)
                when (cmd) {
                    KeyCmd.Quit -> running = false
                    KeyCmd.SearchForward, KeyCmd.SearchBackward -> {
                        val forward = (cmd == KeyCmd.SearchForward)
                        val q = promptLine(if (forward) "/" else "?")
                        if (q != null && q.isNotEmpty()) {
                            lastQuery = q
                            lastDir = if (forward) 1 else -1
                            val start = pager.viewportRange().first
                            val hit = if (forward) Search.findNext(lines, q, start) else Search.findPrev(lines, q, start)
                            if (hit != null) pager.setTopLine(hit)
                        }
                    }
                    KeyCmd.SearchNext, KeyCmd.SearchPrev -> {
                        val q = lastQuery
                        if (q != null && q.isNotEmpty()) {
                            val forward = if (cmd == KeyCmd.SearchNext) lastDir == 1 else lastDir == -1
                            val start = pager.viewportRange().first
                            val hit = if (forward) Search.findNext(lines, q, start) else Search.findPrev(lines, q, start)
                            if (hit != null) pager.setTopLine(hit)
                        }
                    }
                    else -> {
                        App.handleKey(pager, cmd)
                    }
                }
            }
        }
    }
}
