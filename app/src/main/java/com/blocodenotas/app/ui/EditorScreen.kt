package com.blocodenotas.app.ui

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blocodenotas.app.Graph
import com.blocodenotas.app.data.Block
import com.blocodenotas.app.data.BlockType
import com.blocodenotas.app.data.Note
import com.blocodenotas.app.data.NoteIcon
import com.blocodenotas.app.data.countWords

/** Parte editável de uma nota, usada pelo desfazer/refazer. */
private data class EditState(val title: String, val blocks: List<Block>)

private fun Note.editState() = EditState(title, blocks)

/**
 * Histórico de edições. Digitação contínua vira um único passo: um novo ponto de desfazer
 * só é criado depois de uma pausa ou quando o tipo de alteração muda.
 */
private class EditHistory {
    private val undoStack = ArrayDeque<EditState>()
    private val redoStack = ArrayDeque<EditState>()
    private var lastTime = 0L
    private var lastKind = ""

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    fun record(before: EditState, kind: String) {
        val now = SystemClock.uptimeMillis()
        if (undoStack.isEmpty() || kind != lastKind || now - lastTime > 1000) {
            undoStack.addLast(before)
            if (undoStack.size > 200) undoStack.removeFirst()
        }
        lastTime = now
        lastKind = kind
        redoStack.clear()
        refresh()
    }

    fun undo(current: EditState): EditState? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        lastKind = ""
        refresh()
        return previous
    }

    fun redo(current: EditState): EditState? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        lastKind = ""
        refresh()
        return next
    }

    private fun refresh() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(noteId: Long, onBack: () -> Unit, onOpenNote: (Long) -> Unit) {
    val repo = Graph.repo
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val note = repo.note(noteId)
    if (note == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val history = remember(noteId) { EditHistory() }
    val state = remember(noteId) {
        BlockEditorState(noteId) { kind, transform ->
            // Toda alteração vai direto para o repositório, que grava no aparelho em seguida.
            repo.note(noteId)?.let { history.record(it.editState(), kind) }
            repo.updateNote(noteId, transform = transform)
        }
    }
    val titleRequester = remember { FocusRequester() }
    var menu by remember { mutableStateOf(false) }
    var confirmTrash by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var pickIcon by remember { mutableStateOf(false) }
    var pickFolder by remember { mutableStateOf(false) }

    fun applyState(s: EditState) {
        state.slash = null
        repo.updateNote(noteId) { it.copy(title = s.title, blocks = s.blocks) }
    }

    fun leave() {
        val current = repo.note(noteId)
        if (current != null && current.isBlank && !current.isDeleted) repo.deletePermanently(noteId)
        repo.flush()
        onBack()
    }

    LaunchedEffect(noteId) {
        if (repo.note(noteId)?.blocks.isNullOrEmpty()) {
            repo.updateNote(noteId, touch = false) { it.copy(blocks = listOf(Block(repo.newId()))) }
        }
        if (repo.note(noteId)?.isBlank == true) runCatching { titleRequester.requestFocus() }
    }

    BackHandler { leave() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {},
                navigationIcon = {
                    IconButton(onClick = { leave() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                actions = {
                    IconButton(enabled = history.canUndo, onClick = { history.undo(note.editState())?.let { applyState(it) } }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Desfazer")
                    }
                    IconButton(enabled = history.canRedo, onClick = { history.redo(note.editState())?.let { applyState(it) } }) {
                        Icon(Icons.AutoMirrored.Filled.Redo, "Refazer")
                    }
                    IconButton(onClick = { repo.togglePinned(noteId) }) {
                        Icon(
                            if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            if (note.pinned) "Desafixar" else "Fixar",
                            tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { repo.toggleFavorite(noteId) }) {
                        Icon(
                            if (note.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            if (note.favorite) "Remover dos favoritos" else "Favoritar",
                            tint = if (note.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Mais opções") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Copiar todo o conteúdo") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                            onClick = {
                                menu = false
                                clipboard.setText(AnnotatedString(note.fullText()))
                                Toast.makeText(context, "Conteúdo copiado", Toast.LENGTH_SHORT).show()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicar nota") },
                            leadingIcon = { Icon(Icons.Outlined.LibraryAdd, null) },
                            onClick = {
                                menu = false
                                repo.duplicate(noteId)?.let {
                                    Toast.makeText(context, "Cópia criada", Toast.LENGTH_SHORT).show()
                                    onOpenNote(it.id)
                                }
                            },
                        )
                        if (note.blocks.any { it.type == BlockType.TODO && it.checked }) {
                            DropdownMenuItem(
                                text = { Text("Remover tarefas concluídas") },
                                leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) },
                                onClick = {
                                    menu = false
                                    state.edit("clearChecked") { n ->
                                        val left = n.blocks.filterNot { it.type == BlockType.TODO && it.checked }
                                        n.copy(blocks = left.ifEmpty { listOf(Block(repo.newId())) })
                                    }
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Mudar ícone") },
                            leadingIcon = { Icon(Icons.Outlined.Palette, null) },
                            onClick = { menu = false; pickIcon = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Mover para pasta") },
                            leadingIcon = { Icon(Icons.Outlined.Folder, null) },
                            onClick = { menu = false; pickFolder = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Informações") },
                            leadingIcon = { Icon(Icons.Outlined.Info, null) },
                            onClick = { menu = false; showInfo = true },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Mover para a lixeira", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; confirmTrash = true },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                PlainTextField(
                    value = note.title,
                    onValueChange = { value -> state.edit("title") { it.copy(title = value) } },
                    placeholder = "Título",
                    textStyle = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp).focusRequester(titleRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { state.focusFirst() }),
                )
                val folder = repo.folder(note.folderId)
                Text(
                    buildString {
                        append("Editada ").append(relativeDay(note.updatedAt))
                        if (folder != null) append("  ·  ").append(folder.name)
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                BlockList(note, state)
                // Área livre abaixo do texto: tocar nela continua a escrever no fim.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.focusEnd() },
                )
            }
            SlashMenu(state)
            if (state.focusedId != null) BlockToolbar(note, state) else StatsBar(note)
        }
    }

    if (confirmTrash) {
        ConfirmDialog(
            title = "Mover para a lixeira?",
            message = "Você poderá recuperar esta nota na Lixeira por até 30 dias.",
            confirmLabel = "Mover",
            onConfirm = {
                if (note.isBlank) repo.deletePermanently(noteId) else repo.moveToTrash(noteId)
                repo.flush()
                onBack()
            },
            onDismiss = { confirmTrash = false },
        )
    }
    if (showInfo) InfoDialog(note, onDismiss = { showInfo = false })
    if (pickIcon) {
        IconPickerDialog(
            current = note.icon,
            onPick = { icon -> repo.updateNote(noteId, touch = false) { it.copy(icon = icon) } },
            onDismiss = { pickIcon = false },
        )
    }
    if (pickFolder) {
        FolderPickerDialog(
            current = note.folderId,
            onPick = { id -> repo.updateNote(noteId, touch = false) { it.copy(folderId = id) } },
            onDismiss = { pickFolder = false },
        )
    }
}

@Composable
private fun StatsBar(note: Note) {
    val text = note.title + "\n" + note.plainText()
    val words = countWords(text)
    val chars = (note.title + note.plainText()).length
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(
                    "${if (words == 1) "1 palavra" else "$words palavras"}  ·  $chars caracteres",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text("Salvo automaticamente", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoDialog(note: Note, onDismiss: () -> Unit) {
    val body = note.plainText()
    val folder = Graph.repo.folder(note.folderId)
    val todos = note.blocks.filter { it.type == BlockType.TODO }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informações") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoLine("Criada em", formatDateTime(note.createdAt))
                InfoLine("Última alteração", formatDateTime(note.updatedAt))
                InfoLine("Pasta", folder?.name ?: "Sem pasta")
                InfoLine("Palavras", countWords(note.title + "\n" + body).toString())
                InfoLine("Caracteres", (note.title + body).length.toString())
                InfoLine("Blocos", note.blocks.size.toString())
                if (todos.isNotEmpty()) InfoLine("Tarefas", "${todos.count { it.checked }} de ${todos.size} concluídas")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun IconPickerDialog(current: NoteIcon, onPick: (NoteIcon) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ícone da nota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoteIcon.entries.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { icon ->
                            val selected = icon == current
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPick(icon); onDismiss() },
                            ) {
                                Column(
                                    Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(icon.vector, null, tint = MaterialTheme.colorScheme.onSurface)
                                    Text(icon.label, fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun FolderPickerDialog(current: Long?, onPick: (Long?) -> Unit, onDismiss: () -> Unit) {
    val repo = Graph.repo
    var creating by remember { mutableStateOf(false) }
    val options: List<Pair<Long?, String>> =
        listOf<Pair<Long?, String>>(null to "Sem pasta") + repo.folders.sortedBy { it.name.lowercase() }.map { it.id to it.name }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mover para pasta") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (id, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(id); onDismiss() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = current == id, onClick = { onPick(id); onDismiss() })
                        Spacer(Modifier.width(8.dp))
                        Text(name, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
        dismissButton = { TextButton(onClick = { creating = true }) { Text("Nova pasta") } },
    )
    if (creating) {
        TextInputDialog(
            "Nova pasta",
            "",
            "Nome da pasta",
            "Criar e mover",
            onConfirm = { name -> onPick(repo.createFolder(name).id); onDismiss() },
            onDismiss = { creating = false },
        )
    }
}
