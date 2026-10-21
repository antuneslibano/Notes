package com.blocodenotas.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blocodenotas.app.Graph
import com.blocodenotas.app.data.NoteFilter
import com.blocodenotas.app.data.NotesRepository
import com.blocodenotas.app.data.SortField
import com.blocodenotas.app.data.filteredBy
import com.blocodenotas.app.data.searchedFor
import com.blocodenotas.app.data.sortedFor
import kotlin.math.max

private val ListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)

// ---------------------------------------------------------------- Início

@Composable
fun HomeScreen(onOpen: (Long) -> Unit, onSettings: () -> Unit) {
    val repo = Graph.repo
    val settings = Graph.settings
    var query by rememberSaveable { mutableStateOf("") }

    val searching = query.isNotBlank()
    val visible = repo.notes
        .filterNot { it.isDeleted }
        .filteredBy(settings.filter)
        .searchedFor(query)
        .sortedFor(settings.sortField, settings.sortAscending)
    val pinned = if (searching) emptyList() else visible.filter { it.pinned }
    val others = if (searching) visible else visible.filterNot { it.pinned }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = ListPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader("Meu espaço pessoal", "Minhas notas") {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Tune, "Configurações", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        item { SearchField(query, onChange = { query = it }) }

        if (pinned.isNotEmpty()) {
            item {
                SectionHeader("Fixadas") {
                    Icon(
                        Icons.Outlined.PushPin,
                        null,
                        Modifier.padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            items(pinned, key = { "p" + it.id }) { note ->
                PinnedCard(note, onOpen = { onOpen(note.id) })
            }
        }

        item {
            SectionHeader(if (searching) "Resultados (${visible.size})" else "Todas as notas") {
                FilterButton()
                SortButton()
            }
        }
        if (others.isEmpty()) {
            item {
                when {
                    searching -> EmptyState(Icons.Outlined.Search, "Nada encontrado", "Tente outras palavras.")
                    pinned.isNotEmpty() -> EmptyState(Icons.Outlined.PushPin, "Só notas fixadas", "Toque no + para criar outra nota.")
                    settings.filter != NoteFilter.ALL -> EmptyState(Icons.Outlined.FilterAlt, "Nenhuma nota neste filtro", "Mude o filtro para ver todas.")
                    else -> EmptyState(Icons.Outlined.Edit, "Nenhuma nota ainda", "Toque no + para escrever a primeira.")
                }
            }
        }
        items(others, key = { it.id }) { note ->
            NoteRow(note, onOpen = { onOpen(note.id) })
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("search"),
        placeholder = { Text("Pesquisar notas...", fontSize = 18.sp) },
        leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.padding(start = 8.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, "Limpar") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun FilterButton() {
    val settings = Graph.settings
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                Icons.Outlined.FilterAlt,
                "Filtrar",
                tint = if (settings.filter != NoteFilter.ALL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            NoteFilter.entries.forEach { f ->
                DropdownMenuItem(
                    text = { Text(f.label) },
                    trailingIcon = { if (settings.filter == f) Icon(Icons.Filled.Check, null) },
                    onClick = { settings.updateFilter(f); open = false },
                )
            }
        }
    }
}

@Composable
private fun SortButton() {
    val settings = Graph.settings
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.SwapVert, "Ordenar", tint = MaterialTheme.colorScheme.onBackground)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortField.entries.forEach { field ->
                DropdownMenuItem(
                    text = { Text(field.label) },
                    trailingIcon = { if (settings.sortField == field) Icon(Icons.Filled.Check, null) },
                    onClick = {
                        // Nome em ordem alfabética; datas da mais recente para a mais antiga.
                        settings.updateSort(field, ascending = field == SortField.TITLE)
                        open = false
                    },
                )
            }
            HorizontalDivider()
            val label = when (settings.sortField) {
                SortField.TITLE -> if (settings.sortAscending) "Ordem: A → Z" else "Ordem: Z → A"
                else -> if (settings.sortAscending) "Ordem: mais antigas primeiro" else "Ordem: mais recentes primeiro"
            }
            DropdownMenuItem(
                text = { Text(label) },
                leadingIcon = { Icon(Icons.Outlined.SwapVert, null) },
                onClick = { settings.updateSort(settings.sortField, !settings.sortAscending); open = false },
            )
        }
    }
}

// ---------------------------------------------------------------- Pastas

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(onOpenFolder: (Long) -> Unit) {
    val repo = Graph.repo
    var creating by remember { mutableStateOf(false) }
    val active = repo.notes.filterNot { it.isDeleted }
    val folders = repo.folders.sortedBy { it.name.lowercase() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHeader("Organize suas notas", "Pastas") {
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Outlined.CreateNewFolder, "Nova pasta", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        if (folders.isEmpty()) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    EmptyState(Icons.Outlined.Folder, "Nenhuma pasta", "Crie pastas para separar trabalho, casa, estudos...")
                    TextButton(onClick = { creating = true }) { Text("Criar pasta") }
                }
            }
        }
        items(folders, key = { it.id }) { folder ->
            var menu by remember { mutableStateOf(false) }
            var renaming by remember { mutableStateOf(false) }
            var deleting by remember { mutableStateOf(false) }
            val count = active.count { it.folderId == folder.id }
            Box {
                Surface(
                    shape = CardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .combinedClickable(onClick = { onOpenFolder(folder.id) }, onLongClick = { menu = true }),
                ) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Folder, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(folder.name, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                when (count) { 0 -> "Vazia"; 1 -> "1 nota"; else -> "$count notas" },
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Renomear") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menu = false; renaming = true })
                    DropdownMenuItem(
                        text = { Text("Excluir pasta", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; deleting = true },
                    )
                }
            }
            if (renaming) {
                TextInputDialog("Renomear pasta", folder.name, "Nome", "Salvar", { repo.renameFolder(folder.id, it) }, { renaming = false })
            }
            if (deleting) {
                ConfirmDialog(
                    "Excluir pasta?",
                    "A pasta \"${folder.name}\" será excluída. As notas dela não serão apagadas, apenas ficarão sem pasta.",
                    "Excluir",
                    onConfirm = { repo.deleteFolder(folder.id) },
                    onDismiss = { deleting = false },
                )
            }
        }
    }
    if (creating) {
        TextInputDialog("Nova pasta", "", "Nome da pasta", "Criar", { repo.createFolder(it) }, { creating = false })
    }
}

@Composable
fun FolderDetailScreen(folderId: Long, onOpen: (Long) -> Unit, onBack: () -> Unit) {
    val repo = Graph.repo
    val settings = Graph.settings
    val folder = repo.folder(folderId)
    if (folder == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val notes = repo.notes
        .filter { !it.isDeleted && it.folderId == folderId }
        .sortedFor(settings.sortField, settings.sortAscending)
        .sortedByDescending { it.pinned }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = MaterialTheme.colorScheme.onBackground)
                }
                ScreenHeader("Pasta", folder.name, Modifier.weight(1f)) {
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Outlined.MoreVert, "Mais opções", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Renomear") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menu = false; renaming = true })
                            DropdownMenuItem(
                                text = { Text("Excluir pasta", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; deleting = true },
                            )
                        }
                    }
                }
            }
        }
        if (notes.isEmpty()) {
            item { EmptyState(Icons.Outlined.Folder, "Pasta vazia", "Toque no + para criar uma nota aqui.") }
        }
        items(notes, key = { it.id }) { note -> NoteRow(note, onOpen = { onOpen(note.id) }, showFolder = false) }
    }
    if (renaming) {
        TextInputDialog("Renomear pasta", folder.name, "Nome", "Salvar", { repo.renameFolder(folder.id, it) }, { renaming = false })
    }
    if (deleting) {
        ConfirmDialog(
            "Excluir pasta?",
            "A pasta \"${folder.name}\" será excluída. As notas dela não serão apagadas, apenas ficarão sem pasta.",
            "Excluir",
            onConfirm = { repo.deleteFolder(folder.id); onBack() },
            onDismiss = { deleting = false },
        )
    }
}

// ---------------------------------------------------------------- Favoritos

@Composable
fun FavoritesScreen(onOpen: (Long) -> Unit) {
    val repo = Graph.repo
    val settings = Graph.settings
    val notes = repo.notes
        .filter { !it.isDeleted && it.favorite }
        .sortedFor(settings.sortField, settings.sortAscending)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader("Suas notas especiais", "Favoritos") }
        if (notes.isEmpty()) {
            item { EmptyState(Icons.Outlined.StarBorder, "Nenhuma favorita", "Toque na estrela de uma nota para encontrá-la aqui.") }
        }
        items(notes, key = { it.id }) { note -> NoteRow(note, onOpen = { onOpen(note.id) }) }
    }
}

// ---------------------------------------------------------------- Lixeira

@Composable
fun TrashScreen() {
    val repo = Graph.repo
    var emptying by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<Long?>(null) }
    val notes = repo.notes.filter { it.isDeleted }.sortedByDescending { it.deletedAt }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHeader("Notas apagadas", "Lixeira") {
                if (notes.isNotEmpty()) {
                    TextButton(onClick = { emptying = true }) {
                        Text("Esvaziar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item {
            Text(
                "As notas ficam aqui por ${NotesRepository.TRASH_RETENTION_DAYS} dias e depois são excluídas definitivamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (notes.isEmpty()) {
            item { EmptyState(Icons.Outlined.Delete, "Lixeira vazia", "Notas apagadas aparecem aqui e podem ser recuperadas.") }
        }
        items(notes, key = { it.id }) { note ->
            val daysLeft = max(
                0,
                NotesRepository.TRASH_RETENTION_DAYS -
                    ((System.currentTimeMillis() - (note.deletedAt ?: 0L)) / (24L * 60 * 60 * 1000)).toInt(),
            )
            Surface(
                shape = CardShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(note.icon.vector, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(note.displayTitle(), fontSize = 18.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (daysLeft == 1) "Será excluída em 1 dia" else "Será excluída em $daysLeft dias",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { repo.restore(note.id) }) {
                        Icon(Icons.Outlined.RestoreFromTrash, "Restaurar", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { deletingId = note.id }) {
                        Icon(Icons.Outlined.DeleteForever, "Excluir definitivamente", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (emptying) {
        ConfirmDialog(
            "Esvaziar a lixeira?",
            "Todas as ${notes.size} notas da lixeira serão excluídas para sempre. Não é possível desfazer.",
            "Esvaziar",
            onConfirm = { repo.emptyTrash() },
            onDismiss = { emptying = false },
        )
    }
    deletingId?.let { id ->
        val note = repo.note(id)
        ConfirmDialog(
            "Excluir definitivamente?",
            "\"${note?.displayTitle() ?: "Nota"}\" será apagada para sempre. Não é possível desfazer.",
            "Excluir",
            onConfirm = { repo.deletePermanently(id) },
            onDismiss = { deletingId = null },
        )
    }
}
