package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CanvasSizeDialog(
    initialWidth: Int,
    initialHeight: Int,
    onDismiss: () -> Unit,
    onConfirm: (width: Int, height: Int) -> Unit
) {
    var widthText by remember { mutableStateOf(initialWidth.toString()) }
    var heightText by remember { mutableStateOf(initialHeight.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Canvas Size (px)") },
        text = {
            Column {
                OutlinedTextField(
                    value = widthText,
                    onValueChange = { widthText = it.filter { c -> c.isDigit() } },
                    label = { Text("Width") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { c -> c.isDigit() } },
                    label = { Text("Height") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = widthText.toIntOrNull()
                val h = heightText.toIntOrNull()
                if (w != null && h != null && w > 0 && h > 0) {
                    onConfirm(w, h)
                } else {
                    errorMessage = "Please enter positive integers."
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}