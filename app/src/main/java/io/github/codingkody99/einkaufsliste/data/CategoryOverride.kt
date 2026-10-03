package io.github.codingkody99.einkaufsliste.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A category the user corrected by hand. Keyed by the normalized item name, so
 * the correction also applies to the plural and to different spellings.
 */
@Entity(tableName = "category_overrides")
data class CategoryOverride(
    @PrimaryKey
    @ColumnInfo(name = "name_key")
    val nameKey: String,
    val category: Category,
)
