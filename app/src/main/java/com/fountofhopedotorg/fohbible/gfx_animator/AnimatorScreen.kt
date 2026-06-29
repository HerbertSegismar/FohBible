package com.fountofhopedotorg.fohbible.gfx_animator

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.color_wheel.ColorWheelDialog
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.gfx_creator.CustomPolygonDialog
import com.fountofhopedotorg.fohbible.gfx_creator.GroupDialog
import com.fountofhopedotorg.fohbible.gfx_creator.RenameDialog
import com.fountofhopedotorg.fohbible.gfx_creator.getElementDisplayName
import com.fountofhopedotorg.fohbible.gfx_creator.getRandomColor
import com.fountofhopedotorg.fohbible.gfx_creator.getSerializedPointsForShape
import com.fountofhopedotorg.fohbible.models.AnimatorDialogType
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.min
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AnimatorScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val canvasWidthPx = if (isLandscape) 1920 else 1080
    val canvasHeightPx = if (isLandscape) 1080 else 1920
    val density = LocalDensity.current
    val canvasWidthDp = with(density) { canvasWidthPx.toDp() }
    val canvasHeightDp = with(density) { canvasHeightPx.toDp() }

    val viewModel: AppViewModel = viewModel()
    val graphicsLayer = rememberGraphicsLayer()

    var showCanvasElementsTree by rememberSaveable { mutableStateOf(true) }
    var dragGroupDelta by remember { mutableStateOf(Offset.Zero) }

    var isPlayingAnimation by remember { mutableStateOf(false) }
    var animationCurrentTimeUs by remember { mutableLongStateOf(0L) }
    var originalNoteStates by remember { mutableStateOf<Map<String, CanvasNote>>(emptyMap()) }

    var isRecording by remember { mutableStateOf(false) }
    val encoder = remember { mutableStateOf<ComposeVideoEncoder?>(null) }

    var lastCaptureTimeUs by remember { mutableLongStateOf(0L) }
    var hasCapturedFirstFrame by remember { mutableStateOf(false) }

    var exportProgress by remember { mutableFloatStateOf(0f) }
    var recordingMaxTimestamp by remember { mutableLongStateOf(0L) }

    var showMp4SettingsDialog by remember { mutableStateOf(false) }
    var selectedFrameRate by remember { mutableIntStateOf(30) }
    var selectedBitRateMbps by remember { mutableIntStateOf(20) }

    val cancelExport: () -> Unit = remember { { isPlayingAnimation = false } }

    val onPlayPause = remember {
        {
            if (isPlayingAnimation) {
                isPlayingAnimation = false
            } else {
                originalNoteStates = viewModel.animatorCanvasNotes
                    .filter { it.keyframes.isNotEmpty() }
                    .associateBy { it.id }
                    .mapValues { it.value.copy() }
                animationCurrentTimeUs = 0L
                isPlayingAnimation = true
            }
        }
    }

    val onTimelineClick = remember {
        {
            if (isPlayingAnimation) {
                isPlayingAnimation = false
            } else {
                val selectedNote = viewModel.animatorCanvasNotes.firstOrNull { it.id == viewModel.animatorSelectedNoteId }
                if (selectedNote != null) {
                    viewModel.animatorKeyframeTargetNoteId = selectedNote.id
                    viewModel.animatorShowKeyframeDialog = true
                }
            }
        }
    }

    val notesGrouped = remember(viewModel.animatorCanvasNotes) {
        viewModel.animatorCanvasNotes.groupBy { it.groupId }
    }
    val selectedGroups = remember(viewModel.animatorSelectedNoteIds, viewModel.animatorCanvasNotes) {
        viewModel.animatorCanvasNotes
            .filter { it.groupId != null && it.id in viewModel.animatorSelectedNoteIds }
            .map { it.groupId!! }
            .toSet()
    }
    val hasAnyKeyframes by remember {
        derivedStateOf {
            viewModel.animatorCanvasNotes.any { it.keyframes.isNotEmpty() }
        }
    }
    val enablePlayStop = hasAnyKeyframes || isPlayingAnimation

    fun toggleGroupSelection(note: CanvasNote) {
        val groupId = note.groupId
        if (groupId != null) {
            val groupNotes = viewModel.animatorCanvasNotes.filter { it.groupId == groupId }
            val allIds = groupNotes.map { it.id }.toSet()
            val currentlySelected = viewModel.animatorSelectedNoteIds.containsAll(allIds)

            viewModel.animatorSelectedNoteIds = if (currentlySelected) {
                viewModel.animatorSelectedNoteIds - allIds
            } else {
                viewModel.animatorSelectedNoteIds + allIds
            }

            viewModel.animatorSelectedNoteId = if (currentlySelected) {
                null
            } else {
                groupNotes.firstOrNull()?.id
            }
        } else {
            val currentlySelected = viewModel.animatorSelectedNoteIds.contains(note.id)
            viewModel.animatorSelectedNoteIds = if (currentlySelected) {
                viewModel.animatorSelectedNoteIds - note.id
            } else {
                viewModel.animatorSelectedNoteIds + note.id
            }
            viewModel.animatorSelectedNoteId = if (currentlySelected) null else note.id
        }
    }

    fun onCanvasNoteTap(note: CanvasNote) {
        val groupId = note.groupId
        if (groupId != null) {
            val groupNotes = viewModel.animatorCanvasNotes.filter { it.groupId == groupId }
            viewModel.animatorSelectedNoteIds = groupNotes.map { it.id }.toSet()
            viewModel.animatorSelectedNoteId = note.id
        } else {
            viewModel.animatorSelectedNoteIds = setOf(note.id)
            viewModel.animatorSelectedNoteId = note.id
        }
    }

    fun onSingleSelect(note: CanvasNote) {
        viewModel.animatorSelectedNoteId = note.id
        viewModel.animatorSelectedNoteIds = emptySet()
    }

    fun onGroupHeaderTap(groupId: String) {
        val members = viewModel.animatorCanvasNotes.filter { it.groupId == groupId }
        viewModel.animatorSelectedNoteIds = members.map { it.id }.toSet()
        viewModel.animatorSelectedNoteId = members.firstOrNull()?.id
    }

    val onProportionalToggle: () -> Unit = remember {
        { viewModel.proportionalEditing = !viewModel.proportionalEditing }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addToAnimatorCanvas(
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

    val onSaveVideo: () -> Unit = remember {
        {
            if (viewModel.animatorCanvasNotes.isEmpty()) {
                Toast.makeText(context, "Canvas is empty", Toast.LENGTH_SHORT).show()
                return@remember
            }
            showMp4SettingsDialog = true
        }
    }

    LaunchedEffect(isPlayingAnimation) {
        if (!isPlayingAnimation) {
            if (originalNoteStates.isNotEmpty()) {
                val restored = viewModel.animatorCanvasNotes.map { note ->
                    originalNoteStates[note.id] ?: note
                }
                viewModel.animatorCanvasNotes.clear()
                viewModel.animatorCanvasNotes.addAll(restored)
                originalNoteStates = emptyMap()
            }
            encoder.value?.let {
                it.releaseAndDiscard()
                encoder.value = null
            }
            isRecording = false
            return@LaunchedEffect
        }

        val keyframedNotes = viewModel.animatorCanvasNotes.filter { it.keyframes.isNotEmpty() }
        val maxTimestamp = if (keyframedNotes.isNotEmpty()) {
            keyframedNotes.maxOf { note -> note.keyframes.maxOfOrNull { it.timestampMs } ?: 0L }
        } else {
            if (isRecording) 2000L else {
                isPlayingAnimation = false
                return@LaunchedEffect
            }
        }

        if (isRecording) {
            recordingMaxTimestamp = maxTimestamp
            exportProgress = 0f
            while (graphicsLayer.size.width == 0) {
                delay(16.milliseconds)
            }
            val width = graphicsLayer.size.width
            val height = graphicsLayer.size.height
            encoder.value = ComposeVideoEncoder(
                context,
                width,
                height,
                frameRate = selectedFrameRate,
                bitRate = selectedBitRateMbps * 1_000_000
            )
            originalNoteStates = viewModel.animatorCanvasNotes.associateBy { it.id }
            animationCurrentTimeUs = 0L
            lastCaptureTimeUs = 0L
            hasCapturedFirstFrame = false
        }
        val playbackStepUs = if (isRecording) {
            (1_000_000L / selectedFrameRate).coerceAtLeast(4_000L)
        } else {
            8_333L
        }

        while (isActive && isPlayingAnimation) {
            val currentMs = animationCurrentTimeUs / 1000L

            val snapshot = viewModel.animatorCanvasNotes.toList()
            for (i in snapshot.indices) {
                val note = snapshot[i]
                if (note.keyframes.isEmpty()) continue

                val sortedKeyframes = note.keyframes.sortedBy { it.timestampMs }
                val (kfPrev, kfNext) = findSurroundingKeyframes(sortedKeyframes, currentMs)
                val progress = if (kfNext != null && kfPrev != null && kfNext.timestampMs != kfPrev.timestampMs) {
                    ((currentMs - kfPrev.timestampMs).toFloat() /
                            (kfNext.timestampMs - kfPrev.timestampMs)).coerceIn(0f, 1f)
                } else 0f

                val newX = lerp(kfPrev?.x ?: note.offset.x, kfNext?.x ?: note.offset.x, progress)
                val newY = lerp(kfPrev?.y ?: note.offset.y, kfNext?.y ?: note.offset.y, progress)
                val newScaleX = lerp(kfPrev?.scaleX ?: note.scaleX, kfNext?.scaleX ?: note.scaleX, progress)
                val newScaleY = lerp(kfPrev?.scaleY ?: note.scaleY, kfNext?.scaleY ?: note.scaleY, progress)
                val newRotation = lerp(kfPrev?.rotation ?: note.rotation, kfNext?.rotation ?: note.rotation, progress)

                val newColor = if (kfPrev?.color != null && kfNext?.color != null) {
                    lerpColor(kfPrev.color, kfNext.color, progress)
                } else kfNext?.color ?: kfPrev?.color

                viewModel.animatorCanvasNotes[i] = note.copy(
                    offset = Offset(newX, newY),
                    scaleX = newScaleX,
                    scaleY = newScaleY,
                    rotation = newRotation
                )

                if (newColor != null) {
                    val isText = !note.content.startsWith("Shape:") && !note.content.startsWith("Image:")
                    if (isText) {
                        viewModel.updateAnimatorNoteTextColor(note.id, newColor)
                    } else {
                        viewModel.updateAnimatorNoteColor(note.id, newColor)
                    }
                }

                val newGradient = if (kfPrev?.gradientConfig != null && kfNext?.gradientConfig != null) {
                    lerpGradient(kfPrev.gradientConfig, kfNext.gradientConfig, progress)
                } else kfNext?.gradientConfig ?: kfPrev?.gradientConfig
                if (newGradient != null) {
                    viewModel.animatorGradientPairs[note.id] = newGradient
                } else {
                    viewModel.animatorGradientPairs.remove(note.id)
                }
            }

            if (isRecording) {
                androidx.compose.runtime.withFrameNanos { }

                val enc = encoder.value
                if (enc != null) {
                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    enc.addFrame(bitmap, animationCurrentTimeUs)
                }
                exportProgress = if (recordingMaxTimestamp > 0) {
                    (currentMs.toFloat() / recordingMaxTimestamp.toFloat()).coerceIn(0f, 1f)
                } else {
                    (currentMs.toFloat() / 2000f).coerceIn(0f, 1f)
                }
            }

            animationCurrentTimeUs += playbackStepUs

            if (currentMs > maxTimestamp + 500) {
                isPlayingAnimation = false
                break
            }

            if (!isRecording) {
                delay(playbackStepUs.microseconds)
            }
        }

        if (isRecording) {
            exportProgress = 1f
            val enc = encoder.value
            if (enc != null) {
                val fileName = "VideoEditor_${System.currentTimeMillis()}"
                val savedPath = enc.releaseAndSaveToGallery(fileName)
                if (savedPath != null) {
                    Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save video", Toast.LENGTH_SHORT).show()
                }
                encoder.value = null
            }
            isRecording = false
            if (originalNoteStates.isNotEmpty()) {
                viewModel.animatorCanvasNotes.clear()
                viewModel.animatorCanvasNotes.addAll(originalNoteStates.values.toList())
                originalNoteStates = emptyMap()
            }
        }
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
                        .weight(1.7f)
                        .fillMaxHeight()
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val containerWidthDp = maxWidth
                        val containerHeightDp = maxHeight
                        val scale = min(
                            containerWidthDp / canvasWidthDp,
                            containerHeightDp / canvasHeightDp
                        )
                        val offsetXDp = (containerWidthDp - canvasWidthDp * scale) / 2
                        val offsetYDp = (containerHeightDp - canvasHeightDp * scale) / 2

                        Box(
                            modifier = Modifier
                                .requiredSize(canvasWidthDp, canvasHeightDp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetXDp.toPx()
                                    translationY = offsetYDp.toPx()
                                }
                        ) {
                            AnimatorCanvasArea(
                                modifier = Modifier.fillMaxSize(),
                                notes = viewModel.animatorCanvasNotes,
                                selectedNoteIds = viewModel.animatorSelectedNoteIds,
                                selectedNoteId = viewModel.animatorSelectedNoteId,
                                selectedGroups = selectedGroups,
                                dragGroupDelta = dragGroupDelta,
                                onGroupDragDeltaChange = { dragGroupDelta = it },
                                onCanvasNoteTap = { onCanvasNoteTap(it) },
                                onNoteUpdatePosition = { note, offset, w, h, rotation ->
                                    viewModel.updateAnimatorNoteProperties(
                                        id = note.id,
                                        x = offset.x,
                                        y = offset.y,
                                        width = w,
                                        height = h,
                                        rotation = rotation
                                    )
                                },
                                onColorPickerRequested = {
                                    viewModel.animatorNoteToColorEditId = it
                                    viewModel.animatorShowColorPicker = true
                                },
                                onDeleteRequested = {
                                    val idx = viewModel.animatorCanvasNotes.indexOfFirst { note -> note.id == it }
                                    if (idx != -1) viewModel.removeFromAnimatorCanvas(idx)
                                },
                                onClearSelection = {
                                    viewModel.animatorSelectedNoteIds = emptySet()
                                    viewModel.animatorSelectedNoteId = null
                                },
                                themeColors = themeColors,
                                isDark = isDark,
                                notesGrouped = notesGrouped,
                                graphicsLayer = graphicsLayer,
                                onNoteScaleChange = { id, sx, sy -> viewModel.updateAnimatorNoteScale(id, sx, sy) },
                                proportionalEditing = viewModel.proportionalEditing,
                                onProportionalToggle = onProportionalToggle
                            )

                            if (isPlayingAnimation) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .pointerInput(Unit) {},
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (isRecording) "Exporting Video…" else "Playing Animation…",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                        .padding(top = 20.dp)
                ) {
                    AnimatorCanvasElementsPanel(
                        notes = viewModel.animatorCanvasNotes,
                        onReorder = { from, to ->
                            viewModel.reorderAnimatorCanvasNotes(from, to)
                        },
                        selectedNoteIds = viewModel.animatorSelectedNoteIds,
                        selectedNoteId = viewModel.animatorSelectedNoteId,
                        showTree = showCanvasElementsTree,
                        onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
                        onSingleSelect = { onSingleSelect(it) },
                        onToggleGroupSelection = { toggleGroupSelection(it) },
                        onGroupHeaderTap = { onGroupHeaderTap(it) },
                        onEditNote = { note ->
                            viewModel.animatorDialogType = AnimatorDialogType.Edit(note.id, note.content)
                        },
                        onCustomPolygonEdit = { note ->
                            val content = note.content.trim()
                            when {
                                content.startsWith("Shape:CustomPolygon:") -> {
                                    viewModel.animatorPolygonNoteToEditId = note.id
                                    viewModel.animatorInitialPolygonString = content.removePrefix("Shape:CustomPolygon:")
                                    viewModel.animatorInitialIsLineMode = false
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                content.startsWith("Shape:CustomLine:") -> {
                                    viewModel.animatorPolygonNoteToEditId = note.id
                                    viewModel.animatorInitialPolygonString = content.removePrefix("Shape:CustomLine:")
                                    viewModel.animatorInitialIsLineMode = true
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                content.startsWith("Shape: Line") || content == "Shape: Line" -> {
                                    viewModel.animatorPolygonNoteToEditId = note.id
                                    viewModel.animatorInitialPolygonString = getSerializedPointsForShape("Line")
                                    viewModel.animatorInitialIsLineMode = true
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                else -> {
                                    val shapeType = content.removePrefix("Shape:").trim()
                                    val prefilledPoints = getSerializedPointsForShape(shapeType)
                                    if (prefilledPoints.isNotEmpty()) {
                                        viewModel.animatorPolygonNoteToEditId = note.id
                                        viewModel.animatorInitialPolygonString = prefilledPoints
                                        viewModel.animatorInitialIsLineMode = false
                                        viewModel.animatorShowCustomPolygonDialog = true
                                    } else {
                                        viewModel.animatorDialogType = AnimatorDialogType.Edit(note.id, content)
                                    }
                                }
                            }
                        },
                        onRename = { note ->
                            viewModel.animatorNoteToRenameId = note.id
                            viewModel.animatorRenameText = getElementDisplayName(
                                note,
                                viewModel.animatorCanvasNotes.indexOf(note),
                                viewModel.animatorCanvasNotes
                            )
                        },
                        onEditProperties = { note ->
                            viewModel.animatorEditPropertiesNoteId = note.id
                            viewModel.animatorEditX = note.offset.x.toString()
                            viewModel.animatorEditY = note.offset.y.toString()
                            viewModel.animatorEditScaleX = note.scaleX.toString()
                            viewModel.animatorEditScaleY = note.scaleY.toString()
                            viewModel.animatorEditRotation = note.rotation.toString()
                            viewModel.animatorEditColorForDialog = if (!note.content.startsWith("Shape:") && !note.content.startsWith("Image:"))
                                note.textColor ?: Color.Black
                            else
                                note.backgroundColor
                            viewModel.animatorEditShadowColorForDialog = note.shadowColor
                            viewModel.animatorEditShadowOffsetX = note.shadowOffsetX
                            viewModel.animatorEditShadowOffsetY = note.shadowOffsetY
                            viewModel.animatorEditBorderThickness = note.borderThickness
                            viewModel.animatorEditBorderColorForDialog = note.borderColor
                            viewModel.animatorShowEditPropertiesDialog = true
                        },
                        onAnimateKeyframes = { note ->
                            viewModel.animatorKeyframeTargetNoteId = note.id
                            viewModel.animatorShowKeyframeDialog = true
                        },
                        onToggleVisibility = { viewModel.toggleAnimatorVisibility(it) },
                        onToggleLock = { viewModel.toggleAnimatorLock(it) },
                        onDuplicate = { viewModel.addToAnimatorCanvas(CanvasNote(content = it.content)) },
                        onDelete = {
                            val idx = viewModel.animatorCanvasNotes.indexOf(it)
                            if (idx != -1) viewModel.removeFromAnimatorCanvas(idx)
                            if (viewModel.animatorSelectedNoteId == it.id) viewModel.animatorSelectedNoteId = null
                        },
                        onUngroup = { ids ->
                            viewModel.ungroupAnimatorNotes(ids)
                            viewModel.animatorSelectedNoteIds = emptySet()
                        },
                        onGroup = { name, ids ->
                            if (name.isNotBlank() && ids.isNotEmpty()) {
                                viewModel.createAnimatorGroup(ids)
                                viewModel.animatorSelectedNoteIds = emptySet()
                                viewModel.animatorShowGroupDialog = false
                                viewModel.animatorGroupName = ""
                            }
                        },
                        onClearSelection = { viewModel.animatorSelectedNoteIds = emptySet() },
                        themeColors = themeColors,
                        density = LocalDensity.current,
                        groupNames = viewModel.animatorGroupNames,
                        onRenameGroup = { groupId, currentName ->
                            viewModel.animatorGroupToRenameId = groupId
                            viewModel.animatorGroupRenameText = currentName
                        },
                        gradientConfigs = viewModel.animatorGradientPairs,
                    )
                }
            }

            ToolbarSection(
                onAddShape = { shape ->
                    val color = getRandomColor()
                    viewModel.addToAnimatorCanvas(
                        CanvasNote(content = "Shape: $shape", backgroundColor = color, width = 100f, height = 100f)
                    )
                },
                onCustomPolygon = {
                    viewModel.animatorPolygonNoteToEditId = null
                    viewModel.animatorInitialPolygonString = ""
                    viewModel.animatorInitialIsLineMode = false
                    viewModel.animatorShowCustomPolygonDialog = true
                },
                selectedInputMode = viewModel.animatorSelectedInputMode,
                onModeSelected = { mode ->
                    when (mode) {
                        "Add SVG" -> viewModel.animatorSelectedInputMode = "Add SVG"
                        "Add Text" -> {
                            viewModel.animatorSelectedInputMode = "Add Text"
                            viewModel.animatorDialogType = AnimatorDialogType.AddText
                        }
                        "Fetch Verse" -> {
                            viewModel.animatorSelectedInputMode = "Fetch Verse"
                            viewModel.animatorDialogType = AnimatorDialogType.FetchVerse
                        }
                        else -> viewModel.animatorSelectedInputMode = "Add SVG"
                    }
                },
                themeColors = themeColors,
                isFullScreen = viewModel.isAnimatorFullScreen,
                onToggleFullScreen = {
                    viewModel.isAnimatorFullScreen = !viewModel.isAnimatorFullScreen
                },
                onChooseFromGallery = { imagePickerLauncher.launch("image/*") },
                graphicsLayer = graphicsLayer,
                isLandscape = true,
                onSaveVideo = onSaveVideo,
                isPlayingAnimation = isPlayingAnimation,
                onPlayPause = onPlayPause,
                onTimelineClick = onTimelineClick,
                enablePlayStop = enablePlayStop
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {

            Column(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
            ) {
                ToolbarSection(
                    onAddShape = { shape ->
                        val color = getRandomColor()
                        viewModel.addToAnimatorCanvas(
                            CanvasNote(content = "Shape: $shape", backgroundColor = color, width = 100f, height = 100f)
                        )
                    },
                    onCustomPolygon = {
                        viewModel.animatorPolygonNoteToEditId = null
                        viewModel.animatorInitialPolygonString = ""
                        viewModel.animatorInitialIsLineMode = false
                        viewModel.animatorShowCustomPolygonDialog = true
                    },
                    selectedInputMode = viewModel.animatorSelectedInputMode,
                    onModeSelected = { mode ->
                        when (mode) {
                            "Add SVG" -> viewModel.animatorSelectedInputMode = "Add SVG"
                            "Add Text" -> {
                                viewModel.animatorSelectedInputMode = "Add Text"
                                viewModel.animatorDialogType = AnimatorDialogType.AddText
                            }
                            "Fetch Verse" -> {
                                viewModel.animatorSelectedInputMode = "Fetch Verse"
                                viewModel.animatorDialogType = AnimatorDialogType.FetchVerse
                            }
                            else -> viewModel.animatorSelectedInputMode = "Add SVG"
                        }
                    },
                    themeColors = themeColors,
                    isFullScreen = viewModel.isAnimatorFullScreen,
                    onToggleFullScreen = {
                        viewModel.isAnimatorFullScreen = !viewModel.isAnimatorFullScreen
                    },
                    onChooseFromGallery = { imagePickerLauncher.launch("image/*") },
                    graphicsLayer = graphicsLayer,
                    isLandscape = false,
                    onSaveVideo = onSaveVideo,
                    isPlayingAnimation = isPlayingAnimation,
                    onPlayPause = onPlayPause,
                    onTimelineClick = onTimelineClick,
                    enablePlayStop = enablePlayStop
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.7f)
                ) {
                    val containerWidthDp = maxWidth
                    val containerHeightDp = maxHeight
                    val scale = min(
                        containerWidthDp / canvasWidthDp,
                        containerHeightDp / canvasHeightDp
                    )
                    val offsetXDp = (containerWidthDp - canvasWidthDp * scale) / 2
                    val offsetYDp = (containerHeightDp - canvasHeightDp * scale) / 2

                    Box(
                        modifier = Modifier
                            .requiredSize(canvasWidthDp, canvasHeightDp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetXDp.toPx()
                                translationY = offsetYDp.toPx()
                            }
                    ) {
                        AnimatorCanvasArea(
                            modifier = Modifier.fillMaxSize(),
                            notes = viewModel.animatorCanvasNotes,
                            selectedNoteIds = viewModel.animatorSelectedNoteIds,
                            selectedNoteId = viewModel.animatorSelectedNoteId,
                            selectedGroups = selectedGroups,
                            dragGroupDelta = dragGroupDelta,
                            onGroupDragDeltaChange = { dragGroupDelta = it },
                            onCanvasNoteTap = { onCanvasNoteTap(it) },
                            onNoteUpdatePosition = { note, offset, w, h, rotation ->
                                viewModel.updateAnimatorNoteProperties(
                                    id = note.id,
                                    x = offset.x,
                                    y = offset.y,
                                    width = w,
                                    height = h,
                                    rotation = rotation
                                )
                            },
                            onNoteScaleChange = { id, sx, sy -> viewModel.updateAnimatorNoteScale(id, sx, sy) },
                            onColorPickerRequested = {
                                viewModel.animatorNoteToColorEditId = it
                                viewModel.animatorShowColorPicker = true
                            },
                            onDeleteRequested = {
                                val idx = viewModel.animatorCanvasNotes.indexOfFirst { note -> note.id == it }
                                if (idx != -1) viewModel.removeFromAnimatorCanvas(idx)
                            },
                            onClearSelection = {
                                viewModel.animatorSelectedNoteIds = emptySet()
                                viewModel.animatorSelectedNoteId = null
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
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .pointerInput(Unit) {},
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isRecording) "Exporting Video…" else "Playing Animation…",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxWidth()
                        .verticalScroll(mainScrollState)
                ) {
                    AnimatorCanvasElementsPanel(
                        notes = viewModel.animatorCanvasNotes,
                        onReorder = { from, to ->
                            viewModel.reorderAnimatorCanvasNotes(from, to)
                        },
                        selectedNoteIds = viewModel.animatorSelectedNoteIds,
                        selectedNoteId = viewModel.animatorSelectedNoteId,
                        showTree = showCanvasElementsTree,
                        onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
                        onSingleSelect = { onSingleSelect(it) },
                        onToggleGroupSelection = { toggleGroupSelection(it) },
                        onGroupHeaderTap = { onGroupHeaderTap(it) },
                        onEditNote = { note ->
                            viewModel.animatorDialogType = AnimatorDialogType.Edit(note.id, note.content)
                        },
                        onCustomPolygonEdit = { note ->
                            val content = note.content.trim()
                            when {
                                content.startsWith("Shape:CustomPolygon:") -> {
                                    viewModel.animatorPolygonNoteToEditId = note.id
                                    viewModel.animatorInitialPolygonString = content.removePrefix("Shape:CustomPolygon:")
                                    viewModel.animatorInitialIsLineMode = false
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                content.startsWith("Shape:CustomLine:") -> {
                                    viewModel.animatorPolygonNoteToEditId = note.id
                                    viewModel.animatorInitialPolygonString = content.removePrefix("Shape:CustomLine:")
                                    viewModel.animatorInitialIsLineMode = true
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                content.startsWith("Shape: Line") || content == "Shape: Line" -> {
                                    viewModel.animatorPolygonNoteToEditId = note.id
                                    viewModel.animatorInitialPolygonString = getSerializedPointsForShape("Line")
                                    viewModel.animatorInitialIsLineMode = true
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                else -> {
                                    val shapeType = content.removePrefix("Shape:").trim()
                                    val prefilledPoints = getSerializedPointsForShape(shapeType)
                                    if (prefilledPoints.isNotEmpty()) {
                                        viewModel.animatorPolygonNoteToEditId = note.id
                                        viewModel.animatorInitialPolygonString = prefilledPoints
                                        viewModel.animatorInitialIsLineMode = false
                                        viewModel.animatorShowCustomPolygonDialog = true
                                    } else {
                                        viewModel.animatorDialogType = AnimatorDialogType.Edit(note.id, content)
                                    }
                                }
                            }
                        },
                        onRename = { note ->
                            viewModel.animatorNoteToRenameId = note.id
                            viewModel.animatorRenameText = getElementDisplayName(
                                note,
                                viewModel.animatorCanvasNotes.indexOf(note),
                                viewModel.animatorCanvasNotes
                            )
                        },
                        onEditProperties = { note ->
                            viewModel.animatorEditPropertiesNoteId = note.id
                            viewModel.animatorEditX = note.offset.x.toString()
                            viewModel.animatorEditY = note.offset.y.toString()
                            viewModel.animatorEditScaleX = note.scaleX.toString()
                            viewModel.animatorEditScaleY = note.scaleY.toString()
                            viewModel.animatorEditRotation = note.rotation.toString()
                            viewModel.animatorEditColorForDialog = if (!note.content.startsWith("Shape:") && !note.content.startsWith("Image:"))
                                note.textColor ?: Color.Black
                            else
                                note.backgroundColor
                            viewModel.animatorEditShadowColorForDialog = note.shadowColor
                            viewModel.animatorEditShadowOffsetX = note.shadowOffsetX
                            viewModel.animatorEditShadowOffsetY = note.shadowOffsetY
                            viewModel.animatorEditBorderThickness = note.borderThickness
                            viewModel.animatorEditBorderColorForDialog = note.borderColor
                            viewModel.animatorShowEditPropertiesDialog = true
                        },
                        onAnimateKeyframes = { note ->
                            viewModel.animatorKeyframeTargetNoteId = note.id
                            viewModel.animatorShowKeyframeDialog = true
                        },
                        onToggleVisibility = { viewModel.toggleAnimatorVisibility(it) },
                        onToggleLock = { viewModel.toggleAnimatorLock(it) },
                        onDuplicate = { viewModel.addToAnimatorCanvas(CanvasNote(content = it.content)) },
                        onDelete = {
                            val idx = viewModel.animatorCanvasNotes.indexOf(it)
                            if (idx != -1) viewModel.removeFromAnimatorCanvas(idx)
                            if (viewModel.animatorSelectedNoteId == it.id) viewModel.animatorSelectedNoteId = null
                        },
                        onUngroup = { ids ->
                            viewModel.ungroupAnimatorNotes(ids)
                            viewModel.animatorSelectedNoteIds = emptySet()
                        },
                        onGroup = { name, ids ->
                            if (name.isNotBlank() && ids.isNotEmpty()) {
                                viewModel.createAnimatorGroup(ids)
                                viewModel.animatorSelectedNoteIds = emptySet()
                                viewModel.animatorShowGroupDialog = false
                                viewModel.animatorGroupName = ""
                            }
                        },
                        onClearSelection = { viewModel.animatorSelectedNoteIds = emptySet() },
                        themeColors = themeColors,
                        density = LocalDensity.current,
                        groupNames = viewModel.animatorGroupNames,
                        onRenameGroup = { groupId, currentName ->
                            viewModel.animatorGroupToRenameId = groupId
                            viewModel.animatorGroupRenameText = currentName
                        },
                        gradientConfigs = viewModel.animatorGradientPairs,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    when (val dialog = viewModel.animatorDialogType) {
        is AnimatorDialogType.Edit -> {
            AnimatorEditNoteDialog(
                noteId = dialog.noteId,
                initialContent = dialog.initialContent,
                onDismiss = { viewModel.animatorDialogType = null },
                onSave = { id, newContent ->
                    val index = viewModel.animatorCanvasNotes.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val old = viewModel.animatorCanvasNotes[index]
                        val updated = old.copy(content = newContent)
                        viewModel.removeFromAnimatorCanvas(index)
                        viewModel.addToAnimatorCanvas(updated)
                    }
                    viewModel.animatorDialogType = null
                }
            )
        }
        AnimatorDialogType.AddText -> {
            AnimatorEditNoteDialog(
                noteId = null,
                initialContent = "",
                isNew = true,
                onDismiss = { viewModel.animatorDialogType = null },
                onSave = { _, newContent ->
                    if (newContent.isNotBlank()) {
                        viewModel.addToAnimatorCanvas(
                            CanvasNote(
                                content = newContent,
                                textColor = getRandomColor()
                            )
                        )
                    }
                    viewModel.animatorDialogType = null
                }
            )
        }
        AnimatorDialogType.FetchVerse -> {
            AnimatorEditNoteDialog(
                noteId = null,
                initialContent = "",
                isNew = true,
                fetchMode = true,
                dbHelper = dbHelper,
                viewModel = viewModel,
                verseProcessor = verseProcessor,
                themeColors = themeColors,
                onDismiss = { viewModel.animatorDialogType = null },
                onSave = { _, newContent ->
                    if (newContent.isNotBlank()) {
                        viewModel.addToAnimatorCanvas(
                            CanvasNote(
                                content = newContent,
                                textColor = getRandomColor(),
                            )
                        )
                    }
                    viewModel.animatorDialogType = null
                }
            )
        }
        null -> {}
    }

    RenameDialog(
        noteId = viewModel.animatorNoteToRenameId,
        currentName = viewModel.animatorRenameText,
        onDismiss = {
            viewModel.animatorNoteToRenameId = null
            viewModel.animatorRenameText = ""
        },
        onConfirm = { id, newName ->
            if (newName.isNotBlank()) {
                viewModel.renameAnimatorCanvasNote(id, newName)
            }
            viewModel.animatorNoteToRenameId = null
            viewModel.animatorRenameText = ""
        }
    )

    if (viewModel.animatorGroupToRenameId != null) {
        RenameDialog(
            noteId = viewModel.animatorGroupToRenameId,
            currentName = viewModel.animatorGroupRenameText,
            title = "Rename Group",
            onDismiss = {
                viewModel.animatorGroupToRenameId = null
                viewModel.animatorGroupRenameText = ""
            },
            onConfirm = { id, newName ->
                if (newName.isNotBlank()) {
                    viewModel.renameAnimatorGroup(id, newName)
                }
                viewModel.animatorGroupToRenameId = null
                viewModel.animatorGroupRenameText = ""
            }
        )
    }

    GroupDialog(
        show = viewModel.animatorShowGroupDialog,
        initialName = viewModel.animatorGroupName,
        onDismiss = { viewModel.animatorShowGroupDialog = false },
        onConfirm = { name ->
            if (name.isNotBlank() && viewModel.animatorSelectedNoteIds.isNotEmpty()) {
                viewModel.createAnimatorGroup(viewModel.animatorSelectedNoteIds.toList())
                viewModel.animatorSelectedNoteIds = emptySet()
                viewModel.animatorShowGroupDialog = false
                viewModel.animatorGroupName = ""
            }
        }
    )

    if (viewModel.animatorShowColorPicker && viewModel.animatorNoteToColorEditId != null) {
        val targetNote = viewModel.animatorCanvasNotes.find { it.id == viewModel.animatorNoteToColorEditId }

        val isShape = targetNote?.content?.startsWith("Shape:") == true
        val existingGradient = viewModel.animatorGradientPairs[viewModel.animatorNoteToColorEditId]

        ColorWheelDialog(
            onDismissRequest = {
                viewModel.animatorShowColorPicker = false
                viewModel.animatorNoteToColorEditId = null
            },
            onColorSelected = { color ->
                val noteId = viewModel.animatorNoteToColorEditId!!
                viewModel.animatorGradientPairs.remove(noteId)
                if (isShape) {
                    viewModel.updateAnimatorNoteColor(noteId, color)
                } else {
                    viewModel.updateAnimatorNoteTextColor(noteId, color)
                }
                viewModel.animatorShowColorPicker = false
                viewModel.animatorNoteToColorEditId = null
            },
            initialColor = targetNote?.backgroundColor ?: Color.White,
            enableGradient = isShape,
            onGradientSelected = if (isShape) {
                { startColor, endColor, startOffset, endOffset ->
                    val noteId = viewModel.animatorNoteToColorEditId!!
                    viewModel.animatorGradientPairs[noteId] = GradientConfig(
                        startColor = startColor,
                        endColor = endColor,
                        startOffset = startOffset,
                        endOffset = endOffset
                    )
                    viewModel.updateAnimatorNoteColor(noteId, startColor)
                    viewModel.animatorShowColorPicker = false
                    viewModel.animatorNoteToColorEditId = null
                }
            } else null,
            initialGradientConfig = existingGradient
        )
    }

    if (viewModel.animatorShowCustomPolygonDialog) {
        CustomPolygonDialog(
            initialSerializedPoints = viewModel.animatorInitialPolygonString.takeIf { it.isNotEmpty() },
            isLineMode = viewModel.animatorInitialIsLineMode,
            onDismiss = {
                viewModel.animatorShowCustomPolygonDialog = false
                viewModel.animatorPolygonNoteToEditId = null
                viewModel.animatorInitialPolygonString = ""
                viewModel.animatorInitialIsLineMode = false
            },
            onConfirm = { points, isLine ->
                val serialized = points.joinToString(";") { node ->
                    "${node.anchor.x},${node.anchor.y}:${node.handleIn.x},${node.handleIn.y}:${node.handleOut.x},${node.handleOut.y}"
                }
                val shapeType = if (isLine) "CustomLine" else "CustomPolygon"
                val contentString = "Shape:$shapeType:$serialized"
                if (viewModel.animatorPolygonNoteToEditId != null) {
                    viewModel.updateAnimatorNoteContent(viewModel.animatorPolygonNoteToEditId!!, contentString)
                    viewModel.animatorSelectedNoteId = viewModel.animatorPolygonNoteToEditId
                } else {
                    viewModel.addToAnimatorCanvas(
                        CanvasNote(
                            content = contentString,
                            backgroundColor = getRandomColor(),
                            width = 100f,
                            height = 100f
                        )
                    )
                }
                viewModel.animatorShowCustomPolygonDialog = false
                viewModel.animatorPolygonNoteToEditId = null
                viewModel.animatorInitialPolygonString = ""
                viewModel.animatorInitialIsLineMode = false
            }
        )
    }

    if (viewModel.animatorShowKeyframeDialog && viewModel.animatorKeyframeTargetNoteId != null) {
        val targetNote = viewModel.animatorCanvasNotes.find { it.id == viewModel.animatorKeyframeTargetNoteId }
        val noteGradient = viewModel.animatorGradientPairs[viewModel.animatorKeyframeTargetNoteId]
        KeyframeAnimationDialog(
            note = targetNote,
            onDismiss = {
                viewModel.animatorShowKeyframeDialog = false
                viewModel.animatorKeyframeTargetNoteId = null
            },
            onSaveKeyframes = { noteId, updatedKeyframes ->
                viewModel.updateAnimatorNoteKeyframes(noteId, updatedKeyframes)
                viewModel.animatorShowKeyframeDialog = false
                viewModel.animatorKeyframeTargetNoteId = null
            },
            timeMultiplier = 1f,
            initialGradientConfig = noteGradient
        )
    }

    val existingGradient = viewModel.animatorGradientPairs[viewModel.animatorEditPropertiesNoteId]

    AnimatorEditPropertiesDialog (
        show = viewModel.animatorShowEditPropertiesDialog,
        noteId = viewModel.animatorEditPropertiesNoteId,
        initialX = viewModel.animatorEditX,
        initialY = viewModel.animatorEditY,
        initialScaleX = viewModel.animatorEditScaleX,
        initialScaleY = viewModel.animatorEditScaleY,
        initialRotation = viewModel.animatorEditRotation,
        initialColor = viewModel.animatorEditColorForDialog,
        proportionalEnabled = viewModel.proportionalEditing,
        onProportionalToggle = { viewModel.proportionalEditing = it },
        initialShadowColor = viewModel.animatorEditShadowColorForDialog,
        initialShadowOffsetX = viewModel.animatorEditShadowOffsetX,
        initialShadowOffsetY = viewModel.animatorEditShadowOffsetY,
        initialBorderThickness = viewModel.animatorEditBorderThickness,
        initialBorderColor = viewModel.animatorEditBorderColorForDialog,
        initialGradientConfig = existingGradient,
        onDismiss = {
            viewModel.animatorShowEditPropertiesDialog = false
            viewModel.animatorEditPropertiesNoteId = null
        },
        onApply = { id, x, y, scaleX, scaleY, rot, color,
                    shadowColor, shadowOffsetX, shadowOffsetY,
                    borderThickness, borderColor,
                    gradientConfig ->
            val currentNote = viewModel.animatorCanvasNotes.find { it.id == id }
            if (currentNote != null) {
                val isText = !currentNote.content.startsWith("Shape:") && !currentNote.content.startsWith("Image:")
                viewModel.applyAllAnimatorNoteProperties(
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
                if (gradientConfig != null) {
                    viewModel.animatorGradientPairs[id] = gradientConfig
                    viewModel.updateAnimatorNoteColor(id, gradientConfig.startColor)
                } else {
                    viewModel.animatorGradientPairs.remove(id)
                }
            }
            viewModel.animatorShowEditPropertiesDialog = false
            viewModel.animatorEditPropertiesNoteId = null
        }
    )

    if (showMp4SettingsDialog) {
        Mp4ExportSettingsDialog(
            initialFrameRate = selectedFrameRate,
            initialBitRateMbps = selectedBitRateMbps,
            onDismiss = { showMp4SettingsDialog = false },
            onConfirm = { chosenFrameRate: Int, chosenBitRateMbps: Int ->
                selectedFrameRate = chosenFrameRate
                selectedBitRateMbps = chosenBitRateMbps
                showMp4SettingsDialog = false
                isPlayingAnimation = false
                isRecording = true
                encoder.value = null
                exportProgress = 0f
                recordingMaxTimestamp = 0L
                lastCaptureTimeUs = 0L
                hasCapturedFirstFrame = false
                isPlayingAnimation = true
            }
        )
    }

    if (isRecording) {
        ExportDialog(
            progress = exportProgress,
            onCancelRequested = cancelExport
        )
    }
}