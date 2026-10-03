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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Creates or edits a recipe. One text field takes the ingredients however they
 * arrive — typed, pasted, loaded from a link or dictated — because they all end
 * up as the same lines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorSheet(
    state: RecipeEditorState,
    onNameChange: (String) -> Unit,
    onIngredientsChange: (String) -> Unit,
    onDictated: (String) -> Unit,
    onLoadLink: () -> Unit,
    onSave: () -> Unit,
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
                text = if (state.isEditing) "Rezept bearbeiten" else "Neues Rezept",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Zutaten eintippen, einen Rezept-Link einfügen oder diktieren — " +
                    "zum Beispiel „Ich will Sommerrollen kochen, dafür brauche ich " +
                    "Reisnudeln, Karotte, Gurke, Tofu“.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Name des Rezepts") },
                placeholder = { Text("z. B. Sommerrollen") },
                singleLine = true,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.ingredientsText,
                onValueChange = onIngredientsChange,
                label = { Text("Zutaten — eine pro Zeile") },
                placeholder = {
                    Text(
                        text = "250 g Mehl\n3 Eier\n500 ml Milch\n\n" +
                            "oder einen Link einfügen",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                enabled = !state.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            state.error?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(12.dp))

            if (state.loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Rezept wird geladen …", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DictateButton(
                        onText = onDictated,
                        prompt = "Sag, was in das Rezept gehört",
                    )
                    if (state.detectedUrl != null) {
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = onLoadLink) { Text("Link laden") }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSave, enabled = state.canSave) {
                    Text(if (state.isEditing) "Speichern" else "Rezept speichern")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private const val SHEET_HEIGHT_FRACTION = 0.92f
