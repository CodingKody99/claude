package io.github.codingkody99.einkaufsliste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.codingkody99.einkaufsliste.ui.ShoppingListRoute
import io.github.codingkody99.einkaufsliste.ui.theme.EinkaufslisteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EinkaufslisteTheme {
                ShoppingListRoute()
            }
        }
    }
}
