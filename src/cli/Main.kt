import kotlin.system.exitProcess

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

fun main(args: Array<String>) {
    if (args.contains("-h") || args.contains("--help")) {
        printHelp()
        return
    }
    // Bootstrap placeholder: detect TTY vs pipe later; for now just print help if no args
    if (args.isEmpty()) {
        printHelp()
        return
    }
    println("mdless: not yet implemented — got args: ${args.joinToString(" ")}")
    exitProcess(0)
}

