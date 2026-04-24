package com.codegrogu.guava.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GuavaGreenPastel,
    secondary = GearGrayLight,
    tertiary = WarningAmber,
    background = OilCharcoalDark,
    surface = OilCharcoalDark,
    onPrimary = OilCharcoalDark,
    onSecondary = OilCharcoalDark,
    onBackground = Color.White,
    onSurface = Color.White,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = GuavaGreen,
    secondary = GearGray,
    tertiary = GuavaGreenDark,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = OilCharcoal,
    onSurface = OilCharcoal,
    error = ErrorRed
)

@Composable
fun GuavaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamic color by default to keep the "Garage" branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Dynamic color is not used here for brand consistency, but keeping param for compatibility
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
