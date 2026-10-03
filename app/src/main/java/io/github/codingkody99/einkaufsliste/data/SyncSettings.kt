package io.github.codingkody99.einkaufsliste.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Whether this phone keeps its lists to itself or shares them.
 *
 * A single row: [householdId] set means the data lives in the shared household
 * and is synchronised; null means everything stays on the device.
 */
@Entity(tableName = "sync_settings")
data class SyncSettings(
    @PrimaryKey
    val id: Int = ROW_ID,
    @ColumnInfo(name = "household_id")
    val householdId: String? = null,
) {
    val isShared: Boolean get() = !householdId.isNullOrBlank()

    companion object {
        const val ROW_ID = 1
    }
}
