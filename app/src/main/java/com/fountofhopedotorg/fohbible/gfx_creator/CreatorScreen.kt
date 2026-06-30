package com.fountofhopedotorg.fohbible.gfx_creator

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.color_wheel.ColorWheelDialog
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor

sealed class ContentDialogType {
    data class Edit(val elementId: String, val initialContent: String) : ContentDialogType()
    object AddText : ContentDialogType()
    object FetchVerse : ContentDialogType()
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun CreatorScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var editShadowColorForDialog by remember { mutableStateOf<Color?>(null) }
    var editShadowOffsetX by remember { mutableFloatStateOf(0f) }
    var editShadowOffsetY by remember { mutableFloatStateOf(0f) }
    var editBorderThickness by remember { mutableFloatStateOf(0f) }
    var editBorderColorForDialog by remember { mutableStateOf<Color?>(null) }

    val viewModel: AppViewModel = viewModel()
    val graphicsLayer = rememberGraphicsLayer()

    var elementToRenameId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var groupToRenameId by rememberSaveable { mutableStateOf<String?>(null) }
    var groupRenameText by rememberSaveable { mutableStateOf("") }

    var selectedElementIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showGroupDialog by rememberSaveable { mutableStateOf(false) }
    var groupName by rememberSaveable { mutableStateOf("") }
    var showEditPropertiesDialog by rememberSaveable { mutableStateOf(false) }
    var editX by rememberSaveable { mutableStateOf("") }
    var editY by rememberSaveable { mutableStateOf("") }
    var editScaleX by rememberSaveable { mutableStateOf("") }
    var editScaleY by rememberSaveable { mutableStateOf("") }
    var editRotation by rememberSaveable { mutableStateOf("") }
    var editPropertiesElementId by rememberSaveable { mutableStateOf<String?>(null) }
    var editColorForDialog by remember { mutableStateOf(Color.White) }

    var showCustomPolygonDialog by rememberSaveable { mutableStateOf(false) }
    var selectedElementId by rememberSaveable { mutableStateOf<String?>(null) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var elementToColorEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedInputMode by rememberSaveable { mutableStateOf("Add SVG") }
    var contentDialogType by remember { mutableStateOf<ContentDialogType?>(null) }
    var showCanvasElementsTree by rememberSaveable { mutableStateOf(true) }
    var polygonElementToEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var initialPolygonString by rememberSaveable { mutableStateOf("") }
    var initialIsLineMode by rememberSaveable { mutableStateOf(false) }
    var dragGroupDelta by remember { mutableStateOf(Offset.Zero) }

    val onProportionalToggle: () -> Unit = remember {
        { viewModel.proportionalEditing = !viewModel.proportionalEditing }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addToCanvas(
                CanvasElement(
                    content = "Image: $uri",
                    backgroundColor = Color.Transparent
                )
            )
        }
    }

    val dbHelper = remember(viewModel.currentDbName) {
        DatabaseHelper(context, viewModel.currentDbName)
    }
    DisposableEffect(dbHelper) {
        onDispose { dbHelper.close() }
    }

    val verseProcessor = remember { VerseTextProcessor() }
    val theme = LocalAppTheme.current
    val isDark = theme.darkTheme
    val themeColors = remember(isDark, theme.primaryColor, viewModel.wordsOfJesus) {
        ThemeColors(
            textColor = if (isDark) Color.White else Color.Black,
            verseNumber = theme.primaryColor,
            primary = theme.primaryColor,
            tagColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
            tagBg = if (isDark) Color(0xFF1E293B) else Color.White,
            wordsOfJesus = viewModel.wordsOfJesus,
            searchHighlightBg = theme.primaryColor.copy(alpha = 0.2f),
            highlightIcon = theme.primaryColor
        )
    }

    val mainScrollState = rememberScrollState()
    val elementsGrouped = remember(viewModel.canvasElements) {
        viewModel.canvasElements.groupBy { it.groupId }
    }
    val selectedGroups = remember(selectedElementIds, viewModel.canvasElements) {
        viewModel.canvasElements
            .filter { it.groupId != null && it.id in selectedElementIds }
            .map { it.groupId!! }
            .toSet()
    }

    fun toggleGroupSelection(element: CanvasElement) {
        val groupId = element.groupId
        if (groupId != null) {
            val groupElements = viewModel.canvasElements.filter { it.groupId == groupId }
            val allIds = groupElements.map { it.id }.toSet()
            selectedElementIds = if (selectedElementIds.containsAll(allIds)) {
                selectedElementIds - allIds
            } else {
                selectedElementIds + allIds
            }
        } else {
            selectedElementIds = if (selectedElementIds.contains(element.id))
                selectedElementIds - element.id
            else
                selectedElementIds + element.id
        }
    }

    fun onCanvasElementTap(element: CanvasElement) {
        val groupId = element.groupId
        if (groupId != null) {
            val groupElements = viewModel.canvasElements.filter { it.groupId == groupId }
            selectedElementIds = groupElements.map { it.id }.toSet()
            selectedElementId = element.id
        } else {
            selectedElementIds = setOf(element.id)
            selectedElementId = element.id
        }
    }

    fun onSingleSelect(element: CanvasElement) {
        selectedElementId = element.id
        selectedElementIds = emptySet()
    }

    fun onGroupHeaderTap(groupId: String) {
        val members = viewModel.canvasElements.filter { it.groupId == groupId }
        selectedElementIds = members.map { it.id }.toSet()
        selectedElementId = members.firstOrNull()?.id
    }

    if (isLandscape) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(2.dp)
                ) {
                    CanvasArea(
                        modifier = Modifier.fillMaxSize(),
                        elements = viewModel.canvasElements,
                        selectedElementIds = selectedElementIds,
                        selectedElementId = selectedElementId,
                        selectedGroups = selectedGroups,
                        dragGroupDelta = dragGroupDelta,
                        onGroupDragDeltaChange = { dragGroupDelta = it },
                        onCanvasElementTap = { onCanvasElementTap(it) },
                        onElementUpdatePosition = { element, offset, w, h, rotation ->
                            viewModel.updateElementProperties(
                                id = element.id,
                                x = offset.x,
                                y = offset.y,
                                width = w,
                                height = h,
                                rotation = rotation
                            )
                        },
                        onColorPickerRequested = {
                            elementToColorEditId = it
                            showColorPicker = true
                        },
                        onDeleteRequested = {
                            val idx = viewModel.canvasElements.indexOfFirst { element -> element.id == it }
                            if (idx != -1) viewModel.removeFromCanvas(idx)
                        },
                        onClearSelection = {
                            selectedElementIds = emptySet()
                            selectedElementId = null
                        },
                        themeColors = themeColors,
                        isDark = isDark,
                        elementsGrouped = elementsGrouped,
                        graphicsLayer = graphicsLayer,
                        onElementScaleChange = { id, sx, sy -> viewModel.updateElementScale(id, sx, sy) },
                        proportionalEditing = viewModel.proportionalEditing,
                        onProportionalToggle = onProportionalToggle
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Elements panel (right, fixed proportion)
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(top = 10.dp)
                ) {
                    CanvasElementsPanel(
                        elements = viewModel.canvasElements,
                        selectedElementIds = selectedElementIds,
                        selectedElementId = selectedElementId,
                        showTree = showCanvasElementsTree,
                        onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
                        onSingleSelect = { onSingleSelect(it) },
                        onToggleGroupSelection = { toggleGroupSelection(it) },
                        onGroupHeaderTap = { onGroupHeaderTap(it) },
                        onEditElement = { element ->
                            contentDialogType = ContentDialogType.Edit(element.id, element.content)
                        },
                        onCustomPolygonEdit = { element ->
                            val content = element.content.trim()
                            when {
                                content.startsWith("Shape:CustomPolygon:") -> {
                                    polygonElementToEditId = element.id
                                    initialPolygonString = content.removePrefix("Shape:CustomPolygon:")
                                    initialIsLineMode = false
                                    showCustomPolygonDialog = true
                                }
                                content.startsWith("Shape:CustomLine:") -> {
                                    polygonElementToEditId = element.id
                                    initialPolygonString = content.removePrefix("Shape:CustomLine:")
                                    initialIsLineMode = true
                                    showCustomPolygonDialog = true
                                }
                                content.startsWith("Shape: Line") || content == "Shape: Line" -> {
                                    polygonElementToEditId = element.id
                                    initialPolygonString = getSerializedPointsForShape("Line")
                                    initialIsLineMode = true
                                    showCustomPolygonDialog = true
                                }
                                else -> {
                                    val shapeType = content.removePrefix("Shape:").trim()
                                    val prefilledPoints = getSerializedPointsForShape(shapeType)
                                    if (prefilledPoints.isNotEmpty()) {
                                        polygonElementToEditId = element.id
                                        initialPolygonString = prefilledPoints
                                        initialIsLineMode = false
                                        showCustomPolygonDialog = true
                                    } else {
                                        contentDialogType = ContentDialogType.Edit(element.id, content)
                                    }
                                }
                            }
                        },
                        onRename = { element ->
                            elementToRenameId = element.id
                            renameText = getElementDisplayName(
                                element,
                                viewModel.canvasElements.indexOf(element),
                                viewModel.canvasElements
                            )
                        },
                        onEditProperties = { element ->
                            editPropertiesElementId = element.id
                            editX = element.offset.x.toString()
                            editY = element.offset.y.toString()
                            editScaleX = element.scaleX.toString()
                            editScaleY = element.scaleY.toString()
                            editRotation = element.rotation.toString()
                            editColorForDialog = if (!element.content.startsWith("Shape:") && !element.content.startsWith("Image:"))
                                element.textColor ?: Color.Black
                            else
                                element.backgroundColor
                            editShadowColorForDialog = element.shadowColor
                            editShadowOffsetX = element.shadowOffsetX
                            editShadowOffsetY = element.shadowOffsetY
                            editBorderThickness = element.borderThickness
                            editBorderColorForDialog = element.borderColor
                            showEditPropertiesDialog = true
                        },
                        onToggleVisibility = { viewModel.toggleVisibility(it) },
                        onToggleLock = { viewModel.toggleLock(it) },
                        onDuplicate = { viewModel.addToCanvas(CanvasElement(content = it.content)) },
                        onDelete = {
                            val idx = viewModel.canvasElements.indexOf(it)
                            if (idx != -1) viewModel.removeFromCanvas(idx)
                            if (selectedElementId == it.id) selectedElementId = null
                        },
                        onUngroup = { ids ->
                            viewModel.ungroupElements(ids)
                            selectedElementIds = emptySet()
                        },
                        onGroup = { name, ids ->
                            if (name.isNotBlank() && ids.isNotEmpty()) {
                                viewModel.createGroup(ids)
                                selectedElementIds = emptySet()
                                showGroupDialog = false
                                groupName = ""
                            }
                        },
                        onClearSelection = { selectedElementIds = emptySet() },
                        onReorder = { from, to -> viewModel.reorderCanvasElements(from, to) },
                        themeColors = themeColors,
                        density = LocalDensity.current,
                        groupNames = viewModel.groupNames,
                        onRenameGroup = { groupId, currentName ->
                            groupToRenameId = groupId
                            groupRenameText = currentName
                        }
                    )
                }
            }

            CombinedToolbarSection(
                onAddShape = { shape ->
                    val color = getRandomColor()
                    viewModel.addToCanvas(
                        CanvasElement(content = "Shape: $shape", backgroundColor = color, width = 100f, height = 100f)
                    )
                },
                onCustomPolygon = {
                    polygonElementToEditId = null
                    initialPolygonString = ""
                    initialIsLineMode = false
                    showCustomPolygonDialog = true
                },
                selectedInputMode = selectedInputMode,
                onModeSelected = { mode ->
                    when (mode) {
                        "Add SVG" -> selectedInputMode = "Add SVG"
                        "Add Text" -> {
                            selectedInputMode = "Add Text"
                            contentDialogType = ContentDialogType.AddText
                        }
                        "Fetch Verse" -> {
                            selectedInputMode = "Fetch Verse"
                            contentDialogType = ContentDialogType.FetchVerse
                        }
                        else -> selectedInputMode = "Add SVG"
                    }
                },
                themeColors = themeColors,
                isFullScreen = viewModel.isCreatorFullScreen,
                onToggleFullScreen = {
                    viewModel.isCreatorFullScreen = !viewModel.isCreatorFullScreen
                },
                onChooseFromGallery = { imagePickerLauncher.launch("image/*") },
                graphicsLayer = graphicsLayer,
                isLandscape = true
            )
        }
    }
    else {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
            ) {
                CombinedToolbarSection(
                    onAddShape = { shape ->
                        val color = getRandomColor()
                        viewModel.addToCanvas(
                            CanvasElement(content = "Shape: $shape", backgroundColor = color, width = 100f, height = 100f)
                        )
                    },
                    onCustomPolygon = {
                        polygonElementToEditId = null
                        initialPolygonString = ""
                        initialIsLineMode = false
                        showCustomPolygonDialog = true
                    },
                    selectedInputMode = selectedInputMode,
                    onModeSelected = { mode ->
                        when (mode) {
                            "Add SVG" -> selectedInputMode = "Add SVG"
                            "Add Text" -> {
                                selectedInputMode = "Add Text"
                                contentDialogType = ContentDialogType.AddText
                            }
                            "Fetch Verse" -> {
                                selectedInputMode = "Fetch Verse"
                                contentDialogType = ContentDialogType.FetchVerse
                            }
                            else -> selectedInputMode = "Add SVG"
                        }
                    },
                    themeColors = themeColors,
                    isFullScreen = viewModel.isCreatorFullScreen,
                    onToggleFullScreen = {
                        viewModel.isCreatorFullScreen = !viewModel.isCreatorFullScreen
                    },
                    onChooseFromGallery = { imagePickerLauncher.launch("image/*") },
                    graphicsLayer = graphicsLayer,
                    isLandscape = false
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.99f)
                        .weight(1f)
                        .verticalScroll(mainScrollState)
                ) {
                    CanvasArea(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(520.dp),
                        elements = viewModel.canvasElements,
                        selectedElementIds = selectedElementIds,
                        selectedElementId = selectedElementId,
                        selectedGroups = selectedGroups,
                        dragGroupDelta = dragGroupDelta,
                        onGroupDragDeltaChange = { dragGroupDelta = it },
                        onCanvasElementTap = { onCanvasElementTap(it) },
                        onElementUpdatePosition = { element, offset, w, h, rotation ->
                            viewModel.updateElementProperties(
                                id = element.id,
                                x = offset.x,
                                y = offset.y,
                                width = w,
                                height = h,
                                rotation = rotation
                            )
                        },
                        onElementScaleChange = { id, sx, sy -> viewModel.updateElementScale(id, sx, sy) },
                        onColorPickerRequested = {
                            elementToColorEditId = it
                            showColorPicker = true
                        },
                        onDeleteRequested = {
                            val idx = viewModel.canvasElements.indexOfFirst { element -> element.id == it }
                            if (idx != -1) viewModel.removeFromCanvas(idx)
                        },
                        onClearSelection = {
                            selectedElementIds = emptySet()
                            selectedElementId = null
                        },
                        themeColors = themeColors,
                        isDark = isDark,
                        elementsGrouped = elementsGrouped,
                        graphicsLayer = graphicsLayer,
                        proportionalEditing = viewModel.proportionalEditing,
                        onProportionalToggle = onProportionalToggle
                    )

                    Spacer(Modifier.height(4.dp))

                    CanvasElementsPanel(
                        elements = viewModel.canvasElements,
                        selectedElementIds = selectedElementIds,
                        selectedElementId = selectedElementId,
                        showTree = showCanvasElementsTree,
                        onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
                        onSingleSelect = { onSingleSelect(it) },
                        onToggleGroupSelection = { toggleGroupSelection(it) },
                        onGroupHeaderTap = { onGroupHeaderTap(it) },
                        onEditElement = { element ->
                            contentDialogType = ContentDialogType.Edit(element.id, element.content)
                        },
                        onCustomPolygonEdit = { element ->
                            val content = element.content.trim()
                            when {
                                content.startsWith("Shape:CustomPolygon:") -> {
                                    polygonElementToEditId = element.id
                                    initialPolygonString =
                                        content.removePrefix("Shape:CustomPolygon:")
                                    initialIsLineMode = false
                                    showCustomPolygonDialog = true
                                }
                                content.startsWith("Shape:CustomLine:") -> {
                                    polygonElementToEditId = element.id
                                    initialPolygonString =
                                        content.removePrefix("Shape:CustomLine:")
                                    initialIsLineMode = true
                                    showCustomPolygonDialog = true
                                }
                                content.startsWith("Shape: Line") || content == "Shape: Line" -> {
                                    polygonElementToEditId = element.id
                                    initialPolygonString = getSerializedPointsForShape("Line")
                                    initialIsLineMode = true
                                    showCustomPolygonDialog = true
                                }
                                else -> {
                                    val shapeType = content.removePrefix("Shape:").trim()
                                    val prefilledPoints = getSerializedPointsForShape(shapeType)
                                    if (prefilledPoints.isNotEmpty()) {
                                        polygonElementToEditId = element.id
                                        initialPolygonString = prefilledPoints
                                        initialIsLineMode = false
                                        showCustomPolygonDialog = true
                                    } else {
                                        contentDialogType =
                                            ContentDialogType.Edit(element.id, content)
                                    }
                                }
                            }
                        },
                        onRename = { element ->
                            elementToRenameId = element.id
                            renameText = getElementDisplayName(
                                element,
                                viewModel.canvasElements.indexOf(element),
                                viewModel.canvasElements
                            )
                        },
                        onEditProperties = { element ->
                            editPropertiesElementId = element.id
                            editX = element.offset.x.toString()
                            editY = element.offset.y.toString()
                            editScaleX = element.scaleX.toString()
                            editScaleY = element.scaleY.toString()
                            editRotation = element.rotation.toString()
                            editColorForDialog = if (!element.content.startsWith("Shape:") && !element.content.startsWith("Image:"))
                                element.textColor ?: Color.Black
                            else
                                element.backgroundColor
                            editShadowColorForDialog = element.shadowColor
                            editShadowOffsetX = element.shadowOffsetX
                            editShadowOffsetY = element.shadowOffsetY
                            editBorderThickness = element.borderThickness
                            editBorderColorForDialog = element.borderColor
                            showEditPropertiesDialog = true
                        },
                        onToggleVisibility = { viewModel.toggleVisibility(it) },
                        onToggleLock = { viewModel.toggleLock(it) },
                        onDuplicate = { viewModel.addToCanvas(CanvasElement(content = it.content)) },
                        onDelete = {
                            val idx = viewModel.canvasElements.indexOf(it)
                            if (idx != -1) viewModel.removeFromCanvas(idx)
                            if (selectedElementId == it.id) selectedElementId = null
                        },
                        onUngroup = { ids ->
                            viewModel.ungroupElements(ids)
                            selectedElementIds = emptySet()
                        },
                        onGroup = { name, ids ->
                            if (name.isNotBlank() && ids.isNotEmpty()) {
                                viewModel.createGroup(ids)
                                selectedElementIds = emptySet()
                                showGroupDialog = false
                                groupName = ""
                            }
                        },
                        onClearSelection = { selectedElementIds = emptySet() },
                        onReorder = { from, to -> viewModel.reorderCanvasElements(from, to) },
                        themeColors = themeColors,
                        density = LocalDensity.current,
                        groupNames = viewModel.groupNames,
                        onRenameGroup = { groupId, currentName ->
                            groupToRenameId = groupId
                            groupRenameText = currentName
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    when (val dialog = contentDialogType) {
        is ContentDialogType.Edit -> {
            EditElementDialog(
                elementId = dialog.elementId,
                initialContent = dialog.initialContent,
                onDismiss = { contentDialogType = null },
                onSave = { id, newContent ->
                    val index = viewModel.canvasElements.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val old = viewModel.canvasElements[index]
                        val updated = old.copy(content = newContent)
                        viewModel.removeFromCanvas(index)
                        viewModel.addToCanvas(updated)
                    }
                    contentDialogType = null
                }
            )
        }
        ContentDialogType.AddText -> {
            EditElementDialog(
                elementId = null,
                initialContent = "",
                isNew = true,
                onDismiss = { contentDialogType = null },
                onSave = { _, newContent ->
                    if (newContent.isNotBlank()) {
                        viewModel.addToCanvas(
                            CanvasElement(
                                content = newContent,
                                textColor = getRandomColor()
                            )
                        )
                    }
                    contentDialogType = null
                }
            )
        }
        ContentDialogType.FetchVerse -> {
            EditElementDialog(
                elementId = null,
                initialContent = "",
                isNew = true,
                fetchMode = true,
                dbHelper = dbHelper,
                viewModel = viewModel,
                verseProcessor = verseProcessor,
                themeColors = themeColors,
                onDismiss = { contentDialogType = null },
                onSave = { _, newContent ->
                    if (newContent.isNotBlank()) {
                        viewModel.addToCanvas(
                            CanvasElement(
                                content = newContent,
                                textColor = getRandomColor(),
                            )
                        )
                    }
                    contentDialogType = null
                }
            )
        }
        null -> {}
    }

    RenameDialog(
        elementId = elementToRenameId,
        currentName = renameText,
        onDismiss = {
            elementToRenameId = null
            renameText = ""
        },
        onConfirm = { id, newName ->
            if (newName.isNotBlank()) {
                viewModel.renameCanvasElement(id, newName)
            }
            elementToRenameId = null
            renameText = ""
        }
    )

    if (groupToRenameId != null) {
        RenameDialog(
            elementId = groupToRenameId,
            currentName = groupRenameText,
            title = "Rename Group",
            onDismiss = {
                groupToRenameId = null
                groupRenameText = ""
            },
            onConfirm = { id, newName ->
                if (newName.isNotBlank()) {
                    viewModel.renameGroup(id, newName)
                }
                groupToRenameId = null
                groupRenameText = ""
            }
        )
    }

    GroupDialog(
        show = showGroupDialog,
        initialName = groupName,
        onDismiss = { showGroupDialog = false },
        onConfirm = { name ->
            if (name.isNotBlank() && selectedElementIds.isNotEmpty()) {
                viewModel.createGroup(selectedElementIds.toList())
                selectedElementIds = emptySet()
                showGroupDialog = false
                groupName = ""
            }
        }
    )

    if (showColorPicker && elementToColorEditId != null) {
        val targetElement = viewModel.canvasElements.find { it.id == elementToColorEditId }
        val initialColor = when {
            targetElement == null -> Color.White
            targetElement.content.startsWith("Shape:") || targetElement.content.startsWith("Image:") ->
                targetElement.backgroundColor
            else -> targetElement.textColor ?: Color.Black
        }
        ColorWheelDialog(
            onDismissRequest = {
                showColorPicker = false
                elementToColorEditId = null
            },
            onColorSelected = { color ->
                val element = viewModel.canvasElements.find { it.id == elementToColorEditId }
                if (element != null && !element.content.startsWith("Shape:") && !element.content.startsWith("Image:")) {
                    viewModel.updateElementTextColor(elementToColorEditId!!, color)
                } else {
                    viewModel.updateElementColor(elementToColorEditId!!, color)
                }
                showColorPicker = false
                elementToColorEditId = null
            },
            initialColor = initialColor
        )
    }

    if (showCustomPolygonDialog) {
        CustomPolygonDialog(
            initialSerializedPoints = initialPolygonString.takeIf { it.isNotEmpty() },
            isLineMode = initialIsLineMode,
            onDismiss = {
                showCustomPolygonDialog = false
                polygonElementToEditId = null
                initialPolygonString = ""
                initialIsLineMode = false
            },
            onConfirm = { points, isLine ->
                val serialized = points.joinToString(";") { node ->
                    "${node.anchor.x},${node.anchor.y}:${node.handleIn.x},${node.handleIn.y}:${node.handleOut.x},${node.handleOut.y}"
                }
                val shapeType = if (isLine) "CustomLine" else "CustomPolygon"
                val contentString = "Shape:$shapeType:$serialized"
                if (polygonElementToEditId != null) {
                    viewModel.updateElementContent(polygonElementToEditId!!, contentString)
                    selectedElementId = polygonElementToEditId
                } else {
                    viewModel.addToCanvas(
                        CanvasElement(
                            content = contentString,
                            backgroundColor = getRandomColor(),
                            width = 100f,
                            height = 100f
                        )
                    )
                }
                showCustomPolygonDialog = false
                polygonElementToEditId = null
                initialPolygonString = ""
                initialIsLineMode = false
            }
        )
    }

    EditPropertiesDialog(
        show = showEditPropertiesDialog,
        elementId = editPropertiesElementId,
        initialX = editX,
        initialY = editY,
        initialScaleX = editScaleX,
        initialScaleY = editScaleY,
        initialRotation = editRotation,
        initialColor = editColorForDialog,
        proportionalEnabled = viewModel.proportionalEditing,
        onProportionalToggle = { viewModel.proportionalEditing = it },
        initialShadowColor = editShadowColorForDialog,
        initialShadowOffsetX = editShadowOffsetX,
        initialShadowOffsetY = editShadowOffsetY,
        initialBorderThickness = editBorderThickness,
        initialBorderColor = editBorderColorForDialog,
        onDismiss = {
            showEditPropertiesDialog = false
            editPropertiesElementId = null
        },
        onApply = { id, x, y, scaleX, scaleY, rot, color,
                    shadowColor, shadowOffsetX, shadowOffsetY,
                    borderThickness, borderColor ->

            val currentElement = viewModel.canvasElements.find { it.id == id }
            if (currentElement != null) {
                val isText = !currentElement.content.startsWith("Shape:") && !currentElement.content.startsWith("Image:")

                viewModel.applyAllElementProperties(
                    id = id,
                    x = x.toFloatOrNull() ?: currentElement.offset.x,
                    y = y.toFloatOrNull() ?: currentElement.offset.y,
                    width = currentElement.width,
                    height = currentElement.height,
                    rotation = rot.toFloatOrNull() ?: currentElement.rotation,
                    scaleX = scaleX.toFloatOrNull()?.coerceIn(0.1f, 10f) ?: currentElement.scaleX,
                    scaleY = scaleY.toFloatOrNull()?.coerceIn(0.1f, 10f) ?: currentElement.scaleY,
                    color = color,
                    isTextElement = isText,
                    shadowColor = shadowColor,
                    shadowOffsetX = shadowOffsetX,
                    shadowOffsetY = shadowOffsetY,
                    borderThickness = borderThickness,
                    borderColor = borderColor
                )
            }
            showEditPropertiesDialog = false
            editPropertiesElementId = null
        }
    )
}