package io.github.codingkody99.einkaufsliste.ui

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.FakeCategoryOverrideDao
import io.github.codingkody99.einkaufsliste.data.FakeRecipeDao
import io.github.codingkody99.einkaufsliste.data.FakeRecipeFetcher
import io.github.codingkody99.einkaufsliste.data.FakeShoppingDao
import io.github.codingkody99.einkaufsliste.data.FakeShoppingListDao
import io.github.codingkody99.einkaufsliste.data.NewItem
import io.github.codingkody99.einkaufsliste.data.Recipe
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

/** Saving recipes and putting their ingredients on the shopping list. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeBookTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = FakeShoppingDao()
    private val overrideDao = FakeCategoryOverrideDao()
    private val listDao = FakeShoppingListDao()
    private val recipeDao = FakeRecipeDao()
    private var clock = 0L
    private val repository = RoomShoppingRepository(dao, overrideDao, listDao, recipeDao) { ++clock }
    private val fetcher = FakeRecipeFetcher()

    private lateinit var viewModel: ShoppingListViewModel

    private val pancakePage = """
        <html><head><script type="application/ld+json">
        {"@type":"Recipe","name":"Der perfekte Pfannkuchen",
         "recipeIngredient":["250 g Mehl","3 Ei(er)","500 ml Milch","1 Prise Salz"]}
        </script></head><body></body></html>
    """.trimIndent()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ShoppingListViewModel(repository, recipeFetcher = fetcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- the sheet ------------------------------------------------------------

    @Test
    fun `the recipe area is closed until it is opened`() {
        assertFalse(viewModel.recipesVisible.value)
        viewModel.openRecipes()
        assertTrue(viewModel.recipesVisible.value)
        viewModel.dismissRecipes()
        assertFalse(viewModel.recipesVisible.value)
    }

    @Test
    fun `the editor starts empty for a new recipe`() {
        viewModel.startCreateRecipe()

        val editor = viewModel.recipeEditor.value
        assertTrue(editor.visible)
        assertFalse(editor.isEditing)
        assertEquals("", editor.name)
        assertEquals("", editor.ingredientsText)
        assertFalse(editor.canSave)
    }

    // --- typing a recipe ------------------------------------------------------

    @Test
    fun `a typed recipe is saved`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        advanceUntilIdle()

        viewModel.startCreateRecipe()
        viewModel.onRecipeNameChange("Sommerrollen")
        viewModel.onRecipeIngredientsChange("Reisnudeln\nKarotte\nGurke\nTofu")
        assertTrue(viewModel.recipeEditor.value.canSave)
        viewModel.saveRecipe()
        advanceUntilIdle()

        val saved = recipes().single()
        assertEquals("Sommerrollen", saved.name)
        assertEquals(listOf("Reisnudeln", "Karotte", "Gurke", "Tofu"), saved.ingredientLines)
        assertFalse(viewModel.recipeEditor.value.visible)
    }

    @Test
    fun `a recipe without a name or without ingredients is not saved`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.startCreateRecipe()
        viewModel.onRecipeIngredientsChange("Reisnudeln")
        assertFalse(viewModel.recipeEditor.value.canSave)
        viewModel.saveRecipe()
        advanceUntilIdle()

        viewModel.onRecipeNameChange("Sommerrollen")
        viewModel.onRecipeIngredientsChange("   ")
        assertFalse(viewModel.recipeEditor.value.canSave)
        viewModel.saveRecipe()
        advanceUntilIdle()

        assertTrue(recipeDao.recipes.isEmpty())
    }

    @Test
    fun `blank lines are dropped when saving`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        advanceUntilIdle()

        viewModel.startCreateRecipe()
        viewModel.onRecipeNameChange("Test")
        viewModel.onRecipeIngredientsChange("Mehl\n\n  \nMilch\n")
        viewModel.saveRecipe()
        advanceUntilIdle()

        assertEquals(listOf("Mehl", "Milch"), recipes().single().ingredientLines)
    }

    @Test
    fun `an existing recipe can be edited`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        repository.saveRecipe("Sommerrollen", "Reisnudeln\nTofu", "", null)
        advanceUntilIdle()

        viewModel.startEditRecipe(recipes().single())
        assertTrue(viewModel.recipeEditor.value.isEditing)
        viewModel.onRecipeIngredientsChange("Reisnudeln\nTofu\nErdnusssauce")
        viewModel.saveRecipe()
        advanceUntilIdle()

        assertEquals(1, recipes().size)
        assertEquals(
            listOf("Reisnudeln", "Tofu", "Erdnusssauce"),
            recipes().single().ingredientLines,
        )
    }

    @Test
    fun `the method text is saved with the recipe`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        advanceUntilIdle()

        viewModel.startCreateRecipe()
        viewModel.onRecipeNameChange("Sommerrollen")
        viewModel.onRecipeIngredientsChange("Reisnudeln\nTofu")
        viewModel.onRecipeStepsChange("1. Reispapier einweichen\n2. Füllen und rollen")
        viewModel.saveRecipe()
        advanceUntilIdle()

        assertEquals("1. Reispapier einweichen\n2. Füllen und rollen", recipes().single().steps)
    }

    @Test
    fun `a recipe without a method is still valid`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        advanceUntilIdle()

        viewModel.startCreateRecipe()
        viewModel.onRecipeNameChange("Sommerrollen")
        viewModel.onRecipeIngredientsChange("Reisnudeln")
        assertTrue(viewModel.recipeEditor.value.canSave)
        viewModel.saveRecipe()
        advanceUntilIdle()

        assertEquals("", recipes().single().steps)
    }

    @Test
    fun `editing keeps the method text and can change it`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        repository.saveRecipe("Sommerrollen", "Reisnudeln", "Erst einweichen", null)
        advanceUntilIdle()

        viewModel.startEditRecipe(recipes().single())
        assertEquals("Erst einweichen", viewModel.recipeEditor.value.steps)
        viewModel.onRecipeStepsChange("Erst einweichen, dann rollen")
        viewModel.saveRecipe()
        advanceUntilIdle()

        assertEquals("Erst einweichen, dann rollen", recipes().single().steps)
    }

    @Test
    fun `scanned text is appended to the ingredients line by line`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateRecipe()
        viewModel.onRecipeIngredientsChange("Reisnudeln")

        viewModel.onRecipeIngredientsScanned("  Karotte  \n\n  Gurke \n")

        assertEquals(
            listOf("Reisnudeln", "Karotte", "Gurke"),
            viewModel.recipeEditor.value.ingredientsText.split('\n'),
        )
    }

    @Test
    fun `scanned text is appended to the method`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateRecipe()
        viewModel.onRecipeStepsChange("Schritt 1")

        viewModel.onRecipeStepsAppended("Schritt 2")

        assertEquals("Schritt 1\nSchritt 2", viewModel.recipeEditor.value.steps)
    }

    @Test
    fun `scanning nothing changes nothing`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateRecipe()
        viewModel.onRecipeIngredientsScanned("   ")
        viewModel.onRecipeStepsAppended("")

        assertEquals("", viewModel.recipeEditor.value.ingredientsText)
        assertEquals("", viewModel.recipeEditor.value.steps)
    }

    @Test
    fun `a recipe opens for reading and leads on to the list`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        repository.saveRecipe("Sommerrollen", "Reisnudeln\nTofu", "Rollen", null)
        advanceUntilIdle()

        viewModel.viewRecipe(recipes().single())
        assertEquals("Sommerrollen", viewModel.viewedRecipe.value?.name)

        viewModel.useRecipe(recipes().single())
        advanceUntilIdle()

        assertNull("Die Leseansicht schließt sich", viewModel.viewedRecipe.value)
        assertTrue(viewModel.import.value.visible)
    }

    @Test
    fun `editing from the reading view closes it`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        repository.saveRecipe("Sommerrollen", "Reisnudeln", "", null)
        advanceUntilIdle()

        viewModel.viewRecipe(recipes().single())
        viewModel.startEditRecipe(recipes().single())

        assertNull(viewModel.viewedRecipe.value)
        assertTrue(viewModel.recipeEditor.value.visible)
    }

    @Test
    fun `a recipe can be deleted`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        repository.saveRecipe("Sommerrollen", "Reisnudeln", "", null)
        advanceUntilIdle()

        viewModel.deleteRecipe(recipes().single())
        advanceUntilIdle()

        assertTrue(recipes().isEmpty())
    }

    @Test
    fun `recipes are listed by name`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        repository.saveRecipe("Zwiebelkuchen", "Zwiebeln", "", null)
        repository.saveRecipe("Apfelkuchen", "Äpfel", "", null)
        advanceUntilIdle()

        assertEquals(listOf("Apfelkuchen", "Zwiebelkuchen"), recipes().map { it.name })
    }

    // --- dictating a recipe ---------------------------------------------------

    @Test
    fun `a dictated sentence fills name and ingredients`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateRecipe()

        viewModel.onRecipeDictated(
            "Ich will Sommerrollen kochen, dafür brauche ich Reisnudeln, Karotte, Gurke, Tofu",
        )

        val editor = viewModel.recipeEditor.value
        assertEquals("Sommerrollen", editor.name)
        assertEquals(
            listOf("Reisnudeln", "Karotte", "Gurke", "Tofu"),
            editor.ingredientsText.split('\n'),
        )
        assertTrue(editor.canSave)
    }

    @Test
    fun `dictated amounts are kept`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateRecipe()

        viewModel.onRecipeDictated("ich brauche 500 Gramm Mehl und 3 Eier")

        assertEquals(listOf("500 g Mehl", "3 Eier"), viewModel.recipeEditor.value.ingredientsText.split('\n'))
    }

    @Test
    fun `dictating twice appends instead of replacing`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateRecipe()
        viewModel.onRecipeNameChange("Sommerrollen")

        viewModel.onRecipeDictated("Reisnudeln, Karotte")
        viewModel.onRecipeDictated("noch Gurke und Tofu")

        assertEquals(
            listOf("Reisnudeln", "Karotte", "Gurke", "Tofu"),
            viewModel.recipeEditor.value.ingredientsText.split('\n'),
        )
    }

    @Test
    fun `a dictated dish does not overwrite a name already given`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateRecipe()
        viewModel.onRecipeNameChange("Meine Rollen")

        viewModel.onRecipeDictated("ich will Sommerrollen kochen, dafür Reisnudeln")

        assertEquals("Meine Rollen", viewModel.recipeEditor.value.name)
    }

    @Test
    fun `dictating nothing changes nothing`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateRecipe()
        viewModel.onRecipeDictated("   ")

        assertEquals("", viewModel.recipeEditor.value.ingredientsText)
    }

    // --- recipe from a link ---------------------------------------------------

    @Test
    fun `a link in the ingredients field fills the recipe`() = runTest(dispatcher) {
        fetcher.returns(pancakePage)
        advanceUntilIdle()

        viewModel.startCreateRecipe()
        viewModel.onRecipeIngredientsChange("https://www.chefkoch.de/rezepte/1/x.html")
        assertNotNull(viewModel.recipeEditor.value.detectedUrl)
        viewModel.loadRecipeIntoEditor()
        advanceUntilIdle()

        val editor = viewModel.recipeEditor.value
        assertFalse(editor.loading)
        assertNull(editor.error)
        assertEquals("Der perfekte Pfannkuchen", editor.name)
        assertEquals(
            listOf("250 g Mehl", "3 Ei(er)", "500 ml Milch", "1 Prise Salz"),
            editor.ingredientsText.split('\n'),
        )
        assertEquals("https://www.chefkoch.de/rezepte/1/x.html", editor.sourceUrl)
    }

    @Test
    fun `the source link is kept with the saved recipe`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        fetcher.returns(pancakePage)
        advanceUntilIdle()

        viewModel.startCreateRecipe()
        viewModel.onRecipeIngredientsChange("https://www.chefkoch.de/rezepte/1/x.html")
        viewModel.loadRecipeIntoEditor()
        advanceUntilIdle()
        viewModel.saveRecipe()
        advanceUntilIdle()

        assertEquals("https://www.chefkoch.de/rezepte/1/x.html", recipes().single().sourceUrl)
    }

    @Test
    fun `a page without a recipe is reported in the editor`() = runTest(dispatcher) {
        fetcher.returns("<html><body>nichts</body></html>")
        advanceUntilIdle()

        viewModel.startCreateRecipe()
        viewModel.onRecipeIngredientsChange("https://example.org/seite")
        viewModel.loadRecipeIntoEditor()
        advanceUntilIdle()

        val editor = viewModel.recipeEditor.value
        assertFalse(editor.loading)
        assertTrue(editor.error!!.contains("keine Zutaten"))
        // what was typed stays, so it can be fixed instead of retyped
        assertEquals("https://example.org/seite", editor.ingredientsText)
    }

    @Test
    fun `loading without a link does nothing`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startCreateRecipe()
        viewModel.onRecipeIngredientsChange("Mehl\nMilch")
        viewModel.loadRecipeIntoEditor()
        advanceUntilIdle()

        assertTrue(fetcher.requestedUrls.isEmpty())
    }

    @Test
    fun `a name already typed survives loading a link`() = runTest(dispatcher) {
        fetcher.returns(pancakePage)
        advanceUntilIdle()

        viewModel.startCreateRecipe()
        viewModel.onRecipeNameChange("Omas Pfannkuchen")
        viewModel.onRecipeIngredientsChange("https://www.chefkoch.de/rezepte/1/x.html")
        viewModel.loadRecipeIntoEditor()
        advanceUntilIdle()

        assertEquals("Omas Pfannkuchen", viewModel.recipeEditor.value.name)
    }

    // --- using a recipe -------------------------------------------------------

    @Test
    fun `using a recipe opens the import preview, categorised and sorted`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        repository.saveRecipe("Sommerrollen", "Reisnudeln\nKarotte\nGurke\nTofu", "", null)
        advanceUntilIdle()

        viewModel.openRecipes()
        viewModel.useRecipe(recipes().single())
        advanceUntilIdle()

        val importState = viewModel.import.value
        assertFalse("Die Rezeptliste schließt sich", viewModel.recipesVisible.value)
        assertTrue(importState.visible)
        assertEquals("Sommerrollen", importState.sourceTitle)
        assertEquals(
            listOf("Reisnudeln", "Karotte", "Gurke", "Tofu"),
            importState.rows!!.map { it.name },
        )
        assertEquals(
            listOf(Category.OBST_GEMUESE, Category.MOLKEREI, Category.VORRAT),
            importState.previewByCategory.map { it.first },
        )
    }

    @Test
    fun `the ingredients land on the current list once confirmed`() = runTest(dispatcher) {
        val state = collectUiState()
        val recipes = collectRecipes()
        repository.saveRecipe("Sommerrollen", "Reisnudeln\nKarotte\nTofu", "", null)
        advanceUntilIdle()

        viewModel.useRecipe(recipes().single())
        advanceUntilIdle()
        viewModel.applyImport()
        advanceUntilIdle()

        assertEquals(3, state().openCount)
        assertEquals(
            listOf("Karotte", "Tofu", "Reisnudeln"),
            state().entries().map { it.name },
        )
    }

    @Test
    fun `ingredients already on the list are flagged when using a recipe`() = runTest(dispatcher) {
        collectUiState()
        val recipes = collectRecipes()
        repository.add(ShoppingList.DEFAULT_ID, NewItem("Tofu", category = Category.MOLKEREI))
        repository.saveRecipe("Sommerrollen", "Reisnudeln\nTofu", "", null)
        advanceUntilIdle()

        viewModel.useRecipe(recipes().single())
        advanceUntilIdle()

        val tofu = viewModel.import.value.rows!!.single { it.name == "Tofu" }
        assertTrue(tofu.duplicate)
        assertFalse(tofu.selected)
    }

    @Test
    fun `a recipe keeps its wording and is re-read each time it is used`() = runTest(dispatcher) {
        val recipes = collectRecipes()
        repository.saveRecipe("Test", "Yuzu", "", null)
        advanceUntilIdle()

        // Unknown at first ...
        viewModel.useRecipe(recipes().single())
        advanceUntilIdle()
        assertEquals(Category.DEFAULT, viewModel.import.value.rows!!.single().category)
        viewModel.dismissImport()

        // ... and filed correctly once the app has learned it.
        repository.rememberCategory("Yuzu", Category.OBST_GEMUESE)
        advanceUntilIdle()
        viewModel.useRecipe(recipes().single())
        advanceUntilIdle()

        assertEquals(Category.OBST_GEMUESE, viewModel.import.value.rows!!.single().category)
        assertEquals("Yuzu", recipes().single().ingredientsText)
    }

    // --- dictating into the shopping list -------------------------------------

    @Test
    fun `dictated text is appended to the import field`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()

        viewModel.onImportDictated("Milch, Brot")
        viewModel.onImportDictated("und Butter")

        assertEquals("Milch, Brot\nund Butter", viewModel.import.value.text)
    }

    @Test
    fun `a dictated shopping list analyses into items`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openImport()
        viewModel.onImportDictated("ich brauche Milch, Brot und 500 Gramm Hackfleisch")
        viewModel.analyzeImport()
        advanceUntilIdle()

        val rows = viewModel.import.value.rows!!
        assertEquals(listOf("Milch", "Brot", "Hackfleisch"), rows.map { it.name })
        assertEquals("500 g", rows.last().quantity)
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

    private fun TestScope.collectRecipes(): () -> List<Recipe> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.recipes.collect {}
        }
        return { viewModel.recipes.value }
    }
}
