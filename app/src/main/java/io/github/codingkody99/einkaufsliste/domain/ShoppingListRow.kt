package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.ShoppingItem

/** A single line in the rendered list: either a section header or an item. */
sealed interface ShoppingListRow {

    /** Stable key for `LazyColumn`, so rows keep their identity across updates. */
    val key: String

    data class CategoryHeader(val category: Category, val openCount: Int) : ShoppingListRow {
        override val key: String get() = "header-${category.name}"
    }

    data class DoneHeader(val count: Int) : ShoppingListRow {
        override val key: String get() = "header-done"
    }

    data class Entry(val item: ShoppingItem) : ShoppingListRow {
        override val key: String get() = "item-${item.id}"
    }
}
