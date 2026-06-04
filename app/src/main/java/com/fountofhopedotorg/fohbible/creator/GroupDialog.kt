package com.fountofhopedotorg.fohbible.creator

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun GroupDialog(
    show: Boolean,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (show) {
        var groupName by remember { mutableStateOf(initialName) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Group Selected Elements") },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = { onConfirm(groupName) }) { Text("Create Group") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}