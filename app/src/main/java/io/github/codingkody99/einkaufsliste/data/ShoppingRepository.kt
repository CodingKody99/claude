package io.github.codingkody99.einkaufsliste.data

import io.github.codingkody99.einkaufsliste.domain.TextNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** An item about to be put on the list, already filed into a category. */
data class NewItem(
    val name: String,
    val quantity: String = "",
    val category: Category = Category.DEFAULT,
)

interface ShoppingRepository {

    fun observeItems(): Flow<List<ShoppingItem>>

    /** Categories the user corrected, keyed by normalized name. */
    fun observeOverrides(): Flow<Map<String, Category>>

    /** Returns the new row id, or null when the name was blank. */
    suspend fun add(item: NewItem): Long?

    /** Returns the new row ids, in the order the items were given. */
    suspend fun addAll(items: List<NewItem>): List<Long>

    suspend fun setChecked(id: Long, checked: Boolean)

    suspend fun update(item: ShoppingItem, name: String, quantity: String, category: Category)

    suspend fun delete(id: Long)

    /** Re-inserts deleted items (under fresh ids) to back the undo action. */
    suspend fun restore(items: List<ShoppingItem>)

    suspend fun deleteChecked()

    suspend fun deleteAll()

    /** Teaches the app where this name belongs from now on. */
    suspend fun rememberCategory(name: String, category: Category)

    suspend fun forgetCategory(name: String)
}

class RoomShoppingRepository(
    private val dao: ShoppingDao,
    private val overrideDao: CategoryOverrideDao,
    private val now: () -> Long = System::currentTimeMillis,
) : ShoppingRepository {

    override fun observeItems(): Flow<List<ShoppingItem>> = dao.observeAll()

    override fun observeOverrides(): Flow<Map<String, Category>> =
        overrideDao.observeAll().map { rows -> rows.associate { it.nameKey to it.category } }

    override suspend fun add(item: NewItem): Long? {
        val name = item.name.trim()
        if (name.isEmpty()) return null
        return dao.insert(
            ShoppingItem(
                name = name,
                quantity = item.quantity.trim(),
                category = item.category,
                isChecked = false,
                createdAt = now(),
            ),
        )
    }

    override suspend fun addAll(items: List<NewItem>): List<Long> =
        items.mapNotNull { add(it) }

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

    override suspend fun deleteChecked() = dao.deleteChecked()

    override suspend fun deleteAll() = dao.deleteAll()

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
