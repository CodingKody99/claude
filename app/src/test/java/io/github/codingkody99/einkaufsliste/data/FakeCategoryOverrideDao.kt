package io.github.codingkody99.einkaufsliste.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory stand-in for the learned-category table. */
class FakeCategoryOverrideDao : CategoryOverrideDao {

    private val state = MutableStateFlow<List<CategoryOverride>>(emptyList())

    val rows: List<CategoryOverride> get() = state.value

    override fun observeAll(): Flow<List<CategoryOverride>> = state

    override suspend fun upsert(override: CategoryOverride) {
        state.value = state.value.filterNot { it.nameKey == override.nameKey } + override
    }

    override suspend fun delete(nameKey: String) {
        state.value = state.value.filterNot { it.nameKey == nameKey }
    }
}
