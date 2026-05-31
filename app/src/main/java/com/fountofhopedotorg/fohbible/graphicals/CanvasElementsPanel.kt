package com.fountofhopedotorg.fohbible.graphicals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel
import kotlin.math.roundToInt

@Composable
fun CanvasElementsPanel(
    viewModel: AppViewModel,
    selectedNoteIds: Set<String>,
    selectedNoteId: String?,
    showTree: Boolean,
    onToggleTree: () -> Unit,
    onSingleSelect: (CanvasNote) -> Unit,
    onNoteTap: (CanvasNote) -> Unit,
    onToggleGroupSelection: (CanvasNote) -> Unit,
    onEditNote: (CanvasNote) -> Unit,
    onCustomPolygonEdit: (CanvasNote) -> Unit,
    onRename: (CanvasNote) -> Unit,
    onEditProperties: (CanvasNote) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDuplicate: (CanvasNote) -> Unit,
    onDelete: (CanvasNote) -> Unit,
    onUngroup: (Set<String>) -> Unit,
    onGroup: (String, List<String>) -> Unit,
    onClearSelection: () -> Unit,
    themeColors: ThemeColors,
    density: Density
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleTree() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Canvas Elements", style = MaterialTheme.typography.titleMedium)
            Icon(
                imageVector = if (showTree) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                contentDescription = if (showTree) "Collapse" else "Expand",
                tint = themeColors.textColor
            )
        }

        AnimatedVisibility(visible = showTree) {
            Column {
                if (viewModel.canvasNotes.isEmpty()) {
                    Text(
                        "No elements on canvas yet.",
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    var draggedIndex by remember { mutableStateOf<Int?>(null) }
                    var dragOffset by remember { mutableFloatStateOf(0f) }
                    val itemHeightPx = remember(density) { with(density) { 56.dp.toPx() } }
                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.heightIn(max = 500.dp)
                    ) {
                        itemsIndexed(
                            viewModel.canvasNotes,
                            key = { _, note -> note.id }
                        ) { index, note ->
                            val isSelected = selectedNoteId == note.id
                            val isUpEnabled = index > 0
                            val isDownEnabled = index < viewModel.canvasNotes.size - 1
                            val itemModifier = Modifier.animateItem()

                            CanvasElementItem(
                                viewModel = viewModel,
                                note = note,
                                index = index,
                                isSelected = isSelected,
                                isDragTarget = draggedIndex == index,
                                isUpEnabled = isUpEnabled,
                                isDownEnabled = isDownEnabled,
                                dragOffset = if (draggedIndex == index) dragOffset else 0f,
                                selectedNoteIds = selectedNoteIds,
                                onRowTap = { onSingleSelect(note) },
                                onToggleGroupSelection = { onToggleGroupSelection(note) },
                                onDragStart = { offset ->
                                    draggedIndex = index
                                    dragOffset = offset.y
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y
                                },
                                onDragEnd = {
                                    val fromIndex = draggedIndex
                                    if (fromIndex != null) {
                                        val target = (fromIndex + (dragOffset / itemHeightPx).roundToInt())
                                            .coerceIn(0, viewModel.canvasNotes.size - 1)
                                        if (target != fromIndex) {
                                            viewModel.reorderCanvasNotes(fromIndex, target)
                                        }
                                    }
                                    draggedIndex = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffset = 0f
                                },
                                onEdit = { onEditNote(note) },
                                onCustomPolygonEdit = { onCustomPolygonEdit(note) },
                                onVisibilityToggle = { onToggleVisibility(note.id) },
                                onLockToggle = { onToggleLock(note.id) },
                                onDuplicate = { onDuplicate(note) },
                                onDelete = { onDelete(note) },
                                themeColors = themeColors,
                                density = density,
                                modifier = itemModifier
                            )
                        }
                    }

                    AnimatedVisibility(visible = selectedNoteIds.isNotEmpty()) {
                        GroupActionRow(
                            selectedCount = selectedNoteIds.size,
                            hasGroup = viewModel.canvasNotes.any { it.groupId != null && it.id in selectedNoteIds },
                            onGroup = {
                                onGroup(
                                    "Group ${selectedNoteIds.size}",
                                    selectedNoteIds.toList()
                                )
                            },
                            onUngroup = { onUngroup(selectedNoteIds) },
                            onRename = {
                                if (selectedNoteIds.size == 1) {
                                    val note = viewModel.canvasNotes.find { it.id == selectedNoteIds.first() }
                                    if (note != null) onRename(note)
                                }
                            },
                            onEditProperties = {
                                if (selectedNoteIds.size == 1) {
                                    val note = viewModel.canvasNotes.find { it.id == selectedNoteIds.first() }
                                    if (note != null) onEditProperties(note)
                                }
                            },
                            onClearSelection = onClearSelection
                        )
                    }
                }
            }
        }
    }
}