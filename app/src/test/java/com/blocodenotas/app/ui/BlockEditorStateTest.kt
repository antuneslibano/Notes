package com.blocodenotas.app.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.blocodenotas.app.data.Block
import com.blocodenotas.app.data.BlockType
import com.blocodenotas.app.data.MarkKind
import com.blocodenotas.app.data.Note
import com.blocodenotas.app.data.TextMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ZW = "​"

/** Simula o editor de uma nota, com um teclado que envia o texto completo do campo. */
private class Harness(vararg initial: Block) {
    var note = Note(id = 1, blocks = initial.toList(), createdAt = 0, updatedAt = 0)
    var nextId = 1000L
    var now = 10_000L
    val state = BlockEditorState(
        currentBlocks = { note.blocks },
        newId = { nextId++ },
        edit = { _, transform -> note = transform(note) },
        clock = { now },
    )

    val blocks get() = note.blocks
    val types get() = blocks.map { it.type }
    val texts get() = blocks.map { it.text }

    fun block(id: Long) = blocks.first { it.id == id }
    fun field(id: Long) = state.value(block(id))

    /** Envia ao editor exatamente o que o campo mandaria, e devolve esse valor. */
    fun send(id: Long, value: TextFieldValue): TextFieldValue {
        state.focusedId = id
        state.onValueChange(id, value)
        return value
    }

    /** Digita [text] na posição do cursor (ou sobre a seleção). */
    fun type(id: Long, text: String): TextFieldValue {
        val v = field(id)
        val sel = v.selection
        val newText = v.text.replaceRange(sel.min, sel.max, text)
        return send(id, TextFieldValue(newText, TextRange(sel.min + text.length)))
    }

    fun backspace(id: Long): TextFieldValue {
        val v = field(id)
        val sel = v.selection
        return if (sel.collapsed) {
            if (sel.start == 0) return v
            send(id, TextFieldValue(v.text.removeRange(sel.start - 1, sel.start), TextRange(sel.start - 1)))
        } else {
            send(id, TextFieldValue(v.text.removeRange(sel.min, sel.max), TextRange(sel.min)))
        }
    }

    fun moveCursor(id: Long, position: Int) = send(id, TextFieldValue(field(id).text, TextRange(position + 1)))

    fun select(id: Long, start: Int, end: Int) = send(id, TextFieldValue(field(id).text, TextRange(start + 1, end + 1)))

    /** Avança o relógio além da janela de proteção contra eventos atrasados. */
    fun later() {
        now += 2_000
    }
}

class BlockEditorStateTest {

    private fun text(id: Long, value: String = "", type: BlockType = BlockType.TEXT, marks: List<TextMark> = emptyList()) =
        Block(id, type, value, marks)

    // ------------------------------------------------------------ digitação básica

    @Test
    fun typingUpdatesBlockText() {
        val h = Harness(text(1))
        h.type(1, "Olá")
        h.type(1, " mundo")
        assertEquals(listOf("Olá mundo"), h.texts)
        assertEquals(ZW + "Olá mundo", h.field(1).text)
    }

    @Test
    fun cursorCannotGoBeforeInvisibleCharacter() {
        val h = Harness(text(1, "abc"))
        h.send(1, TextFieldValue(ZW + "abc", TextRange(0, 3)))
        assertEquals(TextRange(1, 3), h.field(1).selection)
    }

    @Test
    fun selectAllAndTypeReplacesText() {
        val h = Harness(text(1, "abc"))
        // Alguns teclados apagam tudo, inclusive o caractere invisível, ao substituir.
        h.send(1, TextFieldValue("x", TextRange(1)))
        assertEquals(listOf("x"), h.texts)
        assertEquals(ZW + "x", h.field(1).text)
    }

    // ------------------------------------------------------------ comandos com "/"

    @Test
    fun slashOpensMenuWithQuery() {
        val h = Harness(text(1))
        h.type(1, "/to")
        assertEquals(SlashState(1, 0, "to"), h.state.slash)
        assertEquals(BlockType.TODO, matchCommands("to").first().type)
    }

    @Test
    fun slashInsideWordDoesNotOpenMenu() {
        val h = Harness(text(1))
        h.type(1, "e/ou")
        assertNull(h.state.slash)
    }

    @Test
    fun slashMenuClosesAfterSpace() {
        val h = Harness(text(1))
        h.type(1, "/todo ")
        assertNull(h.state.slash)
    }

    @Test
    fun slashTodoEnterCreatesExactlyOneTodo() {
        val h = Harness(text(1))
        h.type(1, "/todo")
        h.type(1, "\n")
        assertEquals(listOf(BlockType.TODO), h.types)
        assertEquals(listOf(""), h.texts)
        assertNull(h.state.slash)
    }

    @Test
    fun staleKeyboardEventsAfterSlashEnterDoNotMultiplyTodos() {
        val h = Harness(text(1))
        val beforeEnter = h.type(1, "/todo")
        val enter = h.type(1, "\n")
        // O teclado continua mandando o texto antigo do campo (finishComposingText,
        // setComposingRegion...). Antes da correção cada evento criava mais uma caixa.
        repeat(3) { h.send(1, enter) }
        h.send(1, beforeEnter)
        h.send(1, TextFieldValue(enter.text, TextRange(3)))
        assertEquals(listOf(BlockType.TODO), h.types)
        assertEquals(listOf(""), h.texts)

        h.later()
        h.type(1, "Leite")
        assertEquals(listOf("Leite"), h.texts)
        val secondEnter = h.type(1, "\n")
        repeat(3) { h.send(1, secondEnter) }
        assertEquals(listOf(BlockType.TODO, BlockType.TODO), h.types)
        assertEquals(listOf("Leite", ""), h.texts)
    }

    @Test
    fun typingRightAfterStructuralChangeIsKept() {
        val h = Harness(text(1))
        h.type(1, "/todo")
        h.type(1, "\n")
        // Sem esperar: digitar logo em seguida precisa funcionar.
        h.type(1, "P")
        h.type(1, "ão")
        assertEquals(listOf("Pão"), h.texts)
    }

    @Test
    fun fastDoubleEnterLeavesList() {
        val h = Harness(text(1, "Leite", BlockType.TODO))
        h.type(1, "\n")
        val newId = h.blocks[1].id
        // Enter de novo, sem pausa, no item vazio recém-criado: sai da lista.
        h.type(newId, "\n")
        assertEquals(listOf(BlockType.TODO, BlockType.TEXT), h.types)
    }

    @Test
    fun fastEnterAfterTypingInConvertedBlockWorks() {
        val h = Harness(text(1))
        h.type(1, "/todo")
        h.type(1, "\n")
        h.type(1, "Leite")
        // Sem esperar a janela de proteção: a tecla aceita já liberou o campo.
        h.type(1, "\n")
        assertEquals(listOf(BlockType.TODO, BlockType.TODO), h.types)
        assertEquals(listOf("Leite", ""), h.texts)
    }

    @Test
    fun slashCommandFromMenuClick() {
        val h = Harness(text(1))
        h.type(1, "/h1")
        h.state.applyCommand(SlashCommands.first { it.type == BlockType.H1 })
        assertEquals(listOf(BlockType.H1), h.types)
        assertEquals(listOf(""), h.texts)
    }

    @Test
    fun slashCommandKeepsSurroundingText() {
        val h = Harness(text(1))
        h.type(1, "Comprar /lis")
        h.state.applyCommand(matchCommands("lis").first())
        assertEquals(listOf(BlockType.BULLET), h.types)
        assertEquals(listOf("Comprar "), h.texts)
    }

    @Test
    fun englishAliasListWorks() {
        assertEquals(BlockType.BULLET, matchCommands("list").first().type)
        assertEquals(BlockType.TODO, matchCommands("todo").first().type)
        assertEquals(BlockType.QUOTE, matchCommands("citação").first().type)
        assertTrue(matchCommands("xyz").isEmpty())
    }

    @Test
    fun dividerCommandOnEmptyBlockAddsTextBlockAfter() {
        val h = Harness(text(1))
        h.type(1, "/divisor")
        h.type(1, "\n")
        assertEquals(listOf(BlockType.DIVIDER, BlockType.TEXT), h.types)
        assertEquals(h.blocks[1].id, h.state.pendingFocus)
    }

    @Test
    fun dividerCommandAfterTextKeepsText() {
        val h = Harness(text(1))
        h.type(1, "Fim /div")
        h.state.applyCommand(matchCommands("div").first())
        assertEquals(listOf(BlockType.TEXT, BlockType.DIVIDER, BlockType.TEXT), h.types)
        assertEquals("Fim ", h.texts[0])
    }

    // ------------------------------------------------------------ atalhos estilo Markdown

    @Test
    fun markdownShortcuts() {
        val cases = mapOf(
            "# " to BlockType.H1,
            "## " to BlockType.H2,
            "### " to BlockType.H3,
            "- " to BlockType.BULLET,
            "* " to BlockType.BULLET,
            "1. " to BlockType.NUMBERED,
            "[] " to BlockType.TODO,
            "> " to BlockType.QUOTE,
            "```" to BlockType.CODE,
        )
        for ((prefix, type) in cases) {
            val h = Harness(text(1))
            prefix.forEach { h.type(1, it.toString()) }
            assertEquals("atalho '$prefix'", listOf(type), h.types)
            assertEquals("atalho '$prefix'", listOf(""), h.texts)
            // O texto antigo repetido pelo teclado não pode voltar a aparecer.
            h.send(1, TextFieldValue(ZW + prefix, TextRange(prefix.length + 1)))
            assertEquals("atalho '$prefix'", listOf(""), h.texts)
        }
    }

    @Test
    fun dashesBecomeDivider() {
        val h = Harness(text(1))
        "---".forEach { h.type(1, it.toString()) }
        assertEquals(listOf(BlockType.DIVIDER, BlockType.TEXT), h.types)
    }

    @Test
    fun shortcutOnlyAtStartOfLine() {
        val h = Harness(text(1))
        h.type(1, "a - b")
        assertEquals(listOf(BlockType.TEXT), h.types)
        assertEquals(listOf("a - b"), h.texts)
    }

    @Test
    fun shortcutDoesNotApplyInsideList() {
        val h = Harness(text(1, "", BlockType.BULLET))
        h.type(1, "- ")
        assertEquals(listOf(BlockType.BULLET), h.types)
        assertEquals(listOf("- "), h.texts)
    }

    // ------------------------------------------------------------ Enter

    @Test
    fun enterSplitsBlockAndContinuesList() {
        val h = Harness(text(1, "LeiteCafé", BlockType.TODO))
        h.moveCursor(1, 5)
        h.type(1, "\n")
        assertEquals(listOf(BlockType.TODO, BlockType.TODO), h.types)
        assertEquals(listOf("Leite", "Café"), h.texts)
        assertEquals(h.blocks[1].id, h.state.pendingFocus)
        assertEquals(TextRange(1), h.field(h.blocks[1].id).selection)
    }

    @Test
    fun enterInHeadingCreatesParagraph() {
        val h = Harness(text(1, "Título", BlockType.H1))
        h.type(1, "\n")
        assertEquals(listOf(BlockType.H1, BlockType.TEXT), h.types)
    }

    @Test
    fun enterOnEmptyListItemLeavesList() {
        val h = Harness(text(1, "", BlockType.NUMBERED))
        h.type(1, "\n")
        assertEquals(listOf(BlockType.TEXT), h.types)
        assertEquals(1, h.blocks.size)
    }

    @Test
    fun enterAtStartInsertsEmptyLineAbove() {
        val h = Harness(text(1, "Texto", BlockType.H2))
        h.moveCursor(1, 0)
        h.type(1, "\n")
        assertEquals(listOf(BlockType.TEXT, BlockType.H2), h.types)
        assertEquals(listOf("", "Texto"), h.texts)
    }

    @Test
    fun enterKeepsCheckedStateOnlyOnFirstPart() {
        val h = Harness(Block(1, BlockType.TODO, "AB", checked = true))
        h.moveCursor(1, 1)
        h.type(1, "\n")
        assertEquals(listOf(true, false), h.blocks.map { it.checked })
    }

    @Test
    fun codeBlockKeepsNewlines() {
        val h = Harness(text(1, "val a = 1", BlockType.CODE))
        h.type(1, "\n")
        h.type(1, "val b = 2")
        assertEquals(1, h.blocks.size)
        assertEquals("val a = 1\nval b = 2", h.texts[0])
    }

    @Test
    fun pastingSeveralLinesCreatesSeveralBlocks() {
        val h = Harness(text(1, "", BlockType.BULLET))
        h.type(1, "um\ndois\ntrês")
        assertEquals(listOf("um", "dois", "três"), h.texts)
        assertTrue(h.types.all { it == BlockType.BULLET })
    }

    @Test
    fun splitCarriesFormatting() {
        val h = Harness(text(1, "aaabbb", marks = listOf(TextMark(0, 6, MarkKind.BOLD))))
        h.moveCursor(1, 3)
        h.type(1, "\n")
        assertEquals(listOf(TextMark(0, 3, MarkKind.BOLD)), h.blocks[0].marks)
        assertEquals(listOf(TextMark(0, 3, MarkKind.BOLD)), h.blocks[1].marks)
    }

    // ------------------------------------------------------------ Backspace no começo

    @Test
    fun backspaceAtStartTurnsListIntoText() {
        val h = Harness(text(1, "Item", BlockType.TODO))
        h.moveCursor(1, 0)
        h.backspace(1)
        assertEquals(listOf(BlockType.TEXT), h.types)
        assertEquals(listOf("Item"), h.texts)
    }

    @Test
    fun backspaceAtStartMergesWithPrevious() {
        val h = Harness(
            text(1, "Olá "),
            text(2, "mundo", marks = listOf(TextMark(0, 5, MarkKind.COLOR, 3))),
        )
        h.moveCursor(2, 0)
        h.backspace(2)
        assertEquals(listOf("Olá mundo"), h.texts)
        assertEquals(listOf(TextMark(4, 9, MarkKind.COLOR, 3)), h.blocks[0].marks)
        assertEquals(1L, h.state.pendingFocus)
        assertEquals(TextRange(5), h.field(1).selection)
    }

    @Test
    fun backspaceOnEmptyBlockRemovesIt() {
        val h = Harness(text(1, "Linha"), text(2))
        h.backspace(2)
        assertEquals(listOf("Linha"), h.texts)
    }

    @Test
    fun backspaceAfterDividerRemovesDivider() {
        val h = Harness(text(1, "a"), Block(2, BlockType.DIVIDER), text(3, "b"))
        h.moveCursor(3, 0)
        h.backspace(3)
        assertEquals(listOf(BlockType.TEXT, BlockType.TEXT), h.types)
        assertEquals(listOf("a", "b"), h.texts)
    }

    @Test
    fun backspaceOnFirstBlockKeepsText() {
        val h = Harness(text(1, "Começo"))
        h.moveCursor(1, 0)
        h.backspace(1)
        assertEquals(listOf("Começo"), h.texts)
        assertEquals(ZW + "Começo", h.field(1).text)
    }

    @Test
    fun repeatedBackspaceEventsDoNotMergeTwice() {
        val h = Harness(text(1, "a"), text(2, "b"), text(3, "c"))
        h.moveCursor(3, 0)
        val event = h.backspace(3)
        h.send(3, event)
        assertEquals(listOf("a", "bc"), h.texts)
    }

    @Test
    fun backspaceInsideTextDeletesCharacter() {
        val h = Harness(text(1, "abc"))
        h.backspace(1)
        assertEquals(listOf("ab"), h.texts)
    }

    // ------------------------------------------------------------ formatação e cores

    @Test
    fun boldTogglesOnSelection() {
        val h = Harness(text(1, "uma palavra"))
        h.select(1, 4, 11)
        assertNotNull(h.state.selection())
        h.state.format(MarkKind.BOLD)
        assertEquals(listOf(TextMark(4, 11, MarkKind.BOLD)), h.blocks[0].marks)
        h.state.format(MarkKind.BOLD)
        assertTrue(h.blocks[0].marks.isEmpty())
    }

    @Test
    fun colorsReplaceEachOther() {
        val h = Harness(text(1, "colorido"))
        h.select(1, 0, 8)
        h.state.format(MarkKind.COLOR, 3)
        h.state.format(MarkKind.COLOR, 6)
        assertEquals(listOf(TextMark(0, 8, MarkKind.COLOR, 6)), h.blocks[0].marks)
        h.state.format(MarkKind.HIGHLIGHT, 4)
        h.state.format(MarkKind.COLOR, 0)
        assertEquals(listOf(TextMark(0, 8, MarkKind.HIGHLIGHT, 4)), h.blocks[0].marks)
        h.state.clearFormat()
        assertTrue(h.blocks[0].marks.isEmpty())
    }

    @Test
    fun formatWithoutSelectionDoesNothing() {
        val h = Harness(text(1, "abc"))
        h.moveCursor(1, 1)
        assertNull(h.state.selection())
        h.state.format(MarkKind.BOLD)
        assertTrue(h.blocks[0].marks.isEmpty())
    }

    @Test
    fun typingAfterColoredWordKeepsColor() {
        val h = Harness(text(1, "azul", marks = listOf(TextMark(0, 4, MarkKind.COLOR, 6))))
        h.type(1, "ão")
        assertEquals(listOf(TextMark(0, 6, MarkKind.COLOR, 6)), h.blocks[0].marks)
    }

    // ------------------------------------------------------------ barra de tipos e outros

    @Test
    fun toolbarTypeTogglesBackToText() {
        val h = Harness(text(1, "Item"))
        h.state.focusedId = 1
        h.state.setType(BlockType.TODO)
        assertEquals(listOf(BlockType.TODO), h.types)
        h.state.setType(BlockType.TODO)
        assertEquals(listOf(BlockType.TEXT), h.types)
    }

    @Test
    fun toggleCheckedAndRemoveBlock() {
        val h = Harness(text(1, "a", BlockType.TODO), Block(2, BlockType.DIVIDER))
        h.state.toggleChecked(1)
        assertTrue(h.blocks[0].checked)
        h.state.removeBlock(2)
        assertEquals(1, h.blocks.size)
        h.state.removeBlock(1)
        assertEquals(1, h.blocks.size)
        assertEquals("", h.texts[0])
    }

    @Test
    fun focusEndReusesEmptyLastBlockOrAddsOne() {
        val h = Harness(text(1, "a"))
        h.state.focusEnd()
        assertEquals(2, h.blocks.size)
        h.state.focusEnd()
        assertEquals(2, h.blocks.size)
        assertEquals(h.blocks[1].id, h.state.pendingFocus)
    }
}
