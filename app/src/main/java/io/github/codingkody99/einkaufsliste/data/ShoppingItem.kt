package io.github.codingkody99.einkaufsliste.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Free text so "2", "500 g" and "zwei Packungen" are all valid. */
    val quantity: String = "",
    val category: Category = Category.DEFAULT,
    @ColumnInfo(name = "is_checked")
    val isChecked: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
)
