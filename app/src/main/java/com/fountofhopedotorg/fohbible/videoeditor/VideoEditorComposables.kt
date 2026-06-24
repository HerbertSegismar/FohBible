package com.fountofhopedotorg.fohbible.videoeditor

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.creator.ArrowRightShape
import com.fountofhopedotorg.fohbible.creator.CircleShape
import com.fountofhopedotorg.fohbible.creator.CrossShape
import com.fountofhopedotorg.fohbible.creator.DavidStarShape
import com.fountofhopedotorg.fohbible.creator.DiamondShape
import com.fountofhopedotorg.fohbible.creator.GearShape
import com.fountofhopedotorg.fohbible.creator.HeartShape
import com.fountofhopedotorg.fohbible.creator.HexagonShape
import com.fountofhopedotorg.fohbible.creator.LineShape
import com.fountofhopedotorg.fohbible.creator.MoonShape
import com.fountofhopedotorg.fohbible.creator.NoteThumbnail
import com.fountofhopedotorg.fohbible.creator.OctagonShape
import com.fountofhopedotorg.fohbible.creator.PolygonShape
import com.fountofhopedotorg.fohbible.creator.ReorderHandle
import com.fountofhopedotorg.fohbible.creator.ShapeSelectionCard
import com.fountofhopedotorg.fohbible.creator.SquareShape
import com.fountofhopedotorg.fohbible.creator.StarShape
import com.fountofhopedotorg.fohbible.creator.ThornCrownShape
import com.fountofhopedotorg.fohbible.creator.TriangleShape
import com.fountofhopedotorg.fohbible.creator.getElementDisplayName
import com.fountofhopedotorg.fohbible.creator.getRandomColor
import com.fountofhopedotorg.fohbible.creator.saveCanvasAsImage
import com.fountofhopedotorg.fohbible.creator.saveCanvasAsPDF
import com.fountofhopedotorg.fohbible.creator.saveCanvasAsSVG
import com.fountofhopedotorg.fohbible.data.CanvasKeyframe
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.DisplayItem
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VideoCanvasElementsPanel(
    notes: List<CanvasNote>,
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
    onReorder: (Int, Int) -> Unit,
    themeColors: ThemeColors,
    density: Density,
    groupNames: Map<String, String> = emptyMap(),
    onRenameGroup: ((groupId: String, currentName: String) -> Unit)? = null,
    onAnimateKeyframes: ((CanvasNote) -> Unit)? = null,
) {
    val groupedNotes = notes.groupBy { it.groupId }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val baseItems = buildList {
        for ((groupId, groupNotes) in groupedNotes) {
            if (groupId == null) continue
            val expanded = expandedGroups[groupId] ?: false
            val actualChildren = groupNotes.filter { note -> note.id != groupId }
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
                val originalIndex = notes.indexOf(note)
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
            val originalIndex = notes.indexOf(note)
            add(DisplayItem.NoteItem(note, originalIndex, isGrouped = false))
        }
    }
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
                                            notes.any {
                                                it.groupId == displayItem.groupId &&
                                                        (it.id == selectedNoteId || selectedNoteIds.contains(it.id))
                                            }

                                    VideoGroupHeaderRow(
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

                                        VideoCanvasElementItem(
                                            notes = notes,
                                            onReorder = onReorder,
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
                                                                onReorder(
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
                                    val hasGroup = notes.any { it.groupId != null && it.id in selectedNoteIds }
                                    VideoGroupActionRow(
                                        selectedCount = selectedNoteIds.size,
                                        hasGroup = hasGroup,
                                        onGroup = { onGroup("Group ${selectedNoteIds.size}", selectedNoteIds.toList()) },
                                        onUngroup = { onUngroup(selectedNoteIds) },
                                        onRename = {
                                            val selectedNotes = notes.filter { it.id in selectedNoteIds }
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
                                                val note = notes.find { it.id == selectedNoteIds.first() }
                                                if (note != null) onEditProperties(note)
                                            }
                                        },
                                        onClearSelection = onClearSelection,
                                        modifier = Modifier.animateItem(),
                                        onAnimate = {
                                            if (selectedNoteIds.size == 1) {
                                                val note = notes.first { it.id in selectedNoteIds }
                                                onAnimateKeyframes?.invoke(note)
                                            }
                                        }
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
fun VideoCanvasElementItem(
    notes: List<CanvasNote>,
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
    onReorder: (Int, Int) -> Unit,
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
                text = getElementDisplayName(note, originalIndex, notes),
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
                onReorder = onReorder,
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

@Composable
private fun VideoGroupHeaderRow(
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
private fun VideoGroupActionRow(
    selectedCount: Int,
    hasGroup: Boolean,
    onGroup: () -> Unit,
    onUngroup: () -> Unit,
    onRename: () -> Unit,
    onEditProperties: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
    onAnimate: (() -> Unit)? = null
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
            TextButton(onClick = onGroup) { Text("Group") }
        }
        if (hasGroup) {
            TextButton(onClick = onUngroup) { Text("Ungroup") }
            TextButton(onClick = onRename) { Text("Rename") }
        } else if (selectedCount == 1) {
            TextButton(onClick = onRename) { Text("Rename") }
            TextButton(onClick = onEditProperties) { Text("Properties") }
            if (onAnimate != null) {
                TextButton(onClick = onAnimate) {
                    Text("Animate")
                }
            }
        }
        IconButton(onClick = onClearSelection, modifier = Modifier.size(18.dp)) {
            Icon(Icons.Filled.Close,"Clear", tint = MaterialTheme.colorScheme.error)
        }
    }
}



@Composable
fun KeyframeAnimationDialog(
    note: CanvasNote?,
    onDismiss: () -> Unit,
    onSaveKeyframes: (String, List<CanvasKeyframe>) -> Unit
) {
    if (note == null) return

    val localKeyframes = remember(note.keyframes) { mutableStateListOf(*note.keyframes.toTypedArray()) }

    var timeInput by remember { mutableStateOf("") }
    var xInput by remember { mutableStateOf(note.offset.x.toString()) }
    var yInput by remember { mutableStateOf(note.offset.y.toString()) }
    var scaleXInput by remember { mutableStateOf(note.scaleX.toString()) }
    var scaleYInput by remember { mutableStateOf(note.scaleY.toString()) }
    var rotationInput by remember { mutableStateOf(note.rotation.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Keyframes") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Existing Keyframes", style = MaterialTheme.typography.titleSmall)
                if (localKeyframes.isEmpty()) {
                    Text(text = "No keyframes added yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    localKeyframes.sortedBy { it.timestampMs }.forEach { kf ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${kf.timestampMs}ms -> Pos: (${kf.x?.toInt()}, ${kf.y?.toInt()}) | Scale: (${kf.scaleX}, ${kf.scaleY}) | Rot: ${kf.rotation}°",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { localKeyframes.remove(kf) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Keyframe", tint = Color.Red)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(text = "Add / Overwrite Keyframe", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { timeInput = it },
                    label = { Text("Timestamp (ms)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = xInput,
                        onValueChange = { xInput = it },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yInput,
                        onValueChange = { yInput = it },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scaleXInput,
                        onValueChange = { scaleXInput = it },
                        label = { Text("Scale X") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = scaleYInput,
                        onValueChange = { scaleYInput = it },
                        label = { Text("Scale Y") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = rotationInput,
                    onValueChange = { rotationInput = it },
                    label = { Text("Rotation (Degrees)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val t = timeInput.toLongOrNull() ?: 0L
                        val x = xInput.toFloatOrNull() ?: note.offset.x
                        val y = yInput.toFloatOrNull() ?: note.offset.y
                        val sx = scaleXInput.toFloatOrNull() ?: note.scaleX
                        val sy = scaleYInput.toFloatOrNull() ?: note.scaleY
                        val rot = rotationInput.toFloatOrNull() ?: note.rotation

                        localKeyframes.removeAll { it.timestampMs == t }
                        localKeyframes.add(CanvasKeyframe(t, x, y, sx, sy, rot))
                        timeInput = ""
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Insert Keyframe", color = Color.White)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaveKeyframes(note.id, localKeyframes.sortedBy { it.timestampMs }) }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ToolbarSection(
    onAddShape: (shape: String) -> Unit,
    onCustomPolygon: () -> Unit,
    selectedInputMode: String,
    onModeSelected: (String) -> Unit,
    themeColors: ThemeColors,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onChooseFromGallery: () -> Unit,
    graphicsLayer: GraphicsLayer,
    isLandscape: Boolean = false,
    onSaveVideo: () -> Unit = {}
) {

    var showMoreShapes by remember { mutableStateOf(false) }
    val viewModel: AppViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val textIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val bookIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val imageIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val fullscreenIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val saveIconColor = remember { getRandomColor().copy(alpha = 0.8f) }

    val modes = listOf(
        Triple("Add Text", Icons.Default.TextFields, textIconColor),
        Triple("Fetch Verse", Icons.Default.Book, bookIconColor),
        Triple("Add Image", Icons.Default.Image, imageIconColor)
    )

    val pentagonPoints = listOf(
        Offset(0.5000f, 0.0000f),
        Offset(0.9755f, 0.3455f),
        Offset(0.7939f, 0.9045f),
        Offset(0.2061f, 0.9045f),
        Offset(0.0245f, 0.3455f)
    )
    val toolbarContent: @Composable () -> Unit = {
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = { onAddShape("Square") }
        ) {
            SquareShape(modifier = Modifier.size(30.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = { onAddShape("Circle") }
        ) {
            CircleShape(modifier = Modifier.size(30.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = { onAddShape("Triangle") }
        ) {
            TriangleShape(modifier = Modifier.size(30.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = { onAddShape("Pentagon") }
        ) {
            PolygonShape(
                points = pentagonPoints,
                modifier = Modifier.size(30.dp)
            )
        }
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = { onAddShape("Line") }
        ) {
            LineShape(modifier = Modifier.size(18.dp).padding(top = 6.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = onCustomPolygon
        ) {
            Icon(
                imageVector = Icons.Default.ShapeLine,
                contentDescription = "Custom Polygon",
                modifier = Modifier.size(30.dp),
                tint = getRandomColor().copy(0.8f)
            )
        }
        Box {
            ShapeSelectionCard(
                modifier = Modifier.size(40.dp),
                onClick = { showMoreShapes = true }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More Shapes",
                    modifier = Modifier.size(40.dp),
                    tint = getRandomColor()
                )
            }

            DropdownMenu(
                expanded = showMoreShapes,
                onDismissRequest = { showMoreShapes = false },
                modifier = Modifier
                    .width(if (isLandscape) 80.dp else 40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(0.05f))
            ) {
                val newShapes = listOf(
                    "Hexagon" to @Composable { HexagonShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Star" to @Composable { StarShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Diamond" to @Composable { DiamondShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Heart" to @Composable { HeartShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Octagon" to @Composable { OctagonShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Cross" to @Composable { CrossShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "ThornCrown" to @Composable { ThornCrownShape(modifier = Modifier.fillMaxSize(), thornColor = getRandomColor().copy(0.8f)) },
                    "Moon" to @Composable { MoonShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "DavidStar" to @Composable { DavidStarShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Gear" to @Composable { GearShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "ArrowRight" to @Composable { ArrowRightShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) }
                )

                if (isLandscape) {
                    val chunkedShapes = newShapes.chunked(2)
                    chunkedShapes.forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { (name, preview) ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            showMoreShapes = false
                                            onAddShape(name)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(Modifier.size(18.dp)) {
                                        preview()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    newShapes.forEach { (name, preview) ->
                        DropdownMenuItem(
                            text = {},
                            onClick = {
                                showMoreShapes = false
                                onAddShape(name)
                            },
                            leadingIcon = {
                                Box(Modifier.size(18.dp)) {
                                    preview()
                                }
                            }
                        )
                    }
                }
            }
        }

        modes.forEach { (mode, icon, color) ->
            val isSelected = selectedInputMode == mode
            IconButton(
                onClick = {
                    if (mode == "Add Image") {
                        onChooseFromGallery()
                    }
                    onModeSelected(mode)
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = mode,
                    tint = if (isSelected) themeColors.primary else color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        IconButton(onClick = onToggleFullScreen) {
            Icon(
                modifier = Modifier.size(26.dp),
                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullScreen) "Exit Fullscreen" else "Enter Fullscreen",
                tint = fullscreenIconColor
            )
        }

        Box {
            IconButton(
                onClick = { viewModel.showSaveMenu = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save As",
                    tint = saveIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            SaveAsMenuWithVideo(
                expanded = viewModel.showSaveMenu,
                onDismiss = { viewModel.showSaveMenu = false },
                onSavePng = {
                    coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "PNG") }
                },
                onSaveJpg = {
                    coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "JPG") }
                },
                onSavePdf = {
                    coroutineScope.launch {
                        saveCanvasAsPDF(
                            graphicsLayer,
                            context
                        )
                    }
                },
                onSaveSvg = {
                    coroutineScope.launch {
                        saveCanvasAsSVG(graphicsLayer, context, viewModel.canvasNotes)
                    }
                },
                onSaveVideo = onSaveVideo
            )
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            toolbarContent()
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            toolbarContent()
        }
    }
}

@Composable
fun SaveAsMenuWithVideo(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSavePng: () -> Unit,
    onSaveJpg: () -> Unit,
    onSavePdf: () -> Unit,
    onSaveSvg: () -> Unit,
    onSaveVideo: () -> Unit
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
            DropdownMenuItem(
                text = { Text("MP4") },
                onClick = {
                    onDismiss()
                    onSaveVideo()
                }
            )
        }
    }
}