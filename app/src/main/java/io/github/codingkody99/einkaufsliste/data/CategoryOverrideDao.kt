package io.github.codingkody99.einkaufsliste.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryOverrideDao {

    @Query("SELECT * FROM category_overrides")
    fun observeAll(): Flow<List<CategoryOverride>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: CategoryOverride)

    @Query("DELETE FROM category_overrides WHERE name_key = :nameKey")
    suspend fun delete(nameKey: String)
}
