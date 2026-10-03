package io.github.codingkody99.einkaufsliste.ui

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.FakeCategoryOverrideDao
import io.github.codingkody99.einkaufsliste.data.FakeRecipeFetcher
import io.github.codingkody99.einkaufsliste.data.FakeShoppingDao
import io.github.codingkody99.einkaufsliste.data.NewItem
import io.github.codingkody99.einkaufsliste.data.FakeRecipeDao
import io.github.codingkody99.einkaufsliste.data.FakeShoppingListDao
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

/** The "paste a recipe link" path, end to end apart from the actual download. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeImportTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = FakeShoppingDao()
    private val overrideDao = FakeCategoryOverrideDao()
    private val listDao = FakeShoppingListDao()
    private val recipeDao = FakeRecipeDao()
    private var clock = 0L
    private val repository = RoomShoppingRepository(dao, overrideDao, listDao, recipeDao) { ++clock }

    private val listId = ShoppingList.DEFAULT_ID
    private val fetcher = FakeRecipeFetcher()

    private lateinit var viewModel: ShoppingListViewModel

    private val pancakePage = """
        <html><head><title>Pfannkuchen | Chefkoch</title>
        <script type="application/ld+json">
        {"@context":"https://schema.org","@graph":[
          {"@type":"WebSite","name":"Chefkoch"},
          {"@type":["Recipe","CreativeWork"],
           "name":"Der perfekte Pfannkuchen",
           "recipeYield":"4 Portionen",
           "recipeIngredient":["250 g Mehl","3 Ei(er)","500 ml Milch",
             "1 Prise Salz","2 EL Zucker","etwas Butter zum Braten"]}
        ]}</script></head><body></body></html>
    """.trimIndent()

    private val recipeUrl =
        "https://www.chefkoch.de/rezepte/1208161226570428/Der-perfekte-Pfannkuchen.html"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ShoppingListViewModel(repository, recipeFetcher = fetcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a pasted link is detected`() {
        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)

        val state = viewModel.import.value
        assertEquals(recipeUrl, state.detectedUrl)
        assertTrue(state.looksLikeOnlyUrl)
    }

    @Test
    fun `a link shared together with its title is still just a link`() {
        viewModel.openImport()
        viewModel.onImportTextChange("Der perfekte Pfannkuchen | Chefkoch\n$recipeUrl")

        assertTrue(viewModel.import.value.looksLikeOnlyUrl)
    }

    @Test
    fun `a shopping list is not treated as a link`() {
        viewModel.openImport()
        viewModel.onImportTextChange("Milch\nBrot\nTomaten")

        val state = viewModel.import.value
        assertNull(state.detectedUrl)
        assertFalse(state.looksLikeOnlyUrl)
    }

    @Test
    fun `loading a recipe turns its ingredients into categorised rows`() = runTest(dispatcher) {
        fetcher.returns(pancakePage)
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()
        advanceUntilIdle()

        val state = viewModel.import.value
        assertFalse(state.loading)
        assertNull(state.error)
        assertEquals("Der perfekte Pfannkuchen", state.sourceTitle)
        assertEquals(listOf(recipeUrl), fetcher.requestedUrls)

        val rows = state.rows!!
        assertEquals(listOf("Mehl", "Ei", "Milch", "Salz", "Zucker", "Butter"), rows.map { it.name })
        assertEquals(
            listOf("250 g", "3", "500 ml", "1 Prise", "2 EL", ""),
            rows.map { it.quantity },
        )
        assertEquals(
            listOf(
                Category.VORRAT, Category.MOLKEREI, Category.MOLKEREI,
                Category.VORRAT, Category.VORRAT, Category.MOLKEREI,
            ),
            rows.map { it.category },
        )
        assertTrue(rows.all { it.selected })
    }

    @Test
    fun `the recipe preview is grouped in supermarket order`() = runTest(dispatcher) {
        fetcher.returns(pancakePage)
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()
        advanceUntilIdle()

        assertEquals(
            listOf(Category.MOLKEREI, Category.VORRAT),
            viewModel.import.value.previewByCategory.map { it.first },
        )
    }

    @Test
    fun `applying a loaded recipe puts it on the list in order`() = runTest(dispatcher) {
        val state = collectUiState()
        fetcher.returns(pancakePage)
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()
        advanceUntilIdle()
        viewModel.applyImport()
        advanceUntilIdle()

        assertEquals(6, state().openCount)
        assertEquals(
            listOf(Category.MOLKEREI, Category.VORRAT),
            state().rows.filterIsInstance<ShoppingListRow.CategoryHeader>().map { it.category },
        )
        assertEquals(
            listOf("Ei", "Milch", "Butter", "Mehl", "Salz", "Zucker"),
            state().entries().map { it.name },
        )
    }

    @Test
    fun `ingredients already on the list are flagged as duplicates`() = runTest(dispatcher) {
        collectUiState()
        repository.add(listId, NewItem("Milch", category = Category.MOLKEREI))
        fetcher.returns(pancakePage)
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()
        advanceUntilIdle()

        val milk = viewModel.import.value.rows!!.single { it.name == "Milch" }
        assertTrue(milk.duplicate)
        assertFalse(milk.selected)
    }

    @Test
    fun `a learned category applies to recipe ingredients too`() = runTest(dispatcher) {
        collectUiState()
        repository.rememberCategory("Mehl", Category.SUESS_SNACKS)
        fetcher.returns(pancakePage)
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()
        advanceUntilIdle()

        assertEquals(
            Category.SUESS_SNACKS,
            viewModel.import.value.rows!!.single { it.name == "Mehl" }.category,
        )
    }

    @Test
    fun `a page without a recipe reports it instead of importing nothing`() = runTest(dispatcher) {
        fetcher.returns("<html><body><p>Kein Rezept.</p></body></html>")
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()
        advanceUntilIdle()

        val state = viewModel.import.value
        assertNull(state.rows)
        assertFalse(state.loading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("keine Zutaten"))
        // the text is kept so the user can still paste the ingredients by hand
        assertEquals(recipeUrl, state.text)
    }

    @Test
    fun `a download failure is reported verbatim`() = runTest(dispatcher) {
        fetcher.fails("Keine Verbindung — ist das Internet erreichbar?")
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()
        advanceUntilIdle()

        assertEquals("Keine Verbindung — ist das Internet erreichbar?", viewModel.import.value.error)
        assertFalse(viewModel.import.value.loading)
    }

    @Test
    fun `the sheet shows that it is working while loading`() = runTest(dispatcher) {
        fetcher.returns(pancakePage)
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()

        // before the coroutine runs
        assertTrue(viewModel.import.value.loading)
        assertFalse(viewModel.import.value.canAnalyze)

        advanceUntilIdle()
        assertFalse(viewModel.import.value.loading)
    }

    @Test
    fun `loading twice in a row does not fetch twice`() = runTest(dispatcher) {
        fetcher.returns(pancakePage)
        advanceUntilIdle()

        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()
        viewModel.loadRecipe()
        advanceUntilIdle()

        assertEquals(1, fetcher.requestedUrls.size)
    }

    @Test
    fun `loading without a link does nothing`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("Milch\nBrot")
        viewModel.loadRecipe()
        advanceUntilIdle()

        assertTrue(fetcher.requestedUrls.isEmpty())
        assertNull(viewModel.import.value.rows)
        assertFalse(viewModel.import.value.loading)
    }

    @Test
    fun `editing the text clears a previous error and title`() = runTest(dispatcher) {
        fetcher.fails("kaputt")
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()
        advanceUntilIdle()
        assertNotNull(viewModel.import.value.error)

        viewModel.onImportTextChange("$recipeUrl ")

        assertNull(viewModel.import.value.error)
        assertNull(viewModel.import.value.sourceTitle)
    }

    @Test
    fun `analysing a link as text keeps working as an escape hatch`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange("250 g Mehl\n3 Eier")
        viewModel.analyzeImport()
        advanceUntilIdle()

        assertEquals(listOf("Mehl", "Eier"), viewModel.import.value.rows!!.map { it.name })
        assertNull(viewModel.import.value.sourceTitle)
    }

    @Test
    fun `dismissing clears the loaded recipe`() = runTest(dispatcher) {
        fetcher.returns(pancakePage)
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportTextChange(recipeUrl)
        viewModel.loadRecipe()
        advanceUntilIdle()

        viewModel.dismissImport()

        val state = viewModel.import.value
        assertFalse(state.visible)
        assertNull(state.rows)
        assertNull(state.sourceTitle)
        assertEquals("", state.text)
    }

    private fun ShoppingListUiState.entries() =
        rows.filterIsInstance<ShoppingListRow.Entry>().map { it.item }

    private fun TestScope.collectUiState(): () -> ShoppingListUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        return { viewModel.uiState.value }
    }
}
