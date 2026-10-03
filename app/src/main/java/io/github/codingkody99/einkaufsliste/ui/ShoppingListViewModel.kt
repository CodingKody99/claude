package io.github.codingkody99.einkaufsliste.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.FetchResult
import io.github.codingkody99.einkaufsliste.data.HttpRecipeFetcher
import io.github.codingkody99.einkaufsliste.data.NewItem
import io.github.codingkody99.einkaufsliste.data.RecipeFetcher
import io.github.codingkody99.einkaufsliste.data.ShoppingItem
import io.github.codingkody99.einkaufsliste.data.ShoppingRepository
import io.github.codingkody99.einkaufsliste.domain.CategoryClassifier
import io.github.codingkody99.einkaufsliste.domain.RecipeExtractor
import io.github.codingkody99.einkaufsliste.domain.ShoppingListGrouper
import io.github.codingkody99.einkaufsliste.domain.ShoppingListParser
import io.github.codingkody99.einkaufsliste.domain.TextNormalizer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val repository: ShoppingRepository,
    private val classifier: CategoryClassifier = CategoryClassifier(),
    private val parser: ShoppingListParser = ShoppingListParser(classifier),
    private val recipeFetcher: RecipeFetcher = HttpRecipeFetcher(),
    private val recipeExtractor: RecipeExtractor = RecipeExtractor(),
) : ViewModel() {

    val uiState: StateFlow<ShoppingListUiState> = repository.observeItems()
        .map { items ->
            ShoppingListUiState(
                rows = ShoppingListGrouper.group(items),
                openCount = items.count { !it.isChecked },
                checkedCount = items.count { it.isChecked },
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShoppingListUiState())

    /**
     * Collected eagerly because the editor and the import preview classify on
     * every keystroke and must not fall back to the bare lexicon meanwhile.
     */
    private val overrides: StateFlow<Map<String, Category>> = repository.observeOverrides()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    private val _import = MutableStateFlow(ImportState())
    val import: StateFlow<ImportState> = _import.asStateFlow()

    private val _notices = MutableSharedFlow<Notice>(extraBufferCapacity = 4)
    val notices: SharedFlow<Notice> = _notices

    private var noticeCounter = 0L

    // --- single item editor ----------------------------------------------------

    fun openAddEditor() {
        _editor.value = EditorState(visible = true)
    }

    fun openEditEditor(item: ShoppingItem) {
        val match = classifier.match(item.name, overrides.value)
        _editor.value = EditorState(
            visible = true,
            editing = item,
            name = item.name,
            quantity = item.quantity,
            category = item.category,
            suggested = match?.category ?: Category.DEFAULT,
            recognized = match != null,
            categoryTouched = match?.category != item.category,
        )
    }

    fun dismissEditor() {
        _editor.value = EditorState()
    }

    /** Re-classifies as the user types, unless they already picked a category. */
    fun onNameChange(value: String) = _editor.update { state ->
        val match = classifier.match(value, overrides.value)
        val suggested = match?.category ?: Category.DEFAULT
        state.copy(
            name = value,
            suggested = suggested,
            recognized = match != null,
            category = if (state.categoryTouched) state.category else suggested,
        )
    }

    fun onQuantityChange(value: String) = _editor.update { it.copy(quantity = value) }

    fun onCategoryChange(value: Category) = _editor.update {
        it.copy(category = value, categoryTouched = true)
    }

    /** Saves the editor contents and closes the sheet. No-op on a blank name. */
    fun save() {
        val state = _editor.value
        if (!state.canSave) return
        val editing = state.editing
        viewModelScope.launch {
            if (state.teachesSomething) {
                repository.rememberCategory(state.name, state.category)
            }
            if (editing == null) {
                repository.add(NewItem(state.name, state.quantity, state.category))
            } else {
                repository.update(editing, state.name, state.quantity, state.category)
            }
        }
        dismissEditor()
    }

    // --- bulk import ----------------------------------------------------------

    fun openImport() {
        _import.value = ImportState(visible = true)
    }

    fun dismissImport() {
        _import.value = ImportState()
    }

    /** Any edit invalidates the preview so it can never be stale. */
    fun onImportTextChange(value: String) {
        _import.value = _import.value.copy(
            text = value,
            rows = null,
            error = null,
            sourceTitle = null,
        )
    }

    fun analyzeImport() {
        val state = _import.value
        if (!state.canAnalyze) return
        viewModelScope.launch {
            _import.update { it.copy(rows = buildRows(state.text), error = null, sourceTitle = null) }
        }
    }

    /**
     * Loads the recipe behind the pasted link and treats its ingredient list
     * exactly like pasted text, so amounts, categories, duplicates and the
     * preview all work the same way.
     */
    fun loadRecipe() {
        val url = _import.value.detectedUrl ?: return
        if (_import.value.loading) return
        _import.update { it.copy(loading = true, error = null, rows = null, sourceTitle = null) }

        viewModelScope.launch {
            when (val result = recipeFetcher.fetch(url)) {
                is FetchResult.Failure ->
                    _import.update { it.copy(loading = false, error = result.reason) }

                is FetchResult.Success -> {
                    val recipe = recipeExtractor.extract(result.html)
                    if (recipe == null || recipe.ingredients.isEmpty()) {
                        _import.update {
                            it.copy(
                                loading = false,
                                error = "Auf dieser Seite wurden keine Zutaten gefunden. " +
                                    "Du kannst die Zutatenliste stattdessen kopieren und hier einfügen.",
                            )
                        }
                    } else {
                        val rows = buildRows(recipe.ingredients.joinToString("\n"))
                        _import.update {
                            it.copy(
                                loading = false,
                                rows = rows,
                                sourceTitle = recipe.title.takeIf { title -> title.isNotBlank() },
                            )
                        }
                    }
                }
            }
        }
    }

    /** Parses text into preview rows, marking what is already on the list. */
    private suspend fun buildRows(text: String): List<ImportRow> {
        val existing = repository.observeItems().first()
            .filterNot { it.isChecked }
            .map { TextNormalizer.normalize(it.name) }
            .toSet()

        return parser.parse(text, overrides.value).mapIndexed { index, entry ->
            val duplicate = TextNormalizer.normalize(entry.name) in existing
            ImportRow(
                id = index,
                entry = entry,
                category = entry.category,
                // Captions and things already on the list start unchecked;
                // the user can still include them.
                selected = !entry.isHeading && !duplicate,
                duplicate = duplicate,
            )
        }
    }

    fun toggleImportRow(id: Int) = _import.update { state ->
        state.copy(
            rows = state.rows?.map { if (it.id == id) it.copy(selected = !it.selected) else it },
        )
    }

    fun setImportRowCategory(id: Int, category: Category) = _import.update { state ->
        state.copy(
            rows = state.rows?.map {
                if (it.id == id) it.copy(category = category, selected = true) else it
            },
        )
    }

    fun setAllImportRowsSelected(selected: Boolean) = _import.update { state ->
        state.copy(rows = state.rows?.map { it.copy(selected = selected) })
    }

    fun applyImport() {
        val rows = _import.value.selectedRows
        if (rows.isEmpty()) return
        viewModelScope.launch {
            // A category the user moved by hand is worth remembering for next time.
            rows.filter { it.recategorized }
                .forEach { repository.rememberCategory(it.name, it.category) }

            val ids = repository.addAll(
                rows.map { NewItem(name = it.name, quantity = it.quantity, category = it.category) },
            )
            emitNotice(
                text = if (ids.size == 1) "1 Artikel übernommen" else "${ids.size} Artikel übernommen",
                undo = UndoAction.Remove(ids),
            )
        }
        dismissImport()
    }

    // --- list actions ---------------------------------------------------------

    fun toggleChecked(item: ShoppingItem) {
        viewModelScope.launch { repository.setChecked(item.id, !item.isChecked) }
    }

    fun delete(item: ShoppingItem) {
        viewModelScope.launch {
            repository.delete(item.id)
            emitNotice("„${item.name}“ gelöscht", UndoAction.Restore(listOf(item)))
        }
    }

    fun deleteChecked() {
        viewModelScope.launch {
            val removed = repository.observeItems().first().filter { it.isChecked }
            if (removed.isEmpty()) return@launch
            repository.deleteChecked()
            emitNotice("${removed.size} erledigte entfernt", UndoAction.Restore(removed))
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            val removed = repository.observeItems().first()
            if (removed.isEmpty()) return@launch
            repository.deleteAll()
            emitNotice("Liste geleert (${removed.size})", UndoAction.Restore(removed))
        }
    }

    fun undo(notice: Notice) {
        val action = notice.undo ?: return
        viewModelScope.launch {
            when (action) {
                is UndoAction.Restore -> repository.restore(action.items)
                is UndoAction.Remove -> action.ids.forEach { repository.delete(it) }
            }
        }
    }

    private suspend fun emitNotice(text: String, undo: UndoAction?) {
        _notices.emit(Notice(text = text, undo = undo, id = ++noticeCounter))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
