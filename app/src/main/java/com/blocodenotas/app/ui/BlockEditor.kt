package com.blocodenotas.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FormatClear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blocodenotas.app.Graph
import com.blocodenotas.app.data.Block
import com.blocodenotas.app.data.BlockType
import com.blocodenotas.app.data.MarkKind
import com.blocodenotas.app.data.Marks
import com.blocodenotas.app.data.Note
import com.blocodenotas.app.data.TextMark
import com.blocodenotas.app.data.normalizeForSearch

/**
 * Cada campo começa com um caractere invisível. Quando ele some, o usuário apertou
 * backspace no início do bloco (o teclado não avisa isso de outra forma).
 */
private const val ZW = "​"

// ---------------------------------------------------------------- Cores

val PaletteNames = listOf("Padrão", "Cinza", "Marrom", "Laranja", "Amarelo", "Verde", "Azul", "Roxo", "Rosa", "Vermelho")

private val PaletteColors = listOf(
    Color.Unspecified,
    Color(0xFF8F8E8A),
    Color(0xFFA9714B),
    Color(0xFFE5822F),
    Color(0xFFD4A20C),
    Color(0xFF3DA368),
    Color(0xFF3B8BDB),
    Color(0xFF9466D6),
    Color(0xFFD5559E),
    Color(0xFFE24C46),
)

fun paletteColor(index: Int): Color = PaletteColors.getOrElse(index) { Color.Unspecified }

fun highlightColor(index: Int): Color =
    if (index == 0) Color.Transparent else paletteColor(index).copy(alpha = 0.32f)

private fun spanFor(mark: TextMark): SpanStyle = when (mark.kind) {
    MarkKind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    MarkKind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    MarkKind.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
    MarkKind.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    MarkKind.COLOR -> SpanStyle(color = paletteColor(mark.color))
    MarkKind.HIGHLIGHT -> SpanStyle(background = highlightColor(mark.color))
}

/** Desenha as marcações sobre o texto do campo (que tem o caractere invisível no início). */
private data class MarksTransformation(val marks: List<TextMark>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (marks.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val builder = AnnotatedString.Builder(text)
        for (m in marks) {
            val s = (m.start + 1).coerceIn(0, text.length)
            val e = (m.end + 1).coerceIn(0, text.length)
            if (e > s) builder.addStyle(spanFor(m), s, e)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

// ---------------------------------------------------------------- Comandos

data class SlashCommand(
    val type: BlockType,
    val label: String,
    val glyph: String,
    val hint: String,
    val keys: List<String>,
)

val SlashCommands = listOf(
    SlashCommand(BlockType.TEXT, "Texto", "T", "/texto", listOf("texto", "text", "paragrafo", "p")),
    SlashCommand(BlockType.H1, "Título 1", "H1", "/h1  ou  # ", listOf("h1", "titulo1", "titulo", "heading1", "heading")),
    SlashCommand(BlockType.H2, "Título 2", "H2", "/h2  ou  ## ", listOf("h2", "titulo2", "subtitulo", "heading2")),
    SlashCommand(BlockType.H3, "Título 3", "H3", "/h3  ou  ### ", listOf("h3", "titulo3", "heading3")),
    SlashCommand(BlockType.BULLET, "Lista com marcadores", "•", "/lista  ou  - ", listOf("lista", "list", "bullet", "marcador", "ul")),
    SlashCommand(BlockType.NUMBERED, "Lista numerada", "1.", "/numerada  ou  1. ", listOf("numerada", "numero", "numbered", "ol")),
    SlashCommand(BlockType.TODO, "Lista de tarefas", "☑", "/todo  ou  [] ", listOf("todo", "tarefa", "check", "checklist", "caixa")),
    SlashCommand(BlockType.QUOTE, "Citação", "❝", "/citacao  ou  > ", listOf("citacao", "quote", "aspas")),
    SlashCommand(BlockType.CALLOUT, "Destaque", "💡", "/destaque", listOf("destaque", "callout", "aviso", "nota")),
    SlashCommand(BlockType.CODE, "Código", "</>", "/codigo  ou  ```", listOf("codigo", "code")),
    SlashCommand(BlockType.DIVIDER, "Divisor", "—", "/divisor  ou  ---", listOf("divisor", "divider", "linha", "hr")),
)

fun matchCommands(query: String): List<SlashCommand> {
    val q = normalizeForSearch(query)
    if (q.isEmpty()) return SlashCommands
    return SlashCommands.filter { c -> c.keys.any { it.startsWith(q) } || normalizeForSearch(c.label).contains(q) }
}

/** Atalhos estilo Markdown digitados no começo de um bloco de texto. */
private val Shortcuts = listOf(
    "### " to BlockType.H3,
    "## " to BlockType.H2,
    "# " to BlockType.H1,
    "- " to BlockType.BULLET,
    "* " to BlockType.BULLET,
    "1. " to BlockType.NUMBERED,
    "[] " to BlockType.TODO,
    "[ ] " to BlockType.TODO,
    "> " to BlockType.QUOTE,
    "```" to BlockType.CODE,
    "---" to BlockType.DIVIDER,
)

data class SlashState(val blockId: Long, val start: Int, val query: String)

// ---------------------------------------------------------------- Estado e regras de edição

class BlockEditorState(
    private val noteId: Long,
    /** Registra no histórico (desfazer) e grava a alteração. */
    val edit: (kind: String, transform: (Note) -> Note) -> Unit,
) {
    private val repo = Graph.repo
    private val requesters = HashMap<Long, FocusRequester>()

    val values = mutableStateMapOf<Long, TextFieldValue>()
    var focusedId by mutableStateOf<Long?>(null)
    var pendingFocus by mutableStateOf<Long?>(null)
    var slash by mutableStateOf<SlashState?>(null)

    fun requester(id: Long): FocusRequester = requesters.getOrPut(id) { FocusRequester() }

    fun value(block: Block): TextFieldValue {
        val expected = ZW + block.text
        val current = values[block.id]
        return if (current != null && current.text == expected) current else TextFieldValue(expected, TextRange(expected.length))
    }

    fun focus(id: Long, cursor: Int, text: String) {
        values[id] = TextFieldValue(ZW + text, TextRange(cursor.coerceIn(0, text.length) + 1))
        pendingFocus = id
    }

    private fun blocks(): List<Block>? = repo.note(noteId)?.blocks

    private fun commit(kind: String, blocks: List<Block>) = edit(kind) { it.copy(blocks = blocks) }

    fun onValueChange(blockId: Long, input: TextFieldValue) {
        val blocks = blocks() ?: return
        val index = blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return
        val block = blocks[index]

        var nv = input
        if (!nv.text.startsWith(ZW)) {
            if (nv.text == block.text) {
                backspaceAtStart(blocks, index)
                return
            }
            nv = TextFieldValue(ZW + nv.text, TextRange(nv.selection.start + 1, nv.selection.end + 1))
        }
        if (nv.selection.min < 1) {
            nv = TextFieldValue(nv.text, TextRange(maxOf(1, nv.selection.start), maxOf(1, nv.selection.end)), nv.composition)
        }
        val newText = nv.text.substring(1)
        val cursor = nv.selection.end - 1

        if (newText == block.text) {
            values[blockId] = nv
            updateSlash(blockId, newText, cursor)
            return
        }

        if (block.type != BlockType.CODE && newText.contains('\n')) {
            // Enter com o menu de comandos aberto escolhe o primeiro comando.
            val s = slash
            if (s != null && s.blockId == blockId && newText.length == block.text.length + 1) {
                val first = matchCommands(s.query).firstOrNull()
                if (first != null) {
                    applyCommand(first)
                    return
                }
            }
            splitLines(blocks, index, newText)
            return
        }

        val marks = Marks.adjust(block.marks, block.text, newText)
        if (block.type == BlockType.TEXT) {
            val shortcut = Shortcuts.firstOrNull { (prefix, _) -> newText.startsWith(prefix) && cursor == prefix.length }
            if (shortcut != null) {
                applyShortcut(blocks, index, newText, marks, shortcut.first.length, shortcut.second)
                return
            }
        }

        values[blockId] = nv
        edit("text$blockId") { n ->
            n.copy(blocks = n.blocks.map { if (it.id == blockId) it.copy(text = newText, marks = marks) else it })
        }
        updateSlash(blockId, newText, cursor)
    }

    private fun updateSlash(blockId: Long, text: String, cursor: Int) {
        val c = cursor.coerceIn(0, text.length)
        val idx = if (c == 0) -1 else text.lastIndexOf('/', c - 1)
        slash = if (idx >= 0 && (idx == 0 || text[idx - 1].isWhitespace())) {
            val query = text.substring(idx + 1, c)
            if (query.length <= 20 && query.none { it.isWhitespace() }) SlashState(blockId, idx, query) else null
        } else {
            null
        }
    }

    private fun applyShortcut(
        blocks: List<Block>,
        index: Int,
        text: String,
        marks: List<TextMark>,
        prefixLength: Int,
        type: BlockType,
    ) {
        val block = blocks[index]
        val rest = text.substring(prefixLength)
        val restMarks = Marks.adjust(marks, text, rest)
        val result = blocks.toMutableList()
        slash = null
        if (type == BlockType.DIVIDER) {
            val next = Block(repo.newId(), BlockType.TEXT, rest, restMarks)
            result[index] = Block(block.id, BlockType.DIVIDER)
            result.add(index + 1, next)
            focus(next.id, 0, rest)
        } else {
            result[index] = block.copy(type = type, text = rest, marks = restMarks)
            values[block.id] = TextFieldValue(ZW + rest, TextRange(1))
        }
        commit("type", result)
    }

    private fun splitLines(blocks: List<Block>, index: Int, newText: String) {
        val block = blocks[index]
        slash = null

        // Enter em um item de lista vazio sai da lista.
        if (block.type.isList && block.text.isEmpty() && newText == "\n") {
            values[block.id] = TextFieldValue(ZW, TextRange(1))
            commit("type", blocks.map { if (it.id == block.id) it.copy(type = BlockType.TEXT, checked = false) else it })
            return
        }

        val lines = newText.split('\n')
        // Enter no começo de um bloco com texto abre uma linha vazia acima dele.
        if (lines.size == 2 && lines[0].isEmpty() && lines[1] == block.text) {
            val above = Block(repo.newId(), BlockType.TEXT)
            focus(block.id, 0, block.text)
            commit("split", blocks.toMutableList().apply { add(index, above) })
            return
        }

        val marks = Marks.adjust(block.marks, block.text, newText)
        var offset = 0
        val created = lines.mapIndexed { i, line ->
            val m = Marks.slice(marks, offset, offset + line.length)
            offset += line.length + 1
            if (i == 0) block.copy(text = line, marks = m) else Block(repo.newId(), block.type.continuation, line, m)
        }
        values[block.id] = TextFieldValue(ZW + created.first().text, TextRange(created.first().text.length + 1))
        val last = created.last()
        focus(last.id, if (lines.size == 2) 0 else last.text.length, last.text)
        commit("split", blocks.take(index) + created + blocks.drop(index + 1))
    }

    private fun backspaceAtStart(blocks: List<Block>, index: Int) {
        val block = blocks[index]
        slash = null
        values[block.id] = TextFieldValue(ZW + block.text, TextRange(1))
        when {
            // Primeiro o bloco volta a ser texto simples, como no Notion.
            block.type != BlockType.TEXT ->
                commit("type", blocks.map { if (it.id == block.id) it.copy(type = BlockType.TEXT, checked = false) else it })
            index == 0 -> Unit
            blocks[index - 1].type == BlockType.DIVIDER ->
                commit("merge", blocks.filterIndexed { i, _ -> i != index - 1 })
            else -> {
                val prev = blocks[index - 1]
                val merged = prev.copy(
                    text = prev.text + block.text,
                    marks = prev.marks + Marks.shift(block.marks, prev.text.length),
                )
                focus(prev.id, prev.text.length, merged.text)
                commit("merge", blocks.toMutableList().apply { set(index - 1, merged); removeAt(index) })
            }
        }
    }

    fun applyCommand(command: SlashCommand) {
        val s = slash ?: return
        slash = null
        val blocks = blocks() ?: return
        val index = blocks.indexOfFirst { it.id == s.blockId }
        if (index < 0) return
        val block = blocks[index]
        if (block.text.getOrNull(s.start) != '/') return
        val end = (s.start + 1 + s.query.length).coerceAtMost(block.text.length)
        val text = block.text.removeRange(s.start, end)
        val marks = Marks.adjust(block.marks, block.text, text)
        changeType(blocks, index, block.copy(text = text, marks = marks), command.type, cursor = s.start)
    }

    /** Muda o tipo do bloco em foco (usado pela barra de ferramentas). */
    fun setType(type: BlockType) {
        val id = focusedId ?: return
        val blocks = blocks() ?: return
        val index = blocks.indexOfFirst { it.id == id }
        if (index < 0) return
        val block = blocks[index]
        val target = if (block.type == type && type != BlockType.TEXT) BlockType.TEXT else type
        val cursor = (values[id]?.selection?.end ?: (block.text.length + 1)) - 1
        changeType(blocks, index, block, target, cursor)
    }

    private fun changeType(blocks: List<Block>, index: Int, block: Block, type: BlockType, cursor: Int) {
        val result = blocks.toMutableList()
        if (type == BlockType.DIVIDER) {
            val next = Block(repo.newId())
            if (block.text.isBlank()) {
                result[index] = Block(block.id, BlockType.DIVIDER)
                result.add(index + 1, next)
            } else {
                result[index] = block
                result.add(index + 1, Block(repo.newId(), BlockType.DIVIDER))
                result.add(index + 2, next)
            }
            focus(next.id, 0, "")
        } else {
            result[index] = block.copy(type = type, checked = if (type == BlockType.TODO) block.checked else false)
            focus(block.id, cursor, block.text)
        }
        commit("type", result)
    }

    fun toggleChecked(blockId: Long) {
        val blocks = blocks() ?: return
        commit("check", blocks.map { if (it.id == blockId) it.copy(checked = !it.checked) else it })
    }

    fun removeBlock(blockId: Long) {
        val blocks = blocks() ?: return
        val result = blocks.filterNot { it.id == blockId }
        commit("remove", result.ifEmpty { listOf(Block(repo.newId())) })
    }

    /** Toque na área vazia abaixo do texto: continua escrevendo no fim. */
    fun focusEnd() {
        val blocks = blocks() ?: return
        val last = blocks.lastOrNull()
        if (last != null && last.type == BlockType.TEXT && last.text.isEmpty()) {
            focus(last.id, 0, "")
        } else {
            val next = Block(repo.newId())
            focus(next.id, 0, "")
            commit("add", blocks + next)
        }
    }

    fun focusFirst() {
        val first = blocks()?.firstOrNull { it.type != BlockType.DIVIDER } ?: return focusEnd()
        focus(first.id, first.text.length, first.text)
    }

    /** Trecho selecionado no bloco em foco, nas posições do texto do bloco. */
    fun selection(): Pair<Block, IntRange>? {
        val id = focusedId ?: return null
        val block = blocks()?.firstOrNull { it.id == id } ?: return null
        val sel = values[id]?.selection ?: return null
        if (sel.collapsed) return null
        val start = (sel.min - 1).coerceIn(0, block.text.length)
        val end = (sel.max - 1).coerceIn(0, block.text.length)
        return if (end > start) block to (start until end) else null
    }

    fun format(kind: MarkKind, color: Int = 0) {
        val (block, range) = selection() ?: return
        val start = range.first
        val end = range.last + 1
        val marks = when (kind) {
            MarkKind.COLOR, MarkKind.HIGHLIGHT -> Marks.setColor(block.marks, start, end, kind, color)
            else -> Marks.toggle(block.marks, start, end, kind)
        }
        edit("format") { n -> n.copy(blocks = n.blocks.map { if (it.id == block.id) it.copy(marks = marks) else it }) }
    }

    fun clearFormat() {
        val (block, range) = selection() ?: return
        val marks = Marks.remove(block.marks, range.first, range.last + 1, null)
        edit("format") { n -> n.copy(blocks = n.blocks.map { if (it.id == block.id) it.copy(marks = marks) else it }) }
    }
}

// ---------------------------------------------------------------- Interface

private fun styleFor(type: BlockType): TextStyle = when (type) {
    BlockType.H1 -> TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    BlockType.H2 -> TextStyle(fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold)
    BlockType.H3 -> TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
    BlockType.CODE -> TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontFamily = FontFamily.Monospace)
    BlockType.QUOTE -> TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontStyle = FontStyle.Italic)
    else -> TextStyle(fontSize = 17.sp, lineHeight = 25.sp)
}

private fun placeholderFor(type: BlockType): String = when (type) {
    BlockType.H1 -> "Título 1"
    BlockType.H2 -> "Título 2"
    BlockType.H3 -> "Título 3"
    BlockType.BULLET, BlockType.NUMBERED -> "Lista"
    BlockType.TODO -> "Tarefa"
    BlockType.QUOTE -> "Citação"
    BlockType.CODE -> "Código"
    BlockType.CALLOUT -> "Escreva algo importante"
    else -> "Digite / para ver os comandos"
}

@Composable
fun BlockList(note: Note, state: BlockEditorState) {
    val single = note.blocks.size == 1
    var number = 0
    Column {
        note.blocks.forEach { block ->
            number = if (block.type == BlockType.NUMBERED) number + 1 else 0
            val n = number
            key(block.id) {
                BlockRow(block, n, single, state)
            }
        }
    }
}

@Composable
private fun BlockRow(block: Block, number: Int, single: Boolean, state: BlockEditorState) {
    val colors = MaterialTheme.colorScheme
    val topPadding = when (block.type) {
        BlockType.H1 -> 14.dp
        BlockType.H2 -> 10.dp
        BlockType.H3 -> 6.dp
        BlockType.CALLOUT, BlockType.CODE -> 6.dp
        else -> 0.dp
    }
    val base = styleFor(block.type)
    val style = if (block.type == BlockType.TODO && block.checked) {
        base.copy(color = colors.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)
    } else {
        base.copy(color = colors.onBackground)
    }
    val field: @Composable (Modifier) -> Unit = { modifier -> BlockField(block, state, style, single, modifier) }

    Box(Modifier.fillMaxWidth().padding(top = topPadding, bottom = 2.dp)) {
        when (block.type) {
            BlockType.DIVIDER -> Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = colors.outline)
                IconButton(onClick = { state.removeBlock(block.id) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Close, "Remover divisor", Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                }
            }
            BlockType.BULLET -> Row(Modifier.padding(vertical = 2.dp)) {
                Text("•", style = style.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(26.dp).padding(start = 6.dp))
                field(Modifier.weight(1f))
            }
            BlockType.NUMBERED -> Row(Modifier.padding(vertical = 2.dp)) {
                Text("$number.", style = style, modifier = Modifier.widthIn(min = 26.dp).padding(end = 6.dp))
                field(Modifier.weight(1f))
            }
            BlockType.TODO -> Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = block.checked, onCheckedChange = { state.toggleChecked(block.id) })
                field(Modifier.weight(1f))
            }
            BlockType.QUOTE -> {
                val bar = colors.onBackground
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .drawBehind { drawRect(bar, size = Size(3.dp.toPx(), size.height)) }
                        .padding(start = 14.dp),
                ) { field(Modifier.fillMaxWidth()) }
            }
            BlockType.CALLOUT -> Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("💡", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    field(Modifier.weight(1f))
                }
            }
            BlockType.CODE -> Surface(shape = RoundedCornerShape(10.dp), color = colors.surfaceVariant) {
                field(Modifier.fillMaxWidth().padding(12.dp))
            }
            else -> field(Modifier.fillMaxWidth().padding(vertical = 3.dp))
        }
    }
}

@Composable
private fun BlockField(block: Block, state: BlockEditorState, style: TextStyle, single: Boolean, modifier: Modifier) {
    val requester = state.requester(block.id)
    val focused = state.focusedId == block.id
    val transformation = remember(block.marks) { MarksTransformation(block.marks) }
    BasicTextField(
        value = state.value(block),
        onValueChange = { state.onValueChange(block.id, it) },
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier
            .focusRequester(requester)
            .onFocusChanged {
                if (it.isFocused) {
                    state.focusedId = block.id
                } else if (state.focusedId == block.id) {
                    state.focusedId = null
                }
            },
        decorationBox = { inner ->
            Box {
                if (block.text.isEmpty() && (focused || single || block.type != BlockType.TEXT)) {
                    Text(placeholderFor(block.type), style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textDecoration = TextDecoration.None))
                }
                inner()
            }
        },
    )
    LaunchedEffect(state.pendingFocus) {
        if (state.pendingFocus == block.id) {
            runCatching { requester.requestFocus() }
            state.pendingFocus = null
        }
    }
}

/** Lista de comandos que aparece ao digitar "/". */
@Composable
fun SlashMenu(state: BlockEditorState) {
    val s = state.slash ?: return
    if (state.focusedId != s.blockId) return
    val commands = matchCommands(s.query)
    if (commands.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 270.dp).verticalScroll(rememberScrollState())) {
            Text(
                if (s.query.isEmpty()) "Blocos" else "Blocos para \"/${s.query}\"",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            )
            commands.forEach { command ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { state.applyCommand(command) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Glyph(command.glyph)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(command.label, fontSize = 16.sp, color = colors.onSurface)
                        Text(command.hint, fontSize = 12.sp, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Glyph(text: String, active: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) colors.primaryContainer else colors.surfaceVariant)
            .border(1.dp, if (active) colors.primary else colors.outlineVariant, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = if (text.length > 2) 12.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) colors.onPrimaryContainer else colors.onSurface,
        )
    }
}

/**
 * Barra acima do teclado. Sem seleção, troca o tipo do bloco; com palavras selecionadas,
 * aplica negrito, itálico, sublinhado, tachado, cor do texto e cor de fundo.
 */
@Composable
fun BlockToolbar(note: Note, state: BlockEditorState) {
    val id = state.focusedId ?: return
    val block = note.blocks.firstOrNull { it.id == id } ?: return
    val colors = MaterialTheme.colorScheme
    val selection = state.selection()

    Surface(color = colors.surfaceContainer) {
        Column(Modifier.fillMaxWidth()) {
            HorizontalDivider(color = colors.outlineVariant)
            if (selection == null) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SlashCommands.forEach { command ->
                        Box(Modifier.clip(RoundedCornerShape(10.dp)).clickable { state.setType(command.type) }) {
                            Glyph(command.glyph, active = block.type == command.type)
                        }
                    }
                }
            } else {
                val (selected, range) = selection
                val start = range.first
                val end = range.last + 1
                fun has(kind: MarkKind) = Marks.covers(selected.marks, start, end, kind)
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FormatKey("B", has(MarkKind.BOLD), SpanStyle(fontWeight = FontWeight.Bold)) { state.format(MarkKind.BOLD) }
                    FormatKey("I", has(MarkKind.ITALIC), SpanStyle(fontStyle = FontStyle.Italic)) { state.format(MarkKind.ITALIC) }
                    FormatKey("U", has(MarkKind.UNDERLINE), SpanStyle(textDecoration = TextDecoration.Underline)) { state.format(MarkKind.UNDERLINE) }
                    FormatKey("S", has(MarkKind.STRIKE), SpanStyle(textDecoration = TextDecoration.LineThrough)) { state.format(MarkKind.STRIKE) }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { state.clearFormat() }) {
                        Icon(Icons.Outlined.FormatClear, "Limpar formatação", tint = colors.onSurface)
                    }
                }
                val currentColor = selected.marks.lastOrNull { it.kind == MarkKind.COLOR && it.start <= start && it.end >= end }?.color ?: 0
                val currentBg = selected.marks.lastOrNull { it.kind == MarkKind.HIGHLIGHT && it.start <= start && it.end >= end }?.color ?: 0
                ColorRow("Cor", currentColor, background = false) { state.format(MarkKind.COLOR, it) }
                ColorRow("Fundo", currentBg, background = true) { state.format(MarkKind.HIGHLIGHT, it) }
                Spacer(Modifier.size(4.dp))
            }
        }
    }
}

@Composable
private fun FormatKey(label: String, active: Boolean, span: SpanStyle, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) colors.primaryContainer else colors.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            AnnotatedString(label, spanStyle = span),
            fontSize = 18.sp,
            color = if (active) colors.onPrimaryContainer else colors.onSurface,
        )
    }
}

@Composable
private fun ColorRow(label: String, current: Int, background: Boolean, onPick: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.width(44.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PaletteNames.indices.forEach { i ->
                val selected = i == current
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (background) (if (i == 0) colors.surfaceVariant else highlightColor(i)) else colors.surfaceVariant)
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) colors.primary else colors.outlineVariant,
                            CircleShape,
                        )
                        .clickable { onPick(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    val textColor = if (!background && i != 0) paletteColor(i) else colors.onSurface
                    Text("A", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                }
            }
        }
    }
}
