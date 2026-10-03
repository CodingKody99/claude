package io.github.codingkody99.einkaufsliste.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Query("SELECT * FROM shopping_lists ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<ShoppingList>>

    /** The main list: first by position, then by age. */
    @Query("SELECT * FROM shopping_lists ORDER BY position ASC, id ASC LIMIT 1")
    suspend fun firstList(): ShoppingList?

    @Query("SELECT COUNT(*) FROM shopping_lists")
    suspend fun count(): Int

    @Query("SELECT MAX(position) FROM shopping_lists")
    suspend fun maxPosition(): Int?

    @Insert
    suspend fun insert(list: ShoppingList): Long

    @Query("UPDATE shopping_lists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun delete(id: Long)
}
