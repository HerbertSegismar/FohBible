package com.fountofhopedotorg.fohbible.color_wheel

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.theme.ThemeManager
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
import android.graphics.Color as AndroidColor

@Composable
fun ColorWheelDialog(
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    initialColor: Color = ThemeManager.primaryColor,
    enableGradient: Boolean = false,
    onGradientSelected: ((Color, Color, Offset, Offset) -> Unit)? = null,
    initialGradientConfig: GradientConfig? = null
) {
    var brightness by remember { mutableFloatStateOf(initialColor.getBrightness()) }
    var saturation by remember { mutableFloatStateOf(initialColor.getSaturation()) }
    var opacity by remember { mutableFloatStateOf(initialColor.alpha) }
    val initialHex = colorToHexString(initialColor)
    var hexTextFieldValue by remember {
        mutableStateOf(TextFieldValue(initialHex, selection = TextRange(initialHex.length)))
    }
    var isValidHex by remember { mutableStateOf(true) }

    val gradientEnabled = enableGradient && onGradientSelected != null
    var isSolidColor by remember {
        mutableStateOf(initialGradientConfig == null)
    }

    var gradientStartColor by remember {
        mutableStateOf(initialGradientConfig?.startColor ?: initialColor)
    }
    var gradientEndColor by remember {
        mutableStateOf(initialGradientConfig?.endColor ?: Color.White)
    }
    var gradientStartOffset by remember {
        mutableStateOf(initialGradientConfig?.startOffset ?: Offset(0.2f, 0.5f))
    }
    var gradientEndOffset by remember {
        mutableStateOf(initialGradientConfig?.endOffset ?: Offset(0.8f, 0.5f))
    }

    var activeGradientButton by remember {
        mutableStateOf(
            if (initialGradientConfig != null) GradientButton.START else null
        )
    }

    var selectedColor by remember {
        val initial = when (activeGradientButton) {
            GradientButton.START -> gradientStartColor
            GradientButton.END   -> gradientEndColor
            null                 -> initialColor
        }
        mutableStateOf(initial)
    }

    fun updateColor(newColor: Color) {
        when (activeGradientButton) {
            GradientButton.START -> gradientStartColor = newColor
            GradientButton.END   -> gradientEndColor = newColor
            null -> {
                gradientStartColor = newColor
            }
        }
        selectedColor = newColor
        brightness = newColor.getBrightness()
        saturation = newColor.getSaturation()
        opacity = newColor.alpha
        val newHex = colorToHexString(newColor)
        hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
        isValidHex = true
    }

    val lightBackground = Color.White
    val darkBackground = Color.Black

    val colorPalette = remember {
        listOf(
            Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B), Color(0xFFEAB308),
            Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF10B981), Color(0xFF14B8A6),
            Color(0xFF06B6D4), Color(0xFF0EA5E9), Color(0xFF3B82F6), Color(0xFF6366F1),
            Color(0xFF8B5CF6), Color(0xFFA855F7), Color(0xFFD946EF), Color(0xFFEC4899),
            Color(0xFF6B7280), Color(0xFF000000), Color(0xFFFFFFFF)
        )
    }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Scaffold(
                    topBar = {
                        FixedHeader(
                            title = "Color Picker",
                            onBackClick = onDismissRequest
                        )
                    },
                    containerColor = Color.Transparent
                ) { padding ->
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(0.3f),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ColorWheelSection(
                                    selectedColor = selectedColor,
                                    brightness = brightness,
                                    onColorSelected = { updateColor(it) }
                                )
                            }
                            Column(
                                modifier = Modifier.weight(0.32f).padding(end = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ColorAdjustmentsSection(
                                    brightness = brightness,
                                    saturation = saturation,
                                    opacity = opacity,
                                    selectedColor = selectedColor,
                                    onBrightnessChange = {
                                        brightness = it
                                        updateColor(adjustBrightness(selectedColor, it))
                                    },
                                    onSaturationChange = {
                                        saturation = it
                                        updateColor(adjustSaturation(selectedColor, it))
                                    },
                                    onOpacityChange = {
                                        opacity = it
                                        updateColor(adjustOpacity(selectedColor, it))
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                ColorPreviewSection(
                                    selectedColor = selectedColor,
                                    isSolidColor = isSolidColor,
                                    onSolidColorToggle = {
                                        isSolidColor = it
                                        activeGradientButton = if (it) {
                                            null
                                        } else {
                                            GradientButton.START
                                        }
                                    },
                                    hexTextFieldValue = hexTextFieldValue,
                                    isValidHex = isValidHex,
                                    lightBackground = lightBackground,
                                    darkBackground = darkBackground,
                                    startColor = gradientStartColor,
                                    endColor = gradientEndColor,
                                    startOffset = gradientStartOffset,
                                    endOffset = gradientEndOffset,
                                    onHexTextFieldValueChange = { newValue ->
                                        val processed = processHexInput(newValue.text, newValue.selection)
                                        hexTextFieldValue = processed
                                        if (processed.text.length > 1 && validateHex(processed.text)) {
                                            try {
                                                val colorInt = if (processed.text.length == 4) {
                                                    val hexValue = processed.text.substring(1)
                                                    val expanded = "#${hexValue[0]}${hexValue[0]}${hexValue[1]}${hexValue[1]}${hexValue[2]}${hexValue[2]}"
                                                    expanded.toColorInt()
                                                } else {
                                                    processed.text.toColorInt()
                                                }
                                                updateColor(Color(colorInt))
                                            } catch (_: Exception) {
                                                isValidHex = false
                                            }
                                        } else if (processed.text == "#") {
                                            isValidHex = false
                                        } else {
                                            isValidHex = false
                                        }
                                    }
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(0.38f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        GradientPickerSection(
                                            startColor = gradientStartColor,
                                            endColor = gradientEndColor,
                                            startOffset = gradientStartOffset,
                                            endOffset = gradientEndOffset,
                                            isSolidColor = isSolidColor,
                                            onStartOffsetChange = { gradientStartOffset = it },
                                            onEndOffsetChange = { gradientEndOffset = it },
                                            onButtonClick = { button ->
                                                activeGradientButton = button
                                                updateColor(
                                                    if (button == GradientButton.START) gradientStartColor
                                                    else gradientEndColor
                                                )
                                            },
                                            activeButton = activeGradientButton,
                                            modifier = Modifier.padding(top = 12.dp)
                                        )
                                        ColorPaletteSection(
                                            colorPalette = colorPalette,
                                            selectedColor = selectedColor,
                                            onColorClick = { color -> updateColor(color) }
                                        )
                                    }
                                }
                                ActionButtonsSection(
                                    selectedColor = selectedColor,
                                    isValidHex = isValidHex,
                                    onCancel = onDismissRequest,
                                    onApply = {
                                        if (gradientEnabled && !isSolidColor) {
                                            onGradientSelected(
                                                gradientStartColor,
                                                gradientEndColor,
                                                gradientStartOffset,
                                                gradientEndOffset
                                            )
                                            onDismissRequest()
                                        } else if (isValidHex) {
                                            onColorSelected(selectedColor)
                                            onDismissRequest()
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                ColorWheelSection(
                                    selectedColor = selectedColor,
                                    brightness = brightness,
                                    onColorSelected = { updateColor(it) }
                                )
                                ColorPreviewSection(
                                    selectedColor = selectedColor,
                                    isSolidColor = isSolidColor,
                                    onSolidColorToggle = {
                                        isSolidColor = it
                                        activeGradientButton = if (it) {
                                            null
                                        } else {
                                            GradientButton.START
                                        }
                                    },
                                    hexTextFieldValue = hexTextFieldValue,
                                    isValidHex = isValidHex,
                                    lightBackground = lightBackground,
                                    darkBackground = darkBackground,
                                    startColor = gradientStartColor,
                                    endColor = gradientEndColor,
                                    startOffset = gradientStartOffset,
                                    endOffset = gradientEndOffset,
                                    onHexTextFieldValueChange = { newValue ->
                                        val processed = processHexInput(newValue.text, newValue.selection)
                                        hexTextFieldValue = processed
                                        if (processed.text.length > 1 && validateHex(processed.text)) {
                                            try {
                                                val colorInt = if (processed.text.length == 4) {
                                                    val hexValue = processed.text.substring(1)
                                                    val expanded = "#${hexValue[0]}${hexValue[0]}${hexValue[1]}${hexValue[1]}${hexValue[2]}${hexValue[2]}"
                                                    expanded.toColorInt()
                                                } else {
                                                    processed.text.toColorInt()
                                                }
                                                updateColor(Color(colorInt))
                                            } catch (_: Exception) {
                                                isValidHex = false
                                            }
                                        } else if (processed.text == "#") {
                                            isValidHex = false
                                        } else {
                                            isValidHex = false
                                        }
                                    }
                                )
                                ColorAdjustmentsSection(
                                    brightness = brightness,
                                    saturation = saturation,
                                    opacity = opacity,
                                    selectedColor = selectedColor,
                                    onBrightnessChange = {
                                        brightness = it
                                        updateColor(adjustBrightness(selectedColor, it))
                                    },
                                    onSaturationChange = {
                                        saturation = it
                                        updateColor(adjustSaturation(selectedColor, it))
                                    },
                                    onOpacityChange = {
                                        opacity = it
                                        updateColor(adjustOpacity(selectedColor, it))
                                    }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GradientPickerSection(
                                        startColor = gradientStartColor,
                                        endColor = gradientEndColor,
                                        startOffset = gradientStartOffset,
                                        endOffset = gradientEndOffset,
                                        isSolidColor = isSolidColor,
                                        onStartOffsetChange = { gradientStartOffset = it },
                                        onEndOffsetChange = { gradientEndOffset = it },
                                        onButtonClick = { button ->
                                            activeGradientButton = button
                                            updateColor(
                                                if (button == GradientButton.START) gradientStartColor
                                                else gradientEndColor
                                            )
                                        },
                                        activeButton = activeGradientButton,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                    ColorPaletteSection(
                                        colorPalette = colorPalette,
                                        selectedColor = selectedColor,
                                        onColorClick = { color -> updateColor(color) }
                                    )

                                }

                                ActionButtonsSection(
                                    selectedColor = selectedColor,
                                    isValidHex = isValidHex,
                                    onCancel = onDismissRequest,
                                    onApply = {
                                        if (gradientEnabled && !isSolidColor) {
                                            onGradientSelected(
                                                gradientStartColor,
                                                gradientEndColor,
                                                gradientStartOffset,
                                                gradientEndOffset
                                            )
                                            onDismissRequest()
                                        } else if (isValidHex) {
                                            onColorSelected(selectedColor)
                                            onDismissRequest()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun colorToHexString(color: Color): String {
    return if (color.alpha == 1f) {
        String.format("#%06X", color.toArgb() and 0xFFFFFF)
    } else {
        String.format("#%08X", color.toArgb())
    }
}

private fun processHexInput(input: String, selection: TextRange): TextFieldValue {
    var processed = input.uppercase()
    var newSelection = selection
    if (!processed.startsWith("#")) {
        processed = "#$processed"
        newSelection = if (selection.start == 0) {
            TextRange(1, 1)
        } else {
            TextRange(selection.start + 1, selection.end + 1)
        }
    }
    val filtered = StringBuilder()
    val originalToFilteredMapping = mutableListOf<Int>()

    for (i in processed.indices) {
        val char = processed[i]
        if (i == 0 && char == '#') {
            filtered.append('#')
            originalToFilteredMapping.add(filtered.length - 1)
        } else if (char.isDigit() || char in 'A'..'F') {
            filtered.append(char)
            originalToFilteredMapping.add(filtered.length - 1)
        } else {
            originalToFilteredMapping.add(-1)
        }
    }
    if (filtered.length > 9) {
        filtered.length - 9
        filtered.delete(9, filtered.length)
        for (i in originalToFilteredMapping.indices) {
            if (originalToFilteredMapping[i] >= 9) {
                originalToFilteredMapping[i] = -1
            }
        }
    }
    val newStart = calculateNewCursorPosition(newSelection.start, originalToFilteredMapping)
    val newEnd = calculateNewCursorPosition(newSelection.end, originalToFilteredMapping)
    val finalStart = newStart.coerceIn(1, filtered.length)
    val finalEnd = newEnd.coerceIn(1, filtered.length)

    return TextFieldValue(filtered.toString(), selection = TextRange(finalStart, finalEnd))
}
private fun calculateNewCursorPosition(
    originalPos: Int,
    mapping: List<Int>
): Int {
    if (originalPos >= mapping.size) {
        return mapping.lastOrNull { it != -1 }?.plus(1) ?: 1
    }
    if (mapping[originalPos] != -1) {
        return mapping[originalPos] + 1
    }
    for (i in originalPos - 1 downTo 0) {
        if (mapping[i] != -1) {
            return mapping[i] + 1
        }
    }
    for (i in originalPos + 1 until mapping.size) {
        if (mapping[i] != -1) {
            return mapping[i] + 1
        }
    }
    return 1
}

fun updateColorFromOffset(
    offset: Offset,
    size: IntSize,
    brightness: Float,
    onColorSelected: (Color) -> Unit
) {
    val radius = min(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val newAngle = ((atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f)
    val distance = sqrt(dx * dx + dy * dy)
    val newSaturation = (distance / radius).coerceIn(0f, 1f)
    val color = Color.hsv(newAngle, newSaturation, brightness)
    onColorSelected(color)
}
fun Color.getBrightness(): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this.toArgb(), hsv)
    return hsv[2]
}

fun Color.getSaturation(): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this.toArgb(), hsv)
    return hsv[1]
}

fun adjustBrightness(color: Color, brightness: Float): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    hsv[2] = brightness.coerceIn(0f, 1f)
    return Color.hsv(hsv[0], hsv[1], hsv[2]).copy(alpha = color.alpha)
}

fun adjustSaturation(color: Color, saturation: Float): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    hsv[1] = saturation.coerceIn(0f, 1f)
    return Color.hsv(hsv[0], hsv[1], hsv[2]).copy(alpha = color.alpha)
}

fun adjustOpacity(color: Color, opacity: Float): Color {
    return color.copy(alpha = opacity.coerceIn(0f, 1f))
}
fun validateHex(hex: String): Boolean {
    if (hex.isEmpty() || !hex.startsWith("#")) return false
    if (hex.length != 4 && hex.length != 7 && hex.length != 9) return false
    for (i in 1 until hex.length) {
        val char = hex[i].uppercaseChar()
        if (!(char in '0'..'9' || char in 'A'..'F')) {
            return false
        }
    }

    return try {
        if (hex.length == 4) {
            val hexValue = hex.substring(1)
            val expanded = "#${hexValue[0]}${hexValue[0]}${hexValue[1]}${hexValue[1]}${hexValue[2]}${hexValue[2]}"
            expanded.toColorInt()
        } else {
            hex.toColorInt()
        }
        true
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: NumberFormatException) {
        false
    }
}