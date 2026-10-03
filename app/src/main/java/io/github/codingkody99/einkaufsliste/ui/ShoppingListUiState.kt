package io.github.codingkody99.einkaufsliste.ui

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.Recipe
import io.github.codingkody99.einkaufsliste.data.ShoppingItem
import io.github.codingkody99.einkaufsliste.data.ShoppingList
import io.github.codingkody99.einkaufsliste.domain.ParsedEntry
import io.github.codingkody99.einkaufsliste.domain.ShoppingListRow
import io.github.codingkody99.einkaufsliste.domain.UrlDetector

data class ShoppingListUiState(
    val rows: List<ShoppingListRow> = emptyList(),
    val openCount: Int = 0,
    val checkedCount: Int = 0,
    val isLoading: Boolean = true,
) {
    val showEmptyState: Boolean get() = !isLoading && rows.isEmpty()
}

/**
 * The "share with another phone" sheet.
 *
 * [householdCode] is both the invitation and the address of the shared data, so
 * it is only ever shown to someone who is already in the household.
 */
data class SharingState(
    val visible: Boolean = false,
    val householdCode: String? = null,
    /** What has been typed into the join field. */
    val joinCode: String = "",
    val joinFailed: Boolean = false,
) {
    val isShared: Boolean get() = !householdCode.isNullOrBlank()
    val canJoin: Boolean get() = joinCode.isNotBlank()
}

/** One list in the switcher, with how full it is. */
data class ListSummary(
    val list: ShoppingList,
    val openCount: Int,
    val totalCount: Int,
    val isCurrent: Boolean,
    val isMain: Boolean,
) {
    val name: String get() = list.name
    val id: Long get() = list.id
}

/** The name prompt used for both creating and renaming a list. */
data class NameDialogState(
    /** Null when creating a new list. */
    val target: ShoppingList?,
    val name: String,
) {
    val isRename: Boolean get() = target != null
    val canSave: Boolean get() = name.isNotBlank()
}

data class SwitcherState(
    val visible: Boolean = false,
    val nameDialog: NameDialogState? = null,
)

/**
 * The always-visible field at the bottom of the screen. Typing a name and
 * pressing enter is the whole interaction; the category comes from the
 * classifier and is shown so it can be corrected before or after adding.
 */
data class QuickAddState(
    val text: String = "",
    val suggested: Category = Category.DEFAULT,
    val recognized: Boolean = false,
) {
    val canAdd: Boolean get() = text.isNotBlank()
}

/**
 * The recipe editor, used for both new and existing recipes. Ingredients live
 * as text so the same field takes typing, pasting, a link and dictation.
 */
data class RecipeEditorState(
    val visible: Boolean = false,
    val editing: Recipe? = null,
    val name: String = "",
    val ingredientsText: String = "",
    val steps: String = "",
    val sourceUrl: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val isEditing: Boolean get() = editing != null
    val canSave: Boolean get() = name.isNotBlank() && ingredientsText.isNotBlank() && !loading

    /** A link in the ingredients field, which can be loaded instead of typed. */
    val detectedUrl: String? get() = UrlDetector.firstUrl(ingredientsText)

    val ingredientCount: Int
        get() = ingredientsText.split('\n').count { it.isNotBlank() }
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
    /** A recipe page is being fetched. */
    val loading: Boolean = false,
    /** Why the last attempt failed, phrased for the user. */
    val error: String? = null,
    /** Name of the recipe the rows came from, when they came from a link. */
    val sourceTitle: String? = null,
) {
    val analyzed: Boolean get() = rows != null
    val canAnalyze: Boolean get() = text.isNotBlank() && !loading

    /** The link in the pasted text, if there is one. */
    val detectedUrl: String? get() = UrlDetector.firstUrl(text)

    /**
     * True when the text is essentially just a link, so loading the recipe is
     * the obvious action rather than parsing the text as a list.
     */
    val looksLikeOnlyUrl: Boolean
        get() = detectedUrl != null && UrlDetector.withoutUrls(text).length <= MAX_TRAILING_TEXT
    val selectedRows: List<ImportRow> get() = rows?.filter { it.selected }.orEmpty()
    val canApply: Boolean get() = selectedRows.isNotEmpty()

    /** Rows that will be added, in supermarket order. */
    val previewByCategory: List<Pair<Category, List<ImportRow>>>
        get() = selectedRows
            .groupBy { it.category }
            .toList()
            .sortedBy { (category, _) -> category.ordinal }

    val skippedRows: List<ImportRow> get() = rows?.filterNot { it.selected }.orEmpty()

    private companion object {
        /** A shared link usually carries the page title alongside it. */
        const val MAX_TRAILING_TEXT = 120
    }
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
