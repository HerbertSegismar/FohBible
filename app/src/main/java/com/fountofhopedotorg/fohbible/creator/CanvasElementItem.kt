package com.fountofhopedotorg.fohbible.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel

@Composable
fun CanvasElementItem(
    viewModel: AppViewModel,
    note: CanvasNote,
    originalIndex: Int,
    isSelected: Boolean,
    isDragTarget: Boolean,
    isUpEnabled: Boolean,
    isDownEnabled: Boolean,
    dragOffset: Float,
    selectedNoteIds: Set<String>,
    isGrouped: Boolean,
    onRowTap: () -> Unit,
    onToggleGroupSelection: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onEdit: () -> Unit,
    onCustomPolygonEdit: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    themeColors: ThemeColors,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .graphicsLayer { if (isDragTarget) translationY = dragOffset }
            .then(
                if (!isGrouped) {
                    Modifier.pointerInput(note.id, originalIndex) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset -> onDragStart(offset) },
                            onDrag = { change, dragAmount -> onDrag(change, dragAmount) },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel
                        )
                    }
                } else Modifier
            )
            .background(
                color = if (isSelected) themeColors.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleGroupSelection() }
                    .then(
                        if (selectedNoteIds.contains(note.id)) {
                            Modifier.border(2.dp, themeColors.primary, RoundedCornerShape(6.dp))
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                NoteThumbnail(note, themeColors)
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = getElementDisplayName(note, originalIndex, viewModel.canvasNotes),
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRowTap() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            ReorderHandle(
                originalIndex = originalIndex,
                isUpEnabled = isUpEnabled,
                isDownEnabled = isDownEnabled,
                onReorder = { from, to -> viewModel.reorderCanvasNotes(from, to) },
                primaryColor = themeColors.primary
            )
            IconButton(onClick = onVisibilityToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (note.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (note.isVisible) "Hide Element" else "Show Element",
                    tint = if (note.isVisible) themeColors.primary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onLockToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (note.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (note.isLocked) "Unlock Element" else "Lock Element",
                    tint = if (note.isLocked) Color.Gray else themeColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = {
                if (note.content.startsWith("Shape:")) {
                    onCustomPolygonEdit()
                } else {
                    onEdit()
                }
            }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = themeColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Duplicate",
                    tint = themeColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}