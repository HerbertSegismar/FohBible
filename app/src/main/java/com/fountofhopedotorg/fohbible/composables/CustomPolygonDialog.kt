package com.fountofhopedotorg.fohbible.composables

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class ActiveControl { ANCHOR, HANDLE_IN, HANDLE_OUT }

data class BezierNode(
    val anchor: Offset,
    val handleIn: Offset,
    val handleOut: Offset
)

private val BezierNodeListSaver = listSaver<List<BezierNode>, String>(
    save = { list ->
        list.map { "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}" }
    },
    restore = { strings ->
        strings.map { s ->
            val parts = s.split(":")
            val a = parts[0].split(",")
            val hi = parts[1].split(",")
            val ho = parts[2].split(",")
            BezierNode(
                Offset(a[0].toFloat(), a[1].toFloat()),
                Offset(hi[0].toFloat(), hi[1].toFloat()),
                Offset(ho[0].toFloat(), ho[1].toFloat())
            )
        }
    }
)

@Composable
fun CustomPolygonDialog(
    initialSerializedPoints: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<BezierNode>) -> Unit
) {
    var points by rememberSaveable(stateSaver = BezierNodeListSaver) {
        mutableStateOf(
            if (!initialSerializedPoints.isNullOrBlank()) {
                try {
                    initialSerializedPoints.split(";").map { nodeStr ->
                        val parts = nodeStr.split(":")
                        val anchorParts = parts[0].split(",")
                        val handleInParts = parts[1].split(",")
                        val handleOutParts = parts[2].split(",")
                        BezierNode(
                            anchor = Offset(anchorParts[0].toFloat(), anchorParts[1].toFloat()),
                            handleIn = Offset(handleInParts[0].toFloat(), handleInParts[1].toFloat()),
                            handleOut = Offset(handleOutParts[0].toFloat(), handleOutParts[1].toFloat())
                        )
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        )
    }

    var redoStack by rememberSaveable(stateSaver = BezierNodeListSaver) { mutableStateOf(emptyList()) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(if (points.isNotEmpty()) 0 else -1) }
    var nudgeAmountIndex by rememberSaveable { mutableIntStateOf(0) }
    var activeControl by rememberSaveable { mutableStateOf(ActiveControl.ANCHOR) }

    var drawingAreaSize by remember { mutableStateOf(IntSize.Zero) }
    val nudgeAmounts = listOf(0.01f, 0.05f, 0.1f)
    val currentNudgeAmount = nudgeAmounts[nudgeAmountIndex]

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isEditing = !initialSerializedPoints.isNullOrBlank()

    val nudgePoint = { dx: Float, dy: Float ->
        if (selectedIndex in points.indices) {
            val p = points[selectedIndex]
            points = points.toMutableList().also { list ->
                when (activeControl) {
                    ActiveControl.ANCHOR -> {
                        val newAnchor = Offset(
                            (p.anchor.x + dx).coerceIn(0f, 1f),
                            (p.anchor.y + dy).coerceIn(0f, 1f)
                        )
                        val moveX = newAnchor.x - p.anchor.x
                        val moveY = newAnchor.y - p.anchor.y
                        list[selectedIndex] = p.copy(
                            anchor = newAnchor,
                            handleIn = Offset((p.handleIn.x + moveX).coerceIn(0f, 1f), (p.handleIn.y + moveY).coerceIn(0f, 1f)),
                            handleOut = Offset((p.handleOut.x + moveX).coerceIn(0f, 1f), (p.handleOut.y + moveY).coerceIn(0f, 1f))
                        )
                    }
                    ActiveControl.HANDLE_IN -> {
                        list[selectedIndex] = p.copy(
                            handleIn = Offset(
                                (p.handleIn.x + dx).coerceIn(0f, 1f),
                                (p.handleIn.y + dy).coerceIn(0f, 1f)
                            )
                        )
                    }
                    ActiveControl.HANDLE_OUT -> {
                        list[selectedIndex] = p.copy(
                            handleOut = Offset(
                                (p.handleOut.x + dx).coerceIn(0f, 1f),
                                (p.handleOut.y + dy).coerceIn(0f, 1f)
                            )
                        )
                    }
                }
            }
        }
    }

    val canvasArea: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier
                .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                .onSizeChanged { drawingAreaSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        if (drawingAreaSize.width > 0 && drawingAreaSize.height > 0) {
                            val normX = tapOffset.x / drawingAreaSize.width
                            val normY = tapOffset.y / drawingAreaSize.height
                            val clamped = Offset(
                                normX.coerceIn(0f, 1f),
                                normY.coerceIn(0f, 1f)
                            )
                            points = points + BezierNode(clamped, clamped, clamped)
                            selectedIndex = points.lastIndex
                            activeControl = ActiveControl.ANCHOR
                            redoStack = emptyList()
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasSize = this.size
                if (points.size >= 3) {
                    val path = Path().apply {
                        moveTo(
                            points[0].anchor.x * canvasSize.width,
                            points[0].anchor.y * canvasSize.height
                        )
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            cubicTo(
                                prev.handleOut.x * canvasSize.width, prev.handleOut.y * canvasSize.height,
                                curr.handleIn.x * canvasSize.width, curr.handleIn.y * canvasSize.height,
                                curr.anchor.x * canvasSize.width, curr.anchor.y * canvasSize.height
                            )
                        }
                        val last = points.last()
                        val first = points.first()
                        cubicTo(
                            last.handleOut.x * canvasSize.width, last.handleOut.y * canvasSize.height,
                            first.handleIn.x * canvasSize.width, first.handleIn.y * canvasSize.height,
                            first.anchor.x * canvasSize.width, first.anchor.y * canvasSize.height
                        )
                        close()
                    }
                    drawPath(path, color = Color(0x4000BCD4), style = Fill)
                    drawPath(path, color = Color(0xFF00BCD4), style = Stroke(width = 3f))
                }
                if (points.size >= 2) {
                    for (i in 0 until points.size) {
                        val curr = points[i]
                        val next = points[(i + 1) % points.size]
                        if (i == points.lastIndex && points.size < 3) continue
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(curr.anchor.x * canvasSize.width, curr.anchor.y * canvasSize.height),
                            end = Offset(next.anchor.x * canvasSize.width, next.anchor.y * canvasSize.height),
                            strokeWidth = 2f
                        )
                    }
                }
                points.forEachIndexed { index, point ->
                    val isSelected = index == selectedIndex
                    val anchorPos = Offset(point.anchor.x * canvasSize.width, point.anchor.y * canvasSize.height)

                    drawCircle(
                        color = if (isSelected && activeControl == ActiveControl.ANCHOR) Color.Blue
                        else if (isSelected) Color.Cyan
                        else Color.Red,
                        radius = if (isSelected && activeControl == ActiveControl.ANCHOR) 12f else 8f,
                        center = anchorPos
                    )
                    if (isSelected) {
                        val hiPos = Offset(point.handleIn.x * canvasSize.width, point.handleIn.y * canvasSize.height)
                        val hoPos = Offset(point.handleOut.x * canvasSize.width, point.handleOut.y * canvasSize.height)

                        drawLine(Color.Gray, anchorPos, hiPos, 2f)
                        drawCircle(
                            color = if (activeControl == ActiveControl.HANDLE_IN) Color.Green else Color.DarkGray,
                            radius = if (activeControl == ActiveControl.HANDLE_IN) 10f else 6f,
                            center = hiPos
                        )

                        drawLine(Color.Gray, anchorPos, hoPos, 2f)
                        drawCircle(
                            color = if (activeControl == ActiveControl.HANDLE_OUT) Color.Magenta else Color.DarkGray,
                            radius = if (activeControl == ActiveControl.HANDLE_OUT) 10f else 6f,
                            center = hoPos
                        )
                    }
                }
            }
        }
    }

    val controlsArea: @Composable (Modifier) -> Unit = { modifier ->
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLandscape) {
                Text(
                    text = if (isEditing) "Edit Polygon Points" else "Tap to Add Polygon Points",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newPoint = if (selectedIndex in points.indices) {
                            val p = points[selectedIndex]
                            BezierNode(
                                Offset((p.anchor.x + 0.05f).coerceIn(0f, 1f), (p.anchor.y + 0.05f).coerceIn(0f, 1f)),
                                Offset((p.handleIn.x + 0.05f).coerceIn(0f, 1f), (p.handleIn.y + 0.05f).coerceIn(0f, 1f)),
                                Offset((p.handleOut.x + 0.05f).coerceIn(0f, 1f), (p.handleOut.y + 0.05f).coerceIn(0f, 1f))
                            )
                        } else {
                            BezierNode(Offset(0.5f, 0.5f), Offset(0.5f, 0.5f), Offset(0.5f, 0.5f))
                        }
                        val insertIndex = if (selectedIndex in points.indices) selectedIndex + 1 else points.size
                        points = points.toMutableList().apply { add(insertIndex, newPoint) }
                        selectedIndex = insertIndex
                        activeControl = ActiveControl.ANCHOR
                        redoStack = emptyList()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Point", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(
                    onClick = {
                        if (points.isNotEmpty() && selectedIndex in points.indices) {
                            points = points.toMutableList().apply { removeAt(selectedIndex) }
                            if (points.isEmpty()) {
                                selectedIndex = -1
                            } else if (selectedIndex >= points.size) {
                                selectedIndex = points.lastIndex
                            }
                            redoStack = emptyList()
                        }
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Point", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(
                    onClick = {
                        if (points.isNotEmpty()) {
                            selectedIndex = (selectedIndex - 1 + points.size) % points.size
                        }
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cycle Selected Point", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(
                    onClick = {
                        if (points.isNotEmpty()) {
                            selectedIndex = (selectedIndex + 1) % points.size
                        }
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Cycle Selected Point", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(
                    onClick = {
                        if (points.isNotEmpty()) {
                            val removedPoint = points.last()
                            points = points.dropLast(1)
                            redoStack = redoStack + removedPoint

                            if (selectedIndex >= points.size) {
                                selectedIndex = points.lastIndex
                            }
                        }
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(
                    onClick = {
                        if (redoStack.isNotEmpty()) {
                            val restoredPoint = redoStack.last()
                            redoStack = redoStack.dropLast(1)
                            points = points + restoredPoint
                            selectedIndex = points.lastIndex
                        }
                    },
                    enabled = redoStack.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    activeControl = when (activeControl) {
                        ActiveControl.ANCHOR -> ActiveControl.HANDLE_IN
                        ActiveControl.HANDLE_IN -> ActiveControl.HANDLE_OUT
                        ActiveControl.HANDLE_OUT -> ActiveControl.ANCHOR
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                enabled = points.isNotEmpty()
            ) {
                Text(
                    text = "Editing: ${activeControl.name.replace("_", " ")}",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            HybridJoystick(
                nudgeAmount = currentNudgeAmount,
                enabled = points.isNotEmpty(),
                onNudgeAmountClick = {
                    nudgeAmountIndex = (nudgeAmountIndex + 1) % nudgeAmounts.size
                },
                onDirectionClick = { dx, dy ->
                    nudgePoint(dx, dy)
                }
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.75f else 0.9f)
                .fillMaxHeight(if (isLandscape) 1f else 0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                if (!isLandscape) {
                    Text(
                        text = if (isEditing) "Edit Polygon Points" else "Tap to Add Polygon Points",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                canvasArea(Modifier)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                controlsArea(Modifier)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            canvasArea(Modifier.weight(1f, fill = false))
                            Spacer(Modifier.height(16.dp))
                            controlsArea(Modifier.fillMaxWidth())
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Close") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                points = emptyList()
                                redoStack = emptyList()
                                selectedIndex = -1
                            },
                            enabled = points.isNotEmpty()
                        ) {
                            Text("Clear")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                if (points.size >= 3) {
                                    onConfirm(points)
                                }
                            },
                            enabled = points.size >= 3
                        ) {
                            Text(if (isEditing) "Save Changes" else "Add to Canvas")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HybridJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    thumbSize: Dp = 60.dp,
    nudgeAmount: Float,
    enabled: Boolean = true,
    onNudgeAmountClick: () -> Unit,
    onDirectionClick: (dx: Float, dy: Float) -> Unit
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val thumbSizePx = with(density) { thumbSize.toPx() }

    val maxDisplacementPx = (sizePx - thumbSizePx) / 2f
    val allowedDisplacementPx = maxDisplacementPx * 1.3f

    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val currentOffset by rememberUpdatedState(thumbOffset)
    val currentNudgeAmount by rememberUpdatedState(nudgeAmount)
    val currentOnDirectionClick by rememberUpdatedState(onDirectionClick)

    LaunchedEffect(isDragging, enabled) {
        if (!enabled) {
            isDragging = false
            thumbOffset = Offset.Zero
        }

        while (isDragging && enabled) {
            if (currentOffset != Offset.Zero) {
                val normalizedX = currentOffset.x / maxDisplacementPx
                val normalizedY = currentOffset.y / maxDisplacementPx
                val frameSpeedFactor = 0.05f

                currentOnDirectionClick(
                    normalizedX * currentNudgeAmount * frameSpeedFactor,
                    normalizedY * currentNudgeAmount * frameSpeedFactor
                )
            }
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = thumbOffset + dragAmount
                            val currentDisplacement = newOffset.getDistance()
                            thumbOffset = if (currentDisplacement <= allowedDisplacementPx) {
                                newOffset
                            } else {
                                newOffset.times(allowedDisplacementPx / currentDisplacement)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            thumbOffset = Offset.Zero
                        },
                        onDragCancel = {
                            isDragging = false
                            thumbOffset = Offset.Zero
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { onDirectionClick(0f, -nudgeAmount) },
            modifier = Modifier.align(Alignment.TopCenter).size(36.dp),
            enabled = enabled
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Up",
                tint = MaterialTheme.colorScheme.primary.copy(if (enabled) 0.5f else 0.2f)
            )
        }
        IconButton(
            onClick = { onDirectionClick(0f, nudgeAmount) },
            modifier = Modifier.align(Alignment.BottomCenter).size(36.dp),
            enabled = enabled
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Down",
                tint = MaterialTheme.colorScheme.primary.copy(if (enabled) 0.5f else 0.2f)
            )
        }
        IconButton(
            onClick = { onDirectionClick(-nudgeAmount, 0f) },
            modifier = Modifier.align(Alignment.CenterStart).size(36.dp),
            enabled = enabled
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = MaterialTheme.colorScheme.primary.copy(if (enabled) 0.5f else 0.2f)
            )
        }
        IconButton(
            onClick = { onDirectionClick(nudgeAmount, 0f) },
            modifier = Modifier.align(Alignment.CenterEnd).size(36.dp),
            enabled = enabled
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Right",
                tint = MaterialTheme.colorScheme.primary.copy(if (enabled) 0.5f else 0.2f)
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size(thumbSize)
                .background(
                    MaterialTheme.colorScheme.primary.copy(if (enabled) 1f else 0.5f),
                    CircleShape
                )
                .clickable(enabled = enabled) { onNudgeAmountClick() },
            contentAlignment = Alignment.Center
        ) {
            val amountText = when (nudgeAmount) {
                0.01f -> "1%"
                0.05f -> "5%"
                0.1f -> "10%"
                else -> "${(nudgeAmount * 100).toInt()}%"
            }
            Text(
                text = amountText,
                color = Color.White.copy(if (enabled) 1f else 0.5f),
                style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )
        }
    }
}