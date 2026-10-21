package com.blocodenotas.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Conversão das notas para JSON (arquivo interno e backups) e de volta. */
object NotesJson {
    fun serialize(notes: List<Note>, folders: List<Folder>): JSONObject =
        JSONObject().apply {
            put("app", "minhas-notas")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("folders", JSONArray().apply { folders.forEach { put(folderToJson(it)) } })
            put("notes", JSONArray().apply { notes.forEach { put(noteToJson(it)) } })
        }

    fun parse(json: JSONObject): Pair<List<Note>, List<Folder>> {
        val notesArray = json.optJSONArray("notes") ?: JSONArray()
        val foldersArray = json.optJSONArray("folders") ?: JSONArray()
        val notes = (0 until notesArray.length()).map { noteFromJson(notesArray.getJSONObject(it)) }
        val folders = (0 until foldersArray.length()).map { folderFromJson(foldersArray.getJSONObject(it)) }
        return notes to folders
    }

    private fun folderToJson(f: Folder) = JSONObject().apply {
        put("id", f.id)
        put("name", f.name)
        put("createdAt", f.createdAt)
    }

    private fun folderFromJson(o: JSONObject) = Folder(
        id = o.getLong("id"),
        name = o.optString("name"),
        createdAt = o.optLong("createdAt"),
    )

    private fun noteToJson(n: Note) = JSONObject().apply {
        put("id", n.id)
        put("title", n.title)
        put("blocks", JSONArray().apply { n.blocks.forEach { put(blockToJson(it)) } })
        put("icon", n.icon.name)
        put("folderId", n.folderId ?: JSONObject.NULL)
        put("pinned", n.pinned)
        put("favorite", n.favorite)
        put("createdAt", n.createdAt)
        put("updatedAt", n.updatedAt)
        put("deletedAt", n.deletedAt ?: JSONObject.NULL)
    }

    private fun blockToJson(b: Block) = JSONObject().apply {
        put("id", b.id)
        put("type", b.type.name)
        put("text", b.text)
        if (b.checked) put("checked", true)
        if (b.marks.isNotEmpty()) {
            put("marks", JSONArray().apply {
                b.marks.forEach { m ->
                    put(JSONObject().put("s", m.start).put("e", m.end).put("k", m.kind.name).put("c", m.color))
                }
            })
        }
    }

    private fun blockFromJson(o: JSONObject): Block {
        val text = o.optString("text")
        val marksArray = o.optJSONArray("marks") ?: JSONArray()
        val marks = (0 until marksArray.length()).mapNotNull {
            val m = marksArray.getJSONObject(it)
            val kind = MarkKind.entries.firstOrNull { k -> k.name == m.optString("k") } ?: return@mapNotNull null
            val start = m.optInt("s").coerceIn(0, text.length)
            val end = m.optInt("e").coerceIn(0, text.length)
            if (end > start) TextMark(start, end, kind, m.optInt("c")) else null
        }
        return Block(
            id = o.getLong("id"),
            type = BlockType.from(o.optString("type")),
            text = text,
            marks = marks,
            checked = o.optBoolean("checked"),
        )
    }

    private fun noteFromJson(o: JSONObject): Note {
        val id = o.getLong("id")
        val blocksArray = o.optJSONArray("blocks")
        val blocks = if (blocksArray != null) {
            (0 until blocksArray.length()).map { blockFromJson(blocksArray.getJSONObject(it)) }
        } else {
            // Formato da primeira versão: texto corrido ou lista de itens.
            var next = id * 1000
            if (o.optBoolean("isChecklist")) {
                val items = o.optJSONArray("items") ?: JSONArray()
                (0 until items.length()).map {
                    val item = items.getJSONObject(it)
                    Block(item.optLong("id", ++next), BlockType.TODO, item.optString("text"), checked = item.optBoolean("checked"))
                }
            } else {
                o.optString("content").split("\n").map { Block(++next, BlockType.TEXT, it) }
            }
        }
        val created = o.optLong("createdAt", System.currentTimeMillis())
        return Note(
            id = id,
            title = o.optString("title"),
            blocks = blocks.ifEmpty { listOf(Block(id * 1000 + 1)) },
            icon = NoteIcon.from(o.optString("icon")),
            folderId = if (o.isNull("folderId")) null else o.getLong("folderId"),
            pinned = o.optBoolean("pinned"),
            favorite = o.optBoolean("favorite"),
            createdAt = created,
            updatedAt = o.optLong("updatedAt", created),
            deletedAt = if (o.isNull("deletedAt")) null else o.getLong("deletedAt"),
        )
    }
}
