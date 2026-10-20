package com.blocodenotas.app.ui

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blocodenotas.app.Graph
import com.blocodenotas.app.data.ChecklistItem
import com.blocodenotas.app.data.Note
import com.blocodenotas.app.data.NoteIcon
import com.blocodenotas.app.data.countWords

/** Parte editável de uma nota, usada pelo desfazer/refazer. */
private data class EditState(
    val title: String,
    val content: String,
    val isChecklist: Boolean,
    val items: List<ChecklistItem>,
)

private fun Note.editState() = EditState(title, content, isChecklist, items)

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
    var menu by remember { mutableStateOf(false) }
    var confirmTrash by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var pickIcon by remember { mutableStateOf(false) }
    var pickFolder by remember { mutableStateOf(false) }
    var focusItemId by remember { mutableStateOf<Long?>(null) }

    // Toda alteração vai direto para o repositório, que grava no aparelho em seguida.
    fun edit(kind: String, transform: (Note) -> Note) {
        history.record(note.editState(), kind)
        repo.updateNote(noteId, transform = transform)
    }

    fun applyState(state: EditState) {
        repo.updateNote(noteId) {
            it.copy(title = state.title, content = state.content, isChecklist = state.isChecklist, items = state.items)
        }
    }

    fun leave() {
        val current = repo.note(noteId)
        if (current != null && current.isBlank && !current.isDeleted) repo.deletePermanently(noteId)
        repo.flush()
        onBack()
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
                        if (note.isChecklist) {
                            DropdownMenuItem(
                                text = { Text("Converter em texto") },
                                leadingIcon = { Icon(Icons.Outlined.Description, null) },
                                onClick = {
                                    menu = false
                                    edit("convert") { n ->
                                        n.copy(
                                            isChecklist = false,
                                            content = n.items.filter { it.text.isNotBlank() }.joinToString("\n") { it.text },
                                            items = emptyList(),
                                        )
                                    }
                                },
                            )
                            if (note.items.any { it.checked }) {
                                DropdownMenuItem(
                                    text = { Text("Remover itens marcados") },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) },
                                    onClick = {
                                        menu = false
                                        edit("clearChecked") { n -> n.copy(items = n.items.filterNot { it.checked }) }
                                    },
                                )
                            }
                        } else {
                            DropdownMenuItem(
                                text = { Text("Converter em lista") },
                                leadingIcon = { Icon(Icons.Outlined.CheckBox, null) },
                                onClick = {
                                    menu = false
                                    edit("convert") { n ->
                                        val lines = n.content.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                        n.copy(
                                            isChecklist = true,
                                            content = "",
                                            items = (lines.ifEmpty { listOf("") }).map { ChecklistItem(repo.newId(), it) },
                                        )
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
                    onValueChange = { value -> edit("title") { it.copy(title = value) } },
                    placeholder = "Título",
                    textStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                Spacer(Modifier.height(16.dp))

                if (note.isChecklist) {
                    ChecklistEditor(
                        items = note.items,
                        focusItemId = focusItemId,
                        onFocusHandled = { focusItemId = null },
                        onChange = { kind, items -> edit(kind) { it.copy(items = items) } },
                        newItem = { ChecklistItem(repo.newId(), "") },
                        onRequestFocus = { focusItemId = it },
                    )
                } else {
                    PlainTextField(
                        value = note.content,
                        onValueChange = { value -> edit("content") { it.copy(content = value) } },
                        placeholder = "Comece a escrever...",
                        textStyle = TextStyle(fontSize = 18.sp, lineHeight = 27.sp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
            StatsBar(note)
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
private fun ChecklistEditor(
    items: List<ChecklistItem>,
    focusItemId: Long?,
    onFocusHandled: () -> Unit,
    onChange: (kind: String, items: List<ChecklistItem>) -> Unit,
    newItem: () -> ChecklistItem,
    onRequestFocus: (Long) -> Unit,
) {
    val requesters = remember { mutableStateMapOf<Long, FocusRequester>() }
    val done = items.count { it.checked }
    if (items.isNotEmpty()) {
        Text(
            "$done de ${items.size} concluídos",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEachIndexed { index, item ->
            val requester = requesters.getOrPut(item.id) { FocusRequester() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = { checked ->
                        onChange("check", items.map { if (it.id == item.id) it.copy(checked = checked) else it })
                    },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                PlainTextField(
                    value = item.text,
                    onValueChange = { value ->
                        onChange("item${item.id}", items.map { if (it.id == item.id) it.copy(text = value) else it })
                    },
                    placeholder = "Item da lista",
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f).focusRequester(requester),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        // "Próximo" no teclado cria um novo item logo abaixo.
                        val added = newItem()
                        onChange("add", items.toMutableList().apply { add(index + 1, added) })
                        onRequestFocus(added.id)
                    }),
                )
                IconButton(onClick = { onChange("remove", items.filterNot { it.id == item.id }) }) {
                    Icon(
                        Icons.Outlined.Close,
                        "Remover item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (focusItemId == item.id) {
                LaunchedEffect(item.id) {
                    runCatching { requester.requestFocus() }
                    onFocusHandled()
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    val added = newItem()
                    onChange("add", items + added)
                    onRequestFocus(added.id)
                }
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text("Adicionar item", fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StatsBar(note: Note) {
    val text = note.bodyText()
    val words = countWords(note.title + " " + text)
    val chars = (note.title + text).length
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
    val body = note.bodyText()
    val folder = Graph.repo.folder(note.folderId)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informações") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoLine("Criada em", formatDateTime(note.createdAt))
                InfoLine("Última alteração", formatDateTime(note.updatedAt))
                InfoLine("Tipo", if (note.isChecklist) "Lista (${note.items.size} itens)" else "Texto")
                InfoLine("Pasta", folder?.name ?: "Sem pasta")
                InfoLine("Palavras", countWords(note.title + " " + body).toString())
                InfoLine("Caracteres", (note.title + body).length.toString())
                InfoLine("Linhas", if (body.isEmpty()) "0" else body.lines().size.toString())
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
