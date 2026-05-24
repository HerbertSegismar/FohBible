package com.fountofhopedotorg.fohbible.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.fountofhopedotorg.fohbible.ui.theme.ThemeManager
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import android.content.res.Configuration
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import android.graphics.Color as AndroidColor

@Composable
fun ColorWheelDialog(
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    initialColor: Color = ThemeManager.primaryColor
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    var brightness by remember { mutableFloatStateOf(initialColor.getBrightness()) }
    var saturation by remember { mutableFloatStateOf(initialColor.getSaturation()) }
    var opacity by remember { mutableFloatStateOf(initialColor.alpha) }
    val initialHex = colorToHexString(initialColor)
    var hexTextFieldValue by remember {
        mutableStateOf(TextFieldValue(initialHex, selection = TextRange(initialHex.length)))
    }
    var isValidHex by remember { mutableStateOf(true) }

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
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(0.3f),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ColorWheelSection(
                                    selectedColor = selectedColor,
                                    brightness = brightness,
                                    onColorSelected = { color ->
                                        selectedColor = color
                                        saturation = color.getSaturation()
                                        brightness = color.getBrightness()
                                        opacity = color.alpha
                                        val newHex = colorToHexString(color)
                                        hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
                                        isValidHex = true
                                    }
                                )
                            }
                            Column(
                                modifier = Modifier.weight(0.32f),
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
                                        selectedColor = adjustBrightness(selectedColor, it)
                                        val newHex = colorToHexString(selectedColor)
                                        hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
                                    },
                                    onSaturationChange = {
                                        saturation = it
                                        selectedColor = adjustSaturation(selectedColor, it)
                                        val newHex = colorToHexString(selectedColor)
                                        hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
                                    },
                                    onOpacityChange = {
                                        opacity = it
                                        selectedColor = adjustOpacity(selectedColor, it)
                                        val newHex = colorToHexString(selectedColor)
                                        hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
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
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    ColorPreviewSection(
                                        selectedColor = selectedColor,
                                        hexTextFieldValue = hexTextFieldValue,
                                        isValidHex = isValidHex,
                                        lightBackground = lightBackground,
                                        darkBackground = darkBackground,
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
                                                    selectedColor = Color(colorInt)
                                                    brightness = selectedColor.getBrightness()
                                                    saturation = selectedColor.getSaturation()
                                                    opacity = selectedColor.alpha
                                                    isValidHex = true
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
                                    ColorPaletteSection(
                                        colorPalette = colorPalette,
                                        selectedColor = selectedColor,
                                        onColorClick = { color ->
                                            selectedColor = color
                                            brightness = color.getBrightness()
                                            saturation = color.getSaturation()
                                            opacity = color.alpha
                                            val newHex = colorToHexString(color)
                                            hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
                                            isValidHex = true
                                        }
                                    )
                                }
                                ActionButtonsSection(
                                    selectedColor = selectedColor,
                                    isValidHex = isValidHex,
                                    onCancel = onDismissRequest,
                                    onApply = {
                                        if (isValidHex) {
                                            onColorSelected(selectedColor)
                                            onDismissRequest()
                                        }
                                    }
                                )
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
                                    onColorSelected = { color ->
                                        selectedColor = color
                                        saturation = color.getSaturation()
                                        brightness = color.getBrightness()
                                        opacity = color.alpha
                                        val newHex = colorToHexString(color)
                                        hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
                                        isValidHex = true
                                    }
                                )
                                ColorPreviewSection(
                                    selectedColor = selectedColor,
                                    hexTextFieldValue = hexTextFieldValue,
                                    isValidHex = isValidHex,
                                    lightBackground = lightBackground,
                                    darkBackground = darkBackground,
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
                                                selectedColor = Color(colorInt)
                                                brightness = selectedColor.getBrightness()
                                                saturation = selectedColor.getSaturation()
                                                opacity = selectedColor.alpha
                                                isValidHex = true
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
                                        selectedColor = adjustBrightness(selectedColor, it)
                                        val newHex = colorToHexString(selectedColor)
                                        hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
                                    },
                                    onSaturationChange = {
                                        saturation = it
                                        selectedColor = adjustSaturation(selectedColor, it)
                                        val newHex = colorToHexString(selectedColor)
                                        hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
                                    },
                                    onOpacityChange = {
                                        opacity = it
                                        selectedColor = adjustOpacity(selectedColor, it)
                                        val newHex = colorToHexString(selectedColor)
                                        hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
                                    }
                                )
                                ColorPaletteSection(
                                    colorPalette = colorPalette,
                                    selectedColor = selectedColor,
                                    onColorClick = { color ->
                                        selectedColor = color
                                        brightness = color.getBrightness()
                                        saturation = color.getSaturation()
                                        opacity = color.alpha
                                        val newHex = colorToHexString(color)
                                        hexTextFieldValue = TextFieldValue(newHex, selection = TextRange(newHex.length))
                                        isValidHex = true
                                    }
                                )
                                ActionButtonsSection(
                                    selectedColor = selectedColor,
                                    isValidHex = isValidHex,
                                    onCancel = onDismissRequest,
                                    onApply = {
                                        if (isValidHex) {
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

@Composable
fun FixedHeader(
    title: String,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold, fontSize = 20.dp.value.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Customize your color selection",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ColorWheelSection(
    selectedColor: Color,
    brightness: Float,
    onColorSelected: (Color) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Palette,
                contentDescription = "Color Wheel",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Color Wheel",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                ImprovedColorWheel(
                    modifier = Modifier.size(195.dp),
                    selectedColor = selectedColor,
                    brightness = brightness,
                    onColorSelected = onColorSelected
                )
            }
        }
    }
}

@Composable
fun ColorPreviewSection(
    selectedColor: Color,
    hexTextFieldValue: TextFieldValue,
    isValidHex: Boolean,
    lightBackground: Color,
    darkBackground: Color,
    onHexTextFieldValueChange: (TextFieldValue) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected Color",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Selected Color",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextField(
                    value = hexTextFieldValue,
                    onValueChange = onHexTextFieldValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("HEX") },
                    isError = !isValidHex && hexTextFieldValue.text.length > 1,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                )

                if (!isValidHex && hexTextFieldValue.text.length > 1) {
                    Text(
                        text = "Invalid hex code",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(lightBackground)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                    )
                }
                Text(
                    text = "Light",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(darkBackground)
                        .border(1.dp, Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                    )
                }
                Text(
                    text = "Dark",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ColorAdjustmentsSection(
    brightness: Float,
    saturation: Float,
    opacity: Float,
    selectedColor: Color,
    onBrightnessChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Tune,
                contentDescription = "Adjustments",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Color Adjustments",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessMedium,
                    contentDescription = "Brightness",
                    tint = MaterialTheme.colorScheme.secondary
                )
                CustomSlider(
                    value = brightness,
                    onValueChange = onBrightnessChange,
                    thumbColor = selectedColor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(brightness * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tonality,
                    contentDescription = "Saturation",
                    tint = MaterialTheme.colorScheme.secondary
                )
                CustomSlider(
                    value = saturation,
                    onValueChange = onSaturationChange,
                    thumbColor = selectedColor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(saturation * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Opacity,
                    contentDescription = "Opacity",
                    tint = MaterialTheme.colorScheme.secondary
                )
                CustomSlider(
                    value = opacity,
                    onValueChange = onOpacityChange,
                    thumbColor = selectedColor.copy(alpha = opacity),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    thumbColor: Color,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(thumbColor, CircleShape)
            )
        },
        colors = SliderDefaults.colors(
            thumbColor = thumbColor,
            activeTrackColor = thumbColor,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
fun ColorPaletteSection(
    colorPalette: List<Color>,
    selectedColor: Color,
    onColorClick: (Color) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Colorize,
                contentDescription = "Color Wheel",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Color Palette",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colorPalette.forEach { color ->
                CompactCircularColorSwatch(
                    color = color,
                    isSelected = selectedColor == color,
                    onClick = { onColorClick(color) },
                    width = 20.dp
                )
            }
        }
    }
}

@Composable
fun ActionButtonsSection(
    selectedColor: Color,
    isValidHex: Boolean,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    val buttonTextColor = if (selectedColor.getBrightness() > 0.6f) Color.Black else Color.White

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 1f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = "Cancel",
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = onApply,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isValidHex) selectedColor else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                contentColor = if (isValidHex) buttonTextColor else MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            enabled = isValidHex
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Apply",
                    modifier = Modifier.size(18.dp),
                    tint = if (isValidHex) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = "Apply",
                    fontWeight = FontWeight.SemiBold,
                    color = if (isValidHex) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
fun CompactCircularColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    width: Dp,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(width)
            .clip(CircleShape)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 26.dp else 30.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 2.dp else 0.5.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )

        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = if (color.getBrightness() > 0.6f) Color.Black else Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
@Composable
fun ImprovedColorWheel(
    modifier: Modifier = Modifier,
    selectedColor: Color,
    brightness: Float = 0.5f,
    onColorSelected: (Color) -> Unit
) {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(selectedColor.toArgb(), hsv)
    val hue = hsv[0]
    val saturation = hsv[1]

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        updateColorFromOffset(offset, size, brightness, onColorSelected)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        updateColorFromOffset(change.position, size, brightness, onColorSelected)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        updateColorFromOffset(offset, size, brightness, onColorSelected)
                        tryAwaitRelease()
                    },
                    onTap = { offset ->
                        updateColorFromOffset(offset, size, brightness, onColorSelected)
                    }
                )
            }
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        for (angle in 0 until 360 step 1) {
            val wheelHue = angle.toFloat()
            val color = Color.hsv(wheelHue, 1f, brightness)
            drawArc(
                color = color,
                startAngle = angle.toFloat(),
                sweepAngle = 3f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.hsv(0f, 0f, brightness).copy(alpha = 0.7f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius
        )
        val radians = (hue * PI / 180).toFloat()
        val indicatorRadius = saturation * radius
        val indicatorX = center.x + indicatorRadius * cos(radians)
        val indicatorY = center.y + indicatorRadius * sin(radians)
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = center,
            end = Offset(indicatorX, indicatorY),
            strokeWidth = 1.dp.toPx()
        )
        drawCircle(
            color = Color.White,
            center = Offset(indicatorX, indicatorY),
            radius = 12.dp.toPx(),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = selectedColor,
            center = Offset(indicatorX, indicatorY),
            radius = 8.dp.toPx()
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            center = center,
            radius = 2.dp.toPx()
        )
    }
}
private fun updateColorFromOffset(
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