package com.fountofhopedotorg.fohbible.modals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.settings.MAX_ORB_COUNT
import com.fountofhopedotorg.fohbible.settings.MIN_ORB_COUNT

@Composable
fun OrbsCountModal(
    tempSize: String,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    appViewModel: AppViewModel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (appViewModel.darkTheme)
            appViewModel.darkModalBackgroundColor
        else
            appViewModel.lightModalBackgroundColor,
        title = {
            Text(
                "Orbs Count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Enter the number of Orbs you want ($MIN_ORB_COUNT - $MAX_ORB_COUNT):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tempSize,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            onChange(newValue)
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter number of Orbs") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = tempSize.toIntOrNull()?.let { it in MIN_ORB_COUNT..MAX_ORB_COUNT } ?: false
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}