package com.fountofhopedotorg.fohbible.gfx_animator

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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fountofhopedotorg.fohbible.color_wheel.ColorWheelDialog
import com.fountofhopedotorg.fohbible.data.CanvasKeyframe
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.GradientConfig
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import java.util.Locale

private val GradientConfigNullableSaver = Saver<GradientConfig?, String>(
    save = { config ->
        if (config != null) {
            listOf(
                config.startColor.toArgb(),
                config.endColor.toArgb(),
                config.startOffset.x,
                config.startOffset.y,
                config.endOffset.x,
                config.endOffset.y
            ).joinToString(",")
        } else {
            "NULL"
        }
    },
    restore = { str ->
        if (str == "NULL") null
        else {
            val parts = str.split(",").map { it.toFloatOrNull() ?: 0f }
            if (parts.size < 6) null
            else GradientConfig(
                startColor = Color(parts[0].toInt()),
                endColor = Color(parts[1].toInt()),
                startOffset = Offset(parts[2], parts[3]),
                endOffset = Offset(parts[4], parts[5])
            )
        }
    }
)

private const val MAX_VISIBLE_DURATION_MS = 10_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KeyframeAnimationDialog(
    element: CanvasElement?,
    allElements: List<CanvasElement>,
    onElementSelected: (CanvasElement) -> Unit,
    onDismiss: () -> Unit,
    onSaveKeyframes: (String, List<CanvasKeyframe>, Long, Long) -> Unit,
    timeMultiplier: Float,
    initialGradientConfig: GradientConfig? = null
) {

    var scrollOffsetSaved by rememberSaveable("timelineScroll") { mutableIntStateOf(0) }

    if (element == null) return
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

    var trimStartMs by rememberSaveable(element.id) { mutableLongStateOf(element.startTimeMs) }
    var trimEndMs   by rememberSaveable(element.id) {
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

    var xInput by rememberSaveable(element.id) {
        mutableStateOf(element.offset.x.toString())
    }
    var yInput by rememberSaveable(element.id) {
        mutableStateOf(element.offset.y.toString())
    }
    var scaleXInput by rememberSaveable(element.id) {
        mutableStateOf(element.scaleX.toString())
    }
    var scaleYInput by rememberSaveable(element.id) {
        mutableStateOf(element.scaleY.toString())
    }
    var rotationInput by rememberSaveable(element.id) {
        mutableStateOf(element.rotation.toString())
    }

    var pickedColorArgb by rememberSaveable(element.id) {
        mutableLongStateOf(initialElementColor.toArgb().toLong())
    }

    var showColorDialog by rememberSaveable(element.id) {
        mutableStateOf(false)
    }

    val pickedColor = Color(pickedColorArgb.toInt())

    val currentElement by rememberUpdatedState(element)
    val currentTrimStartMs by rememberUpdatedState(trimStartMs)
    val currentTrimEndMs by rememberUpdatedState(trimEndMs)
    val currentLocalKeyframes by rememberUpdatedState(localKeyframes.toList())

    val autoSaveAndSelect: (CanvasElement) -> Unit = remember {
        { newElement ->
            val oldElement = currentElement
            if (newElement.id != oldElement.id) {
                onSaveKeyframes(
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
        xInput = kf.x?.toString() ?: element.offset.x.toString()
        yInput = kf.y?.toString() ?: element.offset.y.toString()
        scaleXInput = kf.scaleX?.toString() ?: element.scaleX.toString()
        scaleYInput = kf.scaleY?.toString() ?: element.scaleY.toString()
        rotationInput = kf.rotation?.toString() ?: element.rotation.toString()
        val displayColor = kf.gradientConfig?.startColor ?: kf.color ?: initialElementColor
        pickedColorArgb = displayColor.toArgb().toLong()
        currentGradientConfig = kf.gradientConfig
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
                    gradientConfig = null
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
        val firstKeyframe = localKeyframes.minByOrNull { it.timestampMs }
        if (firstKeyframe != null) {
            populateFromKeyframe(firstKeyframe)
        } else {
            timeInput = storedToDisplay(
                (trimStartMs + (trimEndMs - trimStartMs) / 2)
            ).toString()
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
                        gradientConfig = currentGradientConfig
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

    // ================== Fixed Header ==================
    @Composable
    fun TimelineHeader() {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Timeline Animation",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
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

    // ================== Timeline Track (no header) ==================
    @Composable
    fun UnifiedTimelineTrack(
        elements: List<CanvasElement>,
        selectedElement: CanvasElement?,
        onElementSelected: (CanvasElement) -> Unit,
        universalDurationMs: Long,
        scrollState: ScrollState = rememberScrollState()
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

        val textMeasurer = rememberTextMeasurer()
        val secondsLineColor = Color.Gray.copy(alpha = 0.6f)
        val millisLineColor = Color.Gray.copy(alpha = 0.2f)
        val markerTextStyle = MaterialTheme.typography.labelSmall.copy(color = Color.Gray.copy(alpha = 0.7f))

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

                                        // Block bounds for drag detection
                                        val startPx = timeToPx(trimStartMs)
                                        val endPx = timeToPx(trimEndMs)
                                        val insideSelectedBlock =
                                            isDownOnSelectedTrack && (downPos.x in startPx..endPx)

                                        // Hit‑test diamonds of the currently selected element
                                        fun findKeyframeAt(position: Offset): CanvasKeyframe? {
                                            val sel = selectedElement ?: return null
                                            val selIndex = elements.indexOfFirst { it.id == sel.id }
                                            if (selIndex == -1) return null
                                            val trackTop = selIndex * trackHeightPx
                                            val centerY = trackTop + (trackHeightPx - 4.dp.toPx()) / 2f
                                            val hitRadius = 12.dp.toPx()
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

                                        var longPressHandled = false
                                        var isTap = true
                                        var dragTriggered = false
                                        var lastPosition = downPos
                                        val touchSlop = viewConfiguration.touchSlop

                                        var initialEvent: PointerEvent? = null   // event that arrived before timeout

                                        // If we hit a diamond, wait for long‑press timeout OR first pointer event
                                        if (hitKeyframe != null) {
                                            val timeoutResult =
                                                withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                                    awaitPointerEvent(PointerEventPass.Main)
                                                }
                                            if (timeoutResult == null) {
                                                // No event during timeout → long press
                                                longPressHandled = true
                                                editingKeyframeTimestamp = hitKeyframe.timestampMs
                                                populateFromKeyframe(hitKeyframe)

                                                // Consume the rest of this gesture until the finger is lifted
                                                while (true) {
                                                    val upEvent = awaitPointerEvent()
                                                    if (!upEvent.changes.any { it.id == down.id && it.pressed }) break
                                                }
                                                continue  // go to next down event in the outer loop
                                            } else {
                                                // An event arrived before timeout → normal drag/tap handling
                                                initialEvent = timeoutResult
                                            }
                                        }

                                        // Process the initial event (if any) and then continue waiting for more
                                        while (true) {
                                            val event = if (initialEvent != null) {
                                                val e = initialEvent
                                                initialEvent = null
                                                e
                                            } else {
                                                awaitPointerEvent()
                                            }

                                            val change = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null || !change.pressed) {
                                                // Finger lifted → tap
                                                break
                                            }

                                            val currentPos = change.position
                                            val dragDistance = (currentPos - downPos).getDistance()

                                            if (dragDistance > touchSlop) {
                                                // Drag started
                                                isTap = false
                                                val dx = currentPos.x - downPos.x
                                                val dy = currentPos.y - downPos.y
                                                if (insideSelectedBlock && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                                    dragTriggered = true
                                                    change.consume()
                                                    lastPosition = currentPos
                                                }
                                                // If not a block drag we simply keep waiting for the up
                                            }

                                            if (dragTriggered) {
                                                // Exit the tap‑waiting loop and enter the dedicated drag‑handling loop
                                                break
                                            }
                                        }

                                        // ----- Block drag handling (unchanged) -----
                                        if (dragTriggered) {
                                            isBlockDragActive = true
                                            var activeStartMs = trimStartMs
                                            var activeEndMs = trimEndMs
                                            val dragDuration = activeEndMs - activeStartMs

                                            try {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val dragChange = event.changes.firstOrNull { it.id == down.id }
                                                    if (dragChange == null || !dragChange.pressed) break

                                                    dragChange.consume()
                                                    val currentPosition = dragChange.position
                                                    val dx = currentPosition.x - lastPosition.x
                                                    lastPosition = currentPosition

                                                    val timeDelta =
                                                        ((dx / safeTrackWidth) * universalDurationMs).roundToLong()
                                                    if (timeDelta != 0L) {
                                                        var newStart = activeStartMs + timeDelta
                                                        var newEnd = activeEndMs + timeDelta
                                                        if (newStart < 0L) {
                                                            newStart = 0L
                                                            newEnd = dragDuration
                                                        } else if (newEnd > universalDurationMs) {
                                                            newEnd = universalDurationMs
                                                            newStart = universalDurationMs - dragDuration
                                                        }
                                                        activeStartMs = newStart
                                                        activeEndMs = newEnd
                                                        trimStartMs = newStart
                                                        trimEndMs = newEnd
                                                    }
                                                }
                                            } finally {
                                                isBlockDragActive = false
                                            }
                                        }

                                        // ----- Tap handling (only if no long press was performed) -----
                                        if (isTap) {
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
                    ) {
                        elements.forEachIndexed { index, trackElement ->
                            val isSelected = trackElement.id == selectedElement?.id
                            val yOffset = index * trackHeightPx
                            val trackRectHeight = trackHeightPx - 4.dp.toPx()
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
                                    val textLayoutResult = textMeasurer.measure(
                                        text = label,
                                        style = markerTextStyle
                                    )
                                    val textX = x - textLayoutResult.size.width / 2f
                                    val textY = trackEndY - textLayoutResult.size.height - 2.dp.toPx()
                                    drawText(
                                        textLayoutResult = textLayoutResult,
                                        topLeft = Offset(textX, textY)
                                    )
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
                        }

                        val selectedTrackIndex =
                            elements.indexOfFirst { it.id == selectedElement?.id }
                        if (selectedTrackIndex >= 0) {
                            val trackTop = selectedTrackIndex * trackHeightPx
                            val trackBottom = trackTop + trackHeightPx - 4.dp.toPx()
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
                                            tempDragPx =
                                                (tempDragPx + dragAmount).coerceIn(dragLimit)
                                        },
                                        onDragEnd = {
                                            onPositionChange(tempDragPx)
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
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        title = {},
        text = {
            val portraitScrollState = rememberScrollState()
            val leftColumnScrollState = rememberScrollState()
            val rightColumnScrollState = rememberScrollState()

            Column(modifier = Modifier.fillMaxSize()) {
                TimelineHeader()
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .imePadding(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.4f)
                                .verticalScroll(rightColumnScrollState),
                        ) {
                            ParameterSlider("X", xInput, { xInput = it }, -2000f..2000f, "px")
                            ParameterSlider("Y", yInput, { yInput = it }, -2000f..2000f, "px")
                            ParameterSlider("SX", scaleXInput, { scaleXInput = it }, 0.1f..10f, "",
                                { String.format(Locale.US, "%.2f", it) })
                            ParameterSlider("SY", scaleYInput, { scaleYInput = it }, 0.1f..10f, "",
                                { String.format(Locale.US, "%.2f", it) })
                            ParameterSlider("Rot", rotationInput, { rotationInput = it }, -360f..360f, "°",
                                { String.format(Locale.US, "%.1f", it) })

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                ColorSwatch(modifier = Modifier.weight(1f))
                                InsertUpdateButton(modifier = Modifier.height(33.dp))
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(0.6f)
                                .verticalScroll(leftColumnScrollState),
                        ) {
                            UnifiedTimelineTrack(
                                elements = allElements,
                                selectedElement = element,
                                onElementSelected = autoSaveAndSelect,
                                universalDurationMs = universalDurationMs,
                                scrollState = timelineScrollState
                            )
                            Text("Keyframes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 2.dp))
                            KeyframeListCompact()
                        }
                    }
                }  else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .imePadding()
                    ) {
                        Text(
                            if (editingKeyframeTimestamp != null) "Editing Keyframe" else "New Keyframe",
                            style = MaterialTheme.typography.labelMedium
                        )
                        ParameterSlider("X", xInput, { xInput = it }, -2000f..2000f, "px")
                        ParameterSlider("Y", yInput, { yInput = it }, -2000f..2000f, "px")
                        ParameterSlider("SX", scaleXInput, { scaleXInput = it }, 0.1f..10f, "",
                            { String.format(Locale.US, "%.2f", it) })
                        ParameterSlider("SY", scaleYInput, { scaleYInput = it }, 0.1f..10f, "",
                            { String.format(Locale.US, "%.2f", it) })
                        ParameterSlider("Rot", rotationInput, { rotationInput = it }, -360f..360f, "°",
                            { String.format(Locale.US, "%.1f", it) })

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            ColorSwatch(modifier = Modifier.weight(1f))
                            InsertUpdateButton(modifier = Modifier.height(30.dp))
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(portraitScrollState),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            UnifiedTimelineTrack(
                                elements = allElements,
                                selectedElement = element,
                                onElementSelected = autoSaveAndSelect,
                                universalDurationMs = universalDurationMs,
                                scrollState = timelineScrollState
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Keyframes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                            KeyframeListCompact()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSaveKeyframes(
                    element.id,
                    localKeyframes.sortedBy { it.timestampMs },
                    trimStartMs,
                    trimEndMs
                )
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}