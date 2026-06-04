package com.fountofhopedotorg.fohbible.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GroupActionRow(
    selectedCount: Int,
    hasGroup: Boolean,
    onGroup: () -> Unit,
    onUngroup: () -> Unit,
    onRename: () -> Unit,
    onEditProperties: () -> Unit,
    onClearSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onGroup,
            enabled = selectedCount > 1 && !hasGroup
        ) {
            Icon(Icons.Default.Group, contentDescription = "Group")
        }
        IconButton(
            onClick = onUngroup,
            enabled = hasGroup
        ) {
            Icon(Icons.Default.GroupRemove, contentDescription = "Ungroup")
        }

        IconButton(
            onClick = onRename,
            enabled = selectedCount == 1 || hasGroup
        ) {
            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename")
        }

        IconButton(
            onClick = onEditProperties,
            enabled = selectedCount == 1
        ) {
            Icon(Icons.Default.Transform, contentDescription = "Edit Properties")
        }
        IconButton(onClick = onClearSelection) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}