package io.github.codingkody99.einkaufsliste.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3's scale, enlarged for easier reading.
 *
 * These are `sp`, so the device's own font-size setting still applies on top —
 * this is an offset to the default, not a replacement for it. Weight is nudged
 * up as well: item names carry the most meaning on screen and read more sharply
 * at medium weight, which helps legibility without touching any colour.
 */
internal val AppTypography = Typography(
    // Screen and sheet headings.
    titleLarge = TextStyle(
        fontSize = 25.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 21.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    // Category headers in the list.
    titleSmall = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    // Item names.
    bodyLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    // Amounts and hints below an item.
    bodySmall = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    // Buttons and chips.
    labelLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
)
