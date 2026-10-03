package io.github.codingkody99.einkaufsliste.ui

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.FakeCategoryOverrideDao
import io.github.codingkody99.einkaufsliste.data.FakeShoppingDao
import io.github.codingkody99.einkaufsliste.data.FakeShoppingListDao
import io.github.codingkody99.einkaufsliste.data.NewItem
import io.github.codingkody99.einkaufsliste.data.RoomShoppingRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Several lists, with the main one always in front when the app opens. */
@OptIn(ExperimentalCoroutinesApi::class)
class MultipleListsTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = FakeShoppingDao()
    private val overrideDao = FakeCategoryOverrideDao()
    private val listDao = FakeShoppingListDao()
    private var clock = 0L
    private val repository = RoomShoppingRepository(dao, overrideDao, listDao) { ++clock }

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
    fun `a main list is created on first start`() = runTest(dispatcher) {
        advanceUntilIdle()

        assertEquals(ShoppingList.DEFAULT_ID, viewModel.selectedListId.value)
        assertEquals(listOf(ShoppingList.DEFAULT_NAME), listDao.lists.map { it.name })
    }

    @Test
    fun `an existing main list is reused instead of duplicated`() = runTest(dispatcher) {
        advanceUntilIdle()
        val second = ShoppingListViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, listDao.lists.size)
        assertEquals(ShoppingList.DEFAULT_ID, second.selectedListId.value)
    }

    @Test
    fun `creating a list switches to it`() = runTest(dispatcher) {
        val summaries = collectLists()
        advanceUntilIdle()

        viewModel.startCreateList()
        viewModel.onNameDialogChange("Drogerie")
        viewModel.confirmNameDialog()
        advanceUntilIdle()

        assertEquals(listOf("Einkaufsliste", "Drogerie"), summaries().map { it.name })
        assertEquals("Drogerie", summaries().single { it.isCurrent }.name)
        assertNull(viewModel.switcher.value.nameDialog)
    }

    @Test
    fun `the first list stays the main one`() = runTest(dispatcher) {
        val summaries = collectLists()
        advanceUntilIdle()
        viewModel.startCreateList()
        viewModel.onNameDialogChange("Drogerie")
        viewModel.confirmNameDialog()
        advanceUntilIdle()

        assertEquals("Einkaufsliste", summaries().single { it.isMain }.name)
    }

    @Test
    fun `a fresh view model opens on the main list, not the last used one`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateList()
        viewModel.onNameDialogChange("Drogerie")
        viewModel.confirmNameDialog()
        advanceUntilIdle()
        val drogerie = viewModel.selectedListId.value
        assertEquals(2L, drogerie)

        // Reopening the app
        val reopened = ShoppingListViewModel(repository)
        advanceUntilIdle()

        assertEquals(ShoppingList.DEFAULT_ID, reopened.selectedListId.value)
    }

    @Test
    fun `a list with a blank name is not created`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateList()
        viewModel.onNameDialogChange("   ")

        assertFalse(viewModel.switcher.value.nameDialog!!.canSave)
        viewModel.confirmNameDialog()
        advanceUntilIdle()

        assertEquals(1, listDao.lists.size)
        assertNotNull("Der Dialog bleibt offen", viewModel.switcher.value.nameDialog)
    }

    @Test
    fun `items belong to the list they were added to`() = runTest(dispatcher) {
        val state = collectUiState()
        advanceUntilIdle()

        viewModel.onQuickAddChange("Tomaten")
        viewModel.submitQuickAdd()
        advanceUntilIdle()

        viewModel.startCreateList()
        viewModel.onNameDialogChange("Drogerie")
        viewModel.confirmNameDialog()
        advanceUntilIdle()

        viewModel.onQuickAddChange("Zahnpasta")
        viewModel.submitQuickAdd()
        advanceUntilIdle()

        // The new list shows only its own item ...
        assertEquals(listOf("Zahnpasta"), state().entries().map { it.name })

        // ... and switching back shows the other one again.
        viewModel.selectList(ShoppingList.DEFAULT_ID)
        advanceUntilIdle()
        assertEquals(listOf("Tomaten"), state().entries().map { it.name })
    }

    @Test
    fun `the switcher reports how full each list is`() = runTest(dispatcher) {
        val summaries = collectLists()
        advanceUntilIdle()
        repository.add(ShoppingList.DEFAULT_ID, NewItem("Tomaten", category = Category.OBST_GEMUESE))
        repository.add(ShoppingList.DEFAULT_ID, NewItem("Milch", category = Category.MOLKEREI))
        advanceUntilIdle()
        val milk = dao.items.first { it.name == "Milch" }
        repository.setChecked(milk.id, true)
        advanceUntilIdle()

        val main = summaries().single { it.isMain }
        assertEquals(1, main.openCount)
        assertEquals(2, main.totalCount)
    }

    @Test
    fun `an empty list reports zero rather than nothing`() = runTest(dispatcher) {
        val summaries = collectLists()
        advanceUntilIdle()

        assertEquals(0, summaries().single().openCount)
        assertEquals(0, summaries().single().totalCount)
    }

    @Test
    fun `renaming keeps the items`() = runTest(dispatcher) {
        val state = collectUiState()
        val summaries = collectLists()
        advanceUntilIdle()
        viewModel.onQuickAddChange("Tomaten")
        viewModel.submitQuickAdd()
        advanceUntilIdle()

        viewModel.startRenameList(listDao.lists.first())
        viewModel.onNameDialogChange("Wocheneinkauf")
        viewModel.confirmNameDialog()
        advanceUntilIdle()

        assertEquals(listOf("Wocheneinkauf"), summaries().map { it.name })
        assertEquals(listOf("Tomaten"), state().entries().map { it.name })
    }

    @Test
    fun `deleting a list removes its items too`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateList()
        viewModel.onNameDialogChange("Drogerie")
        viewModel.confirmNameDialog()
        advanceUntilIdle()
        viewModel.onQuickAddChange("Zahnpasta")
        viewModel.submitQuickAdd()
        advanceUntilIdle()
        assertEquals(1, dao.items.size)

        val drogerie = listDao.lists.single { it.name == "Drogerie" }
        viewModel.deleteList(
            ListSummary(drogerie, openCount = 1, totalCount = 1, isCurrent = true, isMain = false),
        )
        advanceUntilIdle()

        assertEquals(listOf("Einkaufsliste"), listDao.lists.map { it.name })
        assertTrue(dao.items.isEmpty())
    }

    @Test
    fun `deleting the current list falls back to the main one`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateList()
        viewModel.onNameDialogChange("Drogerie")
        viewModel.confirmNameDialog()
        advanceUntilIdle()
        val drogerie = listDao.lists.single { it.name == "Drogerie" }

        viewModel.deleteList(
            ListSummary(drogerie, openCount = 0, totalCount = 0, isCurrent = true, isMain = false),
        )
        advanceUntilIdle()

        assertEquals(ShoppingList.DEFAULT_ID, viewModel.selectedListId.value)
    }

    @Test
    fun `the last list cannot be deleted`() = runTest(dispatcher) {
        val notices = collectNotices()
        advanceUntilIdle()
        val main = listDao.lists.single()

        viewModel.deleteList(
            ListSummary(main, openCount = 0, totalCount = 0, isCurrent = true, isMain = true),
        )
        advanceUntilIdle()

        assertEquals(1, listDao.lists.size)
        assertEquals("Die letzte Liste kann nicht gelöscht werden.", notices.single().text)
        assertFalse(notices.single().canUndo)
    }

    @Test
    fun `clearing affects only the current list`() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.add(ShoppingList.DEFAULT_ID, NewItem("Tomaten"))
        viewModel.startCreateList()
        viewModel.onNameDialogChange("Drogerie")
        viewModel.confirmNameDialog()
        advanceUntilIdle()
        viewModel.onQuickAddChange("Zahnpasta")
        viewModel.submitQuickAdd()
        advanceUntilIdle()

        viewModel.clearList()
        advanceUntilIdle()

        assertEquals(listOf("Tomaten"), dao.items.map { it.name })
    }

    @Test
    fun `the switcher opens and closes`() {
        assertFalse(viewModel.switcher.value.visible)
        viewModel.openSwitcher()
        assertTrue(viewModel.switcher.value.visible)
        viewModel.dismissSwitcher()
        assertFalse(viewModel.switcher.value.visible)
    }

    @Test
    fun `picking a list closes the switcher`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openSwitcher()
        viewModel.selectList(ShoppingList.DEFAULT_ID)

        assertFalse(viewModel.switcher.value.visible)
    }

    // --- quick add ------------------------------------------------------------

    @Test
    fun `the quick field classifies while typing`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.onQuickAddChange("Tomaten")

        val state = viewModel.quickAdd.value
        assertEquals(Category.OBST_GEMUESE, state.suggested)
        assertTrue(state.recognized)
        assertTrue(state.canAdd)
    }

    @Test
    fun `an unknown name in the quick field is flagged`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.onQuickAddChange("Flurbereinigungsgerät")

        assertFalse(viewModel.quickAdd.value.recognized)
        assertEquals(Category.DEFAULT, viewModel.quickAdd.value.suggested)
    }

    @Test
    fun `submitting adds the item and clears the field for the next one`() = runTest(dispatcher) {
        val state = collectUiState()
        advanceUntilIdle()

        viewModel.onQuickAddChange("Tomaten")
        viewModel.submitQuickAdd()
        advanceUntilIdle()

        assertEquals("", viewModel.quickAdd.value.text)
        val item = state().entries().single()
        assertEquals("Tomaten", item.name)
        assertEquals(Category.OBST_GEMUESE, item.category)

        viewModel.onQuickAddChange("Milch")
        viewModel.submitQuickAdd()
        advanceUntilIdle()

        assertEquals(listOf("Tomaten", "Milch"), state().entries().map { it.name })
    }

    @Test
    fun `submitting a blank name does nothing`() = runTest(dispatcher) {
        val state = collectUiState()
        advanceUntilIdle()

        viewModel.onQuickAddChange("   ")
        assertFalse(viewModel.quickAdd.value.canAdd)
        viewModel.submitQuickAdd()
        advanceUntilIdle()

        assertTrue(state().rows.isEmpty())
    }

    @Test
    fun `a learned category applies to the quick field`() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.rememberCategory("Yuzu", Category.OBST_GEMUESE)
        advanceUntilIdle()

        viewModel.onQuickAddChange("Yuzu")

        assertEquals(Category.OBST_GEMUESE, viewModel.quickAdd.value.suggested)
    }

    @Test
    fun `expanding hands what is typed to the full editor`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.onQuickAddChange("Tomaten")
        viewModel.expandQuickAdd()

        assertEquals("", viewModel.quickAdd.value.text)
        val editor = viewModel.editor.value
        assertTrue(editor.visible)
        assertEquals("Tomaten", editor.name)
        assertEquals(Category.OBST_GEMUESE, editor.category)
    }

    @Test
    fun `expanding an empty field just opens the editor`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.expandQuickAdd()

        assertTrue(viewModel.editor.value.visible)
        assertEquals("", viewModel.editor.value.name)
    }

    // --- helpers --------------------------------------------------------------

    private fun ShoppingListUiState.entries() =
        rows.filterIsInstance<ShoppingListRow.Entry>().map { it.item }

    private fun TestScope.collectUiState(): () -> ShoppingListUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        return { viewModel.uiState.value }
    }

    private fun TestScope.collectLists(): () -> List<ListSummary> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.lists.collect {}
        }
        return { viewModel.lists.value }
    }

    private fun TestScope.collectNotices(): List<Notice> {
        val notices = mutableListOf<Notice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.notices.collect { notices += it }
        }
        return notices
    }
}
