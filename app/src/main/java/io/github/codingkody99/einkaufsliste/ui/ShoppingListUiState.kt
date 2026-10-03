package io.github.codingkody99.einkaufsliste.ui

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.ShoppingItem
import io.github.codingkody99.einkaufsliste.domain.ParsedEntry
import io.github.codingkody99.einkaufsliste.domain.ShoppingListRow

data class ShoppingListUiState(
    val rows: List<ShoppingListRow> = emptyList(),
    val openCount: Int = 0,
    val checkedCount: Int = 0,
    val isLoading: Boolean = true,
) {
    val showEmptyState: Boolean get() = !isLoading && rows.isEmpty()
}

/** State of the sheet used for both adding and editing a single item. */
data class EditorState(
    val visible: Boolean = false,
    val editing: ShoppingItem? = null,
    val name: String = "",
    val quantity: String = "",
    val category: Category = Category.DEFAULT,
    /** What the classifier suggests for the current name. */
    val suggested: Category = Category.DEFAULT,
    /** False when the name is unknown, so the section is only a guess. */
    val recognized: Boolean = false,
    /** True once the user picked a category by hand. */
    val categoryTouched: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
    val isEditing: Boolean get() = editing != null

    /** The chosen category differs from the suggestion, so it is worth learning. */
    val teachesSomething: Boolean get() = categoryTouched && category != suggested
}

/** One parsed line in the import preview. */
data class ImportRow(
    val id: Int,
    val entry: ParsedEntry,
    val category: Category,
    val selected: Boolean,
    /** The open list already has an item with this name. */
    val duplicate: Boolean,
) {
    val name: String get() = entry.name
    val quantity: String get() = entry.quantity
    val isHeading: Boolean get() = entry.isHeading
    val uncertain: Boolean get() = !entry.recognized && !entry.isHeading

    /** True when the user moved this row away from what was detected. */
    val recategorized: Boolean get() = category != entry.category
}

data class ImportState(
    val visible: Boolean = false,
    val text: String = "",
    /** Null until the text has been analysed; any edit clears it again. */
    val rows: List<ImportRow>? = null,
) {
    val analyzed: Boolean get() = rows != null
    val canAnalyze: Boolean get() = text.isNotBlank()
    val selectedRows: List<ImportRow> get() = rows?.filter { it.selected }.orEmpty()
    val canApply: Boolean get() = selectedRows.isNotEmpty()

    /** Rows that will be added, in supermarket order. */
    val previewByCategory: List<Pair<Category, List<ImportRow>>>
        get() = selectedRows
            .groupBy { it.category }
            .toList()
            .sortedBy { (category, _) -> category.ordinal }

    val skippedRows: List<ImportRow> get() = rows?.filterNot { it.selected }.orEmpty()
}

/** What tapping "Rückgängig" on a notice should do. */
sealed interface UndoAction {
    /** Put deleted items back. */
    data class Restore(val items: List<ShoppingItem>) : UndoAction

    /** Take freshly added items off again. */
    data class Remove(val ids: List<Long>) : UndoAction
}

/** A one-shot snackbar message, optionally undoable. */
data class Notice(
    val text: String,
    val undo: UndoAction?,
    val id: Long,
) {
    val canUndo: Boolean get() = undo != null
}
