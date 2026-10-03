package io.github.codingkody99.einkaufsliste.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Sand40,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Sand90,
    onSecondaryContainer = Sand10,
    tertiary = Brick40,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = Brick90,
    onTertiaryContainer = Brick10,
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Green10,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Sand80,
    onSecondary = Sand10,
    secondaryContainer = Sand30,
    onSecondaryContainer = Sand90,
    tertiary = Brick80,
    onTertiary = Brick10,
    tertiaryContainer = Brick30,
    onTertiaryContainer = Brick90,
)

/**
 * Uses Material You (the wallpaper-derived palette) on Android 12 and newer,
 * which is what a Pixel will do; falls back to the green palette above.
 */
@Composable
fun EinkaufslisteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
