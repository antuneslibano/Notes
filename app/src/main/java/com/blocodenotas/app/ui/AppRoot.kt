package com.blocodenotas.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blocodenotas.app.Graph

sealed interface Screen {
    data object Home : Screen
    data object Folders : Screen
    data class FolderDetail(val folderId: Long) : Screen
    data object Favorites : Screen
    data object Trash : Screen
    data class Editor(val noteId: Long) : Screen
    data object Settings : Screen
}

class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun selectTab(tab: Screen) {
        stack.clear()
        stack.add(Screen.Home)
        if (tab != Screen.Home) stack.add(tab)
    }
}

@Composable
fun AppRoot(nav: Navigator) {
    BackHandler(enabled = nav.stack.size > 1) { nav.pop() }

    when (val screen = nav.current) {
        is Screen.Editor -> EditorScreen(
            noteId = screen.noteId,
            onBack = { nav.pop() },
            onOpenNote = { nav.replace(Screen.Editor(it)) },
        )
        Screen.Settings -> SettingsScreen(onBack = { nav.pop() })
        else -> MainScaffold(nav, screen)
    }
}

@Composable
private fun MainScaffold(nav: Navigator, screen: Screen) {
    val repo = Graph.repo
    val open: (Long) -> Unit = { nav.push(Screen.Editor(it)) }
    val tab = when (screen) {
        is Screen.FolderDetail -> Screen.Folders
        else -> screen
    }
    val newNoteFolder = (screen as? Screen.FolderDetail)?.folderId

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomBar(
                selected = tab,
                onSelect = { nav.selectTab(it) },
                onCreate = { checklist ->
                    val note = repo.createNote(folderId = newNoteFolder, checklist = checklist)
                    if (screen == Screen.Favorites) repo.updateNote(note.id, touch = false) { it.copy(favorite = true) }
                    open(note.id)
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                Screen.Home -> HomeScreen(onOpen = open, onSettings = { nav.push(Screen.Settings) })
                Screen.Folders -> FoldersScreen(onOpenFolder = { nav.push(Screen.FolderDetail(it)) })
                is Screen.FolderDetail -> FolderDetailScreen(screen.folderId, onOpen = open, onBack = { nav.pop() })
                Screen.Favorites -> FavoritesScreen(onOpen = open)
                Screen.Trash -> TrashScreen()
                else -> LaunchedEffect(Unit) { nav.selectTab(Screen.Home) }
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Screen, onSelect: (Screen) -> Unit, onCreate: (checklist: Boolean) -> Unit) {
    var createMenu by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(84.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem("Início", Icons.Outlined.Home, Icons.Filled.Home, selected == Screen.Home) { onSelect(Screen.Home) }
            NavItem("Pastas", Icons.Outlined.Folder, Icons.Filled.Folder, selected == Screen.Folders) { onSelect(Screen.Folders) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FloatingActionButton(
                    onClick = { createMenu = true },
                    shape = CircleShape,
                    containerColor = Accent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                    modifier = Modifier.size(62.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nova nota", modifier = Modifier.size(30.dp))
                }
                DropdownMenu(expanded = createMenu, onDismissRequest = { createMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Nova nota") },
                        leadingIcon = { Icon(Icons.Outlined.Description, null) },
                        onClick = { createMenu = false; onCreate(false) },
                    )
                    DropdownMenuItem(
                        text = { Text("Nova lista") },
                        leadingIcon = { Icon(Icons.Outlined.CheckBox, null) },
                        onClick = { createMenu = false; onCreate(true) },
                    )
                }
            }
            NavItem("Favoritos", Icons.Outlined.StarBorder, Icons.Filled.Star, selected == Screen.Favorites) { onSelect(Screen.Favorites) }
            NavItem("Lixeira", Icons.Outlined.Delete, Icons.Filled.Delete, selected == Screen.Trash) { onSelect(Screen.Trash) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(if (selected) selectedIcon else icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = color, fontSize = 13.sp, maxLines = 1)
    }
}
