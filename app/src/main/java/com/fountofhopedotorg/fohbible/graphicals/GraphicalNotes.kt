package com.fountofhopedotorg.fohbible.graphicals

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.composables.ColorWheelDialog
import com.fountofhopedotorg.fohbible.composables.CustomPolygonDialog
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.ReferenceResult
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.fetchByReference
import com.fountofhopedotorg.fohbible.functions.buildReferenceString
import com.fountofhopedotorg.fohbible.functions.getElementDisplayName
import com.fountofhopedotorg.fohbible.functions.getRandomColor
import com.fountofhopedotorg.fohbible.functions.getSerializedPointsForShape
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsImage
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsPDF
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsSVG
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun GraphicalNotesScreen() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
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
    var editWidth by rememberSaveable { mutableStateOf("") }
    var editHeight by rememberSaveable { mutableStateOf("") }
    var editRotation by rememberSaveable { mutableStateOf("") }
    var editPropertiesNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var editColorForDialog by remember { mutableStateOf(Color.White) }

    var showCustomPolygonDialog by rememberSaveable { mutableStateOf(false) }
    var selectedNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentText by rememberSaveable { mutableStateOf("") }
    var referenceInput by rememberSaveable { mutableStateOf("") }
    var fetchError by rememberSaveable { mutableStateOf<String?>(null) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var noteToColorEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedInputMode by rememberSaveable { mutableStateOf("Add Text") }
    var noteToEdit by rememberSaveable { mutableStateOf<String?>(null) }
    var editedNoteText by rememberSaveable { mutableStateOf("") }
    var showCanvasElementsTree by rememberSaveable { mutableStateOf(true) }
    var showSaveMenu by rememberSaveable { mutableStateOf(false) }
    var polygonNoteToEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var initialPolygonString by rememberSaveable { mutableStateOf("") }
    var initialIsLineMode by rememberSaveable { mutableStateOf(false) }
    var dragGroupDelta by remember { mutableStateOf(Offset.Zero) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addToCanvas(CanvasNote(content = "Image: $uri"))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(mainScrollState)
    ) {
        Spacer(Modifier.height(16.dp))

        InputModeSelector(
            selectedInputMode = selectedInputMode,
            onModeSelected = { selectedInputMode = it },
            themeColors = themeColors
        )

        Spacer(Modifier.height(16.dp))

        when (selectedInputMode) {
            "Add Text" -> AddTextSection(
                currentText = currentText,
                onTextChange = { currentText = it },
                onAdd = {
                    if (currentText.isNotBlank()) {
                        viewModel.addToCanvas(CanvasNote(content = currentText))
                        currentText = ""
                    }
                },
                themeColors = themeColors
            )
            "Fetch Verse" -> FetchVerseSection(
                referenceInput = referenceInput,
                onReferenceChange = { referenceInput = it },
                fetchError = fetchError,
                onFetch = {
                    fetchError = null
                    when (val result = fetchByReference(referenceInput, dbHelper)) {
                        is ReferenceResult.Single -> {
                            viewModel.fetchedVerses = listOf(result.verse)
                            viewModel.currentReference = buildReferenceString(result.bookName, result.verse.chapter, result.verse.verseNumber, null)
                        }
                        is ReferenceResult.Range -> {
                            viewModel.fetchedVerses = result.verses
                            val first = result.verses.first()
                            val last = result.verses.last()
                            viewModel.currentReference = buildReferenceString(result.bookName, first.chapter, first.verseNumber, last.verseNumber)
                        }
                        is ReferenceResult.Chapter -> {
                            viewModel.fetchedVerses = result.verses
                            val first = result.verses.first()
                            viewModel.currentReference = buildReferenceString(result.bookName, first.chapter, null, null)
                        }
                        ReferenceResult.Invalid -> {
                            viewModel.fetchedVerses = emptyList()
                            viewModel.currentReference = ""
                            fetchError = "Invalid reference or verse not found"
                        }
                    }
                },
                fetchedVerses = viewModel.fetchedVerses,
                currentReference = viewModel.currentReference,
                themeColors = themeColors,
                viewModel = viewModel,
                verseProcessor = verseProcessor
            )
            "Add SVG" -> AddSvgSection(
                onAddShape = { shape ->
                    val color = getRandomColor()
                    viewModel.addToCanvas(CanvasNote(content = "Shape: $shape", backgroundColor = color))
                },
                onCustomPolygon = {
                    polygonNoteToEditId = null
                    initialPolygonString = ""
                    initialIsLineMode = false
                    showCustomPolygonDialog = true
                },
                themeColors = themeColors
            )
            "Add Image" -> AddImageSection(
                onChooseFromGallery = { imagePickerLauncher.launch("image/*") },
                themeColors = themeColors
            )
        }

        Spacer(Modifier.height(20.dp))

        fun onGroupHeaderTap(groupId: String) {
            val members = viewModel.canvasNotes.filter { it.groupId == groupId }
            selectedNoteIds = members.map { it.id }.toSet()
            selectedNoteId = members.firstOrNull()?.id
        }

        CanvasElementsPanel(
            viewModel = viewModel,
            selectedNoteIds = selectedNoteIds,
            selectedNoteId = selectedNoteId,
            showTree = showCanvasElementsTree,
            onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
            onSingleSelect = { onSingleSelect(it) },
            onToggleGroupSelection = { toggleGroupSelection(it) },
            onGroupHeaderTap = { onGroupHeaderTap(it) },
            onEditNote = { note ->
                noteToEdit = note.id
                editedNoteText = note.content
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
                            noteToEdit = note.id
                            editedNoteText = content
                        }
                    }
                }
            },
            onRename = { note ->
                noteToRenameId = note.id
                renameText = getElementDisplayName(note, viewModel.canvasNotes.indexOf(note), viewModel.canvasNotes)
            },
            onEditProperties = { note ->
                editPropertiesNoteId = note.id
                editX = note.offset.x.toString()
                editY = note.offset.y.toString()
                editWidth = note.width.toString()
                editHeight = note.height.toString()
                editRotation = note.rotation.toString()
                editColorForDialog = note.backgroundColor
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
            themeColors = themeColors,
            density = LocalDensity.current,
            groupNames = viewModel.groupNames,
            onRenameGroup = { groupId, currentName ->
                groupToRenameId = groupId
                groupRenameText = currentName
            }
        )

        Spacer(Modifier.height(16.dp))

        CanvasArea(
            viewModel = viewModel,
            selectedNoteIds = selectedNoteIds,
            selectedNoteId = selectedNoteId,
            selectedGroups = selectedGroups,
            dragGroupDelta = dragGroupDelta,
            onGroupDragDeltaChange = { dragGroupDelta = it },
            onCanvasNoteTap = { onCanvasNoteTap(it) },
            onNoteUpdatePosition = { note, offset, w, h ->
                viewModel.updateNotePosition(note.id, offset, w, h)
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
            graphicsLayer = graphicsLayer
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                Button(onClick = { showSaveMenu = true }) {
                    Icon(Icons.Default.Save, contentDescription = "Save As", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Save As...", color = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand", tint = Color.White)
                }
                SaveAsMenu(
                    expanded = showSaveMenu,
                    onDismiss = { showSaveMenu = false },
                    onSavePng = { coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "PNG") } },
                    onSaveJpg = { coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "JPG") } },
                    onSavePdf = { coroutineScope.launch { saveCanvasAsPDF(graphicsLayer, context) } },
                    onSaveSvg = {
                        coroutineScope.launch {
                            saveCanvasAsSVG(graphicsLayer, context, viewModel.canvasNotes)
                        }
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    EditNoteDialog(
        noteId = noteToEdit,
        initialContent = editedNoteText,
        onDismiss = { noteToEdit = null },
        onSave = { id, newContent ->
            val index = viewModel.canvasNotes.indexOfFirst { it.id == id }
            if (index != -1) {
                val old = viewModel.canvasNotes[index]
                val updated = old.copy(content = newContent)
                viewModel.removeFromCanvas(index)
                viewModel.addToCanvas(updated)
            }
            noteToEdit = null
        }
    )

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
        ColorWheelDialog(
            onDismissRequest = {
                showColorPicker = false
                noteToColorEditId = null
            },
            onColorSelected = { color ->
                viewModel.updateNoteColor(noteToColorEditId!!, color)
                showColorPicker = false
                noteToColorEditId = null
            },
            initialColor = targetNote?.backgroundColor ?: Color.White
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
                    viewModel.addToCanvas(CanvasNote(content = contentString, backgroundColor = getRandomColor()))
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
        initialWidth = editWidth,
        initialHeight = editHeight,
        initialRotation = editRotation,
        initialColor = editColorForDialog,
        onDismiss = {
            showEditPropertiesDialog = false
            editPropertiesNoteId = null
        },
        onApply = { id, x, y, w, h, rot, color ->
            val xFloat = x.toFloatOrNull()
            val yFloat = y.toFloatOrNull()
            val wFloat = w.toFloatOrNull()
            val hFloat = h.toFloatOrNull()
            val rotFloat = rot.toFloatOrNull()
            if (xFloat != null && yFloat != null && wFloat != null && hFloat != null && rotFloat != null) {
                viewModel.updateNoteProperties(id, xFloat, yFloat, wFloat, hFloat, rotFloat)
                viewModel.updateNoteColor(id, color)
            }
            showEditPropertiesDialog = false
            editPropertiesNoteId = null
        }
    )
}
