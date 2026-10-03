package io.github.codingkody99.einkaufsliste.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved recipe.
 *
 * The ingredients are kept as the lines they arrived in rather than as parsed
 * rows. Putting them back through the parser when they are used means they pick
 * up whatever the app has learned since — a recipe saved today files itself
 * better next month — and it keeps the original wording intact.
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Where it came from, when it was imported from a link. */
    @ColumnInfo(name = "source_url")
    val sourceUrl: String? = null,
    /** One ingredient per line, exactly as written or dictated. */
    @ColumnInfo(name = "ingredients_text")
    val ingredientsText: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
) {
    /** Non-empty ingredient lines, for counting and display. */
    val ingredientLines: List<String>
        get() = ingredientsText.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
}
