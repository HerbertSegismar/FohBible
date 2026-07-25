package com.fountofhopedotorg.fohbible.gfx_animator

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.AnimatorTab
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.CanvasKeyframe
import com.fountofhopedotorg.fohbible.data.DisplayItem
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.gfx_creator.ElementThumbnail
import com.fountofhopedotorg.fohbible.gfx_creator.ReorderHandle
import com.fountofhopedotorg.fohbible.gfx_creator.getElementDisplayName
import com.fountofhopedotorg.fohbible.models.AppViewModel
import kotlin.math.roundToInt

@Composable
fun AnimatorCanvasElementsPanel(
    elements: List<CanvasElement>,
    selectedElementIds: Set<String>,
    selectedElementId: String?,
    showTree: Boolean,
    onToggleTree: () -> Unit,
    onSingleSelect: (CanvasElement) -> Unit,
    onToggleGroupSelection: (CanvasElement) -> Unit,
    onGroupHeaderTap: (String) -> Unit,
    onEditElement: (CanvasElement) -> Unit,
    onCustomPolygonEdit: (CanvasElement) -> Unit,
    onRename: (CanvasElement) -> Unit,
    onEditProperties: (CanvasElement) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDuplicate: (CanvasElement) -> Unit,
    onDelete: (CanvasElement) -> Unit,
    onUngroup: (Set<String>) -> Unit,
    onGroup: (String, List<String>) -> Unit,
    onClearSelection: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    themeColors: ThemeColors,
    density: Density,
    groupNames: Map<String, String> = emptyMap(),
    onRenameGroup: ((groupId: String, currentName: String) -> Unit)? = null,
    onAnimateKeyframes: ((CanvasElement) -> Unit)? = null,
    gradientConfigs: Map<String, GradientConfig> = emptyMap(),
    activeTab: AnimatorTab = AnimatorTab.LAYERS,
    onTabSelected: (AnimatorTab) -> Unit = {},

    viewModel: AppViewModel? = null,
    fineTunerSelectedElementId: String? = null,
    onSaveKeyframes: ((elementId: String, keyframes: List<CanvasKeyframe>, startMs: Long, endMs: Long) -> Unit)? = null,
    timeMultiplier: Float = 1f,
    canvasWidth: Int = 1000,
    canvasHeight: Int = 1000
) {
    val groupedElements = elements.groupBy { it.groupId }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val baseItems = buildList {
        for ((groupId, groupElements) in groupedElements) {
            if (groupId == null) continue
            val expanded = expandedGroups[groupId] ?: false
            val actualChildren = groupElements.filter { element -> element.id != groupId }
            val groupName = groupNames[groupId] ?: "Group of ${actualChildren.size}"
            add(
                DisplayItem.GroupHeader(
                    groupId = groupId,
                    groupName = groupName,
                    memberCount = actualChildren.size,
                    isExpanded = expanded
                )
            )
            actualChildren.forEach { element ->
                val originalIndex = elements.indexOf(element)
                add(
                    DisplayItem.ElementItem(
                        element = element,
                        originalIndex = originalIndex,
                        isGrouped = true,
                        groupId = groupId
                    )
                )
            }
        }
        val ungrouped = groupedElements[null] ?: emptyList()
        ungrouped.forEach { element ->
            val originalIndex = elements.indexOf(element)
            add(DisplayItem.ElementItem(element, originalIndex, isGrouped = false))
        }
    }
    val displayItems = remember(baseItems, expandedGroups, selectedElementIds) {
        val mutableList = baseItems.toMutableList()
        var insertionIndex = -1
        for (i in mutableList.indices.reversed()) {
            val item = mutableList[i]
            if (item is DisplayItem.ElementItem &&
                selectedElementIds.contains(item.element.id) &&
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
            if (item is DisplayItem.ElementItem) {
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
                .padding(bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Tabs row – clean, direct selection ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Layers",
                    modifier = Modifier.clickable { onTabSelected(AnimatorTab.LAYERS) },
                    color = if (activeTab == AnimatorTab.LAYERS) themeColors.primary
                    else themeColors.textColor.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(12.dp))

                Text(
                    "Fine Tuner",
                    modifier = Modifier.clickable { onTabSelected(AnimatorTab.FINE_TUNER) },
                    color = if (activeTab == AnimatorTab.FINE_TUNER) themeColors.primary
                    else themeColors.textColor.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(12.dp))

                Text(
                    "Keyframe",
                    modifier = Modifier.clickable { onTabSelected(AnimatorTab.KEYFRAME) },
                    color = if (activeTab == AnimatorTab.KEYFRAME) themeColors.primary
                    else themeColors.textColor.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Collapse/Expand icon only for Layers tab
            if (activeTab == AnimatorTab.LAYERS) {
                Icon(
                    imageVector = if (showTree) Icons.Default.ArrowDropDown
                    else Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = if (showTree) "Collapse" else "Expand",
                    tint = themeColors.textColor,
                    modifier = Modifier.clickable { onToggleTree() }
                )
            }
        }

        when (activeTab) {
            AnimatorTab.KEYFRAME -> {
                val activeElementId = selectedElementId ?: fineTunerSelectedElementId
                val activeElement = elements.find { it.id == activeElementId }

                KeyframeAnimationContent(
                    element = activeElement,
                    allElements = elements,
                    onElementSelected = { onSingleSelect(it) },
                    onCancel = { onTabSelected(AnimatorTab.LAYERS) },
                    onSave = { id, keyframes, startMs, endMs ->
                        onSaveKeyframes?.invoke(id, keyframes, startMs, endMs)
                    },
                    timeMultiplier = timeMultiplier,
                    initialGradientConfig = gradientConfigs[activeElement?.id],
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    themeColors = themeColors,
                    gradientConfigs = gradientConfigs
                )
            }

            AnimatorTab.FINE_TUNER -> {
                if (viewModel != null) {
                    FineTunerPanel(
                        viewModel = viewModel,
                        selectedElementId = fineTunerSelectedElementId,
                        elements = elements,
                        themeColors = themeColors
                    )
                } else {
                    Text(
                        "Fine Tuner unavailable",
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            AnimatorTab.LAYERS -> {
                AnimatedVisibility(visible = showTree) {
                    Column {
                        if (displayItems.isEmpty()) {
                            Text(
                                text =  "No elements on canvas yet",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall
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
                                            is DisplayItem.ElementItem -> item.element.id
                                            is DisplayItem.ActionRow -> "action-row"
                                        }
                                    }
                                ) { displayIndex ->
                                    when (val displayItem = displayItems[displayIndex]) {
                                        is DisplayItem.GroupHeader -> {
                                            val isGroupSelected =
                                                selectedElementId == displayItem.groupId ||
                                                        selectedElementIds.contains(displayItem.groupId) ||
                                                        elements.any {
                                                            it.groupId == displayItem.groupId &&
                                                                    (it.id == selectedElementId || selectedElementIds.contains(
                                                                        it.id
                                                                    ))
                                                        }

                                            AnimatorGroupHeaderRow(
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

                                        is DisplayItem.ElementItem -> {
                                            val element = displayItem.element
                                            val gradConfig = gradientConfigs[element.id]
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
                                                val itemModifier =
                                                    if (isGrouped) Modifier.padding(start = 16.dp) else Modifier
                                                val bounds = groupBounds[groupId]
                                                val isUpEnabled =
                                                    bounds != null && displayIndex > bounds.first
                                                val isDownEnabled =
                                                    bounds != null && displayIndex < bounds.second

                                                AnimatorCanvasElementItem(
                                                    elements = elements,
                                                    onReorder = onReorder,
                                                    element = element,
                                                    originalIndex = originalIndex,
                                                    isSelected = selectedElementId == element.id,
                                                    isDragTarget = draggedDisplayIndex == displayIndex,
                                                    isUpEnabled = isUpEnabled,
                                                    isDownEnabled = isDownEnabled,
                                                    dragOffset = if (draggedDisplayIndex == displayIndex) dragOffset else 0f,
                                                    selectedElementIds = selectedElementIds,
                                                    isGrouped = isGrouped,
                                                    onRowTap = { onSingleSelect(element) },
                                                    onToggleGroupSelection = {
                                                        onToggleGroupSelection(
                                                            element
                                                        )
                                                    },
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
                                                                displayItems[fromDisplayIndex] as? DisplayItem.ElementItem
                                                            if (draggedItem != null) {
                                                                val itemBounds =
                                                                    groupBounds[draggedItem.groupId]
                                                                if (itemBounds != null) {
                                                                    val rawTargetIdx =
                                                                        (fromDisplayIndex + (dragOffset / itemHeightPx).roundToInt())
                                                                    val targetDisplayIdx =
                                                                        rawTargetIdx.coerceIn(
                                                                            itemBounds.first,
                                                                            itemBounds.second
                                                                        )

                                                                    if (targetDisplayIdx != fromDisplayIndex) {
                                                                        val targetItem =
                                                                            displayItems[targetDisplayIdx] as DisplayItem.ElementItem
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
                                                    onEdit = { onEditElement(element) },
                                                    onCustomPolygonEdit = { onCustomPolygonEdit(element) },
                                                    onVisibilityToggle = { onToggleVisibility(element.id) },
                                                    onLockToggle = { onToggleLock(element.id) },
                                                    onDuplicate = { onDuplicate(element) },
                                                    onDelete = { onDelete(element) },
                                                    themeColors = themeColors,
                                                    gradientConfig = gradConfig,
                                                    modifier = itemModifier
                                                )
                                            }
                                        }

                                        is DisplayItem.ActionRow -> {
                                            val hasGroup =
                                                elements.any { it.groupId != null && it.id in selectedElementIds }
                                            AnimatorGroupActionRow(
                                                selectedCount = selectedElementIds.size,
                                                hasGroup = hasGroup,
                                                onGroup = {
                                                    onGroup(
                                                        "Group ${selectedElementIds.size}",
                                                        selectedElementIds.toList()
                                                    )
                                                },
                                                onUngroup = { onUngroup(selectedElementIds) },
                                                onRename = {
                                                    val selectedElements =
                                                        elements.filter { it.id in selectedElementIds }
                                                    val uniqueGroupIds =
                                                        selectedElements.mapNotNull { it.groupId }
                                                            .distinct()
                                                    if (hasGroup && uniqueGroupIds.isNotEmpty()) {
                                                        onRenameGroup?.invoke(
                                                            uniqueGroupIds.first(),
                                                            groupNames[uniqueGroupIds.first()]
                                                                ?: "Group"
                                                        )
                                                    } else if (selectedElementIds.size == 1) {
                                                        val element = selectedElements.firstOrNull()
                                                        if (element != null) onRename(element)
                                                    }
                                                },
                                                onEditProperties = {
                                                    if (selectedElementIds.size == 1) {
                                                        val element =
                                                            elements.find { it.id == selectedElementIds.first() }
                                                        if (element != null) onEditProperties(element)
                                                    }
                                                },
                                                onClearSelection = onClearSelection,
                                                modifier = Modifier.animateItem(),
                                                onAnimate = {
                                                    if (selectedElementIds.size == 1) {
                                                        val element =
                                                            elements.first { it.id in selectedElementIds }
                                                        onAnimateKeyframes?.invoke(element)
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
    }
}

@Composable
fun AnimatorCanvasElementItem(
    modifier: Modifier = Modifier,
    elements: List<CanvasElement>,
    element: CanvasElement,
    originalIndex: Int,
    isSelected: Boolean,
    isDragTarget: Boolean,
    isUpEnabled: Boolean,
    isDownEnabled: Boolean,
    dragOffset: Float,
    selectedElementIds: Set<String>,
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
    gradientConfig: GradientConfig? = null,
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .graphicsLayer { if (isDragTarget) translationY = dragOffset }
            .then(
                if (!isGrouped) {
                    Modifier.pointerInput(element.id, originalIndex) {
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
                        if (selectedElementIds.contains(element.id)) {
                            Modifier.border(2.dp, themeColors.primary, RoundedCornerShape(6.dp))
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                ElementThumbnail(element, themeColors, gradientConfig)
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = getElementDisplayName(element, originalIndex, elements),
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
                    imageVector = if (element.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (element.isVisible) "Hide Element" else "Show Element",
                    tint = if (element.isVisible) themeColors.primary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onLockToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (element.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (element.isLocked) "Unlock Element" else "Lock Element",
                    tint = if (element.isLocked) Color.Gray else themeColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = {
                if (element.content.startsWith("Shape:")) {
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
private fun AnimatorGroupHeaderRow(
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
private fun AnimatorGroupActionRow(
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
    val selectedItems = if (selectedCount > 1) "items" else "item"
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
            text = "$selectedCount $selectedItems",
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
fun Mp4ExportSettingsDialog(
    initialFrameRate: Int,
    initialBitRateMbps: Int,
    initialExportMode: String = "Screen",
    initialOutputMode: String = "Video",
    initialResolutionMultiplier: Float = 1f,
    onDismiss: () -> Unit,
    onConfirm: (
        frameRate: Int,
        bitRate: Int,
        exportMode: String,
        outputMode: String,
        resolutionMultiplier: Float
    ) -> Unit
) {
    var frameRate by remember { mutableIntStateOf(initialFrameRate) }
    var bitRateMbps by remember { mutableFloatStateOf(initialBitRateMbps.toFloat()) }
    var exportMode by remember { mutableStateOf(initialExportMode) }
    var outputMode by remember { mutableStateOf(initialOutputMode) }
    var resolutionMultiplier by remember { mutableFloatStateOf(initialResolutionMultiplier) }

    Column {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Export Settings") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Frame Rate: $frameRate fps")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 60, 120).forEach { fps ->
                            FilterChip(
                                selected = frameRate == fps,
                                onClick = { frameRate = fps },
                                label = { Text("$fps") }
                            )
                        }
                    }

                    Text("Bit Rate: ${bitRateMbps.roundToInt()} Mbps")
                    Slider(
                        value = bitRateMbps,
                        onValueChange = { bitRateMbps = it },
                        valueRange = 5f..50f,
                        steps = 44
                    )

                    Text("Export Mode:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = exportMode == "Screen",
                            onClick = { exportMode = "Screen" },
                            label = { Text("Screen Render") }
                        )
                        FilterChip(
                            selected = exportMode == "Native",
                            onClick = { exportMode = "Native" },
                            label = { Text("Native Render") }
                        )
                    }

                    if (exportMode == "Native") {
                        Text("Resolution Multiplier:")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1f, 2f, 4f).forEach { mult ->
                                FilterChip(
                                    selected = resolutionMultiplier == mult,
                                    onClick = { resolutionMultiplier = mult },
                                    label = { Text("${mult.toInt()}x") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(
                        frameRate,
                        bitRateMbps.roundToInt(),
                        exportMode,
                        outputMode,
                        resolutionMultiplier
                    )
                }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
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
    onSaveVideo: () -> Unit,
    onSaveTemplate: () -> Unit,
) {
    Box {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                text = { Text("PNG") },
                onClick = { onDismiss(); onSavePng() }
            )
            DropdownMenuItem(
                text = { Text("JPG") },
                onClick = { onDismiss(); onSaveJpg() }
            )
            DropdownMenuItem(
                text = { Text("PDF") },
                onClick = { onDismiss(); onSavePdf() }
            )
            DropdownMenuItem(
                text = { Text("SVG") },
                onClick = { onDismiss(); onSaveSvg() }
            )
            DropdownMenuItem(
                text = { Text("MP4") },
                onClick = { onDismiss(); onSaveVideo() }
            )

            DropdownMenuItem(
                text = { Text("Template") },
                onClick = {
                    onDismiss()
                    onSaveTemplate()
                }
            )
        }
    }
}