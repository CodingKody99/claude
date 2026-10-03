package io.github.codingkody99.einkaufsliste.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_items",
    indices = [Index("list_id")],
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /**
     * Which list this belongs to. The default matches the list created for
     * items that existed before the app had more than one.
     */
    @ColumnInfo(name = "list_id", defaultValue = "1")
    val listId: Long = ShoppingList.DEFAULT_ID,
    val name: String,
    /** Free text so "2", "500 g" and "zwei Packungen" are all valid. */
    val quantity: String = "",
    val category: Category = Category.DEFAULT,
    @ColumnInfo(name = "is_checked")
    val isChecked: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
)
