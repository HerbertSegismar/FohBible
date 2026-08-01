package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.color_wheel.ColorWheelDialog
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.availableFontFamilies
import java.util.Locale

private fun parseFloatSafe(value: String): Float {
    return value.replace(',', '.').toFloatOrNull() ?: 0f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatorEditPropertiesDialog(
    show: Boolean,
    elementId: String?,
    initialX: String,
    initialY: String,
    initialScaleX: String,
    initialScaleY: String,
    initialRotation: String,
    initialColor: Color,
    proportionalEnabled: Boolean,
    onProportionalToggle: (Boolean) -> Unit,
    initialShadowColor: Color? = null,
    initialShadowOffsetX: Float = 2f,
    initialShadowOffsetY: Float = 2f,
    initialBorderThickness: Float = 2f,
    initialBorderColor: Color? = null,
    initialGradientConfig: GradientConfig? = null,
    initialFontFamily: String? = null,
    initialTextAlign: String? = null,
    isTextElement: Boolean = false,
    initialPivotX: Float = 0.5f,
    initialPivotY: Float = 0.5f,
    onDismiss: () -> Unit,
    onApply: (
        elementId: String,
        x: String,
        y: String,
        scaleX: String,
        scaleY: String,
        rotation: String,
        color: Color,
        shadowColor: Color?,
        shadowOffsetX: Float,
        shadowOffsetY: Float,
        borderThickness: Float,
        borderColor: Color?,
        gradientConfig: GradientConfig?,
        fontFamily: String?,
        textAlign: String?,
        pivotX: Float,
        pivotY: Float
    ) -> Unit
) {
    val viewModel: AppViewModel = viewModel()
    if (show && elementId != null) {
        key(elementId) {
            val normalizedInitialRotation = remember(true, initialRotation) {
                val degrees = initialRotation.toDoubleOrNull() ?: 0.0
                ((degrees % 360) + 360) % 360
            }.toString()

            var editX by remember(show, initialX) { mutableStateOf(formatPosition(initialX.toFloatOrNull() ?: 0f)) }
            var editY by remember(show, initialY) { mutableStateOf(formatPosition(initialY.toFloatOrNull() ?: 0f)) }
            var editScaleX by remember(show, initialScaleX) { mutableStateOf(formatScale(initialScaleX.toFloatOrNull() ?: 1f)) }
            var editScaleY by remember(show, initialScaleY) { mutableStateOf(formatScale(initialScaleY.toFloatOrNull() ?: 1f)) }
            var editRotation by remember(show, normalizedInitialRotation) { mutableStateOf(formatRotation(normalizedInitialRotation.toFloatOrNull() ?: 0f)) }
            var editShadowOffsetX by remember(show, initialShadowOffsetX) { mutableStateOf(String.format(Locale.US, "%.1f", initialShadowOffsetX)) }
            var editShadowOffsetY by remember(show, initialShadowOffsetY) { mutableStateOf(String.format(Locale.US, "%.1f", initialShadowOffsetY)) }
            var editBorderThickness by remember(show, initialBorderThickness) { mutableStateOf(String.format(Locale.US, "%.1f", initialBorderThickness)) }

            var editPivotX by remember(show, initialPivotX) { mutableStateOf(String.format(Locale.US, "%.3f", initialPivotX)) }
            var editPivotY by remember(show, initialPivotY) { mutableStateOf(String.format(Locale.US, "%.3f", initialPivotY)) }

            var editBorderColor by remember(show, initialBorderColor) { mutableStateOf(initialBorderColor) }
            var editColor by remember(show, initialColor) { mutableStateOf(initialColor) }
            var showEditColorPicker by remember { mutableStateOf(false) }
            var editShadowColor by remember(show, initialShadowColor) { mutableStateOf(initialShadowColor) }
            var showShadowColorPicker by remember { mutableStateOf(false) }
            var showBorderColorPicker by remember { mutableStateOf(false) }
            var editGradientConfig by remember(show, initialGradientConfig) {
                mutableStateOf(initialGradientConfig)
            }

            var editFontFamily by remember(show, initialFontFamily) { mutableStateOf(initialFontFamily ?: "system") }
            var editTextAlign by remember(show, initialTextAlign) { mutableStateOf(initialTextAlign ?: "Center") }

            val scrollState = rememberScrollState()

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Edit Element Properties") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editX,
                            onValueChange = { editX = it },
                            label = { Text("X Position") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = editY,
                            onValueChange = { editY = it },
                            label = { Text("Y Position") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Proportional Scaling", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = proportionalEnabled,
                                onCheckedChange = onProportionalToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        OutlinedTextField(
                            value = editScaleX,
                            onValueChange = { newValue ->
                                val parsed = newValue.toFloatOrNull()
                                if (parsed != null && parsed in 0.05f..25f) {
                                    editScaleX = newValue
                                    if (proportionalEnabled) {
                                        editScaleY = newValue
                                    }
                                } else if (newValue.isEmpty() || newValue == "." || newValue == "-") {
                                    editScaleX = newValue
                                }
                            },
                            label = { Text("Scale X (0.05 – 25)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = editScaleY,
                            onValueChange = { newValue ->
                                val parsed = newValue.toFloatOrNull()
                                if (parsed != null && parsed in 0.05f..25f) {
                                    editScaleY = newValue
                                    if (proportionalEnabled) {
                                        editScaleX = newValue
                                    }
                                } else if (newValue.isEmpty() || newValue == "." || newValue == "-") {
                                    editScaleY = newValue
                                }
                            },
                            label = { Text("Scale Y (0.05 – 25)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        OutlinedTextField(
                            value = editRotation,
                            onValueChange = { editRotation = it },
                            label = { Text("Rotation Angle (degrees)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Text("Pivot", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editPivotX,
                                onValueChange = { editPivotX = it },
                                label = { Text("X") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editPivotY,
                                onValueChange = { editPivotY = it },
                                label = { Text("Y") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = " ",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Color") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
                            leadingIcon = {
                                val density = LocalDensity.current
                                val swatchSizePx = with(density) { 32.dp.toPx() }
                                Box(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(32.dp)
                                        .background(
                                            brush = if (editGradientConfig != null) {
                                                val cfg = editGradientConfig!!
                                                val startPx = Offset(cfg.startOffset.x * swatchSizePx, cfg.startOffset.y * swatchSizePx)
                                                val endPx = Offset(cfg.endOffset.x * swatchSizePx, cfg.endOffset.y * swatchSizePx)
                                                Brush.linearGradient(
                                                    colors = listOf(cfg.startColor, cfg.endColor),
                                                    start = startPx,
                                                    end = endPx
                                                )
                                            } else {
                                                Brush.horizontalGradient(listOf(editColor, editColor))
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                        .clickable { showEditColorPicker = true }
                                )
                            }
                        )

                        Text("Shadow", style = MaterialTheme.typography.titleSmall)

                        OutlinedTextField(
                            value = " ",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Shadow Color") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(32.dp)
                                        .background(editShadowColor ?: Color.Transparent, RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                        .clickable { showShadowColorPicker = true }
                                )
                            }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editShadowOffsetX,
                                onValueChange = { editShadowOffsetX = it },
                                label = { Text("Offset X") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editShadowOffsetY,
                                onValueChange = { editShadowOffsetY = it },
                                label = { Text("Offset Y") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("Border", style = MaterialTheme.typography.titleSmall)

                        OutlinedTextField(
                            value = " ",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Border Color") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(32.dp)
                                        .background(editBorderColor ?: Color.Transparent, RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                        .clickable { showBorderColorPicker = true }
                                )
                            }
                        )

                        OutlinedTextField(
                            value = editBorderThickness,
                            onValueChange = { editBorderThickness = it },
                            label = { Text("Thickness") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        if (isTextElement) {
                            Text("Text Style", style = MaterialTheme.typography.titleSmall)

                            var fontExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = fontExpanded,
                                onExpandedChange = { fontExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = editFontFamily,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Font") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = fontExpanded,
                                    onDismissRequest = { fontExpanded = false }
                                ) {
                                    availableFontFamilies.forEach { font ->
                                        DropdownMenuItem(
                                            text = { Text(font) },
                                            onClick = {
                                                editFontFamily = font
                                                fontExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }

                            Text("Alignment")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val alignments = listOf("Left", "Center", "Right")
                                alignments.forEach { align ->
                                    TextButton(
                                        onClick = { editTextAlign = align },
                                        modifier = Modifier
                                            .background(
                                                if (editTextAlign == align) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                            )
                                    ) {
                                        Text(
                                            align,
                                            color = if (editTextAlign == align) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val rotationValue = try {
                            val input = editRotation.toDouble()
                            ((input % 360) + 360) % 360
                        } catch (_: NumberFormatException) { 0.0 }
                        val rotationToApply = rotationValue.toString()
                        val shadowColor = editShadowColor

                        val shadowOffsetX = parseFloatSafe(editShadowOffsetX)
                        val shadowOffsetY = parseFloatSafe(editShadowOffsetY)
                        val borderThickness = parseFloatSafe(editBorderThickness)
                        val borderColor = editBorderColor

                        val pivotXValue = parseFloatSafe(editPivotX)
                        val pivotYValue = parseFloatSafe(editPivotY)

                        onApply(
                            elementId,
                            editX,
                            editY,
                            editScaleX,
                            editScaleY,
                            rotationToApply,
                            editColor,
                            shadowColor,
                            shadowOffsetX,
                            shadowOffsetY,
                            borderThickness,
                            borderColor,
                            editGradientConfig,
                            editFontFamily,
                            editTextAlign,
                            pivotXValue,
                            pivotYValue
                        )
                    }) { Text("Apply") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )

            if (showEditColorPicker) {
                viewModel.animatorColorWheel = true
                ColorWheelDialog(
                    onDismissRequest = { showEditColorPicker = false; viewModel.animatorColorWheel = false },
                    onColorSelected = { selectedColor ->
                        editColor = selectedColor
                        editGradientConfig = null
                        showEditColorPicker = false
                    },
                    initialColor = editColor,
                    enableGradient = true,
                    onGradientSelected = { startColor, endColor, startOffset, endOffset ->
                        editGradientConfig = GradientConfig(startColor, endColor, startOffset, endOffset)
                        editColor = startColor
                        showEditColorPicker = false
                    },
                    initialGradientConfig = editGradientConfig
                )
            }

            if (showShadowColorPicker) {
                ColorWheelDialog(
                    onDismissRequest = { showShadowColorPicker = false },
                    onColorSelected = { selectedColor ->
                        editShadowColor = selectedColor
                        showShadowColorPicker = false
                    },
                    initialColor = editShadowColor ?: Color.Black
                )
            }
            if (showBorderColorPicker) {
                ColorWheelDialog(
                    onDismissRequest = { showBorderColorPicker = false },
                    onColorSelected = { selectedColor ->
                        editBorderColor = selectedColor
                        showBorderColorPicker = false
                    },
                    initialColor = editBorderColor ?: Color.Black
                )
            }
        }
    }
}