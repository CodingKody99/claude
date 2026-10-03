package io.github.codingkody99.einkaufsliste.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.codingkody99.einkaufsliste.data.Recipe

/**
 * Reads a saved recipe: what to buy and how it is made, in one scrollable page
 * — which is what you want open while cooking. The shopping list is one button
 * away, and that button leads into the usual preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeViewSheet(
    recipe: Recipe,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(text = recipe.name, style = MaterialTheme.typography.titleLarge)

            recipe.sourceUrl?.let { url ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(text = "Zutaten", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            recipe.ingredientLines.forEach { line ->
                Text(
                    text = "•  $line",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            if (recipe.steps.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text(text = "Ablauf", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text(text = recipe.steps, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) { Text("Bearbeiten") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onUse) { Text("Zutaten auf die Liste") }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
