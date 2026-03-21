package com.fountofhopedotorg.fohbible.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.fountofhopedotorg.fohbible.data.AppColorScheme
import com.fountofhopedotorg.fohbible.data.AppThemeState
import com.fountofhopedotorg.fohbible.data.ColorTheme
import android.graphics.Color as AndroidColor

val LocalAppTheme = staticCompositionLocalOf { AppThemeState() }

object ThemeManager {
    var primaryColor: Color by mutableStateOf(DefaultPrimaryColor)
    var darkTheme: Boolean by mutableStateOf(false)
    var isCustomColor: Boolean by mutableStateOf(false)
    fun generateColorScheme(primary: Color, isDark: Boolean): AppColorScheme {
        return if (isDark) {
            generateDarkColorScheme(primary)
        } else {
            generateLightColorScheme(primary)
        }
    }

    private fun generateLightColorScheme(primary: Color): AppColorScheme {
        val secondary = ColorUtils.blendARGB(primary.toArgb(), Color.Yellow.toArgb(), 0.4f)
        val tertiary = ColorUtils.blendARGB(primary.toArgb(), Color.Cyan.toArgb(), 0.4f)

        return AppColorScheme(
            primary = primary,
            onPrimary = if (primary.calculateBrightness() > 0.6f) Color.Black else Color.White,
            secondary = Color(secondary),
            onSecondary = Color.Black,
            tertiary = Color(tertiary),
            onTertiary = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color(0xFFF5F5DC),
            onSurface = Color.Black,
            surfaceVariant = Color(0xFFE0E0E0),
            primaryContainer = Color(0xFFFCF9EA),
            secondaryContainer = Color(secondary).copy(alpha = 0.2f)
        )
    }

    private fun generateDarkColorScheme(primary: Color): AppColorScheme {
        Color(ColorUtils.blendARGB(primary.toArgb(), Color.Black.toArgb(), 0.3f))
        val secondary = ColorUtils.blendARGB(primary.toArgb(), Color.Yellow.toArgb(), 0.4f)
        val tertiary = ColorUtils.blendARGB(primary.toArgb(), Color.Magenta.toArgb(), 0.4f)

        return AppColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = Color(secondary),
            onSecondary = Color.White,
            tertiary = Color(tertiary),
            onTertiary = Color.White,
            background = Color(0xFF1E1F21),
            onBackground = Color.White,
            surface = Color(0xFF04040C),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2D2D2D),
            primaryContainer = Color(0xFF070017),
            secondaryContainer = Color(secondary).copy(alpha = 0.2f)
        )
    }
}

fun Color.calculateBrightness(): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this.toArgb(), hsv)
    return hsv[2]
}

val DefaultPrimaryColor = Color(0xFF2196F3)

val PredefinedColorThemes = listOf(
    ColorTheme("Blue", Color(0xFF2196F3), Color(0xFF1976D2)),
    ColorTheme("Green", Color(0xFF4CAF50), Color(0xFF388E3C)),
    ColorTheme("Purple", Color(0xFF9C27B0), Color(0xFF7B1FA2)),
    ColorTheme("Orange", Color(0xFFFF9800), Color(0xFFF57C00)),
    ColorTheme("Red", Color(0xFFF44336), Color(0xFFD32F2F)),
    ColorTheme("Teal", Color(0xFF009688), Color(0xFF00796B)),
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun FohBibleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val themeState = LocalAppTheme.current
    val colorScheme = {
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
    }

    MaterialTheme(
        colorScheme = colorScheme(),
        typography = Typography,
        content = content
    )
}