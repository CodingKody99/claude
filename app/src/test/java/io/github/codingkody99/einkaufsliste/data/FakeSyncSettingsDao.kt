package io.github.codingkody99.einkaufsliste.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory stand-in for the single-row sharing settings. */
class FakeSyncSettingsDao : SyncSettingsDao {

    private val state = MutableStateFlow<SyncSettings?>(null)

    val settings: SyncSettings? get() = state.value

    override fun observe(): Flow<SyncSettings?> = state

    override suspend fun put(settings: SyncSettings) {
        state.value = settings
    }
}
