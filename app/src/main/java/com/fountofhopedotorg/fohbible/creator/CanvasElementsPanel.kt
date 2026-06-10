package com.fountofhopedotorg.fohbible.creator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    // Build the base list of group headers and notes
    val baseItems = buildList {
        for ((groupId, notes) in groupedNotes) {
            if (groupId == null) continue
            val expanded = expandedGroups[groupId] ?: false
            val actualChildren = notes.filter { note -> note.id != groupId }
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

    // Insert the action row after the last visible selected note (if any)
    val displayItems = remember(baseItems, expandedGroups, selectedNoteIds) {
        val mutableList = baseItems.toMutableList()
        var insertionIndex = -1
        for (i in mutableList.indices.reversed()) {
            val item = mutableList[i]
            if (item is DisplayItem.NoteItem &&
                selectedNoteIds.contains(item.note.id) &&
                (item.groupId == null || expandedGroups[item.groupId] == true)
            ) {
                insertionIndex = i + 1
                break
            }
        }
        if (insertionIndex >= 0) {
            mutableList.add(insertionIndex, DisplayItem.ActionRow)
        }
        mutableList
    }

    // Group bounds for reordering (unchanged)
    val groupBounds = remember(displayItems) {
        val bounds = mutableMapOf<String?, Pair<Int, Int>>()
        displayItems.forEachIndexed { index, item ->
            if (item is DisplayItem.NoteItem) {
                val current = bounds[item.groupId]
                if (current == null) {
                    bounds[item.groupId] = index to index
                } else {
                    bounds[item.groupId] = current.first to index
                }
            }
        }
        bounds
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
                .clickable { onToggleTree() },
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
                    var draggedDisplayIndex by remember { mutableStateOf<Int?>(null) }
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
                                    is DisplayItem.ActionRow -> "action-row"
                                }
                            }
                        ) { displayIndex ->
                            when (val displayItem = displayItems[displayIndex]) {
                                is DisplayItem.GroupHeader -> {
                                    val isGroupSelected = selectedNoteId == displayItem.groupId ||
                                            selectedNoteIds.contains(displayItem.groupId) ||
                                            viewModel.canvasNotes.any {
                                                it.groupId == displayItem.groupId &&
                                                        (it.id == selectedNoteId || selectedNoteIds.contains(it.id))
                                            }

                                    GroupHeaderRow(
                                        groupName = displayItem.groupName,
                                        isExpanded = displayItem.isExpanded,
                                        isSelected = isGroupSelected,
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
                                        exit = shrinkVertically() + fadeOut(),
                                        modifier = Modifier.animateItem()
                                    ) {
                                        val itemModifier = if (isGrouped) Modifier.padding(start = 16.dp) else Modifier
                                        val bounds = groupBounds[groupId]
                                        val isUpEnabled = bounds != null && displayIndex > bounds.first
                                        val isDownEnabled = bounds != null && displayIndex < bounds.second

                                        CanvasElementItem(
                                            viewModel = viewModel,
                                            note = note,
                                            originalIndex = originalIndex,
                                            isSelected = selectedNoteId == note.id,
                                            isDragTarget = draggedDisplayIndex == displayIndex,
                                            isUpEnabled = isUpEnabled,
                                            isDownEnabled = isDownEnabled,
                                            dragOffset = if (draggedDisplayIndex == displayIndex) dragOffset else 0f,
                                            selectedNoteIds = selectedNoteIds,
                                            isGrouped = isGrouped,
                                            onRowTap = { onSingleSelect(note) },
                                            onToggleGroupSelection = { onToggleGroupSelection(note) },
                                            onDragStart = { offset ->
                                                draggedDisplayIndex = displayIndex
                                                dragOffset = offset.y
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount.y
                                            },
                                            onDragEnd = {
                                                val fromDisplayIndex = draggedDisplayIndex
                                                if (fromDisplayIndex != null) {
                                                    val draggedItem =
                                                        displayItems[fromDisplayIndex] as? DisplayItem.NoteItem
                                                    if (draggedItem != null) {
                                                        val itemBounds = groupBounds[draggedItem.groupId]
                                                        if (itemBounds != null) {
                                                            val rawTargetIdx =
                                                                (fromDisplayIndex + (dragOffset / itemHeightPx).roundToInt())
                                                            val targetDisplayIdx = rawTargetIdx.coerceIn(
                                                                itemBounds.first,
                                                                itemBounds.second
                                                            )

                                                            if (targetDisplayIdx != fromDisplayIndex) {
                                                                val targetItem =
                                                                    displayItems[targetDisplayIdx] as DisplayItem.NoteItem
                                                                viewModel.reorderCanvasNotes(
                                                                    draggedItem.originalIndex,
                                                                    targetItem.originalIndex
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                draggedDisplayIndex = null
                                                dragOffset = 0f
                                            },
                                            onDragCancel = {
                                                draggedDisplayIndex = null
                                                dragOffset = 0f
                                            },
                                            onEdit = { onEditNote(note) },
                                            onCustomPolygonEdit = { onCustomPolygonEdit(note) },
                                            onVisibilityToggle = { onToggleVisibility(note.id) },
                                            onLockToggle = { onToggleLock(note.id) },
                                            onDuplicate = { onDuplicate(note) },
                                            onDelete = { onDelete(note) },
                                            themeColors = themeColors,
                                            modifier = itemModifier
                                        )
                                    }
                                }

                                is DisplayItem.ActionRow -> {
                                    val hasGroup = viewModel.canvasNotes.any {
                                        it.groupId != null && it.id in selectedNoteIds
                                    }
                                    GroupActionRow(
                                        selectedCount = selectedNoteIds.size,
                                        hasGroup = hasGroup,
                                        onGroup = {
                                            onGroup("Group ${selectedNoteIds.size}", selectedNoteIds.toList())
                                        },
                                        onUngroup = { onUngroup(selectedNoteIds) },
                                        onRename = {
                                            val selectedNotes =
                                                viewModel.canvasNotes.filter { it.id in selectedNoteIds }
                                            val uniqueGroupIds = selectedNotes.mapNotNull { it.groupId }.distinct()
                                            if (hasGroup && uniqueGroupIds.isNotEmpty()) {
                                                onRenameGroup?.invoke(
                                                    uniqueGroupIds.first(),
                                                    groupNames[uniqueGroupIds.first()] ?: "Group"
                                                )
                                            } else if (selectedNoteIds.size == 1) {
                                                val note = selectedNotes.firstOrNull()
                                                if (note != null) onRename(note)
                                            }
                                        },
                                        onEditProperties = {
                                            if (selectedNoteIds.size == 1) {
                                                val note =
                                                    viewModel.canvasNotes.find { it.id == selectedNoteIds.first() }
                                                if (note != null) onEditProperties(note)
                                            }
                                        },
                                        onClearSelection = onClearSelection,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
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
    isSelected: Boolean,
    onToggleExpand: () -> Unit,
    onTap: () -> Unit,
    themeColors: ThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { onTap() }
            .padding(vertical = 2.dp)
            .background(
                color = if (isSelected) themeColors.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle group",
                modifier = Modifier
                    .clickable { onToggleExpand() }
                    .size(30.dp),
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

@Composable
private fun GroupActionRow(
    selectedCount: Int,
    hasGroup: Boolean,
    onGroup: () -> Unit,
    onUngroup: () -> Unit,
    onRename: () -> Unit,
    onEditProperties: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(vertical = 2.dp)
            .background(
                MaterialTheme.colorScheme.inversePrimary.copy(0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (!hasGroup && selectedCount > 1) {
            TextButton(onClick = onGroup) {
                Text("Group")
            }
        }
        if (hasGroup) {
            TextButton(onClick = onUngroup) {
                Text("Ungroup")
            }
            TextButton(onClick = onRename) {
                Text("Rename")
            }
        } else if (selectedCount == 1) {
            TextButton(onClick = onRename) {
                Text("Rename")
            }
            TextButton(onClick = onEditProperties) {
                Text("Properties")
            }
        }
        TextButton(onClick = onClearSelection) {
            Text(
                "Clear",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}