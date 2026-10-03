package io.github.codingkody99.einkaufsliste.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for the Room DAO, mirroring its ordering guarantees. */
class FakeShoppingDao : ShoppingDao {

    private val state = MutableStateFlow<List<ShoppingItem>>(emptyList())
    private var nextId = 1L

    val items: List<ShoppingItem> get() = sorted(state.value)

    private fun sorted(list: List<ShoppingItem>) =
        list.sortedWith(compareBy({ it.createdAt }, { it.id }))

    override fun observeAll(): Flow<List<ShoppingItem>> = state.map { sorted(it) }

    override suspend fun insert(item: ShoppingItem): Long {
        val id = if (item.id == 0L) nextId++ else item.id
        state.value = state.value + item.copy(id = id)
        return id
    }

    override suspend fun update(item: ShoppingItem) {
        state.value = state.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun setChecked(id: Long, checked: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(isChecked = checked) else it }
    }

    override suspend fun deleteById(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun deleteChecked() {
        state.value = state.value.filterNot { it.isChecked }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}
