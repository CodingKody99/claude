package io.github.codingkody99.einkaufsliste.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Recipe>>

    @Insert
    suspend fun insert(recipe: Recipe): Long

    @Update
    suspend fun update(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: Long)
}
