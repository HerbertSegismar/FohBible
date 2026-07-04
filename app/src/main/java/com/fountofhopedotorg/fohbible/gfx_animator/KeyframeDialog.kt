package com.fountofhopedotorg.fohbible.gfx_animator

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.*
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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalConfiguration

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KeyframeAnimationDialog(
    element: CanvasElement?,
    onDismiss: () -> Unit,
    onSaveKeyframes: (String, List<CanvasKeyframe>, Long, Long) -> Unit,
    timeMultiplier: Float,
    initialGradientConfig: GradientConfig? = null
) {
    if (element == null) return

    val localKeyframes = remember(element.keyframes) { mutableStateListOf(*element.keyframes.toTypedArray()) }

    var trimStartMs by rememberSaveable { mutableLongStateOf(element.startTimeMs) }
    var trimEndMs by rememberSaveable {
        mutableLongStateOf(if (element.endTimeMs == Long.MAX_VALUE) 5_000L else element.endTimeMs)
    }

    var timeInput by rememberSaveable {
        val initialCenterMs = trimStartMs + (trimEndMs - trimStartMs) / 2
        val initialDisplay = (initialCenterMs / timeMultiplier).roundToInt().toLong()
        mutableStateOf(initialDisplay.toString())
    }

    var xInput by rememberSaveable { mutableStateOf(element.offset.x.toString()) }
    var yInput by rememberSaveable { mutableStateOf(element.offset.y.toString()) }
    var scaleXInput by rememberSaveable { mutableStateOf(element.scaleX.toString()) }
    var scaleYInput by rememberSaveable { mutableStateOf(element.scaleY.toString()) }
    var rotationInput by rememberSaveable { mutableStateOf(element.rotation.toString()) }

    val initialElementColor: Color = remember(element) {
        if (element.content.startsWith("Shape:") || element.content.startsWith("Image:")) element.backgroundColor
        else element.textColor ?: Color.Black
    }
    var pickedColorArgb by rememberSaveable(initialElementColor) { mutableLongStateOf(initialElementColor.toArgb().toLong()) }

    var currentGradientConfig by rememberSaveable(
        initialGradientConfig,
        stateSaver = GradientConfigNullableSaver
    ) { mutableStateOf(initialGradientConfig) }

    var showColorDialog by rememberSaveable { mutableStateOf(false) }

    val pickedColor = Color(pickedColorArgb.toInt())
    var editingKeyframeTimestamp by rememberSaveable { mutableStateOf<Long?>(null) }


    fun storedToDisplay(storedMs: Long): Long = (storedMs / timeMultiplier).roundToInt().toLong()
    fun displayToStored(displayMs: Long): Long = (displayMs * timeMultiplier).roundToInt().toLong()

    val maxKeyframeStored = localKeyframes.maxOfOrNull { it.timestampMs } ?: 0L
    val trimLimitLow = 0L
    val trimLimitHigh = maxKeyframeStored + 10_000L

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

    LaunchedEffect(Unit) {
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

    LaunchedEffect(initialGradientConfig, initialElementColor) {
        val firstKf = localKeyframes.minByOrNull { it.timestampMs }
        if (firstKf != null) {
            val index = localKeyframes.indexOf(firstKf)
            localKeyframes[index] = firstKf.copy(
                color = initialGradientConfig?.startColor ?: initialElementColor,
                gradientConfig = initialGradientConfig
            )
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
                .heightIn(max = 120.dp)
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
                    .height(36.dp),
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
                            .border(1.5.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                    )
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(70.dp)
                    .height(36.dp)
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
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(18.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(18.dp))
                }
            }
        }
    }

    @Composable
    fun ColorSwatch() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
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
                    trimEndMs = storedMs + 200
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
    fun UnifiedTimelineTrimmer() {
        val currentStoredMs = displayToStored(timeInput.toLongOrNull() ?: 0L)

        var trackMax by remember {
            mutableFloatStateOf(
                maxOf(
                    trimLimitHigh.toFloat(),
                    trimEndMs.toFloat() + 2000f,
                    currentStoredMs.toFloat() + 2000f
                )
            )
        }

        LaunchedEffect(currentStoredMs, trimEndMs) {
            if (currentStoredMs > trackMax) trackMax = currentStoredMs.toFloat() + 2000f
            if (trimEndMs > trackMax) trackMax = trimEndMs.toFloat() + 2000f
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                "Timeline & Trim",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                @Composable
                fun TimeField(label: String, value: String, isPrimary: Boolean, onValueChange: (String) -> Unit) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 4.dp))
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true
                        )
                    }
                }

                TimeField("Start", trimStartMs.toString(), false) { new ->
                    new.toLongOrNull()?.let { v ->
                        if (v > trackMax) trackMax = v.toFloat() + 2000f
                        trimStartMs = v.coerceIn(trimLimitLow, trackMax.toLong()).coerceAtMost(trimEndMs)
                    }
                }

                TimeField("Time", timeInput, true) { new ->
                    if (new.all { it.isDigit() }) {
                        timeInput = new
                        new.toLongOrNull()?.let { v ->
                            val storedV = displayToStored(v)
                            if (storedV > trackMax) trackMax = storedV.toFloat() + 2000f
                        }
                    }
                }

                TimeField("End", trimEndMs.toString(), false) { new ->
                    new.toLongOrNull()?.let { v ->
                        if (v > trackMax) trackMax = v.toFloat() + 2000f
                        trimEndMs = v.coerceIn(trimLimitLow, trackMax.toLong()).coerceAtLeast(trimStartMs)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // The Custom 3-Thumb unified timeline
            val density = androidx.compose.ui.platform.LocalDensity.current
            val primaryColor = MaterialTheme.colorScheme.primary
            val inactiveColor = MaterialTheme.colorScheme.surfaceVariant

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val widthPx = constraints.maxWidth.toFloat()
                val thumbRadiusPx = with(density) { 11.dp.toPx() }
                val usableWidth = (widthPx - (thumbRadiusPx * 2)).coerceAtLeast(1f)
                val safeTrackMax = trackMax.toLong().coerceAtLeast(1L)

                fun timeToPx(time: Long): Float {
                    return (time.toFloat() / safeTrackMax) * usableWidth
                }

                fun pxToTime(px: Float): Long {
                    return ((px / usableWidth) * safeTrackMax).roundToLong().coerceIn(0L, safeTrackMax)
                }

                val startPxBase = timeToPx(trimStartMs)
                val endPxBase = timeToPx(trimEndMs)
                val currentPxBase = timeToPx(currentStoredMs)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val trackY = size.height / 2
                    val trackEnd = size.width - thumbRadiusPx

                    drawLine(
                        color = inactiveColor,
                        start = Offset(thumbRadiusPx, trackY),
                        end = Offset(trackEnd, trackY),
                        strokeWidth = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset(thumbRadiusPx + startPxBase, trackY),
                        end = Offset(thumbRadiusPx + endPxBase, trackY),
                        strokeWidth = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // Draw keyframe indicators
                    localKeyframes.forEach { kf ->
                        val kfPx = timeToPx(kf.timestampMs)
                        if (kfPx in 0f..usableWidth) {
                            val indicatorColor = kf.gradientConfig?.startColor ?: kf.color ?: Color.White

                            // Fill
                            drawCircle(
                                color = indicatorColor,
                                radius = 4.dp.toPx(),
                                center = Offset(thumbRadiusPx + kfPx, trackY)
                            )
                            // Stroke
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.4f),
                                radius = 4.dp.toPx(),
                                center = Offset(thumbRadiusPx + kfPx, trackY),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }

                @Composable
                fun DraggableThumb(
                    basePx: Float,
                    isPlayhead: Boolean,
                    onPositionChange: (Float) -> Unit
                ) {
                    var tempDragPx by remember { mutableFloatStateOf(basePx) }
                    var isDragging by remember { mutableStateOf(false) }

                    LaunchedEffect(basePx) {
                        if (!isDragging) {
                            tempDragPx = basePx
                        }
                    }

                    val sizeDp = if (isPlayhead) 22.dp else 16.dp

                    Box(
                        modifier = Modifier
                            .offset {
                                androidx.compose.ui.unit.IntOffset(
                                    x = (thumbRadiusPx + tempDragPx - with(density) { sizeDp.toPx() } / 2).roundToInt(),
                                    y = 0
                                )
                            }
                            .size(sizeDp)
                            .shadow(if (isPlayhead) 4.dp else 2.dp, CircleShape)
                            .background(if (isPlayhead) primaryColor else Color.White, CircleShape)
                            .border(
                                width = if (isPlayhead) 2.dp else 1.dp,
                                color = if (isPlayhead) MaterialTheme.colorScheme.onPrimary else primaryColor,
                                shape = CircleShape
                            )
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onHorizontalDrag = { change: PointerInputChange, dragAmount: Float ->
                                        change.consume()
                                        tempDragPx = (tempDragPx + dragAmount).coerceIn(0f, usableWidth)
                                    },
                                    onDragEnd = {
                                        onPositionChange(tempDragPx)
                                        isDragging = false
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                    }
                                )
                            }
                    )
                }

                DraggableThumb(basePx = startPxBase, isPlayhead = false) { newPx ->
                    trimStartMs = pxToTime(newPx).coerceAtMost(trimEndMs)
                }

                DraggableThumb(basePx = endPxBase, isPlayhead = false) { newPx ->
                    trimEndMs = pxToTime(newPx).coerceAtLeast(trimStartMs)
                }

                DraggableThumb(basePx = currentPxBase, isPlayhead = true) { newPx ->
                    val newStoredTime = pxToTime(newPx)
                    timeInput = storedToDisplay(newStoredTime).toString()
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE

    @Composable
    fun EditorContent(includeKeyframeList: Boolean = true) {
        if (includeKeyframeList) {
            Text("Keyframes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            KeyframeListCompact()
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }

        Text(
            if (editingKeyframeTimestamp != null) "Editing Keyframe" else "New Keyframe",
            style = MaterialTheme.typography.labelMedium
        )

        ParameterSlider("X", xInput, { xInput = it }, -2000f..2000f, "px")
        ParameterSlider("Y", yInput, { yInput = it }, -2000f..2000f, "px")
        ParameterSlider("SX", scaleXInput, { scaleXInput = it }, 0.1f..10f, "",
            { String.format(java.util.Locale.US, "%.2f", it) })
        ParameterSlider("SY", scaleYInput, { scaleYInput = it }, 0.1f..10f, "",
            { String.format(java.util.Locale.US, "%.2f", it) })
        ParameterSlider("Rot", rotationInput, { rotationInput = it }, -360f..360f, "°",
            { String.format(java.util.Locale.US, "%.1f", it) })

        ColorSwatch()

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            InsertUpdateButton()
        }

        UnifiedTimelineTrimmer()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxSize(0.95f)
            .padding(4.dp),
        title = {
            Text("Keyframe Editor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
        text = {
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Keyframes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                        KeyframeListCompact()
                        UnifiedTimelineTrimmer()
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            if (editingKeyframeTimestamp != null) "Editing Keyframe" else "New Keyframe",
                            style = MaterialTheme.typography.labelMedium
                        )
                        ParameterSlider("X", xInput, { xInput = it }, -2000f..2000f, "px")
                        ParameterSlider("Y", yInput, { yInput = it }, -2000f..2000f, "px")
                        ParameterSlider("SX", scaleXInput, { scaleXInput = it }, 0.1f..10f, "",
                            { String.format(java.util.Locale.US, "%.2f", it) })
                        ParameterSlider("SY", scaleYInput, { scaleYInput = it }, 0.1f..10f, "",
                            { String.format(java.util.Locale.US, "%.2f", it) })
                        ParameterSlider("Rot", rotationInput, { rotationInput = it }, -360f..360f, "°",
                            { String.format(java.util.Locale.US, "%.1f", it) })
                        ColorSwatch()
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            InsertUpdateButton()
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EditorContent()
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