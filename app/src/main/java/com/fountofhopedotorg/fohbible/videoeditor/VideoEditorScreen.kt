package com.fountofhopedotorg.fohbible.videoeditor

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
import com.fountofhopedotorg.fohbible.creator.CanvasArea
import com.fountofhopedotorg.fohbible.creator.CanvasElementsPanel
import com.fountofhopedotorg.fohbible.creator.CombinedToolbarSection
import com.fountofhopedotorg.fohbible.creator.CustomPolygonDialog
import com.fountofhopedotorg.fohbible.creator.EditNoteDialog
import com.fountofhopedotorg.fohbible.creator.EditPropertiesDialog
import com.fountofhopedotorg.fohbible.creator.GroupDialog
import com.fountofhopedotorg.fohbible.creator.RenameDialog
import com.fountofhopedotorg.fohbible.creator.getElementDisplayName
import com.fountofhopedotorg.fohbible.creator.getRandomColor
import com.fountofhopedotorg.fohbible.creator.getSerializedPointsForShape
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.models.VideoContentDialogType
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun VideoEditorScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val viewModel: AppViewModel = viewModel()
    val graphicsLayer = rememberGraphicsLayer()

    // ---- Local UI toggles ----
    var showCanvasElementsTree by rememberSaveable { mutableStateOf(true) }
    var dragGroupDelta by remember { mutableStateOf(Offset.Zero) }

    // ---- Derived video data ----
    val notesGrouped = remember(viewModel.videoCanvasNotes) {
        viewModel.videoCanvasNotes.groupBy { it.groupId }
    }
    val selectedGroups = remember(viewModel.videoSelectedNoteIds, viewModel.videoCanvasNotes) {
        viewModel.videoCanvasNotes
            .filter { it.groupId != null && it.id in viewModel.videoSelectedNoteIds }
            .map { it.groupId!! }
            .toSet()
    }

    // ---- Selection helpers ----
    fun toggleGroupSelection(note: CanvasNote) {
        val groupId = note.groupId
        if (groupId != null) {
            val groupNotes = viewModel.videoCanvasNotes.filter { it.groupId == groupId }
            val allIds = groupNotes.map { it.id }.toSet()
            viewModel.videoSelectedNoteIds = if (viewModel.videoSelectedNoteIds.containsAll(allIds)) {
                viewModel.videoSelectedNoteIds - allIds
            } else {
                viewModel.videoSelectedNoteIds + allIds
            }
        } else {
            viewModel.videoSelectedNoteIds = if (viewModel.videoSelectedNoteIds.contains(note.id))
                viewModel.videoSelectedNoteIds - note.id
            else
                viewModel.videoSelectedNoteIds + note.id
        }
    }

    fun onCanvasNoteTap(note: CanvasNote) {
        val groupId = note.groupId
        if (groupId != null) {
            val groupNotes = viewModel.videoCanvasNotes.filter { it.groupId == groupId }
            viewModel.videoSelectedNoteIds = groupNotes.map { it.id }.toSet()
            viewModel.videoSelectedNoteId = note.id
        } else {
            viewModel.videoSelectedNoteIds = setOf(note.id)
            viewModel.videoSelectedNoteId = note.id
        }
    }

    fun onSingleSelect(note: CanvasNote) {
        viewModel.videoSelectedNoteId = note.id
        viewModel.videoSelectedNoteIds = emptySet()
    }

    fun onGroupHeaderTap(groupId: String) {
        val members = viewModel.videoCanvasNotes.filter { it.groupId == groupId }
        viewModel.videoSelectedNoteIds = members.map { it.id }.toSet()
        viewModel.videoSelectedNoteId = members.firstOrNull()?.id
    }

    val onProportionalToggle: () -> Unit = remember {
        { viewModel.proportionalEditing = !viewModel.proportionalEditing }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addToVideoCanvas(
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

    // ====================== Landscape Layout ======================
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
                        viewModel.addToVideoCanvas(
                            CanvasNote(content = "Shape: $shape", backgroundColor = color, width = 100f, height = 100f)
                        )
                    },
                    onCustomPolygon = {
                        viewModel.videoPolygonNoteToEditId = null
                        viewModel.videoInitialPolygonString = ""
                        viewModel.videoInitialIsLineMode = false
                        viewModel.videoShowCustomPolygonDialog = true
                    },
                    selectedInputMode = viewModel.videoSelectedInputMode,
                    onModeSelected = { mode ->
                        when (mode) {
                            "Add SVG" -> viewModel.videoSelectedInputMode = "Add SVG"
                            "Add Text" -> {
                                viewModel.videoSelectedInputMode = "Add Text"
                                viewModel.videoContentDialogType = VideoContentDialogType.AddText
                            }
                            "Fetch Verse" -> {
                                viewModel.videoSelectedInputMode = "Fetch Verse"
                                viewModel.videoContentDialogType = VideoContentDialogType.FetchVerse
                            }
                            else -> viewModel.videoSelectedInputMode = "Add SVG"
                        }
                    },
                    themeColors = themeColors,
                    isFullScreen = viewModel.videoIsGraphicalFullScreen,
                    onToggleFullScreen = {
                        viewModel.videoIsGraphicalFullScreen = !viewModel.videoIsGraphicalFullScreen
                    },
                    onChooseFromGallery = { imagePickerLauncher.launch("image/*") },
                    graphicsLayer = graphicsLayer,
                    isLandscape = true
                )

                CanvasArea(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    notes = viewModel.videoCanvasNotes,
                    selectedNoteIds = viewModel.videoSelectedNoteIds,
                    selectedNoteId = viewModel.videoSelectedNoteId,
                    selectedGroups = selectedGroups,
                    dragGroupDelta = dragGroupDelta,
                    onGroupDragDeltaChange = { dragGroupDelta = it },
                    onCanvasNoteTap = { onCanvasNoteTap(it) },
                    onNoteUpdatePosition = { note, offset, w, h, rotation ->
                        viewModel.updateVideoNoteProperties(
                            id = note.id,
                            x = offset.x,
                            y = offset.y,
                            width = w,
                            height = h,
                            rotation = rotation
                        )
                    },
                    onColorPickerRequested = {
                        viewModel.videoNoteToColorEditId = it
                        viewModel.videoShowColorPicker = true
                    },
                    onDeleteRequested = {
                        val idx = viewModel.videoCanvasNotes.indexOfFirst { note -> note.id == it }
                        if (idx != -1) viewModel.removeFromVideoCanvas(idx)
                    },
                    onClearSelection = {
                        viewModel.videoSelectedNoteIds = emptySet()
                        viewModel.videoSelectedNoteId = null
                    },
                    themeColors = themeColors,
                    isDark = isDark,
                    notesGrouped = notesGrouped,
                    graphicsLayer = graphicsLayer,
                    onNoteScaleChange = { id, sx, sy -> viewModel.updateVideoNoteScale(id, sx, sy) },
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
                CanvasElementsPanel(
                    notes = viewModel.videoCanvasNotes,                         // ✅ new
                    onReorder = { from, to ->
                        viewModel.reorderVideoCanvasNotes(from, to)            // ✅ new
                    },
                    selectedNoteIds = viewModel.videoSelectedNoteIds,
                    selectedNoteId = viewModel.videoSelectedNoteId,
                    showTree = showCanvasElementsTree,
                    onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
                    onSingleSelect = { onSingleSelect(it) },
                    onToggleGroupSelection = { toggleGroupSelection(it) },
                    onGroupHeaderTap = { onGroupHeaderTap(it) },
                    onEditNote = { note ->
                        viewModel.videoContentDialogType = VideoContentDialogType.Edit(note.id, note.content)
                    },
                    onCustomPolygonEdit = { note ->
                        val content = note.content.trim()
                        when {
                            content.startsWith("Shape:CustomPolygon:") -> {
                                viewModel.videoPolygonNoteToEditId = note.id
                                viewModel.videoInitialPolygonString = content.removePrefix("Shape:CustomPolygon:")
                                viewModel.videoInitialIsLineMode = false
                                viewModel.videoShowCustomPolygonDialog = true
                            }
                            content.startsWith("Shape:CustomLine:") -> {
                                viewModel.videoPolygonNoteToEditId = note.id
                                viewModel.videoInitialPolygonString = content.removePrefix("Shape:CustomLine:")
                                viewModel.videoInitialIsLineMode = true
                                viewModel.videoShowCustomPolygonDialog = true
                            }
                            content.startsWith("Shape: Line") || content == "Shape: Line" -> {
                                viewModel.videoPolygonNoteToEditId = note.id
                                viewModel.videoInitialPolygonString = getSerializedPointsForShape("Line")
                                viewModel.videoInitialIsLineMode = true
                                viewModel.videoShowCustomPolygonDialog = true
                            }
                            else -> {
                                val shapeType = content.removePrefix("Shape:").trim()
                                val prefilledPoints = getSerializedPointsForShape(shapeType)
                                if (prefilledPoints.isNotEmpty()) {
                                    viewModel.videoPolygonNoteToEditId = note.id
                                    viewModel.videoInitialPolygonString = prefilledPoints
                                    viewModel.videoInitialIsLineMode = false
                                    viewModel.videoShowCustomPolygonDialog = true
                                } else {
                                    viewModel.videoContentDialogType = VideoContentDialogType.Edit(note.id, content)
                                }
                            }
                        }
                    },
                    onRename = { note ->
                        viewModel.videoNoteToRenameId = note.id
                        viewModel.videoRenameText = getElementDisplayName(
                            note,
                            viewModel.videoCanvasNotes.indexOf(note),
                            viewModel.videoCanvasNotes
                        )
                    },
                    onEditProperties = { note ->
                        viewModel.videoEditPropertiesNoteId = note.id
                        viewModel.videoEditX = note.offset.x.toString()
                        viewModel.videoEditY = note.offset.y.toString()
                        viewModel.videoEditScaleX = note.scaleX.toString()
                        viewModel.videoEditScaleY = note.scaleY.toString()
                        viewModel.videoEditRotation = note.rotation.toString()
                        viewModel.videoEditColorForDialog = if (!note.content.startsWith("Shape:") && !note.content.startsWith("Image:"))
                            note.textColor ?: Color.Black
                        else
                            note.backgroundColor
                        viewModel.videoEditShadowColorForDialog = note.shadowColor
                        viewModel.videoEditShadowOffsetX = note.shadowOffsetX
                        viewModel.videoEditShadowOffsetY = note.shadowOffsetY
                        viewModel.videoEditBorderThickness = note.borderThickness
                        viewModel.videoEditBorderColorForDialog = note.borderColor
                        viewModel.videoShowEditPropertiesDialog = true
                    },
                    onToggleVisibility = { viewModel.toggleVideoVisibility(it) },
                    onToggleLock = { viewModel.toggleVideoLock(it) },
                    onDuplicate = { viewModel.addToVideoCanvas(CanvasNote(content = it.content)) },
                    onDelete = {
                        val idx = viewModel.videoCanvasNotes.indexOf(it)
                        if (idx != -1) viewModel.removeFromVideoCanvas(idx)
                        if (viewModel.videoSelectedNoteId == it.id) viewModel.videoSelectedNoteId = null
                    },
                    onUngroup = { ids ->
                        viewModel.ungroupVideoNotes(ids)
                        viewModel.videoSelectedNoteIds = emptySet()
                    },
                    onGroup = { name, ids ->
                        if (name.isNotBlank() && ids.isNotEmpty()) {
                            viewModel.createVideoGroup(ids)
                            viewModel.videoSelectedNoteIds = emptySet()
                            viewModel.videoShowGroupDialog = false
                            viewModel.videoGroupName = ""
                        }
                    },
                    onClearSelection = { viewModel.videoSelectedNoteIds = emptySet() },
                    themeColors = themeColors,
                    density = LocalDensity.current,
                    groupNames = viewModel.videoGroupNames,
                    onRenameGroup = { groupId, currentName ->
                        viewModel.videoGroupToRenameId = groupId
                        viewModel.videoGroupRenameText = currentName
                    }
                )
            }
        }
    } else {
        // ====================== Portrait Layout ======================
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
            ) {
                CombinedToolbarSection(
                    onAddShape = { shape ->
                        val color = getRandomColor()
                        viewModel.addToVideoCanvas(
                            CanvasNote(content = "Shape: $shape", backgroundColor = color, width = 100f, height = 100f)
                        )
                    },
                    onCustomPolygon = {
                        viewModel.videoPolygonNoteToEditId = null
                        viewModel.videoInitialPolygonString = ""
                        viewModel.videoInitialIsLineMode = false
                        viewModel.videoShowCustomPolygonDialog = true
                    },
                    selectedInputMode = viewModel.videoSelectedInputMode,
                    onModeSelected = { mode ->
                        when (mode) {
                            "Add SVG" -> viewModel.videoSelectedInputMode = "Add SVG"
                            "Add Text" -> {
                                viewModel.videoSelectedInputMode = "Add Text"
                                viewModel.videoContentDialogType = VideoContentDialogType.AddText
                            }
                            "Fetch Verse" -> {
                                viewModel.videoSelectedInputMode = "Fetch Verse"
                                viewModel.videoContentDialogType = VideoContentDialogType.FetchVerse
                            }
                            else -> viewModel.videoSelectedInputMode = "Add SVG"
                        }
                    },
                    themeColors = themeColors,
                    isFullScreen = viewModel.videoIsGraphicalFullScreen,
                    onToggleFullScreen = {
                        viewModel.videoIsGraphicalFullScreen = !viewModel.videoIsGraphicalFullScreen
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
                        notes = viewModel.videoCanvasNotes,
                        selectedNoteIds = viewModel.videoSelectedNoteIds,
                        selectedNoteId = viewModel.videoSelectedNoteId,
                        selectedGroups = selectedGroups,
                        dragGroupDelta = dragGroupDelta,
                        onGroupDragDeltaChange = { dragGroupDelta = it },
                        onCanvasNoteTap = { onCanvasNoteTap(it) },
                        onNoteUpdatePosition = { note, offset, w, h, rotation ->
                            viewModel.updateVideoNoteProperties(
                                id = note.id,
                                x = offset.x,
                                y = offset.y,
                                width = w,
                                height = h,
                                rotation = rotation
                            )
                        },
                        onNoteScaleChange = { id, sx, sy -> viewModel.updateVideoNoteScale(id, sx, sy) },
                        onColorPickerRequested = {
                            viewModel.videoNoteToColorEditId = it
                            viewModel.videoShowColorPicker = true
                        },
                        onDeleteRequested = {
                            val idx = viewModel.videoCanvasNotes.indexOfFirst { note -> note.id == it }
                            if (idx != -1) viewModel.removeFromVideoCanvas(idx)
                        },
                        onClearSelection = {
                            viewModel.videoSelectedNoteIds = emptySet()
                            viewModel.videoSelectedNoteId = null
                        },
                        themeColors = themeColors,
                        isDark = isDark,
                        notesGrouped = notesGrouped,
                        graphicsLayer = graphicsLayer,
                        proportionalEditing = viewModel.proportionalEditing,
                        onProportionalToggle = onProportionalToggle
                    )

                    Spacer(Modifier.height(4.dp))

                    CanvasElementsPanel(
                        notes = viewModel.videoCanvasNotes,                         // ✅ new
                        onReorder = { from, to ->
                            viewModel.reorderVideoCanvasNotes(from, to)            // ✅ new
                        },
                        selectedNoteIds = viewModel.videoSelectedNoteIds,
                        selectedNoteId = viewModel.videoSelectedNoteId,
                        showTree = showCanvasElementsTree,
                        onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
                        onSingleSelect = { onSingleSelect(it) },
                        onToggleGroupSelection = { toggleGroupSelection(it) },
                        onGroupHeaderTap = { onGroupHeaderTap(it) },
                        onEditNote = { note ->
                            viewModel.videoContentDialogType = VideoContentDialogType.Edit(note.id, note.content)
                        },
                        onCustomPolygonEdit = { note ->
                            val content = note.content.trim()
                            when {
                                content.startsWith("Shape:CustomPolygon:") -> {
                                    viewModel.videoPolygonNoteToEditId = note.id
                                    viewModel.videoInitialPolygonString = content.removePrefix("Shape:CustomPolygon:")
                                    viewModel.videoInitialIsLineMode = false
                                    viewModel.videoShowCustomPolygonDialog = true
                                }
                                content.startsWith("Shape:CustomLine:") -> {
                                    viewModel.videoPolygonNoteToEditId = note.id
                                    viewModel.videoInitialPolygonString = content.removePrefix("Shape:CustomLine:")
                                    viewModel.videoInitialIsLineMode = true
                                    viewModel.videoShowCustomPolygonDialog = true
                                }
                                content.startsWith("Shape: Line") || content == "Shape: Line" -> {
                                    viewModel.videoPolygonNoteToEditId = note.id
                                    viewModel.videoInitialPolygonString = getSerializedPointsForShape("Line")
                                    viewModel.videoInitialIsLineMode = true
                                    viewModel.videoShowCustomPolygonDialog = true
                                }
                                else -> {
                                    val shapeType = content.removePrefix("Shape:").trim()
                                    val prefilledPoints = getSerializedPointsForShape(shapeType)
                                    if (prefilledPoints.isNotEmpty()) {
                                        viewModel.videoPolygonNoteToEditId = note.id
                                        viewModel.videoInitialPolygonString = prefilledPoints
                                        viewModel.videoInitialIsLineMode = false
                                        viewModel.videoShowCustomPolygonDialog = true
                                    } else {
                                        viewModel.videoContentDialogType = VideoContentDialogType.Edit(note.id, content)
                                    }
                                }
                            }
                        },
                        onRename = { note ->
                            viewModel.videoNoteToRenameId = note.id
                            viewModel.videoRenameText = getElementDisplayName(
                                note,
                                viewModel.videoCanvasNotes.indexOf(note),
                                viewModel.videoCanvasNotes
                            )
                        },
                        onEditProperties = { note ->
                            viewModel.videoEditPropertiesNoteId = note.id
                            viewModel.videoEditX = note.offset.x.toString()
                            viewModel.videoEditY = note.offset.y.toString()
                            viewModel.videoEditScaleX = note.scaleX.toString()
                            viewModel.videoEditScaleY = note.scaleY.toString()
                            viewModel.videoEditRotation = note.rotation.toString()
                            viewModel.videoEditColorForDialog = if (!note.content.startsWith("Shape:") && !note.content.startsWith("Image:"))
                                note.textColor ?: Color.Black
                            else
                                note.backgroundColor
                            viewModel.videoEditShadowColorForDialog = note.shadowColor
                            viewModel.videoEditShadowOffsetX = note.shadowOffsetX
                            viewModel.videoEditShadowOffsetY = note.shadowOffsetY
                            viewModel.videoEditBorderThickness = note.borderThickness
                            viewModel.videoEditBorderColorForDialog = note.borderColor
                            viewModel.videoShowEditPropertiesDialog = true
                        },
                        onToggleVisibility = { viewModel.toggleVideoVisibility(it) },
                        onToggleLock = { viewModel.toggleVideoLock(it) },
                        onDuplicate = { viewModel.addToVideoCanvas(CanvasNote(content = it.content)) },
                        onDelete = {
                            val idx = viewModel.videoCanvasNotes.indexOf(it)
                            if (idx != -1) viewModel.removeFromVideoCanvas(idx)
                            if (viewModel.videoSelectedNoteId == it.id) viewModel.videoSelectedNoteId = null
                        },
                        onUngroup = { ids ->
                            viewModel.ungroupVideoNotes(ids)
                            viewModel.videoSelectedNoteIds = emptySet()
                        },
                        onGroup = { name, ids ->
                            if (name.isNotBlank() && ids.isNotEmpty()) {
                                viewModel.createVideoGroup(ids)
                                viewModel.videoSelectedNoteIds = emptySet()
                                viewModel.videoShowGroupDialog = false
                                viewModel.videoGroupName = ""
                            }
                        },
                        onClearSelection = { viewModel.videoSelectedNoteIds = emptySet() },
                        themeColors = themeColors,
                        density = LocalDensity.current,
                        groupNames = viewModel.videoGroupNames,
                        onRenameGroup = { groupId, currentName ->
                            viewModel.videoGroupToRenameId = groupId
                            viewModel.videoGroupRenameText = currentName
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // ====================== Dialogs ======================
    when (val dialog = viewModel.videoContentDialogType) {
        is VideoContentDialogType.Edit -> {
            EditNoteDialog(
                noteId = dialog.noteId,
                initialContent = dialog.initialContent,
                onDismiss = { viewModel.videoContentDialogType = null },
                onSave = { id, newContent ->
                    val index = viewModel.videoCanvasNotes.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val old = viewModel.videoCanvasNotes[index]
                        val updated = old.copy(content = newContent)
                        viewModel.removeFromVideoCanvas(index)
                        viewModel.addToVideoCanvas(updated)
                    }
                    viewModel.videoContentDialogType = null
                }
            )
        }
        VideoContentDialogType.AddText -> {
            EditNoteDialog(
                noteId = null,
                initialContent = "",
                isNew = true,
                onDismiss = { viewModel.videoContentDialogType = null },
                onSave = { _, newContent ->
                    if (newContent.isNotBlank()) {
                        viewModel.addToVideoCanvas(
                            CanvasNote(
                                content = newContent,
                                textColor = getRandomColor()
                            )
                        )
                    }
                    viewModel.videoContentDialogType = null
                }
            )
        }
        VideoContentDialogType.FetchVerse -> {
            EditNoteDialog(
                noteId = null,
                initialContent = "",
                isNew = true,
                fetchMode = true,
                dbHelper = dbHelper,
                viewModel = viewModel,
                verseProcessor = verseProcessor,
                themeColors = themeColors,
                onDismiss = { viewModel.videoContentDialogType = null },
                onSave = { _, newContent ->
                    if (newContent.isNotBlank()) {
                        viewModel.addToVideoCanvas(
                            CanvasNote(
                                content = newContent,
                                textColor = getRandomColor(),
                            )
                        )
                    }
                    viewModel.videoContentDialogType = null
                }
            )
        }
        null -> {}
    }

    RenameDialog(
        noteId = viewModel.videoNoteToRenameId,
        currentName = viewModel.videoRenameText,
        onDismiss = {
            viewModel.videoNoteToRenameId = null
            viewModel.videoRenameText = ""
        },
        onConfirm = { id, newName ->
            if (newName.isNotBlank()) {
                viewModel.renameVideoCanvasNote(id, newName)
            }
            viewModel.videoNoteToRenameId = null
            viewModel.videoRenameText = ""
        }
    )

    if (viewModel.videoGroupToRenameId != null) {
        RenameDialog(
            noteId = viewModel.videoGroupToRenameId,
            currentName = viewModel.videoGroupRenameText,
            title = "Rename Group",
            onDismiss = {
                viewModel.videoGroupToRenameId = null
                viewModel.videoGroupRenameText = ""
            },
            onConfirm = { id, newName ->
                if (newName.isNotBlank()) {
                    viewModel.renameVideoGroup(id, newName)
                }
                viewModel.videoGroupToRenameId = null
                viewModel.videoGroupRenameText = ""
            }
        )
    }

    GroupDialog(
        show = viewModel.videoShowGroupDialog,
        initialName = viewModel.videoGroupName,
        onDismiss = { viewModel.videoShowGroupDialog = false },
        onConfirm = { name ->
            if (name.isNotBlank() && viewModel.videoSelectedNoteIds.isNotEmpty()) {
                viewModel.createVideoGroup(viewModel.videoSelectedNoteIds.toList())
                viewModel.videoSelectedNoteIds = emptySet()
                viewModel.videoShowGroupDialog = false
                viewModel.videoGroupName = ""
            }
        }
    )

    if (viewModel.videoShowColorPicker && viewModel.videoNoteToColorEditId != null) {
        val targetNote = viewModel.videoCanvasNotes.find { it.id == viewModel.videoNoteToColorEditId }
        val initialColor = when {
            targetNote == null -> Color.White
            targetNote.content.startsWith("Shape:") || targetNote.content.startsWith("Image:") ->
                targetNote.backgroundColor
            else -> targetNote.textColor ?: Color.Black
        }
        ColorWheelDialog(
            onDismissRequest = {
                viewModel.videoShowColorPicker = false
                viewModel.videoNoteToColorEditId = null
            },
            onColorSelected = { color ->
                val note = viewModel.videoCanvasNotes.find { it.id == viewModel.videoNoteToColorEditId }
                if (note != null && !note.content.startsWith("Shape:") && !note.content.startsWith("Image:")) {
                    viewModel.updateVideoNoteTextColor(viewModel.videoNoteToColorEditId!!, color)
                } else {
                    viewModel.updateVideoNoteColor(viewModel.videoNoteToColorEditId!!, color)
                }
                viewModel.videoShowColorPicker = false
                viewModel.videoNoteToColorEditId = null
            },
            initialColor = initialColor
        )
    }

    if (viewModel.videoShowCustomPolygonDialog) {
        CustomPolygonDialog(
            initialSerializedPoints = viewModel.videoInitialPolygonString.takeIf { it.isNotEmpty() },
            isLineMode = viewModel.videoInitialIsLineMode,
            onDismiss = {
                viewModel.videoShowCustomPolygonDialog = false
                viewModel.videoPolygonNoteToEditId = null
                viewModel.videoInitialPolygonString = ""
                viewModel.videoInitialIsLineMode = false
            },
            onConfirm = { points, isLine ->
                val serialized = points.joinToString(";") { node ->
                    "${node.anchor.x},${node.anchor.y}:${node.handleIn.x},${node.handleIn.y}:${node.handleOut.x},${node.handleOut.y}"
                }
                val shapeType = if (isLine) "CustomLine" else "CustomPolygon"
                val contentString = "Shape:$shapeType:$serialized"
                if (viewModel.videoPolygonNoteToEditId != null) {
                    viewModel.updateVideoNoteContent(viewModel.videoPolygonNoteToEditId!!, contentString)
                    viewModel.videoSelectedNoteId = viewModel.videoPolygonNoteToEditId
                } else {
                    viewModel.addToVideoCanvas(
                        CanvasNote(
                            content = contentString,
                            backgroundColor = getRandomColor(),
                            width = 100f,
                            height = 100f
                        )
                    )
                }
                viewModel.videoShowCustomPolygonDialog = false
                viewModel.videoPolygonNoteToEditId = null
                viewModel.videoInitialPolygonString = ""
                viewModel.videoInitialIsLineMode = false
            }
        )
    }

    EditPropertiesDialog(
        show = viewModel.videoShowEditPropertiesDialog,
        noteId = viewModel.videoEditPropertiesNoteId,
        initialX = viewModel.videoEditX,
        initialY = viewModel.videoEditY,
        initialScaleX = viewModel.videoEditScaleX,
        initialScaleY = viewModel.videoEditScaleY,
        initialRotation = viewModel.videoEditRotation,
        initialColor = viewModel.videoEditColorForDialog,
        proportionalEnabled = viewModel.proportionalEditing,
        onProportionalToggle = { viewModel.proportionalEditing = it },
        initialShadowColor = viewModel.videoEditShadowColorForDialog,
        initialShadowOffsetX = viewModel.videoEditShadowOffsetX,
        initialShadowOffsetY = viewModel.videoEditShadowOffsetY,
        initialBorderThickness = viewModel.videoEditBorderThickness,
        initialBorderColor = viewModel.videoEditBorderColorForDialog,
        onDismiss = {
            viewModel.videoShowEditPropertiesDialog = false
            viewModel.videoEditPropertiesNoteId = null
        },
        onApply = { id, x, y, scaleX, scaleY, rot, color,
                    shadowColor, shadowOffsetX, shadowOffsetY,
                    borderThickness, borderColor ->

            val currentNote = viewModel.videoCanvasNotes.find { it.id == id }
            if (currentNote != null) {
                val isText = !currentNote.content.startsWith("Shape:") && !currentNote.content.startsWith("Image:")

                viewModel.applyAllVideoNoteProperties(
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
            viewModel.videoShowEditPropertiesDialog = false
            viewModel.videoEditPropertiesNoteId = null
        }
    )
}