package com.blocodenotas.app.data

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

data class ChecklistItem(
    val id: Long,
    val text: String,
    val checked: Boolean = false,
)

data class Note(
    val id: Long,
    val title: String = "",
    val content: String = "",
    val isChecklist: Boolean = false,
    val items: List<ChecklistItem> = emptyList(),
    val icon: NoteIcon = NoteIcon.NOTE,
    val folderId: Long? = null,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
) {
    val isDeleted: Boolean get() = deletedAt != null

    val isBlank: Boolean
        get() = title.isBlank() && content.isBlank() && items.all { it.text.isBlank() }

    fun displayTitle(): String = title.ifBlank { "Sem título" }

    /** Corpo da nota como texto puro (listas viram linhas com caixas de seleção). */
    fun bodyText(): String =
        if (isChecklist) {
            items.filter { it.text.isNotBlank() }
                .joinToString("\n") { (if (it.checked) "☑ " else "☐ ") + it.text }
        } else {
            content
        }

    fun fullText(): String = listOf(title, bodyText()).filter { it.isNotBlank() }.joinToString("\n\n")

    fun preview(): String =
        if (isChecklist) {
            items.filter { it.text.isNotBlank() }.joinToString(", ") { it.text.trim() }
        } else {
            content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
        }
}

data class Folder(
    val id: Long,
    val name: String,
    val createdAt: Long,
)

enum class NoteIcon(val label: String) {
    NOTE("Nota"),
    LIGHTBULB("Ideia"),
    LOCK("Privado"),
    CHECK("Tarefa"),
    CART("Compras"),
    WORK("Trabalho"),
    HOME("Casa"),
    HEART("Pessoal"),
    SCHOOL("Estudos"),
    TRAVEL("Viagem"),
    FOOD("Receita"),
    MONEY("Finanças"),
    BOOK("Leitura"),
    PET("Pet"),
    FITNESS("Saúde"),
    STAR("Importante");

    companion object {
        fun from(name: String?): NoteIcon = entries.firstOrNull { it.name == name } ?: NOTE
    }
}

enum class SortField(val label: String) {
    UPDATED("Última alteração"),
    CREATED("Data de criação"),
    TITLE("Nome"),
}

enum class NoteFilter(val label: String) {
    ALL("Todas"),
    TEXT("Somente textos"),
    CHECKLIST("Somente listas"),
    FAVORITES("Somente favoritas"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("Seguir o sistema"),
    LIGHT("Claro"),
    DARK("Escuro"),
}

private val collator: Collator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply {
    strength = Collator.PRIMARY
}

fun List<Note>.sortedFor(field: SortField, ascending: Boolean): List<Note> {
    val comparator: Comparator<Note> = when (field) {
        SortField.UPDATED -> compareBy<Note> { it.updatedAt }
        SortField.CREATED -> compareBy<Note> { it.createdAt }
        SortField.TITLE -> compareBy<Note, String>(collator) { it.displayTitle() }
    }
    return sortedWith(if (ascending) comparator else comparator.reversed())
}

fun List<Note>.filteredBy(filter: NoteFilter): List<Note> = when (filter) {
    NoteFilter.ALL -> this
    NoteFilter.TEXT -> filter { !it.isChecklist }
    NoteFilter.CHECKLIST -> filter { it.isChecklist }
    NoteFilter.FAVORITES -> filter { it.favorite }
}

private val marks = Regex("\\p{M}+")

fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(marks, "").lowercase(Locale.ROOT)

fun List<Note>.searchedFor(query: String): List<Note> {
    val q = normalizeForSearch(query.trim())
    if (q.isEmpty()) return this
    return filter { normalizeForSearch(it.title + "\n" + it.bodyText()).contains(q) }
}

private val wordRegex = Regex("\\S+")

fun countWords(text: String): Int = wordRegex.findAll(text).count()
