package com.fountofhopedotorg.fohbible.creator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddTextSection(
    currentText: String,
    onTextChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = currentText,
                onValueChange = onTextChange,
                label = { Text("Enter text") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}