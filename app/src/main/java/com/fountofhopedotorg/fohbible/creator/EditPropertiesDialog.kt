package com.fountofhopedotorg.fohbible.creator

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.composables.ColorWheelDialog

@Composable
fun EditPropertiesDialog(
    show: Boolean,
    noteId: String?,
    initialX: String,
    initialY: String,
    initialScaleX: String,
    initialScaleY: String,
    initialRotation: String,
    initialColor: Color,
    proportionalEnabled: Boolean,
    onProportionalToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onApply: (
        noteId: String,
        x: String,
        y: String,
        scaleX: String,
        scaleY: String,
        rotation: String,
        color: Color
    ) -> Unit
) {
    if (show && noteId != null) {
        key(noteId) {
            val normalizedInitialRotation = remember(true, initialRotation) {
                val degrees = initialRotation.toDoubleOrNull() ?: 0.0
                ((degrees % 360) + 360) % 360
            }.toString()

            var editX by remember(show, initialX) { mutableStateOf(initialX) }
            var editY by remember(show, initialY) { mutableStateOf(initialY) }
            var editScaleX by remember(show, initialScaleX) { mutableStateOf(initialScaleX) }
            var editScaleY by remember(show, initialScaleY) { mutableStateOf(initialScaleY) }
            var editRotation by remember(show, normalizedInitialRotation) { mutableStateOf(normalizedInitialRotation) }
            var editColor by remember(show, initialColor) { mutableStateOf(initialColor) }
            var showEditColorPicker by remember { mutableStateOf(false) }
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
                                if (parsed != null && parsed in 0.1f..10f) {
                                    editScaleX = newValue
                                    if (proportionalEnabled) {
                                        editScaleY = newValue
                                    }
                                } else if (newValue.isEmpty() || newValue == "." || newValue == "-") {
                                    editScaleX = newValue
                                }
                            },
                            label = { Text("Scale X (0.1 – 10)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = editScaleY,
                            onValueChange = { newValue ->
                                val parsed = newValue.toFloatOrNull()
                                if (parsed != null && parsed in 0.1f..10f) {
                                    editScaleY = newValue
                                    if (proportionalEnabled) {
                                        editScaleX = newValue
                                    }
                                } else if (newValue.isEmpty() || newValue == "." || newValue == "-") {
                                    editScaleY = newValue
                                }
                            },
                            label = { Text("Scale Y (0.1 – 10)") },
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

                        OutlinedTextField(
                            value = " ",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Color") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(32.dp)
                                        .background(editColor, RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                        .clickable { showEditColorPicker = true }
                                )
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val rotationValue = try {
                            val input = editRotation.toDouble()
                            ((input % 360) + 360) % 360
                        } catch (_: NumberFormatException) { 0.0 }
                        val rotationToApply = rotationValue.toString()

                        onApply(
                            noteId,
                            editX,
                            editY,
                            editScaleX,
                            editScaleY,
                            rotationToApply,
                            editColor
                        )
                    }) { Text("Apply") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )

            if (showEditColorPicker) {
                ColorWheelDialog(
                    onDismissRequest = { showEditColorPicker = false },
                    onColorSelected = { selectedColor ->
                        editColor = selectedColor
                        showEditColorPicker = false
                    },
                    initialColor = editColor
                )
            }
        }
    }
}