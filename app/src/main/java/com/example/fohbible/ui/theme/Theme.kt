package com.example.fohbible.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.fohbible.LocalAppTheme
import com.example.fohbible.ThemeManager

// Define default light color scheme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF3700B3),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5DC),
    surfaceVariant = Color(0xFFE1E1E1),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    primaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFFC8E6C9)
)

// Define default dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF3700B3),
    background = Color(0xFF121212),
    surface = Color(0xFF4B0082),
    surfaceVariant = Color(0xFF2D2D2D),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF4F378B),
    secondaryContainer = Color(0xFF1B5E20)
)

@Composable
fun FohBibleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val themeState = LocalAppTheme.current
    val colorScheme = if (themeState.isCustomColor) {
        // Use custom color scheme
        val appColorScheme = ThemeManager.generateColorScheme(themeState.primaryColor, darkTheme)

        if (darkTheme) {
            darkColorScheme(
                primary = appColorScheme.primary,
                onPrimary = appColorScheme.onPrimary,
                secondary = appColorScheme.secondary,
                onSecondary = appColorScheme.onSecondary,
                tertiary = appColorScheme.tertiary,
                onTertiary = appColorScheme.onTertiary,
                background = appColorScheme.background,
                onBackground = appColorScheme.onBackground,
                surface = appColorScheme.surface,
                onSurface = appColorScheme.onSurface,
                surfaceVariant = appColorScheme.surfaceVariant,
                primaryContainer = appColorScheme.primaryContainer,
                secondaryContainer = appColorScheme.secondaryContainer
            )
        } else {
            lightColorScheme(
                primary = appColorScheme.primary,
                onPrimary = appColorScheme.onPrimary,
                secondary = appColorScheme.secondary,
                onSecondary = appColorScheme.onSecondary,
                tertiary = appColorScheme.tertiary,
                onTertiary = appColorScheme.onTertiary,
                background = appColorScheme.background,
                onBackground = appColorScheme.onBackground,
                surface = appColorScheme.surface,
                onSurface = appColorScheme.onSurface,
                surfaceVariant = appColorScheme.surfaceVariant,
                primaryContainer = appColorScheme.primaryContainer,
                secondaryContainer = appColorScheme.secondaryContainer
            )
        }
    } else {
        // Use default theme
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}