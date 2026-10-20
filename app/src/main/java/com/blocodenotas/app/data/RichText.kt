package com.blocodenotas.app.data

/** Tipos de bloco do editor, no estilo do Notion. */
enum class BlockType {
    TEXT, H1, H2, H3, BULLET, NUMBERED, TODO, QUOTE, CALLOUT, CODE, DIVIDER;

    /** Tipo do bloco criado ao apertar Enter no fim deste. Listas continuam; o resto vira texto. */
    val continuation: BlockType
        get() = when (this) {
            BULLET, NUMBERED, TODO -> this
            else -> TEXT
        }

    val isList: Boolean get() = this == BULLET || this == NUMBERED || this == TODO

    companion object {
        fun from(name: String?): BlockType = entries.firstOrNull { it.name == name } ?: TEXT
    }
}

enum class MarkKind { BOLD, ITALIC, UNDERLINE, STRIKE, COLOR, HIGHLIGHT }

/** Formatação aplicada ao trecho [start, end) do texto de um bloco. [color] indexa a paleta. */
data class TextMark(val start: Int, val end: Int, val kind: MarkKind, val color: Int = 0)

data class Block(
    val id: Long,
    val type: BlockType = BlockType.TEXT,
    val text: String = "",
    val marks: List<TextMark> = emptyList(),
    val checked: Boolean = false,
)

object Marks {

    /**
     * Ajusta as marcações depois que o texto mudou de [old] para [new]. Texto digitado no fim
     * de um trecho formatado herda a formatação, como no Notion.
     */
    fun adjust(marks: List<TextMark>, old: String, new: String): List<TextMark> {
        if (marks.isEmpty() || old == new) return marks
        var prefix = 0
        val maxPrefix = minOf(old.length, new.length)
        while (prefix < maxPrefix && old[prefix] == new[prefix]) prefix++
        var suffix = 0
        val maxSuffix = minOf(old.length, new.length) - prefix
        while (suffix < maxSuffix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        val oldEnd = old.length - suffix
        val newEnd = new.length - suffix
        val delta = new.length - old.length

        fun mapStart(a: Int): Int = when {
            a < prefix -> a
            a >= oldEnd -> a + delta
            else -> newEnd
        }

        fun mapEnd(b: Int): Int = when {
            b < prefix -> b
            b >= oldEnd -> b + delta
            else -> prefix
        }

        return marks.mapNotNull { m ->
            val s = mapStart(m.start).coerceIn(0, new.length)
            val e = mapEnd(m.end).coerceIn(0, new.length)
            if (e > s) m.copy(start = s, end = e) else null
        }
    }

    /** Marcações dentro de [start, end), deslocadas para começar em zero. */
    fun slice(marks: List<TextMark>, start: Int, end: Int): List<TextMark> = marks.mapNotNull { m ->
        val s = maxOf(m.start, start)
        val e = minOf(m.end, end)
        if (e > s) m.copy(start = s - start, end = e - start) else null
    }

    fun shift(marks: List<TextMark>, by: Int): List<TextMark> =
        marks.map { it.copy(start = it.start + by, end = it.end + by) }

    /** Remove a formatação [kind] do trecho [start, end), dividindo marcações quando preciso. */
    fun remove(marks: List<TextMark>, start: Int, end: Int, kind: MarkKind?): List<TextMark> =
        marks.flatMap { m ->
            if ((kind != null && m.kind != kind) || m.end <= start || m.start >= end) {
                listOf(m)
            } else {
                listOfNotNull(
                    if (m.start < start) m.copy(end = start) else null,
                    if (m.end > end) m.copy(start = end) else null,
                )
            }
        }

    fun covers(marks: List<TextMark>, start: Int, end: Int, kind: MarkKind): Boolean {
        if (end <= start) return false
        val ranges = marks.filter { it.kind == kind }.sortedBy { it.start }
        var pos = start
        for (m in ranges) {
            if (m.start > pos) break
            if (m.end > pos) pos = m.end
            if (pos >= end) return true
        }
        return pos >= end
    }

    /** Liga/desliga negrito, itálico, sublinhado ou tachado no trecho. */
    fun toggle(marks: List<TextMark>, start: Int, end: Int, kind: MarkKind): List<TextMark> =
        if (covers(marks, start, end, kind)) {
            remove(marks, start, end, kind)
        } else {
            remove(marks, start, end, kind) + TextMark(start, end, kind)
        }

    /** Aplica uma cor (0 = padrão, que apenas remove a cor). */
    fun setColor(marks: List<TextMark>, start: Int, end: Int, kind: MarkKind, color: Int): List<TextMark> {
        val cleared = remove(marks, start, end, kind)
        return if (color == 0) cleared else cleared + TextMark(start, end, kind, color)
    }
}
