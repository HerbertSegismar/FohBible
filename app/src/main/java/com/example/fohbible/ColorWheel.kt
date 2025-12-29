package com.example.fohbible

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fohbible.ui.theme.FohBibleTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Color selection dialog with a color wheel interface
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ColorWheelDialog(
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    initialColor: Color = Color.White
) {
    // State for color selection
    var selectedColor by remember { mutableStateOf(initialColor) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var showPalette by remember { mutableStateOf(true) }

    // Predefined color palette
    val colorPalette = remember {
        listOf(
            Color(0xFFDC2626), // Red
            Color(0xFFEA580C), // Orange
            Color(0xFFFBBF24), // Amber
            Color(0xFF65A30D), // Green
            Color(0xFF059669), // Emerald
            Color(0xFF0D9488), // Teal
            Color(0xFF0891B2), // Cyan
            Color(0xFF2563EB), // Blue
            Color(0xFF4F46E5), // Indigo
            Color(0xFF7C3AED), // Violet
            Color(0xFF9333EA), // Purple
            Color(0xFFDB2777), // Pink
            Color(0xFF44403C), // Gray
            Color(0xFF000000), // Black
            Color(0xFFFFFFFF), // White
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Color Palette") },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Toggle between wheel and palette
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showPalette = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                        enabled = !showPalette
                    ) {
                        Text("Palette")
                    }
                    Button(
                        onClick = { showPalette = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                        enabled = showPalette
                    ) {
                        Text("Color Wheel")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (showPalette) {
                    // Predefined color palette view
                    Text(
                        text = "Select a Color",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        colorPalette.forEach { color ->
                            ColorSwatch(
                                color = color,
                                isSelected = selectedColor == color,
                                onClick = {
                                    selectedColor = color
                                    brightness = color.getBrightness()
                                    saturation = color.getSaturation()
                                }
                            )
                        }
                    }
                } else {
                    // Color wheel view
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ColorWheel(
                            modifier = Modifier.size(300.dp),
                            brightness = brightness,
                            onColorSelected = { color ->
                                selectedColor = color
                                saturation = color.getSaturation()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Brightness slider
                    Text("Brightness")
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            selectedColor = adjustBrightness(selectedColor, it)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Saturation slider
                    Text("Saturation")
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            selectedColor = adjustSaturation(selectedColor, it)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Selected color preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = selectedColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Selected Color",
                            color = if (selectedColor.luminance() > 0.5) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm button
                Button(
                    onClick = {
                        onColorSelected(selectedColor)
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use This Color")
                }
            }
        }
    }
}

@Composable
fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .shadow(if (isSelected) 8.dp else 4.dp, CircleShape)
    )
}

@Composable
fun ColorWheel(
    modifier: Modifier = Modifier,
    brightness: Float = 0.5f,
    onColorSelected: (Color) -> Unit
) {
    var selectedAngle by remember { mutableFloatStateOf(0f) }
    var selectedRadius by remember { mutableFloatStateOf(0.5f) }

    Box(
        modifier = modifier
            .drawBehind {
                // Calculate the minimum dimension manually
                val radius = min(size.width, size.height) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Draw color wheel sectors
                for (angle in 0 until 360 step 10) { // Reduced from 1 to 10 for better performance
                    val hue = angle.toFloat()
                    val color = Color.hsv(hue, 1f, brightness)

                    // Draw each sector
                    rotate(degrees = angle.toFloat()) {
                        drawRect(
                            color = color,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = radius * 2)
                        )
                    }
                }

                // Draw inner gradient (saturation)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )

                // Draw selection indicator
                val radians = Math.toRadians(selectedAngle.toDouble())
                val indicatorX = center.x + selectedRadius * radius * cos(radians).toFloat()
                val indicatorY = center.y + selectedRadius * radius * sin(radians).toFloat()

                drawCircle(
                    color = Color.White,
                    center = Offset(indicatorX, indicatorY),
                    radius = 8.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.Black,
                    center = Offset(indicatorX, indicatorY),
                    radius = 6.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Calculate the minimum dimension manually
                    val radius = min(size.width, size.height) / 2
                    val center = Offset((size.width / 2).toFloat(), (size.height / 2).toFloat())

                    val dx = offset.x - center.x
                    val dy = offset.y - center.y

                    // Calculate angle
                    selectedAngle = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360) % 360).toFloat()

                    // Calculate radius (distance from center)
                    val distance = sqrt(dx * dx + dy * dy)
                    selectedRadius = (distance / radius).coerceIn(0f, 1f)

                    // Calculate color
                    val hue = selectedAngle
                    val saturation = selectedRadius
                    val color = Color.hsv(hue, saturation, brightness)

                    onColorSelected(color)
                }
            }
    )
}

// Alternative implementation using Canvas for better performance
@Composable
fun ColorWheelCanvas(
    modifier: Modifier = Modifier,
    brightness: Float = 0.5f,
    onColorSelected: (Color) -> Unit
) {
    var selectedAngle by remember { mutableFloatStateOf(0f) }
    var selectedRadius by remember { mutableFloatStateOf(0.5f) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val radius = min(size.width, size.height) / 2
                    val center = Offset((size.width / 2).toFloat(), (size.height / 2).toFloat())

                    val dx = offset.x - center.x
                    val dy = offset.y - center.y

                    // Calculate angle
                    selectedAngle = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360) % 360).toFloat()

                    // Calculate radius (distance from center)
                    val distance = sqrt(dx * dx + dy * dy)
                    selectedRadius = (distance / radius).coerceIn(0f, 1f)

                    // Calculate color
                    val hue = selectedAngle
                    val saturation = selectedRadius
                    val color = Color.hsv(hue, saturation, brightness)

                    onColorSelected(color)
                }
            }
    ) {
        val radius = min(size.width, size.height) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Draw color wheel
        for (angle in 0 until 360 step 5) { // Step 5 for better performance
            val hue = angle.toFloat()
            val color = Color.hsv(hue, 1f, brightness)

            // Convert degrees to radians
            val startRad = Math.toRadians(angle.toDouble())
            val endRad = Math.toRadians((angle + 5).toDouble())

            // Draw pie slice
            drawArc(
                color = color,
                startAngle = angle.toFloat(),
                sweepAngle = 5f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }

        // Draw saturation gradient overlay
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius
        )

        // Draw selection indicator
        val radians = Math.toRadians(selectedAngle.toDouble())
        val indicatorX = center.x + selectedRadius * radius * cos(radians).toFloat()
        val indicatorY = center.y + selectedRadius * radius * sin(radians).toFloat()

        drawCircle(
            color = Color.White,
            center = Offset(indicatorX, indicatorY),
            radius = 8.dp.toPx(),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = Color.Black,
            center = Offset(indicatorX, indicatorY),
            radius = 6.dp.toPx(),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

// Helper functions for color manipulation
fun Color.getBrightness(): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    return hsv[2]
}

fun Color.getSaturation(): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    return hsv[1]
}

fun adjustBrightness(color: Color, brightness: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[2] = brightness.coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun adjustSaturation(color: Color, saturation: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = saturation.coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Simple color picker dialog with minimal options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleColorPickerDialog(
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val colorPalette = remember {
        listOf(
            Color(0xFFDC2626), // Red
            Color(0xFFEA580C), // Orange
            Color(0xFFFBBF24), // Amber
            Color(0xFF65A30D), // Green
            Color(0xFF059669), // Emerald
            Color(0xFF0D9488), // Teal
            Color(0xFF0891B2), // Cyan
            Color(0xFF2563EB), // Blue
            Color(0xFF4F46E5), // Indigo
            Color(0xFF7C3AED), // Violet
            Color(0xFF9333EA), // Purple
            Color(0xFFDB2777), // Pink
            Color(0xFF44403C), // Gray
            Color(0xFF000000), // Black
            Color(0xFFFFFFFF), // White
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose Color",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorPalette.forEach { color ->
                        ColorSwatch(
                            color = color,
                            isSelected = false,
                            onClick = {
                                onColorSelected(color)
                                onDismissRequest()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Color Wheel Dialog")
@Composable
fun ColorWheelDialogPreview() {
    FohBibleTheme {
        ColorWheelDialog(
            onDismissRequest = { },
            onColorSelected = { }
        )
    }
}

@Preview(showBackground = true, name = "Simple Color Picker")
@Composable
fun SimpleColorPickerPreview() {
    FohBibleTheme {
        SimpleColorPickerDialog(
            onDismissRequest = { },
            onColorSelected = { }
        )
    }
}

@Preview(showBackground = true, name = "Color Wheel Canvas")
@Composable
fun ColorWheelCanvasPreview() {
    FohBibleTheme {
        Box(modifier = Modifier.size(300.dp)) {
            ColorWheelCanvas(
                modifier = Modifier.fillMaxSize(),
                brightness = 0.7f,
                onColorSelected = { }
            )
        }
    }
}