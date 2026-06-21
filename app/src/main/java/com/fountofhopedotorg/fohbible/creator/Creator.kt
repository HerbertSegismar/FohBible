package com.fountofhopedotorg.fohbible.creator

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.fountofhopedotorg.fohbible.composables.ColorWheelDialog
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor

sealed class ContentDialogType {
    data class Edit(val noteId: String, val initialContent: String) : ContentDialogType()
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

    var noteToRenameId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var groupToRenameId by rememberSaveable { mutableStateOf<String?>(null) }
    var groupRenameText by rememberSaveable { mutableStateOf("") }

    var selectedNoteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showGroupDialog by rememberSaveable { mutableStateOf(false) }
    var groupName by rememberSaveable { mutableStateOf("") }
    var showEditPropertiesDialog by rememberSaveable { mutableStateOf(false) }
    var editX by rememberSaveable { mutableStateOf("") }
    var editY by rememberSaveable { mutableStateOf("") }
    var editScaleX by rememberSaveable { mutableStateOf("") }
    var editScaleY by rememberSaveable { mutableStateOf("") }
    var editRotation by rememberSaveable { mutableStateOf("") }
    var editPropertiesNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var editColorForDialog by remember { mutableStateOf(Color.White) }

    var showCustomPolygonDialog by rememberSaveable { mutableStateOf(false) }
    var selectedNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var noteToColorEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedInputMode by rememberSaveable { mutableStateOf("Add SVG") }
    var contentDialogType by remember { mutableStateOf<ContentDialogType?>(null) }
    var showCanvasElementsTree by rememberSaveable { mutableStateOf(true) }
    var polygonNoteToEditId by rememberSaveable { mutableStateOf<String?>(null) }
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
                CanvasNote(
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
    val notesGrouped = remember(viewModel.canvasNotes) {
        viewModel.canvasNotes.groupBy { it.groupId }
    }
    val selectedGroups = remember(selectedNoteIds, viewModel.canvasNotes) {
        viewModel.canvasNotes
            .filter { it.groupId != null && it.id in selectedNoteIds }
            .map { it.groupId!! }
            .toSet()
    }

    fun toggleGroupSelection(note: CanvasNote) {
        val groupId = note.groupId
        if (groupId != null) {
            val groupNotes = viewModel.canvasNotes.filter { it.groupId == groupId }
            val allIds = groupNotes.map { it.id }.toSet()
            selectedNoteIds = if (selectedNoteIds.containsAll(allIds)) {
                selectedNoteIds - allIds
            } else {
                selectedNoteIds + allIds
            }
        } else {
            selectedNoteIds = if (selectedNoteIds.contains(note.id))
                selectedNoteIds - note.id
            else
                selectedNoteIds + note.id
        }
    }

    fun onCanvasNoteTap(note: CanvasNote) {
        val groupId = note.groupId
        if (groupId != null) {
            val groupNotes = viewModel.canvasNotes.filter { it.groupId == groupId }
            selectedNoteIds = groupNotes.map { it.id }.toSet()
            selectedNoteId = note.id
        } else {
            selectedNoteIds = setOf(note.id)
            selectedNoteId = note.id
        }
    }

    fun onSingleSelect(note: CanvasNote) {
        selectedNoteId = note.id
        selectedNoteIds = emptySet()
    }

    fun onGroupHeaderTap(groupId: String) {
        val members = viewModel.canvasNotes.filter { it.groupId == groupId }
        selectedNoteIds = members.map { it.id }.toSet()
        selectedNoteId = members.firstOrNull()?.id
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
            ) {
                CombinedToolbarSection(
                    onAddShape = { shape ->
                        val color = getRandomColor()
                        viewModel.addToCanvas(
                            CanvasNote(content = "Shape: $shape", backgroundColor = color, width = 100f, height = 100f)
                        )
                    },
                    onCustomPolygon = {
                        polygonNoteToEditId = null
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
                    isFullScreen = viewModel.isGraphicalFullScreen,
                    onToggleFullScreen = {
                        viewModel.isGraphicalFullScreen = !viewModel.isGraphicalFullScreen
                    },
                    onChooseFromGallery = { imagePickerLauncher.launch("image/*") },
                    graphicsLayer = graphicsLayer,
                    isLandscape = true
                )

                CanvasArea(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    notes = viewModel.canvasNotes,
                    selectedNoteIds = selectedNoteIds,
                    selectedNoteId = selectedNoteId,
                    selectedGroups = selectedGroups,
                    dragGroupDelta = dragGroupDelta,
                    onGroupDragDeltaChange = { dragGroupDelta = it },
                    onCanvasNoteTap = { onCanvasNoteTap(it) },
                    onNoteUpdatePosition = { note, offset, w, h, rotation ->
                        viewModel.updateNoteProperties(
                            id = note.id,
                            x = offset.x,
                            y = offset.y,
                            width = w,
                            height = h,
                            rotation = rotation
                        )
                    },
                    onColorPickerRequested = {
                        noteToColorEditId = it
                        showColorPicker = true
                    },
                    onDeleteRequested = {
                        val idx = viewModel.canvasNotes.indexOfFirst { note -> note.id == it }
                        if (idx != -1) viewModel.removeFromCanvas(idx)
                    },
                    onClearSelection = {
                        selectedNoteIds = emptySet()
                        selectedNoteId = null
                    },
                    themeColors = themeColors,
                    isDark = isDark,
                    notesGrouped = notesGrouped,
                    graphicsLayer = graphicsLayer,
                    onNoteScaleChange = { id, sx, sy -> viewModel.updateNoteScale(id, sx, sy) },
                    proportionalEditing = viewModel.proportionalEditing,
                    onProportionalToggle = onProportionalToggle
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(top = 10.dp)
            ) {
                // ----- CHANGED -----
                CanvasElementsPanel(
                    notes = viewModel.canvasNotes,
                    selectedNoteIds = selectedNoteIds,
                    selectedNoteId = selectedNoteId,
                    showTree = showCanvasElementsTree,
                    onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
                    onSingleSelect = { onSingleSelect(it) },
                    onToggleGroupSelection = { toggleGroupSelection(it) },
                    onGroupHeaderTap = { onGroupHeaderTap(it) },
                    onEditNote = { note ->
                        contentDialogType = ContentDialogType.Edit(note.id, note.content)
                    },
                    onCustomPolygonEdit = { note ->
                        val content = note.content.trim()
                        when {
                            content.startsWith("Shape:CustomPolygon:") -> {
                                polygonNoteToEditId = note.id
                                initialPolygonString = content.removePrefix("Shape:CustomPolygon:")
                                initialIsLineMode = false
                                showCustomPolygonDialog = true
                            }
                            content.startsWith("Shape:CustomLine:") -> {
                                polygonNoteToEditId = note.id
                                initialPolygonString = content.removePrefix("Shape:CustomLine:")
                                initialIsLineMode = true
                                showCustomPolygonDialog = true
                            }
                            content.startsWith("Shape: Line") || content == "Shape: Line" -> {
                                polygonNoteToEditId = note.id
                                initialPolygonString = getSerializedPointsForShape("Line")
                                initialIsLineMode = true
                                showCustomPolygonDialog = true
                            }
                            else -> {
                                val shapeType = content.removePrefix("Shape:").trim()
                                val prefilledPoints = getSerializedPointsForShape(shapeType)
                                if (prefilledPoints.isNotEmpty()) {
                                    polygonNoteToEditId = note.id
                                    initialPolygonString = prefilledPoints
                                    initialIsLineMode = false
                                    showCustomPolygonDialog = true
                                } else {
                                    contentDialogType = ContentDialogType.Edit(note.id, content)
                                }
                            }
                        }
                    },
                    onRename = { note ->
                        noteToRenameId = note.id
                        renameText = getElementDisplayName(
                            note,
                            viewModel.canvasNotes.indexOf(note),
                            viewModel.canvasNotes
                        )
                    },
                    onEditProperties = { note ->
                        editPropertiesNoteId = note.id
                        editX = note.offset.x.toString()
                        editY = note.offset.y.toString()
                        editScaleX = note.scaleX.toString()
                        editScaleY = note.scaleY.toString()
                        editRotation = note.rotation.toString()
                        editColorForDialog = if (!note.content.startsWith("Shape:") && !note.content.startsWith("Image:"))
                            note.textColor ?: Color.Black
                        else
                            note.backgroundColor
                        editShadowColorForDialog = note.shadowColor
                        editShadowOffsetX = note.shadowOffsetX
                        editShadowOffsetY = note.shadowOffsetY
                        editBorderThickness = note.borderThickness
                        editBorderColorForDialog = note.borderColor
                        showEditPropertiesDialog = true
                    },
                    onToggleVisibility = { viewModel.toggleVisibility(it) },
                    onToggleLock = { viewModel.toggleLock(it) },
                    onDuplicate = { viewModel.addToCanvas(CanvasNote(content = it.content)) },
                    onDelete = {
                        val idx = viewModel.canvasNotes.indexOf(it)
                        if (idx != -1) viewModel.removeFromCanvas(idx)
                        if (selectedNoteId == it.id) selectedNoteId = null
                    },
                    onUngroup = { ids ->
                        viewModel.ungroupNotes(ids)
                        selectedNoteIds = emptySet()
                    },
                    onGroup = { name, ids ->
                        if (name.isNotBlank() && ids.isNotEmpty()) {
                            viewModel.createGroup(ids)
                            selectedNoteIds = emptySet()
                            showGroupDialog = false
                            groupName = ""
                        }
                    },
                    onClearSelection = { selectedNoteIds = emptySet() },
                    onReorder = { from, to -> viewModel.reorderCanvasNotes(from, to) },
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
                            CanvasNote(content = "Shape: $shape", backgroundColor = color, width = 100f, height = 100f)
                        )
                    },
                    onCustomPolygon = {
                        polygonNoteToEditId = null
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
                    isFullScreen = viewModel.isGraphicalFullScreen,
                    onToggleFullScreen = {
                        viewModel.isGraphicalFullScreen = !viewModel.isGraphicalFullScreen
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
                        notes = viewModel.canvasNotes,
                        selectedNoteIds = selectedNoteIds,
                        selectedNoteId = selectedNoteId,
                        selectedGroups = selectedGroups,
                        dragGroupDelta = dragGroupDelta,
                        onGroupDragDeltaChange = { dragGroupDelta = it },
                        onCanvasNoteTap = { onCanvasNoteTap(it) },
                        onNoteUpdatePosition = { note, offset, w, h, rotation ->
                            viewModel.updateNoteProperties(
                                id = note.id,
                                x = offset.x,
                                y = offset.y,
                                width = w,
                                height = h,
                                rotation = rotation
                            )
                        },
                        onNoteScaleChange = { id, sx, sy -> viewModel.updateNoteScale(id, sx, sy) },
                        onColorPickerRequested = {
                            noteToColorEditId = it
                            showColorPicker = true
                        },
                        onDeleteRequested = {
                            val idx = viewModel.canvasNotes.indexOfFirst { note -> note.id == it }
                            if (idx != -1) viewModel.removeFromCanvas(idx)
                        },
                        onClearSelection = {
                            selectedNoteIds = emptySet()
                            selectedNoteId = null
                        },
                        themeColors = themeColors,
                        isDark = isDark,
                        notesGrouped = notesGrouped,
                        graphicsLayer = graphicsLayer,
                        proportionalEditing = viewModel.proportionalEditing,
                        onProportionalToggle = onProportionalToggle
                    )

                    Spacer(Modifier.height(4.dp))

                    // ----- CHANGED -----
                    CanvasElementsPanel(
                        notes = viewModel.canvasNotes,
                        selectedNoteIds = selectedNoteIds,
                        selectedNoteId = selectedNoteId,
                        showTree = showCanvasElementsTree,
                        onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
                        onSingleSelect = { onSingleSelect(it) },
                        onToggleGroupSelection = { toggleGroupSelection(it) },
                        onGroupHeaderTap = { onGroupHeaderTap(it) },
                        onEditNote = { note ->
                            contentDialogType = ContentDialogType.Edit(note.id, note.content)
                        },
                        onCustomPolygonEdit = { note ->
                            val content = note.content.trim()
                            when {
                                content.startsWith("Shape:CustomPolygon:") -> {
                                    polygonNoteToEditId = note.id
                                    initialPolygonString =
                                        content.removePrefix("Shape:CustomPolygon:")
                                    initialIsLineMode = false
                                    showCustomPolygonDialog = true
                                }
                                content.startsWith("Shape:CustomLine:") -> {
                                    polygonNoteToEditId = note.id
                                    initialPolygonString =
                                        content.removePrefix("Shape:CustomLine:")
                                    initialIsLineMode = true
                                    showCustomPolygonDialog = true
                                }
                                content.startsWith("Shape: Line") || content == "Shape: Line" -> {
                                    polygonNoteToEditId = note.id
                                    initialPolygonString = getSerializedPointsForShape("Line")
                                    initialIsLineMode = true
                                    showCustomPolygonDialog = true
                                }
                                else -> {
                                    val shapeType = content.removePrefix("Shape:").trim()
                                    val prefilledPoints = getSerializedPointsForShape(shapeType)
                                    if (prefilledPoints.isNotEmpty()) {
                                        polygonNoteToEditId = note.id
                                        initialPolygonString = prefilledPoints
                                        initialIsLineMode = false
                                        showCustomPolygonDialog = true
                                    } else {
                                        contentDialogType =
                                            ContentDialogType.Edit(note.id, content)
                                    }
                                }
                            }
                        },
                        onRename = { note ->
                            noteToRenameId = note.id
                            renameText = getElementDisplayName(
                                note,
                                viewModel.canvasNotes.indexOf(note),
                                viewModel.canvasNotes
                            )
                        },
                        onEditProperties = { note ->
                            editPropertiesNoteId = note.id
                            editX = note.offset.x.toString()
                            editY = note.offset.y.toString()
                            editScaleX = note.scaleX.toString()
                            editScaleY = note.scaleY.toString()
                            editRotation = note.rotation.toString()
                            editColorForDialog = if (!note.content.startsWith("Shape:") && !note.content.startsWith("Image:"))
                                note.textColor ?: Color.Black
                            else
                                note.backgroundColor
                            editShadowColorForDialog = note.shadowColor
                            editShadowOffsetX = note.shadowOffsetX
                            editShadowOffsetY = note.shadowOffsetY
                            editBorderThickness = note.borderThickness
                            editBorderColorForDialog = note.borderColor
                            showEditPropertiesDialog = true
                        },
                        onToggleVisibility = { viewModel.toggleVisibility(it) },
                        onToggleLock = { viewModel.toggleLock(it) },
                        onDuplicate = { viewModel.addToCanvas(CanvasNote(content = it.content)) },
                        onDelete = {
                            val idx = viewModel.canvasNotes.indexOf(it)
                            if (idx != -1) viewModel.removeFromCanvas(idx)
                            if (selectedNoteId == it.id) selectedNoteId = null
                        },
                        onUngroup = { ids ->
                            viewModel.ungroupNotes(ids)
                            selectedNoteIds = emptySet()
                        },
                        onGroup = { name, ids ->
                            if (name.isNotBlank() && ids.isNotEmpty()) {
                                viewModel.createGroup(ids)
                                selectedNoteIds = emptySet()
                                showGroupDialog = false
                                groupName = ""
                            }
                        },
                        onClearSelection = { selectedNoteIds = emptySet() },
                        onReorder = { from, to -> viewModel.reorderCanvasNotes(from, to) },
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
            EditNoteDialog(
                noteId = dialog.noteId,
                initialContent = dialog.initialContent,
                onDismiss = { contentDialogType = null },
                onSave = { id, newContent ->
                    val index = viewModel.canvasNotes.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val old = viewModel.canvasNotes[index]
                        val updated = old.copy(content = newContent)
                        viewModel.removeFromCanvas(index)
                        viewModel.addToCanvas(updated)
                    }
                    contentDialogType = null
                }
            )
        }
        ContentDialogType.AddText -> {
            EditNoteDialog(
                noteId = null,
                initialContent = "",
                isNew = true,
                onDismiss = { contentDialogType = null },
                onSave = { _, newContent ->
                    if (newContent.isNotBlank()) {
                        viewModel.addToCanvas(
                            CanvasNote(
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
            EditNoteDialog(
                noteId = null,
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
                            CanvasNote(
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
        noteId = noteToRenameId,
        currentName = renameText,
        onDismiss = {
            noteToRenameId = null
            renameText = ""
        },
        onConfirm = { id, newName ->
            if (newName.isNotBlank()) {
                viewModel.renameCanvasNote(id, newName)
            }
            noteToRenameId = null
            renameText = ""
        }
    )

    if (groupToRenameId != null) {
        RenameDialog(
            noteId = groupToRenameId,
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
            if (name.isNotBlank() && selectedNoteIds.isNotEmpty()) {
                viewModel.createGroup(selectedNoteIds.toList())
                selectedNoteIds = emptySet()
                showGroupDialog = false
                groupName = ""
            }
        }
    )

    if (showColorPicker && noteToColorEditId != null) {
        val targetNote = viewModel.canvasNotes.find { it.id == noteToColorEditId }
        val initialColor = when {
            targetNote == null -> Color.White
            targetNote.content.startsWith("Shape:") || targetNote.content.startsWith("Image:") ->
                targetNote.backgroundColor
            else -> targetNote.textColor ?: Color.Black
        }
        ColorWheelDialog(
            onDismissRequest = {
                showColorPicker = false
                noteToColorEditId = null
            },
            onColorSelected = { color ->
                val note = viewModel.canvasNotes.find { it.id == noteToColorEditId }
                if (note != null && !note.content.startsWith("Shape:") && !note.content.startsWith("Image:")) {
                    viewModel.updateNoteTextColor(noteToColorEditId!!, color)
                } else {
                    viewModel.updateNoteColor(noteToColorEditId!!, color)
                }
                showColorPicker = false
                noteToColorEditId = null
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
                polygonNoteToEditId = null
                initialPolygonString = ""
                initialIsLineMode = false
            },
            onConfirm = { points, isLine ->
                val serialized = points.joinToString(";") { node ->
                    "${node.anchor.x},${node.anchor.y}:${node.handleIn.x},${node.handleIn.y}:${node.handleOut.x},${node.handleOut.y}"
                }
                val shapeType = if (isLine) "CustomLine" else "CustomPolygon"
                val contentString = "Shape:$shapeType:$serialized"
                if (polygonNoteToEditId != null) {
                    viewModel.updateNoteContent(polygonNoteToEditId!!, contentString)
                    selectedNoteId = polygonNoteToEditId
                } else {
                    viewModel.addToCanvas(
                        CanvasNote(
                            content = contentString,
                            backgroundColor = getRandomColor(),
                            width = 100f,
                            height = 100f
                        )
                    )
                }
                showCustomPolygonDialog = false
                polygonNoteToEditId = null
                initialPolygonString = ""
                initialIsLineMode = false
            }
        )
    }

    EditPropertiesDialog(
        show = showEditPropertiesDialog,
        noteId = editPropertiesNoteId,
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
            editPropertiesNoteId = null
        },
        onApply = { id, x, y, scaleX, scaleY, rot, color,
                    shadowColor, shadowOffsetX, shadowOffsetY,
                    borderThickness, borderColor ->

            val currentNote = viewModel.canvasNotes.find { it.id == id }
            if (currentNote != null) {
                val isText = !currentNote.content.startsWith("Shape:") && !currentNote.content.startsWith("Image:")

                viewModel.applyAllNoteProperties(
                    id = id,
                    x = x.toFloatOrNull() ?: currentNote.offset.x,
                    y = y.toFloatOrNull() ?: currentNote.offset.y,
                    width = currentNote.width,
                    height = currentNote.height,
                    rotation = rot.toFloatOrNull() ?: currentNote.rotation,
                    scaleX = scaleX.toFloatOrNull()?.coerceIn(0.1f, 10f) ?: currentNote.scaleX,
                    scaleY = scaleY.toFloatOrNull()?.coerceIn(0.1f, 10f) ?: currentNote.scaleY,
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
            editPropertiesNoteId = null
        }
    )
}