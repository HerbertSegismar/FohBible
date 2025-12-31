// ThemeManager.kt
package com.example.fohbible

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

data class AppThemeState(
    val darkTheme: Boolean = false,
    val primaryColor: Color = Color(0xFF6200EE), // Default primary color
    val isCustomColor: Boolean = false
)

val LocalAppTheme = staticCompositionLocalOf { AppThemeState() }

object ThemeManager {
    var primaryColor: Color by mutableStateOf(Color(0xFF6200EE))
    var darkTheme: Boolean by mutableStateOf(false)
    var isCustomColor: Boolean by mutableStateOf(false)

    // Generate color scheme based on primary color
    fun generateColorScheme(primary: Color, isDark: Boolean): AppColorScheme {
        return if (isDark) {
            generateDarkColorScheme(primary)
        } else {
            generateLightColorScheme(primary)
        }
    }

    private fun generateLightColorScheme(primary: Color): AppColorScheme {
        // Generate harmonious colors based on primary
        val secondary = ColorUtils.blendARGB(primary.toArgb(), Color.Yellow.toArgb(), 0.3f)
        val tertiary = ColorUtils.blendARGB(primary.toArgb(), Color.Cyan.toArgb(), 0.3f)

        return AppColorScheme(
            primary = primary,
            onPrimary = if (primary.calculateBrightness() > 0.6f) Color.Black else Color.White,
            secondary = Color(secondary),
            onSecondary = Color.Black,
            tertiary = Color(tertiary),
            onTertiary = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color(0xFFE0E0E0),
            primaryContainer = primary.copy(alpha = 0.1f),
            secondaryContainer = Color(secondary).copy(alpha = 0.1f)
        )
    }

    private fun generateDarkColorScheme(primary: Color): AppColorScheme {
        // Darken the primary color for dark theme
        val darkPrimary = Color(ColorUtils.blendARGB(primary.toArgb(), Color.Black.toArgb(), 0.2f))
        val secondary = ColorUtils.blendARGB(primary.toArgb(), Color.Yellow.toArgb(), 0.3f)
        val tertiary = ColorUtils.blendARGB(primary.toArgb(), Color.Magenta.toArgb(), 0.3f)

        return AppColorScheme(
            primary = darkPrimary,
            onPrimary = Color.White,
            secondary = Color(secondary),
            onSecondary = Color.White,
            tertiary = Color(tertiary),
            onTertiary = Color.White,
            background = Color(0xFF121212),
            onBackground = Color.White,
            surface = Color(0xFF121212),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2D2D2D),
            primaryContainer = darkPrimary.copy(alpha = 0.2f),
            secondaryContainer = Color(secondary).copy(alpha = 0.2f)
        )
    }
}

data class AppColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color
)

// Helper extension to get brightness
fun Color.calculateBrightness(): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    return hsv[2]
}