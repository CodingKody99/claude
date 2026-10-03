package io.github.codingkody99.einkaufsliste.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.github.codingkody99.einkaufsliste.data.ShoppingList

/**
 * Picks, creates, renames and removes lists.
 *
 * A sheet rather than a dropdown: the rows can be full width with generous
 * touch targets, which matters more here than saving a tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSwitcherSheet(
    lists: List<ListSummary>,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
    onRename: (ShoppingList) -> Unit,
    onDelete: (ListSummary) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Listen",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )

            lists.forEach { summary ->
                ListRow(
                    summary = summary,
                    canDelete = lists.size > 1,
                    onSelect = { onSelect(summary.id) },
                    onRename = { onRename(summary.list) },
                    onDelete = { onDelete(summary) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            TextButton(
                onClick = onCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Neue Liste", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ListRow(
    summary: ListSummary,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 24.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (summary.isCurrent) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "aktuelle Liste",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
        } else {
            Spacer(Modifier.size(26.dp))
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitleFor(summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Liste „${summary.name}“ bearbeiten")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Umbenennen") },
                onClick = {
                    menuOpen = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Liste löschen") },
                enabled = canDelete,
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

private fun subtitleFor(summary: ListSummary): String = when {
    summary.totalCount == 0 -> if (summary.isMain) "leer · Hauptliste" else "leer"
    summary.isMain -> "${summary.openCount} offen von ${summary.totalCount} · Hauptliste"
    else -> "${summary.openCount} offen von ${summary.totalCount}"
}

/** Used for both "new list" and "rename list". */
@Composable
fun ListNameDialog(
    state: NameDialogState,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.isRename) "Liste umbenennen" else "Neue Liste") },
        text = {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                singleLine = true,
                label = { Text("Name") },
                placeholder = { Text("z. B. Drogerie") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = state.canSave) {
                Text(if (state.isRename) "Speichern" else "Anlegen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
