package io.github.codingkody99.einkaufsliste.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for the Room DAO, mirroring its ordering guarantees. */
class FakeShoppingDao : ShoppingDao {

    private val state = MutableStateFlow<List<ShoppingItem>>(emptyList())
    private var nextId = 1L

    val items: List<ShoppingItem> get() = sorted(state.value)

    fun itemsOf(listId: Long): List<ShoppingItem> = items.filter { it.listId == listId }

    private fun sorted(list: List<ShoppingItem>) =
        list.sortedWith(compareBy({ it.createdAt }, { it.id }))

    override fun observeByList(listId: Long): Flow<List<ShoppingItem>> =
        state.map { all -> sorted(all).filter { it.listId == listId } }

    override fun observeCounts(): Flow<List<ListItemCounts>> = state.map { all ->
        all.groupBy { it.listId }.map { (listId, rows) ->
            ListItemCounts(
                listId = listId,
                openCount = rows.count { !it.isChecked },
                totalCount = rows.size,
            )
        }
    }

    override suspend fun insert(item: ShoppingItem): Long {
        val id = if (item.id == 0L) nextId++ else item.id
        if (id >= nextId) nextId = id + 1
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

    override suspend fun deleteChecked(listId: Long) {
        state.value = state.value.filterNot { it.listId == listId && it.isChecked }
    }

    override suspend fun deleteByList(listId: Long) {
        state.value = state.value.filterNot { it.listId == listId }
    }
}
