package io.github.codingkody99.einkaufsliste.data

import kotlinx.coroutines.flow.Flow

interface ShoppingRepository {
    fun observeItems(): Flow<List<ShoppingItem>>
    suspend fun add(name: String, quantity: String, category: Category)
    suspend fun setChecked(id: Long, checked: Boolean)
    suspend fun rename(item: ShoppingItem, name: String, quantity: String, category: Category)
    suspend fun delete(id: Long)

    /** Re-inserts a deleted item (under a fresh id) to back the undo action. */
    suspend fun restore(items: List<ShoppingItem>)
    suspend fun deleteChecked()
    suspend fun deleteAll()
}

class RoomShoppingRepository(
    private val dao: ShoppingDao,
    private val now: () -> Long = System::currentTimeMillis,
) : ShoppingRepository {

    override fun observeItems(): Flow<List<ShoppingItem>> = dao.observeAll()

    override suspend fun add(name: String, quantity: String, category: Category) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        dao.insert(
            ShoppingItem(
                name = cleanName,
                quantity = quantity.trim(),
                category = category,
                isChecked = false,
                createdAt = now(),
            ),
        )
    }

    override suspend fun setChecked(id: Long, checked: Boolean) = dao.setChecked(id, checked)

    override suspend fun rename(
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
}
