package io.github.codingkody99.einkaufsliste.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for the list table. */
class FakeShoppingListDao : ShoppingListDao {

    private val state = MutableStateFlow<List<ShoppingList>>(emptyList())
    private var nextId = 1L

    val lists: List<ShoppingList> get() = sorted(state.value)

    private fun sorted(list: List<ShoppingList>) =
        list.sortedWith(compareBy({ it.position }, { it.id }))

    override fun observeAll(): Flow<List<ShoppingList>> = state.map { sorted(it) }

    override suspend fun firstList(): ShoppingList? = sorted(state.value).firstOrNull()

    override suspend fun count(): Int = state.value.size

    override suspend fun maxPosition(): Int? = state.value.maxOfOrNull { it.position }

    override suspend fun insert(list: ShoppingList): Long {
        val id = if (list.id == 0L) nextId++ else list.id
        if (id >= nextId) nextId = id + 1
        state.value = state.value + list.copy(id = id)
        return id
    }

    override suspend fun rename(id: Long, name: String) {
        state.value = state.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}
