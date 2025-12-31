package com.example.fohbible

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import android.graphics.Color as AndroidColor

/**
 * Professional Color Picker Dialog with unified single-container design
 */
@SuppressLint("FrequentlyChangingValue")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ColorWheelDialog(
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    initialColor: Color = Color.White
) {
    // State for color selection
    var selectedColor by remember { mutableStateOf(initialColor) }
    var brightness by remember { mutableFloatStateOf(initialColor.getBrightness()) }
    var saturation by remember { mutableFloatStateOf(initialColor.getSaturation()) }
    var hexInput by remember { mutableStateOf("#${(initialColor.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}") }
    var isValidHex by remember { mutableStateOf(true) }

    // For previewing the color in different contexts
    val lightBackground = Color.White
    val darkBackground = Color.Black

    // Predefined color palette
    val colorPalette = remember {
        listOf(
            Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B), Color(0xFFEAB308),
            Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF10B981), Color(0xFF14B8A6),
            Color(0xFF06B6D4), Color(0xFF0EA5E9), Color(0xFF3B82F6), Color(0xFF6366F1),
            Color(0xFF8B5CF6), Color(0xFFA855F7), Color(0xFFD946EF), Color(0xFFEC4899),
            Color(0xFF6B7280), Color(0xFF000000), Color(0xFFFFFFFF)
        )
    }

    // Track scroll state
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
            // Main Container with professional background
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
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
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Unified content container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Color Wheel Section
                            ColorWheelSection(
                                selectedColor = selectedColor,
                                brightness = brightness,
                                onColorSelected = { color ->
                                    selectedColor = color
                                    saturation = color.getSaturation()
                                    hexInput = "#${(color.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}"
                                    isValidHex = true
                                }
                            )

                            // Color Preview and Hex Input
                            ColorPreviewSection(
                                selectedColor = selectedColor,
                                hexInput = hexInput,
                                isValidHex = isValidHex,
                                lightBackground = lightBackground,
                                darkBackground = darkBackground,
                                onHexInputChange = { newHex ->
                                    hexInput = newHex.uppercase()
                                    if (newHex.length <= 7) {
                                        if (validateHex(newHex)) {
                                            try {
                                                val colorInt = newHex.toColorInt()
                                                selectedColor = Color(colorInt)
                                                brightness = selectedColor.getBrightness()
                                                saturation = selectedColor.getSaturation()
                                                isValidHex = true
                                            } catch (_: IllegalArgumentException) {
                                                isValidHex = false
                                            }
                                        } else {
                                            isValidHex = false
                                        }
                                    } else {
                                        isValidHex = false
                                    }
                                }
                            )

                            // Color Adjustments
                            ColorAdjustmentsSection(
                                brightness = brightness,
                                saturation = saturation,
                                selectedColor = selectedColor,
                                onBrightnessChange = {
                                    brightness = it
                                    selectedColor = adjustBrightness(selectedColor, it)
                                    hexInput = "#${(selectedColor.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}"
                                },
                                onSaturationChange = {
                                    saturation = it
                                    selectedColor = adjustSaturation(selectedColor, it)
                                    hexInput = "#${(selectedColor.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}"
                                }
                            )

                            // Color Palette
                            ColorPaletteSection(
                                colorPalette = colorPalette,
                                selectedColor = selectedColor,
                                onColorClick = { color ->
                                    selectedColor = color
                                    brightness = color.getBrightness()
                                    saturation = color.getSaturation()
                                    hexInput = "#${(color.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}"
                                    isValidHex = true
                                }
                            )

                            // Action Buttons
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

@Composable
fun FixedHeader(
    title: String,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
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
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold, fontSize = 18.dp.value.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Customize your color selection",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    hexInput: String,
    isValidHex: Boolean,
    lightBackground: Color,
    darkBackground: Color,
    onHexInputChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Selected Color",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Color preview row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview circle
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                // Hex input
                TextField(
                    value = hexInput,
                    onValueChange = onHexInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("HEX") },
                    isError = !isValidHex,
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
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                )

                if (!isValidHex) {
                    Text(
                        text = "Invalid hex code",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }

            // Background previews
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
    selectedColor: Color,
    onBrightnessChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit
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
                text = "Adjustments",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Brightness Slider
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Brightness",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(brightness * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                colors = SliderDefaults.colors(
                    thumbColor = selectedColor,
                    activeTrackColor = selectedColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        // Saturation Slider
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saturation",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(saturation * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = saturation,
                onValueChange = onSaturationChange,
                colors = SliderDefaults.colors(
                    thumbColor = selectedColor,
                    activeTrackColor = selectedColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
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
        Text(
            text = "Color Palette",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            colorPalette.forEach { color ->
                CompactCircularColorSwatch(
                    color = color,
                    isSelected = selectedColor == color,
                    onClick = { onColorClick(color) }
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
    // Calculate appropriate text color based on selected color brightness
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
                    tint = if (isValidHex) buttonTextColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = "Apply",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CompactCircularColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(32.dp)
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
        // Color circle with border
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

// Improved Color Wheel with dragging support
@Composable
fun ImprovedColorWheel(
    modifier: Modifier = Modifier,
    selectedColor: Color,
    brightness: Float = 0.5f,
    onColorSelected: (Color) -> Unit
) {
    // Get hue and saturation from the selected color
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(selectedColor.toArgb(), hsv)
    val hue = hsv[0]
    val saturation = hsv[1]

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        // This gives us dragging behavior
                        val radius = min(size.width, size.height) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val newAngle = ((atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f)
                        val distance = sqrt(dx * dx + dy * dy)
                        val newSaturation = (distance / radius).coerceIn(0f, 1f)
                        val color = Color.hsv(newAngle, newSaturation, brightness)
                        onColorSelected(color)

                        // Wait for release or drag
                        val released = tryAwaitRelease()
                        if (!released) {
                            // User dragged instead of releasing
                            // The onPress will continue to be called during drag
                        }
                    },
                    onTap = { offset ->
                        // Handle simple tap
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
                )
            }
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Draw color wheel
        for (angle in 0 until 360 step 3) {
            val wheelHue = angle.toFloat()
            val color = Color.hsv(wheelHue, 1f, brightness)
            drawArc(
                color = color,
                startAngle = angle.toFloat(),
                sweepAngle = 3f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }

        // Draw saturation gradient
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

        // Draw selection indicator
        val radians = (hue * PI / 180).toFloat()
        val indicatorRadius = saturation * radius
        val indicatorX = center.x + indicatorRadius * cos(radians)
        val indicatorY = center.y + indicatorRadius * sin(radians)

        // Draw a line from center to indicator for better visibility
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = center,
            end = Offset(indicatorX, indicatorY),
            strokeWidth = 1.dp.toPx()
        )

        // Outer ring
        drawCircle(
            color = Color.White,
            center = Offset(indicatorX, indicatorY),
            radius = 12.dp.toPx(),
            style = Stroke(width = 2.dp.toPx())
        )

        // Inner color
        drawCircle(
            color = selectedColor,
            center = Offset(indicatorX, indicatorY),
            radius = 8.dp.toPx()
        )

        // Center dot for reference
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            center = center,
            radius = 2.dp.toPx()
        )
    }
}

// Helper functions
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
    return Color(AndroidColor.HSVToColor(hsv))
}

fun adjustSaturation(color: Color, saturation: Float): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    hsv[1] = saturation.coerceIn(0f, 1f)
    return Color(AndroidColor.HSVToColor(hsv))
}

fun validateHex(hex: String): Boolean {
    return try {
        hex.toColorInt()
        true
    } catch (_: IllegalArgumentException) {
        false
    }
}