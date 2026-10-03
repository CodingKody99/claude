package io.github.codingkody99.einkaufsliste.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.ShoppingItem
import io.github.codingkody99.einkaufsliste.data.ShoppingRepository
import io.github.codingkody99.einkaufsliste.domain.ShoppingListGrouper
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ShoppingListUiState(),
        )

    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    private val _undoMessages = MutableSharedFlow<UndoMessage>(extraBufferCapacity = 4)
    val undoMessages: SharedFlow<UndoMessage> = _undoMessages

    private var undoCounter = 0L

    // --- editor ---------------------------------------------------------------

    fun openAddEditor() {
        _editor.value = EditorState(visible = true)
    }

    fun openEditEditor(item: ShoppingItem) {
        _editor.value = EditorState(
            visible = true,
            editing = item,
            name = item.name,
            quantity = item.quantity,
            category = item.category,
        )
    }

    fun dismissEditor() {
        _editor.value = EditorState()
    }

    fun onNameChange(value: String) = _editor.update { it.copy(name = value) }

    fun onQuantityChange(value: String) = _editor.update { it.copy(quantity = value) }

    fun onCategoryChange(value: Category) = _editor.update { it.copy(category = value) }

    /** Saves the editor contents, then closes the sheet. No-op on a blank name. */
    fun save() {
        val state = _editor.value
        if (!state.canSave) return
        val editing = state.editing
        viewModelScope.launch {
            if (editing == null) {
                repository.add(state.name, state.quantity, state.category)
            } else {
                repository.rename(editing, state.name, state.quantity, state.category)
            }
        }
        dismissEditor()
    }

    // --- list actions ---------------------------------------------------------

    fun toggleChecked(item: ShoppingItem) {
        viewModelScope.launch { repository.setChecked(item.id, !item.isChecked) }
    }

    fun delete(item: ShoppingItem) {
        viewModelScope.launch {
            repository.delete(item.id)
            emitUndo("„${item.name}“ gelöscht", listOf(item))
        }
    }

    fun deleteChecked() {
        viewModelScope.launch {
            val removed = repository.observeItems().first().filter { it.isChecked }
            if (removed.isEmpty()) return@launch
            repository.deleteChecked()
            emitUndo("${removed.size} erledigte entfernt", removed)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            val removed = repository.observeItems().first()
            if (removed.isEmpty()) return@launch
            repository.deleteAll()
            emitUndo("Liste geleert (${removed.size})", removed)
        }
    }

    fun undo(message: UndoMessage) {
        viewModelScope.launch { repository.restore(message.items) }
    }

    private suspend fun emitUndo(text: String, items: List<ShoppingItem>) {
        _undoMessages.emit(UndoMessage(text = text, items = items, id = ++undoCounter))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
