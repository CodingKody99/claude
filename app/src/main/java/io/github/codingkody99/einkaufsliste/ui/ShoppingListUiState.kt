package io.github.codingkody99.einkaufsliste.ui

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.ShoppingItem
import io.github.codingkody99.einkaufsliste.domain.ShoppingListRow

data class ShoppingListUiState(
    val rows: List<ShoppingListRow> = emptyList(),
    val openCount: Int = 0,
    val checkedCount: Int = 0,
    val isLoading: Boolean = true,
) {
    val showEmptyState: Boolean get() = !isLoading && rows.isEmpty()
}

/** State of the bottom sheet used for both adding and editing an item. */
data class EditorState(
    val visible: Boolean = false,
    val editing: ShoppingItem? = null,
    val name: String = "",
    val quantity: String = "",
    val category: Category = Category.DEFAULT,
) {
    val canSave: Boolean get() = name.isNotBlank()
    val isEditing: Boolean get() = editing != null
}

/** A one-shot snackbar offering to undo the deletion of [items]. */
data class UndoMessage(
    val text: String,
    val items: List<ShoppingItem>,
    val id: Long,
)
