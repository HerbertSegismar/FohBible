package com.fountofhopedotorg.fohbible.videoeditor

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.composables.ColorWheelDialog
import com.fountofhopedotorg.fohbible.creator.CanvasArea
import com.fountofhopedotorg.fohbible.creator.CombinedToolbarSection
import com.fountofhopedotorg.fohbible.creator.EditNoteDialog
import com.fountofhopedotorg.fohbible.creator.GroupDialog
import com.fountofhopedotorg.fohbible.creator.RenameDialog
import com.fountofhopedotorg.fohbible.creator.getElementDisplayName
import com.fountofhopedotorg.fohbible.creator.getRandomColor
import com.fountofhopedotorg.fohbible.creator.getSerializedPointsForShape
import com.fountofhopedotorg.fohbible.data.CanvasKeyframe
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.models.VideoContentDialogType
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun VideoEditorScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val viewModel: AppViewModel = viewModel()
    val graphicsLayer = rememberGraphicsLayer()

    var showCanvasElementsTree by rememberSaveable { mutableStateOf(true) }
    var dragGroupDelta by remember { mutableStateOf(Offset.Zero) }

    // Animation state
    var isPlayingAnimation by remember { mutableStateOf(false) }
    var animationCurrentTimeMs by remember { mutableLongStateOf(0L) }
    var animatingNoteId by remember { mutableStateOf<String?>(null) }
    var originalNoteState by remember { mutableStateOf<CanvasNote?>(null) }

    val notesGrouped = remember(viewModel.videoCanvasNotes) {
        viewModel.videoCanvasNotes.groupBy { it.groupId }
    }
    val selectedGroups = remember(viewModel.videoSelectedNoteIds, viewModel.videoCanvasNotes) {
        viewModel.videoCanvasNotes
            .filter { it.groupId != null && it.id in viewModel.videoSelectedNoteIds }
            .map { it.groupId!! }
            .toSet()
    }

    // ---- Helper functions for selection and canvas interaction ----
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

    // ---- Animation loop ----
    LaunchedEffect(isPlayingAnimation, animatingNoteId) {
        if (!isPlayingAnimation || animatingNoteId == null) return@LaunchedEffect

        val note = viewModel.videoCanvasNotes.firstOrNull { it.id == animatingNoteId }
        if (note == null || note.keyframes.isEmpty()) {
            isPlayingAnimation = false
            return@LaunchedEffect
        }

        val sortedKeyframes = note.keyframes.sortedBy { it.timestampMs }
        if (sortedKeyframes.isEmpty()) {
            isPlayingAnimation = false
            return@LaunchedEffect
        }

        val lastTimestamp = sortedKeyframes.last().timestampMs

        while (isActive && isPlayingAnimation) {
            val currentMs = animationCurrentTimeMs
            val noteNow = viewModel.videoCanvasNotes.firstOrNull { it.id == animatingNoteId }
            if (noteNow == null) {
                isPlayingAnimation = false
                break
            }

            val (kfPrev, kfNext) = findSurroundingKeyframes(sortedKeyframes, currentMs)
            val progress = if (kfNext != null && kfNext.timestampMs != kfPrev!!.timestampMs) {
                ((currentMs - kfPrev.timestampMs).toFloat() /
                        (kfNext.timestampMs - kfPrev.timestampMs)).coerceIn(0f, 1f)
            } else 0f

            val newX = lerp(kfPrev?.x ?: noteNow.offset.x, kfNext?.x ?: noteNow.offset.x, progress)
            val newY = lerp(kfPrev?.y ?: noteNow.offset.y, kfNext?.y ?: noteNow.offset.y, progress)
            val newScaleX = lerp(kfPrev?.scaleX ?: noteNow.scaleX, kfNext?.scaleX ?: noteNow.scaleX, progress)
            val newScaleY = lerp(kfPrev?.scaleY ?: noteNow.scaleY, kfNext?.scaleY ?: noteNow.scaleY, progress)
            val newRotation = lerp(kfPrev?.rotation ?: noteNow.rotation, kfNext?.rotation ?: noteNow.rotation, progress)

            val idx = viewModel.videoCanvasNotes.indexOfFirst { it.id == animatingNoteId }
            if (idx != -1) {
                viewModel.videoCanvasNotes[idx] = viewModel.videoCanvasNotes[idx].copy(
                    offset = Offset(newX, newY),
                    scaleX = newScaleX,
                    scaleY = newScaleY,
                    rotation = newRotation
                )
            }

            animationCurrentTimeMs += 16
            if (animationCurrentTimeMs > lastTimestamp + 500) {
                isPlayingAnimation = false
                break
            }
            delay(16.milliseconds)
        }

        // Reset note to original state when animation stops
        if (!isPlayingAnimation && originalNoteState != null) {
            val orig = originalNoteState!!
            val idx = viewModel.videoCanvasNotes.indexOfFirst { it.id == orig.id }
            if (idx != -1) {
                viewModel.videoCanvasNotes[idx] = viewModel.videoCanvasNotes[idx].copy(
                    offset = orig.offset,
                    scaleX = orig.scaleX,
                    scaleY = orig.scaleY,
                    rotation = orig.rotation
                )
            }
            originalNoteState = null
            animatingNoteId = null
        }
    }

    // ---- Layout ----
    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left column: toolbar + canvas
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

                Box(modifier = Modifier
                    .weight(1f).fillMaxSize()) {
                    CanvasArea(
                        modifier = Modifier
                            .fillMaxSize(),
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

                    // Overlay while animation is playing (block input)
                    if (isPlayingAnimation) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                                .pointerInput(Unit) {},  // consume all touch events
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Playing Animation…",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Right column: elements panel and toolbar buttons
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(top = 10.dp)
            ) {
                // Toolbar buttons for animation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectedNote = viewModel.videoCanvasNotes.firstOrNull { it.id == viewModel.videoSelectedNoteId }
                    val hasKeyframes = selectedNote?.keyframes?.isNotEmpty() == true

                    IconButton(
                        onClick = {
                            if (isPlayingAnimation) {
                                isPlayingAnimation = false
                            } else if (selectedNote != null) {
                                viewModel.videoKeyframeTargetNoteId = selectedNote.id
                                viewModel.videoShowKeyframeDialog = true
                            }
                        },
                        enabled = viewModel.videoSelectedNoteId != null && !isPlayingAnimation
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Keyframe Animation",
                            tint = if (viewModel.videoSelectedNoteId != null && !isPlayingAnimation)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            if (isPlayingAnimation) {
                                isPlayingAnimation = false
                            } else {
                                selectedNote?.let { note ->
                                    originalNoteState = note.copy()
                                    animatingNoteId = note.id
                                    animationCurrentTimeMs = 0L
                                    isPlayingAnimation = true
                                }
                            }
                        },
                        enabled = viewModel.videoSelectedNoteId != null && (hasKeyframes || isPlayingAnimation)
                    ) {
                        Icon(
                            imageVector = if (isPlayingAnimation) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingAnimation) "Stop Animation" else "Play Animation",
                            tint = if (isPlayingAnimation) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                VideoCanvasElementsPanel(
                    notes = viewModel.videoCanvasNotes,
                    onReorder = { from, to ->
                        viewModel.reorderVideoCanvasNotes(from, to)
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
                    onAnimateKeyframes = { note ->
                        viewModel.videoKeyframeTargetNoteId = note.id
                        viewModel.videoShowKeyframeDialog = true
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
        // Portrait layout
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
                // Animation toolbar row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectedNote = viewModel.videoCanvasNotes.firstOrNull { it.id == viewModel.videoSelectedNoteId }
                    val hasKeyframes = selectedNote?.keyframes?.isNotEmpty() == true

                    IconButton(
                        onClick = {
                            if (isPlayingAnimation) {
                                isPlayingAnimation = false
                            } else if (selectedNote != null) {
                                viewModel.videoKeyframeTargetNoteId = selectedNote.id
                                viewModel.videoShowKeyframeDialog = true
                            }
                        },
                        enabled = viewModel.videoSelectedNoteId != null && !isPlayingAnimation
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Keyframe Animation",
                            tint = if (viewModel.videoSelectedNoteId != null && !isPlayingAnimation)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            if (isPlayingAnimation) {
                                isPlayingAnimation = false
                            } else {
                                selectedNote?.let { note ->
                                    originalNoteState = note.copy()
                                    animatingNoteId = note.id
                                    animationCurrentTimeMs = 0L
                                    isPlayingAnimation = true
                                }
                            }
                        },
                        enabled = viewModel.videoSelectedNoteId != null && (hasKeyframes || isPlayingAnimation)
                    ) {
                        Icon(
                            imageVector = if (isPlayingAnimation) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingAnimation) "Stop Animation" else "Play Animation",
                            tint = if (isPlayingAnimation) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.99f)
                        .weight(1f)
                        .verticalScroll(mainScrollState)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
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

                        if (isPlayingAnimation) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .pointerInput(Unit) {},
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Playing Animation…",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    VideoCanvasElementsPanel(
                        notes = viewModel.videoCanvasNotes,
                        onReorder = { from, to ->
                            viewModel.reorderVideoCanvasNotes(from, to)
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
                        onAnimateKeyframes = { note ->
                            viewModel.videoKeyframeTargetNoteId = note.id
                            viewModel.videoShowKeyframeDialog = true
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

    // ---- Dialogs ----

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
            targetNote.content.startsWith("Shape:") || targetNote.content.startsWith("Image:") -> targetNote.backgroundColor
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

    if (viewModel.videoShowKeyframeDialog && viewModel.videoKeyframeTargetNoteId != null) {
        val targetNote = viewModel.videoCanvasNotes.find { it.id == viewModel.videoKeyframeTargetNoteId }
        KeyframeAnimationDialog(
            note = targetNote,
            onDismiss = {
                viewModel.videoShowKeyframeDialog = false
                viewModel.videoKeyframeTargetNoteId = null
            },
            onSaveKeyframes = { noteId, updatedKeyframes ->
                viewModel.updateVideoNoteKeyframes(noteId, updatedKeyframes)
                viewModel.videoShowKeyframeDialog = false
                viewModel.videoKeyframeTargetNoteId = null
            }
        )
    }
}

// ---- Helper functions for animation ----

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun findSurroundingKeyframes(
    keyframes: List<CanvasKeyframe>,
    currentMs: Long
): Pair<CanvasKeyframe?, CanvasKeyframe?> {
    if (keyframes.isEmpty()) return null to null
    if (currentMs <= keyframes.first().timestampMs) return keyframes.first() to keyframes.first()
    if (currentMs >= keyframes.last().timestampMs) return keyframes.last() to keyframes.last()
    for (i in 0 until keyframes.size - 1) {
        if (currentMs in keyframes[i].timestampMs..keyframes[i + 1].timestampMs) {
            return keyframes[i] to keyframes[i + 1]
        }
    }
    return keyframes.last() to keyframes.last()
}

// ---- KeyframeAnimationDialog (unchanged) ----
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
        title = { Text(text = "Keyframe Editor: ${note.content.take(15)}...") },
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
                    Text("Insert Keyframe")
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