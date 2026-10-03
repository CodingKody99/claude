package io.github.codingkody99.einkaufsliste.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.ShoppingItem
import io.github.codingkody99.einkaufsliste.domain.ShoppingListGrouper
import io.github.codingkody99.einkaufsliste.domain.ShoppingListRow
import io.github.codingkody99.einkaufsliste.ui.theme.EinkaufslisteTheme
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun ShoppingListRoute(
    viewModel: ShoppingListViewModel = viewModel(factory = ShoppingListViewModelFactory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val importState by viewModel.import.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    NoticeSnackbarEffect(
        notices = viewModel.notices,
        snackbarHostState = snackbarHostState,
        onUndo = viewModel::undo,
    )

    ShoppingListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onPasteClick = viewModel::openImport,
        onAddClick = viewModel::openAddEditor,
        onItemClick = viewModel::openEditEditor,
        onToggle = viewModel::toggleChecked,
        onDelete = viewModel::delete,
        onDeleteChecked = viewModel::deleteChecked,
        onDeleteAll = viewModel::deleteAll,
    )

    if (editor.visible) {
        ItemEditorSheet(
            state = editor,
            onNameChange = viewModel::onNameChange,
            onQuantityChange = viewModel::onQuantityChange,
            onCategoryChange = viewModel::onCategoryChange,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissEditor,
        )
    }

    if (importState.visible) {
        ImportSheet(
            state = importState,
            onTextChange = viewModel::onImportTextChange,
            onAnalyze = viewModel::analyzeImport,
            onLoadRecipe = viewModel::loadRecipe,
            onToggleRow = viewModel::toggleImportRow,
            onRowCategoryChange = viewModel::setImportRowCategory,
            onSelectAll = viewModel::setAllImportRowsSelected,
            onApply = viewModel::applyImport,
            onDismiss = viewModel::dismissImport,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    uiState: ShoppingListUiState,
    snackbarHostState: SnackbarHostState,
    onPasteClick: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (ShoppingItem) -> Unit,
    onToggle: (ShoppingItem) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    onDeleteChecked: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    var confirmClearAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Einkaufsliste")
                        Text(
                            text = subtitleFor(uiState),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                actions = {
                    ListMenu(
                        checkedCount = uiState.checkedCount,
                        totalCount = uiState.openCount + uiState.checkedCount,
                        onDeleteChecked = onDeleteChecked,
                        onClearAllRequest = { confirmClearAll = true },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Hinzufügen") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PasteField(onClick = onPasteClick)

            // weight, not fillMaxSize: the paste field above already took height.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (uiState.showEmptyState) {
                    EmptyState(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Leaves room for the FAB so the last row stays reachable.
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(items = uiState.rows, key = { it.key }) { row ->
                            when (row) {
                                is ShoppingListRow.CategoryHeader -> SectionHeader(
                                    title = "${row.category.emoji}  ${row.category.label}",
                                    trailing = row.openCount.toString(),
                                )

                                is ShoppingListRow.DoneHeader -> SectionHeader(
                                    title = "✓  Erledigt",
                                    trailing = row.count.toString(),
                                )

                                is ShoppingListRow.Entry -> ItemRow(
                                    item = row.item,
                                    onClick = { onItemClick(row.item) },
                                    onToggle = { onToggle(row.item) },
                                    onDelete = { onDelete(row.item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Liste leeren?") },
            text = { Text("Alle Einträge werden entfernt. Das lässt sich direkt danach rückgängig machen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearAll = false
                        onDeleteAll()
                    },
                ) { Text("Leeren") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Abbrechen") }
            },
        )
    }
}

/**
 * The entry point for pasting a whole list. Looks like a text field and opens
 * the import sheet, where there is room for a real multi-line editor.
 */
@Composable
private fun PasteField(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Ganze Liste einfügen",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "wird erkannt und nach Supermarkt sortiert",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun subtitleFor(uiState: ShoppingListUiState): String = when {
    uiState.isLoading -> "lädt …"
    uiState.openCount == 0 && uiState.checkedCount == 0 -> "noch nichts drauf"
    uiState.openCount == 0 -> "alles erledigt 🎉"
    else -> "${uiState.openCount} offen · ${uiState.checkedCount} erledigt"
}

@Composable
private fun ListMenu(
    checkedCount: Int,
    totalCount: Int,
    onDeleteChecked: () -> Unit,
    onClearAllRequest: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "Weitere Aktionen")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Erledigte entfernen") },
            enabled = checkedCount > 0,
            onClick = {
                expanded = false
                onDeleteChecked()
            },
        )
        DropdownMenuItem(
            text = { Text("Liste leeren") },
            enabled = totalCount > 0,
            onClick = {
                expanded = false
                onClearAllRequest()
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ItemRow(
    item: ShoppingItem,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = { onToggle() })

        // Tapping the label opens the editor; the checkbox keeps its own target.
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                color = if (item.isChecked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (item.quantity.isNotBlank()) {
                Text(
                    text = item.quantity,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "„${item.name}“ löschen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(text = "Liste ist leer", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Oben eine ganze Liste einfügen, oder unten einzelne Artikel hinzufügen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoticeSnackbarEffect(
    notices: SharedFlow<Notice>,
    snackbarHostState: SnackbarHostState,
    onUndo: (Notice) -> Unit,
) {
    LaunchedEffect(notices, snackbarHostState) {
        notices.collect { notice ->
            val result = snackbarHostState.showSnackbar(
                message = notice.text,
                actionLabel = if (notice.canUndo) "Rückgängig" else null,
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo(notice)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListScreenPreview() {
    val items = listOf(
        ShoppingItem(id = 1, name = "Tomaten", quantity = "500 g", category = Category.OBST_GEMUESE),
        ShoppingItem(id = 2, name = "Vollkornbrot", category = Category.BACKWAREN),
        ShoppingItem(id = 3, name = "Feta", category = Category.MOLKEREI),
        ShoppingItem(id = 4, name = "Hackfleisch", quantity = "500 g", category = Category.FLEISCH_FISCH),
        ShoppingItem(id = 5, name = "Spülmittel", category = Category.HAUSHALT, isChecked = true),
    )
    EinkaufslisteTheme(dynamicColor = false) {
        ShoppingListScreen(
            uiState = ShoppingListUiState(
                rows = ShoppingListGrouper.group(items),
                openCount = 4,
                checkedCount = 1,
                isLoading = false,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onPasteClick = {},
            onAddClick = {},
            onItemClick = {},
            onToggle = {},
            onDelete = {},
            onDeleteChecked = {},
            onDeleteAll = {},
        )
    }
}
