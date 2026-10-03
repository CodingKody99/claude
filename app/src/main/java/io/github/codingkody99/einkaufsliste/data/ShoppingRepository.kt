package io.github.codingkody99.einkaufsliste.data

import io.github.codingkody99.einkaufsliste.domain.TextNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** An item about to be put on a list, already filed into a category. */
data class NewItem(
    val name: String,
    val quantity: String = "",
    val category: Category = Category.DEFAULT,
)

interface ShoppingRepository {

    // --- lists ----------------------------------------------------------------

    fun observeLists(): Flow<List<ShoppingList>>

    /** How many open and total items each list holds, keyed by list id. */
    fun observeListCounts(): Flow<Map<Long, ListItemCounts>>

    /** Creates the main list if there is none, and returns its id either way. */
    suspend fun ensureMainList(): Long

    suspend fun createList(name: String): Long?

    suspend fun renameList(id: Long, name: String)

    /** Removes a list and its items. Refuses to remove the last remaining list. */
    suspend fun deleteList(id: Long): Boolean

    // --- items ----------------------------------------------------------------

    fun observeItems(listId: Long): Flow<List<ShoppingItem>>

    /** Categories the user corrected, keyed by normalized name. */
    fun observeOverrides(): Flow<Map<String, Category>>

    /** Returns the new row id, or null when the name was blank. */
    suspend fun add(listId: Long, item: NewItem): Long?

    /** Returns the new row ids, in the order the items were given. */
    suspend fun addAll(listId: Long, items: List<NewItem>): List<Long>

    suspend fun setChecked(id: Long, checked: Boolean)

    suspend fun update(item: ShoppingItem, name: String, quantity: String, category: Category)

    suspend fun delete(id: Long)

    /** Re-inserts deleted items (under fresh ids) to back the undo action. */
    suspend fun restore(items: List<ShoppingItem>)

    suspend fun deleteChecked(listId: Long)

    suspend fun clearList(listId: Long)

    // --- recipes --------------------------------------------------------------

    fun observeRecipes(): Flow<List<Recipe>>

    /** Returns the new id, or null when name or ingredients were blank. */
    suspend fun saveRecipe(name: String, ingredientsText: String, sourceUrl: String?): Long?

    suspend fun updateRecipe(recipe: Recipe, name: String, ingredientsText: String)

    suspend fun deleteRecipe(id: Long)

    // --- learned categories ---------------------------------------------------

    /** Teaches the app where this name belongs from now on. */
    suspend fun rememberCategory(name: String, category: Category)

    suspend fun forgetCategory(name: String)
}

class RoomShoppingRepository(
    private val dao: ShoppingDao,
    private val overrideDao: CategoryOverrideDao,
    private val listDao: ShoppingListDao,
    private val recipeDao: RecipeDao,
    private val now: () -> Long = System::currentTimeMillis,
) : ShoppingRepository {

    // --- lists ----------------------------------------------------------------

    override fun observeLists(): Flow<List<ShoppingList>> = listDao.observeAll()

    override fun observeListCounts(): Flow<Map<Long, ListItemCounts>> =
        dao.observeCounts().map { rows -> rows.associateBy { it.listId } }

    override suspend fun ensureMainList(): Long {
        listDao.firstList()?.let { return it.id }
        return listDao.insert(
            ShoppingList(
                id = ShoppingList.DEFAULT_ID,
                name = ShoppingList.DEFAULT_NAME,
                position = 0,
                createdAt = now(),
            ),
        )
    }

    override suspend fun createList(name: String): Long? {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return null
        val position = (listDao.maxPosition() ?: 0) + 1
        return listDao.insert(ShoppingList(name = cleanName, position = position, createdAt = now()))
    }

    override suspend fun renameList(id: Long, name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        listDao.rename(id, cleanName)
    }

    override suspend fun deleteList(id: Long): Boolean {
        // Without a list there is nowhere to put anything, so one always remains.
        if (listDao.count() <= 1) return false
        dao.deleteByList(id)
        listDao.delete(id)
        return true
    }

    // --- items ----------------------------------------------------------------

    override fun observeItems(listId: Long): Flow<List<ShoppingItem>> = dao.observeByList(listId)

    override fun observeOverrides(): Flow<Map<String, Category>> =
        overrideDao.observeAll().map { rows -> rows.associate { it.nameKey to it.category } }

    override suspend fun add(listId: Long, item: NewItem): Long? {
        val name = item.name.trim()
        if (name.isEmpty()) return null
        return dao.insert(
            ShoppingItem(
                listId = listId,
                name = name,
                quantity = item.quantity.trim(),
                category = item.category,
                isChecked = false,
                createdAt = now(),
            ),
        )
    }

    override suspend fun addAll(listId: Long, items: List<NewItem>): List<Long> =
        items.mapNotNull { add(listId, it) }

    override suspend fun setChecked(id: Long, checked: Boolean) = dao.setChecked(id, checked)

    override suspend fun update(
        item: ShoppingItem,
        name: String,
        quantity: String,
        category: Category,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        dao.update(item.copy(name = cleanName, quantity = quantity.trim(), category = category))
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun restore(items: List<ShoppingItem>) {
        items.forEach { dao.insert(it.copy(id = 0)) }
    }

    override suspend fun deleteChecked(listId: Long) = dao.deleteChecked(listId)

    override suspend fun clearList(listId: Long) = dao.deleteByList(listId)

    // --- recipes --------------------------------------------------------------

    override fun observeRecipes(): Flow<List<Recipe>> = recipeDao.observeAll()

    override suspend fun saveRecipe(
        name: String,
        ingredientsText: String,
        sourceUrl: String?,
    ): Long? {
        val cleanName = name.trim()
        val lines = cleanLines(ingredientsText)
        if (cleanName.isEmpty() || lines.isEmpty()) return null
        return recipeDao.insert(
            Recipe(
                name = cleanName,
                sourceUrl = sourceUrl?.trim()?.takeIf { it.isNotEmpty() },
                ingredientsText = lines.joinToString("\n"),
                createdAt = now(),
            ),
        )
    }

    override suspend fun updateRecipe(recipe: Recipe, name: String, ingredientsText: String) {
        val cleanName = name.trim()
        val lines = cleanLines(ingredientsText)
        if (cleanName.isEmpty() || lines.isEmpty()) return
        recipeDao.update(
            recipe.copy(name = cleanName, ingredientsText = lines.joinToString("\n")),
        )
    }

    override suspend fun deleteRecipe(id: Long) = recipeDao.delete(id)

    private fun cleanLines(text: String): List<String> =
        text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    // --- learned categories ---------------------------------------------------

    override suspend fun rememberCategory(name: String, category: Category) {
        val key = TextNormalizer.normalize(name)
        if (key.isEmpty()) return
        overrideDao.upsert(CategoryOverride(nameKey = key, category = category))
    }

    override suspend fun forgetCategory(name: String) {
        val key = TextNormalizer.normalize(name)
        if (key.isEmpty()) return
        overrideDao.delete(key)
    }
}
