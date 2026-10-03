package io.github.codingkody99.einkaufsliste.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for the recipe table, sorted by name like the query. */
class FakeRecipeDao : RecipeDao {

    private val state = MutableStateFlow<List<Recipe>>(emptyList())
    private var nextId = 1L

    val recipes: List<Recipe> get() = sorted(state.value)

    private fun sorted(list: List<Recipe>) = list.sortedBy { it.name.lowercase() }

    override fun observeAll(): Flow<List<Recipe>> = state.map { sorted(it) }

    override suspend fun insert(recipe: Recipe): Long {
        val id = if (recipe.id == 0L) nextId++ else recipe.id
        if (id >= nextId) nextId = id + 1
        state.value = state.value + recipe.copy(id = id)
        return id
    }

    override suspend fun update(recipe: Recipe) {
        state.value = state.value.map { if (it.id == recipe.id) recipe else it }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}
