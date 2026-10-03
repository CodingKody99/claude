package io.github.codingkody99.einkaufsliste.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.codingkody99.einkaufsliste.EinkaufslisteApplication

/**
 * Hands [ShoppingListViewModel] the repository held by the Application.
 *
 * Kept apart from the view model so that the view model itself depends only on
 * `ViewModel` and can be exercised by plain JVM tests.
 */
val ShoppingListViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            as EinkaufslisteApplication
        ShoppingListViewModel(application.repository)
    }
}
