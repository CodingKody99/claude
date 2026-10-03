package io.github.codingkody99.einkaufsliste.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    /**
     * Emits one list's items in insertion order; grouping and sorting for the UI
     * happen in [io.github.codingkody99.einkaufsliste.domain.ShoppingListGrouper]
     * so that the category order stays testable without a database.
     */
    @Query("SELECT * FROM shopping_items WHERE list_id = :listId ORDER BY created_at ASC, id ASC")
    fun observeByList(listId: Long): Flow<List<ShoppingItem>>

    @Query(
        "SELECT list_id, " +
            "COUNT(CASE WHEN is_checked = 0 THEN 1 END) AS open_count, " +
            "COUNT(*) AS total_count " +
            "FROM shopping_items GROUP BY list_id",
    )
    fun observeCounts(): Flow<List<ListItemCounts>>

    @Insert
    suspend fun insert(item: ShoppingItem): Long

    @Update
    suspend fun update(item: ShoppingItem)

    @Query("UPDATE shopping_items SET is_checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shopping_items WHERE list_id = :listId AND is_checked = 1")
    suspend fun deleteChecked(listId: Long)

    @Query("DELETE FROM shopping_items WHERE list_id = :listId")
    suspend fun deleteByList(listId: Long)
}
