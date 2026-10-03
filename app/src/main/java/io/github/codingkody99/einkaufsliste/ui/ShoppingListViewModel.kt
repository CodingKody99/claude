package io.github.codingkody99.einkaufsliste.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.FetchResult
import io.github.codingkody99.einkaufsliste.data.HttpRecipeFetcher
import io.github.codingkody99.einkaufsliste.data.NewItem
import io.github.codingkody99.einkaufsliste.data.Recipe
import io.github.codingkody99.einkaufsliste.data.RecipeFetcher
import io.github.codingkody99.einkaufsliste.data.ShoppingItem
import io.github.codingkody99.einkaufsliste.data.ShoppingList
import io.github.codingkody99.einkaufsliste.data.ShoppingRepository
import io.github.codingkody99.einkaufsliste.domain.CategoryClassifier
import io.github.codingkody99.einkaufsliste.domain.RecipeExtractor
import io.github.codingkody99.einkaufsliste.domain.ShoppingListGrouper
import io.github.codingkody99.einkaufsliste.domain.ShoppingListParser
import io.github.codingkody99.einkaufsliste.domain.TextNormalizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModel(
    private val repository: ShoppingRepository,
    private val classifier: CategoryClassifier = CategoryClassifier(),
    private val parser: ShoppingListParser = ShoppingListParser(classifier),
    private val recipeFetcher: RecipeFetcher = HttpRecipeFetcher(),
    private val recipeExtractor: RecipeExtractor = RecipeExtractor(),
) : ViewModel() {

    /**
     * Deliberately not persisted: opening the app always lands on the main list,
     * so switching lists never leaves you somewhere you forgot you were.
     */
    private val _selectedListId = MutableStateFlow<Long?>(null)
    val selectedListId: StateFlow<Long?> = _selectedListId.asStateFlow()

    init {
        viewModelScope.launch { _selectedListId.value = repository.ensureMainList() }
    }

    val uiState: StateFlow<ShoppingListUiState> = _selectedListId
        .flatMapLatest { listId ->
            if (listId == null) {
                flowOf(ShoppingListUiState())
            } else {
                repository.observeItems(listId).map { items ->
                    ShoppingListUiState(
                        rows = ShoppingListGrouper.group(items),
                        openCount = items.count { !it.isChecked },
                        checkedCount = items.count { it.isChecked },
                        isLoading = false,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShoppingListUiState())

    val lists: StateFlow<List<ListSummary>> = combine(
        repository.observeLists(),
        repository.observeListCounts(),
        _selectedListId,
    ) { lists, counts, currentId ->
        lists.mapIndexed { index, list ->
            ListSummary(
                list = list,
                openCount = counts[list.id]?.openCount ?: 0,
                totalCount = counts[list.id]?.totalCount ?: 0,
                isCurrent = list.id == currentId,
                isMain = index == 0,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    /**
     * Collected eagerly because the quick-add field, the editor and the import
     * preview classify on every keystroke and must not fall back to the bare
     * lexicon meanwhile.
     */
    private val overrides: StateFlow<Map<String, Category>> = repository.observeOverrides()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val recipes: StateFlow<List<Recipe>> = repository.observeRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _recipesVisible = MutableStateFlow(false)
    val recipesVisible: StateFlow<Boolean> = _recipesVisible.asStateFlow()

    private val _viewedRecipe = MutableStateFlow<Recipe?>(null)
    val viewedRecipe: StateFlow<Recipe?> = _viewedRecipe.asStateFlow()

    private val _recipeEditor = MutableStateFlow(RecipeEditorState())
    val recipeEditor: StateFlow<RecipeEditorState> = _recipeEditor.asStateFlow()

    private val _switcher = MutableStateFlow(SwitcherState())
    val switcher: StateFlow<SwitcherState> = _switcher.asStateFlow()

    private val _quickAdd = MutableStateFlow(QuickAddState())
    val quickAdd: StateFlow<QuickAddState> = _quickAdd.asStateFlow()

    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    private val _import = MutableStateFlow(ImportState())
    val import: StateFlow<ImportState> = _import.asStateFlow()

    private val _notices = MutableSharedFlow<Notice>(extraBufferCapacity = 4)
    val notices: SharedFlow<Notice> = _notices

    private var noticeCounter = 0L

    // --- lists ----------------------------------------------------------------

    fun openSwitcher() = _switcher.update { it.copy(visible = true) }

    fun dismissSwitcher() {
        _switcher.value = SwitcherState()
    }

    fun selectList(id: Long) {
        _selectedListId.value = id
        dismissSwitcher()
    }

    fun startCreateList() = _switcher.update {
        it.copy(nameDialog = NameDialogState(target = null, name = ""))
    }

    fun startRenameList(list: ShoppingList) = _switcher.update {
        it.copy(nameDialog = NameDialogState(target = list, name = list.name))
    }

    fun onNameDialogChange(value: String) = _switcher.update { state ->
        state.copy(nameDialog = state.nameDialog?.copy(name = value))
    }

    fun dismissNameDialog() = _switcher.update { it.copy(nameDialog = null) }

    fun confirmNameDialog() {
        val dialog = _switcher.value.nameDialog ?: return
        if (!dialog.canSave) return
        val target = dialog.target
        viewModelScope.launch {
            if (target == null) {
                // A newly created list is the one you wanted to work on.
                repository.createList(dialog.name)?.let { _selectedListId.value = it }
            } else {
                repository.renameList(target.id, dialog.name)
            }
        }
        _switcher.update { it.copy(nameDialog = null) }
    }

    fun deleteList(summary: ListSummary) {
        viewModelScope.launch {
            if (!repository.deleteList(summary.id)) {
                emitNotice("Die letzte Liste kann nicht gelöscht werden.", undo = null)
                return@launch
            }
            // Never leave the screen pointing at a list that is gone.
            if (_selectedListId.value == summary.id) {
                _selectedListId.value = repository.ensureMainList()
            }
            emitNotice("Liste „${summary.name}“ gelöscht", undo = null)
        }
    }

    // --- recipes --------------------------------------------------------------

    fun openRecipes() {
        _recipesVisible.value = true
    }

    fun dismissRecipes() {
        _recipesVisible.value = false
    }

    fun startCreateRecipe() {
        _recipeEditor.value = RecipeEditorState(visible = true)
    }

    fun viewRecipe(recipe: Recipe) {
        _viewedRecipe.value = recipe
    }

    fun dismissRecipeView() {
        _viewedRecipe.value = null
    }

    fun startEditRecipe(recipe: Recipe) {
        _viewedRecipe.value = null
        _recipeEditor.value = RecipeEditorState(
            visible = true,
            editing = recipe,
            name = recipe.name,
            ingredientsText = recipe.ingredientsText,
            steps = recipe.steps,
            sourceUrl = recipe.sourceUrl,
        )
    }

    fun dismissRecipeEditor() {
        _recipeEditor.value = RecipeEditorState()
    }

    fun onRecipeNameChange(value: String) = _recipeEditor.update { it.copy(name = value) }

    fun onRecipeIngredientsChange(value: String) = _recipeEditor.update {
        it.copy(ingredientsText = value, error = null)
    }

    fun onRecipeStepsChange(value: String) = _recipeEditor.update {
        it.copy(steps = value, error = null)
    }

    /** Appends scanned or dictated text to the method, keeping what is there. */
    fun onRecipeStepsAppended(text: String) {
        if (text.isBlank()) return
        _recipeEditor.update { it.copy(steps = appendLines(it.steps, listOf(text.trim())), error = null) }
    }

    /**
     * Text from a photo lands in the ingredients field as it was read, line by
     * line, so it can be corrected rather than retyped.
     */
    fun onRecipeIngredientsScanned(text: String) {
        if (text.isBlank()) return
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        _recipeEditor.update {
            it.copy(ingredientsText = appendLines(it.ingredientsText, lines), error = null)
        }
    }

    /**
     * Turns a dictated sentence into a recipe: the dish becomes the name and the
     * rest becomes clean ingredient lines, so "ich will Sommerrollen kochen,
     * dafür brauche ich Reisnudeln, Karotte" needs no tidying up by hand.
     */
    fun onRecipeDictated(spoken: String) {
        if (spoken.isBlank()) return
        val entries = parser.parse(spoken, overrides.value)
        val dish = entries.firstOrNull { it.isHeading }?.name
        val lines = entries.filterNot { it.isHeading }.map { entry ->
            listOf(entry.quantity, entry.name).filter { it.isNotBlank() }.joinToString(" ")
        }
        _recipeEditor.update { state ->
            state.copy(
                name = if (state.name.isBlank() && !dish.isNullOrBlank()) dish else state.name,
                ingredientsText = appendLines(state.ingredientsText, lines),
                error = null,
            )
        }
    }

    /** Fills the editor from the link in its ingredients field. */
    fun loadRecipeIntoEditor() {
        val state = _recipeEditor.value
        val url = state.detectedUrl ?: return
        if (state.loading) return
        _recipeEditor.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            when (val result = recipeFetcher.fetch(url)) {
                is FetchResult.Failure ->
                    _recipeEditor.update { it.copy(loading = false, error = result.reason) }

                is FetchResult.Success -> {
                    val extracted = recipeExtractor.extract(result.html)
                    if (extracted == null || extracted.ingredients.isEmpty()) {
                        _recipeEditor.update {
                            it.copy(
                                loading = false,
                                error = "Auf dieser Seite wurden keine Zutaten gefunden. " +
                                    "Du kannst sie stattdessen eintippen oder diktieren.",
                            )
                        }
                    } else {
                        _recipeEditor.update {
                            it.copy(
                                loading = false,
                                name = it.name.ifBlank { extracted.title },
                                ingredientsText = extracted.ingredients.joinToString("\n"),
                                sourceUrl = url,
                            )
                        }
                    }
                }
            }
        }
    }

    fun saveRecipe() {
        val state = _recipeEditor.value
        if (!state.canSave) return
        val editing = state.editing
        viewModelScope.launch {
            if (editing == null) {
                repository.saveRecipe(
                    name = state.name,
                    ingredientsText = state.ingredientsText,
                    steps = state.steps,
                    sourceUrl = state.sourceUrl,
                )
                emitNotice("Rezept „${state.name.trim()}“ gespeichert", undo = null)
            } else {
                repository.updateRecipe(
                    recipe = editing,
                    name = state.name,
                    ingredientsText = state.ingredientsText,
                    steps = state.steps,
                )
            }
        }
        dismissRecipeEditor()
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe.id)
            emitNotice("Rezept „${recipe.name}“ gelöscht", undo = null)
        }
    }

    /**
     * Opens the import preview filled with a saved recipe, so adding its
     * ingredients to the list goes through exactly the same check and undo.
     */
    fun useRecipe(recipe: Recipe) {
        _viewedRecipe.value = null
        dismissRecipes()
        _import.value = ImportState(visible = true, text = recipe.ingredientsText)
        viewModelScope.launch {
            val rows = buildRows(recipe.ingredientsText)
            _import.update { it.copy(rows = rows, sourceTitle = recipe.name) }
        }
    }

    /** Dictated text is appended, so several bursts can be spoken in a row. */
    fun onImportDictated(spoken: String) {
        if (spoken.isBlank()) return
        onImportTextChange(appendLines(_import.value.text, listOf(spoken.trim())))
    }

    private fun appendLines(existing: String, lines: List<String>): String {
        val wanted = lines.filter { it.isNotBlank() }
        if (wanted.isEmpty()) return existing
        val joined = wanted.joinToString("\n")
        return if (existing.isBlank()) joined else existing.trimEnd() + "\n" + joined
    }

    // --- quick add ------------------------------------------------------------

    fun onQuickAddChange(value: String) = _quickAdd.update {
        val match = classifier.match(value, overrides.value)
        it.copy(
            text = value,
            suggested = match?.category ?: Category.DEFAULT,
            recognized = match != null,
        )
    }

    /** Adds what is typed and clears the field, so the next item can follow. */
    fun submitQuickAdd() {
        val state = _quickAdd.value
        val listId = _selectedListId.value
        if (!state.canAdd || listId == null) return
        viewModelScope.launch {
            repository.add(listId, NewItem(state.text, "", state.suggested))
        }
        _quickAdd.value = QuickAddState()
    }

    /** Hands what is typed over to the full editor, for amount and category. */
    fun expandQuickAdd() {
        val typed = _quickAdd.value.text
        _quickAdd.value = QuickAddState()
        openAddEditor()
        if (typed.isNotBlank()) onNameChange(typed)
    }

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
        val listId = _selectedListId.value
        if (!state.canSave || listId == null) return
        val editing = state.editing
        viewModelScope.launch {
            if (state.teachesSomething) {
                repository.rememberCategory(state.name, state.category)
            }
            if (editing == null) {
                repository.add(listId, NewItem(state.name, state.quantity, state.category))
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

    /** Parses text into preview rows, marking what is already on this list. */
    private suspend fun buildRows(text: String): List<ImportRow> {
        val listId = _selectedListId.value
        val existing = if (listId == null) {
            emptySet()
        } else {
            repository.observeItems(listId).first()
                .filterNot { it.isChecked }
                .map { TextNormalizer.normalize(it.name) }
                .toSet()
        }

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
        val listId = _selectedListId.value
        if (rows.isEmpty() || listId == null) return
        viewModelScope.launch {
            // A category the user moved by hand is worth remembering for next time.
            rows.filter { it.recategorized }
                .forEach { repository.rememberCategory(it.name, it.category) }

            val ids = repository.addAll(
                listId,
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
        val listId = _selectedListId.value ?: return
        viewModelScope.launch {
            val removed = repository.observeItems(listId).first().filter { it.isChecked }
            if (removed.isEmpty()) return@launch
            repository.deleteChecked(listId)
            emitNotice("${removed.size} erledigte entfernt", UndoAction.Restore(removed))
        }
    }

    fun clearList() {
        val listId = _selectedListId.value ?: return
        viewModelScope.launch {
            val removed = repository.observeItems(listId).first()
            if (removed.isEmpty()) return@launch
            repository.clearList(listId)
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
