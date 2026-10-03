package io.github.codingkody99.einkaufsliste.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.codingkody99.einkaufsliste.data.Category

/**
 * Paste a whole list, see exactly what it will become, then take it over.
 *
 * The preview is the point: free text can never be parsed perfectly, so the
 * user gets to see every line, grouped the way it will appear in the shop, and
 * can drop or re-file anything before a single item is written.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(
    state: ImportState,
    onTextChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onToggleRow: (Int) -> Unit,
    onRowCategoryChange: (Int, Category) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(SHEET_HEIGHT_FRACTION)
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Liste einfügen",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Beliebigen Text einfügen — die Artikel werden erkannt, " +
                    "einsortiert und in Supermarkt-Reihenfolge gebracht.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (state.rows == null) {
                TextStep(
                    text = state.text,
                    canAnalyze = state.canAnalyze,
                    onTextChange = onTextChange,
                    onAnalyze = onAnalyze,
                    onDismiss = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            } else {
                PreviewStep(
                    state = state,
                    onToggleRow = onToggleRow,
                    onRowCategoryChange = onRowCategoryChange,
                    onSelectAll = onSelectAll,
                    onApply = onApply,
                    // Re-sending the text clears the analysis, which returns
                    // to the editing step with everything still typed in.
                    onBack = { onTextChange(state.text) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TextStep(
    text: String,
    canAnalyze: Boolean,
    onTextChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("Text einfügen") },
            placeholder = {
                Text(
                    text = "Salat mit Käse\n2 Tomaten\nFeta\nRucola\n\n" +
                        "Abendessen Freitag: Lachs, Kartoffeln\n500g Hackfleisch",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAnalyze, enabled = canAnalyze) { Text("Analysieren") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PreviewStep(
    state: ImportState,
    onToggleRow: (Int) -> Unit,
    onRowCategoryChange: (Int, Category) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = state.rows?.size ?: 0
    val selected = state.selectedRows.size

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$selected von $total Zeilen",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onSelectAll(true) }) { Text("Alle") }
            TextButton(onClick = { onSelectAll(false) }) { Text("Keine") }
        }

        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            state.previewByCategory.forEach { (category, rows) ->
                item(key = "head-${category.name}") {
                    SectionLabel("${category.emoji}  ${category.label}", rows.size.toString())
                }
                items(rows, key = { "row-${it.id}" }) { row ->
                    ImportRowItem(
                        row = row,
                        onToggle = { onToggleRow(row.id) },
                        onCategoryChange = { onRowCategoryChange(row.id, it) },
                    )
                }
            }

            if (state.skippedRows.isNotEmpty()) {
                item(key = "head-skipped") {
                    SectionLabel("Nicht übernommen", state.skippedRows.size.toString())
                }
                items(state.skippedRows, key = { "skip-${it.id}" }) { row ->
                    ImportRowItem(
                        row = row,
                        onToggle = { onToggleRow(row.id) },
                        onCategoryChange = { onRowCategoryChange(row.id, it) },
                    )
                }
            }
        }

        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onBack) { Text("Text ändern") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onApply, enabled = state.canApply) {
                Text(if (selected == 1) "1 Artikel übernehmen" else "$selected Artikel übernehmen")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(title: String, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
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
}

@Composable
private fun ImportRowItem(
    row: ImportRow,
    onToggle: () -> Unit,
    onCategoryChange: (Category) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = row.selected, onCheckedChange = { onToggle() })

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (row.quantity.isBlank()) row.name else "${row.quantity} · ${row.name}",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            hintFor(row)?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        CategoryPicker(selected = row.category, onSelect = onCategoryChange)
    }
}

private fun hintFor(row: ImportRow): String? = when {
    row.isHeading -> "als Überschrift erkannt"
    row.duplicate -> "steht schon auf der Liste"
    row.uncertain -> "Kategorie unsicher — bitte prüfen"
    row.recategorized -> "von dir geändert, wird gemerkt"
    else -> null
}

@Composable
private fun CategoryPicker(selected: Category, onSelect: (Category) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    TextButton(onClick = { expanded = true }) {
        Text(selected.emoji, style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Kategorie ändern")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        Category.entries.forEach { category ->
            DropdownMenuItem(
                text = { Text("${category.emoji}  ${category.label}") },
                onClick = {
                    expanded = false
                    onSelect(category)
                },
            )
        }
    }
}

private const val SHEET_HEIGHT_FRACTION = 0.92f
