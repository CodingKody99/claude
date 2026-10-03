package io.github.codingkody99.einkaufsliste

import android.app.Application
import io.github.codingkody99.einkaufsliste.data.AppDatabase
import io.github.codingkody99.einkaufsliste.data.RoomShoppingRepository
import io.github.codingkody99.einkaufsliste.data.ShoppingRepository

/**
 * Holds the single database and repository instance. The app is small enough
 * that manual wiring beats pulling in a DI framework.
 */
class EinkaufslisteApplication : Application() {

    val repository: ShoppingRepository by lazy {
        val database = AppDatabase.get(this)
        RoomShoppingRepository(database.shoppingDao(), database.categoryOverrideDao())
    }
}
