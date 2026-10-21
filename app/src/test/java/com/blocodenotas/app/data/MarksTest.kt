package com.blocodenotas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarksTest {

    private val bold = TextMark(2, 5, MarkKind.BOLD)

    @Test
    fun typingAtEndOfMarkExtendsIt() {
        assertEquals(listOf(TextMark(2, 6, MarkKind.BOLD)), Marks.adjust(listOf(bold), "abcde", "abcdeX"))
    }

    @Test
    fun typingBeforeMarkShiftsIt() {
        assertEquals(listOf(TextMark(3, 6, MarkKind.BOLD)), Marks.adjust(listOf(bold), "abcde", "Xabcde"))
        assertEquals(listOf(TextMark(3, 6, MarkKind.BOLD)), Marks.adjust(listOf(bold), "abcde", "abXcde"))
    }

    @Test
    fun typingInsideMarkGrowsIt() {
        assertEquals(listOf(TextMark(2, 6, MarkKind.BOLD)), Marks.adjust(listOf(bold), "abcde", "abcXde"))
    }

    @Test
    fun deletingInsideMarkShrinksIt() {
        assertEquals(listOf(TextMark(2, 4, MarkKind.BOLD)), Marks.adjust(listOf(bold), "abcde", "abce"))
    }

    @Test
    fun deletingWholeMarkRemovesIt() {
        assertTrue(Marks.adjust(listOf(bold), "abcde", "ab").isEmpty())
    }

    @Test
    fun typingAfterMarkDoesNotTouchIt() {
        val mark = TextMark(0, 2, MarkKind.ITALIC)
        assertEquals(listOf(mark), Marks.adjust(listOf(mark), "ab cd", "ab cdX"))
    }

    @Test
    fun removeSplitsMark() {
        val result = Marks.remove(listOf(TextMark(0, 10, MarkKind.BOLD)), 3, 6, MarkKind.BOLD)
        assertEquals(listOf(TextMark(0, 3, MarkKind.BOLD), TextMark(6, 10, MarkKind.BOLD)), result)
    }

    @Test
    fun removeOnlyAffectsGivenKind() {
        val marks = listOf(TextMark(0, 4, MarkKind.BOLD), TextMark(0, 4, MarkKind.ITALIC))
        assertEquals(listOf(TextMark(0, 4, MarkKind.ITALIC)), Marks.remove(marks, 0, 4, MarkKind.BOLD))
        assertTrue(Marks.remove(marks, 0, 4, null).isEmpty())
    }

    @Test
    fun coversHandlesAdjacentMarks() {
        val marks = listOf(TextMark(0, 3, MarkKind.BOLD), TextMark(3, 6, MarkKind.BOLD))
        assertTrue(Marks.covers(marks, 1, 5, MarkKind.BOLD))
        assertFalse(Marks.covers(marks, 1, 7, MarkKind.BOLD))
        assertFalse(Marks.covers(marks, 1, 5, MarkKind.ITALIC))
    }

    @Test
    fun togglePartiallyCoveredAddsWholeRange() {
        val result = Marks.toggle(listOf(TextMark(0, 2, MarkKind.BOLD)), 0, 5, MarkKind.BOLD)
        assertTrue(Marks.covers(result, 0, 5, MarkKind.BOLD))
        assertEquals(1, result.size)
    }

    @Test
    fun sliceAndShift() {
        val marks = listOf(TextMark(2, 8, MarkKind.COLOR, 3))
        assertEquals(listOf(TextMark(0, 3, MarkKind.COLOR, 3)), Marks.slice(marks, 5, 10))
        assertEquals(listOf(TextMark(4, 10, MarkKind.COLOR, 3)), Marks.shift(marks, 2))
    }
}
