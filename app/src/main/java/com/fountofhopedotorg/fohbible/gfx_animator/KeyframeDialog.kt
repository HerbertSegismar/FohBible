package com.fountofhopedotorg.fohbible.gfx_animator

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fountofhopedotorg.fohbible.color_wheel.ColorWheelDialog
import com.fountofhopedotorg.fohbible.data.CanvasKeyframe
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.GradientConfig
import java.util.Locale
import kotlin.math.roundToInt

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
        if (str == "NULL") {
            null
        } else {
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
    note: CanvasNote?,
    onDismiss: () -> Unit,
    onSaveKeyframes: (String, List<CanvasKeyframe>) -> Unit,
    timeMultiplier: Float,
    initialGradientConfig: GradientConfig? = null
) {
    if (note == null) return

    val localKeyframes = remember(note.keyframes) { mutableStateListOf(*note.keyframes.toTypedArray()) }

    var timeInput by rememberSaveable { mutableStateOf("") }
    var xInput by rememberSaveable { mutableStateOf(note.offset.x.toString()) }
    var yInput by rememberSaveable { mutableStateOf(note.offset.y.toString()) }
    var scaleXInput by rememberSaveable { mutableStateOf(note.scaleX.toString()) }
    var scaleYInput by rememberSaveable { mutableStateOf(note.scaleY.toString()) }
    var rotationInput by rememberSaveable { mutableStateOf(note.rotation.toString()) }

    val initialNoteColor: Color = remember(note) {
        if (note.content.startsWith("Shape:") || note.content.startsWith("Image:")) note.backgroundColor
        else note.textColor ?: Color.Black
    }
    var pickedColorArgb by rememberSaveable(initialNoteColor) {
        mutableLongStateOf(initialNoteColor.toArgb().toLong())
    }

    var currentGradientConfig by rememberSaveable(
        initialGradientConfig,
        stateSaver = GradientConfigNullableSaver
    ) { mutableStateOf(initialGradientConfig) }

    var showColorDialog by rememberSaveable { mutableStateOf(false) }

    val pickedColor = Color(pickedColorArgb.toInt())
    var editingKeyframeTimestamp by rememberSaveable { mutableStateOf<Long?>(null) }

    fun storedToDisplay(storedMs: Long): Long = (storedMs / timeMultiplier).roundToInt().toLong()
    fun displayToStored(displayMs: Long): Long = (displayMs * timeMultiplier).roundToInt().toLong()

    val autoMaxMs = remember(localKeyframes) {
        val maxStored = localKeyframes.maxOfOrNull { it.timestampMs } ?: 0L
        (storedToDisplay(maxStored) + 1000).coerceAtLeast(5000L)
    }

    var userMaxMs by rememberSaveable { mutableStateOf<Long?>(null) }
    val effectiveMaxMs = maxOf(autoMaxMs, userMaxMs ?: 0L)

    fun populateFromKeyframe(kf: CanvasKeyframe) {
        timeInput = storedToDisplay(kf.timestampMs).toString()
        xInput = kf.x?.toString() ?: note.offset.x.toString()
        yInput = kf.y?.toString() ?: note.offset.y.toString()
        scaleXInput = kf.scaleX?.toString() ?: note.scaleX.toString()
        scaleYInput = kf.scaleY?.toString() ?: note.scaleY.toString()
        rotationInput = kf.rotation?.toString() ?: note.rotation.toString()
        val displayColor = kf.gradientConfig?.startColor ?: kf.color ?: initialNoteColor
        pickedColorArgb = displayColor.toArgb().toLong()
        currentGradientConfig = kf.gradientConfig
    }

    LaunchedEffect(Unit) {
        if (localKeyframes.isEmpty()) {
            val storedZero = displayToStored(0L)
            localKeyframes.add(
                CanvasKeyframe(
                    timestampMs = storedZero,
                    x = note.offset.x,
                    y = note.offset.y,
                    scaleX = note.scaleX,
                    scaleY = note.scaleY,
                    rotation = note.rotation,
                    color = initialNoteColor,
                    gradientConfig = null
                )
            )
        }
    }

    LaunchedEffect(initialGradientConfig, initialNoteColor) {
        val firstKf = localKeyframes.firstOrNull { it.timestampMs == displayToStored(0L) }
        if (firstKf != null) {
            val index = localKeyframes.indexOf(firstKf)
            localKeyframes[index] = firstKf.copy(
                color = initialGradientConfig?.startColor ?: initialNoteColor,
                gradientConfig = initialGradientConfig
            )
        }
    }

    @Composable
    fun TimelineControl() {
        val currentMs = timeInput.toLongOrNull()?.coerceIn(0L, effectiveMaxMs) ?: 0L
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = currentMs.toFloat(),
                onValueChange = { timeInput = it.roundToInt().toString() },
                valueRange = 0f..effectiveMaxMs.toFloat().coerceAtLeast(1000f),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.1f)
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .shadow(4.dp, CircleShape)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                    )
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(72.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(0.1f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                BasicTextField(
                    value = timeInput,
                    onValueChange = { newValue ->
                        if (newValue.all { c -> c.isDigit() }) {
                            timeInput = newValue
                            newValue.toLongOrNull()?.let { typedMs ->
                                if (typedMs > effectiveMaxMs && typedMs > 5000) {
                                    userMaxMs = typedMs
                                }
                            }
                        }
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "ms",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
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
                val rawT = timeInput.toLongOrNull() ?: 0L
                val clampedDisplayMs = rawT.coerceIn(0L, effectiveMaxMs)
                val storedMs = displayToStored(clampedDisplayMs)

                val x = xInput.toFloatOrNull() ?: note.offset.x
                val y = yInput.toFloatOrNull() ?: note.offset.y
                val sx = scaleXInput.toFloatOrNull() ?: note.scaleX
                val sy = scaleYInput.toFloatOrNull() ?: note.scaleY
                val rot = rotationInput.toFloatOrNull() ?: note.rotation

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

                val bumpedMs = (clampedDisplayMs + 10L).coerceAtMost(effectiveMaxMs)
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
                if (editingKeyframeTimestamp != null) "Update" else "Insert",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    @Composable
    fun EditorContent(includeKeyframeList: Boolean = true) {
        Text("Timeline", style = MaterialTheme.typography.labelMedium)
        TimelineControl()

        if (includeKeyframeList) {
            Text("Keyframes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            KeyframeListCompact()
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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

        ColorSwatch()

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            InsertUpdateButton()
        }
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
                        Text("Timeline", style = MaterialTheme.typography.labelMedium)
                        TimelineControl()
                        Text("Keyframes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                        KeyframeListCompact()
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
                            { String.format(Locale.US, "%.2f", it) })
                        ParameterSlider("SY", scaleYInput, { scaleYInput = it }, 0.1f..10f, "",
                            { String.format(Locale.US, "%.2f", it) })
                        ParameterSlider("Rot", rotationInput, { rotationInput = it }, -360f..360f, "°",
                            { String.format(Locale.US, "%.1f", it) })
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
            TextButton(onClick = { onSaveKeyframes(note.id, localKeyframes.sortedBy { it.timestampMs }) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}