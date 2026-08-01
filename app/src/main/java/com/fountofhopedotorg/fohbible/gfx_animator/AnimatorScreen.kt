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
import com.fountofhopedotorg.fohbible.data.AnimatorTab
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.data.ThemeColors
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
import org.json.JSONArray
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import org.json.JSONObject

fun offsetForPivotChange(
    element: CanvasElement,
    oldPivotX: Float,
    oldPivotY: Float,
    newPivotX: Float,
    newPivotY: Float
): Offset {
    val w = element.width
    val h = element.height

    val dx = (newPivotX - oldPivotX) * w
    val dy = (newPivotY - oldPivotY) * h

    val scaledDx = dx * element.scaleX
    val scaledDy = dy * element.scaleY

    val rad = element.rotation * (PI / 180.0).toFloat()
    val cosA = cos(rad)
    val sinA = sin(rad)

    val rotatedDx = scaledDx * cosA - scaledDy * sinA
    val rotatedDy = scaledDx * sinA + scaledDy * cosA

    val deltaOffsetX = rotatedDx - dx
    val deltaOffsetY = rotatedDy - dy

    return element.offset + Offset(deltaOffsetX, deltaOffsetY)
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AnimatorScreen(
    templateUriToLoad: Uri? = null,
    onTemplateConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current

    val viewModel: AppViewModel = viewModel()
    val graphicsLayer = rememberGraphicsLayer()
    var showCanvasSettingsDialog by remember { mutableStateOf(false) }

    var customWidthPx by rememberSaveable { mutableIntStateOf(if (isLandscape) 1920 else 1080) }
    var customHeightPx by rememberSaveable { mutableIntStateOf(if (isLandscape) 1080 else 1920) }
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

    var isPivotPlacementActive by remember { mutableStateOf(false) }
    var pivotTargetId by remember { mutableStateOf<String?>(null) }

    var activeTab by remember { mutableStateOf(AnimatorTab.LAYERS) }

    val cancelExport: () -> Unit = remember { { isPlayingAnimation = false } }

    val onPlayPause = remember {
        {
            if (isPlayingAnimation) {
                isPlayingAnimation = false
            } else {
                viewModel.animatorSelectedElementIds = emptySet()
                viewModel.animatorSelectedElementId = null

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

    val onSaveVideo: () -> Unit = remember {
        {
            if (viewModel.animatorCanvasElements.isEmpty()) {
                Toast.makeText(context, "Canvas is empty", Toast.LENGTH_SHORT).show()
                return@remember
            }
            showMp4SettingsDialog = true
        }
    }
    val onStartPivotPlacement: (String) -> Unit = { elementId ->
        isPivotPlacementActive = true
        pivotTargetId = elementId
    }

    val onPlacePivotLocal: (Float, Float) -> Unit = { px, py ->
        val id = pivotTargetId
        if (id != null) {
            val index = viewModel.animatorCanvasElements.indexOfFirst { it.id == id }
            if (index != -1) {
                val oldElement = viewModel.animatorCanvasElements[index]

                val newOffset = offsetForPivotChange(
                    element = oldElement,
                    oldPivotX = oldElement.pivotX,
                    oldPivotY = oldElement.pivotY,
                    newPivotX = px,
                    newPivotY = py
                )

                viewModel.animatorCanvasElements[index] = oldElement.copy(
                    pivotX = px,
                    pivotY = py,
                    offset = newOffset
                )
            }
            isPivotPlacementActive = false
            pivotTargetId = null
        }
    }

    LaunchedEffect(templateUriToLoad) {
        templateUriToLoad?.let { uri ->
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                }
                if (jsonString.isBlank()) {
                    Toast.makeText(context, "Empty template", Toast.LENGTH_SHORT).show()
                    onTemplateConsumed()
                    return@LaunchedEffect
                }

                val trimmed = jsonString.trim()
                if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)
                    if (root.has("elements")) {
                        val elementsArray = root.getJSONArray("elements")
                        val loadedElements = (0 until elementsArray.length()).map { i ->
                            CanvasElement.fromJson(elementsArray.getJSONObject(i))
                        }
                        val gradientsObj = root.optJSONObject("gradients")
                        val loadedGradients = mutableMapOf<String, GradientConfig>()
                        gradientsObj?.keys()?.forEach { id ->
                            loadedGradients[id] = GradientConfig.fromJson(gradientsObj.getJSONObject(id))
                        }
                        viewModel.animatorCanvasElements.clear()
                        viewModel.animatorCanvasElements.addAll(loadedElements)
                        viewModel.animatorGradientPairs.clear()
                        viewModel.animatorGradientPairs.putAll(loadedGradients)

                        val loadedCanvasWidth = root.optInt("canvasWidth", 0).takeIf { it > 0 }
                        val loadedCanvasHeight = root.optInt("canvasHeight", 0).takeIf { it > 0 }
                        if (loadedCanvasWidth != null && loadedCanvasHeight != null) {
                            customWidthPx = loadedCanvasWidth
                            customHeightPx = loadedCanvasHeight
                        }
                    } else {
                        Toast.makeText(context, "Invalid project format", Toast.LENGTH_SHORT).show()
                    }
                } else if (trimmed.startsWith("[")) {
                    val jsonArray = JSONArray(trimmed)
                    val loadedElements = (0 until jsonArray.length()).map { i ->
                        CanvasElement.fromJson(jsonArray.getJSONObject(i))
                    }
                    viewModel.animatorCanvasElements.clear()
                    viewModel.animatorCanvasElements.addAll(loadedElements)
                    viewModel.animatorGradientPairs.clear()
                } else {
                    Toast.makeText(context, "Unknown file format", Toast.LENGTH_SHORT).show()
                }

                viewModel.animatorSelectedElementIds = emptySet()
                viewModel.animatorSelectedElementId = null
                Toast.makeText(context, "Template loaded", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open template: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                onTemplateConsumed()
            }
        }
    }


    LaunchedEffect(isPlayingAnimation) {
        if (!isPlayingAnimation) {
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
                context = context,
                width = width,
                height = height,
                frameRate = selectedFrameRate,
                bitRate = selectedBitRateMbps * 1_000_000,
                canvasBackgroundColor = viewModel.canvasBackgroundColor,
                canvasBackgroundBrush = viewModel.canvasBackgroundBrush
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
            currentTimeMs = currentMs

            val snapshot = viewModel.animatorCanvasElements.toList()
            for (i in snapshot.indices) {
                val element = snapshot[i]
                if (element.keyframes.isEmpty()) continue

                val sortedKeyframes = element.keyframes.sortedBy { it.timestampMs }
                val (kfPrev, kfNext) = findSurroundingKeyframes(sortedKeyframes, currentMs)

                val progress = if (kfNext != null && kfPrev != null && kfNext.timestampMs != kfPrev.timestampMs) {
                    val rawProgress = ((currentMs - kfPrev.timestampMs).toFloat() /
                            (kfNext.timestampMs - kfPrev.timestampMs)).coerceIn(0f, 1f)

                    ease(
                        rawProgress,
                        kfNext.tweenType,
                        customPoints = kfNext.customPoints ?: emptyList()
                    )
                } else 0f

                val startX = kfPrev?.x ?: element.offset.x
                val startY = kfPrev?.y ?: element.offset.y
                val endX   = kfNext?.x ?: element.offset.x
                val endY   = kfNext?.y ?: element.offset.y

                val newScaleX = lerp(kfPrev?.scaleX ?: element.scaleX, kfNext?.scaleX ?: element.scaleX, progress)
                val newScaleY = lerp(kfPrev?.scaleY ?: element.scaleY, kfNext?.scaleY ?: element.scaleY, progress)
                val newRotation = lerp(kfPrev?.rotation ?: element.rotation, kfNext?.rotation ?: element.rotation, progress)

                val newPivotX = if (kfPrev?.pivotX != null || kfNext?.pivotX != null)
                    lerp(kfPrev?.pivotX ?: element.pivotX, kfNext?.pivotX ?: element.pivotX, progress)
                else element.pivotX

                val newPivotY = if (kfPrev?.pivotY != null || kfNext?.pivotY != null)
                    lerp(kfPrev?.pivotY ?: element.pivotY, kfNext?.pivotY ?: element.pivotY, progress)
                else element.pivotY

                if (kfNext?.ellipticalRotation == true && kfPrev != null) {
                    val startOffset = Offset(startX, startY)
                    val endOffset = Offset(endX, endY)
                    val distance = (endOffset - startOffset).getDistance()

                    val rotStart = kfPrev.rotation ?: element.rotation
                    val rotEnd   = kfNext.rotation ?: element.rotation

                    if (distance < 0.5f) {
                        val orig = originalElementStates[element.id] ?: element
                        val pivotX = kfPrev.pivotX ?: orig.pivotX
                        val pivotY = kfPrev.pivotY ?: orig.pivotY
                        val w = element.width
                        val h = element.height

                        val px = pivotX * w
                        val py = pivotY * h

                        val localCx = w / 2f - px
                        val localCy = h / 2f - py

                        val rotStartRad = rotStart * (PI / 180).toFloat()
                        val cosR0 = cos(rotStartRad)
                        val sinR0 = sin(rotStartRad)

                        val startDx = localCx * cosR0 - localCy * sinR0
                        val startDy = localCx * sinR0 + localCy * cosR0
                        val r0 = sqrt(startDx * startDx + startDy * startDy)

                        if (r0 < 0.5f) {
                            viewModel.animatorCanvasElements[i] = element.copy(
                                offset = startOffset,
                                pivotX = pivotX, pivotY = pivotY,
                                scaleX = newScaleX, scaleY = newScaleY,
                                rotation = newRotation
                            )
                        } else {
                            val stretchX = kfNext.ellipticalStretchX.coerceAtLeast(0.01f)
                            val stretchY = kfNext.ellipticalStretchY.coerceAtLeast(0.01f)

                            val startPhi = atan2(startDy * stretchX, startDx * stretchY)
                            val mag = sqrt((startDx * stretchY) * (startDx * stretchY) + (startDy * stretchX) * (startDy * stretchX))
                            val rBase = mag / (stretchX * stretchY)

                            val a = rBase * stretchX
                            val b = rBase * stretchY

                            val currentRotRad = newRotation * (PI / 180).toFloat()
                            val phi = startPhi + (currentRotRad - rotStartRad)

                            val cosR = cos(currentRotRad)
                            val sinR = sin(currentRotRad)
                            val currentDx = localCx * cosR - localCy * sinR
                            val currentDy = localCx * sinR + localCy * cosR

                            val newOffsetX = startOffset.x + a * cos(phi) - currentDx
                            val newOffsetY = startOffset.y + b * sin(phi) - currentDy

                            viewModel.animatorCanvasElements[i] = element.copy(
                                offset = Offset(newOffsetX, newOffsetY),
                                pivotX = pivotX,
                                pivotY = pivotY,
                                scaleX = newScaleX,
                                scaleY = newScaleY,
                                rotation = newRotation
                            )
                        }
                    } else {
                        val center = Offset(
                            (startOffset.x + endOffset.x) / 2f,
                            (startOffset.y + endOffset.y) / 2f
                        )
                        val delta = endOffset - startOffset
                        val halfDist = distance / 2f

                        val u = if (distance > 0f) Offset(delta.x / distance, delta.y / distance) else Offset(1f, 0f)
                        val v = Offset(-u.y, u.x)

                        val stretchX = kfNext.ellipticalStretchX.coerceAtLeast(0.01f)
                        val stretchY = kfNext.ellipticalStretchY.coerceAtLeast(0.01f)
                        val b = halfDist * (stretchY / stretchX)

                        val t = progress.coerceIn(0f, 1f)
                        val deltaRot = rotEnd - rotStart
                        val phi: Float = if (deltaRot < 0f) {
                            Math.PI.toFloat() * (1f + t)
                        } else {
                            Math.PI.toFloat() * (1f - t)
                        }

                        val targetCenter = Offset(
                            center.x + halfDist * cos(phi) * u.x + b * sin(phi) * v.x,
                            center.y + halfDist * cos(phi) * u.y + b * sin(phi) * v.y
                        )

                        val w = element.width
                        val h = element.height
                        val px = newPivotX * w
                        val py = newPivotY * h
                        val localCx = w / 2f - px
                        val localCy = h / 2f - py

                        val rotRad = newRotation * (PI / 180).toFloat()
                        val cosR = cos(rotRad)
                        val sinR = sin(rotRad)
                        val dx = localCx * cosR - localCy * sinR
                        val dy = localCx * sinR + localCy * cosR

                        viewModel.animatorCanvasElements[i] = element.copy(
                            offset = Offset(targetCenter.x - dx, targetCenter.y - dy),
                            pivotX = newPivotX,
                            pivotY = newPivotY,
                            scaleX = newScaleX,
                            scaleY = newScaleY,
                            rotation = newRotation
                        )
                    }
                }
                else {
                    val newX = lerp(startX, endX, progress)
                    val newY = lerp(startY, endY, progress)

                    val orig = originalElementStates[element.id] ?: element
                    val oldPivotX = orig.pivotX
                    val oldPivotY = orig.pivotY

                    val tempElement = CanvasElement(
                        id = element.id,
                        offset = Offset(newX, newY),
                        width = element.width,
                        height = element.height,
                        scaleX = newScaleX,
                        scaleY = newScaleY,
                        rotation = newRotation,
                        pivotX = newPivotX,
                        pivotY = newPivotY,
                        content = element.content,
                        backgroundColor = element.backgroundColor,
                        textColor = element.textColor
                    )

                    val finalOffset = offsetForPivotChange(
                        element = tempElement,
                        oldPivotX = oldPivotX,
                        oldPivotY = oldPivotY,
                        newPivotX = newPivotX,
                        newPivotY = newPivotY
                    )
                    viewModel.animatorCanvasElements[i] = element.copy(
                        offset = finalOffset,
                        pivotX = newPivotX,
                        pivotY = newPivotY,
                        scaleX = newScaleX,
                        scaleY = newScaleY,
                        rotation = newRotation
                    )
                }
                val newColor = if (kfPrev?.color != null && kfNext?.color != null) {
                    lerpColor(kfPrev.color, kfNext.color, progress)
                } else kfNext?.color ?: kfPrev?.color

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
                        .weight(0.6f)
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
                                    currentTimeMs = currentTimeMs,
                                    isPivotPlacementActive = isPivotPlacementActive,
                                    pivotTargetId = pivotTargetId,
                                    onStartPivotPlacement = onStartPivotPlacement,
                                    onPlacePivotLocal = onPlacePivotLocal,
                                    canvasBackgroundColor = viewModel.canvasBackgroundColor,
                                    canvasBackgroundBrush = viewModel.canvasBackgroundBrush,
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
                        .weight(0.4f)
                        .fillMaxHeight()
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
                            val isText = !element.content.startsWith("Shape:") && !element.content.startsWith("Image:")
                            viewModel.animatorEditPropertiesElementId = element.id
                            viewModel.animatorEditX = element.offset.x.toString()
                            viewModel.animatorEditY = element.offset.y.toString()
                            viewModel.animatorEditScaleX = element.scaleX.toString()
                            viewModel.animatorEditScaleY = element.scaleY.toString()
                            viewModel.animatorEditRotation = element.rotation.toString()
                            viewModel.animatorEditColorForDialog = if (isText) element.textColor ?: Color.Black else element.backgroundColor
                            viewModel.animatorEditShadowColorForDialog = element.shadowColor
                            viewModel.animatorEditShadowOffsetX = element.shadowOffsetX
                            viewModel.animatorEditShadowOffsetY = element.shadowOffsetY
                            viewModel.animatorEditBorderThickness = element.borderThickness
                            viewModel.animatorEditBorderColorForDialog = element.borderColor
                            viewModel.animatorEditFontFamily = element.fontFamily ?: "system"
                            viewModel.animatorEditTextAlign = element.textAlign ?: "Center"
                            viewModel.animatorEditIsTextElement = isText
                            viewModel.animatorEditPivotX = element.pivotX
                            viewModel.animatorEditPivotY = element.pivotY
                            viewModel.animatorShowEditPropertiesDialog = true
                        },
                        onAnimateKeyframes = { element ->
                            viewModel.animatorKeyframeTargetElementId = element.id
                            viewModel.animatorShowKeyframeDialog = true
                        },
                        onToggleVisibility = { viewModel.toggleAnimatorVisibility(it) },
                        onToggleLock = { viewModel.toggleAnimatorLock(it) },
                        onDuplicate = { element ->
                            val newId = UUID.randomUUID().toString()
                            viewModel.addToAnimatorCanvas(element.copy(id = newId))
                            viewModel.animatorGradientPairs[element.id]?.let { existingGradient ->
                                viewModel.animatorGradientPairs[newId] = existingGradient.copy()
                            }
                        },
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
                        activeTab = activeTab,
                        onTabSelected = { activeTab = it },
                        viewModel = viewModel,
                        fineTunerSelectedElementId = viewModel.animatorSelectedElementId,
                        onSaveKeyframes = { elementId, keyframes, startMs, endMs ->
                            val index = viewModel.animatorCanvasElements.indexOfFirst { it.id == elementId }
                            if (index != -1) {
                                val existing = viewModel.animatorCanvasElements[index]
                                viewModel.animatorCanvasElements[index] = existing.copy(
                                    keyframes = keyframes,
                                    startTimeMs = startMs,
                                    endTimeMs = endMs
                                )
                            }
                        },
                        timeMultiplier = 1f,
                        canvasWidth = canvasWidthPx,
                        canvasHeight = canvasHeightPx
                    )
                }
            }
            AnimatorToolbar(
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
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                onLoadCanvasSize = { w, h ->
                    customWidthPx = w
                    customHeightPx = h
                },
                onCanvasSettingsClick = { showCanvasSettingsDialog = true }
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {

            Column(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
            ) {
                AnimatorToolbar(
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
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onLoadCanvasSize = { w, h ->
                        customWidthPx = w
                        customHeightPx = h
                    },
                    onCanvasSettingsClick = { showCanvasSettingsDialog = true }
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
                        .weight(0.6f)
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
                                currentTimeMs = currentTimeMs,
                                isPivotPlacementActive = isPivotPlacementActive,
                                pivotTargetId = pivotTargetId,
                                onStartPivotPlacement = onStartPivotPlacement,
                                onPlacePivotLocal = onPlacePivotLocal,
                                canvasBackgroundColor = viewModel.canvasBackgroundColor,
                                canvasBackgroundBrush = viewModel.canvasBackgroundBrush,
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

                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth()
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
                            val isText = !element.content.startsWith("Shape:") && !element.content.startsWith("Image:")
                            viewModel.animatorEditPropertiesElementId = element.id
                            viewModel.animatorEditX = element.offset.x.toString()
                            viewModel.animatorEditY = element.offset.y.toString()
                            viewModel.animatorEditScaleX = element.scaleX.toString()
                            viewModel.animatorEditScaleY = element.scaleY.toString()
                            viewModel.animatorEditRotation = element.rotation.toString()
                            viewModel.animatorEditColorForDialog = if (isText) element.textColor ?: Color.Black else element.backgroundColor
                            viewModel.animatorEditShadowColorForDialog = element.shadowColor
                            viewModel.animatorEditShadowOffsetX = element.shadowOffsetX
                            viewModel.animatorEditShadowOffsetY = element.shadowOffsetY
                            viewModel.animatorEditBorderThickness = element.borderThickness
                            viewModel.animatorEditBorderColorForDialog = element.borderColor
                            viewModel.animatorEditFontFamily = element.fontFamily ?: "system"
                            viewModel.animatorEditTextAlign = element.textAlign ?: "Center"
                            viewModel.animatorEditIsTextElement = isText
                            viewModel.animatorEditPivotX = element.pivotX
                            viewModel.animatorEditPivotY = element.pivotY
                            viewModel.animatorShowEditPropertiesDialog = true
                        },
                        onAnimateKeyframes = { element ->
                            viewModel.animatorKeyframeTargetElementId = element.id
                            viewModel.animatorShowKeyframeDialog = true
                        },
                        onToggleVisibility = { viewModel.toggleAnimatorVisibility(it) },
                        onToggleLock = { viewModel.toggleAnimatorLock(it) },
                        onDuplicate = { element ->
                            val newId = UUID.randomUUID().toString()
                            viewModel.addToAnimatorCanvas(element.copy(id = newId))
                            viewModel.animatorGradientPairs[element.id]?.let { existingGradient ->
                                viewModel.animatorGradientPairs[newId] = existingGradient.copy()
                            }
                        },
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
                        activeTab = activeTab,
                        onTabSelected = { activeTab = it },
                        viewModel = viewModel,
                        fineTunerSelectedElementId = viewModel.animatorSelectedElementId,

                        onSaveKeyframes = { elementId, keyframes, startMs, endMs ->
                            val index = viewModel.animatorCanvasElements.indexOfFirst { it.id == elementId }
                            if (index != -1) {
                                val existing = viewModel.animatorCanvasElements[index]
                                viewModel.animatorCanvasElements[index] = existing.copy(
                                    keyframes = keyframes,
                                    startTimeMs = startMs,
                                    endTimeMs = endMs
                                )
                            }
                        },
                        timeMultiplier = 1f,
                        canvasWidth = canvasWidthPx,
                        canvasHeight = canvasHeightPx
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

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
        viewModel.animatorColorWheel = true
        val targetElement = viewModel.animatorCanvasElements.find { it.id == viewModel.animatorElementToColorEditId }
        val existingGradient = viewModel.animatorGradientPairs[viewModel.animatorElementToColorEditId]
        val isText = targetElement?.content?.let {
            !it.startsWith("Shape:") && !it.startsWith("Image:")
        } ?: false

        ColorWheelDialog(
            onDismissRequest = {
                viewModel.animatorShowColorPicker = false
                viewModel.animatorElementToColorEditId = null
                viewModel.animatorColorWheel = false
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
            allElements = viewModel.animatorCanvasElements,
            onElementSelected = { selectedElement ->
                viewModel.animatorKeyframeTargetElementId = selectedElement.id
            },
            onDismiss = {
                viewModel.animatorShowKeyframeDialog = false
                viewModel.animatorKeyframeTargetElementId = null
            },
            onSaveKeyframes = { elementId, updatedKeyframes, newStartMs, newEndMs ->
                viewModel.updateAnimatorElementKeyframes(elementId, updatedKeyframes)
                viewModel.updateAnimatorElementDuration(elementId, newStartMs, newEndMs)
            },
            timeMultiplier = 1f,
            initialGradientConfig = elementGradient,
            canvasWidth = customWidthPx,
            canvasHeight = customHeightPx,
            themeColors = themeColors,
            gradientConfigs = viewModel.animatorGradientPairs
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
        initialFontFamily = viewModel.animatorEditFontFamily,
        initialTextAlign = viewModel.animatorEditTextAlign,
        isTextElement = viewModel.animatorEditIsTextElement,
        initialPivotX = viewModel.animatorEditPivotX,
        initialPivotY = viewModel.animatorEditPivotY,
        onDismiss = {
            viewModel.animatorShowEditPropertiesDialog = false
            viewModel.animatorEditPropertiesElementId = null
        },
        onApply = { id, x, y, scaleX, scaleY, rot, color,
                    shadowColor, shadowOffsetX, shadowOffsetY,
                    borderThickness, borderColor,
                    gradientConfig, fontFamily, textAlign,  pivotX, pivotY ->

            val index = viewModel.animatorCanvasElements.indexOfFirst { it.id == id }
            if (index != -1) {
                val element = viewModel.animatorCanvasElements[index]
                val isText = !element.content.startsWith("Shape:") && !element.content.startsWith("Image:")

                viewModel.animatorCanvasElements[index] = element.copy(
                    offset = Offset(
                        x.toFloatOrNull() ?: element.offset.x,
                        y.toFloatOrNull() ?: element.offset.y
                    ),
                    scaleX = scaleX.toFloatOrNull()?.coerceIn(0.05f, 25f) ?: element.scaleX,
                    scaleY = scaleY.toFloatOrNull()?.coerceIn(0.05f, 25f) ?: element.scaleY,
                    rotation = rot.toFloatOrNull() ?: element.rotation,
                    backgroundColor = if (isText) element.backgroundColor else color,
                    textColor = if (isText) color else element.textColor,
                    shadowColor = shadowColor,
                    shadowOffsetX = shadowOffsetX,
                    shadowOffsetY = shadowOffsetY,
                    borderThickness = borderThickness,
                    borderColor = borderColor,
                    fontFamily = fontFamily,
                    textAlign = textAlign,
                    pivotX = pivotX,
                    pivotY = pivotY
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
                                endTimeMs = endMs,
                                canvasBackgroundColor = viewModel.canvasBackgroundColor,
                                canvasBackgroundBrush = viewModel.canvasBackgroundBrush,
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

    if (showCanvasSettingsDialog) {
        CanvasSettingsDialog(
            initialWidth = customWidthPx,
            initialHeight = customHeightPx,
            onDismiss = { showCanvasSettingsDialog = false },
            onConfirmSize = { w, h ->
                customWidthPx = w
                customHeightPx = h
            },
            onSolidColorSelected = { color ->
                viewModel.canvasBackgroundColor = color
                viewModel.canvasBackgroundBrush = null
            },
            onGradientSelected = { brush ->
                viewModel.canvasBackgroundColor = null
                viewModel.canvasBackgroundBrush = brush
            },
            onTransparentSelected = {
                viewModel.canvasBackgroundColor = Color.Transparent
                viewModel.canvasBackgroundBrush = null
            },
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