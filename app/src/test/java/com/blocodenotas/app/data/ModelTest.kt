package com.blocodenotas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {

    private fun note(id: Long, title: String = "", vararg blocks: Block, updated: Long = id, created: Long = id) =
        Note(id = id, title = title, blocks = blocks.toList(), createdAt = created, updatedAt = updated)

    @Test
    fun bodyTextNumbersListsAndRestartsAfterOtherBlocks() {
        val n = note(
            1, "T",
            Block(1, BlockType.NUMBERED, "a"),
            Block(2, BlockType.NUMBERED, "b"),
            Block(3, BlockType.TEXT, "x"),
            Block(4, BlockType.NUMBERED, "c"),
            Block(5, BlockType.TODO, "d", checked = true),
            Block(6, BlockType.DIVIDER),
        )
        assertEquals("1. a\n2. b\nx\n1. c\n☑ d\n———", n.bodyText())
        assertEquals("T\n\n1. a\n2. b\nx\n1. c\n☑ d\n———", n.fullText())
    }

    @Test
    fun previewJoinsListItemsWithCommas() {
        val list = note(1, "", Block(1, BlockType.TODO, "Leite"), Block(2, BlockType.TODO, "Café"), Block(3, BlockType.TODO, ""))
        assertEquals("Leite, Café", list.preview())
        val mixed = note(2, "", Block(1, BlockType.H1, "Título"), Block(2, BlockType.TEXT, "texto"))
        assertEquals("Título texto", mixed.preview())
    }

    @Test
    fun blankDetection() {
        assertTrue(note(1, " ", Block(1, BlockType.TEXT, "  ")).isBlank)
        assertFalse(note(2, "", Block(1, BlockType.TODO, "x")).isBlank)
        assertFalse(note(3, "Título").isBlank)
        assertEquals("Sem título", note(4).displayTitle())
    }

    @Test
    fun searchIgnoresAccentsAndCase() {
        val notes = listOf(note(1, "Reunião", Block(1, BlockType.TEXT, "pauta")), note(2, "Compras", Block(2, BlockType.TODO, "Pão")))
        assertEquals(listOf(1L), notes.searchedFor("REUNIAO").map { it.id })
        assertEquals(listOf(2L), notes.searchedFor("pao").map { it.id })
        assertEquals(2, notes.searchedFor("  ").size)
        assertTrue(notes.searchedFor("inexistente").isEmpty())
    }

    @Test
    fun sortingByEachField() {
        val notes = listOf(
            note(1, "banana", updated = 30, created = 1),
            note(2, "Árvore", updated = 10, created = 3),
            note(3, "casa", updated = 20, created = 2),
        )
        assertEquals(listOf(1L, 3L, 2L), notes.sortedFor(SortField.UPDATED, ascending = false).map { it.id })
        assertEquals(listOf(1L, 3L, 2L), notes.sortedFor(SortField.CREATED, ascending = true).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), notes.sortedFor(SortField.TITLE, ascending = true).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), notes.sortedFor(SortField.TITLE, ascending = false).map { it.id })
    }

    @Test
    fun filters() {
        val done = note(1, "a", Block(1, BlockType.TODO, "x", checked = true))
        val pending = note(2, "b", Block(1, BlockType.TODO, "y"))
        val text = note(3, "c", Block(1, BlockType.TEXT, "z")).copy(favorite = true)
        val all = listOf(done, pending, text)
        assertEquals(3, all.filteredBy(NoteFilter.ALL).size)
        assertEquals(listOf(1L, 2L), all.filteredBy(NoteFilter.TODO).map { it.id })
        assertEquals(listOf(2L), all.filteredBy(NoteFilter.PENDING).map { it.id })
        assertEquals(listOf(3L), all.filteredBy(NoteFilter.FAVORITES).map { it.id })
    }

    @Test
    fun wordCount() {
        assertEquals(0, countWords("   "))
        assertEquals(3, countWords("um  dois\ntrês"))
    }
}
