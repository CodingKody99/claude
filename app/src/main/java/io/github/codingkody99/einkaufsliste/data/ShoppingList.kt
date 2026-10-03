package io.github.codingkody99.einkaufsliste.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named list. The first one by [position] is the main list: the app always
 * opens on it, so switching lists never becomes a thing you have to remember.
 */
@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val position: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
) {
    companion object {
        /** The list every item belongs to before there were several of them. */
        const val DEFAULT_ID = 1L
        const val DEFAULT_NAME = "Einkaufsliste"
    }
}

/** How full a list is, for the switcher. */
data class ListItemCounts(
    @ColumnInfo(name = "list_id") val listId: Long,
    @ColumnInfo(name = "open_count") val openCount: Int,
    @ColumnInfo(name = "total_count") val totalCount: Int,
)
