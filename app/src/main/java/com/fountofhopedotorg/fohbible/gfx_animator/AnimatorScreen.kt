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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
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
import com.fountofhopedotorg.fohbible.data.CanvasElement
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AnimatorScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current

    val viewModel: AppViewModel = viewModel()
    val graphicsLayer = rememberGraphicsLayer()

    var customWidthPx by rememberSaveable { mutableIntStateOf(if (isLandscape) 1920 else 1080) }
    var customHeightPx by rememberSaveable { mutableIntStateOf(if (isLandscape) 1080 else 1920) }
    var showCanvasSizeDialog by remember { mutableStateOf(false) }
    val canvasWidthPx = customWidthPx
    val canvasHeightPx = customHeightPx
    val canvasWidthDp = with(density) { canvasWidthPx.toDp() }
    val canvasHeightDp = with(density) { canvasHeightPx.toDp() }

    var showCanvasElementsTree by rememberSaveable { mutableStateOf(true) }
    var dragGroupDelta by remember { mutableStateOf(Offset.Zero) }

    var isPlayingAnimation by remember { mutableStateOf(false) }
    var animationCurrentTimeUs by remember { mutableLongStateOf(0L) }
    var originalElementStates by remember { mutableStateOf<Map<String, CanvasElement>>(emptyMap()) }

    var isRecording by remember { mutableStateOf(false) }
    val encoder = remember { mutableStateOf<ComposeVideoEncoder?>(null) }

    var lastCaptureTimeUs by remember { mutableLongStateOf(0L) }
    var hasCapturedFirstFrame by remember { mutableStateOf(false) }

    var exportProgress by remember { mutableFloatStateOf(0f) }
    var recordingMaxTimestamp by remember { mutableLongStateOf(0L) }

    var showMp4SettingsDialog by remember { mutableStateOf(false) }
    var selectedFrameRate by remember { mutableIntStateOf(30) }
    var selectedBitRateMbps by remember { mutableIntStateOf(20) }

    var exportMode by remember { mutableStateOf("Screen") }
    var offscreenOutputMode by remember { mutableStateOf("Video") }
    var offscreenResolutionMultiplier by remember { mutableFloatStateOf(1f) }
    var isOffscreenExporting by remember { mutableStateOf(false) }
    var offscreenExportProgress by remember { mutableFloatStateOf(0f) }
    var offscreenExportJob by remember { mutableStateOf<Job?>(null) }
    val exportScope = rememberCoroutineScope()

    var currentTimeMs by remember { mutableLongStateOf(0L) }

    val cancelExport: () -> Unit = remember { { isPlayingAnimation = false } }

    val onPlayPause = remember {
        {
            if (isPlayingAnimation) {
                isPlayingAnimation = false
            } else {
                originalElementStates = viewModel.animatorCanvasElements
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
                val selectedElement = viewModel.animatorCanvasElements.firstOrNull { it.id == viewModel.animatorSelectedElementId }
                if (selectedElement != null) {
                    viewModel.animatorKeyframeTargetElementId = selectedElement.id
                    viewModel.animatorShowKeyframeDialog = true
                }
            }
        }
    }

    val elementsGrouped = remember(viewModel.animatorCanvasElements) {
        viewModel.animatorCanvasElements.groupBy { it.groupId }
    }
    val selectedGroups = remember(viewModel.animatorSelectedElementIds, viewModel.animatorCanvasElements) {
        viewModel.animatorCanvasElements
            .filter { it.groupId != null && it.id in viewModel.animatorSelectedElementIds }
            .map { it.groupId!! }
            .toSet()
    }
    val hasAnyKeyframes by remember {
        derivedStateOf {
            viewModel.animatorCanvasElements.any { it.keyframes.isNotEmpty() }
        }
    }
    val enablePlayStop = hasAnyKeyframes || isPlayingAnimation

    fun toggleGroupSelection(element: CanvasElement) {
        val groupId = element.groupId
        if (groupId != null) {
            val groupElements = viewModel.animatorCanvasElements.filter { it.groupId == groupId }
            val allIds = groupElements.map { it.id }.toSet()
            val currentlySelected = viewModel.animatorSelectedElementIds.containsAll(allIds)

            viewModel.animatorSelectedElementIds = if (currentlySelected) {
                viewModel.animatorSelectedElementIds - allIds
            } else {
                viewModel.animatorSelectedElementIds + allIds
            }

            viewModel.animatorSelectedElementId = if (currentlySelected) {
                null
            } else {
                groupElements.firstOrNull()?.id
            }
        } else {
            val currentlySelected = viewModel.animatorSelectedElementIds.contains(element.id)
            viewModel.animatorSelectedElementIds = if (currentlySelected) {
                viewModel.animatorSelectedElementIds - element.id
            } else {
                viewModel.animatorSelectedElementIds + element.id
            }
            viewModel.animatorSelectedElementId = if (currentlySelected) null else element.id
        }
    }

    fun onCanvasElementTap(element: CanvasElement) {
        val groupId = element.groupId
        if (groupId != null) {
            val groupElements = viewModel.animatorCanvasElements.filter { it.groupId == groupId }
            viewModel.animatorSelectedElementIds = groupElements.map { it.id }.toSet()
            viewModel.animatorSelectedElementId = element.id
        } else {
            viewModel.animatorSelectedElementIds = setOf(element.id)
            viewModel.animatorSelectedElementId = element.id
        }
    }

    fun onSingleSelect(element: CanvasElement) {
        viewModel.animatorSelectedElementId = element.id
        viewModel.animatorSelectedElementIds = emptySet()
    }

    fun onGroupHeaderTap(groupId: String) {
        val members = viewModel.animatorCanvasElements.filter { it.groupId == groupId }
        viewModel.animatorSelectedElementIds = members.map { it.id }.toSet()
        viewModel.animatorSelectedElementId = members.firstOrNull()?.id
    }

    val onProportionalToggle: () -> Unit = remember {
        { viewModel.proportionalEditing = !viewModel.proportionalEditing }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addToAnimatorCanvas(
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

    val onSaveVideo: () -> Unit = remember {
        {
            if (viewModel.animatorCanvasElements.isEmpty()) {
                Toast.makeText(context, "Canvas is empty", Toast.LENGTH_SHORT).show()
                return@remember
            }
            showMp4SettingsDialog = true
        }
    }

    // ---- Main animation loop and time synchronization ----
    LaunchedEffect(isPlayingAnimation) {
        if (!isPlayingAnimation) {
            // Reset timed canvas so all elements reappear
            currentTimeMs = 0L
            if (originalElementStates.isNotEmpty()) {
                val restored = viewModel.animatorCanvasElements.map { element ->
                    originalElementStates[element.id] ?: element
                }
                viewModel.animatorCanvasElements.clear()
                viewModel.animatorCanvasElements.addAll(restored)
                originalElementStates = emptyMap()
            }
            encoder.value?.let {
                it.releaseAndDiscard()
                encoder.value = null
            }
            isRecording = false
            return@LaunchedEffect
        }

        val keyframedElements = viewModel.animatorCanvasElements.filter { it.keyframes.isNotEmpty() }
        val maxTimestamp = if (keyframedElements.isNotEmpty()) {
            keyframedElements.maxOf { element -> element.keyframes.maxOfOrNull { it.timestampMs } ?: 0L }
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
            originalElementStates = viewModel.animatorCanvasElements.associateBy { it.id }
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
            // Update the timed canvas state so elements appear/disappear
            currentTimeMs = currentMs

            val snapshot = viewModel.animatorCanvasElements.toList()
            for (i in snapshot.indices) {
                val element = snapshot[i]
                if (element.keyframes.isEmpty()) continue

                val sortedKeyframes = element.keyframes.sortedBy { it.timestampMs }
                val (kfPrev, kfNext) = findSurroundingKeyframes(sortedKeyframes, currentMs)
                val progress = if (kfNext != null && kfPrev != null && kfNext.timestampMs != kfPrev.timestampMs) {
                    ((currentMs - kfPrev.timestampMs).toFloat() /
                            (kfNext.timestampMs - kfPrev.timestampMs)).coerceIn(0f, 1f)
                } else 0f

                val newX = lerp(kfPrev?.x ?: element.offset.x, kfNext?.x ?: element.offset.x, progress)
                val newY = lerp(kfPrev?.y ?: element.offset.y, kfNext?.y ?: element.offset.y, progress)
                val newScaleX = lerp(kfPrev?.scaleX ?: element.scaleX, kfNext?.scaleX ?: element.scaleX, progress)
                val newScaleY = lerp(kfPrev?.scaleY ?: element.scaleY, kfNext?.scaleY ?: element.scaleY, progress)
                val newRotation = lerp(kfPrev?.rotation ?: element.rotation, kfNext?.rotation ?: element.rotation, progress)

                val newColor = if (kfPrev?.color != null && kfNext?.color != null) {
                    lerpColor(kfPrev.color, kfNext.color, progress)
                } else kfNext?.color ?: kfPrev?.color

                viewModel.animatorCanvasElements[i] = element.copy(
                    offset = Offset(newX, newY),
                    scaleX = newScaleX,
                    scaleY = newScaleY,
                    rotation = newRotation
                )

                if (newColor != null) {
                    val isText = !element.content.startsWith("Shape:") && !element.content.startsWith("Image:")
                    if (isText) {
                        viewModel.updateAnimatorElementTextColor(element.id, newColor)
                    } else {
                        viewModel.updateAnimatorElementColor(element.id, newColor)
                    }
                }

                val newGradient = if (kfPrev?.gradientConfig != null && kfNext?.gradientConfig != null) {
                    lerpGradient(kfPrev.gradientConfig, kfNext.gradientConfig, progress)
                } else kfNext?.gradientConfig ?: kfPrev?.gradientConfig
                if (newGradient != null) {
                    viewModel.animatorGradientPairs[element.id] = newGradient
                } else {
                    viewModel.animatorGradientPairs.remove(element.id)
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
                val fileName = "ScreenRender_${System.currentTimeMillis()}"
                val savedPath = enc.releaseAndSaveToGallery(fileName)
                if (savedPath != null) {
                    Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save video", Toast.LENGTH_SHORT).show()
                }
                encoder.value = null
            }
            isRecording = false
            if (originalElementStates.isNotEmpty()) {
                viewModel.animatorCanvasElements.clear()
                viewModel.animatorCanvasElements.addAll(originalElementStates.values.toList())
                originalElementStates = emptyMap()
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CheckerboardBackground(
                                modifier = Modifier.fillMaxSize(),
                                tileSizeDp = 8.dp,
                                color1 = Color(0xFFCCCCCC),
                                color2 = Color(0xFF999999)
                            )
                            Box(
                                modifier = Modifier
                                    .requiredSize(canvasWidthDp, canvasHeightDp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    }
                                    .background(Color.White)
                            ) {
                                AnimatorCanvasArea(
                                    modifier = Modifier.fillMaxSize(),
                                    elements = viewModel.animatorCanvasElements,
                                    selectedElementIds = viewModel.animatorSelectedElementIds,
                                    selectedElementId = viewModel.animatorSelectedElementId,
                                    selectedGroups = selectedGroups,
                                    dragGroupDelta = dragGroupDelta,
                                    onGroupDragDeltaChange = { dragGroupDelta = it },
                                    onCanvasElementTap = { onCanvasElementTap(it) },
                                    onElementUpdatePosition = { element, offset, w, h, rotation ->
                                        viewModel.updateAnimatorElementProperties(
                                            id = element.id,
                                            x = offset.x,
                                            y = offset.y,
                                            width = w,
                                            height = h,
                                            rotation = rotation
                                        )
                                    },
                                    onColorPickerRequested = {
                                        viewModel.animatorElementToColorEditId = it
                                        viewModel.animatorShowColorPicker = true
                                    },
                                    onDeleteRequested = {
                                        val idx = viewModel.animatorCanvasElements.indexOfFirst { element -> element.id == it }
                                        if (idx != -1) viewModel.removeFromAnimatorCanvas(idx)
                                        viewModel.animatorSelectedElementIds -= it
                                        if (viewModel.animatorSelectedElementId == it) {
                                            viewModel.animatorSelectedElementId = null
                                        }
                                    },
                                    onClearSelection = {
                                        viewModel.animatorSelectedElementIds = emptySet()
                                        viewModel.animatorSelectedElementId = null
                                    },
                                    themeColors = themeColors,
                                    isDark = isDark,
                                    elementsGrouped = elementsGrouped,
                                    graphicsLayer = graphicsLayer,
                                    onElementScaleChange = { id, sx, sy -> viewModel.updateAnimatorElementScale(id, sx, sy) },
                                    proportionalEditing = viewModel.proportionalEditing,
                                    onProportionalToggle = onProportionalToggle,
                                    currentTimeMs = currentTimeMs   // <-- timed canvas
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
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                        .padding(top = 20.dp)
                ) {
                    AnimatorCanvasElementsPanel(
                        elements = viewModel.animatorCanvasElements,
                        onReorder = { from, to ->
                            viewModel.reorderAnimatorCanvasElements(from, to)
                        },
                        selectedElementIds = viewModel.animatorSelectedElementIds,
                        selectedElementId = viewModel.animatorSelectedElementId,
                        showTree = showCanvasElementsTree,
                        onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
                        onSingleSelect = { onSingleSelect(it) },
                        onToggleGroupSelection = { toggleGroupSelection(it) },
                        onGroupHeaderTap = { onGroupHeaderTap(it) },
                        onEditElement = { element ->
                            viewModel.animatorDialogType = AnimatorDialogType.Edit(element.id, element.content)
                        },
                        onCustomPolygonEdit = { element ->
                            val content = element.content.trim()
                            when {
                                content.startsWith("Shape:CustomPolygon:") -> {
                                    viewModel.animatorPolygonElementToEditId = element.id
                                    viewModel.animatorInitialPolygonString = content.removePrefix("Shape:CustomPolygon:")
                                    viewModel.animatorInitialIsLineMode = false
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                content.startsWith("Shape:CustomLine:") -> {
                                    viewModel.animatorPolygonElementToEditId = element.id
                                    viewModel.animatorInitialPolygonString = content.removePrefix("Shape:CustomLine:")
                                    viewModel.animatorInitialIsLineMode = true
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                content.startsWith("Shape: Line") || content == "Shape: Line" -> {
                                    viewModel.animatorPolygonElementToEditId = element.id
                                    viewModel.animatorInitialPolygonString = getSerializedPointsForShape("Line")
                                    viewModel.animatorInitialIsLineMode = true
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                else -> {
                                    val shapeType = content.removePrefix("Shape:").trim()
                                    val prefilledPoints = getSerializedPointsForShape(shapeType)
                                    if (prefilledPoints.isNotEmpty()) {
                                        viewModel.animatorPolygonElementToEditId = element.id
                                        viewModel.animatorInitialPolygonString = prefilledPoints
                                        viewModel.animatorInitialIsLineMode = false
                                        viewModel.animatorShowCustomPolygonDialog = true
                                    } else {
                                        viewModel.animatorDialogType = AnimatorDialogType.Edit(element.id, content)
                                    }
                                }
                            }
                        },
                        onRename = { element ->
                            viewModel.animatorElementToRenameId = element.id
                            viewModel.animatorRenameText = getElementDisplayName(
                                element,
                                viewModel.animatorCanvasElements.indexOf(element),
                                viewModel.animatorCanvasElements
                            )
                        },
                        onEditProperties = { element ->
                            viewModel.animatorEditPropertiesElementId = element.id
                            viewModel.animatorEditX = element.offset.x.toString()
                            viewModel.animatorEditY = element.offset.y.toString()
                            viewModel.animatorEditScaleX = element.scaleX.toString()
                            viewModel.animatorEditScaleY = element.scaleY.toString()
                            viewModel.animatorEditRotation = element.rotation.toString()
                            viewModel.animatorEditColorForDialog = if (!element.content.startsWith("Shape:") && !element.content.startsWith("Image:"))
                                element.textColor ?: Color.Black
                            else
                                element.backgroundColor
                            viewModel.animatorEditShadowColorForDialog = element.shadowColor
                            viewModel.animatorEditShadowOffsetX = element.shadowOffsetX
                            viewModel.animatorEditShadowOffsetY = element.shadowOffsetY
                            viewModel.animatorEditBorderThickness = element.borderThickness
                            viewModel.animatorEditBorderColorForDialog = element.borderColor
                            viewModel.animatorShowEditPropertiesDialog = true
                        },
                        onAnimateKeyframes = { element ->
                            viewModel.animatorKeyframeTargetElementId = element.id
                            viewModel.animatorShowKeyframeDialog = true
                        },
                        onToggleVisibility = { viewModel.toggleAnimatorVisibility(it) },
                        onToggleLock = { viewModel.toggleAnimatorLock(it) },
                        onDuplicate = { viewModel.addToAnimatorCanvas(CanvasElement(content = it.content)) },
                        onDelete = {
                            val idx = viewModel.animatorCanvasElements.indexOf(it)
                            if (idx != -1) viewModel.removeFromAnimatorCanvas(idx)
                            viewModel.animatorSelectedElementIds -= it.id
                            if (viewModel.animatorSelectedElementId == it.id) viewModel.animatorSelectedElementId = null
                        },
                        onUngroup = { ids ->
                            viewModel.ungroupAnimatorElements(ids)
                            viewModel.animatorSelectedElementIds = emptySet()
                        },
                        onGroup = { name, ids ->
                            if (name.isNotBlank() && ids.isNotEmpty()) {
                                viewModel.createAnimatorGroup(ids)
                                viewModel.animatorSelectedElementIds = emptySet()
                                viewModel.animatorShowGroupDialog = false
                                viewModel.animatorGroupName = ""
                            }
                        },
                        onClearSelection = { viewModel.animatorSelectedElementIds = emptySet() },
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
                        CanvasElement(content = "Shape: $shape", backgroundColor = color, width = 200f, height = 200f)
                    )
                },
                onCustomPolygon = {
                    viewModel.animatorPolygonElementToEditId = null
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
                enablePlayStop = enablePlayStop,
                onCanvasSizeClick = { showCanvasSizeDialog = true }
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
                            CanvasElement(content = "Shape: $shape", backgroundColor = color, width = 200f, height = 200f)
                        )
                    },
                    onCustomPolygon = {
                        viewModel.animatorPolygonElementToEditId = null
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
                    enablePlayStop = enablePlayStop,
                    onCanvasSizeClick = { showCanvasSizeDialog = true }
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CheckerboardBackground(
                            modifier = Modifier.fillMaxSize(),
                            tileSizeDp = 8.dp,
                            color1 = Color(0xFFCCCCCC),
                            color2 = Color(0xFF999999)
                        )
                        Box(
                            modifier = Modifier
                                .requiredSize(canvasWidthDp, canvasHeightDp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }
                                .background(Color.White)
                        ) {
                            AnimatorCanvasArea(
                                modifier = Modifier.fillMaxSize(),
                                elements = viewModel.animatorCanvasElements,
                                selectedElementIds = viewModel.animatorSelectedElementIds,
                                selectedElementId = viewModel.animatorSelectedElementId,
                                selectedGroups = selectedGroups,
                                dragGroupDelta = dragGroupDelta,
                                onGroupDragDeltaChange = { dragGroupDelta = it },
                                onCanvasElementTap = { onCanvasElementTap(it) },
                                onElementUpdatePosition = { element, offset, w, h, rotation ->
                                    viewModel.updateAnimatorElementProperties(
                                        id = element.id,
                                        x = offset.x,
                                        y = offset.y,
                                        width = w,
                                        height = h,
                                        rotation = rotation
                                    )
                                },
                                onElementScaleChange = { id, sx, sy -> viewModel.updateAnimatorElementScale(id, sx, sy) },
                                onColorPickerRequested = {
                                    viewModel.animatorElementToColorEditId = it
                                    viewModel.animatorShowColorPicker = true
                                },
                                onDeleteRequested = {
                                    val idx = viewModel.animatorCanvasElements.indexOfFirst { element -> element.id == it }
                                    if (idx != -1) viewModel.removeFromAnimatorCanvas(idx)
                                    viewModel.animatorSelectedElementIds -= it
                                    if (viewModel.animatorSelectedElementId == it) {
                                        viewModel.animatorSelectedElementId = null
                                    }
                                },
                                onClearSelection = {
                                    viewModel.animatorSelectedElementIds = emptySet()
                                    viewModel.animatorSelectedElementId = null
                                },
                                themeColors = themeColors,
                                isDark = isDark,
                                elementsGrouped = elementsGrouped,
                                graphicsLayer = graphicsLayer,
                                proportionalEditing = viewModel.proportionalEditing,
                                onProportionalToggle = onProportionalToggle,
                                currentTimeMs = currentTimeMs   // <-- timed canvas
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

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxWidth()
                        .verticalScroll(mainScrollState)
                ) {
                    AnimatorCanvasElementsPanel(
                        elements = viewModel.animatorCanvasElements,
                        onReorder = { from, to ->
                            viewModel.reorderAnimatorCanvasElements(from, to)
                        },
                        selectedElementIds = viewModel.animatorSelectedElementIds,
                        selectedElementId = viewModel.animatorSelectedElementId,
                        showTree = showCanvasElementsTree,
                        onToggleTree = { showCanvasElementsTree = !showCanvasElementsTree },
                        onSingleSelect = { onSingleSelect(it) },
                        onToggleGroupSelection = { toggleGroupSelection(it) },
                        onGroupHeaderTap = { onGroupHeaderTap(it) },
                        onEditElement = { element ->
                            viewModel.animatorDialogType = AnimatorDialogType.Edit(element.id, element.content)
                        },
                        onCustomPolygonEdit = { element ->
                            val content = element.content.trim()
                            when {
                                content.startsWith("Shape:CustomPolygon:") -> {
                                    viewModel.animatorPolygonElementToEditId = element.id
                                    viewModel.animatorInitialPolygonString = content.removePrefix("Shape:CustomPolygon:")
                                    viewModel.animatorInitialIsLineMode = false
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                content.startsWith("Shape:CustomLine:") -> {
                                    viewModel.animatorPolygonElementToEditId = element.id
                                    viewModel.animatorInitialPolygonString = content.removePrefix("Shape:CustomLine:")
                                    viewModel.animatorInitialIsLineMode = true
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                content.startsWith("Shape: Line") || content == "Shape: Line" -> {
                                    viewModel.animatorPolygonElementToEditId = element.id
                                    viewModel.animatorInitialPolygonString = getSerializedPointsForShape("Line")
                                    viewModel.animatorInitialIsLineMode = true
                                    viewModel.animatorShowCustomPolygonDialog = true
                                }
                                else -> {
                                    val shapeType = content.removePrefix("Shape:").trim()
                                    val prefilledPoints = getSerializedPointsForShape(shapeType)
                                    if (prefilledPoints.isNotEmpty()) {
                                        viewModel.animatorPolygonElementToEditId = element.id
                                        viewModel.animatorInitialPolygonString = prefilledPoints
                                        viewModel.animatorInitialIsLineMode = false
                                        viewModel.animatorShowCustomPolygonDialog = true
                                    } else {
                                        viewModel.animatorDialogType = AnimatorDialogType.Edit(element.id, content)
                                    }
                                }
                            }
                        },
                        onRename = { element ->
                            viewModel.animatorElementToRenameId = element.id
                            viewModel.animatorRenameText = getElementDisplayName(
                                element,
                                viewModel.animatorCanvasElements.indexOf(element),
                                viewModel.animatorCanvasElements
                            )
                        },
                        onEditProperties = { element ->
                            viewModel.animatorEditPropertiesElementId = element.id
                            viewModel.animatorEditX = element.offset.x.toString()
                            viewModel.animatorEditY = element.offset.y.toString()
                            viewModel.animatorEditScaleX = element.scaleX.toString()
                            viewModel.animatorEditScaleY = element.scaleY.toString()
                            viewModel.animatorEditRotation = element.rotation.toString()
                            viewModel.animatorEditColorForDialog = if (!element.content.startsWith("Shape:") && !element.content.startsWith("Image:"))
                                element.textColor ?: Color.Black
                            else
                                element.backgroundColor
                            viewModel.animatorEditShadowColorForDialog = element.shadowColor
                            viewModel.animatorEditShadowOffsetX = element.shadowOffsetX
                            viewModel.animatorEditShadowOffsetY = element.shadowOffsetY
                            viewModel.animatorEditBorderThickness = element.borderThickness
                            viewModel.animatorEditBorderColorForDialog = element.borderColor
                            viewModel.animatorShowEditPropertiesDialog = true
                        },
                        onAnimateKeyframes = { element ->
                            viewModel.animatorKeyframeTargetElementId = element.id
                            viewModel.animatorShowKeyframeDialog = true
                        },
                        onToggleVisibility = { viewModel.toggleAnimatorVisibility(it) },
                        onToggleLock = { viewModel.toggleAnimatorLock(it) },
                        onDuplicate = { viewModel.addToAnimatorCanvas(CanvasElement(content = it.content)) },
                        onDelete = {
                            val idx = viewModel.animatorCanvasElements.indexOf(it)
                            if (idx != -1) viewModel.removeFromAnimatorCanvas(idx)
                            viewModel.animatorSelectedElementIds -= it.id
                            if (viewModel.animatorSelectedElementId == it.id) viewModel.animatorSelectedElementId = null
                        },
                        onUngroup = { ids ->
                            viewModel.ungroupAnimatorElements(ids)
                            viewModel.animatorSelectedElementIds = emptySet()
                        },
                        onGroup = { name, ids ->
                            if (name.isNotBlank() && ids.isNotEmpty()) {
                                viewModel.createAnimatorGroup(ids)
                                viewModel.animatorSelectedElementIds = emptySet()
                                viewModel.animatorShowGroupDialog = false
                                viewModel.animatorGroupName = ""
                            }
                        },
                        onClearSelection = { viewModel.animatorSelectedElementIds = emptySet() },
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

    // ---- Dialog sections ----
    when (val dialog = viewModel.animatorDialogType) {
        is AnimatorDialogType.Edit -> {
            AnimatorEditElementDialog(
                elementId = dialog.elementId,
                initialContent = dialog.initialContent,
                onDismiss = { viewModel.animatorDialogType = null },
                onSave = { id, newContent ->
                    val index = viewModel.animatorCanvasElements.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val old = viewModel.animatorCanvasElements[index]
                        val updated = old.copy(content = newContent)
                        viewModel.removeFromAnimatorCanvas(index)
                        viewModel.addToAnimatorCanvas(updated)
                    }
                    viewModel.animatorDialogType = null
                }
            )
        }
        AnimatorDialogType.AddText -> {
            AnimatorEditElementDialog(
                elementId = null,
                initialContent = "",
                isNew = true,
                onDismiss = { viewModel.animatorDialogType = null },
                onSave = { _, newContent ->
                    if (newContent.isNotBlank()) {
                        viewModel.addToAnimatorCanvas(
                            CanvasElement(
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
            AnimatorEditElementDialog(
                elementId = null,
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
                            CanvasElement(
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
        elementId = viewModel.animatorElementToRenameId,
        currentName = viewModel.animatorRenameText,
        onDismiss = {
            viewModel.animatorElementToRenameId = null
            viewModel.animatorRenameText = ""
        },
        onConfirm = { id, newName ->
            if (newName.isNotBlank()) {
                viewModel.renameAnimatorCanvasElement(id, newName)
            }
            viewModel.animatorElementToRenameId = null
            viewModel.animatorRenameText = ""
        }
    )

    if (viewModel.animatorGroupToRenameId != null) {
        RenameDialog(
            elementId = viewModel.animatorGroupToRenameId,
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
            if (name.isNotBlank() && viewModel.animatorSelectedElementIds.isNotEmpty()) {
                viewModel.createAnimatorGroup(viewModel.animatorSelectedElementIds.toList())
                viewModel.animatorSelectedElementIds = emptySet()
                viewModel.animatorShowGroupDialog = false
                viewModel.animatorGroupName = ""
            }
        }
    )

    if (viewModel.animatorShowColorPicker && viewModel.animatorElementToColorEditId != null) {
        val targetElement = viewModel.animatorCanvasElements.find { it.id == viewModel.animatorElementToColorEditId }
        val existingGradient = viewModel.animatorGradientPairs[viewModel.animatorElementToColorEditId]
        val isText = targetElement?.content?.let {
            !it.startsWith("Shape:") && !it.startsWith("Image:")
        } ?: false

        ColorWheelDialog(
            onDismissRequest = {
                viewModel.animatorShowColorPicker = false
                viewModel.animatorElementToColorEditId = null
            },
            onColorSelected = { color ->
                val elementId = viewModel.animatorElementToColorEditId!!
                viewModel.animatorGradientPairs.remove(elementId)
                if (isText) {
                    viewModel.updateAnimatorElementTextColor(elementId, color)
                } else {
                    viewModel.updateAnimatorElementColor(elementId, color)
                }
                viewModel.animatorShowColorPicker = false
                viewModel.animatorElementToColorEditId = null
            },
            initialColor = if (isText) targetElement.textColor ?: Color.Black
            else targetElement?.backgroundColor ?: Color.White,
            enableGradient = true,
            onGradientSelected = { startColor, endColor, startOffset, endOffset ->
                val elementId = viewModel.animatorElementToColorEditId!!
                viewModel.animatorGradientPairs[elementId] = GradientConfig(
                    startColor = startColor,
                    endColor = endColor,
                    startOffset = startOffset,
                    endOffset = endOffset
                )
                if (isText) {
                    viewModel.updateAnimatorElementTextColor(elementId, startColor)
                } else {
                    viewModel.updateAnimatorElementColor(elementId, startColor)
                }
                viewModel.animatorShowColorPicker = false
                viewModel.animatorElementToColorEditId = null
            },
            initialGradientConfig = existingGradient
        )
    }

    if (viewModel.animatorShowCustomPolygonDialog) {
        CustomPolygonDialog(
            initialSerializedPoints = viewModel.animatorInitialPolygonString.takeIf { it.isNotEmpty() },
            isLineMode = viewModel.animatorInitialIsLineMode,
            onDismiss = {
                viewModel.animatorShowCustomPolygonDialog = false
                viewModel.animatorPolygonElementToEditId = null
                viewModel.animatorInitialPolygonString = ""
                viewModel.animatorInitialIsLineMode = false
            },
            onConfirm = { points, isLine ->
                val serialized = points.joinToString(";") { node ->
                    "${node.anchor.x},${node.anchor.y}:${node.handleIn.x},${node.handleIn.y}:${node.handleOut.x},${node.handleOut.y}"
                }
                val shapeType = if (isLine) "CustomLine" else "CustomPolygon"
                val contentString = "Shape:$shapeType:$serialized"
                if (viewModel.animatorPolygonElementToEditId != null) {
                    viewModel.updateAnimatorElementContent(viewModel.animatorPolygonElementToEditId!!, contentString)
                    viewModel.animatorSelectedElementId = viewModel.animatorPolygonElementToEditId
                } else {
                    viewModel.addToAnimatorCanvas(
                        CanvasElement(
                            content = contentString,
                            backgroundColor = getRandomColor(),
                            width = 200f,
                            height = 200f
                        )
                    )
                }
                viewModel.animatorShowCustomPolygonDialog = false
                viewModel.animatorPolygonElementToEditId = null
                viewModel.animatorInitialPolygonString = ""
                viewModel.animatorInitialIsLineMode = false
            }
        )
    }

    if (viewModel.animatorShowKeyframeDialog && viewModel.animatorKeyframeTargetElementId != null) {
        val targetElement = viewModel.animatorCanvasElements.find { it.id == viewModel.animatorKeyframeTargetElementId }
        val elementGradient = viewModel.animatorGradientPairs[viewModel.animatorKeyframeTargetElementId]
        KeyframeAnimationDialog(
            element = targetElement,
            onDismiss = {
                viewModel.animatorShowKeyframeDialog = false
                viewModel.animatorKeyframeTargetElementId = null
            },
            onSaveKeyframes = { elementId, updatedKeyframes, newStartMs, newEndMs ->
                viewModel.updateAnimatorElementKeyframes(elementId, updatedKeyframes)
                viewModel.updateAnimatorElementDuration(elementId, newStartMs, newEndMs)
                viewModel.animatorShowKeyframeDialog = false
                viewModel.animatorKeyframeTargetElementId = null
            },
            timeMultiplier = 1f,
            initialGradientConfig = elementGradient
        )
    }

    val existingGradient = viewModel.animatorGradientPairs[viewModel.animatorEditPropertiesElementId]

    AnimatorEditPropertiesDialog(
        show = viewModel.animatorShowEditPropertiesDialog,
        elementId = viewModel.animatorEditPropertiesElementId,
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
            viewModel.animatorEditPropertiesElementId = null
        },
        onApply = { id, x, y, scaleX, scaleY, rot, color,
                    shadowColor, shadowOffsetX, shadowOffsetY,
                    borderThickness, borderColor,
                    gradientConfig ->
            val currentElement = viewModel.animatorCanvasElements.find { it.id == id }
            if (currentElement != null) {
                val isText = !currentElement.content.startsWith("Shape:") && !currentElement.content.startsWith("Image:")
                viewModel.applyAllAnimatorElementProperties(
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
                if (gradientConfig != null) {
                    viewModel.animatorGradientPairs[id] = gradientConfig
                    viewModel.updateAnimatorElementColor(id, gradientConfig.startColor)
                } else {
                    viewModel.animatorGradientPairs.remove(id)
                }
            }
            viewModel.animatorShowEditPropertiesDialog = false
            viewModel.animatorEditPropertiesElementId = null
        }
    )

    if (showMp4SettingsDialog) {
        Mp4ExportSettingsDialog(
            initialFrameRate = selectedFrameRate,
            initialBitRateMbps = selectedBitRateMbps,
            initialExportMode = exportMode,
            initialOutputMode = offscreenOutputMode,
            initialResolutionMultiplier = offscreenResolutionMultiplier,
            onDismiss = { showMp4SettingsDialog = false },
            onConfirm = { frameRate, bitRate, mode, outMode, resolution ->
                selectedFrameRate = frameRate
                selectedBitRateMbps = bitRate
                exportMode = mode
                offscreenOutputMode = outMode
                offscreenResolutionMultiplier = resolution
                showMp4SettingsDialog = false

                if (mode == "Screen") {
                    isPlayingAnimation = false
                    isRecording = true
                    encoder.value = null
                    exportProgress = 0f
                    recordingMaxTimestamp = 0L
                    lastCaptureTimeUs = 0L
                    hasCapturedFirstFrame = false
                    isPlayingAnimation = true
                } else {
                    val allElements = viewModel.animatorCanvasElements.toList()
                    val startMs = 0L
                    val endMs = allElements.flatMap { it.keyframes }
                        .maxOfOrNull { it.timestampMs } ?: 0L

                    isOffscreenExporting = true
                    offscreenExportProgress = 0f
                    offscreenExportJob = exportScope.launch(Dispatchers.IO) {
                        try {
                            nativeExport(
                                context,
                                canvasWidthPx,
                                canvasHeightPx,
                                frameRate,
                                bitRate,
                                resolution,
                                viewModel.animatorCanvasElements.toList(),
                                viewModel.animatorGradientPairs.toMap(),
                                startTimeMs = startMs,
                                endTimeMs = endMs
                            ) { progress ->
                                withContext(Dispatchers.Main) {
                                    offscreenExportProgress = progress
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Video saved to gallery", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isOffscreenExporting = false
                            }
                        }
                    }
                }
            }
        )
    }

    if (showCanvasSizeDialog) {
        CanvasSizeDialog(
            initialWidth = customWidthPx,
            initialHeight = customHeightPx,
            onDismiss = { showCanvasSizeDialog = false },
            onConfirm = { width, height ->
                customWidthPx = width
                customHeightPx = height
                showCanvasSizeDialog = false
            }
        )
    }

    if (isOffscreenExporting) {
        ExportDialog(
            progress = offscreenExportProgress,
            onCancelRequested = {
                offscreenExportJob?.cancel()
                isOffscreenExporting = false
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