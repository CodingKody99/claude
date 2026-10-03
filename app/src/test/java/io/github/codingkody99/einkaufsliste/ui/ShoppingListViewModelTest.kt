package io.github.codingkody99.einkaufsliste.ui

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.FakeShoppingDao
import io.github.codingkody99.einkaufsliste.data.RoomShoppingRepository
import io.github.codingkody99.einkaufsliste.data.ShoppingItem
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
    private var clock = 0L
    private val repository = RoomShoppingRepository(dao) { ++clock }

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
    fun `saving the editor adds the item and closes the sheet`() = runTest(dispatcher) {
        val state = collectUiState()

        viewModel.openAddEditor()
        viewModel.onNameChange("Tomaten")
        viewModel.onQuantityChange("500 g")
        viewModel.onCategoryChange(Category.OBST_GEMUESE)
        viewModel.save()
        advanceUntilIdle()

        assertFalse(viewModel.editor.value.visible)
        assertNull(viewModel.editor.value.editing)

        val item = state().entries().single()
        assertEquals("Tomaten", item.name)
        assertEquals("500 g", item.quantity)
        assertEquals(Category.OBST_GEMUESE, item.category)
        assertEquals(1, state().openCount)
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
        // The sheet stays open so what was typed is not silently lost.
        assertTrue(viewModel.editor.value.visible)
    }

    @Test
    fun `opening the edit editor prefills the item`() {
        val item = ShoppingItem(
            id = 7,
            name = "Milch",
            quantity = "2 l",
            category = Category.MOLKEREI,
        )

        viewModel.openEditEditor(item)

        val editor = viewModel.editor.value
        assertTrue(editor.visible)
        assertTrue(editor.isEditing)
        assertEquals("Milch", editor.name)
        assertEquals("2 l", editor.quantity)
        assertEquals(Category.MOLKEREI, editor.category)
    }

    @Test
    fun `editing an existing item updates it in place`() = runTest(dispatcher) {
        val state = collectUiState()
        repository.add("Brot", "", Category.BACKWAREN)
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
    fun `toggling moves the item into the done section and back`() = runTest(dispatcher) {
        val state = collectUiState()
        repository.add("Brot", "", Category.BACKWAREN)
        advanceUntilIdle()

        viewModel.toggleChecked(state().entries().single())
        advanceUntilIdle()

        assertEquals(0, state().openCount)
        assertEquals(1, state().checkedCount)
        assertTrue(state().rows.any { it is ShoppingListRow.DoneHeader })

        viewModel.toggleChecked(state().entries().single())
        advanceUntilIdle()

        assertEquals(1, state().openCount)
        assertEquals(0, state().checkedCount)
        assertFalse(state().rows.any { it is ShoppingListRow.DoneHeader })
    }

    @Test
    fun `deleting emits an undo message that restores the item`() = runTest(dispatcher) {
        val state = collectUiState()
        val messages = collectUndoMessages()
        repository.add("Brot", "1 Stück", Category.BACKWAREN)
        advanceUntilIdle()
        val stored = state().entries().single()

        viewModel.delete(stored)
        advanceUntilIdle()

        assertTrue(state().rows.isEmpty())
        assertEquals(1, messages.size)
        assertEquals(listOf(stored), messages.single().items)

        viewModel.undo(messages.single())
        advanceUntilIdle()

        val restored = state().entries().single()
        assertEquals("Brot", restored.name)
        assertEquals("1 Stück", restored.quantity)
        assertEquals(Category.BACKWAREN, restored.category)
    }

    @Test
    fun `deleting checked items leaves the open ones alone`() = runTest(dispatcher) {
        val state = collectUiState()
        val messages = collectUndoMessages()
        repository.add("Brot", "", Category.BACKWAREN)
        repository.add("Milch", "", Category.MOLKEREI)
        advanceUntilIdle()
        viewModel.toggleChecked(state().entries().first { it.name == "Brot" })
        advanceUntilIdle()

        viewModel.deleteChecked()
        advanceUntilIdle()

        assertEquals(listOf("Milch"), state().entries().map { it.name })
        assertEquals(0, state().checkedCount)
        assertEquals(listOf("Brot"), messages.single().items.map { it.name })
    }

    @Test
    fun `clearing an already empty list emits no undo message`() = runTest(dispatcher) {
        collectUiState()
        val messages = collectUndoMessages()

        viewModel.deleteChecked()
        viewModel.deleteAll()
        advanceUntilIdle()

        assertTrue(messages.isEmpty())
    }

    @Test
    fun `deleteAll removes everything and can be undone`() = runTest(dispatcher) {
        val state = collectUiState()
        val messages = collectUndoMessages()
        repository.add("Brot", "", Category.BACKWAREN)
        repository.add("Milch", "", Category.MOLKEREI)
        advanceUntilIdle()

        viewModel.deleteAll()
        advanceUntilIdle()
        assertTrue(state().rows.isEmpty())

        viewModel.undo(messages.single())
        advanceUntilIdle()

        assertEquals(2, state().openCount)
        assertEquals(
            listOf("Brot", "Milch"),
            state().entries().map { it.name }.sorted(),
        )
    }

    @Test
    fun `each undo message gets its own id`() = runTest(dispatcher) {
        val state = collectUiState()
        val messages = collectUndoMessages()
        repository.add("Brot", "", Category.BACKWAREN)
        repository.add("Milch", "", Category.MOLKEREI)
        advanceUntilIdle()

        state().entries().forEach { viewModel.delete(it) }
        advanceUntilIdle()

        assertEquals(2, messages.size)
        assertEquals(2, messages.map { it.id }.distinct().size)
    }

    @Test
    fun `dismissing the editor clears what was typed`() {
        viewModel.openAddEditor()
        viewModel.onNameChange("Tippfehler")
        viewModel.dismissEditor()

        val editor = viewModel.editor.value
        assertFalse(editor.visible)
        assertEquals("", editor.name)
        assertEquals(Category.DEFAULT, editor.category)
    }

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

    /** Records undo messages as they are emitted; see [collectUiState]. */
    private fun TestScope.collectUndoMessages(): List<UndoMessage> {
        val messages = mutableListOf<UndoMessage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.undoMessages.collect { messages += it }
        }
        return messages
    }
}
