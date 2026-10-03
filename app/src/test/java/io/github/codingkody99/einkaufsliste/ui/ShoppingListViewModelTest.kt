package io.github.codingkody99.einkaufsliste.ui

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.FakeCategoryOverrideDao
import io.github.codingkody99.einkaufsliste.data.FakeShoppingDao
import io.github.codingkody99.einkaufsliste.data.NewItem
import io.github.codingkody99.einkaufsliste.data.FakeRecipeDao
import io.github.codingkody99.einkaufsliste.data.FakeShoppingListDao
import io.github.codingkody99.einkaufsliste.data.FakeSyncSettingsDao
import io.github.codingkody99.einkaufsliste.data.RoomShoppingRepository
import io.github.codingkody99.einkaufsliste.data.ShoppingItem
import io.github.codingkody99.einkaufsliste.data.ShoppingList
import io.github.codingkody99.einkaufsliste.domain.ShoppingListRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = FakeShoppingDao()
    private val overrideDao = FakeCategoryOverrideDao()
    private val listDao = FakeShoppingListDao()
    private val recipeDao = FakeRecipeDao()
    private val syncDao = FakeSyncSettingsDao()
    private var clock = 0L
    private val repository = RoomShoppingRepository(dao, overrideDao, listDao, recipeDao, syncDao) { ++clock }

    private val listId = ShoppingList.DEFAULT_ID

    private lateinit var viewModel: ShoppingListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ShoppingListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- list basics -----------------------------------------------------------

    @Test
    fun `initial state is loading and shows no empty state yet`() {
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertFalse(state.showEmptyState)
    }

    @Test
    fun `an empty list resolves to the empty state`() = runTest(dispatcher) {
        val state = collectUiState()
        advanceUntilIdle()

        assertFalse(state().isLoading)
        assertTrue(state().showEmptyState)
    }

    @Test
    fun `toggling moves the item into the done section and back`() = runTest(dispatcher) {
        val state = collectUiState()
        repository.add(listId, NewItem("Brot", category = Category.BACKWAREN))
        advanceUntilIdle()

        viewModel.toggleChecked(state().entries().single())
        advanceUntilIdle()

        assertEquals(0, state().openCount)
        assertEquals(1, state().checkedCount)
        assertTrue(state().rows.any { it is ShoppingListRow.DoneHeader })

        viewModel.toggleChecked(state().entries().single())
        advanceUntilIdle()

        assertEquals(1, state().openCount)
        assertFalse(state().rows.any { it is ShoppingListRow.DoneHeader })
    }

    // --- automatic categorisation in the editor -------------------------------

    @Test
    fun `typing a name files it automatically`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openAddEditor()
        viewModel.onNameChange("Tomaten")

        val editor = viewModel.editor.value
        assertEquals(Category.OBST_GEMUESE, editor.category)
        assertEquals(Category.OBST_GEMUESE, editor.suggested)
        assertTrue(editor.recognized)
        assertFalse(editor.categoryTouched)
    }

    @Test
    fun `the suggestion follows the name while it is being typed`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openAddEditor()
        viewModel.onNameChange("Tomaten")
        viewModel.onNameChange("Vollkornbrot")

        assertEquals(Category.BACKWAREN, viewModel.editor.value.category)
    }

    @Test
    fun `an unknown name is flagged instead of guessed`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openAddEditor()
        viewModel.onNameChange("Flurbereinigungsgerät")

        val editor = viewModel.editor.value
        assertEquals(Category.DEFAULT, editor.category)
        assertFalse(editor.recognized)
    }

    @Test
    fun `a hand-picked category survives further typing`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openAddEditor()
        viewModel.onNameChange("Tomaten")
        viewModel.onCategoryChange(Category.VORRAT)
        viewModel.onNameChange("Tomatenmark")

        val editor = viewModel.editor.value
        assertEquals(Category.VORRAT, editor.category)
        assertTrue(editor.categoryTouched)
    }

    @Test
    fun `saving a hand-picked category teaches it for next time`() = runTest(dispatcher) {
        val state = collectUiState()
        advanceUntilIdle()

        viewModel.openAddEditor()
        viewModel.onNameChange("Yuzu")
        viewModel.onCategoryChange(Category.OBST_GEMUESE)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(Category.OBST_GEMUESE, state().entries().single().category)

        // The next time the name is typed it is already known.
        viewModel.openAddEditor()
        viewModel.onNameChange("Yuzu")
        assertEquals(Category.OBST_GEMUESE, viewModel.editor.value.category)
        assertTrue(viewModel.editor.value.recognized)
    }

    @Test
    fun `accepting the suggestion teaches nothing`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openAddEditor()
        viewModel.onNameChange("Tomaten")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(overrideDao.rows.isEmpty())
    }

    @Test
    fun `save does nothing while the name is blank`() = runTest(dispatcher) {
        val state = collectUiState()
        viewModel.openAddEditor()
        viewModel.onNameChange("   ")
        assertFalse(viewModel.editor.value.canSave)
        viewModel.save()
        advanceUntilIdle()

        assertTrue(state().rows.isEmpty())
        assertTrue(viewModel.editor.value.visible)
    }

    @Test
    fun `opening the edit editor prefills the item and its suggestion`() = runTest(dispatcher) {
        advanceUntilIdle()
        val item = ShoppingItem(id = 7, name = "Milch", quantity = "2 l", category = Category.MOLKEREI)

        viewModel.openEditEditor(item)

        val editor = viewModel.editor.value
        assertTrue(editor.visible)
        assertTrue(editor.isEditing)
        assertEquals("Milch", editor.name)
        assertEquals("2 l", editor.quantity)
        assertEquals(Category.MOLKEREI, editor.category)
        assertEquals(Category.MOLKEREI, editor.suggested)
        assertFalse("Die Kategorie stimmt mit der Erkennung überein", editor.categoryTouched)
    }

    @Test
    fun `editing an existing item updates it in place`() = runTest(dispatcher) {
        val state = collectUiState()
        repository.add(listId, NewItem("Brot", category = Category.BACKWAREN))
        advanceUntilIdle()
        val stored = state().entries().single()

        viewModel.openEditEditor(stored)
        viewModel.onNameChange("Vollkornbrot")
        viewModel.save()
        advanceUntilIdle()

        val updated = state().entries().single()
        assertEquals("Vollkornbrot", updated.name)
        assertEquals(stored.id, updated.id)
    }

    @Test
    fun `dismissing the editor clears what was typed`() {
        viewModel.openAddEditor()
        viewModel.onNameChange("Tippfehler")
        viewModel.dismissEditor()

        val editor = viewModel.editor.value
        assertFalse(editor.visible)
        assertEquals("", editor.name)
    }

    // --- bulk import ----------------------------------------------------------

    @Test
    fun `the import sheet starts empty and unanalysed`() {
        viewModel.openImport()
        val state = viewModel.import.value
        assertTrue(state.visible)
        assertEquals("", state.text)
        assertNull(state.rows)
        assertFalse(state.canAnalyze)
        assertFalse(state.canApply)
    }

    @Test
    fun `analysing breaks a pasted list into categorised rows`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("Tomaten\nMilch\n500g Hackfleisch")
        viewModel.analyzeImport()
        advanceUntilIdle()

        val rows = viewModel.import.value.rows!!
        assertEquals(listOf("Tomaten", "Milch", "Hackfleisch"), rows.map { it.name })
        assertEquals(
            listOf(Category.OBST_GEMUESE, Category.MOLKEREI, Category.FLEISCH_FISCH),
            rows.map { it.category },
        )
        assertEquals("500 g", rows.last().quantity)
        assertTrue(rows.all { it.selected })
    }

    @Test
    fun `editing the text invalidates a previous analysis`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("Tomaten")
        viewModel.analyzeImport()
        advanceUntilIdle()
        assertTrue(viewModel.import.value.analyzed)

        viewModel.onImportTextChange("Tomaten\nMilch")

        assertFalse(viewModel.import.value.analyzed)
        assertNull(viewModel.import.value.rows)
    }

    @Test
    fun `the preview is grouped in supermarket order`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("Spülmittel\nHackfleisch\nTomaten\nMilch")
        viewModel.analyzeImport()
        advanceUntilIdle()

        assertEquals(
            listOf(
                Category.OBST_GEMUESE,
                Category.MOLKEREI,
                Category.FLEISCH_FISCH,
                Category.HAUSHALT,
            ),
            viewModel.import.value.previewByCategory.map { it.first },
        )
    }

    @Test
    fun `captions are held back from the import but still shown`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("Abendessen Freitag:\nLachs\nKartoffeln")
        viewModel.analyzeImport()
        advanceUntilIdle()

        val state = viewModel.import.value
        val caption = state.rows!!.single { it.isHeading }
        assertEquals("Abendessen Freitag", caption.name)
        assertFalse(caption.selected)
        assertEquals(listOf("Lachs", "Kartoffeln"), state.selectedRows.map { it.name })
        assertEquals(listOf("Abendessen Freitag"), state.skippedRows.map { it.name })
    }

    @Test
    fun `items already on the list are marked and not selected`() = runTest(dispatcher) {
        collectUiState()
        repository.add(listId, NewItem("Tomaten", category = Category.OBST_GEMUESE))
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange("tomaten\nMilch")
        viewModel.analyzeImport()
        advanceUntilIdle()

        val rows = viewModel.import.value.rows!!
        val tomaten = rows.single { it.name == "Tomaten" }
        assertTrue(tomaten.duplicate)
        assertFalse(tomaten.selected)
        assertTrue(rows.single { it.name == "Milch" }.selected)
    }

    @Test
    fun `an item that is already ticked off does not count as a duplicate`() = runTest(dispatcher) {
        val state = collectUiState()
        repository.add(listId, NewItem("Tomaten", category = Category.OBST_GEMUESE))
        advanceUntilIdle()
        viewModel.toggleChecked(state().entries().single())
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange("Tomaten")
        viewModel.analyzeImport()
        advanceUntilIdle()

        assertFalse(viewModel.import.value.rows!!.single().duplicate)
    }

    @Test
    fun `a row can be toggled back in and out`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("Tomaten\nMilch")
        viewModel.analyzeImport()
        advanceUntilIdle()

        viewModel.toggleImportRow(0)
        assertEquals(listOf("Milch"), viewModel.import.value.selectedRows.map { it.name })

        viewModel.toggleImportRow(0)
        assertEquals(2, viewModel.import.value.selectedRows.size)
    }

    @Test
    fun `select all and select none`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("Abendessen:\nLachs\nKartoffeln")
        viewModel.analyzeImport()
        advanceUntilIdle()

        viewModel.setAllImportRowsSelected(true)
        assertEquals(3, viewModel.import.value.selectedRows.size)

        viewModel.setAllImportRowsSelected(false)
        assertFalse(viewModel.import.value.canApply)
    }

    @Test
    fun `moving a row to another category also selects it`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("Abendessen:\nLachs")
        viewModel.analyzeImport()
        advanceUntilIdle()

        viewModel.setImportRowCategory(0, Category.VORRAT)

        val caption = viewModel.import.value.rows!!.first()
        assertEquals(Category.VORRAT, caption.category)
        assertTrue(caption.selected)
        assertTrue(caption.recategorized)
    }

    @Test
    fun `applying the import adds the selected rows in supermarket order`() = runTest(dispatcher) {
        val state = collectUiState()
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange("Salat mit Käse\n- 2 Tomaten\n- Feta\nMilch\n500g Hackfleisch")
        viewModel.analyzeImport()
        advanceUntilIdle()
        viewModel.applyImport()
        advanceUntilIdle()

        assertFalse(viewModel.import.value.visible)
        // Grouped by the route through the shop, not the order they were pasted.
        assertEquals(
            listOf(
                Category.OBST_GEMUESE,
                Category.MOLKEREI,
                Category.FLEISCH_FISCH,
            ),
            state().rows.filterIsInstance<ShoppingListRow.CategoryHeader>().map { it.category },
        )
        assertEquals(
            listOf("Salat", "Tomaten", "Käse", "Feta", "Milch", "Hackfleisch"),
            state().entries().map { it.name },
        )
        assertEquals("2", state().entries().first { it.name == "Tomaten" }.quantity)
    }

    @Test
    fun `applying the import can be undone in one go`() = runTest(dispatcher) {
        val state = collectUiState()
        val notices = collectNotices()
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange("Tomaten\nMilch\nBrot")
        viewModel.analyzeImport()
        advanceUntilIdle()
        viewModel.applyImport()
        advanceUntilIdle()

        assertEquals(3, state().openCount)
        assertEquals("3 Artikel übernommen", notices.single().text)
        assertTrue(notices.single().canUndo)

        viewModel.undo(notices.single())
        advanceUntilIdle()

        assertTrue(state().rows.isEmpty())
    }

    @Test
    fun `a single imported item is announced in the singular`() = runTest(dispatcher) {
        collectUiState()
        val notices = collectNotices()
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange("Tomaten")
        viewModel.analyzeImport()
        advanceUntilIdle()
        viewModel.applyImport()
        advanceUntilIdle()

        assertEquals("1 Artikel übernommen", notices.single().text)
    }

    @Test
    fun `a category corrected in the preview is learned`() = runTest(dispatcher) {
        collectUiState()
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange("Yuzu")
        viewModel.analyzeImport()
        advanceUntilIdle()
        viewModel.setImportRowCategory(0, Category.OBST_GEMUESE)
        viewModel.applyImport()
        advanceUntilIdle()

        viewModel.openAddEditor()
        viewModel.onNameChange("Yuzu")
        assertEquals(Category.OBST_GEMUESE, viewModel.editor.value.category)
    }

    @Test
    fun `a learned category is applied to the next import`() = runTest(dispatcher) {
        collectUiState()
        repository.rememberCategory("Yuzu", Category.OBST_GEMUESE)
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange("Yuzu")
        viewModel.analyzeImport()
        advanceUntilIdle()

        val row = viewModel.import.value.rows!!.single()
        assertEquals(Category.OBST_GEMUESE, row.category)
        assertFalse(row.uncertain)
    }

    @Test
    fun `applying nothing does not emit a notice or close the sheet prematurely`() = runTest(dispatcher) {
        val notices = collectNotices()
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange("Abendessen:")
        viewModel.analyzeImport()
        advanceUntilIdle()
        viewModel.applyImport()
        advanceUntilIdle()

        assertTrue(notices.isEmpty())
        assertTrue(viewModel.import.value.visible)
    }

    @Test
    fun `analysing blank text does nothing`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("   ")
        viewModel.analyzeImport()
        advanceUntilIdle()

        assertNull(viewModel.import.value.rows)
    }

    @Test
    fun `dismissing the import drops the text`() = runTest(dispatcher) {
        viewModel.openImport()
        viewModel.onImportTextChange("Tomaten")
        viewModel.dismissImport()

        assertFalse(viewModel.import.value.visible)
        assertEquals("", viewModel.import.value.text)
    }

    @Test
    fun `an unrecognised import row is marked uncertain`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("Flurbereinigungsgerät")
        viewModel.analyzeImport()
        advanceUntilIdle()

        val row = viewModel.import.value.rows!!.single()
        assertTrue(row.uncertain)
        assertTrue("unsichere Zeilen bleiben ausgewählt", row.selected)
        assertEquals(Category.DEFAULT, row.category)
    }

    // --- deletion notices -----------------------------------------------------

    @Test
    fun `deleting emits a notice that restores the item`() = runTest(dispatcher) {
        val state = collectUiState()
        val notices = collectNotices()
        repository.add(listId, NewItem("Brot", "1 Stück", Category.BACKWAREN))
        advanceUntilIdle()
        val stored = state().entries().single()

        viewModel.delete(stored)
        advanceUntilIdle()

        assertTrue(state().rows.isEmpty())
        assertEquals(1, notices.size)
        assertEquals(UndoAction.Restore(listOf(stored)), notices.single().undo)

        viewModel.undo(notices.single())
        advanceUntilIdle()

        assertEquals("Brot", state().entries().single().name)
    }

    @Test
    fun `deleting checked items leaves the open ones alone`() = runTest(dispatcher) {
        val state = collectUiState()
        val notices = collectNotices()
        repository.add(listId, NewItem("Brot", category = Category.BACKWAREN))
        repository.add(listId, NewItem("Milch", category = Category.MOLKEREI))
        advanceUntilIdle()
        viewModel.toggleChecked(state().entries().first { it.name == "Brot" })
        advanceUntilIdle()

        viewModel.deleteChecked()
        advanceUntilIdle()

        assertEquals(listOf("Milch"), state().entries().map { it.name })
        assertEquals("1 erledigte entfernt", notices.single().text)
    }

    @Test
    fun `clearing an already empty list emits no notice`() = runTest(dispatcher) {
        collectUiState()
        val notices = collectNotices()

        viewModel.deleteChecked()
        viewModel.clearList()
        advanceUntilIdle()

        assertTrue(notices.isEmpty())
    }

    @Test
    fun `clearList removes everything and can be undone`() = runTest(dispatcher) {
        val state = collectUiState()
        val notices = collectNotices()
        repository.add(listId, NewItem("Brot", category = Category.BACKWAREN))
        repository.add(listId, NewItem("Milch", category = Category.MOLKEREI))
        advanceUntilIdle()

        viewModel.clearList()
        advanceUntilIdle()
        assertTrue(state().rows.isEmpty())

        viewModel.undo(notices.single())
        advanceUntilIdle()

        assertEquals(2, state().openCount)
    }

    @Test
    fun `each notice gets its own id`() = runTest(dispatcher) {
        val state = collectUiState()
        val notices = collectNotices()
        repository.add(listId, NewItem("Brot"))
        repository.add(listId, NewItem("Milch"))
        advanceUntilIdle()

        state().entries().forEach { viewModel.delete(it) }
        advanceUntilIdle()

        assertEquals(2, notices.size)
        assertEquals(2, notices.map { it.id }.distinct().size)
    }

    // --- helpers --------------------------------------------------------------

    private fun ShoppingListUiState.entries(): List<ShoppingItem> =
        rows.filterIsInstance<ShoppingListRow.Entry>().map { it.item }

    /**
     * `uiState` is a `WhileSubscribed` flow, so it only produces values while
     * something collects it. An unconfined dispatcher makes the collector
     * subscribe eagerly — `advanceUntilIdle()` alone does not run work launched
     * in `backgroundScope`.
     */
    private fun TestScope.collectUiState(): () -> ShoppingListUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        return { viewModel.uiState.value }
    }

    /** Records snackbar notices as they are emitted; see [collectUiState]. */
    private fun TestScope.collectNotices(): List<Notice> {
        val notices = mutableListOf<Notice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.notices.collect { notices += it }
        }
        return notices
    }
}
