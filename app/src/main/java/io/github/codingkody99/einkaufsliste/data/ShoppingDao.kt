package io.github.codingkody99.einkaufsliste.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    /**
     * Emits the whole list in insertion order; grouping and sorting for the UI
     * happen in [io.github.codingkody99.einkaufsliste.domain.ShoppingListGrouper]
     * so that the category order stays testable without a database.
     */
    @Query("SELECT * FROM shopping_items ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<ShoppingItem>>

    @Insert
    suspend fun insert(item: ShoppingItem): Long

    @Update
    suspend fun update(item: ShoppingItem)

    @Query("UPDATE shopping_items SET is_checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shopping_items WHERE is_checked = 1")
    suspend fun deleteChecked()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()
}
