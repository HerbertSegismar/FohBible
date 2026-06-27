package com.fountofhopedotorg.fohbible.gfx_creator

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SaveAsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSavePng: () -> Unit,
    onSaveJpg: () -> Unit,
    onSavePdf: () -> Unit,
    onSaveSvg: () -> Unit
) {
    Box {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                text = { Text("PNG") },
                onClick = {
                    onDismiss()
                    onSavePng()
                }
            )
            DropdownMenuItem(
                text = { Text("JPG") },
                onClick = {
                    onDismiss()
                    onSaveJpg()
                }
            )
            DropdownMenuItem(
                text = { Text("PDF") },
                onClick = {
                    onDismiss()
                    onSavePdf()
                }
            )
            DropdownMenuItem(
                text = { Text("SVG") },
                onClick = {
                    onDismiss()
                    onSaveSvg()
                }
            )
        }
    }
}