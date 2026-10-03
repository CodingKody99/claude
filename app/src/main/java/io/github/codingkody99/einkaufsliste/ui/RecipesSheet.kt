package io.github.codingkody99.einkaufsliste.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import io.github.codingkody99.einkaufsliste.data.Recipe

/**
 * The saved recipes. Tapping one goes straight to the import preview with its
 * ingredients, so putting a recipe on the shopping list is one tap plus the
 * usual confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesSheet(
    recipes: List<Recipe>,
    onUse: (Recipe) -> Unit,
    onCreate: () -> Unit,
    onEdit: (Recipe) -> Unit,
    onDelete: (Recipe) -> Unit,
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
                text = "🍳  Rezepte",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
            )
            Text(
                text = "Antippen, um die Zutaten auf die Einkaufsliste zu übernehmen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )

            if (recipes.isEmpty()) {
                Text(
                    text = "Noch keine Rezepte gespeichert. Du kannst eines eintippen, " +
                        "einen Link einfügen oder es einfach diktieren.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            } else {
                recipes.forEach { recipe ->
                    RecipeRow(
                        recipe = recipe,
                        onUse = { onUse(recipe) },
                        onEdit = { onEdit(recipe) },
                        onDelete = { onDelete(recipe) },
                    )
                }
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
                Text("Neues Rezept", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecipeRow(
    recipe: Recipe,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUse)
            .padding(start = 24.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitleFor(recipe),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Rezept „${recipe.name}“ bearbeiten")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Bearbeiten") },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("Rezept löschen") },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

private fun subtitleFor(recipe: Recipe): String {
    val count = recipe.ingredientLines.size
    val amount = if (count == 1) "1 Zutat" else "$count Zutaten"
    val host = recipe.sourceUrl?.let { hostOf(it) }
    return if (host == null) amount else "$amount · $host"
}

/** Just the host, so a long link does not push the ingredient count off screen. */
private fun hostOf(url: String): String? =
    runCatching { java.net.URL(url).host }.getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
