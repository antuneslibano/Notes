package com.blocodenotas.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesJsonTest {

    @Test
    fun roundTripKeepsEverything() {
        val folder = Folder(7, "Casa", 5)
        val note = Note(
            id = 10,
            title = "Título",
            blocks = listOf(
                Block(11, BlockType.H1, "Cabeçalho"),
                Block(12, BlockType.TODO, "Tarefa", checked = true),
                Block(13, BlockType.TEXT, "colorido", listOf(TextMark(0, 3, MarkKind.COLOR, 6), TextMark(2, 8, MarkKind.BOLD))),
                Block(14, BlockType.DIVIDER),
            ),
            icon = NoteIcon.CART,
            folderId = 7,
            pinned = true,
            favorite = true,
            createdAt = 100,
            updatedAt = 200,
            deletedAt = 300,
        )
        val text = NotesJson.serialize(listOf(note), listOf(folder)).toString()
        val (notes, folders) = NotesJson.parse(JSONObject(text))
        assertEquals(listOf(note), notes)
        assertEquals(listOf(folder), folders)
    }

    @Test
    fun nullableFieldsRoundTrip() {
        val note = Note(id = 1, blocks = listOf(Block(2)), createdAt = 1, updatedAt = 1)
        val (notes, _) = NotesJson.parse(JSONObject(NotesJson.serialize(listOf(note), emptyList()).toString()))
        assertNull(notes[0].folderId)
        assertNull(notes[0].deletedAt)
    }

    @Test
    fun migratesFirstVersionTextNotes() {
        val json = JSONObject(
            """{"notes":[{"id":5,"title":"Antiga","content":"linha 1\nlinha 2","isChecklist":false,
               "items":[],"icon":"LIGHTBULB","folderId":null,"pinned":false,"favorite":true,
               "createdAt":1,"updatedAt":2,"deletedAt":null}],"folders":[]}""",
        )
        val note = NotesJson.parse(json).first.single()
        assertEquals(listOf("linha 1", "linha 2"), note.blocks.map { it.text })
        assertTrue(note.blocks.all { it.type == BlockType.TEXT })
        assertEquals(NoteIcon.LIGHTBULB, note.icon)
        assertTrue(note.favorite)
        assertEquals(note.blocks.size, note.blocks.map { it.id }.toSet().size)
    }

    @Test
    fun migratesFirstVersionChecklists() {
        val json = JSONObject(
            """{"notes":[{"id":5,"title":"Compras","content":"","isChecklist":true,
               "items":[{"id":6,"text":"Leite","checked":true},{"id":7,"text":"Pão","checked":false}],
               "createdAt":1,"updatedAt":2}]}""",
        )
        val note = NotesJson.parse(json).first.single()
        assertEquals(listOf(BlockType.TODO, BlockType.TODO), note.blocks.map { it.type })
        assertEquals(listOf("Leite", "Pão"), note.blocks.map { it.text })
        assertEquals(listOf(true, false), note.blocks.map { it.checked })
    }

    @Test
    fun brokenMarksAndUnknownValuesAreTolerated() {
        val json = JSONObject(
            """{"notes":[{"id":1,"title":"x","icon":"NAO_EXISTE","createdAt":1,
               "blocks":[{"id":2,"type":"OUTRO","text":"abc","marks":[
                 {"s":1,"e":99,"k":"BOLD","c":0},{"s":2,"e":1,"k":"ITALIC"},{"s":0,"e":2,"k":"PISCAR"}]}]}]}""",
        )
        val note = NotesJson.parse(json).first.single()
        assertEquals(NoteIcon.NOTE, note.icon)
        assertEquals(BlockType.TEXT, note.blocks[0].type)
        assertEquals(listOf(TextMark(1, 3, MarkKind.BOLD)), note.blocks[0].marks)
        assertEquals(1L, note.updatedAt)
    }

    @Test
    fun noteWithoutBlocksGetsOneEmptyBlock() {
        val json = JSONObject("""{"notes":[{"id":1,"title":"x","createdAt":1,"blocks":[]}]}""")
        assertEquals(1, NotesJson.parse(json).first.single().blocks.size)
    }
}
