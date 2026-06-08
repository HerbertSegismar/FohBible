package com.fountofhopedotorg.fohbible.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.composables.ColorWheelDialog

@Composable
fun EditPropertiesDialog(
    show: Boolean,
    noteId: String?,
    initialX: String,
    initialY: String,
    initialWidth: String,
    initialHeight: String,
    initialRotation: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onApply: (String, String, String, String, String, String, Color) -> Unit
) {
    if (show && noteId != null) {
        key(noteId) {
            var editX by remember { mutableStateOf(initialX) }
            var editY by remember { mutableStateOf(initialY) }
            var editWidth by remember { mutableStateOf(initialWidth) }
            var editHeight by remember { mutableStateOf(initialHeight) }
            var editRotation by remember { mutableStateOf(initialRotation) }
            var editColor by remember { mutableStateOf(initialColor) }
            var showEditColorPicker by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Edit Element Properties") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editX,
                            onValueChange = { editX = it },
                            label = { Text("X Position") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editY,
                            onValueChange = { editY = it },
                            label = { Text("Y Position") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editWidth,
                            onValueChange = { editWidth = it },
                            label = { Text("Width") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editHeight,
                            onValueChange = { editHeight = it },
                            label = { Text("Height") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editRotation,
                            onValueChange = { editRotation = it },
                            label = { Text("Rotation Angle (degrees)") },
                            singleLine = true
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
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { showEditColorPicker = true }
                                )
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onApply(noteId, editX, editY, editWidth, editHeight, editRotation, editColor)
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