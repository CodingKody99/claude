package io.github.codingkody99.einkaufsliste.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncSettingsDao {

    @Query("SELECT * FROM sync_settings WHERE id = ${SyncSettings.ROW_ID}")
    fun observe(): Flow<SyncSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(settings: SyncSettings)
}
