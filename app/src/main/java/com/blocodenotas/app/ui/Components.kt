package com.blocodenotas.app.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blocodenotas.app.Graph
import com.blocodenotas.app.data.Note
import com.blocodenotas.app.data.NoteIcon
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val NoteIcon.vector: ImageVector
    get() = when (this) {
        NoteIcon.NOTE -> Icons.Outlined.Description
        NoteIcon.LIGHTBULB -> Icons.Outlined.Lightbulb
        NoteIcon.LOCK -> Icons.Outlined.Lock
        NoteIcon.CHECK -> Icons.Outlined.CheckBox
        NoteIcon.CART -> Icons.Outlined.ShoppingCart
        NoteIcon.WORK -> Icons.Outlined.Work
        NoteIcon.HOME -> Icons.Outlined.Home
        NoteIcon.HEART -> Icons.Outlined.FavoriteBorder
        NoteIcon.SCHOOL -> Icons.Outlined.School
        NoteIcon.TRAVEL -> Icons.Outlined.Flight
        NoteIcon.FOOD -> Icons.Outlined.Restaurant
        NoteIcon.MONEY -> Icons.Outlined.Payments
        NoteIcon.BOOK -> Icons.Outlined.MenuBook
        NoteIcon.PET -> Icons.Outlined.Pets
        NoteIcon.FITNESS -> Icons.Outlined.FitnessCenter
        NoteIcon.STAR -> Icons.Outlined.StarBorder
    }

private val ptBR: Locale = Locale.forLanguageTag("pt-BR")

fun formatDateTime(time: Long): String =
    SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", ptBR).format(Date(time))

/** "hoje", "ontem" ou "em 12/10/2026". */
fun relativeDay(time: Long): String {
    val then = Calendar.getInstance().apply { timeInMillis = time }
    val now = Calendar.getInstance()
    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    if (sameDay(then, now)) return "hoje"
    now.add(Calendar.DAY_OF_YEAR, -1)
    if (sameDay(then, now)) return "ontem"
    return "em " + SimpleDateFormat("dd/MM/yyyy", ptBR).format(Date(time))
}

val CardShape = RoundedCornerShape(20.dp)

@Composable
fun ScreenHeader(
    overline: String?,
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(overline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

@Composable
fun SectionHeader(title: String, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Campo de texto sem bordas, com texto de exemplo quando vazio. */
@Composable
fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val color = MaterialTheme.colorScheme.onBackground
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        textStyle = textStyle.copy(color = if (textStyle.color.isUnspecifiedColor()) color else textStyle.color),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)))
                }
                inner()
            }
        },
    )
}

private fun androidx.compose.ui.graphics.Color.isUnspecifiedColor() =
    this == androidx.compose.ui.graphics.Color.Unspecified

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    label: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()); onDismiss() }) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Menu de ações de uma nota, aberto ao segurar o dedo sobre ela. */
@Composable
fun NoteActionsMenu(note: Note, expanded: Boolean, onDismiss: () -> Unit, onRequestDelete: () -> Unit) {
    val repo = Graph.repo
    val context = LocalContext.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (note.pinned) "Desafixar" else "Fixar") },
            leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
            onClick = { repo.togglePinned(note.id); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text(if (note.favorite) "Remover dos favoritos" else "Favoritar") },
            leadingIcon = { Icon(if (note.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder, null) },
            onClick = { repo.toggleFavorite(note.id); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text("Duplicar") },
            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
            onClick = {
                repo.duplicate(note.id)
                Toast.makeText(context, "Nota duplicada", Toast.LENGTH_SHORT).show()
                onDismiss()
            },
        )
        DropdownMenuItem(
            text = { Text("Mover para a lixeira", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onRequestDelete() },
        )
    }
}

@Composable
fun TrashConfirm(note: Note, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = "Mover para a lixeira?",
        message = "\"${note.displayTitle()}\" poderá ser recuperada na Lixeira por até 30 dias.",
        confirmLabel = "Mover",
        onConfirm = { Graph.repo.moveToTrash(note.id) },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteRow(note: Note, onOpen: () -> Unit, showFolder: Boolean = true) {
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val folder = if (showFolder) Graph.repo.folder(note.folderId) else null
    Box {
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true }),
        ) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(note.icon.vector, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            note.displayTitle(),
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (note.favorite) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    val subtitle = note.preview().ifBlank { "Atualizada " + relativeDay(note.updatedAt) }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (folder != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            folder.name,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        NoteActionsMenu(note, menu, onDismiss = { menu = false }, onRequestDelete = { confirm = true })
    }
    if (confirm) TrashConfirm(note, onDismiss = { confirm = false })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedCard(note: Note, onOpen: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true }),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        note.displayTitle(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { Graph.repo.toggleFavorite(note.id) }) {
                        Icon(
                            if (note.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (note.favorite) "Remover dos favoritos" else "Favoritar",
                            tint = if (note.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val preview = note.preview()
                    if (preview.isNotBlank()) {
                        Text(
                            preview,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "Atualizada " + relativeDay(note.updatedAt),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        NoteActionsMenu(note, menu, onDismiss = { menu = false }, onRequestDelete = { confirm = true })
    }
    if (confirm) TrashConfirm(note, onDismiss = { confirm = false })
}
