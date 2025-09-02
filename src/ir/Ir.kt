package ir

// Core Intermediate Representation (IR)

data class Block(
    val id: Int,
    val kind: BlockKind,
    val inlines: List<Inline> = emptyList(),
)

sealed class BlockKind {
    data class Heading(val level: Int) : BlockKind()
    object Paragraph : BlockKind()
    data class ListBlock(val ordered: Boolean, val items: List<ListItem>) : BlockKind()
    data class Blockquote(val children: List<Block>) : BlockKind()
    data class CodeBlock(val language: String?, val text: String) : BlockKind()
    object ThematicBreak : BlockKind()
    data class Table(val rows: List<List<String>>, val aligns: List<ColAlign>) : BlockKind()
    data class Image(val alt: String) : BlockKind() // alt only in MVP
}

data class ListItem(val blocks: List<Block>)

enum class ColAlign { Left, Center, Right }

sealed class Inline {
    data class Text(val text: String) : Inline()
    data class Emph(val children: List<Inline>) : Inline()
    data class Strong(val children: List<Inline>) : Inline()
    data class Code(val code: String) : Inline()
    data class Link(val children: List<Inline>, val url: String) : Inline()
    object SoftBreak : Inline()
    object HardBreak : Inline()
}

// Styling & Layout

data class Style(
    val fg: AnsiColor? = null,
    val bold: Boolean = false,
    val underline: Boolean = false,
)

enum class AnsiColor { DEFAULT, BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE }

data class StyledSpan(val text: String, val style: Style)

data class LayoutLine(
    val spans: List<StyledSpan>,
    val blockId: Int,
    val rowInBlock: Int,
)

object IdGen {
    private var nextId: Int = 1
    fun next(): Int = nextId++
}

