package com.blocodenotas.app

import android.view.KeyEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blocodenotas.app.data.BlockType
import com.blocodenotas.app.data.MarkKind
import com.blocodenotas.app.data.Note
import com.blocodenotas.app.data.NoteFilter
import com.blocodenotas.app.data.NoteIcon
import com.blocodenotas.app.data.SortField
import com.blocodenotas.app.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Percorre as funções do app na interface real, num emulador.
 * Cada teste cria notas com títulos únicos para não depender dos outros.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {

    /** Antes de abrir o app: sem notas, sem PIN e com as preferências padrão. */
    private val reset = object : ExternalResource() {
        override fun before() = resetApp()
        override fun after() = resetApp()

        private fun resetApp() {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                Graph.repo.notes.forEach { Graph.repo.deletePermanently(it.id) }
                Graph.repo.folders.forEach { Graph.repo.deleteFolder(it.id) }
                Graph.settings.clearPin()
                Graph.settings.updateTheme(ThemeMode.SYSTEM)
                Graph.settings.updateFilter(NoteFilter.ALL)
                Graph.settings.updateSort(SortField.UPDATED, ascending = false)
            }
        }
    }

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(reset).around(compose)

    private fun unique(prefix: String) = "$prefix ${System.nanoTime() % 1_000_000}"

    private fun noteTitled(title: String): Note? = Graph.repo.notes.firstOrNull { it.title == title && !it.isDeleted }

    private fun block(index: Int): SemanticsNodeInteraction = compose.onAllNodesWithTag("block")[index]

    private fun checkboxCount() = compose.onAllNodes(isToggleable()).fetchSemanticsNodes().size

    /** Cria uma nota pelo botão + e escreve o título. */
    private fun createNote(title: String) {
        compose.onNodeWithContentDescription("Nova nota").performClick()
        compose.onNodeWithTag("title").performTextInput(title)
        compose.waitForIdle()
    }

    private fun goBack() {
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.waitForIdle()
    }

    // ------------------------------------------------------------ editor

    @Test
    fun slashTodoWithEnterCreatesOneCheckbox() {
        val title = unique("Todo")
        createNote(title)
        block(0).performTextInput("/todo")
        compose.onNodeWithText("Lista de tarefas").assertIsDisplayed()
        block(0).performTextInput("\n")
        compose.waitForIdle()
        assertEquals(1, checkboxCount())

        block(0).performTextInput("Leite")
        block(0).performTextInput("\n")
        compose.waitForIdle()
        block(1).performTextInput("Café")
        compose.waitForIdle()
        assertEquals(2, checkboxCount())
        val note = noteTitled(title)!!
        assertEquals(listOf("Leite", "Café"), note.blocks.map { it.text })
        assertTrue(note.blocks.all { it.type == BlockType.TODO })
    }

    @Test
    fun slashTodoTypedWithRealKeyEvents() {
        val title = unique("Teclas")
        createNote(title)
        block(0).performClick()
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendStringSync("/todo")
        compose.waitForIdle()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
        compose.waitForIdle()
        Thread.sleep(500)
        compose.waitForIdle()
        assertEquals(1, checkboxCount())
        instrumentation.sendStringSync("leite")
        compose.waitForIdle()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
        compose.waitForIdle()
        instrumentation.sendStringSync("cafe")
        compose.waitForIdle()
        Thread.sleep(500)
        compose.waitForIdle()
        assertEquals(2, checkboxCount())
        val note = noteTitled(title)!!
        assertEquals(listOf("leite", "cafe"), note.blocks.map { it.text })
    }

    @Test
    fun slashMenuClickAndMarkdownShortcuts() {
        val title = unique("Blocos")
        createNote(title)
        block(0).performTextInput("/h1")
        compose.onNodeWithText("Título 1").performClick()
        block(0).performTextInput("Grande")
        block(0).performTextInput("\n")
        block(1).performTextInput("- ")
        block(1).performTextInput("item")
        block(1).performTextInput("\n")
        block(2).performTextInput("\n")
        block(2).performTextInput("> ")
        block(2).performTextInput("citado")
        compose.waitForIdle()
        val note = noteTitled(title)!!
        assertEquals(listOf(BlockType.H1, BlockType.BULLET, BlockType.QUOTE), note.blocks.map { it.type })
        assertEquals(listOf("Grande", "item", "citado"), note.blocks.map { it.text })
    }

    @Test
    fun dividerAndBackspaceMerge() {
        val title = unique("Divisor")
        createNote(title)
        block(0).performTextInput("---")
        compose.waitForIdle()
        assertEquals(BlockType.DIVIDER, noteTitled(title)!!.blocks[0].type)
        compose.onNodeWithContentDescription("Remover divisor").performClick()
        compose.waitForIdle()
        assertTrue(noteTitled(title)!!.blocks.none { it.type == BlockType.DIVIDER })
    }

    @Test
    fun formattingToolbarAppliesStylesAndColors() {
        val title = unique("Cores")
        createNote(title)
        block(0).performTextInput("palavra colorida")
        // O campo tem um caractere invisível na posição 0.
        block(0).performTextInputSelection(TextRange(1, 8))
        compose.onNodeWithContentDescription("Negrito").performClick()
        compose.onNodeWithContentDescription("Cor Vermelho").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Fundo Amarelo").performScrollTo().performClick()
        compose.waitForIdle()
        val block = noteTitled(title)!!.blocks[0]
        val marks = block.marks
        val info = "texto='${block.text}' marcas=$marks"
        assertTrue("negrito: $info", marks.any { it.kind == MarkKind.BOLD && it.start == 0 && it.end == 7 })
        assertTrue("cor: $info", marks.any { it.kind == MarkKind.COLOR && it.color == 9 })
        assertTrue("fundo: $info", marks.any { it.kind == MarkKind.HIGHLIGHT && it.color == 4 })
        compose.onNodeWithContentDescription("Limpar formatação").performClick()
        compose.waitForIdle()
        val after = noteTitled(title)!!.blocks[0].marks
        assertTrue("limpar: $after", after.isEmpty())
    }

    @Test
    fun toolbarChangesBlockType() {
        val title = unique("Barra")
        createNote(title)
        block(0).performTextInput("tarefa")
        compose.onNodeWithContentDescription("Lista de tarefas").performClick()
        compose.waitForIdle()
        assertEquals(1, checkboxCount())
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.waitForIdle()
        assertTrue(noteTitled(title)!!.blocks[0].checked)
    }

    @Test
    fun undoAndRedo() {
        val title = unique("Desfazer")
        createNote(title)
        block(0).performTextInput("primeiro")
        Thread.sleep(1200)
        block(0).performTextInput(" segundo")
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Desfazer").performClick()
        compose.waitForIdle()
        assertEquals("primeiro", noteTitled(title)!!.blocks[0].text)
        compose.onNodeWithContentDescription("Refazer").performClick()
        compose.waitForIdle()
        assertEquals("primeiro segundo", noteTitled(title)!!.blocks[0].text)
    }

    @Test
    fun editorMenuActions() {
        val title = unique("Menu")
        createNote(title)
        block(0).performTextInput("conteúdo")
        compose.onNodeWithContentDescription("Fixar").performClick()
        compose.onNodeWithContentDescription("Favoritar").performClick()
        compose.waitForIdle()
        noteTitled(title)!!.let {
            assertTrue(it.pinned)
            assertTrue(it.favorite)
        }

        compose.onNodeWithContentDescription("Mais opções").performClick()
        compose.onNodeWithText("Mudar ícone").performClick()
        compose.onNodeWithText("Compras").performClick()
        compose.waitForIdle()
        assertEquals(NoteIcon.CART, noteTitled(title)!!.icon)

        compose.onNodeWithContentDescription("Mais opções").performClick()
        compose.onNodeWithText("Informações").performClick()
        compose.onNodeWithText("Criada em").assertIsDisplayed()
        compose.onNodeWithText("Fechar").performClick()

        compose.onNodeWithContentDescription("Mais opções").performClick()
        compose.onNodeWithText("Copiar todo o conteúdo").performClick()

        compose.onNodeWithContentDescription("Mais opções").performClick()
        compose.onNodeWithText("Duplicar nota").performClick()
        compose.waitForIdle()
        assertNotNull(noteTitled("$title (cópia)"))
    }

    @Test
    fun emptyNoteIsDiscardedOnBack() {
        val before = Graph.repo.notes.size
        compose.onNodeWithContentDescription("Nova nota").performClick()
        compose.waitForIdle()
        goBack()
        assertEquals(before, Graph.repo.notes.size)
    }

    // ------------------------------------------------------------ telas

    @Test
    fun noteAppearsOnHomeAndSearchFindsIt() {
        val title = unique("Busca")
        createNote(title)
        block(0).performTextInput("conteúdo secreto")
        goBack()
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithTag("search").performTextInput("SECRETO")
        compose.waitForIdle()
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithContentDescription("Limpar").performClick()
    }

    @Test
    fun trashMoveRestoreAndDeleteForever() {
        val title = unique("Lixo")
        createNote(title)
        block(0).performTextInput("apagar")
        goBack()
        compose.onNodeWithText(title).performTouchInput { longClick() }
        compose.onNodeWithText("Mover para a lixeira").performClick()
        compose.onNodeWithText("Mover").performClick()
        compose.waitForIdle()
        assertTrue(Graph.repo.notes.first { it.title == title }.isDeleted)

        compose.onNodeWithText("Lixeira").performClick()
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithContentDescription("Restaurar").performClick()
        compose.waitForIdle()
        assertFalse(Graph.repo.notes.first { it.title == title }.isDeleted)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Graph.repo.moveToTrash(Graph.repo.notes.first { it.title == title }.id)
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Excluir definitivamente").performClick()
        compose.onNodeWithText("Excluir").performClick()
        compose.waitForIdle()
        assertTrue(Graph.repo.notes.none { it.title == title })
    }

    @Test
    fun foldersCreateOpenAndAddNote() {
        val folder = unique("Pasta")
        compose.onNodeWithText("Pastas").performClick()
        compose.onNodeWithContentDescription("Nova pasta").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(folder)
        compose.onNodeWithText("Criar").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(folder).performClick()
        compose.onNodeWithText("Pasta vazia").assertIsDisplayed()

        val title = unique("NaPasta")
        createNote(title)
        goBack()
        compose.onNodeWithText(title).assertIsDisplayed()
        val folderId = Graph.repo.folders.first { it.name == folder }.id
        assertEquals(folderId, noteTitled(title)!!.folderId)
    }

    @Test
    fun favoritesTabListsFavorites() {
        val title = unique("Favorita")
        createNote(title)
        compose.onNodeWithContentDescription("Favoritar").performClick()
        goBack()
        compose.onNodeWithText("Favoritos").performClick()
        compose.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun sortAndFilterMenus() {
        compose.onNodeWithContentDescription("Ordenar").performClick()
        compose.onNodeWithText("Nome").performClick()
        compose.waitForIdle()
        assertEquals(SortField.TITLE, Graph.settings.sortField)
        assertTrue(Graph.settings.sortAscending)
        compose.onNodeWithContentDescription("Filtrar").performClick()
        compose.onNodeWithText("Somente favoritas").performClick()
        compose.waitForIdle()
        assertEquals(NoteFilter.FAVORITES, Graph.settings.filter)
    }

    @Test
    fun themeCanBeChanged() {
        compose.onNodeWithContentDescription("Configurações").performClick()
        compose.onNodeWithText("Escuro").performClick()
        compose.waitForIdle()
        assertEquals(ThemeMode.DARK, Graph.settings.theme)
        compose.onNodeWithText("Claro").performClick()
        compose.waitForIdle()
        assertEquals(ThemeMode.LIGHT, Graph.settings.theme)
    }

    @Test
    fun pinLockFlow() {
        compose.onNodeWithContentDescription("Configurações").performClick()
        compose.onNodeWithText("Bloquear com PIN").performClick()
        listOf("1", "2", "3", "4").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText("Continuar").performClick()
        listOf("1", "2", "3", "4").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText("Salvar").performClick()
        compose.waitForIdle()
        assertTrue(Graph.settings.pinEnabled)

        // Sair e voltar ao app deve pedir o PIN.
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.onNodeWithText("Digite seu PIN para continuar").assertIsDisplayed()
        listOf("9", "9", "9", "9").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText("PIN incorreto. Tente novamente.").assertIsDisplayed()
        listOf("1", "2", "3", "4").forEach { compose.onNodeWithText(it).performClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Configurações").assertIsDisplayed()
    }

    @Test
    fun backupExportAndRestore() {
        val title = unique("Backup")
        createNote(title)
        block(0).performTextInput("/todo")
        block(0).performTextInput("\n")
        block(0).performTextInput("item salvo")
        goBack()
        lateinit var backup: String
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            backup = Graph.repo.exportBackup()
            Graph.repo.deletePermanently(noteTitled(title)!!.id)
        }
        assertEquals(null, noteTitled(title))
        instrumentation.runOnMainSync { Graph.repo.importBackup(backup, replace = false) }
        val restored = noteTitled(title)!!
        assertEquals(BlockType.TODO, restored.blocks[0].type)
        assertEquals("item salvo", restored.blocks[0].text)
        assertTrue(Graph.repo.exportText().contains("☐ item salvo"))
    }

    @Test
    fun seededNotesOrHomeHeaderVisible() {
        compose.onNodeWithText("Minhas notas").assertIsDisplayed()
        compose.onNode(hasTestTag("search")).assertIsDisplayed()
        compose.onNode(hasText("Todas as notas") or hasText("Resultados", substring = true)).assertExists()
    }
}
