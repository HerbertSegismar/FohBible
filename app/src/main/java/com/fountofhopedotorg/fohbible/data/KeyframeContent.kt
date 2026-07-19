package com.fountofhopedotorg.fohbible.data

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fountofhopedotorg.fohbible.color_wheel.ColorWheelDialog
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.HorizontalDivider
import com.fountofhopedotorg.fohbible.gfx_animator.CustomEasingEditor
import com.fountofhopedotorg.fohbible.gfx_animator.EllipticalRotationSection
import com.fountofhopedotorg.fohbible.gfx_animator.GradientConfigNullableSaver
import com.fountofhopedotorg.fohbible.gfx_animator.MAX_VISIBLE_DURATION_MS
import com.fountofhopedotorg.fohbible.gfx_animator.formatPosition
import com.fountofhopedotorg.fohbible.gfx_animator.formatRotation
import com.fountofhopedotorg.fohbible.gfx_animator.formatScale
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyframeAnimationContent(
    element: CanvasElement?,
    allElements: List<CanvasElement>,
    onElementSelected: (CanvasElement) -> Unit,
    onCancel: () -> Unit,
    onSave: (elementId: String, keyframes: List<CanvasKeyframe>, startMs: Long, endMs: Long) -> Unit,
    timeMultiplier: Float,
    initialGradientConfig: GradientConfig? = null,
    canvasWidth: Int,
    canvasHeight: Int
) {
    if (element == null) return

    var scrollOffsetSaved by rememberSaveable("timelineScroll") { mutableIntStateOf(0) }
    var pivotXState by remember { mutableFloatStateOf(element.pivotX) }
    var pivotYState by remember { mutableFloatStateOf(element.pivotY) }
    var ellipticalRotation by rememberSaveable(element.id) { mutableStateOf(false) }
    var ellipticalStretchX by rememberSaveable(element.id) { mutableFloatStateOf(1f) }
    var ellipticalStretchY by rememberSaveable(element.id) { mutableFloatStateOf(0.5f) }

    val xMin = -0.05f * canvasWidth
    val xMax = 1.05f * canvasWidth
    val yMin = -0.05f * canvasHeight
    val yMax = 1.05f * canvasHeight

    fun storedToDisplay(storedMs: Long): Long = (storedMs / timeMultiplier).roundToInt().toLong()
    fun displayToStored(displayMs: Long): Long = (displayMs * timeMultiplier).roundToInt().toLong()

    val effectiveInitialGradientConfig = remember(element) {
        element.keyframes.minByOrNull { it.timestampMs }?.gradientConfig ?: initialGradientConfig
    }

    val initialElementColor: Color = remember(element) {
        if (element.content.startsWith("Shape:") || element.content.startsWith("Image:")) element.backgroundColor
        else element.textColor ?: Color.Black
    }

    val localKeyframes = remember(element.id) { mutableStateListOf(*element.keyframes.toTypedArray()) }
    var currentGradientConfig by rememberSaveable(element.id, stateSaver = GradientConfigNullableSaver) {
        mutableStateOf(effectiveInitialGradientConfig)
    }
    var editingKeyframeTimestamp by rememberSaveable(element.id) { mutableStateOf<Long?>(null) }
    var editingSegmentIndex by remember { mutableStateOf<Int?>(null) }

    var trimStartMs by rememberSaveable(element.id) { mutableLongStateOf(element.startTimeMs) }
    var trimEndMs by rememberSaveable(element.id) {
        mutableLongStateOf(if (element.endTimeMs == Long.MAX_VALUE) 5_000L else element.endTimeMs)
    }

    val initialUniversalDurationMs = remember {
        val maxEnd = allElements.maxOfOrNull { el ->
            val rawEnd = if (el.endTimeMs == Long.MAX_VALUE) 5_000L else el.endTimeMs
            val maxKf = el.keyframes.maxOfOrNull { it.timestampMs } ?: 0L
            maxOf(rawEnd, maxKf)
        } ?: 5_000L
        maxEnd + 10_000L
    }
    var universalDurationMs by rememberSaveable("universalTimelineDuration") {
        mutableLongStateOf(initialUniversalDurationMs)
    }

    fun clampTrimsToUniversal() {
        trimStartMs = trimStartMs.coerceIn(0L, universalDurationMs)
        trimEndMs = trimEndMs.coerceIn(0L, universalDurationMs)
        if (trimStartMs > trimEndMs) trimStartMs = trimEndMs
    }

    val handleUniversalDurationChange: (Long) -> Unit = { newDuration ->
        universalDurationMs = newDuration
        clampTrimsToUniversal()
    }

    val timelineScrollState = rememberScrollState()

    val rawStart = element.startTimeMs
    val rawEnd = if (element.endTimeMs == Long.MAX_VALUE) 5_000L else element.endTimeMs
    val initialCenterMs = rawStart + (rawEnd - rawStart) / 2
    var timeInput by rememberSaveable(element.id) {
        mutableStateOf(storedToDisplay(initialCenterMs).toString())
    }

    LaunchedEffect(trimStartMs, trimEndMs) {
        val midpointStored = trimStartMs + (trimEndMs - trimStartMs) / 2
        timeInput = storedToDisplay(midpointStored).toString()
    }

    LaunchedEffect(element.id) {
        pivotXState = element.pivotX
        pivotYState = element.pivotY
    }

    var xInput by rememberSaveable(element.id) { mutableStateOf(formatPosition(element.offset.x)) }
    var yInput by rememberSaveable(element.id) { mutableStateOf(formatPosition(element.offset.y)) }
    var scaleXInput by rememberSaveable(element.id) { mutableStateOf(formatScale(element.scaleX)) }
    var scaleYInput by rememberSaveable(element.id) { mutableStateOf(formatScale(element.scaleY)) }
    var rotationInput by rememberSaveable(element.id) { mutableStateOf(formatRotation(element.rotation)) }

    var pickedColorArgb by rememberSaveable(element.id) {
        mutableLongStateOf(initialElementColor.toArgb().toLong())
    }

    var showColorDialog by rememberSaveable(element.id) { mutableStateOf(false) }

    val pickedColor = Color(pickedColorArgb.toInt())

    val currentElement by rememberUpdatedState(element)
    val currentTrimStartMs by rememberUpdatedState(trimStartMs)
    val currentTrimEndMs by rememberUpdatedState(trimEndMs)
    val currentLocalKeyframes by rememberUpdatedState(localKeyframes.toList())

    val autoSaveAndSelect: (CanvasElement) -> Unit = remember {
        { newElement ->
            val oldElement = currentElement
            if (newElement.id != oldElement.id) {
                onSave(
                    oldElement.id,
                    currentLocalKeyframes.sortedBy { it.timestampMs },
                    currentTrimStartMs,
                    currentTrimEndMs
                )
            }
            onElementSelected(newElement)
        }
    }

    fun populateFromKeyframe(kf: CanvasKeyframe) {
        timeInput = storedToDisplay(kf.timestampMs).toString()
        xInput = kf.x?.let { formatPosition(it) } ?: formatPosition(element.offset.x)
        yInput = kf.y?.let { formatPosition(it) } ?: formatPosition(element.offset.y)
        scaleXInput = kf.scaleX?.let { formatScale(it) } ?: formatScale(element.scaleX)
        scaleYInput = kf.scaleY?.let { formatScale(it) } ?: formatScale(element.scaleY)
        rotationInput = kf.rotation?.let { formatRotation(it) } ?: formatRotation(element.rotation)
        val displayColor = kf.gradientConfig?.startColor ?: kf.color ?: initialElementColor
        pickedColorArgb = displayColor.toArgb().toLong()
        currentGradientConfig = kf.gradientConfig
        pivotXState = kf.pivotX ?: element.pivotX
        pivotYState = kf.pivotY ?: element.pivotY
        ellipticalRotation = kf.ellipticalRotation
        ellipticalStretchX = kf.ellipticalStretchX
        ellipticalStretchY = kf.ellipticalStretchY
    }

    LaunchedEffect(element.id) {
        if (localKeyframes.isEmpty()) {
            localKeyframes.add(
                CanvasKeyframe(
                    timestampMs = trimStartMs,
                    x = element.offset.x, y = element.offset.y,
                    scaleX = element.scaleX, scaleY = element.scaleY,
                    rotation = element.rotation,
                    color = initialElementColor,
                    gradientConfig = null,
                    shadowColor = element.shadowColor,
                    shadowOffsetX = element.shadowOffsetX,
                    shadowOffsetY = element.shadowOffsetY,
                    borderThickness = element.borderThickness,
                    borderColor = element.borderColor,
                    fontFamily = element.fontFamily,
                    textAlign = element.textAlign
                )
            )
        }
    }

    LaunchedEffect(trimStartMs) {
        val firstKf = localKeyframes.minByOrNull { it.timestampMs }
        if (firstKf != null && firstKf.timestampMs != trimStartMs) {
            val index = localKeyframes.indexOf(firstKf)
            localKeyframes[index] = firstKf.copy(timestampMs = trimStartMs)
        }
    }

    LaunchedEffect(effectiveInitialGradientConfig, initialElementColor) {
        val firstKf = localKeyframes.minByOrNull { it.timestampMs }
        if (firstKf != null) {
            val index = localKeyframes.indexOf(firstKf)
            localKeyframes[index] = firstKf.copy(
                color = effectiveInitialGradientConfig?.startColor ?: initialElementColor,
                gradientConfig = effectiveInitialGradientConfig
            )
        }
    }

    LaunchedEffect(element) {
        editingKeyframeTimestamp = null
        val midpointStored = trimStartMs + (trimEndMs - trimStartMs) / 2
        timeInput = storedToDisplay(midpointStored).toString()

        val firstKeyframe = localKeyframes.minByOrNull { it.timestampMs }
        if (firstKeyframe != null) {
            xInput = firstKeyframe.x?.let { formatPosition(it) } ?: formatPosition(element.offset.x)
            yInput = firstKeyframe.y?.let { formatPosition(it) } ?: formatPosition(element.offset.y)
            scaleXInput = firstKeyframe.scaleX?.let { formatScale(it) } ?: formatScale(element.scaleX)
            scaleYInput = firstKeyframe.scaleY?.let { formatScale(it) } ?: formatScale(element.scaleY)
            rotationInput = firstKeyframe.rotation?.let { formatRotation(it) } ?: formatRotation(element.rotation)
            val displayColor = firstKeyframe.gradientConfig?.startColor ?: firstKeyframe.color ?: initialElementColor
            pickedColorArgb = displayColor.toArgb().toLong()
            currentGradientConfig = firstKeyframe.gradientConfig
        } else {
            xInput = element.offset.x.toString()
            yInput = element.offset.y.toString()
            scaleXInput = element.scaleX.toString()
            scaleYInput = element.scaleY.toString()
            rotationInput = element.rotation.toString()
            pickedColorArgb = initialElementColor.toArgb().toLong()
            currentGradientConfig = null
        }
    }

    @Composable
    fun KeyframeListCompact() {
        if (localKeyframes.isEmpty()) {
            Text("No keyframes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            return
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            localKeyframes.sortedBy { it.timestampMs }.forEach { kf ->
                val displayMs = storedToDisplay(kf.timestampMs)
                val isEditing = editingKeyframeTimestamp == kf.timestampMs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isEditing) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                        .clickable {
                            if (editingKeyframeTimestamp == kf.timestampMs) {
                                editingKeyframeTimestamp = null
                            } else {
                                editingKeyframeTimestamp = kf.timestampMs
                                populateFromKeyframe(kf)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        val indicatorColor = kf.gradientConfig?.startColor ?: kf.color
                        if (indicatorColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(indicatorColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            "${displayMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isEditing) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        buildString {
                            append("x:${kf.x?.toInt() ?: "—"} ")
                            append("y:${kf.y?.toInt() ?: "—"} ")
                            append("sx:${kf.scaleX?.let { "%.1f".format(it) } ?: "—"} ")
                            append("sy:${kf.scaleY?.let { "%.1f".format(it) } ?: "—"} ")
                            append("rot:${kf.rotation?.let { "%.0f°".format(it) } ?: "—"}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    IconButton(
                        onClick = {
                            if (editingKeyframeTimestamp == kf.timestampMs)
                                editingKeyframeTimestamp = null
                            localKeyframes.remove(kf)
                        },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ParameterSlider(
        label: String,
        valueText: String,
        onValueChange: (String) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        unit: String = "",
        format: (Float) -> String = { it.roundToInt().toString() }
    ) {
        val current = valueText.toFloatOrNull() ?: valueRange.start
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .width(40.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Slider(
                value = current.coerceIn(valueRange),
                onValueChange = { onValueChange(format(it)) },
                valueRange = valueRange,
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.1f)
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .shadow(2.dp, CircleShape)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                    )
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(70.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                BasicTextField(
                    value = valueText,
                    onValueChange = { newValue ->
                        if (newValue.all { c -> c.isDigit() || c == '.' || c == '-' }) onValueChange(newValue)
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (unit.isNotEmpty()) {
                    Text(unit, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(18.dp))
                } else {
                    Spacer(modifier = Modifier.width(18.dp))
                }
            }
        }
    }

    @Composable
    fun ColorSwatch(modifier: Modifier = Modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.padding(vertical = 2.dp)
        ) {
            Text("Color", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (currentGradientConfig != null) {
                            Modifier.background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        currentGradientConfig!!.startColor,
                                        currentGradientConfig!!.endColor
                                    ),
                                    start = currentGradientConfig!!.startOffset,
                                    end = currentGradientConfig!!.endOffset
                                )
                            )
                        } else {
                            Modifier.background(pickedColor)
                        }
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    .clickable { showColorDialog = true }
            )
            if (showColorDialog) {
                ColorWheelDialog(
                    onDismissRequest = { showColorDialog = false },
                    onColorSelected = { color ->
                        pickedColorArgb = color.toArgb().toLong()
                        currentGradientConfig = null
                        showColorDialog = false
                    },
                    initialColor = pickedColor,
                    enableGradient = true,
                    onGradientSelected = { startColor, endColor, startOffset, endOffset ->
                        currentGradientConfig = GradientConfig(startColor, endColor, startOffset, endOffset)
                        pickedColorArgb = startColor.toArgb().toLong()
                        showColorDialog = false
                    },
                    initialGradientConfig = currentGradientConfig
                )
            }
        }
    }

    @Composable
    fun InsertUpdateButton(modifier: Modifier = Modifier) {
        Button(
            onClick = {
                val rawDisplayMs = timeInput.toLongOrNull() ?: 0L
                val storedMs = displayToStored(rawDisplayMs)

                val x = xInput.toFloatOrNull() ?: element.offset.x
                val y = yInput.toFloatOrNull() ?: element.offset.y
                val sx = scaleXInput.toFloatOrNull() ?: element.scaleX
                val sy = scaleYInput.toFloatOrNull() ?: element.scaleY
                val rot = rotationInput.toFloatOrNull() ?: element.rotation

                if (editingKeyframeTimestamp != null) {
                    localKeyframes.removeAll { it.timestampMs == editingKeyframeTimestamp }
                    editingKeyframeTimestamp = null
                } else {
                    localKeyframes.removeAll { it.timestampMs == storedMs }
                }

                localKeyframes.add(
                    CanvasKeyframe(
                        timestampMs = storedMs,
                        x = x, y = y, scaleX = sx, scaleY = sy,
                        rotation = rot,
                        color = pickedColor,
                        gradientConfig = currentGradientConfig,
                        pivotX = pivotXState,
                        pivotY = pivotYState,
                        ellipticalRotation = ellipticalRotation,
                        ellipticalStretchX = ellipticalStretchX,
                        ellipticalStretchY = ellipticalStretchY,
                        shadowColor = element.shadowColor,
                        shadowOffsetX = element.shadowOffsetX,
                        shadowOffsetY = element.shadowOffsetY,
                        borderThickness = element.borderThickness,
                        borderColor = element.borderColor,
                        fontFamily = element.fontFamily,
                        textAlign = element.textAlign
                    )
                )

                if (storedMs > trimEndMs) {
                    trimEndMs = (storedMs + 200).coerceAtMost(universalDurationMs)
                }

                val bumpedMs = rawDisplayMs + 500L
                timeInput = bumpedMs.toString()
            },
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (editingKeyframeTimestamp != null)
                    MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                if (editingKeyframeTimestamp != null) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (editingKeyframeTimestamp != null) "Update Key" else "Insert Key",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }

    @Composable
    fun TimelineHeader() {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                @Composable
                fun TimeField(label: String, value: String, isPrimary: Boolean, onValueChange: (String) -> Unit) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 2.dp))
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 2.dp, vertical = 2.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            ),
                            singleLine = true
                        )
                    }
                }

                TimeField("Start", trimStartMs.toString(), true) { new ->
                    new.toLongOrNull()?.let { v ->
                        trimStartMs = v.coerceIn(0L, universalDurationMs).coerceAtMost(trimEndMs)
                    }
                }

                TimeField("Pos", timeInput, true) { new ->
                    if (new.all { it.isDigit() }) {
                        timeInput = new
                    }
                }

                TimeField("End", trimEndMs.toString(), true) { new ->
                    new.toLongOrNull()?.let { v ->
                        trimEndMs = v.coerceIn(0L, universalDurationMs).coerceAtLeast(trimStartMs)
                    }
                }

                TimeField("Total", universalDurationMs.toString(), false) { new ->
                    new.toLongOrNull()?.let { v ->
                        val newDuration = v.coerceAtLeast(1L)
                        handleUniversalDurationChange(newDuration)
                    }
                }
            }
        }
    }

    @Composable
    fun UnifiedTimelineTrack(
        elements: List<CanvasElement>,
        selectedElement: CanvasElement?,
        onElementSelected: (CanvasElement) -> Unit,
        universalDurationMs: Long,
        scrollState: ScrollState = rememberScrollState(),
        onExtendDuration: (Long) -> Unit = {}
    ) {
        val currentStoredMs = displayToStored(timeInput.toLongOrNull() ?: 0L)
        var isHandleDragging by remember { mutableStateOf(false) }
        var isBlockDragActive by remember { mutableStateOf(false) }

        val density = LocalDensity.current
        val primaryColor = MaterialTheme.colorScheme.primary
        val inactiveColor = MaterialTheme.colorScheme.surfaceVariant

        val trackHeightDp = 30.dp
        val trackHeightPx = with(density) { trackHeightDp.toPx() }
        val handleWidthDp = 20.dp
        val handleWidthPx = with(density) { handleWidthDp.toPx() }

        val trackPaddingPx = with(density) { 4.dp.toPx() }
        val hitHeightPx = with(density) { 10.dp.toPx() }

        val textMeasurer = rememberTextMeasurer()
        val secondsLineColor = Color.Gray.copy(alpha = 0.6f)
        val millisLineColor = Color.Gray.copy(alpha = 0.2f)
        val markerTextStyle = MaterialTheme.typography.labelSmall.copy(color = Color.Gray.copy(alpha = 0.7f))

        // Capture the latest duration so we can read it inside the gesture loop without causing
        // the entire pointerInput block to restart on every duration change.
        val currentDurationMs by rememberUpdatedState(universalDurationMs)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeightDp * elements.size.coerceAtLeast(1))
        ) {
            val viewportWidthPx = with(density) { maxWidth.toPx() }
            val scale = viewportWidthPx / MAX_VISIBLE_DURATION_MS.toFloat()
            val contentWidthPx = scale * universalDurationMs.coerceAtLeast(1L)
            val totalTrackWidth = contentWidthPx - 2 * handleWidthPx
            val safeTrackWidth = totalTrackWidth.coerceAtLeast(1f)
            val onSurface = MaterialTheme.colorScheme.onSurface

            LaunchedEffect(contentWidthPx) {
                val maxScroll = (contentWidthPx - viewportWidthPx).roundToInt().coerceAtLeast(0)
                if (scrollState.value > maxScroll) {
                    scrollState.scrollTo(maxScroll)
                    scrollOffsetSaved = maxScroll
                } else if (scrollOffsetSaved > maxScroll) {
                    scrollState.scrollTo(maxScroll)
                    scrollOffsetSaved = maxScroll
                }
            }

            LaunchedEffect(Unit) {
                scrollState.scrollTo(scrollOffsetSaved)
            }

            LaunchedEffect(Unit) {
                snapshotFlow { scrollState.value }
                    .collect { scrollOffsetSaved = it }
            }

            val playheadPx = handleWidthPx + (currentStoredMs.toFloat() / universalDurationMs.coerceAtLeast(1L)) * safeTrackWidth

            fun timeToPx(timeMs: Long): Float {
                return handleWidthPx + (timeMs.toFloat() / universalDurationMs.coerceAtLeast(1L)) * safeTrackWidth
            }

            fun pxToTime(px: Float): Long {
                val relativeX = (px - handleWidthPx).coerceIn(0f, safeTrackWidth)
                return ((relativeX / safeTrackWidth) * universalDurationMs).roundToLong().coerceIn(0L, universalDurationMs)
            }

            fun distanceToLineSegment(p: Offset, a: Offset, b: Offset): Float {
                val ab = b - a
                val ap = p - a
                val t = (ap.x * ab.x + ap.y * ab.y) / (ab.x * ab.x + ab.y * ab.y).coerceAtLeast(1f)
                val clampedT = t.coerceIn(0f, 1f)
                val closest = a + ab * clampedT
                return (p - closest).getDistance()
            }

            fun findTweenSegmentAt(position: Offset): Int? {
                val sel = selectedElement ?: return null
                val sortedKfs = localKeyframes.sortedBy { it.timestampMs }
                if (sortedKfs.size < 2) return null
                val selIndex = elements.indexOfFirst { it.id == sel.id }
                if (selIndex == -1) return null
                val trackTop = selIndex * trackHeightPx
                val cy = trackTop + (trackHeightPx - trackPaddingPx) / 2f
                val tapY = position.y
                if (tapY !in (cy - hitHeightPx)..(cy + hitHeightPx)) return null
                for (i in 0 until sortedKfs.size - 1) {
                    val x1 = timeToPx(sortedKfs[i].timestampMs)
                    val x2 = timeToPx(sortedKfs[i + 1].timestampMs)
                    val minX = minOf(x1, x2)
                    val maxX = maxOf(x1, x2)
                    if (position.x in minX..maxX) {
                        val dist = distanceToLineSegment(position, Offset(x1, cy), Offset(x2, cy))
                        if (dist <= hitHeightPx) return i
                    }
                }
                return null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState, enabled = !isHandleDragging && !isBlockDragActive)
                    .clipToBounds()
            ) {
                Box(
                    modifier = Modifier
                        .width(with(density) { contentWidthPx.toDp() })
                        .height(trackHeightDp * elements.size.coerceAtLeast(1))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedElement?.id, universalDurationMs) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val downPos = down.position
                                        val downTrackIndex = (downPos.y / trackHeightPx).toInt()
                                        val isDownOnSelectedTrack =
                                            downTrackIndex == elements.indexOfFirst { it.id == selectedElement?.id }

                                        val startPx = timeToPx(trimStartMs)
                                        val endPx = timeToPx(trimEndMs)
                                        val insideSelectedBlock =
                                            isDownOnSelectedTrack && (downPos.x in startPx..endPx)

                                        fun findKeyframeAt(position: Offset): CanvasKeyframe? {
                                            val sel = selectedElement ?: return null
                                            val selIndex = elements.indexOfFirst { it.id == sel.id }
                                            if (selIndex == -1) return null
                                            val trackTop = selIndex * trackHeightPx
                                            val centerY = trackTop + (trackHeightPx - trackPaddingPx) / 2f
                                            val hitRadius = with(density) { 12.dp.toPx() }
                                            return localKeyframes.minByOrNull {
                                                val kfPx = timeToPx(it.timestampMs)
                                                (Offset(kfPx, centerY) - position).getDistance()
                                                    .takeIf { d -> d <= hitRadius }
                                                    ?: Float.MAX_VALUE
                                            }?.takeIf {
                                                val kfPx = timeToPx(it.timestampMs)
                                                (Offset(kfPx, centerY) - position).getDistance() <= hitRadius
                                            }
                                        }

                                        val hitKeyframe = findKeyframeAt(downPos)
                                        val hitSegmentIndex = if (hitKeyframe == null) findTweenSegmentAt(downPos) else null

                                        var isTap = false
                                        var isDrag = false
                                        var isLongPress = false
                                        var lastPosition = downPos
                                        val touchSlop = viewConfiguration.touchSlop

                                        val timeoutResult = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Main)
                                                val change = event.changes.firstOrNull { it.id == down.id }
                                                if (change == null || !change.pressed) {
                                                    isTap = true
                                                    break
                                                }
                                                if ((change.position - downPos).getDistance() > touchSlop) {
                                                    isDrag = true
                                                    lastPosition = change.position
                                                    break
                                                }
                                            }
                                            true
                                        }

                                        if (timeoutResult == null) {
                                            isLongPress = true
                                        }

                                        if (isLongPress) {
                                            if (hitKeyframe != null) {
                                                // Handle Keyframe edit long press
                                                editingKeyframeTimestamp = hitKeyframe.timestampMs
                                                populateFromKeyframe(hitKeyframe)

                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    if (!event.changes.any { it.id == down.id && it.pressed }) break
                                                }
                                            } else if (insideSelectedBlock) {
                                                // Long press inside element block activates block drag
                                                isBlockDragActive = true
                                                var activeStartMs = trimStartMs
                                                var activeEndMs = trimEndMs
                                                val dragDuration = activeEndMs - activeStartMs

                                                val movedIndices = localKeyframes.indices.filter { i ->
                                                    val ts = localKeyframes[i].timestampMs
                                                    ts in activeStartMs..activeEndMs
                                                }
                                                val offsets = movedIndices.map { localKeyframes[it].timestampMs - activeStartMs }

                                                try {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val dragChange = event.changes.firstOrNull { it.id == down.id }
                                                        if (dragChange == null || !dragChange.pressed) break

                                                        dragChange.consume()
                                                        val currentPosition = dragChange.position
                                                        val dx = currentPosition.x - lastPosition.x
                                                        lastPosition = currentPosition

                                                        val timeDelta = ((dx / safeTrackWidth) * universalDurationMs).roundToLong()
                                                        if (timeDelta != 0L) {
                                                            var newStart = activeStartMs + timeDelta
                                                            var newEnd = activeEndMs + timeDelta
                                                            if (newStart < 0L) {
                                                                newStart = 0L
                                                                newEnd = dragDuration
                                                            }
                                                            activeStartMs = newStart
                                                            activeEndMs = newEnd
                                                            trimStartMs = newStart
                                                            trimEndMs = newEnd

                                                            for ((i, idx) in movedIndices.withIndex()) {
                                                                val newTimestamp = newStart + offsets[i]
                                                                localKeyframes[idx] = localKeyframes[idx].copy(timestampMs = newTimestamp)
                                                            }
                                                        }
                                                    }
                                                } finally {
                                                    isBlockDragActive = false
                                                    if (trimEndMs > currentDurationMs) {
                                                        onExtendDuration(trimEndMs)
                                                    }
                                                }
                                            } else {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    if (!event.changes.any { it.id == down.id && it.pressed }) break
                                                }
                                            }
                                        } else if (isTap) {
                                            if (hitSegmentIndex != null) {
                                                // Short tap on segment opens tween dialog
                                                editingSegmentIndex = hitSegmentIndex
                                            } else {
                                                // Short tap anywhere else moves playhead
                                                var newTime = pxToTime(downPos.x)
                                                if (isDownOnSelectedTrack) {
                                                    newTime = newTime.coerceIn(trimStartMs, trimEndMs)
                                                }
                                                timeInput = storedToDisplay(newTime).toString()
                                                if (downTrackIndex in elements.indices) {
                                                    onElementSelected(elements[downTrackIndex])
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        elements.forEachIndexed { index, trackElement ->
                            val isSelected = trackElement.id == selectedElement?.id
                            val yOffset = index * trackHeightPx
                            val trackRectHeight = trackHeightPx - trackPaddingPx
                            val trackEndY = yOffset + trackRectHeight

                            drawRect(
                                color = if (isSelected) inactiveColor else inactiveColor.copy(alpha = 0.3f),
                                topLeft = Offset(handleWidthPx, yOffset),
                                size = Size(safeTrackWidth, trackRectHeight)
                            )
                            drawRect(
                                color = Color(0xFF4CAF50).copy(alpha = 0.5f),
                                topLeft = Offset(0f, yOffset),
                                size = Size(handleWidthPx, trackRectHeight)
                            )
                            drawRect(
                                color = Color(0xFFF44336).copy(alpha = 0.5f),
                                topLeft = Offset(handleWidthPx + safeTrackWidth, yOffset),
                                size = Size(
                                    (contentWidthPx - (handleWidthPx + safeTrackWidth)).coerceAtLeast(0f),
                                    trackRectHeight
                                )
                            )

                            var secTime = 0L
                            val secondsTickHeight = 6.dp.toPx()
                            while (secTime <= universalDurationMs) {
                                val x = timeToPx(secTime)
                                if (x in handleWidthPx..(handleWidthPx + safeTrackWidth)) {
                                    drawLine(
                                        color = secondsLineColor,
                                        start = Offset(x, yOffset),
                                        end = Offset(x, yOffset + secondsTickHeight),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                    val label = "${secTime / 1000}s"
                                    val textLayoutResult = textMeasurer.measure(text = label, style = markerTextStyle)
                                    val textX = x - textLayoutResult.size.width / 2f
                                    val textY = trackEndY - textLayoutResult.size.height - 2.dp.toPx()
                                    drawText(textLayoutResult = textLayoutResult, topLeft = Offset(textX, textY))
                                }
                                secTime += 1000L
                            }

                            val millisStep = 200L
                            val millisTickHeight = 4.dp.toPx()
                            var millisTime = 0L
                            while (millisTime <= universalDurationMs) {
                                if (millisTime % 1000L != 0L) {
                                    val x = timeToPx(millisTime)
                                    if (x in handleWidthPx..(handleWidthPx + safeTrackWidth)) {
                                        drawLine(
                                            color = millisLineColor,
                                            start = Offset(x, yOffset),
                                            end = Offset(x, yOffset + millisTickHeight),
                                            strokeWidth = 0.5.dp.toPx()
                                        )
                                    }
                                }
                                millisTime += millisStep
                            }

                            val startPx = timeToPx(if (isSelected) trimStartMs else trackElement.startTimeMs)
                            val rawEnd = if (trackElement.endTimeMs == Long.MAX_VALUE) 5000L else trackElement.endTimeMs
                            val endPx = timeToPx(if (isSelected) trimEndMs else rawEnd)

                            drawRect(
                                color = primaryColor.copy(alpha = if (isSelected) 0.6f else 0.2f),
                                topLeft = Offset(startPx, yOffset),
                                size = Size(maxOf(0f, endPx - startPx), trackRectHeight)
                            )

                            val keyframesToDraw =
                                if (isSelected) localKeyframes else trackElement.keyframes
                            keyframesToDraw.forEach { kf ->
                                val kfPx = timeToPx(kf.timestampMs)
                                if (kfPx in handleWidthPx..(handleWidthPx + safeTrackWidth)) {
                                    val indicatorColor =
                                        kf.gradientConfig?.startColor ?: kf.color ?: Color.White
                                    val cy = yOffset + (trackRectHeight / 2)
                                    val r = 6.dp.toPx()

                                    val diamondPath = Path().apply {
                                        moveTo(kfPx, cy - r)
                                        lineTo(kfPx + r, cy)
                                        lineTo(kfPx, cy + r)
                                        lineTo(kfPx - r, cy)
                                        close()
                                    }
                                    drawPath(path = diamondPath, color = indicatorColor)
                                    drawPath(
                                        path = diamondPath,
                                        color = Color.Black.copy(alpha = 0.4f),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                            }

                            if (isSelected) {
                                val sortedKfs = localKeyframes.sortedBy { it.timestampMs }
                                if (sortedKfs.size >= 2) {
                                    for (i in 0 until sortedKfs.size - 1) {
                                        val kfA = sortedKfs[i]
                                        val kfB = sortedKfs[i + 1]
                                        val startLinePx = timeToPx(kfA.timestampMs)
                                        val endLinePx = timeToPx(kfB.timestampMs)
                                        val cy = yOffset + (trackRectHeight / 2)
                                        val pathEffect = when (kfB.tweenType) {
                                            TweenType.LINEAR -> null
                                            TweenType.EASE_IN -> PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                                            TweenType.EASE_OUT -> PathEffect.dashPathEffect(floatArrayOf(20f, 5f, 5f, 5f), 0f)
                                            TweenType.EASE_IN_OUT -> PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                            TweenType.CUSTOM -> PathEffect.dashPathEffect(floatArrayOf(5f, 10f, 20f, 10f), 0f)
                                        }
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.8f),
                                            start = Offset(startLinePx, cy),
                                            end = Offset(endLinePx, cy),
                                            strokeWidth = 2.dp.toPx(),
                                            pathEffect = pathEffect
                                        )
                                    }
                                }
                            }
                        }

                        val selectedTrackIndex = elements.indexOfFirst { it.id == selectedElement?.id }
                        if (selectedTrackIndex >= 0) {
                            val trackTop = selectedTrackIndex * trackHeightPx
                            val trackBottom = trackTop + trackHeightPx - trackPaddingPx
                            drawLine(
                                color = onSurface,
                                start = Offset(playheadPx, trackTop),
                                end = Offset(playheadPx, trackBottom),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }

                val selectedIndex = elements.indexOfFirst { it.id == selectedElement?.id }
                if (selectedIndex >= 0) {
                    val yOffsetDp = trackHeightDp * selectedIndex

                    @Composable
                    fun TrimHandle(
                        isLeft: Boolean,
                        basePx: Float,
                        dragLimit: ClosedFloatingPointRange<Float>,
                        onPositionChange: (Float) -> Unit,
                        onDragStateChanged: (Boolean) -> Unit
                    ) {
                        var tempDragPx by remember { mutableFloatStateOf(basePx) }
                        var isDragging by remember { mutableStateOf(false) }

                        val currentDragLimit by rememberUpdatedState(dragLimit)
                        val currentOnPositionChange by rememberUpdatedState(onPositionChange)

                        LaunchedEffect(basePx) {
                            if (!isDragging) tempDragPx = basePx
                        }

                        LaunchedEffect(isDragging) {
                            onDragStateChanged(isDragging)
                        }

                        val xOffset = if (isLeft) tempDragPx - handleWidthPx else tempDragPx

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(xOffset.roundToInt(), yOffsetDp.roundToPx()) }
                                .height(trackHeightDp * 0.87f)
                                .width(handleWidthDp)
                                .background(
                                    color = primaryColor,
                                    shape = if (isLeft)
                                        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                                    else
                                        RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                                )
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { isDragging = true },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            tempDragPx = (tempDragPx + dragAmount).coerceIn(currentDragLimit)
                                            currentOnPositionChange(tempDragPx)
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                        },
                                        onDragCancel = { isDragging = false }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.6f)))
                                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.6f)))
                            }
                        }
                    }

                    val startPxBase = timeToPx(trimStartMs)
                    val endPxBase = timeToPx(trimEndMs)

                    TrimHandle(
                        isLeft = true,
                        basePx = startPxBase,
                        dragLimit = handleWidthPx..endPxBase,
                        onPositionChange = { newPx ->
                            trimStartMs = pxToTime(newPx).coerceAtMost(trimEndMs)
                        },
                        onDragStateChanged = { isHandleDragging = it }
                    )

                    TrimHandle(
                        isLeft = false,
                        basePx = endPxBase,
                        dragLimit = startPxBase..(handleWidthPx + safeTrackWidth),
                        onPositionChange = { newPx ->
                            trimEndMs = pxToTime(newPx).coerceAtLeast(trimStartMs)
                        },
                        onDragStateChanged = { isHandleDragging = it }
                    )
                }
            }

            if (editingSegmentIndex != null) {
                val sortedKfs = localKeyframes.sortedBy { it.timestampMs }
                val laterKf = sortedKfs[editingSegmentIndex!! + 1]

                var selectedType by remember(editingSegmentIndex) { mutableStateOf(laterKf.tweenType) }
                var customPoints by remember(editingSegmentIndex) {
                    mutableStateOf(
                        laterKf.customPoints ?: listOf(EasingPoint(x = 0f, y = 0f), EasingPoint(x = 1f, y = 1f))
                    )
                }

                val landscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

                AlertDialog(
                    onDismissRequest = { editingSegmentIndex = null },
                    modifier = Modifier.fillMaxWidth(if (landscape) 0.9f else 1f),
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    title = { Text("Choose Tween Type") },
                    text = {
                        if (landscape) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    TweenType.entries.forEach { type ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedType = type }
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedType == type,
                                                onClick = { selectedType = type }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                type.name.replace("_", " ")
                                                    .lowercase()
                                                    .replaceFirstChar { it.uppercase() }
                                            )
                                        }
                                    }
                                }
                                if (selectedType == TweenType.CUSTOM) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Tap to add point • Long-press for smooth/sharp",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        CustomEasingEditor(
                                            points = customPoints,
                                            onPointsChanged = { customPoints = it }
                                        )
                                    }
                                }
                            }
                        } else {
                            Column {
                                TweenType.entries.forEach { type ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedType = type }
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedType == type,
                                            onClick = { selectedType = type }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            type.name.replace("_", " ")
                                                .lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                        )
                                    }
                                }

                                if (selectedType == TweenType.CUSTOM) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Tap to add point • Long-press for smooth/sharp",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CustomEasingEditor(
                                        points = customPoints,
                                        onPointsChanged = { customPoints = it }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val updated = laterKf.copy(
                                tweenType = selectedType,
                                customPoints = if (selectedType == TweenType.CUSTOM) customPoints else null
                            )
                            val idx = localKeyframes.indexOf(laterKf)
                            if (idx != -1) localKeyframes[idx] = updated
                            editingSegmentIndex = null
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingSegmentIndex = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    val portraitScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        TimelineHeader()
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .imePadding()
                .verticalScroll(portraitScrollState),
        ) {
            UnifiedTimelineTrack(
                elements = allElements,
                selectedElement = element,
                onElementSelected = autoSaveAndSelect,
                universalDurationMs = universalDurationMs,
                scrollState = timelineScrollState,
                onExtendDuration = { newDuration -> handleUniversalDurationChange(newDuration)  }
            )
            Text(
                if (editingKeyframeTimestamp != null) "Editing Keyframe" else "New Keyframe",
                style = MaterialTheme.typography.labelMedium
            )
            ParameterSlider("X", xInput, { xInput = it }, xMin..xMax, "px")
            ParameterSlider("Y", yInput, { yInput = it }, yMin..yMax, "px")
            ParameterSlider("SX", scaleXInput, { scaleXInput = it }, 0.05f..25f, "",
                { String.format(Locale.US, "%.2f", it) })
            ParameterSlider("SY", scaleYInput, { scaleYInput = it }, 0.05f..25f, "",
                { String.format(Locale.US, "%.2f", it) })
            ParameterSlider("Rot", rotationInput, { rotationInput = it }, -360f..360f, "°",
                { String.format(Locale.US, "%.1f", it) })
            EllipticalRotationSection(
                isEnabled = ellipticalRotation,
                onToggle = { ellipticalRotation = it },
                stretchX = ellipticalStretchX,
                onStretchXChange = { ellipticalStretchX = it.coerceIn(0.1f, 10f) },
                stretchY = ellipticalStretchY,
                onStretchYChange = { ellipticalStretchY = it.coerceIn(0.1f, 10f) }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                ColorSwatch(modifier = Modifier.weight(1f))
                InsertUpdateButton(modifier = Modifier.height(30.dp))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Keyframes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            KeyframeListCompact()
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    onSave(
                        element.id,
                        localKeyframes.sortedBy { it.timestampMs },
                        trimStartMs,
                        trimEndMs
                    )
                }) {
                    Text("Save", color = Color.White)
                }
            }
        }
    }
}