package com.fountofhopedotorg.fohbible.color_wheel

import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.modal_functions.ColorOptionItem
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.ui.theme.PredefinedColorThemes
import com.fountofhopedotorg.fohbible.ui.theme.ThemeManager.primaryColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import android.graphics.Color as AndroidColor


@Composable
fun ColorPickerRow(
    label: String,
    iconSize: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenuItem(
        text = { Text(label, modifier = Modifier.fillMaxWidth()) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(iconSize.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
            )
        },
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun ColorThemeDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onCustomColorClick: () -> Unit,
    appViewModel: AppViewModel
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    Card(
        modifier = Modifier
            .fillMaxWidth().fillMaxHeight(if (isLandscape) 1f else 0.75f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appViewModel.darkTheme) appViewModel.darkModalBackgroundColor else appViewModel.lightModalBackgroundColor
        )
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor)) {
                Row(modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 20.dp).background(LocalAppTheme.current.primaryColor),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Choose Theme Color",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) { Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    ) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(PredefinedColorThemes) { theme ->
                    ColorOptionItem(theme = theme, onClick = { onColorSelected(theme.primaryColor); onDismiss() })
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Custom Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable(onClick = onCustomColorClick),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            colors = listOf(
                                                Color.Red,
                                                Color.Yellow,
                                                Color.Green,
                                                Color.Cyan,
                                                Color.Blue,
                                                Color.Magenta,
                                                Color.Red
                                            )
                                        )
                                    )
                                    .border(2.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Custom Color Picker", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Choose any color with color wheel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(end = 20.dp, bottom = 20.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)) {
                    Text("Cancel")
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