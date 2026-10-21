package com.blocodenotas.app.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Guarda todas as notas em um arquivo JSON no armazenamento interno do aparelho.
 *
 * Cada alteração atualiza o estado em memória na hora e agenda uma gravação em disco
 * logo em seguida. A gravação usa [AtomicFile]: o arquivo antigo só é substituído depois
 * que o novo foi escrito por completo, então um fechamento inesperado não corrompe as notas.
 */
class NotesRepository(context: Context) {

    private val file = AtomicFile(File(context.filesDir, "notes.json"))
    private val filesDir = context.filesDir
    private val handler = Handler(Looper.getMainLooper())
    private val writer = Executors.newSingleThreadExecutor()
    private val saveRunnable = Runnable { writeNow() }
    private var dirty = false
    private var lastId = 0L

    var notes by mutableStateOf<List<Note>>(emptyList())
        private set
    var folders by mutableStateOf<List<Folder>>(emptyList())
        private set

    init {
        load()
    }

    fun newId(): Long {
        val now = System.currentTimeMillis()
        lastId = if (now > lastId) now else lastId + 1
        return lastId
    }

    fun note(id: Long): Note? = notes.firstOrNull { it.id == id }

    fun folder(id: Long?): Folder? = if (id == null) null else folders.firstOrNull { it.id == id }

    // ---------- Notas ----------

    fun createNote(folderId: Long? = null): Note {
        val now = System.currentTimeMillis()
        val note = Note(
            id = newId(),
            blocks = listOf(Block(newId())),
            folderId = folderId,
            createdAt = now,
            updatedAt = now,
        )
        notes = notes + note
        scheduleSave()
        return note
    }

    /** Altera uma nota. Com [touch], a data de alteração é atualizada. */
    fun updateNote(id: Long, touch: Boolean = true, transform: (Note) -> Note) {
        val now = System.currentTimeMillis()
        notes = notes.map {
            if (it.id == id) {
                val changed = transform(it)
                if (touch) changed.copy(updatedAt = now) else changed
            } else {
                it
            }
        }
        scheduleSave()
    }

    fun togglePinned(id: Long) = updateNote(id, touch = false) { it.copy(pinned = !it.pinned) }

    fun toggleFavorite(id: Long) = updateNote(id, touch = false) { it.copy(favorite = !it.favorite) }

    fun moveToTrash(id: Long) = updateNote(id, touch = false) { it.copy(deletedAt = System.currentTimeMillis()) }

    fun restore(id: Long) = updateNote(id, touch = false) { it.copy(deletedAt = null) }

    fun deletePermanently(id: Long) {
        notes = notes.filterNot { it.id == id }
        scheduleSave()
    }

    fun emptyTrash() {
        notes = notes.filterNot { it.isDeleted }
        scheduleSave()
    }

    fun duplicate(id: Long): Note? {
        val original = note(id) ?: return null
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = newId(),
            title = if (original.title.isBlank()) "Cópia" else original.title + " (cópia)",
            blocks = original.blocks.map { it.copy(id = newId()) },
            pinned = false,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        notes = notes + copy
        scheduleSave()
        return copy
    }

    // ---------- Pastas ----------

    fun createFolder(name: String): Folder {
        val folder = Folder(newId(), name.trim(), System.currentTimeMillis())
        folders = folders + folder
        scheduleSave()
        return folder
    }

    fun renameFolder(id: Long, name: String) {
        folders = folders.map { if (it.id == id) it.copy(name = name.trim()) else it }
        scheduleSave()
    }

    /** Exclui a pasta; as notas dela continuam existindo, apenas sem pasta. */
    fun deleteFolder(id: Long) {
        folders = folders.filterNot { it.id == id }
        notes = notes.map { if (it.folderId == id) it.copy(folderId = null) else it }
        scheduleSave()
    }

    // ---------- Persistência ----------

    private fun scheduleSave() {
        dirty = true
        handler.removeCallbacks(saveRunnable)
        handler.postDelayed(saveRunnable, 300)
    }

    /** Grava imediatamente o que estiver pendente (chamado ao sair do app). */
    fun flush() {
        if (dirty) {
            handler.removeCallbacks(saveRunnable)
            writeNow()
        }
    }

    private fun writeNow() {
        dirty = false
        val notesSnapshot = notes
        val foldersSnapshot = folders
        writer.execute {
            val bytes = serialize(notesSnapshot, foldersSnapshot).toString().toByteArray(Charsets.UTF_8)
            var out: FileOutputStream? = null
            try {
                out = file.startWrite()
                out.write(bytes)
                file.finishWrite(out)
            } catch (e: IOException) {
                if (out != null) file.failWrite(out)
            }
        }
    }

    private fun load() {
        val base = file.baseFile
        if (!base.exists() && !File(base.path + ".bak").exists()) {
            seed()
            return
        }
        try {
            val json = JSONObject(String(file.readFully(), Charsets.UTF_8))
            val (loadedNotes, loadedFolders) = parse(json)
            val limit = System.currentTimeMillis() - TRASH_RETENTION_MS
            // Remove notas vazias esquecidas e itens antigos da lixeira.
            notes = loadedNotes.filterNot { it.isBlank || (it.deletedAt != null && it.deletedAt < limit) }
            folders = loadedFolders
            lastId = (notes.map { it.id } + notes.flatMap { n -> n.blocks.map { it.id } } + folders.map { it.id })
                .maxOrNull() ?: 0L
            if (notes.size != loadedNotes.size) scheduleSave()
        } catch (e: Exception) {
            // Arquivo ilegível: guarda uma cópia para não perder nada e começa vazio.
            runCatching { base.copyTo(File(filesDir, "notes-corrompido-${System.currentTimeMillis()}.json")) }
            notes = emptyList()
            folders = emptyList()
        }
    }

    private fun seed() {
        val now = System.currentTimeMillis()
        fun b(type: BlockType, text: String = "", marks: List<TextMark> = emptyList()) = Block(newId(), type, text, marks)
        val list = Note(
            id = newId(),
            title = "Lista de compras",
            blocks = listOf("Leite", "Café", "Pão", "Frutas").map { b(BlockType.TODO, it) },
            icon = NoteIcon.CART,
            pinned = true,
            createdAt = now,
            updatedAt = now,
        )
        val welcome = Note(
            id = newId(),
            title = "Bem-vindo ao Minhas Notas",
            blocks = listOf(
                b(BlockType.TEXT, "Tudo o que você escreve é salvo automaticamente no celular, sem internet."),
                b(BlockType.H2, "Comandos com /"),
                b(BlockType.TEXT, "Digite / em qualquer linha para escolher o tipo de bloco:"),
                b(BlockType.BULLET, "/h1, /h2 e /h3 para títulos"),
                b(BlockType.BULLET, "/lista, /numerada e /todo para listas"),
                b(BlockType.BULLET, "/citacao, /destaque, /codigo e /divisor"),
                b(BlockType.H2, "Atalhos ao digitar"),
                b(BlockType.TEXT, "# título   - lista   1. numerada   [] tarefa   > citação   --- divisor"),
                b(BlockType.H2, "Cores e estilos"),
                b(
                    BlockType.TEXT,
                    "Selecione palavras para deixar em negrito, itálico, colorir ou marcar com destaque.",
                    listOf(
                        TextMark(34, 41, MarkKind.BOLD),
                        TextMark(43, 50, MarkKind.ITALIC),
                        TextMark(52, 59, MarkKind.COLOR, 7),
                        TextMark(63, 82, MarkKind.HIGHLIGHT, 4),
                    ),
                ),
                b(BlockType.DIVIDER),
                b(BlockType.CALLOUT, "Como as notas ficam só neste aparelho, faça um backup de vez em quando em Configurações."),
            ),
            icon = NoteIcon.LIGHTBULB,
            createdAt = now,
            updatedAt = now,
        )
        notes = listOf(list, welcome)
        scheduleSave()
    }

    // ---------- Backup ----------

    fun exportBackup(): String = serialize(notes, folders).toString(2)

    fun exportText(): String {
        val active = notes.filterNot { it.isDeleted }.sortedFor(SortField.UPDATED, ascending = false)
        return active.joinToString("\n\n" + "-".repeat(40) + "\n\n") { note ->
            buildString {
                append(note.displayTitle())
                folder(note.folderId)?.let { append("  [").append(it.name).append("]") }
                append("\n")
                if (note.bodyText().isNotBlank()) append("\n").append(note.bodyText())
            }
        }
    }

    /**
     * Restaura um backup. Com [replace], as notas atuais são substituídas; caso contrário
     * as notas do backup são mescladas (as de mesmo id são atualizadas). Retorna quantas notas vieram.
     */
    fun importBackup(text: String, replace: Boolean): Int {
        val json = JSONObject(text)
        require(json.has("notes")) { "Arquivo de backup inválido" }
        val (importedNotes, importedFolders) = parse(json)
        if (replace) {
            notes = importedNotes
            folders = importedFolders
        } else {
            val noteIds = importedNotes.map { it.id }.toSet()
            val folderIds = importedFolders.map { it.id }.toSet()
            notes = notes.filterNot { it.id in noteIds } + importedNotes
            folders = folders.filterNot { it.id in folderIds } + importedFolders
        }
        lastId = maxOf(lastId, (notes.map { it.id } + notes.flatMap { n -> n.blocks.map { it.id } } + folders.map { it.id }).maxOrNull() ?: 0L)
        scheduleSave()
        return importedNotes.size
    }

    companion object {
        const val TRASH_RETENTION_DAYS = 30
        private const val TRASH_RETENTION_MS = TRASH_RETENTION_DAYS * 24L * 60 * 60 * 1000

        private fun serialize(notes: List<Note>, folders: List<Folder>) = NotesJson.serialize(notes, folders)

        private fun parse(json: JSONObject) = NotesJson.parse(json)
    }
}
