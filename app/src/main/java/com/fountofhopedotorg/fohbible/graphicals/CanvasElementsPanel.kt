package com.fountofhopedotorg.fohbible.graphicals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.DisplayItem
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
    onToggleGroupSelection: (CanvasNote) -> Unit,
    onGroupHeaderTap: (String) -> Unit,
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
    density: Density,
    groupNames: Map<String, String> = emptyMap(),
    onRenameGroup: ((groupId: String, currentName: String) -> Unit)? = null
) {
    val groupedNotes = viewModel.canvasNotes.groupBy { it.groupId }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val displayItems = buildList {
        for ((groupId, notes) in groupedNotes) {
            if (groupId == null) continue
            val expanded = expandedGroups[groupId] ?: false

            val actualChildren = notes.filter { note ->
                note.id != groupId
            }

            val groupName = groupNames[groupId] ?: "Group of ${actualChildren.size}"

            add(
                DisplayItem.GroupHeader(
                    groupId = groupId,
                    groupName = groupName,
                    memberCount = actualChildren.size,
                    isExpanded = expanded
                )
            )
            actualChildren.forEach { note ->
                val originalIndex = viewModel.canvasNotes.indexOf(note)
                add(
                    DisplayItem.NoteItem(
                        note = note,
                        originalIndex = originalIndex,
                        isGrouped = true,
                        groupId = groupId
                    )
                )
            }
        }
        val ungrouped = groupedNotes[null] ?: emptyList()
        ungrouped.forEach { note ->
            val originalIndex = viewModel.canvasNotes.indexOf(note)
            add(DisplayItem.NoteItem(note, originalIndex, isGrouped = false))
        }
    }

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
                if (displayItems.isEmpty()) {
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
                        items(
                            count = displayItems.size,
                            key = { index ->
                                when (val item = displayItems[index]) {
                                    is DisplayItem.GroupHeader -> "header-${item.groupId}"
                                    is DisplayItem.NoteItem -> item.note.id
                                }
                            }
                        ) { displayIndex ->
                            when (val displayItem = displayItems[displayIndex]) {
                                is DisplayItem.GroupHeader -> {
                                    GroupHeaderRow(
                                        groupName = displayItem.groupName,
                                        isExpanded = displayItem.isExpanded,
                                        onToggleExpand = {
                                            expandedGroups[displayItem.groupId] =
                                                !(expandedGroups[displayItem.groupId] ?: false)
                                        },
                                        onTap = { onGroupHeaderTap(displayItem.groupId) },
                                        themeColors = themeColors
                                    )
                                }

                                is DisplayItem.NoteItem -> {
                                    val note = displayItem.note
                                    val originalIndex = displayItem.originalIndex
                                    val isGrouped = displayItem.isGrouped
                                    val groupId = displayItem.groupId
                                    val isVisible = if (isGrouped && groupId != null) {
                                        expandedGroups[groupId] ?: false
                                    } else {
                                        true
                                    }

                                    AnimatedVisibility(
                                        visible = isVisible,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        val itemModifier = Modifier
                                            .animateItem()
                                            .then(
                                                if (isGrouped) Modifier.padding(start = 16.dp)
                                                else Modifier
                                            )

                                        CanvasElementItem(
                                            viewModel = viewModel,
                                            note = note,
                                            originalIndex = originalIndex,
                                            isSelected = selectedNoteId == note.id,
                                            isDragTarget = draggedIndex == originalIndex,
                                            isUpEnabled = !isGrouped && originalIndex > 0,
                                            isDownEnabled = !isGrouped && originalIndex < viewModel.canvasNotes.size - 1,
                                            dragOffset = if (draggedIndex == originalIndex) dragOffset else 0f,
                                            selectedNoteIds = selectedNoteIds,
                                            isGrouped = isGrouped,
                                            onRowTap = { onSingleSelect(note) },
                                            onToggleGroupSelection = { onToggleGroupSelection(note) },
                                            onDragStart = { offset ->
                                                if (!isGrouped) {
                                                    draggedIndex = originalIndex
                                                    dragOffset = offset.y
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                if (!isGrouped) {
                                                    change.consume()
                                                    dragOffset += dragAmount.y
                                                }
                                            },
                                            onDragEnd = {
                                                if (!isGrouped) {
                                                    val fromIndex = draggedIndex
                                                    if (fromIndex != null) {
                                                        val target =
                                                            (fromIndex + (dragOffset / itemHeightPx).roundToInt())
                                                                .coerceIn(
                                                                    0,
                                                                    viewModel.canvasNotes.size - 1
                                                                )
                                                        if (target != fromIndex) {
                                                            viewModel.reorderCanvasNotes(
                                                                fromIndex,
                                                                target
                                                            )
                                                        }
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
                            }
                        }
                    }

                    AnimatedVisibility(visible = selectedNoteIds.isNotEmpty()) {
                        val hasGroup = viewModel.canvasNotes.any { it.groupId != null && it.id in selectedNoteIds }
                        GroupActionRow(
                            selectedCount = selectedNoteIds.size,
                            hasGroup = hasGroup,
                            onGroup = {
                                onGroup("Group ${selectedNoteIds.size}", selectedNoteIds.toList())
                            },
                            onUngroup = { onUngroup(selectedNoteIds) },
                            onRename = {
                                val selectedNotes = viewModel.canvasNotes.filter { it.id in selectedNoteIds }
                                val uniqueGroupIds = selectedNotes.mapNotNull { it.groupId }.distinct()

                                if (hasGroup && uniqueGroupIds.isNotEmpty()) {
                                    if (onRenameGroup != null) {
                                        val groupId = uniqueGroupIds.first()
                                        val currentName = groupNames[groupId] ?: "Group"
                                        onRenameGroup(groupId, currentName)
                                    }
                                } else if (selectedNoteIds.size == 1) {
                                    val note = selectedNotes.firstOrNull()
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

@Composable
private fun GroupHeaderRow(
    groupName: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTap: () -> Unit,
    themeColors: ThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                contentDescription = "Toggle group",
                modifier = Modifier
                    .clickable { onToggleExpand() }
                    .size(20.dp),
                tint = themeColors.primary
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Group",
                modifier = Modifier.size(16.dp),
                tint = themeColors.primary
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = groupName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = themeColors.primary
            )
        }
    }
}