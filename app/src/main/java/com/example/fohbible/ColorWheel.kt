package com.example.fohbible

import android.graphics.Color as AndroidColor
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
import androidx.compose.material3.TextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fohbible.ui.theme.FohBibleTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.core.graphics.toColorInt

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
    var brightness by remember { mutableFloatStateOf(initialColor.getBrightness()) }
    var saturation by remember { mutableFloatStateOf(initialColor.getSaturation()) }
    var hexInput by remember { mutableStateOf("#${(initialColor.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}") }
    var isValidHex by remember { mutableStateOf(true) }

    // Predefined color palette
    val colorPalette = remember {
        listOf(
            Color(0xFFFF0000), // Red
            Color(0xFF00FF00), // Green
            Color(0xFF0000FF), // Blue
            Color(0xFFFFFF00), // Yellow
            Color(0xFFFF00FF), // Magenta
            Color(0xFF00FFFF), // Cyan
            Color(0xFFFFA500), // Orange
            Color(0xFF800080), // Purple
            Color(0xFFFFC0CB), // Pink
            Color(0xFFA52A2A), // Brown
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
                    title = { Text("Choose Custom Color") },
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
                    .verticalScroll(rememberScrollState()) // Added scrolling here
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                            hexInput = "#${(color.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}"
                            isValidHex = true
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
                        hexInput = "#${(selectedColor.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}"
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
                        hexInput = "#${(selectedColor.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}"
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
                // Swatches
                Text(
                    text = "Swatches",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorPalette.forEach { color ->
                        ColorSwatch(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = {
                                selectedColor = color
                                brightness = color.getBrightness()
                                saturation = color.getSaturation()
                                hexInput = "#${(color.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}"
                                isValidHex = true
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                // Selected color preview with hex input
                Text(
                    text = "Selected Color:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(
                            width = 2.dp,
                            color = if (isValidHex) MaterialTheme.colorScheme.outline else Color(0xFFFF3B30),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = hexInput,
                        onValueChange = { newHex ->
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
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        maxLines = 1,
                        placeholder = { Text("#FFFFFF") },
                        isError = !isValidHex
                    )
                }
                if (!isValidHex) {
                    Text(
                        text = "Invalid hex color code",
                        color = Color(0xFFFF3B30),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 6.dp, start = 4.dp)
                    )
                }
                Text(
                    text = "Tap the wheel & sliders to pick a color or type the hex code",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
                )
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (isValidHex) {
                                onColorSelected(selectedColor)
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp)) // Added extra spacer for better scrolling
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
            .size(30.dp)
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

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val radius = min(size.width, size.height) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    // Calculate angle
                    selectedAngle = ((atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f)
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
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Draw color wheel with arcs for hues
        for (angle in 0 until 360 step 1) {
            val hue = angle.toFloat()
            val color = Color.hsv(hue, 1f, brightness)
            drawArc(
                color = color,
                startAngle = angle.toFloat(),
                sweepAngle = 1f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }

        // Draw saturation gradient overlay
        val gray = Color.hsv(0f, 0f, brightness)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(gray, Color.Transparent),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius
        )

        // Draw selection indicator
        val radians = (selectedAngle * PI / 180).toFloat()
        val indicatorX = center.x + selectedRadius * radius * cos(radians)
        val indicatorY = center.y + selectedRadius * radius * sin(radians)
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
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // Added scrolling here too
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

@Preview(showBackground = true, name = "Color Wheel")
@Composable
fun ColorWheelPreview() {
    FohBibleTheme {
        Box(modifier = Modifier.size(300.dp)) {
            ColorWheel(
                modifier = Modifier.fillMaxSize(),
                brightness = 0.7f,
                onColorSelected = { }
            )
        }
    }
}